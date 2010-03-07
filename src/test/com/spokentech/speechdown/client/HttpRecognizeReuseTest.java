package com.spokentech.speechdown.client;

import java.io.File;
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

import com.spokentech.speechdown.client.endpoint.AudioStreamEndPointer;
import com.spokentech.speechdown.client.endpoint.ExternalTriggerEndPointer;
import com.spokentech.speechdown.client.endpoint.FileS4EndPointingInputStream2;
import com.spokentech.speechdown.client.endpoint.S4EndPointer;
import com.spokentech.speechdown.client.endpoint.StreamEndPointingInputStream;
import com.spokentech.speechdown.client.endpoint.JavaSoundStreamS4EndPointingInputStream;
import com.spokentech.speechdown.client.endpoint.StreamEndPointingInputStream;
import com.spokentech.speechdown.client.exceptions.AsynchNotEnabledException;
import com.spokentech.speechdown.client.exceptions.HttpRecognizerException;
import com.spokentech.speechdown.client.exceptions.StreamInUseException;
import com.spokentech.speechdown.client.util.AFormat;
import com.spokentech.speechdown.client.util.FormatUtils;
import com.spokentech.speechdown.common.RecognitionResult;
import com.spokentech.speechdown.common.SpeechEventListener;


import junit.framework.TestCase;

public class HttpRecognizeReuseTest extends TestCase {

		public class Listener implements SpeechEventListener {
	
			@Override
			public void noInputTimeout() {
				// TODO Auto-generated method stub
	            _logger.info("no input timeout event");
			}
	
			@Override
			public void speechEnded() {
				// TODO Auto-generated method stub
	            _logger.info("speech ended event");
			}
	
			@Override
			public void speechStarted() {
				// TODO Auto-generated method stub
	            _logger.info("speech started event");
			}

			@Override
            public void recognitionComplete(RecognitionResult rr) {
	            // TODO Auto-generated method stub
	            _logger.info("recognition complete: "+rr.getText());
            }
	
		}


		private static Logger _logger = Logger.getLogger(HttpRecognizeReuseTest.class);
	    public static final String CRLF = "\r\n";
	    
	   
	    
	    //private static String service = "http://ec2-174-129-20-250.compute-1.amazonaws.com/speechcloud/SpeechUploadServlet";    
	    //private static String service = "http://localhost:8090/speechcloud/SpeechUploadServlet";    
	    private static String service = "http://spokentech.net/speechcloud/SpeechUploadServlet";   
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
		File soundFile4 = new File("c:/work/speechcloud/etc/prompts/fourUtterances2.wav");	 	
		
    	File soundFile0 = new File("c:/work/speechcloud/etc/prompts/cubanson.wav");
    	//File soundFile0 = new File("c:/work/speechcloud/etc/prompts/fabfour.wav");
	
		
		
	    String wav = "audio/x-wav";
	    String s4feature = "audio/x-s4feature";
	    String s4audio = "audio/x-s4audio";
	    
	    
	    String audioConfigFile="c:/work/speechcloud/etc/sphinxfrontendonly-audio.xml";
	    String featureConfigFile="c:/work/speechcloud/etc/sphinxfrontendonly-feature.xml";
	
	    

