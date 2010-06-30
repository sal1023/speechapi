package com.spokentech.speechdown.server.standalone;

public interface SpeechWorker {

	public  void completeFailedJob(SpeechJob job);

	public void completeSuccessfulJob(SpeechJob job, String transcription);

	public SpeechJob getJob();
}
