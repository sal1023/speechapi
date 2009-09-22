package com.spokentech.speechdown.client.endpoint;

import java.io.IOException;
import java.io.InputStream;


import com.spokentech.speechdown.client.SpeechEventListener;

public interface EndPointingInputStream {

	public static final short WAITING_FOR_SPEECH = 0;

	public static final short SPEECH_IN_PROGRESS = 1;

	public static final short COMPLETE = 2;

	public javax.sound.sampled.AudioFormat getFormat1();
	public javax.media.format.AudioFormat getFormat2();
	
	public void startAudioTransfer(long timeout, SpeechEventListener listener) throws InstantiationException, IOException;

	public void stopAudioTransfer();

	/**
	 * Starts the input timers which trigger no-input-timeout if speech has not started after the specified time.
	 * @param noInputTimeout the amount of time to wait, in milliseconds, before triggering a no-input-timeout. 
	 * @return {@code true} if input timers were started or {@code false} if speech has already started.
	 * @throws IllegalStateException if recognition is not in progress or if the input timers have already been started.
	 */
	public boolean startInputTimers(long noInputTimeout) throws IllegalStateException;
	
	public InputStream getInputStream();
	
	public String getMimeType();

}