package com.spokentech.speechdown.server.recog;

import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import com.spokentech.speechdown.common.RecognitionResult;
import com.spokentech.speechdown.server.domain.HttpRequest;

public interface RecEngine {

	public RecognitionResult recognize(AudioInputStream as, String grammar);
	
	public RecognitionResult recognize(InputStream as, String mimeType, String grammar, int sampleRate, boolean bigEndian, int bytesPerValue, AudioFormat.Encoding encoding, boolean doEndpointing, boolean cmnBatch, HttpRequest hr);

	public RecognitionResult recognize(InputStream as, String mimeType, int sampleRate, boolean bigEndian, int bytesPerValue, AudioFormat.Encoding encoding, boolean doEndpointing, boolean cmnBatch, HttpRequest hr);

	public String transcribe(InputStream as, String mimeType, int sampleRate, boolean bigEndian, int bytesPerValue, AudioFormat.Encoding encoding, PrintWriter out, HttpServletResponse response, HttpRequest hr);

	
}