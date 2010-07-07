/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.common;

public interface SpeechEventListener {
	
	public void speechStarted();
	public void speechEnded();
	public void noInputTimeout();
	public void recognitionComplete(Utterance utterance);

}
