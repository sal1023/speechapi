/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;


import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;

import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFileFormat.Type;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.client.util.FormatUtils;
import com.spokentech.speechdown.client.util.WerUtils;
import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.SpeechEventListener;
import com.spokentech.speechdown.common.Utterance;
import com.spokentech.speechdown.common.Utterance.OutputFormat;


import junit.framework.TestCase;

public class HttpTranscriptionTest extends TestCase {

		public class Listener implements SpeechEventListener {
	
			@Override
			public void noInputTimeout() {
				// TODO Auto-generated method stub
	            _logger.info("no input timeout event");
			}
	
			@Override
			public void speechEnded() {
				// TODO Auto-generated method stub
	            _logger.info("speceh ended event");
			}
	
			@Override
			public void speechStarted() {
				// TODO Auto-generated method stub
	            _logger.info("speech started event");
			}

			@Override
            public void recognitionComplete(Utterance rr) {
	            // TODO Auto-generated method stub
	            _logger.info("recognition complete: "+rr.getText());
            }
	
		}


		private static Logger _logger = Logger.getLogger(HttpTranscriptionTest.class);
	    public static final String CRLF = "\r\n";
	    
	   
	    //private static String service = "http://ec2-204-236-206-143.compute-1.amazonaws.com/speechcloud/SpeechUploadServlet";    

	    //private static String service = "http://ec2-174-129-20-250.compute-1.amazonaws.com/speechcloud/SpeechUploadServlet";    
	    private static String service = "http://localhost:8090/speechcloud/SpeechUploadServlet";    
	    //private static String service = "http://spokentech.net/speechcloud/SpeechUploadServlet";   
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
	
		
    	File soundFile0 = new File("c:/work/speechcloud/etc/prompts/cubanson.wav");
    	File soundFile4 = new File("c:/work/speechcloud/etc/prompts/fourUtterances2.wav");
     	File soundFile5 = new File("c:/work/speechcloud/etc/prompts/gtd.wav");

     	File textFile5 = new File("c:/work/speechcloud/etc/prompts/gtd.txt");    	
    	
		
	    String wav = "audio/x-wav";
	    String s4feature = "audio/x-s4feature";
	    String s4audio = "audio/x-s4audio";
	    
	    
	    String audioConfigFile="c:/work/speechcloud/etc/sphinxfrontendonly-audio.xml";
	    String featureConfigFile="c:/work/speechcloud/etc/sphinxfrontendonly-feature.xml";
	
		private String devId ="HttpTranscriptionTest";
		private String userId = null;
		private String key = null;

	     protected void setUp() {
		    	recog = new HttpRecognizerJavaSound(devId,key);
		    	recog.setService(service);

		    	try {
		    		grammarUrl = new URL(grammar);
				} catch (MalformedURLException e) {  
			         e.printStackTrace();  
				}		    
	     }

	 
	     
	    public void testTranscribe() {
	    	System.out.println("Starting Transcribe Test ...");
	        
	    	AudioInputStream audioInputStream = null;
	    	Type type = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile5);
	    		type = AudioSystem.getAudioFileFormat(soundFile5).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	String mimeType = null;
			if (type == AudioFileFormat.Type.WAVE) {
				//Always a audio/x-wav
				mimeType = "audio/x-wav";
			} else if (type == AudioFileFormat.Type.AU) {
				mimeType = "audio/x-au";
			} else {
				_logger.warn("unhanlded format type "+type.getExtension());
			}
	    	
	        AudioFormat f = audioInputStream.getFormat();
	        AFormat format = FormatUtils.covertToNeutral(f);
	       	    	

	        StringBuilder contents = new StringBuilder();
			long start = System.nanoTime();
            try {
		    	boolean lmflg = true;
		    	InputStream s = recog.transcribe(userId, audioInputStream, format,mimeType, grammarUrl, lmflg,OutputFormat.json);

                int c;
                while ((c = s.read()) != -1) {
                    System.out.write(c);
  	                contents.append((char)c);
                }

            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
            
			long stop = System.nanoTime();
			long wall = (stop - start)/1000000;
			
			String actual = contents.toString();
			String expected = getContents(textFile5);
			double wer = WerUtils.calcWer(actual,expected);
			System.out.println("WER: "+wer*100.0);
			
	    	System.out.println("FILE TEST: Batch mode, Server Endpointing, LM result: " + " took "+wall+ " ms");   
	    	
	    	
	    }
		    
	     

	    public void testRecognizeFileLmBatch() {
	    	System.out.println("Starting File Test ...");
	        String fname = "c:/work/speechcloud/etc/prompts/get_me_a_stock_quote.wav"; 	
	    	boolean lmflg = true;
	    	boolean doEndpointing = true;
	    	boolean batchMode = true;
			long start = System.nanoTime();
	    	String r = recog.recognize(userId, fname, grammarUrl, lmflg, doEndpointing,batchMode,OutputFormat.json);
			long stop = System.nanoTime();
			long wall = (stop - start)/1000000;
	    	System.out.println("FILE TEST: Batch mode, Server Endpointing, LM result: "+r + " took "+wall+ " ms");    	
	    }


	    private void MicInput() {   	
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
	    	boolean lmflg = true;
	    	boolean batchFlag = false;
	    	String r = recog.recognize(userId, audioLine, grammarUrl, lmflg, batchFlag, doEndpointing,OutputFormat.text);
	    	System.out.println("lm result: "+r);	        
	    	
	        lmflg = false;
	        r = recog.recognize(userId, audioLine, grammarUrl, lmflg,doEndpointing,batchFlag,OutputFormat.text);
	        System.out.println("grammar result: "+r);
	    	
	    }
   
	    static public String getContents(File aFile) {

	        StringBuilder contents = new StringBuilder();
	        
	        try {
	          //use buffering, reading one line at a time
	          //FileReader always assumes default encoding is OK!
	          BufferedReader input =  new BufferedReader(new FileReader(aFile));
	          try {
	            String line = null; //not declared within while loop
	            /*
	            * readLine is a bit quirky :
	            * it returns the content of a line MINUS the newline.
	            * it returns null only for the END of the stream.
	            * it returns an empty String if two newlines appear in a row.
	            */
	            while (( line = input.readLine()) != null){
	              contents.append(line);
	              //contents.append(System.getProperty("line.separator"));
	            }
	          }
	          finally {
	            input.close();
	          }
	        }
	        catch (IOException ex){
	          ex.printStackTrace();
	        }
	        
	        return contents.toString();
	      }

}



