/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
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

import com.google.gson.Gson;
import com.spokentech.speechdown.client.endpoint.AudioStreamEndPointer;
import com.spokentech.speechdown.client.endpoint.FileS4EndPointingInputStream2;
import com.spokentech.speechdown.client.endpoint.S4EndPointer;
import com.spokentech.speechdown.client.endpoint.StreamEndPointingInputStream;
import com.spokentech.speechdown.client.endpoint.JavaSoundStreamS4EndPointingInputStream;
import com.spokentech.speechdown.client.exceptions.HttpRecognizerException;
import com.spokentech.speechdown.client.util.FormatUtils;
import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.SpeechEventListener;
import com.spokentech.speechdown.common.Utterance;
import com.spokentech.speechdown.common.Utterance.OutputFormat;


import junit.framework.TestCase;

public class HttpRecognizerTest extends TestCase {

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


		private static Logger _logger = Logger.getLogger(HttpRecognizerTest.class);
	    public static final String CRLF = "\r\n";
	    
	    private Gson gson = null;
	    
	    //private static String service = "http://ec2-204-236-206-143.compute-1.amazonaws.com/speechcloud/SpeechUploadServlet";    

	    //private static String service = "http://ec2-174-129-20-250.compute-1.amazonaws.com/speechcloud/SpeechUploadServlet";    
	    //private static String service = "http://localhost:8080/speechcloud/SpeechUploadServlet";    
	    //private static String service = "http://spokentech.net/speechcloud/SpeechUploadServlet";   
  
	    //private static String service = "http://spokentech.net:8000/speechcloud/SpeechUploadServlet";   
	    private static String service = "http://www.speechapi.com:8000/speechcloud/SpeechUploadServlet";   
	    //private static String service = "http://192.168.199.146:8080/speechcloud/SpeechUploadServlet";   

	    
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
		File soundFile4 = new File("c:/work/speechcloud/etc/prompts/fire_and_ice_frost_add_64kb.mp3");	 	
		
		
    	File soundFile0 = new File("c:/work/speechcloud/etc/prompts/cubanson.wav");
    	//File soundFile0 = new File("c:/work/speechcloud/etc/prompts/fabfour.wav");
	
		
		
	    String wav = "audio/x-wav";
	    String s4feature = "audio/x-s4feature";
	    String s4audio = "audio/x-s4audio";
	    
	    
	    String audioConfigFile="c:/work/speechcloud/etc/sphinxfrontendonly-audio.xml";
	    String featureConfigFile="c:/work/speechcloud/etc/sphinxfrontendonly-feature.xml";

		private String devId ="HttpRecognizerTest";
		private String userId = null;
		private String key = null;
	    

	     protected void setUp() {
			    gson = new Gson();
		    	recog = new HttpRecognizerJavaSound(devId, key);
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
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile0);
	    		type = AudioSystem.getAudioFileFormat(soundFile0).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	String mimeType = null;
			//mimeType = "audio/mpeg";
			//if (type == AudioFileFormat.Type.WAVE) {
			//	//Always a audio/x-wav
				mimeType = "audio/x-wav";
			//} else if (type == AudioFileFormat.Type.AU) {
			//	mimeType = "audio/x-au";
			//} else {
			//	_logger.warn("unhanlded format type "+type.getExtension());
			//}
	    	
	        AudioFormat f = audioInputStream.getFormat();
	        AFormat format = FormatUtils.covertToNeutral(f);
	       	    	
	    	
	    	
			long start = System.nanoTime();
            try {
		    	boolean lmflg = true;
		    	InputStream s = recog.transcribe(userId, audioInputStream, format, mimeType, grammarUrl, lmflg,OutputFormat.json);

                int c;
                while ((c = s.read()) != -1) {
                    System.out.write(c);
                }

            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
            
			long stop = System.nanoTime();
			long wall = (stop - start)/1000000;
	    	System.out.println("FILE TEST: Batch mode, Server Endpointing, LM result: " + " took "+wall+ " ms");    	
	    }
		    
	    
	    public void testTranscribeNoFormat() {
	    	System.out.println("Starting Transcribe Test ...");
	        
	    	AudioInputStream audioInputStream = null;
	    	Type type = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile4);
	    		type = AudioSystem.getAudioFileFormat(soundFile4).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	String mimeType = null;

