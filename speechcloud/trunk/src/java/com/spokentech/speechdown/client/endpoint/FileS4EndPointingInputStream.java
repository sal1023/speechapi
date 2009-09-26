package com.spokentech.speechdown.client.endpoint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat.Type;
import org.apache.log4j.Logger;

import com.spokentech.speechdown.client.SpeechEventListener;

public class FileS4EndPointingInputStream implements EndPointingInputStream {
	
    private static Logger _logger = Logger.getLogger(FileS4EndPointingInputStream.class);

    StreamS4EndPointingInputStream delegate;
    
	public FileS4EndPointingInputStream() {
	    super();
	    delegate = new StreamS4EndPointingInputStream();
    }
	

    /**
     * @return the s4ConfigFile
     */
    public String getS4ConfigFile() {
    	return delegate.getS4ConfigFile();
    }


	/**
     * @param configFile the s4ConfigFile to set
     */
    public void setS4ConfigFile(String configFile) {
    	delegate.setS4ConfigFile(configFile);
    }



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

	public void init() {
		delegate.init();
	}
	

	public void shutdownStream() {
		delegate.shutdownStream();
	}
	
	
	public void startAudioTransfer(long timeout, SpeechEventListener listener) throws InstantiationException, IOException {
		delegate.startAudioTransfer(timeout,listener);
	}

	
	
    public synchronized void stopAudioTransfer() {
    	delegate.stopAudioTransfer();
    }
	
	
    /**
     * Starts the input timers which trigger no-input-timeout if speech has not started after the specified time.
     * @param noInputTimeout the amount of time to wait, in milliseconds, before triggering a no-input-timeout. 
     * @return {@code true} if input timers were started or {@code false} if speech has already started.
     * @throws IllegalStateException if recognition is not in progress or if the input timers have already been started.
     */
    public synchronized boolean startInputTimers(long noInputTimeout) throws IllegalStateException {
    	return delegate.startInputTimers(noInputTimeout);
    }


	@Override
    public AudioFormat getFormat1() {
	    return delegate.getFormat1();
    }


	@Override
    public javax.media.format.AudioFormat getFormat2() {

	    return delegate.getFormat2();
    }


	@Override
    public String getMimeType() {
	    return delegate.getMimeType();
    }
	

    public void setMimeType(String mimeType) {
    	delegate.setMimeType(mimeType);
    }


	@Override
    public InputStream getInputStream() {
	    return delegate.getInputStream();
    }

	
}
