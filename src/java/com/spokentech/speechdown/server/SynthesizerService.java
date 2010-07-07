/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server;

import java.io.File;
import java.io.OutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.spokentech.speechdown.server.domain.SpeechRequestDTO;

public interface SynthesizerService {

	public String ttsURL(String text, AudioFormat format, String mime);

	public File ttsFile(String text, AudioFormat format, String mime);

	public void streamTTS(String text, AudioFormat format, String mime, String voice,
	        OutputStream out, SpeechRequestDTO hr) throws UnsupportedAudioFileException;

}