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

import org.apache.log4j.Logger;

import com.spokentech.speechdown.client.util.FormatUtils;
import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.SpeechEventListener;

import com.spokentech.speechdown.server.recog.StreamDataSource;

// TODO: Auto-generated Javadoc
/**
 * The Class StreamS4EndPointingInputStream.  This class will read on a audio stream and stream out only the audio between start and end speech.  
 * It use Sphinx4 frontend to do the endpointing. 
 */
public class JavaSoundStreamS4EndPointingInputStream extends EndPointingInputStreamBase implements EndPointingInputStream {

	private static Logger _logger = Logger.getLogger(JavaSoundStreamS4EndPointingInputStream.class);

	
    public JavaSoundStreamS4EndPointingInputStream(EndPointer ep) {
	    super(ep);
	    // TODO Auto-generated constructor stub
    }



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

    
	/**
	 * Sets the up stream.
	 * 
	 * @param stream the new up stream
	 */
	public void setupStream(InputStream stream, AudioFormat format) {
		_logger.debug("Setting up the stream");
		this.stream = stream;
        this.format = FormatUtils.covertToNeutral(format);
        setupPipedStream();
	}
	
	/**
	 * Sets the up stream.
	 * 
	 * @param stream the new up stream
	 */
	public void setupStream(AudioInputStream stream) {
		_logger.debug("Setting up the stream");
		this.stream = stream;
		this.format = FormatUtils.covertToNeutral(stream.getFormat());
        setupPipedStream();
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
    public AFormat getFormat() {
		return  this.format;
    }

	
}
