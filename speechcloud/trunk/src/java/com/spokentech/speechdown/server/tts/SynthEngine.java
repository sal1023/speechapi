package com.spokentech.speechdown.server.tts;

import java.io.File;

public interface SynthEngine {

	/**
	 * Generates a prompt file containing the specified speech text.
	 * @param text textual content of prompt file.
	 * @param dir directory in which to save the generated prompt file.
	 * @return the generated prompt file.
	 * @throws IllegalArgumentException if the directory specified is not a directory.
	 */
	public File generateAudio(String text, File dir) throws IllegalArgumentException;

}