	    	mimeType = "audio/x-wav";
    	
	    	
			long start = System.nanoTime();
            try {
		    	boolean lmflg = true;
		    	InputStream s = recog.transcribe(userId, audioInputStream, null, mimeType, grammarUrl, lmflg,OutputFormat.json);

                int c;
                while ((c = s.read()) != -1) {
                    System.out.write(c);
                }

            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
            
			long stop = System.nanoTime();
			long wall = (stop - start)/1000000;
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
	    
	    public void testRecognizeFileGrammarBatch() {
	    	System.out.println("Starting File Test ...");
	        String fname = "c:/work/speechcloud/etc/prompts/get_me_a_stock_quote.wav"; 	
	    	boolean lmflg = false;
	    	boolean doEndpointing = true;
	    	boolean batchMode = true;
			long start = System.nanoTime();
	    	String r = recog.recognize(userId, fname, grammarUrl, lmflg, doEndpointing,batchMode,OutputFormat.json);
			long stop = System.nanoTime();
			long wall = (stop - start)/1000000;
	    	System.out.println("FILE TEST: Batch mode, Server Endpointing, Grammar result: "+r + " took "+wall+ " ms");
	    	
	        Utterance u = gson.fromJson(r, Utterance.class);
	        System.out.println(u.toString());
	        
	    	
	    }
	    
	    public void testRecognizeFileLmLive() {
	    	System.out.println("Starting File Test ...");
	        String fname = "c:/work/speechcloud/etc/prompts/get_me_a_stock_quote.wav"; 	
	    	boolean lmflg = true;
	    	boolean doEndpointing = true;
	    	boolean batchMode = false;
			long start = System.nanoTime();
	    	String r = recog.recognize(userId, fname, grammarUrl, lmflg, doEndpointing,batchMode,OutputFormat.text);
			long stop = System.nanoTime();
			long wall = (stop - start)/1000000;
	    	System.out.println("FILETEST: Live mode, Server Endpointing, LM result: "+r + " took "+wall+ " ms");    	
	    }
	    
	    public void testRecognizeFileGrammarLive() {
	    	System.out.println("Starting File Test ...");
	        String fname = "c:/work/speechcloud/etc/prompts/get_me_a_stock_quote.wav"; 	
	    	boolean lmflg = false;
	    	boolean doEndpointing = true;
	    	boolean batchMode = false;
			long start = System.nanoTime();
	    	String r = recog.recognize(userId, fname, grammarUrl, lmflg, doEndpointing,batchMode,OutputFormat.text);
			long stop = System.nanoTime();
			long wall = (stop - start)/1000000;
	    	System.out.println("FILE TEST: Live mode, Server endpointing, Grammar result: "+r + " took "+wall+ " ms");
	    }
	     
	    
	    
	    

	    public void testRecognizeAudioStreamLmBatch() {
	    	System.out.println("Starting Stream Test ...");
	    	
	    	// Get a stream for the test
	    	AudioInputStream	audioInputStream = null;
	    	Type type = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile2);
	    		type = AudioSystem.getAudioFileFormat(soundFile2).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}

	    	//run the test
	    	boolean lmflg = true;
	    	boolean doEndpointing = true;
	    	boolean batchMode = true;
			long start = System.nanoTime();

	    	String r = recog.recognize(userId, audioInputStream, type, null, lmflg, doEndpointing,batchMode,OutputFormat.text);
			long stop = System.nanoTime();
			long wall = (stop - start)/1000000;
	    	System.out.println("STREAM TEST: Batch mode, No endpointing, LM result: "+r + " took "+wall+ " ms");	    	
	    	
