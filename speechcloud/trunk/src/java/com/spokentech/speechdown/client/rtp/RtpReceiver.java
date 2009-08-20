package com.spokentech.speechdown.client.rtp;

import static org.speechforge.cairo.jmf.JMFUtil.CONTENT_DESCRIPTOR_RAW;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.Processor;
import javax.media.format.AudioFormat;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.speechforge.cairo.jmf.ProcessorStarter;
import org.speechforge.cairo.rtp.server.RTPStreamReplicator;
import org.speechforge.cairo.rtp.server.sphinx.RawAudioProcessor;
import org.speechforge.cairo.rtp.server.sphinx.RawAudioTransferHandler;
import org.speechforge.cairo.rtp.server.sphinx.SourceAudioFormat;


import edu.cmu.sphinx.util.props.ConfigurationManager;
import com.spokentech.speechdown.client.SpeechEventListener;
import com.spokentech.speechdown.client.SpeechEventListenerDecorator;
import com.spokentech.speechdown.client.sphinx.SpeechDataMonitor;

public class RtpReceiver {
	
    private static Logger _logger = Logger.getLogger(RtpReceiver.class);

    public static final short WAITING_FOR_SPEECH = 0;
    public static final short SPEECH_IN_PROGRESS = 1;
    public static final short COMPLETE = 2;
    volatile short _state = COMPLETE;

	private int id;
	private RawAudioProcessor _rawAudioProcessor;
	private RawAudioTransferHandler _rawAudioTransferHandler;
	private ConfigurationManager cm;
	
	private SpeechEventListener _listener;
	
    private RTPStreamReplicator _replicator;
    private Processor _processor;
	
	private /*static*/ Timer _timer = new Timer();
    TimerTask _noInputTimeoutTask;
	 
	private String s4ConfigFile = "/config/sphinx-config.xml";
	

    /**
     * @return the s4ConfigFile
     */
    public String getS4ConfigFile() {
    	return s4ConfigFile;
    }


	/**
     * @param configFile the s4ConfigFile to set
     */
    public void setS4ConfigFile(String configFile) {
    	s4ConfigFile = configFile;
    }


	public void init() {
	    URL sphinxConfigUrl = RtpReceiver.class.getResource(s4ConfigFile);
        if (sphinxConfigUrl == null) {
            throw new RuntimeException("Sphinx config file not found!");
        }
        cm = new ConfigurationManager(sphinxConfigUrl);
	}
	
	public void setupStream(RTPStreamReplicator replicator) {
		_logger.info("Setting up the stream");
        Validate.notNull(replicator, "Null replicator!");
        _replicator = replicator;
	}

	public void shutdownStream() {
		//TODO:
		_logger.info("Shutdown stream not implemented!");
	}
	
	
	public AudioFormat getFormat() {
		return SourceAudioFormat.PREFERRED_MEDIA_FORMATS[0];
	}
	
	public void startAudioTransfer(long timeout, OutputStream out, SpeechEventListener listener) throws InstantiationException, IOException {

		_listener = new Listener(listener);

		
		//TODO: do I need multiple front ends
		id = 1;

		//get elements from the s4 front end
		SpeechDataMonitor speechDataMonitor = (SpeechDataMonitor) cm.lookup("speechDataMonitor"+id);
		if (speechDataMonitor != null) {
			speechDataMonitor.setSpeechEventListener(listener);
		}

		Object primaryInput = cm.lookup("primaryInput"+id);

		if (primaryInput instanceof RawAudioProcessor) {
			_rawAudioProcessor = (RawAudioProcessor) primaryInput;
		} else {
			String className = (primaryInput == null) ? null : primaryInput.getClass().getName();
			throw new InstantiationException("Unsupported primary input type: " + className);
		}

		//get the processor from the rtpStream
		if (_processor != null) {
			throw new IllegalStateException("Grabbing already in progress!");
			// TODO: cancel or queue request instead (depending upon value of 'cancel-if-queue' header)
		}
		_processor = _replicator.createRealizedProcessor(CONTENT_DESCRIPTOR_RAW, 10000,SourceAudioFormat.PREFERRED_MEDIA_FORMATS); // TODO: specify audio format

		PushBufferDataSource dataSource = (PushBufferDataSource) _processor.getDataOutput();

		if (dataSource == null) {
			throw new IOException("Processor.getDataOutput() returned null!");
		}

		try {
			_logger.debug("xxx...");

			if (_rawAudioTransferHandler != null) {
				throw new IllegalStateException("Recognition already in progress!");
			}

			PushBufferStream[] streams = dataSource.getStreams();
			if (streams.length != 1) {
				throw new IllegalArgumentException(
						"Rec engine can handle only single stream datasources, # of streams: " + streams);
			}
			if (_logger.isDebugEnabled()) {
				_logger.debug("Starting recognition on stream format: " + streams[0].getFormat());
			}
			try {
				_rawAudioTransferHandler = new RawAudioTransferHandler(_rawAudioProcessor);
				_rawAudioTransferHandler.startProcessing(streams[0]);
			} catch (UnsupportedEncodingException e) {
				_rawAudioTransferHandler = null;
				throw e;
			}

			_processor.addControllerListener(new ProcessorStarter());
			_processor.start();

			if (timeout > 0) {
				startInputTimers(timeout);
			}

		} catch (IOException e) {
			stopAudioTransfer();
			throw e;
		}
	}

	
	
