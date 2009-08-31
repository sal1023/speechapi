package com.spokentech.speechdown.server.tts;

import java.io.File;
import java.io.OutputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;

public interface SynthEngine {

	/**
	 * Generates a prompt file containing the specified speech text.
	 * @param text textual content of prompt file.
	 * @param dir directory in which to save the generated prompt file.
	 * @return the generated prompt file.
	 * @throws IllegalArgumentException if the directory specified is not a directory.
	 */
	public File generateAudio(String text, File dir, AudioFormat format, AudioFileFormat.Type fileType) throws IllegalArgumentException;
	
	public File generateAudio(String text, OutputStream stream, AudioFormat format, AudioFileFormat.Type fileType) throws IllegalArgumentException;

}