	    	// Get a stream for the test
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile2);
	    		type = AudioSystem.getAudioFileFormat(soundFile2).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    }
	    	
	    public void testRecognizeAudioStreamGrammarLive() {
	    	System.out.println("Starting Stream Test ...");


	    	// Get a stream for the test
	    	AudioInputStream audioInputStream = null;
	    	Type type = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile2);
	    		type = AudioSystem.getAudioFileFormat(soundFile2).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	
	    	String mimeType =  "audio/x-wav";
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
	       
	    	
	    	//run the test
	    	
	    	boolean doEndpointing = true;
	    	boolean lmflg = false;
	    	boolean batchMode =false;
	    	long start = System.nanoTime();
	    	String r = recog.recognize(userId, audioInputStream, format, mimeType, grammarUrl, lmflg, doEndpointing,batchMode,OutputFormat.text);
	    	long stop = System.nanoTime();
	    	long wall = (stop - start)/1000000;
	    	System.out.println("STREAM TEST: Live mode, No Endpointing Grammar result: "+r + " took "+wall+ " ms");   	

	    }
	  
	    
	    
	    public void testRecognizeStreamGrammarLiveEPWithFormatParamater() {
	    	System.out.println("Starting Input Stream with format parameter Test ...");
	
	    	// Get a stream for the test
	    	AudioInputStream	audioInputStream = null;
	    	Type type = null;
	     
	    	boolean lmflg = false;
	    	boolean doEndpointing = true;
	    	boolean batchMode = false;

	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile2);
	    		type = AudioSystem.getAudioFileFormat(soundFile2).getType();
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
	       
	    	
	    	
	    	long start = System.nanoTime();
	    	String r = recog.recognize(userId, audioInputStream, format, mimeType, grammarUrl, lmflg,doEndpointing, batchMode,OutputFormat.text);
	    	long stop = System.nanoTime();
	    	long wall = (stop - start)/1000000;
	    	System.out.println("STREAM TEST: Live mode, Endpointing, with parameter Grammar result: "+r + " took "+wall+ " ms");   	
    	
	    	
	    }
	    public void testRecognizeStreamLMLiveEPWithFormatParamater() {
	    	System.out.println("Starting Input Stream with format parameter Test ...");
	
	    	
	    	// Get a stream for the test
	    	AudioInputStream	audioInputStream = null;
	    	Type type = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile2);
	    		type = AudioSystem.getAudioFileFormat(soundFile2).getType();
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
	        	        
	    	//run the test
	    	boolean lmflg = true;
	    	boolean doEndpointing = true;
	    	boolean batchMode = false;
	    	long start = System.nanoTime();
	    	String r = recog.recognize(userId, audioInputStream, format, mimeType, null, lmflg,doEndpointing, batchMode,OutputFormat.text);
	    	long stop = System.nanoTime();
	    	long wall = (stop - start)/1000000;
	    	System.out.println("STREAM TEST: Live mode, Endpointing, with parameter LM result: "+r + " took "+wall+ " ms");   	
	    	
	    }
	     
	    public void xxxtestRecognizeStreamEP() {	    	
	    	System.out.println("Starting EP Stream Test ...");

	    	// read in the sound file.
	    	AudioInputStream	audioInputStream = null;
	    	Type type = null;
	    	AFormat format= null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile2);
	    		type = AudioSystem.getAudioFileFormat(soundFile2).getType();
	    		format = FormatUtils.covertToNeutral(audioInputStream.getFormat());
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}

	    	StreamInputEP(audioInputStream,format);

	    }
	    
	    
	    public void testRecognizeFileS4EPGrammar() {
	    	System.out.println("Starting File EP Test ...");	
	    	
	    	long timeout = 10000;     
	    	S4EndPointer ep = new S4EndPointer();
	    	FileS4EndPointingInputStream2 epStream = new FileS4EndPointingInputStream2(ep);

	    	epStream.setMimeType(s4audio);
	    	epStream.setupStream(soundFile2);
	 
	    	String r = null;
	    	
	    	boolean lmflg = false;
	    	boolean batchFlag = false;
	        try {	            
	            r = recog.recognize(userId, grammarUrl,  epStream,  lmflg,  batchFlag,OutputFormat.text, timeout) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
	    	
            System.out.println("grammar result: "+r);
	    }
            
	    public void testRecognizeFileS4EPGrammarAsynch() {
	    	System.out.println("Starting File EP Test ...");	
	    	recog.enableAsynchMode(2);
	    	
	    	long timeout = 10000;
	    	S4EndPointer ep = new S4EndPointer();
	    	FileS4EndPointingInputStream2 epStream = new FileS4EndPointingInputStream2(ep);

	    	epStream.setMimeType(s4audio);
	    	epStream.setupStream(soundFile2);
	 
	    	String r = null;
	    	
	    	Listener l = new Listener();
	    	boolean lmflg = false;
	    	boolean batchFlag = true;
	    	String id;
	        try {	            
	            id = recog.recognizeAsynch(devId,key,userId, grammarUrl,  epStream,  lmflg,  batchFlag,OutputFormat.text, timeout,l) ;
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
	    public void testRecognizeFileS4EPGrammarAsynchLM() {
	    	System.out.println("Starting File EP Test ...");	
	    	recog.enableAsynchMode(2);
	    	
	    	long timeout = 10000;
	    	S4EndPointer ep = new S4EndPointer();
	    	FileS4EndPointingInputStream2 epStream = new FileS4EndPointingInputStream2(ep);

	    	epStream.setMimeType(s4audio);
	    	epStream.setupStream(soundFile2);
	 
	    	String r = null;
	    	
	    	Listener l = new Listener();
	    	boolean lmflg = true;
	    	boolean batchFlag = true;
	    	String id;
	        try {	            
	            id = recog.recognizeAsynch(devId,key,userId, grammarUrl,  epStream,  lmflg,  batchFlag,OutputFormat.text, timeout,l) ;
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
	 
	    	String r = null;
	    	
	    	Listener l = new Listener();
	    	boolean lmflg = false;
	    	boolean batchFlag = false;
	    	String id = null;
	        try {	            
	            id = recog.recognizeAsynch(devId,key,userId, grammarUrl,  epStream,  lmflg,  batchFlag,OutputFormat.text, timeout,l) ;
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
	
            
	    	String r = null;
	    	boolean lmflg = true;
	    	boolean batchFlag = false;
	        try {	            
	            r = recog.recognize(userId, grammarUrl,  epStream,  lmflg,  batchFlag,OutputFormat.text, timeout) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            
            }
            System.out.println("ENDPOINT TEST: lm  result: "+r);
	    	
	    }

	    
	    
	    
	    public void xtestRecognizeStreamS4EP() {
	
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

	    	String r = null;
	    	boolean lmflg = true;
	    	boolean batchFlag = false;
	        try {
	        	r = recog.recognize(userId, grammarUrl,  epStream,  lmflg,  batchFlag,OutputFormat.text, timeout) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
	    	
           System.out.println("lm result: "+r);
	
           
       	    // get an audio stream for the test from a file
            audioInputStream = null;
	    	type = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile2);
	    		type = AudioSystem.getAudioFileFormat(soundFile2).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	
	    	//run the test

	    	epStream = new JavaSoundStreamS4EndPointingInputStream(ep);
	    	epStream.setMimeType(s4audio);
	    	epStream.setupStream(audioInputStream);
	
	    	lmflg = false;
	    	batchFlag = false;
	        try {
	        	r = recog.recognize(userId, grammarUrl,  epStream,  lmflg, batchFlag,OutputFormat.text, timeout) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
	    	
            System.out.println("grammar result: "+r);
        	
	    	

	    }
	    
	    
	    //-------------------------------------------------------------------
	    


	    
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
	    
	    
	    //private void testRtpInput(InputStream stream) {
	    //	
	    //}


	    private void StreamInputEP(AudioInputStream stream, AFormat format) {
	    
	    	
	    	long timeout = 10000;
	    	
	    	AudioStreamEndPointer ep = new AudioStreamEndPointer();
	    	StreamEndPointingInputStream epStream = new StreamEndPointingInputStream(ep);
	    	epStream.setMimeType(wav);
	    	epStream.setupStream(stream,format);
	    	

	    	String r = null;
	    	boolean lmflg = true;
	    	boolean batchFlag = false;
	        try {          
	            r = recog.recognize(userId, grammarUrl,  epStream,  lmflg,  batchFlag,OutputFormat.text, timeout) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }    	
            System.out.println("lm result: "+r);

	    	lmflg = false;
	    	batchFlag = false;
	        try {          
	            r = recog.recognize(userId, grammarUrl,  epStream,  lmflg, batchFlag,OutputFormat.text, timeout) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }    	
            System.out.println("grammar result: "+r);
	    	

	    	
	    }
	    
	    


	    //private void testRtpInputS4EP(InputStream stream) {
	    //	
	    //}
	    
	    
}



