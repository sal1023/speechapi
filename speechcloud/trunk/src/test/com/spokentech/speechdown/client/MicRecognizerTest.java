package com.spokentech.speechdown.client;

import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;

import javax.sound.sampled.TargetDataLine;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.common.RecognitionResult;
import com.spokentech.speechdown.common.Utterance.OutputFormat;


import junit.framework.TestCase;

public class MicRecognizerTest extends TestCase {

    private static Logger _logger = Logger.getLogger(MicRecognizerTest.class);
    public static final String CRLF = "\r\n";
    
    

    //private static String service = "http://spokentech.net/speechcloud/SpeechUploadServlet";  
    private static String service = "http://localhost:8090/speechcloud/SpeechUploadServlet";    
    private static AudioFormat desiredFormat;
    private static int sampleRate = 8000;
    private static boolean signed = true;
    private static boolean bigEndian = true;
    private static int channels = 1;
    private static int sampleSizeInBits = 16;
    
    private static int audioBufferSize = 160000;
    private static int msecPerRead = 10;
    private static int frameSizeInBytes;
    
	private String grammar = "file:///work/speechcloud/etc/grammar/example.gram";    
	URL grammarUrl = null;
	HttpRecognizerJavaSound recog;
	
	File soundFile1 = new File("c:/work/speechcloud/etc/prompts/lookupsports.wav");	 	
	File soundFile2 = new File("c:/work/speechcloud/etc/prompts/get_me_a_stock_quote.wav");	 	
	File soundFile3 = new File("c:/work/speechcloud/etc/prompts/i_would_like_sports_news.wav");	 	

	
    String wav = "audio/x-wav";
    String s4feature = "audio/x-s4feature";
    String s4audio = "audio/x-s4audio";
    

	     protected void setUp() {
		    	recog = new HttpRecognizerJavaSound();
		    	recog.setService(service);

		    	try {
		    		grammarUrl = new URL(grammar);
				} catch (MalformedURLException e) {  
			         e.printStackTrace();  
				}		    
	     }

	 
	    
	    public void testMicInput() {
	    	
	        desiredFormat = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	        DataLine.Info info = new DataLine.Info(TargetDataLine.class, desiredFormat);
	        if (!AudioSystem.isLineSupported(info)) {
	            _logger.info(desiredFormat + " not supported");
	            throw new RuntimeException("unsupported audio format");
	        } 
	        System.out.println("Desired format: " + desiredFormat);

	        
	        // Get the audio line from the microphone (java sound api) 
	    	TargetDataLine audioLine = null;
	        try {
	             audioLine = (TargetDataLine) AudioSystem.getLine(info);
	            /* Add a line listener that just traces
	             * the line states.
	             */
	            audioLine.addLineListener(new LineListener() {
	                public void update(LineEvent event) {
	                    _logger.info("line listener " + event);
	                }
	            });
	        } catch (LineUnavailableException e) {
	            _logger.info("microphone unavailable " + e.getMessage());
	        }
	 
	        // open up the line from the mic (javasound)
	        if (audioLine != null) {
	        	if (!audioLine.isOpen()) {
	        		_logger.info("open");
	        		try {
	        			audioLine.open(desiredFormat, audioBufferSize);
	        		} catch (LineUnavailableException e) {
	        			_logger.info("Can't open microphone " + e.getMessage());
	        			e.printStackTrace();
	        		}  
	        	} 
	        } else {
	        	_logger.info("Can't find microphone");
	        	throw new RuntimeException("Can't find microphone");
	        }
	    	boolean doEndpointing = true;
	    	boolean lmflg = false;
	        boolean batchFlag = false;
	    	//RecognitionResult r = recog.recognize(audioLine, grammarUrl, lmflg, doEndpointing);
	    	//System.out.println("lm result: "+r.getText());	        
	    	
	        lmflg = false;
	        batchFlag = false;
	        RecognitionResult r = recog.recognize(audioLine, grammarUrl,lmflg,doEndpointing,batchFlag,OutputFormat.text);
	        System.out.println("grammar result: "+r.getText());
	    	
	    }
	    




	    
}



