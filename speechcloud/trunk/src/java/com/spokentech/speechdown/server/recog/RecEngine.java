package com.spokentech.speechdown.server.recog;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import com.spokentech.speechdown.common.RecognitionResult;

public interface RecEngine {

	public RecognitionResult recognize(AudioInputStream as, String grammar);
	
	public RecognitionResult recognize(InputStream as, String mimeType, String grammar, int sampleRate, boolean bigEndian, int bytesPerValue, AudioFormat.Encoding encoding);

	public String recognize(InputStream as, String mimeType, int sampleRate, boolean bigEndian, int bytesPerValue, AudioFormat.Encoding encoding);

	public String transcribe(InputStream as, String mimeType, int sampleRate, boolean bigEndian, int bytesPerValue, AudioFormat.Encoding encoding, PrintWriter out);

	
}