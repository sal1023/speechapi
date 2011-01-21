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
import com.spokentech.speechdown.server.util.pool.SphinxRecEngineFactory;
import com.sun.speech.engine.recognition.BaseRecognizer;
import com.sun.speech.engine.recognition.BaseRuleGrammar;
import java.io.IOException;
import javax.speech.recognition.GrammarException;

public class SphinxRecEngineTest extends TestCase implements BeanFactoryAware {


		private static Logger _logger = Logger.getLogger(SphinxRecEngineTest.class);


     	private BeanFactory beanFactory;
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


	     protected void setUp() {
			    gson = new Gson();
		    	


	     }
	     public  ClassPathXmlApplicationContext context;
	     public void testOog2() {
	         context = new ClassPathXmlApplicationContext("file:///c:/work/speechcloud/etc/test/oog/speechserver.xml");

	     
	     }
	     
	     public void startup() {
	    	 System.out.println("starting up ...");
	    	 
	    	 try {
		    		grammarUrl = new URL(grammar);
				} catch (MalformedURLException e) {  
			         e.printStackTrace();  
				}	
				try {
					BufferedReader in = new BufferedReader(
							new InputStreamReader(
									grammarUrl.openStream()));
	
					String inputLine;
					grammarString="";
					while ((inputLine = in.readLine()) != null) {
						grammarString  = grammarString+inputLine+"\n";
						//System.out.println("XXX-< "+inputLine);
					}
					System.out.println(grammarString);
	
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	         SphinxRecEngineFactory sphinxRecEngineFactory =(SphinxRecEngineFactory) this.beanFactory.getBean("sphinxRecEngineFactory");
	         RecEngine recEngine = sphinxRecEngineFactory.createSphinxRecEngine(true);
	         
	         	String testFileName = "c:/work/speechcloud/etc/test/oog/tal.wav";
	         	File testFile = new File(testFileName);
	         	/*InputStream as = null;
				try {
		         	URL audioUrl = new URL("file://"+testFile);
		            as = audioUrl.openStream();
	            } catch (IOException e1) {
		            // TODO Auto-generated catch block
		            e1.printStackTrace();
	            }*/
	            
	            
	           //since no xuggler on 64 bit, doing this just to get the format
		    	AudioInputStream audioInputStream = null;
		    	Type type = null;
		    	try {

		    		audioInputStream = AudioSystem.getAudioInputStream(testFile);
		    		type = AudioSystem.getAudioFileFormat(testFile).getType();
		    	} catch (Exception e) {
		    		e.printStackTrace();
		    	}
		        AudioFormat f = audioInputStream.getFormat();
		        AFormat af = FormatUtils.covertToNeutral(f);
	            
	            
				
				String mimeType = "audio/x-wav";

				String outputFormat;
				OutputFormat outMode =  OutputFormat.valueOf("json"); //OutputFormat.json;
				PrintWriter out = null;
				HttpServletResponse response = null;
				SpeechRequestDTO hr = null;
				try {
					long start = System.nanoTime();
					boolean doEndpointing = false;
					boolean cmnBatch = true;
					//System.out.println("LKJHHHH "+grammarString);
					Utterance u = recEngine.recognize(audioInputStream, mimeType, grammarString, af, outMode, doEndpointing, cmnBatch , hr);
			        //transcription = recEngine.transcribe( as,  mimeType,  af,   outMode,  out, response,  hr);
					long stop = System.nanoTime();
					long wall = (stop - start)/1000000;
					
					_logger.info("Done, Wall time was: "+wall);
					//_logger.info(u);

				   
				}catch (Exception e) {
					e.printStackTrace();
		
				}
	         
	     }
	 
	     
	   
		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			System.out.println("setting the bean factory "+beanFactory);
		      this.beanFactory = beanFactory;	
			
		}


	    
}



