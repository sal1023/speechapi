/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.recog;

import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioInputStream;

import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.Utterance;
import com.spokentech.speechdown.common.Utterance.OutputFormat;
import com.spokentech.speechdown.server.domain.SpeechRequestDTO;

public interface RecEngine {


    
	public Utterance recognize(AudioInputStream as, String grammar);
	
	public Utterance recognize(InputStream as, String mimeType, String grammar, AFormat af, OutputFormat outMode, boolean doEndpointing, boolean cmnBatch, SpeechRequestDTO hr);

	public Utterance recognize(InputStream as, String mimeType, AFormat af, OutputFormat outMode, boolean doEndpointing, boolean cmnBatch, SpeechRequestDTO hr);

	public String transcribe(InputStream as, String mimeType, AFormat af, OutputFormat outMode, PrintWriter out, HttpServletResponse response, SpeechRequestDTO hr);

	
}