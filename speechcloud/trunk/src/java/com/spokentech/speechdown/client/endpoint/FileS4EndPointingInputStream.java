/**
 * The Class FileS4EndPointingInputStream.  This class will read on a audio file and stream out only the audio between start and end speech.  
 * It use Sphinx4 frontend to do the endpointing.
 *
 * @author Spencer Lord {@literal <}<a href="mailto:spencer@spokentech.com">spencer@spokentech.com</a>{@literal >}
 */
package com.spokentech.speechdown.client.endpoint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat.Type;
import org.apache.log4j.Logger;

import com.spokentech.speechdown.common.SpeechEventListener;

// TODO: Auto-generated Javadoc
/**
 * The Class FileS4EndPointingInputStream.  This class will read on a audio file and stream out only the audio between start and end speech.  
 * It use Sphinx4 frontend to do the endpointing.
 */
public class FileS4EndPointingInputStream implements EndPointingInputStream {
	
    private static Logger _logger = Logger.getLogger(FileS4EndPointingInputStream.class);

    StreamS4EndPointingInputStream delegate;
    
	/**
	 * Instantiates a new file s4 end pointing input stream.
	 */
	public FileS4EndPointingInputStream() {
	    super();
	    delegate = new StreamS4EndPointingInputStream();
    }
	

    /**
     * Gets the s4 config file.
     * 
     * @return the s4ConfigFile
     */
    public String getS4ConfigFile() {
    	return delegate.getS4ConfigFile();
    }


	/**
	 * Sets the s4 config file.
	 * 
	 * @param configFile the s4ConfigFile to set
	 */
    public void setS4ConfigFile(String configFile) {
    	delegate.setS4ConfigFile(configFile);
    }



	/**
	 * Sets the up stream.
	 * 
	 * @param file the new up stream
	 */
	public void setupStream(File file) {
    	// read in the sound file.
    	AudioInputStream audioInputStream = null;
    	Type type = null;
    	try {
    		audioInputStream = AudioSystem.getAudioInputStream(file);
    		type = AudioSystem.getAudioFileFormat(file).getType();

    	} catch (Exception e) {
    		e.printStackTrace();
    	}
		delegate.setupStream(audioInputStream);
	}

	/**
	 * Inits the.
	 */
	public void init() {
		delegate.init();
	}
	

	/**
	 * Shutdown stream.
	 */
	public void shutdownStream() {
		delegate.shutdownStream();
	}
	
	
	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#startAudioTransfer(long, com.spokentech.speechdown.client.SpeechEventListener)
	 */
	public void startAudioTransfer(long timeout, SpeechEventListener listener) throws InstantiationException, IOException {
		delegate.startAudioTransfer(timeout,listener);
	}

	
	
    /* (non-Javadoc)
     * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#stopAudioTransfer()
     */
    public synchronized void stopAudioTransfer() {
    	delegate.stopAudioTransfer();
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
    	return delegate.startInputTimers(noInputTimeout);
    }


	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#getFormat1()
	 */
	@Override
    public AudioFormat getFormat1() {
	    return delegate.getFormat1();
    }


	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#getFormat2()
	 */
	@Override
    public javax.media.format.AudioFormat getFormat2() {

	    return delegate.getFormat2();
    }


	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#getMimeType()
	 */
	@Override
    public String getMimeType() {
	    return delegate.getMimeType();
    }
	

    /**
     * Sets the mime type.
     * 
     * @param mimeType the new mime type
     */
    public void setMimeType(String mimeType) {
    	delegate.setMimeType(mimeType);
    }


	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#getInputStream()
	 */
	@Override
    public InputStream getInputStream() {
	    return delegate.getInputStream();
    }

	
}