	     protected void setUp() {
	     	    long t1 = System.nanoTime();
		    	recog = new HttpRecognizerJavaSound();
		    	recog.setService(service);
		    	long t2 = System.nanoTime();
		    	long t3 = (t2-t1)/1000000;
		        _logger.info("took "+t3+ "ms. to create recognizer");
		    	
		    	try {
		    		grammarUrl = new URL(grammar);
				} catch (MalformedURLException e) {  
			         e.printStackTrace();  
				}		    
	     }

	 
    
	    
	    public void testRecognizeS4EP() {
	    	System.out.println("Starting File EP Test ...");	
	    	
	    	long timeout = 10000;     
	    	S4EndPointer ep = new S4EndPointer();
	    	FileS4EndPointingInputStream2 epStream = new FileS4EndPointingInputStream2(ep);

	    	epStream.setMimeType(s4audio);
	    	epStream.setupStream(soundFile2);
	 
	    	RecognitionResult r = null;
	    	
	    	boolean lmflg = false;
	    	boolean batchFlag = false;
	        try {	            
	            r = recog.recognize(grammarUrl,  epStream,  lmflg,  batchFlag, timeout) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
	    	
            System.out.println("grammar result: "+r.getText());
	    }
            



	    public void testBasicStreamInputEP() {
	    
	    	
	    	System.out.println("Starting EP Stream Test ...");

	    	// get an audio stream for the test from a file
	    	AudioInputStream	audioInputStream = null;
	    	Type type = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile2);
	    		type = AudioSystem.getAudioFileFormat(soundFile2).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	
    
	        AudioFormat f = audioInputStream.getFormat();
	        AFormat format = FormatUtils.covertToNeutral(f);
	       
	    	
	    	long timeout = 10000;
	    	long t1 = System.nanoTime();
	    	AudioStreamEndPointer ep = new AudioStreamEndPointer();
	    	StreamEndPointingInputStream epStream = new StreamEndPointingInputStream(ep);
	    	epStream.setMimeType(wav);
	    	epStream.setupStream(audioInputStream,format);
	    	long t2 = System.nanoTime();
	    	long t3 = (t2-t1)/1000000;
	        _logger.info("took "+t3+ "ms. to create basic endpointing stream");
	    	

	    	RecognitionResult r = null;
	    	boolean lmflg = false;
	    	boolean batchFlag = true;
	        try {          
	            r = recog.recognize(grammarUrl,  epStream,  lmflg,  batchFlag, timeout) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }    	
            System.out.println("grammar result: "+r.getText());

            
    	
	    }
	    

	    public void testBasicStreamInputExternalTriggerEP() {
	    
	    	
	    	System.out.println("Starting EP extrernal trigger  Test ...");
	       	recog.enableAsynchMode(2);
		    boolean streamInUse = true;
	    	// get an audio stream for the test from a file
	    	AudioInputStream	audioInputStream = null;
	    	Type type = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile4);
	    		type = AudioSystem.getAudioFileFormat(soundFile4).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	
    
	        AudioFormat f = audioInputStream.getFormat();
	        AFormat format = FormatUtils.covertToNeutral(f);
	       
	    	
	    	long timeout = 10000;
	    	
	    	
	    	long t1 = System.nanoTime();
	    	ExternalTriggerEndPointer ep = new ExternalTriggerEndPointer();
	    	StreamEndPointingInputStream epStream = new StreamEndPointingInputStream(ep);
	    	epStream.setMimeType(wav);
	    	epStream.setupStream(audioInputStream,format);
	    	long t2 = System.nanoTime();
	    	long t3 = (t2-t1)/1000000;
	        _logger.info("took "+t3+ "ms. to create x trig endpointing stream");
	    	
	    	Listener l = new Listener();
	    	boolean lmflg = false;
	    	boolean batchFlag = false;
	    	String id;
	    	
            streamInUse = true;
	    	while (streamInUse) {
		    	streamInUse = epStream.inUse();
	    		if (streamInUse) {
		            _logger.info("Stream in use, waiting a sec");
	                try {
	    	            Thread.sleep(1000);
	                } catch (InterruptedException e) {
	    	            // TODO Auto-generated catch block
	    	            e.printStackTrace();
	                }
	    		} else
			        try {	            
			        	_logger.info("Calling rec, epStream.inUse() = "+epStream.inUse());
			            id = recog.recognizeAsynch(grammarUrl,  epStream,  lmflg,  batchFlag, timeout,l) ;
		            } catch (InstantiationException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
		            } catch (IOException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
		            } catch (AsynchNotEnabledException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
		            } catch (StreamInUseException e) {
			            e.printStackTrace();
		            }  
	        }
	    	
            ep.triggerStart();
            
            _logger.info("called asynch method. sleeping for 2 secs");
            try {
	            Thread.sleep(2000);
            } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
            ep.triggerEnd();
            
