/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.standalone;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

public class AmazonQSpeechWorker implements SpeechWorker {
	
    private static Logger logger = Logger.getLogger(AmazonQSpeechWorker.class);
	private String myQueueUrl;

	//TODO: Use Spring to inject these 
	private AmazonSQS sqs;
	private AmazonS3 s3;
	 
	public AmazonQSpeechWorker(String myQueueUrl) {
	    super();
	    this.myQueueUrl = myQueueUrl;
	    init();
    }
	
	public void init() {
		//TODO: set credentials
		AWSCredentials cred = null;	
		sqs = new AmazonSQSClient(cred);
		s3 = new AmazonS3Client(cred);
	}

	@Override
	public SpeechJob getJob() {
		
		//TODO: get just one request
		//TODO: block until message is returned
		//TODO: move data from queue message to SpeechJob object
		
        System.out.println("Receiving messages from MyQueue.\n");
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(myQueueUrl);
        List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
        for (Message message : messages) {
            System.out.println("  Message");
            System.out.println("    MessageId:     " + message.getMessageId());
            System.out.println("    ReceiptHandle: " + message.getReceiptHandle());
            System.out.println("    MD5OfBody:     " + message.getMD5OfBody());
            System.out.println("    Body:          " + message.getBody());
            for (Entry<String, String> entry : message.getAttributes().entrySet()) {
                System.out.println("  Attribute");
                System.out.println("    Name:  " + entry.getKey());
                System.out.println("    Value: " + entry.getValue());
            }
        }
        System.out.println();
        
        
        

		SpeechJob job = null;
    	job = new SpeechJob();
    	job.setId( messages.get(0).getMessageId());
    	job.setId(messages.get(0).getReceiptHandle());
    	//job.setPriority();
    	//job.setRequestor(tokens[2]);
    	//String urlName = tokens[3];
    	//URL url = new URL(urlName);
    	//job.setUrl(url);
    	
    	
    	String urlStr = null;
    	URL u = null;
        try {
	        u = new URL(urlStr);
        } catch (MalformedURLException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
    	InputStream as = null;
    	if (u.getProtocol().equalsIgnoreCase("s3")) {
    		//TODO: Is this right?  How is a S3 url actually formed (where is key and and bucket?)
    		String key = u.getFile();
    		String bucketName = u.getHost();
    		S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
    		System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
    		as = object.getObjectContent();
    	} else {
    		try {
	            as = job.getUrl().openStream();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
    	}
		
		

     
		return job;
	}

	

	@Override
	public void completeFailedJob(SpeechJob job) {
		// TODO;  Add job back to queue to try again
		logger.warn("Job failed, adding back to queue. "+job.toString());

	}

	@Override
	public void completeSuccessfulJob(SpeechJob job, String transcription) {
		 // Delete a message
        System.out.println("Deleting a message.\n");
        //TODO: maybe need to use receipt handle (separate filed from ID)  why different?
        sqs.deleteMessage(new DeleteMessageRequest(myQueueUrl, job.getId()));
        
		logger.info("************* JOB COMPLETE*******************\n"+job.getUrl().toString()+"\n*************************************************");

	}



}
