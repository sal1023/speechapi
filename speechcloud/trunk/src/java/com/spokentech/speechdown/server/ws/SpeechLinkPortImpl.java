
package com.spokentech.speechdown.server.ws;

import java.io.File;

import javax.jws.WebService;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.server.RecognizerService;
import com.spokentech.speechdown.server.PoolingSynthesizerService;
import com.spokentech.speechdown.server.RecRequestLinkType;
import com.spokentech.speechdown.server.RecResponseType;
import com.spokentech.speechdown.server.SpeechLinkPortType; 
import com.spokentech.speechdown.server.SynthesizerService;
import com.spokentech.speechdown.server.tts.MarySynthesizerService;


@WebService(
    portName="SpeechLinkPort",
    targetNamespace="http://spokentech.com/speechcloud",
    serviceName="SpeechLinkService",
    wsdlLocation="/WEB-INF/wsdl/SpeechLink.wsdl",
    endpointInterface="com.spokentech.speechdown.server.SpeechLinkPortType")
public class SpeechLinkPortImpl implements SpeechLinkPortType{

	   /** The _logger. */
    private static Logger _logger = Logger.getLogger(SpeechLinkPortImpl.class);
 
	/**
     * @return the recognizerService
     */

    public RecognizerService getRecognizerService() {
    	return recognizerService;
    }



	/**
     * @param recognizerService the recognizerService to set
     */

    public void setRecognizerService(RecognizerService recognizerService) {
    	this.recognizerService = recognizerService;
    }



	/**
     * @return the synthesizerService
     */
    public SynthesizerService getSynthesizerService() {
    	return synthesizerService;
    }



	/**
     * @param synthesizerService the synthesizerService to set
     */
    public void setSynthesizerService(SynthesizerService synthesizerService) {
    	this.synthesizerService = synthesizerService;
    }



	private RecognizerService recognizerService;
	private SynthesizerService synthesizerService;
	
    
	
	public RecResponseType recognize(RecRequestLinkType request) {
		
		_logger.info("Got a (link) recognize request: "+request.toString());
        
		RecResponseType result = new RecResponseType();
		//if (audio == null || grammar == null) {
        //    throw new RecognitionException("Null url passed in as arguments not allowed!",
        //        "audio: " + audio + ", grammar:" + grammar);
        //}
        
        result.setText("hello!");
        result.setOog(false);
        
        return result;
        
	}

	public String synthesize(String prompt) {
		_logger.info("Got a (link) synthesize request: "+prompt);
		 
		//TODO: add format and filetype to the wsdl
	    int sampleRate = 8000;
	    boolean signed = true;
	    boolean bigEndian = true;
	    int channels = 1;
	    int sampleSizeInBits = 16;
		AudioFormat format = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);
		
        String result=synthesizerService.ttsURL(prompt,format,"audio/x-au");
        return result;

	}

}
