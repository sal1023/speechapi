package com.spokentech.speechdown.server.ws;


import javax.jws.WebService;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.ws.WebServiceException;

import javax.xml.ws.Holder;

import java.awt.Image;

import java.net.MalformedURLException;


import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.transform.Source;

import com.sun.xml.ws.developer.StreamingDataHandler; 
import com.sun.xml.ws.developer.StreamingAttachment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.ws.BindingType;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.log4j.Logger;

/*<element name="RecognizeRequestAttachType" type="s:RecRequestAttachType"/>
<element name="RecognizeResponseType" type="s:RecResponseType"/>
<element name="SynthesizeRequestType" type="xsd:string"/>
<element name="SynthesizeResponseAttachType" type="s:SynthResponseType"/>
<element name="SynthesizeResponseLinkType" type="xsd:string"/>*/

import com.spokentech.speechdown.common.RecognitionResult;
import com.spokentech.speechdown.server.RecognizerService;
import com.spokentech.speechdown.server.SynthesizerService;
import com.spokentech.speechdown.server.recog.AudioStreamDataSource;
import com.spokentech.speechdown.types.RecRequestAttachType;
import com.spokentech.speechdown.types.RecResponseType;
import com.spokentech.speechdown.types.SynthResponseAttachType;
import com.spokentech.speechdown.SpeechAttachPortType; 


@WebService(
    portName="SpeechAttachPort",
    targetNamespace="http://spokentech.com/speechdown",
    serviceName="SpeechAttachService",
    wsdlLocation="/WEB-INF/wsdl/SpeechAttach.wsdl",
    endpointInterface="com.spokentech.speechdown.SpeechAttachPortType")

@BindingType(SOAPBinding.SOAP11HTTP_MTOM_BINDING)
@StreamingAttachment(parseEagerly=true, memoryThreshold=4000000L)
public class SpeechAttachPortImpl implements SpeechAttachPortType {

    private static Logger _logger = Logger.getLogger(SpeechAttachPortType.class);
	private RecognizerService recognizerService;
	private SynthesizerService synthesizerService;
	private int counter = 1;
	
	public RecResponseType recognize(RecRequestAttachType request) {
		_logger.info("Got a (attach) recognize request.  grammar: "+request.getGrammar());
	
		
		RecognitionResult result = null;
		try {
		   result = recognizerService.Recognize(request.getAudio(), request.getGrammar());
		}catch (Exception e) {
			e.printStackTrace();
		}
		   
		RecResponseType response = new RecResponseType();
		response.setOog(result.isOutOfGrammar());
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
		 
        File ttsFile = synthesizerService.ttsFile(prompt);
       
        DataHandler dh = new DataHandler(new FileDataSource(ttsFile));
        
        SynthResponseAttachType result= new SynthResponseAttachType();
        result.setAudio(dh);
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


