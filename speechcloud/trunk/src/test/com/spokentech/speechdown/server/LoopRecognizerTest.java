/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;


import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat.Type;

import org.apache.log4j.Logger;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.gson.Gson;
import junit.framework.TestCase;


import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;

import java.net.MalformedURLException;
import java.net.URL;

import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import javax.speech.EngineException;
import javax.speech.EngineStateError;
import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

import com.spokentech.speechdown.client.util.FormatUtils;
import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.Utterance;
import com.spokentech.speechdown.common.Utterance.OutputFormat;
import com.spokentech.speechdown.server.domain.SpeechRequestDTO;
import com.spokentech.speechdown.server.recog.RecEngine;
import com.spokentech.speechdown.server.standalone.SpeechWorker;
import com.spokentech.speechdown.server.util.pool.SpringSphinxRecEngineFactory;
import com.sun.speech.engine.recognition.BaseRecognizer;
import com.sun.speech.engine.recognition.BaseRuleGrammar;
import java.io.IOException;
import javax.speech.recognition.GrammarException;

public class LoopRecognizerTest extends TestCase  {


		private static Logger _logger = Logger.getLogger(LoopRecognizerTest.class);

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

	     public void startup() {}
	     protected void setUp() {
			    gson = new Gson();
	     }
	     
	   
	     public void testOog() {

	    	 URL audioFileURL=null;
	    	 URL configURL = null;

	    	 //audioFileURL = WavFile.class.getResource("12345.wav");
	    	 //audioFileURL = WavFile.class.getResource("1.wav");
	    	 try{
	    		 audioFileURL = new URL("file:///work/speechcloud/etc/test/oog/1.wav");		
	    	 }catch(MalformedURLException mue){
	    		 System.err.println(mue); 
	    	 }        
	    	 System.out.println("Hey");
	    	 //configURL = WavFile.class.getResource("config.xml");
	    	 try {
				configURL =  new URL("file:///work/speechcloud/etc/test/oog/config.xml");
			} catch (MalformedURLException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

	    	 System.out.println("Loading Recognizer as defined in '" + configURL.toString() + "'...\n");
	    	 ConfigurationManager cm = new ConfigurationManager(configURL);

	    	 // look up the recognizer (which will also lookup all its dependencies
	    	 Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
	    	 recognizer.allocate();



	    	 JSGFGrammar jsgf = (JSGFGrammar) cm.lookup("jsgfGrammar");
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
	    	 AudioFileDataSource dataSource = (AudioFileDataSource) cm.lookup("audioFileDataSource");
	    	 dataSource.setAudioFile(audioFileURL, null);

	    	 // decode the audio file.
	    	 System.out.println("Decoding " + audioFileURL);
	    	 Result result = recognizer.recognize();

	    	 System.out.println("Result: " + (result != null ? result.getBestFinalResultNoFiller() : null));

	    	 RuleGrammar ruleGrammar = new BaseRuleGrammar (jsapiRecognizer, jsgf.getRuleGrammar());
	    	 //utterance = ResultUtils.getAllResults(r, false, false,ruleGrammar);

	     }

	    
}



