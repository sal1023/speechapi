package com.spokentech.speechdown.client;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFormat;

import org.apache.log4j.Logger;
import org.speechforge.cairo.rtp.server.sphinx.SourceAudioFormat;
import edu.cmu.sphinx.frontend.BaseDataProcessor;

import edu.cmu.sphinx.util.props.ConfigurationManager;import com.spokentech.speechdown.client.SpeechEventListener;
import com.spokentech.speechdown.client.SpeechEventListenerDecorator;
import com.spokentech.speechdown.client.sphinx.SpeechDataMonitor;
import com.spokentech.speechdown.client.sphinx.SpeechDataStreamer;

public class MicReceiver {
	
    private static Logger _logger = Logger.getLogger(MicReceiver.class);

    public static final short WAITING_FOR_SPEECH = 0;
    public static final short SPEECH_IN_PROGRESS = 1;
    public static final short COMPLETE = 2;
    volatile short _state = COMPLETE;

	private int id;

	private ConfigurationManager cm;

	private Microphone mic;
	
	private SpeechEventListener _listener;

	
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
	    //URL sphinxConfigUrl = MicReceiver.class.getResource(s4ConfigFile);
        //if (sphinxConfigUrl == null) {
        //    throw new RuntimeException("Sphinx config file not found!");
        //}
        //cm = new ConfigurationManager(sphinxConfigUrl);
        _logger.debug("config: "+s4ConfigFile);
        cm = new ConfigurationManager(s4ConfigFile);
	}
	
	public void setupStream() {
		_logger.info("Setting up the stream");
	}

	public void shutdownStream() {
		//TODO:
		_logger.info("Shutdown stream not implemented!");
	}
	
	
	public AudioFormat getFormat() {
		return mic.getAudioFormat();

	}
	
	public void startAudioTransfer(long timeout, OutputStream out, SpeechEventListener listener) throws InstantiationException, IOException {

		_listener = new Listener(listener);
		
		//TODO: do I need multiple front ends
		id = 10;

		//get elements from the s4 front end
		SpeechDataMonitor speechDataMonitor = (SpeechDataMonitor) cm.lookup("speechDataMonitor");
		if (speechDataMonitor != null) {
			speechDataMonitor.setSpeechEventListener(_listener);
		}

		BaseDataProcessor frontEnd = (BaseDataProcessor) cm.lookup("frontEnd");
		frontEnd.initialize();
		
		SpeechDataStreamer sds = new SpeechDataStreamer();
		sds.startStreaming(frontEnd, out);

		Object primaryInput = cm.lookup("microphone");

		if (primaryInput instanceof Microphone) {
			mic = (Microphone) primaryInput;
			mic.initialize();
			mic.startRecording();
		} else {
			String className = (primaryInput == null) ? null : primaryInput.getClass().getName();
			throw new InstantiationException("Unsupported primary input type: " + className);
		}
		_state = WAITING_FOR_SPEECH;
	}

	
	
    public synchronized void stopAudioTransfer() {
    	_logger.info("Stopping mic");
    	mic.stopRecording();
    	//mic.shutDown();
    	_timer.cancel();
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
        //TODO: Determine if recognition is in progress and throw exception as needed
        //if (_processor == null) {
        //    throw new IllegalStateException("Recognition not in progress!");
        //}
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
            synchronized (MicReceiver.this) {
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
	            _logger.info("speechStarted()");

	            synchronized (MicReceiver.this) {
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
	            _logger.info("speechEnded()");
	            synchronized (MicReceiver.this) {
	            	stopAudioTransfer();
	            	_state = COMPLETE;
	            }
	            super.speechEnded();
	        }

	        public void noInputTimeout() {
	            _logger.info("no input timeout()");
	            synchronized (MicReceiver.this) {
	            	stopAudioTransfer();
	            	_state = COMPLETE;
	            }	   
	            super.noInputTimeout();
	        }

	    }
	
	
}