    public synchronized void stopAudioTransfer() {
        if (_processor != null) {
          _logger.debug("Closing processor...");
            _processor.close();
            _processor = null;
        }

        if (_rawAudioTransferHandler != null) {
            _rawAudioTransferHandler.stopProcessing();
            _rawAudioTransferHandler = null;
        }
        // TODO: should wait to set this until after run thread completes (i.e. recognizer is cleared)

    }
	
	
    /**
     * Starts the input timers which trigger no-input-timeout if speech has not started after the specified time.
     * @param noInputTimeout the amount of time to wait, in milliseconds, before triggering a no-input-timeout. 
     * @return {@code true} if input timers were started or {@code false} if speech has already started.
     * @throws IllegalStateException if recognition is not in progress or if the input timers have already been started.
     */
    public synchronized boolean startInputTimers(long noInputTimeout) throws IllegalStateException {
        if (noInputTimeout <= 0) {
            throw new IllegalArgumentException("Illegal value for no-input-timeout: " + noInputTimeout);
        }
        if (_processor == null) {
            throw new IllegalStateException("Recognition not in progress!");
        }
        if (_noInputTimeoutTask != null) {
            throw new IllegalStateException("InputTimer already started!");
        }

        boolean startInputTimers = (_state == WAITING_FOR_SPEECH); 
        if (startInputTimers) {
            _noInputTimeoutTask = new NoInputTimeoutTask();
            _timer.schedule(_noInputTimeoutTask, noInputTimeout);
        }

        return startInputTimers;
    }
	
    private class NoInputTimeoutTask extends TimerTask {

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            synchronized (RtpReceiver.this) {
                _noInputTimeoutTask = null;
                if (_state == WAITING_FOR_SPEECH) {
                    _state = COMPLETE;
                    stopAudioTransfer();
                    if (_listener != null) {
                    	_listener.noInputTimeout();
                    }
                }
            }
        }
        
    }

	   private class Listener extends SpeechEventListenerDecorator {

	        /**
	         * TODOC
	         * @param recogListener
	         */
	        public Listener(SpeechEventListener speechEventListener) {
	            super(speechEventListener);
	        }

	        /* (non-Javadoc)
	         * @see org.speechforge.cairo.server.recog.RecogListener#speechStarted()
	         */
	        @Override
	        public void speechStarted() {
	            _logger.debug("speechStarted()");

	            synchronized (RtpReceiver.this) {
	                if (_state == WAITING_FOR_SPEECH) {
	                    _state = SPEECH_IN_PROGRESS;
	                }
	                if (_noInputTimeoutTask != null) {
	                    _noInputTimeoutTask.cancel();
	                    _noInputTimeoutTask = null;
	                }
	            }
	            super.speechStarted();
	        }

	        public void speechEnded() {
	            _logger.debug("speechEnded()");
	            synchronized (RtpReceiver.this) {
	            	stopAudioTransfer();
	            	_state = COMPLETE;
	            }
	            super.speechEnded();
	        }

	        public void noInputTimeout() {
	            _logger.debug("no input timeout()");
	            synchronized (RtpReceiver.this) {
	            	stopAudioTransfer();
	            	_state = COMPLETE;
	            }	   
	            super.noInputTimeout();
	        }

	    }
	
	
}
