/**
 * This class will read on a rtp stream and stream out only the audio between start and end speech.  
 * It use Sphinx4 frontend to do the endpointing. 
 *
 * @author Spencer Lord {@literal <}<a href="mailto:spencer@spokentech.com">spencer@spokentech.com</a>{@literal >}
 */
package com.spokentech.speechdown.client.endpoint;

import static org.speechforge.cairo.jmf.JMFUtil.CONTENT_DESCRIPTOR_RAW;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.media.Processor;
import javax.media.format.AudioFormat;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.speechforge.cairo.jmf.ProcessorStarter;
import org.speechforge.cairo.rtp.AudioFormats;
import org.speechforge.cairo.rtp.server.RTPStreamReplicator;
import org.speechforge.cairo.rtp.server.RTPStreamReplicator.ProcessorReplicatorPair;
import org.speechforge.cairo.rtp.server.sphinx.RawAudioProcessor;
import org.speechforge.cairo.rtp.server.sphinx.RawAudioTransferHandler;
import org.speechforge.cairo.rtp.server.sphinx.SourceAudioFormat;

import edu.cmu.sphinx.frontend.FrontEnd;
import com.spokentech.speechdown.client.sphinx.SpeechDataStreamer;
import com.spokentech.speechdown.common.SpeechEventListener;

// TODO: Auto-generated Javadoc
/**
 * receives audio from an rtp channel and sends to outputstream (after doing endpointing)
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:slord@users.sourceforge.net">slord@users.sourceforge.net</a>{@literal >}
 */
public class RtpS4EndPointingInputStream extends EndPointingInputStreamBase implements EndPointingInputStream {
	
    private static Logger _logger = Logger.getLogger(RtpS4EndPointingInputStream.class);

    public static final javax.media.format.AudioFormat[] PREFERRED_MEDIA_FORMATS = { new javax.media.format.AudioFormat(
            javax.media.format.AudioFormat.LINEAR, //encoding
            8000.0,                                //sample rate
            16,                                    //sample size in bits
            1,                                     //channels
            javax.media.format.AudioFormat.LITTLE_ENDIAN,
            javax.media.format.AudioFormat.SIGNED
        ) };


    
    
	private int id;
	private RawAudioProcessor _rawAudioProcessor;
	private RawAudioTransferHandler _rawAudioTransferHandler;

    private RTPStreamReplicator _replicator;
    private Processor _processor;
	
	private String mimeType;

	private PushBufferDataSource _pbds;
	
	
	
	/**
	 * Gets the mime type.
	 * 
	 * @return the mimeType
	 */
    public String getMimeType() {
    	return mimeType;
    }


	/**
	 * Sets the mime type.
	 * 
	 * @param mimeType the mimeType to set
	 */
    public void setMimeType(String mimeType) {
    	this.mimeType = mimeType;
    }
	
	
	/**
	 * Sets the up stream.
	 * 
	 * @param replicator the new up stream
	 */
	public void setupStream(RTPStreamReplicator replicator) {
		_logger.info("Setting up the stream");
        Validate.notNull(replicator, "Null replicator!");
        this._replicator = replicator;
        setupPipedStream();
	}

	/* (non-Javadoc)
     * @see com.spokentech.speechdown.client.rtp.EndPointingReceiver#shutdownStream()
     */
	/**
	 * Shutdown stream.
	 */
	public void shutdownStream() {
		//TODO:
		_logger.info("Shutdown stream not implemented!");
	}

	
	/* (non-Javadoc)
     * @see com.spokentech.speechdown.client.rtp.EndPointingReceiver#startAudioTransfer(long, java.io.OutputStream, com.spokentech.speechdown.client.SpeechEventListener)
     */
	public void startAudioTransfer(long timeout, SpeechEventListener listener) throws InstantiationException, IOException {

		_logger.debug("STARTING AUIDO TRANSFER!!!!!!");
		_listener = new Listener(listener);

		RawAudioProcessor primaryInput = new RawAudioProcessor(10);
		FrontEnd frontEnd = createFrontend(false, false, primaryInput, _listener);



		//get the processor from the rtpStream
		if (_processor != null) {
			throw new IllegalStateException("Grabbing already in progress!");
			// TODO: cancel or queue request instead (depending upon value of 'cancel-if-queue' header)
		}
		
		
		_logger.info("SAL: " +SourceAudioFormat.PREFERRED_MEDIA_FORMATS[0].toString());
		_logger.info("SAL COUNT:"+SourceAudioFormat.PREFERRED_MEDIA_FORMATS.length);
		
		ProcessorReplicatorPair prp = _replicator.createRealizedProcessor(CONTENT_DESCRIPTOR_RAW, 10000,SourceAudioFormat.PREFERRED_MEDIA_FORMATS); // TODO: specify audio format

		 _processor = prp.getProc();
		 _pbds = prp.getPbds();
		PushBufferDataSource dataSource = (PushBufferDataSource) _processor.getDataOutput();

		if (dataSource == null) {
			throw new IOException("Processor.getDataOutput() returned null!");
		}


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
		_logger.info("Starting recognition on stream format: " + streams[0].getFormat());
		try {
			_rawAudioTransferHandler = new RawAudioTransferHandler(primaryInput);
			_rawAudioTransferHandler.startProcessing(streams[0]);
			_logger.debug("Started the raw audio transfer handler");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		_processor.addControllerListener(new ProcessorStarter());
		_processor.start();

		if (timeout > 0) {
			startInputTimers(timeout);
		}

		SpeechDataStreamer sds = new SpeechDataStreamer();
		sds.startStreaming(frontEnd, outputStream);
		
		_state = WAITING_FOR_SPEECH;
		
	}

    /* (non-Javadoc)
     * @see com.spokentech.speechdown.client.rtp.EndPointingReceiver#stopAudioTransfer()
     */
    public synchronized void stopAudioTransfer() {
        if (_processor != null) {
            _logger.debug("Closing processor...");
            _replicator.removeReplicant(_pbds);
            _processor.close();
            _processor = null;
        }

        if (_rawAudioTransferHandler != null) {
            _rawAudioTransferHandler.stopProcessing();
            _rawAudioTransferHandler = null;
        }
  

    }
	
	
    /* (non-Javadoc)
     * @see com.spokentech.speechdown.client.rtp.EndPointingReceiver#startInputTimers(long)
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


	//TODO: Refactor Source Audio Fromat to handel conversions to and from all formats of audio formats (Sound, jmf, sip)
	//private  SourceAudioFormat format;
	
    
	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#getFormat1()
	 */
	@Override
    public javax.sound.sampled.AudioFormat getFormat1() {
		javax.sound.sampled.AudioFormat f = new javax.sound.sampled.AudioFormat(
				javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
                8000,
                16,
                1,
                2,
                16000,
                true);;
		return f;
    }
	
	/* (non-Javadoc)
     * @see com.spokentech.speechdown.client.rtp.EndPointingReceiver#getFormat()
     */
	public AudioFormat getFormat2() {
		return SourceAudioFormat.PREFERRED_MEDIA_FORMATS[0];
	}

}
