/**
 * Interface for imputstream wrapper that does endpointing. It starts propagating the audio once start speech  is detected.
 *  and stops when end speech is detected.
 *
 * @author Spencer Lord {@literal <}<a href="mailto:spencer@spokentech.com">spencer@spokentech.com</a>{@literal >}
 */
package com.spokentech.speechdown.client.endpoint;

import java.io.IOException;
import java.io.InputStream;


import com.spokentech.speechdown.client.SpeechEventListener;

// TODO: Auto-generated Javadoc
/**
 * The Interface EndPointingInputStream.  This is the interface implemented by concrete endpointing streams.
 */
public interface EndPointingInputStream {

	public static final short WAITING_FOR_SPEECH = 0;

	public static final short SPEECH_IN_PROGRESS = 1;

	public static final short COMPLETE = 2;

	/**
	 * Gets the format1.  returns the format of the audio (Format defined in java.sound)
	 * 
	 * @return the format1
	 */
	public javax.sound.sampled.AudioFormat getFormat1();
	
	/**
	 * Gets the format2.   returns the format of the audio (Format defined in JMF)
	 * 
	 * @return the format2
	 */
	public javax.media.format.AudioFormat getFormat2();
	
	/**
	 * Start audio transfer.
	 * 
	 * @param timeout the no speech started timeout in ms.  (if no start speech is detected in thsi time a timeout will occur)
	 * @param listener the listener
	 * 
	 * @throws InstantiationException the instantiation exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void startAudioTransfer(long timeout, SpeechEventListener listener) throws InstantiationException, IOException;

	/**
	 * Stop audio transfer.  This method will force audio transfer to stop.
	 */
	public void stopAudioTransfer();

	/**
	 * Starts the input timers which trigger no-input-timeout if speech has not started after the specified time.
	 * 
	 * @param noInputTimeout the amount of time to wait, in milliseconds, before triggering a no-input-timeout.
	 * 
	 * @return {@code true} if input timers were started or {@code false} if speech has already started.
	 * 
	 * @throws IllegalStateException if recognition is not in progress or if the input timers have already been started.
	 */
	public boolean startInputTimers(long noInputTimeout) throws IllegalStateException;
	
	/**
	 * Gets the input stream.
	 * 
	 * @return the input stream
	 */
	public InputStream getInputStream();
	
	/**
	 * Gets the mime type.
	 * 
	 * @return the mime type
	 */
	public String getMimeType();

}