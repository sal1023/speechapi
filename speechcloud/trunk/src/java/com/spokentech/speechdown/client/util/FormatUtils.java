package com.spokentech.speechdown.client.util;

import javax.media.Format;
import javax.sound.sampled.AudioFormat;

public class FormatUtils {

	
	public static AFormat covertToNeutral(AudioFormat javaSoundFormat) {

		boolean signed;
    	if (javaSoundFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {
    		signed = true;
    	} else if (javaSoundFormat.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
    		signed = true;
    	} else if (javaSoundFormat.getEncoding() == AudioFormat.Encoding.ALAW) {
    		signed = true;
    	} else if (javaSoundFormat.getEncoding() == AudioFormat.Encoding.ULAW) {
    		signed = true;
    	} else {
        	signed = true;
        }
    	
    	
		AFormat f = new AFormat(javaSoundFormat.getEncoding().toString(), 
				                javaSoundFormat.getSampleRate(),
				                javaSoundFormat.getSampleSizeInBits(),
				                javaSoundFormat.getChannels(),
				                javaSoundFormat.isBigEndian(),
				                signed,
				                javaSoundFormat.getFrameSize(),
				                javaSoundFormat.getFrameRate());
		return f;
		
	}
	
    
	   public  static javax.media.format.AudioFormat convertToJmf(AudioFormat af) {
	    	
	    	String encoding;
	    	double sampleRate;
	    	int sampleSizeInBits;
	    	int channels;
	    	int endian;
	    	int signed;
	    	int frameSizeInBytes;
	    	double frameRate;
	    	
	    	if (af.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {
	    		encoding = javax.media.format.AudioFormat.LINEAR;
	    		signed = javax.media.format.AudioFormat.SIGNED;
	    	} else if (af.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
	    		encoding = javax.media.format.AudioFormat.LINEAR;
	    		signed = javax.media.format.AudioFormat.UNSIGNED;
	    	} else if (af.getEncoding() == AudioFormat.Encoding.ALAW) {
	    		encoding = javax.media.format.AudioFormat.ALAW;
	    		signed = javax.media.format.AudioFormat.UNSIGNED;
	    	} else if (af.getEncoding() == AudioFormat.Encoding.ULAW) {
	    		encoding = javax.media.format.AudioFormat.ULAW;
	    		signed = javax.media.format.AudioFormat.UNSIGNED;
	    	} else {
	    		encoding = javax.media.format.AudioFormat.LINEAR;
	    		signed = javax.media.format.AudioFormat.SIGNED;
	    	}
	    	
	    	sampleRate = af.getSampleRate();
	    	sampleSizeInBits = af.getSampleSizeInBits();
	    	channels = af.getChannels();
	    	if (af.isBigEndian() ) {
	    		endian = javax.media.format.AudioFormat.BIG_ENDIAN;
	    	} else {
	    		endian = javax.media.format.AudioFormat.LITTLE_ENDIAN;
	    	}
	    	frameSizeInBytes = 8 * af.getFrameSize();
	    	
	    	frameRate = af.getFrameRate();
	    	
	    	javax.media.format.AudioFormat audioFormat = new javax.media.format.AudioFormat(encoding,
	    			sampleRate,
	    			sampleSizeInBits,
	    			channels,
	    			endian,
	    			signed,
	    			frameSizeInBytes,
	    			frameRate,
					Format.byteArray);
			return audioFormat;

	    }

}
