/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.ws;


import javax.jws.WebService;

import java.io.File;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import com.sun.xml.ws.developer.StreamingAttachment;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.common.Utterance;
import com.spokentech.speechdown.server.RecognizerService;
import com.spokentech.speechdown.server.PoolingSynthesizerService;

import com.spokentech.speechdown.server.RecRequestAttachType;
import com.spokentech.speechdown.server.RecResponseType;
import com.spokentech.speechdown.server.SynthResponseAttachType;
import com.spokentech.speechdown.server.SpeechAttachPortType; 
import com.spokentech.speechdown.server.SynthesizerService;
import com.spokentech.speechdown.server.tts.MarySynthesizerService;


@WebService(
    portName="SpeechAttachPort",
    targetNamespace="http://spokentech.com/speechcloud",
    serviceName="SpeechAttachService",
    wsdlLocation="/WEB-INF/wsdl/SpeechAttach.wsdl",
    endpointInterface="com.spokentech.speechdown.server.SpeechAttachPortType")

@BindingType(SOAPBinding.SOAP11HTTP_MTOM_BINDING)
@StreamingAttachment(parseEagerly=true, memoryThreshold=4000000L)
public class SpeechAttachPortImpl implements SpeechAttachPortType {

    private static Logger _logger = Logger.getLogger(SpeechAttachPortType.class);
	private RecognizerService recognizerService;
	private SynthesizerService synthesizerService;
	private int counter = 1;
	
	public RecResponseType recognize(RecRequestAttachType request) {
		_logger.info("Got a (attach) recognize request.  grammar: "+request.getGrammar());
	
		
		Utterance result = null;
		try {
		   Utterance u = recognizerService.Recognize(request.getAudio(), request.getGrammar());
		}catch (Exception e) {
			e.printStackTrace();
		}
		   
		RecResponseType response = new RecResponseType();
		response.setOog(result.isOog());
		response.setText(result.getText());
		response.setSerialized(result.toString());
		
		/*
		try {
			request.getAudio().getInputStream();
			File f = new File("recog"+(counter++)+".wav");
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));

			BufferedInputStream in = new BufferedInputStream(request.getAudio().getInputStream());

			byte[] buffer = new byte[256]; 
			while (true) { 
				int bytesRead = in.read(buffer);
				//_logger.trace("Read "+ bytesRead + "bytes.");
				if (bytesRead == -1) break; 
				out.write(buffer, 0, bytesRead); 
			} 
			_logger.info("Closing streams");
			in.close(); 
			out.close(); 
		} 
		catch (Exception e) { 
			System.out.println("upload Exception"); e.printStackTrace(); 
			System.out.println(e); 
		} 
		*/

		return response;
	}

	public SynthResponseAttachType synthesize(String prompt) {
		_logger.info("Got a (attach) synthesize request: "+prompt);
		 
		//TODO: add format and filetype to the wsdl
	    int sampleRate = 8000;
	    boolean signed = true;
	    boolean bigEndian = true;
	    int channels = 1;
	    int sampleSizeInBits = 16;
		AudioFormat format = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        //File ttsFile = synthesizerService.ttsFile(prompt,format,AudioFileFormat.Type.AU);
       
        //DataHandler dh = new DataHandler(new FileDataSource(ttsFile));
        
        SynthResponseAttachType result= new SynthResponseAttachType();
        //result.setAudio(dh);
        return result;
	}
	
	
	/**
     * @return the recognizerService
     */
	//@WebMethod (exclude)
    public RecognizerService getRecognizerService() {
    	return recognizerService;
    }



	/**
     * @param recognizerService the recognizerService to set
     */
	//@WebMethod (exclude)
    public void setRecognizerService(RecognizerService recognizerService) {
    	this.recognizerService = recognizerService;
    }



	/**
     * @return the synthesizerService
     */
	//@WebMethod (exclude)
    public SynthesizerService getSynthesizerService() {
    	return synthesizerService;
    }



	/**
     * @param synthesizerService the synthesizerService to set
     */
	//@WebMethod (exclude)
    public void setSynthesizerService(SynthesizerService synthesizerService) {
    	this.synthesizerService = synthesizerService;
    }


	
}


