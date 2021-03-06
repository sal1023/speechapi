/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.client.endpoint;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import org.apache.log4j.Logger;

import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;

import com.spokentech.speechdown.client.endpoint.EndPointingInputStreamBase.Listener;
import com.spokentech.speechdown.client.sphinx.Microphone2;
import com.spokentech.speechdown.client.sphinx.SpeechDataStreamer;
import com.spokentech.speechdown.client.util.FormatUtils;
import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.SpeechEventListener;

// TODO: Auto-generated Javadoc
/**
 * The Class MicS4EndPointingInputStream.  This class will read audio from the mic and stream out only the audio between start and end speech.  
 * It use Sphinx4 frontend to do the endpointing. 
 */
public class MicS4EndPointingInputStream extends EndPointingInputStreamBase  {

	private static Logger _logger = Logger.getLogger(MicS4EndPointingInputStream.class);

	private int id;

	private Microphone2 mic;
	
	private AudioFormat desiredFormat;

	private String mimeType;
		
	
	public MicS4EndPointingInputStream(EndPointer ep) {
	    super(ep);

    }


	/**
	 * Gets the mime type.
	 * 
	 * @return the mimeType
	 */
    public String getMimeType() {
    	return mimeType;
    }

	
	/**
     * @return the desiredFormat
     */
    public AudioFormat getDesiredFormat() {
    	return desiredFormat;
    }


	/**
     * @param desiredFormat the desiredFormat to set
     */
    public void setDesiredFormat(AudioFormat desiredFormat) {
    	this.desiredFormat = desiredFormat;
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
	 * Setup stream.
	 */
	public void setupStream( AudioFormat desiredFormat, String mimeType) {
		_logger.debug("Setting up the stream");
	    this.desiredFormat = desiredFormat;
	    this.mimeType = mimeType;
        setupPipedStream();
	}

	/**
	 * Shutdown stream.
	 */
	public void shutdownStream() {
		//TODO:
		_logger.debug("Shutdown stream not implemented!");
	}
	

	
	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#startAudioTransfer(long, com.spokentech.speechdown.client.SpeechEventListener)
	 */
	public void startAudioTransfer(long timeout, SpeechEventListener listener) throws InstantiationException, IOException {
		

		//setup the listener (it is a decorator)
		_listener = new Listener(listener);

		
		int sampleRate = (int)desiredFormat.getSampleRate();
		int sampleSizeInBits = desiredFormat.getSampleSizeInBits();
		int channels = desiredFormat.getChannels();
        boolean bigEndian = desiredFormat.isBigEndian();
        boolean signed = true;
        boolean closeBetweenUtterances = true;
        int msecsPerread = 10;
        boolean keepLastAudio = false;
        String stereoToMono = "average";
        int selectedChannel = 0;
        String selectedMixerIndex = "default";
        
		_logger.debug("FORMAT" + (int)desiredFormat.getSampleRate() +","+desiredFormat.getSampleSizeInBits()+","+desiredFormat.getChannels()+","+ desiredFormat.isBigEndian());
        AudioFormat a  = new AudioFormat((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        _logger.debug("FORMAT: "+a);
        AFormat format = FormatUtils.covertToNeutral(a);
        
    	mic = new Microphone2(sampleRate, sampleSizeInBits, channels , bigEndian , signed, closeBetweenUtterances , msecsPerread,keepLastAudio, stereoToMono ,selectedChannel ,selectedMixerIndex);
        //mic = new Microphone(a);
		//mic.initialize();
    	
    	
    	//TODO:  Don't like this casting to subclass of end pointer.   Only needed now for this mic and rtp version.  
    	//       All other streamers just take a stream in the start method (not a dataprocessor)
	    // start the endpointer thread
     	((S4EndPointer)ep).start((DataProcessor)mic, format, outputStream, _listener);

     	//start the timeout of necessary
		if (timeout > 0)
			startInputTimers(timeout);
		
		//set the state
		_state = WAITING_FOR_SPEECH;
    	

		mic.startRecording();

		
		_state = WAITING_FOR_SPEECH;
	}

	
	
    /* (non-Javadoc)
     * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#stopAudioTransfer()
     */
    public synchronized void stopAudioTransfer() {
    	_logger.debug("Stopping mic");
    	mic.stopRecording();
    	//mic.shutDown();
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



	@Override
    public AFormat getFormat() {
		return FormatUtils.covertToNeutral(desiredFormat);
    }

	
	
}
