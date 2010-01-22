package com.spokentech.speechdown.client.endpoint;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat.Type;

import org.apache.log4j.Logger;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import com.spokentech.speechdown.client.util.AFormat;
import com.spokentech.speechdown.client.util.FormatUtils;
import com.spokentech.speechdown.common.SpeechEventListener;



public class FileS4EndPointingInputStream2 extends EndPointingInputStreamBase  {
	

	private static Logger _logger = Logger.getLogger(FileS4EndPointingInputStream2.class);

	private int id;

	private ConfigurationManager cm;

	private File  file;
	private Type type;

	private String mimeType;
	
    public FileS4EndPointingInputStream2(EndPointer ep) {
	    super(ep);
	    // TODO Auto-generated constructor stub
    }

	
	/**
     * @return the mimeType
     */
    public String getMimeType() {
    	return mimeType;
    }


	/**
     * @param mimeType the mimeType to set
     */
    public void setMimeType(String mimeType) {
    	this.mimeType = mimeType;
    }


	public void setupStream(File file) {
		_logger.debug("Setting up the file");
		this.file = file;

    	try {
    		stream = AudioSystem.getAudioInputStream(file);
    		type = AudioSystem.getAudioFileFormat(file).getType();
	        AudioFormat f = ((AudioInputStream) stream).getFormat();
	        format = FormatUtils.covertToNeutral(f);

    	} catch (Exception e) {
    		e.printStackTrace();
    	}
        setupPipedStream();
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


	@Override
    public AFormat getFormat() {
		return FormatUtils.covertToNeutral(((AudioInputStream) stream).getFormat());
    }



	
}
