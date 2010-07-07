/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.standalone;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;

public class FileSpeechWorker implements SpeechWorker {
	
    private static Logger logger = Logger.getLogger(FileSpeechWorker.class);
    private static int count =0;
	private String fileName;
	private BufferedReader inputStream = null;
	 
	public FileSpeechWorker(String fileName) {
	    super();
	    this.fileName = fileName;
	    init();
    }
	
	public void init() {

	}

	@Override
	public SpeechJob getJob() {

		SpeechJob job = null;
        String l;
        try {
        	l = getNext();
	        if (l != null) {
	            logger.info("got a job from queue: "+l);

	            String delims = "[,]";
	            String[] tokens = l.split(delims);
	            if (tokens.length == 4) {
	            	job = new SpeechJob();
	            	job.setId(tokens[0]);
	            	job.setPriority(Integer.parseInt(tokens[1]));
	            	job.setRequestor(tokens[2]);
	            	String urlName = tokens[3];
	            	URL url = new URL(urlName);
	            	job.setUrl(url);            	
	            }

	            
	        } else {
	            logger.info("No more jobs in file queue ");
	        }
	        inputStream.close();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }

		return job;
	}

	private synchronized String getNext() throws FileNotFoundException, IOException {
	    String l;
	    FileReader fr = new FileReader(fileName);
	    inputStream =  new BufferedReader(fr);
	    int loopCount=0;
	    while (((l = inputStream.readLine()) != null) &&(loopCount<count)) {
	    	logger.info(count+" "+loopCount);
	    	logger.info(l);
	    	loopCount = loopCount+1;
	    }
	    
	    logger.info("Taking job # "+count);
	    count++;
	    return l;
    }
	

	@Override
	public void completeFailedJob(SpeechJob job) {
		// TODO Auto-generated method stub

	}

	@Override
	public void completeSuccessfulJob(SpeechJob job, String transcription) {
		logger.info("************* JOB COMPLETE*******************\n"+job.getUrl().toString()+"\n*************************************************");

	}



}