            streamInUse = true;
	    	while (streamInUse) {
		    	streamInUse = epStream.inUse();
	    		if (streamInUse) {
		            _logger.info("Stream in use, waiting a sec");
	                try {
	    	            Thread.sleep(1000);
	                } catch (InterruptedException e) {
	    	            // TODO Auto-generated catch block
	    	            e.printStackTrace();
	                }
	    		} else
			        try {	            
			        	_logger.info("Calling rec, epStream.inUse() = "+epStream.inUse());
			            id = recog.recognizeAsynch(grammarUrl,  epStream,  lmflg,  batchFlag, timeout,l) ;

		            } catch (InstantiationException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
		            } catch (IOException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
		            } catch (AsynchNotEnabledException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
		            } catch (StreamInUseException e) {
			            e.printStackTrace();
		            }  
	        }
	    	
            ep.triggerStart();
            
            _logger.info("called asynch method. sleeping for 2 secs");
            try {
	            Thread.sleep(2000);
            } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
            ep.triggerEnd();
            
            streamInUse = true;
	    	while (streamInUse) {
		    	streamInUse = epStream.inUse();
	    		if (streamInUse) {
		            _logger.info("Stream in use, waiting a sec");
	                try {
	    	            Thread.sleep(1000);
	                } catch (InterruptedException e) {
	    	            // TODO Auto-generated catch block
	    	            e.printStackTrace();
	                }
	    		} else
			        try {	            
			        	_logger.info("Calling rec, epStream.inUse() = "+epStream.inUse());
			            id = recog.recognizeAsynch(grammarUrl,  epStream,  lmflg,  batchFlag, timeout,l) ;

		            } catch (InstantiationException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
		            } catch (IOException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
		            } catch (AsynchNotEnabledException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
		            } catch (StreamInUseException e) {
			            e.printStackTrace();
		            }  
	        }
	    	
            ep.triggerStart();
            
            _logger.info("called asynch method. sleeping for 2 secs");
            try {
	            Thread.sleep(2000);
            } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
            ep.triggerEnd();
            
            streamInUse = true;
	    	while (streamInUse) {
		    	streamInUse = epStream.inUse();
	    		if (streamInUse) {
		            _logger.info("Stream in use, waiting a sec");
	                try {
	    	            Thread.sleep(1000);
	                } catch (InterruptedException e) {
	    	            // TODO Auto-generated catch block
	    	            e.printStackTrace();
	                }
	    		} else
			        try {	            
			        	_logger.info("Calling rec, epStream.inUse() = "+epStream.inUse());
			            id = recog.recognizeAsynch(grammarUrl,  epStream,  lmflg,  batchFlag, timeout,l) ;

		            } catch (InstantiationException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
		            } catch (IOException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
		            } catch (AsynchNotEnabledException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
		            } catch (StreamInUseException e) {
			            e.printStackTrace();
		            }  
	        }
	    	
            ep.triggerStart();
            
            _logger.info("called asynch method. sleeping for 2 secs");
            try {
	            Thread.sleep(2000);
            } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
            ep.triggerEnd();
    	
	    }

	    public void testRecognizeS4EPAsynch() {
	    	System.out.println("Starting File EP Test ...");	
	    	recog.enableAsynchMode(2);
	    	
	    	long timeout = 10000;
	    	S4EndPointer ep = new S4EndPointer();
	    	FileS4EndPointingInputStream2 epStream = new FileS4EndPointingInputStream2(ep);

	    	epStream.setMimeType(s4audio);
	    	epStream.setupStream(soundFile2);
	 
	    	RecognitionResult r = null;
	    	
	    	Listener l = new Listener();
	    	boolean lmflg = false;
	    	boolean batchFlag = false;
	    	String id;
	        try {	            
	            id = recog.recognizeAsynch(grammarUrl,  epStream,  lmflg,  batchFlag, timeout,l) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (HttpRecognizerException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
	    	
            _logger.info("called asynch method. sleeping for 20 secs");
            try {
	            Thread.sleep(20000);
            } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
            
	    }
	    
	
	    
  

  
	    
}



