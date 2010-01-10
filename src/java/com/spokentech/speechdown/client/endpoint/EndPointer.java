package com.spokentech.speechdown.client.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.spokentech.speechdown.client.util.AFormat;
import com.spokentech.speechdown.common.SpeechEventListener;

public interface EndPointer {

	/** Starts the thread, and waits for recorder to be ready 
	 * @throws IOException */
	public void start(InputStream audioStream,  AFormat format, OutputStream outputStream, SpeechEventListener listener) throws IOException;

	public long triggerStart();

	public void triggerEnd();

	/**
	 * Stops the thread. This method does not return until recording has actually stopped, and all the data has been
	 * read from the audio line.
	 */
	public void stopRecording();

	public boolean requiresServerSideEndPointing();

}