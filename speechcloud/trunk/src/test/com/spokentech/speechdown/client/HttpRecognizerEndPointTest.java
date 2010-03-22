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
import com.spokentech.speechdown.client.endpoint.EndPointer;
import com.spokentech.speechdown.client.endpoint.ExternalTriggerEndPointer;
import com.spokentech.speechdown.client.endpoint.FileS4EndPointingInputStream2;
import com.spokentech.speechdown.client.endpoint.S4EndPointer;
import com.spokentech.speechdown.client.endpoint.StreamEndPointingInputStream;
import com.spokentech.speechdown.client.endpoint.JavaSoundStreamS4EndPointingInputStream;
import com.spokentech.speechdown.client.endpoint.StreamEndPointingInputStream;
import com.spokentech.speechdown.client.exceptions.HttpRecognizerException;
import com.spokentech.speechdown.client.util.FormatUtils;
import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.RecognitionResult;
import com.spokentech.speechdown.common.SpeechEventListener;


import junit.framework.TestCase;

public class HttpRecognizerEndPointTest extends TestCase {

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
	            if (rr.isCflag())
	            	System.out.println("confidence is "+rr.getConfidence());
            }
	
		}


		private static Logger _logger = Logger.getLogger(HttpRecognizerEndPointTest.class);
	    public static final String CRLF = "\r\n";
	    
	   
	    
	    //private static String service = "http://ec2-174-129-20-250.compute-1.amazonaws.com/speechcloud/SpeechUploadServlet";    
	    private static String service = "http://localhost:8090/speechcloud/SpeechUploadServlet";    
	    //private static String service = "http://spokentech.net:/speechcloud/SpeechUploadServlet";   
	    
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
		URL grammarUrl2 = null;
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

	 
    
	    
	    public void testRecognizeFileS4EPGrammar() {
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
            
	    public void testRecognizeFileS4EPGrammarAsynch() {
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
	    
	    public void testRecognizeFileS4EPGrammarAsynchWithCancel() {
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
	    	String id = null;
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
            
            
            _logger.info("called asynch method. sleeping for 2 secs");
            try {
	            Thread.sleep(2000);
            } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
            recog.cancel(id);
            
	    }
	    
	    public void testRecognizeFileS4EPLm() {
	    	long timeout = 10000;       	
	    	S4EndPointer ep = new S4EndPointer();
	    	FileS4EndPointingInputStream2 epStream = new FileS4EndPointingInputStream2(ep);

	    	epStream.setMimeType(s4audio);
	    	epStream.setupStream(soundFile2);
	
            
	    	RecognitionResult r = null;
	    	boolean lmflg = true;
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
            System.out.println("ENDPOINT TEST: lm  result: "+r.getText());
            if (r.isCflag())
            	System.out.println("confidence is "+r.getConfidence());
	    }

	    
	    
	    
	    public void testRecognizeStreamS4JavaSoundEP() {
	
	    	System.out.println("Starting S4 EP Stream Test ...");

	    	// get an audio stream for the test from a file
	    	AudioInputStream	audioInputStream = null;
	    	Type type = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile2);
	    		type = AudioSystem.getAudioFileFormat(soundFile2).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	
	    	//run the test
	    	long timeout = 10000;
	    	S4EndPointer ep = new S4EndPointer();
	    	JavaSoundStreamS4EndPointingInputStream epStream = new JavaSoundStreamS4EndPointingInputStream(ep);
	    	epStream.setMimeType(s4audio);
	    	epStream.setupStream(audioInputStream);


	    	RecognitionResult r = null;
	    	boolean lmflg = true;
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
	    	
           System.out.println("lm result: "+r.getText());
	    }
	    
	    public void testRecognizeStreamS4EP() {
	    	
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
           
       	    // get an audio stream for the test from a file
            audioInputStream = null;
	    	type = null;

	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile2);
	    		type = AudioSystem.getAudioFileFormat(soundFile2).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	        AudioFormat f = audioInputStream.getFormat();
	        AFormat format = FormatUtils.covertToNeutral(f);
	       
	    	//run the test
	    	long t1 = System.nanoTime();
	    	S4EndPointer ep = new S4EndPointer();
	    	StreamEndPointingInputStream epStream = new StreamEndPointingInputStream(ep);
	    	epStream.setMimeType(s4audio);
	    	epStream.setupStream(audioInputStream, format);

	    	long t2 = System.nanoTime();
	    	long t3 = (t2-t1)/1000000;
	        _logger.info("took "+t3+ "ms. to create s4 endpointing stream");
	    	
	    	
	    	long timeout = 10000;
	    	boolean lmflg = false;
	    	boolean batchFlag = false;
	    	RecognitionResult r = null;
	        try {
	        	r = recog.recognize(grammarUrl,  epStream,  lmflg, batchFlag, timeout) ;
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
	    	ExternalTriggerEndPointer ep = new ExternalTriggerEndPointer();
	    	StreamEndPointingInputStream epStream = new StreamEndPointingInputStream(ep);
	    	epStream.setMimeType(wav);
	    	epStream.setupStream(audioInputStream,format);
	    	long t2 = System.nanoTime();
	    	long t3 = (t2-t1)/1000000;
	        _logger.info("took "+t3+ "ms. to create x trig endpointing stream");
	    	
	    	Listener l = new Listener();
	    	boolean lmflg = false;
	    	boolean batchFlag = true;
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
	    	
            ep.triggerStart();
           //while ( ep.triggerStart() <0) {
        	//   _logger.info("not setup yet");
           //}
            
            _logger.info("called asynch method. sleeping for 2 secs");
            try {
	            Thread.sleep(5000);
            } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
            ep.triggerEnd();
    	
	    }
	    
	    
	    public void testBasicStreamInputExternalTriggerEPLMSynch() {
	    	    	
	    	System.out.println("Starting EP extrernal trigger  Test ...");

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
	    	ExternalTriggerEndPointer ep = new ExternalTriggerEndPointer();
	    	StreamEndPointingInputStream epStream = new StreamEndPointingInputStream(ep);
	    	epStream.setMimeType(wav);
	    	epStream.setupStream(audioInputStream,format);
	    	long t2 = System.nanoTime();
	    	long t3 = (t2-t1)/1000000;
	        _logger.info("took "+t3+ "ms. to create x trig endpointing stream");
	    	
	        Thread t = new Thread(new Triggerer(ep));
	        t.start();
	        
	    	Listener l = new Listener();
	    	boolean lmflg = true;
	    	boolean batchFlag = true;
	    	RecognitionResult result = null;
	        try {	            
	            result = recog.recognize(grammarUrl2,  epStream,  lmflg,  batchFlag, timeout,l) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
	    	

            _logger.info("recognition complete: "+result.getText());
            if (result.isCflag())
            	System.out.println("confidence is "+result.getConfidence());
    	
	    }
	    

	    public void testBasicStreamInputExternalTriggerEPLM() {
	    
	    	
	    	System.out.println("Starting EP extrernal trigger  Test ...");
	       	recog.enableAsynchMode(2);
		    
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
	    	ExternalTriggerEndPointer ep = new ExternalTriggerEndPointer();
	    	StreamEndPointingInputStream epStream = new StreamEndPointingInputStream(ep);
	    	epStream.setMimeType(wav);
	    	epStream.setupStream(audioInputStream,format);
	    	long t2 = System.nanoTime();
	    	long t3 = (t2-t1)/1000000;
	        _logger.info("took "+t3+ "ms. to create x trig endpointing stream");
	    	
	    	Listener l = new Listener();
	    	boolean lmflg = true;
	    	boolean batchFlag = true;
	    	String id;
	        try {	            
	            id = recog.recognizeAsynch(grammarUrl2,  epStream,  lmflg,  batchFlag, timeout,l) ;
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
	    	
            ep.triggerStart();
           //while ( ep.triggerStart() <0) {
        	//   _logger.info("not setup yet");
           //}
            
            _logger.info("called asynch method. sleeping for 2 secs");
            try {
	            Thread.sleep(5000);
            } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
            ep.triggerEnd();
    	
	    }



  public void testExternalTriggerWithoutEndTrigger() {
	    
	    	
	    	System.out.println("Starting EP extrernal trigger  Test ...");
	       	recog.enableAsynchMode(2);
		    
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
	    	
	    	ExternalTriggerEndPointer ep = new ExternalTriggerEndPointer();
	    	StreamEndPointingInputStream epStream = new StreamEndPointingInputStream(ep);
	    	epStream.setMimeType(wav);
	    	epStream.setupStream(audioInputStream,format);
	    	
	    	
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
	    	
            ep.triggerStart();
           //while ( ep.triggerStart() <0) {
        	//   _logger.info("not setup yet");
           //}
            
            _logger.info("called asynch method. sleeping for 2 secs");
            try {
	            Thread.sleep(5000);
            } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
            //ep.triggerEnd();
    	    
	    }
  
  		private static class Triggerer implements Runnable {
  			
  
  			public Triggerer(ExternalTriggerEndPointer ep) {
	            super();
	            this.ep = ep;
            }

  			ExternalTriggerEndPointer ep = null;
  			
  			public void run() {
  				
  				ep.triggerStart();
     	
                  //Pause for 5 seconds
                  try {
	                Thread.sleep(5000);
                } catch (InterruptedException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                }
                
                ep.triggerEnd();


  			}
		}

	    
}



