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
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.google.gson.Gson;
import junit.framework.TestCase;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifier;
import edu.cmu.sphinx.frontend.endpoint.SpeechMarker;
import edu.cmu.sphinx.frontend.feature.BatchCMN;
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.feature.FeatureTransform;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.recognizer.Recognizer.State;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.flat.FlatLinguist;
import edu.cmu.sphinx.linguist.language.ngram.large.LargeTrigramModel;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist;

import javax.speech.EngineException;
import javax.speech.EngineStateError;
import javax.speech.recognition.GrammarException;

import com.spokentech.speechdown.server.util.pool.ModelPools;
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
		
		//front end elements
		private static DataBlocker dataBlocker ;
		private static SpeechClassifier speechClassifier;
		private static SpeechMarker speechMarker;
		private static NonSpeechDataFilter nonSpeechDataFilter;
		//InsertSpeechSignalStage insertSpeechSignalStage;
		private static Preemphasizer preemphasizer;

		private static RaisedCosineWindower raisedCosineWindower ;
		private static DiscreteFourierTransform discreteFourierTransform ;
		private static MelFrequencyFilterBank melFrequencyFilterBank16k ;
		private static MelFrequencyFilterBank melFrequencyFilterBank8k ;
		private static DiscreteCosineTransform discreteCosineTransform8k ;
		private static DiscreteCosineTransform discreteCosineTransform16k ;
		private static BatchCMN batchCmn ;

		private static FeatureTransform lda;
		private static DeltasFeatureExtractor deltasFeatureExtractor;
		//private WavWriter recorder; 
		

	     public void startup() {
	    	 System.out.println("XXXXX");
	     }
	     
	     protected void setUp() {
			    gson = new Gson();
			    context = new ClassPathXmlApplicationContext("speechserver.xml");
			    //context = new ClassPathXmlApplicationContext("file:///c:/work/speechcloud/etc/test/oog/speechserver-test.xml");
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
	    	 
	    	 Recognizer recognizer =(Recognizer) context.getBean("recognizerGrammar");
	    	 
    	 
    	 	createFrontEndElements();
        
            // configure the audio input for the recognizer
            AudioFileDataSource dataSource = (AudioFileDataSource) context.getBean("audioFileDataSource");
            dataSource.setAudioFile(audioFileURL, null);
            ModelPools modelPool = (ModelPools) context.getBean("modelPool");
    		//AcousticModel am = modelPool.getAcoustic().get("english16").getModel();
    		//LargeTrigramModel trigramModel = modelPool.getLanguage().get("hub4");
    		AcousticModel am = modelPool.getAcoustic().get("french16").getModel();
    		Dictionary d = modelPool.getDictionary().get("french");
    		
  
    		JSGFGrammar jsgf =(JSGFGrammar) context.getBean("jsgf");
	    	jsgf.setDictionary(d);
	    	 
    		FlatLinguist ling = (FlatLinguist)recognizer.getDecoder().getSearchManager().getLinguist();		
    		ling.setAcousticModel(am);
    		ling.setGrammar(jsgf);
            
            
            Sphinx3Loader modelLoader = (Sphinx3Loader) recognizer.getDecoder().getSearchManager().getLinguist().getAcousticModel().getLoader();        
            //FrontEnd fe = (FrontEnd) as.getFrontEnd();
            FrontEnd fe = createAudioFrontend((DataProcessor)dataSource,modelLoader,16000);
    		//fe.setDataSource((DataProcessor) dataSource);
     	    // set the front end in the scorer in realtime
    		recognizer.getDecoder().getSearchManager().getScorer().setFrontEnd(fe);
            
	    	 
	     	 recognizer.allocate();
	     	 
	         MyStateListener stateListener =  new MyStateListener();
	         MyResultListener resultListener = new MyResultListener();
	 		 recognizer.addResultListener(resultListener);

	 		 recognizer.addStateListener(stateListener);
	

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
	     
	     
	 	
		 private static void createFrontEndElements() {
		    	
		    	//TODO: use spring rather than constructors here...
		    	/*There are the spring bean prototype names
		    	 * Could get the beanfactory and get them that way, 
		    	 * not sure what is best yet...
		    	speechClassifier
		        speechMarker
		        speechDataMonitor
		        nonSpeechDataFilter
		        identityStage
		        dataBlocker
		        insertSpeechSignal
		        recorder
		        preemphasizer
		        dither
		        windower
		        fft
		        melFilterBank16k
		        melFilterBank8k
		        dct
		        batchCMN
		        liveCMN
		        featureExtraction*/
		    	
		    	

		    	// create all the components that may be needed for the front end ahead of time. 
		    	// to save time at recognition requests time.
		    	dataBlocker = new DataBlocker(10);
		    	speechClassifier = new SpeechClassifier(10,0.003,10.0,0.0);
		    	speechMarker = new SpeechMarker(200,200,100,50,100,15);
		    	nonSpeechDataFilter = new NonSpeechDataFilter();

		    	//insertSpeechSignalStage = new InsertSpeechSignalStage();

		    	preemphasizer = new Preemphasizer(0.97);
		    	//dither = new Dither();
		    	raisedCosineWindower = new RaisedCosineWindower(0.46,(float)25.625,(float)10.0);
		    	discreteFourierTransform = new DiscreteFourierTransform(-1,false);
		    	melFrequencyFilterBank16k = new MelFrequencyFilterBank((double)130.0,(double)6800.0,40);
		    	melFrequencyFilterBank8k = new MelFrequencyFilterBank((double)200.0,(double)3500.0,31);
		    	discreteCosineTransform8k = new DiscreteCosineTransform(31,13);
		    	discreteCosineTransform16k = new DiscreteCosineTransform(40,13);
		    	batchCmn = new BatchCMN();
		    	//liveCmn = new LiveCMN(12,500,800);
		    	deltasFeatureExtractor = new DeltasFeatureExtractor(3);
				//recorder = new WavWriter(recordingFilePath,isCompletePath,bitsPerSample,bigEndian,isSigned,captureUtts);
		    }
		    
		    
		 private static FrontEnd createAudioFrontend(DataProcessor dataSource, Loader loader, int sampleRate) {

			 ArrayList<DataProcessor> components = new ArrayList <DataProcessor>();
			 components.add(dataSource);
			 components.add (dataBlocker);
			 components.add (speechClassifier);
			 components.add (speechMarker);
			 components.add (nonSpeechDataFilter);

			 components.add (preemphasizer);
			 //components.add (dither);
			 components.add (raisedCosineWindower);
			 components.add (discreteFourierTransform);
			 if (sampleRate == 16000) {
				 components.add (melFrequencyFilterBank16k);
				 components.add (discreteCosineTransform16k);
			 }else if (sampleRate == 8000) {
				 components.add (melFrequencyFilterBank8k);
				 components.add (discreteCosineTransform8k);
			 }else {
				 components.add (melFrequencyFilterBank8k);
				 components.add (discreteCosineTransform8k);
			 }

			 components.add (batchCmn);

			 components.add (deltasFeatureExtractor);

			 //TODO:  feature extractor is constructed on the fly, it needs the loader for the model.  Should not be very high overhead if
			 //it already loaded anyway.  Some models dont have a matrix. 
			 if (loader.getTransformMatrix() != null) {
			 System.out.println(loader.getTransformMatrix());
				 lda = new FeatureTransform(loader);
				 components.add (lda);
			 }
			 //for (DataProcessor dp : components) {
			 //   _logger.debug(dp);
			 //}
			 FrontEnd fe = new FrontEnd (components);
			 return fe;   
		 }
}



