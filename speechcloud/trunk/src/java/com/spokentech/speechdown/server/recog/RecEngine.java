package com.spokentech.speechdown.server.recog;

import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioInputStream;

import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.RecognitionResult;
import com.spokentech.speechdown.common.Utterance.OutputFormat;
import com.spokentech.speechdown.server.domain.HttpRequest;

public interface RecEngine {


    
	public RecognitionResult recognize(AudioInputStream as, String grammar);
	
	public RecognitionResult recognize(InputStream as, String mimeType, String grammar, AFormat af, OutputFormat outMode, boolean doEndpointing, boolean cmnBatch, HttpRequest hr);

	public RecognitionResult recognize(InputStream as, String mimeType, AFormat af, OutputFormat outMode, boolean doEndpointing, boolean cmnBatch, HttpRequest hr);

	public String transcribe(InputStream as, String mimeType, AFormat af, OutputFormat outMode, PrintWriter out, HttpServletResponse response, HttpRequest hr);

	
}