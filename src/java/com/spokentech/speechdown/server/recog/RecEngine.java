package com.spokentech.speechdown.server.recog;

import javax.sound.sampled.AudioInputStream;

import com.spokentech.speechdown.common.RecognitionResult;

public interface RecEngine {

	public RecognitionResult recognize(AudioInputStream as, String grammar);

}