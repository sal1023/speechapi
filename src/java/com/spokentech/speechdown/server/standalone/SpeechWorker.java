/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.standalone;

public interface SpeechWorker {

	public  void completeFailedJob(SpeechJob job);

	public void completeSuccessfulJob(SpeechJob job, String transcription);

	public SpeechJob getJob();
}
