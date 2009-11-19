/**
 * This class will read on a audio stream and stream out only the audio between start and end speech.  
 * It use Sphinx4 frontend to do the endpointing. 
 *
 * @author Spencer Lord {@literal <}<a href="mailto:spencer@spokentech.com">spencer@spokentech.com</a>{@literal >}
 */
package com.spokentech.speechdown.client.endpoint;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.log4j.Logger;

import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;


import com.spokentech.speechdown.client.sphinx.SpeechDataStreamer;
import com.spokentech.speechdown.common.SpeechEventListener;
import com.spokentech.speechdown.common.sphinx.AudioStreamDataSource;

import com.spokentech.speechdown.server.recog.StreamDataSource;

// TODO: Auto-generated Javadoc
/**
 * The Class StreamS4EndPointingInputStream.  This class will read on a audio stream and stream out only the audio between start and end speech.  
 * It use Sphinx4 frontend to do the endpointing. 
 */
public class StreamS4EndPointingInputStream extends EndPointingInputStreamBase implements EndPointingInputStream {
	
    private static Logger _logger = Logger.getLogger(StreamS4EndPointingInputStream.class);

	private InputStream  stream;
	private AudioFormat  format;

	StreamDataSource dataSource = null;


	private String mimeType;
	
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


    
	public void init() {
	}
	
    
	/**
	 * Sets the up stream.
	 * 
	 * @param stream the new up stream
	 */
	public void setupStream(InputStream stream, AudioFormat format) {
		_logger.info("Setting up the stream");
		this.stream = stream;
		this.format = format;
        setupPipedStream();
	}
	
	/**
	 * Sets the up stream.
	 * 
	 * @param stream the new up stream
	 */
	public void setupStream(AudioInputStream stream) {
		_logger.info("Setting up the stream");
		this.stream = stream;
		this.format = stream.getFormat();
        setupPipedStream();
	}
	
	
	/**
	 * Sets the up stream.
	 * 
	 * @param stream the new up stream
	 * @deprecated
	 */
	public void setupStream(PipedInputStream stream) {
		_logger.info("Setting up the stream");
		this.stream = stream;
    	float sampleRate = 8000.0F;
    	//8000,11025,16000,22050,44100
    	int sampleSizeInBits = 16;
    	//8,16
    	int channels = 1;
    	//1,2
    	boolean signed = true;
    	//true,false
    	boolean bigEndian = false;
    	//true,false
		this.format= new AudioFormat(sampleRate,
    			sampleSizeInBits,
    			channels,
    			signed,
    			bigEndian);
		//this.stream = new AudioInputStream(stream,getAudioFormat(),-1);
		setupPipedStream();
	}
	

    /**
     * gets a hard coded audioformat
     * 
     * @param configFile the s4ConfigFile to set
     * @deprecated
     */
    private static AudioFormat getAudioFormat(){
    	float sampleRate = 8000.0F;
    	//8000,11025,16000,22050,44100
    	int sampleSizeInBits = 16;
    	//8,16
    	int channels = 1;
    	//1,2
    	boolean signed = true;
    	//true,false
    	boolean bigEndian = false;
    	//true,false
    	return new AudioFormat(sampleRate,
    			sampleSizeInBits,
    			channels,
    			signed,
    			bigEndian);
    }
	


	/**
	 * Shutdown stream.
	 */
	public void shutdownStream() {
		//TODO:
		_logger.info("Shutdown stream not implemented!");
	}
	
	
	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#startAudioTransfer(long, com.spokentech.speechdown.client.SpeechEventListener)
	 */
	public void startAudioTransfer(long timeout, SpeechEventListener listener) throws InstantiationException, IOException {
		
		

		_listener = new Listener(listener);
		
		dataSource = new AudioStreamDataSource();
		 	
		FrontEnd frontEnd = createFrontend(false, false, (DataProcessor) dataSource, listener);
 	
		dataSource.setInputStream((InputStream)stream, "ws-audiostream", (int)format.getSampleRate(), format.isBigEndian(), format.getSampleSizeInBits()/8,format.getEncoding());
 		
		_logger.info("Starting audio trasnfer");
		
		
	
		_logger.info("Starting audio trasnfer");
		SpeechDataStreamer sds = new SpeechDataStreamer();
		sds.startStreaming(frontEnd, outputStream);
		
		if (timeout > 0)
			startInputTimers(timeout);
		
		_state = WAITING_FOR_SPEECH;
	}

	
	
    /* (non-Javadoc)
     * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#stopAudioTransfer()
     */
    public synchronized void stopAudioTransfer() {
    	_logger.debug("Stopping stream");
    	if (dataSource != null) {
	    	try {
		        dataSource.closeDataStream();
	        } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
	        }
    	}
    	if (_timer !=null) 
    	   _timer.cancel();
    }
	
	
    /**
     * Starts the input timers which trigger no-input-timeout if speech has not started after the specified time.
     * 
     * @param noInputTimeout the amount of time to wait, in milliseconds, before triggering a no-input-timeout.
     * 
     * @return {@code true} if input timers were started or {@code false} if speech has already started.
     * 
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


	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#getFormat1()
	 */
	@Override
    public AudioFormat getFormat1() {
	    // TODO Auto-generated method stub
		return this.format;
    }


	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#getFormat2()
	 */
	@Override
    public javax.media.format.AudioFormat getFormat2() {
	    // TODO Auto-generated method stub
	    return null;
    }
	

	

	
}
