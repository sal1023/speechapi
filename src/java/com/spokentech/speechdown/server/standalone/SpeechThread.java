/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.standalone;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.Utterance.OutputFormat;
import com.spokentech.speechdown.server.domain.SpeechRequestDTO;
import com.spokentech.speechdown.server.recog.RecEngine;

public class SpeechThread implements Runnable {
    private static Logger logger = Logger.getLogger(SpeechThread.class);

	private boolean shutdown = false;
	
	private RecEngine recEngine = null;
	private SpeechWorker worker;

	private String outputFormat;

	public SpeechThread(RecEngine recEngine, SpeechWorker worker, String outputFormat) {
		this.recEngine = recEngine;
		this.worker = worker;
		this.outputFormat = outputFormat;
	}


	@Override
	public void run() {
		
		String transcription;
		
		while (!shutdown) {		
			SpeechJob job = worker.getJob();
					
			InputStream as = null;
            try {
	            as = job.getUrl().openStream();
            } catch (IOException e1) {
	            // TODO Auto-generated catch block
	            e1.printStackTrace();
            }
			String mimeType = null;
			AFormat af = null;
			OutputFormat outMode =  OutputFormat.valueOf(outputFormat); //OutputFormat.json;
			PrintWriter out = null;
			HttpServletResponse response = null;
			SpeechRequestDTO hr = null;
			try {
				long start = System.nanoTime();

		        String amId = "default";
				String lmId = "default";
				String dictId = "defualt";
				transcription = recEngine.transcribe( as,  mimeType,  af,   outMode,  out, response,  amId, lmId, dictId, hr);
				long stop = System.nanoTime();
				long wall = (stop - start)/1000000;
				
				logger.info("Job "+job.getId()+ " id, Wall time was: "+wall);
				logger.info(transcription);
			    worker.completeSuccessfulJob(job,transcription);
			   
			}catch (Exception e) {
				e.printStackTrace();
				logger.warn("Error processing transcription request");
				worker.completeFailedJob(job);
			}
			
		}

	}

}