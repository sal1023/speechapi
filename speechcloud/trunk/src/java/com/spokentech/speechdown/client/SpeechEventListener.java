package com.spokentech.speechdown.client;

public interface SpeechEventListener {
	
	public void speechStarted();
	public void speechEnded();
	public void noInputTimeout();

}
