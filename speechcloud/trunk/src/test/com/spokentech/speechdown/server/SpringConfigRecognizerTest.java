/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.google.gson.Gson;
import junit.framework.TestCase;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.recognizer.Recognizer.State;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import javax.speech.EngineException;
import javax.speech.EngineStateError;
import javax.speech.recognition.GrammarException;
import com.sun.speech.engine.recognition.BaseRecognizer;
import java.io.IOException;

public class SpringConfigRecognizerTest extends TestCase  {


		private static Logger _logger = Logger.getLogger(SpringConfigRecognizerTest.class);

		public  ClassPathXmlApplicationContext context;	
	    private Gson gson = null;
	    

		private String grammar = "file:///work/speechcloud/etc/test/oog/commands.gram";    
		URL grammarUrl = null;

		
		File soundFile1 = new File("c:/work/speechcloud/etc/prompts/lookupsports.wav");	 	
		File soundFile2 = new File("c:/work/speechcloud/etc/prompts/get_me_a_stock_quote.wav");	 	
		File soundFile3 = new File("c:/work/speechcloud/etc/prompts/i_would_like_sports_news.wav");	 	
		File soundFile4 = new File("c:/work/speechcloud/etc/prompts/fire_and_ice_frost_add_64kb.mp3");	 	
		
		
    	File soundFile0 = new File("c:/work/speechcloud/etc/prompts/cubanson.wav");

		private String grammarString;
    	//File soundFile0 = new File("c:/work/speechcloud/etc/prompts/fabfour.wav");

	     public void startup() {
	    	 System.out.println("XXXXX");
	     }
	     
	     protected void setUp() {
			    gson = new Gson();
			    context = new ClassPathXmlApplicationContext("file:///c:/work/speechcloud/etc/test/oog/speechserver-test.xml");
	     }
	     
	     public void testNewConfig() {    	 
	    	 URL audioFileURL=null;

	    	 //audioFileURL = WavFile.class.getResource("12345.wav");
	    	 //audioFileURL = WavFile.class.getResource("1.wav");
	    	 try{
	    		 //audioFileURL = new URL("file:///work/speechcloud/etc/prompts/get_me_a_stock_quote.wav");	
	    		 audioFileURL = new URL("file:///work/speechcloud/etc/test/oog/1.wav");		
	    	 }catch(MalformedURLException mue){
	    		 System.err.println(mue); 
	    	 }        
	    	 
	    	 Recognizer recognizer =(Recognizer) context.getBean("recognizerGramar");
	     	 recognizer.allocate();
	     	 
	         MyStateListener stateListener =  new MyStateListener();
	         MyResultListener resultListener = new MyResultListener();
	 		 recognizer.addResultListener(resultListener);

	 		 recognizer.addStateListener(stateListener);
	
	    	 JSGFGrammar jsgf =(JSGFGrammar) context.getBean("jsgf");
	    	 BaseRecognizer jsapiRecognizer = new BaseRecognizer(jsgf.getGrammarManager());
	    	 try {
	    		 jsapiRecognizer.allocate();
	    	 } catch (EngineException e1) {
	    		 // TODO Auto-generated catch block
	    		 e1.printStackTrace();
	    	 } catch (EngineStateError e1) {
	    		 // TODO Auto-generated catch block
	    		 e1.printStackTrace();
	    	 }

	    	 try {
				jsgf.setBaseURL(new URL("file:c:///work/speechcloud/etc/test/oog"));
			} catch (MalformedURLException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
	    	 try {
	    		 jsgf.loadJSGF("commands");
	    		 jsapiRecognizer.commitChanges();
	    	 } catch (JSGFGrammarParseException e) {
	    		 // TODO Auto-generated catch block
	    		 e.printStackTrace();
	    	 } catch (IOException e) {
	    		 // TODO Auto-generated catch block
	    		 e.printStackTrace();
	    	 } catch (JSGFGrammarException e) {
	    		 // TODO Auto-generated catch block
	    		 e.printStackTrace();
	    	 } catch (GrammarException e1) {
	    		 // TODO Auto-generated catch block
	    		 e1.printStackTrace();
	    	 }
	    	 
	    	 // configure the audio input for the recognizer
	    	 AudioFileDataSource dataSource = (AudioFileDataSource) context.getBean("audioFileDataSource");
	    	 dataSource.setAudioFile(audioFileURL, null);

	    	 // decode the audio file.
	    	 System.out.println("Decoding " + audioFileURL);
	    	 Result result = recognizer.recognize();
	    	 System.out.println("Result: " +result.getBestFinalResultNoFiller() );

	    	 System.out.println("Result: " +result );
	     }
	        
	     
	    
	     private class MyStateListener implements StateListener {

	 		public void statusChanged(State  arg0) {
	 	        _logger.debug("Recognizer Status changed to "+arg0.toString() +" "+System.currentTimeMillis());
	 	        
	         }

	 		public void newProperties(PropertySheet arg0) throws PropertyException {
	 	        _logger.debug("StateListener New properties called");
	 	        
	         }	
	     }
	     
	     private class MyResultListener implements ResultListener {

	 		public void newResult(Result arg0) {
	 			_logger.debug("best final result: "+arg0.getBestFinalResultNoFiller());
	 			_logger.debug("best pronuciation: "+arg0.getBestPronunciationResult());
	 			_logger.debug("Frame "+arg0.getStartFrame()+ " to "+arg0.getEndFrame()+"("+arg0.getFrameNumber()+")");
	 	        
	         }

	 		public void newProperties(PropertySheet arg0) throws PropertyException {
	 	        _logger.debug("ResultListener New properties called");
	 	        
	         }
	     	
	     }
}



