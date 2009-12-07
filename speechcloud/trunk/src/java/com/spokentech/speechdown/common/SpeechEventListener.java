package com.spokentech.speechdown.common;

public interface SpeechEventListener {
	
	public void speechStarted();
	public void speechEnded();
	public void noInputTimeout();
	public void recognitionComplete(RecognitionResult rr);

}
