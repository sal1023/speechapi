/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.util.pool;


import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import com.spokentech.speechdown.server.recog.GrammarManager;
import com.spokentech.speechdown.server.recog.RecEngine;
import com.spokentech.speechdown.server.recog.SphinxRecEngine;

import edu.cmu.sphinx.decoder.Decoder;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.decoder.pruner.Pruner;
import edu.cmu.sphinx.decoder.pruner.SimplePruner;
import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer;
import edu.cmu.sphinx.decoder.search.ActiveListFactory;
import edu.cmu.sphinx.decoder.search.ActiveListManager;
import edu.cmu.sphinx.decoder.search.PartitionActiveListFactory;
import edu.cmu.sphinx.decoder.search.SearchManager;
import edu.cmu.sphinx.decoder.search.SimpleActiveListManager;
import edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager;
import edu.cmu.sphinx.decoder.search.WordActiveListFactory;
import edu.cmu.sphinx.decoder.search.WordPruningBreadthFirstSearchManager;
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
import edu.cmu.sphinx.instrumentation.MemoryTracker;
import edu.cmu.sphinx.instrumentation.Monitor;
import edu.cmu.sphinx.instrumentation.SpeedTracker;
import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FastDictionary;
import edu.cmu.sphinx.linguist.language.ngram.large.LargeTrigramModel;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist;

import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.ConfidenceScorer;
import edu.cmu.sphinx.result.MAPConfidenceScorer;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.PropertyException;


// TODO: Auto-generated Javadoc
/**
 * Main program for the speech server.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SphinxRecEngineFactory {  

    private static Logger logger = Logger.getLogger(SphinxRecEngineFactory.class);	
	
	 
	private static double wordInsertionProbability = .1;
	private static double silenceInsertionProbability = 1;
    private static double outOfGrammarBranchProbability = 1e-20 ;//1e-50 ;//1e-20 ;
    private static double phoneInsertionProbability =  1e-10 ; //1e-20
    private static float languageWeight = 8.0f;
    private static int absoluteBeamWidth = -1;
	private static double relativeBeamWidth = 1e-90;
	private static double relativeWordBeamWidth = 1e-30;
	// language models
	private static String lm= "file:///C:/work/models/lm/wsj5kc.Z.DMP";
	private  String amLoc ="file:///C:/work/models/am/voxforge-en-0.4/model_parameters/voxforge_en_sphinx.cd_cont_5000";
	private  String amDloc="/";
	private  String amDef="mdef";
	//Dictionaries
	private static String dict ="file:///c:/work/models/dict/cmudict.0.7a";
	private static String fillerDict ="file:///c:/work/models/dict/mcmodel.filler";
	private static AudioFileDataSource audioFileDataSource;
	Properties props = null;

	/*
	 * 
	speech.search.checkPriorListsEmpty = false
	speech.speechClassifierThreshold=13
	speech.showCreations =false
	speech.lm.queryLogFile=queryLogFile
	speech.scorer.scoreNormalizer = null
	#Thread.NORM_PRIORITY=5
	speech.dictionary.createMissingWords=false
	speech.search.showTokenCount=false
	speech.am.useComposites=true
	speech.am.loader.isBinary=true
	speech.am.loader.sparseForm=true
	speech.am.loader.vectorLength=39
	speech.am.loader.distFloor=0.0
	speech.am.voxforge.en.id=english16
	speech.am.voxforge.en.sampleRate=16000
	speech.am.voxforge.en.langugae=en

	 */

	private String speech_dictionary_dictionaryPath ="file:///c:/work/models/dict/cmudict.0.7a";
	private String speech_dictionary_fillerPath = "file:///c:/work/models/dict/mcmodel.filler";
	private boolean speech_dictionary_addSilEndingPronunciation =false ;
	private String speech_dictionary_wordReplacement = "&lt;sil&gt";
	private boolean speech_dictionary_allowMissingWords = false;
	private String speech_am_spokentech_en_loader_Location="file:///C:/work/models/am/voxforge-en-0.4/model_parameters/voxforge_en_sphinx.cd_cont_5000";
	private String speech_am_spokentech_en_loader_ModelDefinition="/";
	private String speech_am_spokentech_en_loader_DataLocation="mdef";
	private float speech_am_loader_varianceFloor= 0.0001f;
	private float speech_am_loader_mixtureWeightFloor=1e-7f;
	private boolean speech_am_loader_useCDUnits=true;
	private String speech_lm_languageModelFile="file:///C:/work/models/lm/wsj5kc.Z.DMP";
	private String speech_lm_format="DMP";
	private int speech_lm_maxTrigramCacheSize=100000;
	private int speech_lm_maxBigramCacheSize=50000;
	private boolean speech_lm_clearCachesAfterUtterance=false;
	private int speech_lm_maxDepth=3;
	private boolean speech_lm_applyLanguageWeightAndWip=false;
	private float speech_lm_languageWeight=1.0f;
	private float speech_lm_wordInsertionProbability=1.0f;
	private float speech_lm_unigramWeight=0.7f;
	private boolean speech_lm_fullSmear=false;
	private boolean speech_linguist_fullWordHistories=true;
	private boolean speech_linguist_wantUnigramSmear=true;
	private double speech_linguist_lmwordInsertionProbability=0.2d;
	private double speech_linguist_lmsilenceInsertionProbability=0.1d;
	private double speech_linguist_lmfillerInsertionProbability=1e-6d;
	private double speech_linguist_lmunitInsertionProbability=1.0d;
	private float speech_linguist_lmlanguageWeight=6.5f;
	private boolean speech_linguist_addFillerWords=false;
	private boolean speech_linguist_generateUnitStates=false;
	private float speech_linguist_smear=1.3f;
	private int speech_linguist_cachesize=0;
	private int speech_scorer_minScoreablesPerThread=10;
	private boolean speech_scorer_cpuRelative=true;
	private int speech_scorer_numThreads =0;
	private int speech_scorer_threadPriority=5;
	private int speech_search_growSkipInterval=0;
	private boolean speech_search_checkStateOrder=false;
	private boolean speech_search_buildWordLattice=true;
	private int speech_search_maxLatticeEdges=100;
	private int speech_search_acousticLookahead=2;
	private boolean speech_search_keepAllTokens=true;
	private boolean speech_confidence_dumpLattice= false;
	private boolean speech_confidence_dumpSausage =false;
	
	private double speech_relativeWordBeamWidth2=1e-30;
	private int speech_absoluteWordBeamWidth2=30;
	private int speech_absoluteBeamWidth2=25000;
	private double speech_relativeBeamWidth2=1e-40;

	private double speech_relativeBeamWidth=1e-90;
	private int speech_absoluteBeamWidth=-1;
	
	private boolean speech_grammar_showGrammar=false;
	private boolean speech_grammar_optimizeGrammar=true;
	private boolean speech_grammar_addSilenceWords=true;
	private boolean speech_grammar_addFillerWords=true;
	private double speech_grammar_wordInsertionProbability=1e-36;
	private double speech_grammar_silenceInsertionProbability=1.0d;
	private double speech_grammar_fillerInsertionProbability=1.0d;
	private double speech_grammar_unitInsertionProbability=1.0;
	private double speech_grammar_languageWeight=8.0;
	
	private boolean speech_grammar_addOutOfGrammarBranch=false;
	private boolean speech_grammar_spreadWordProbabilitiesAcrossPronunciations=false;
	private boolean speech_grammar_showCompilationProgress=true;
	private boolean speech_grammar_dumpGStates=false;
	private double speech_grammar_outOfGrammarProbability=1e-25;
	private double speech_grammar_phoneInsertionProbability=1e-20;
	private boolean speech_recordingEnabled=false;
	private String speech_recordingDir="c:/temp";
	private String speech_grammarName="example";
	private String speech_baseGrammarDir="c/work";
	private String speech_grammarLocation="file:///C:/work/speechserver-1_0_0/temp/grammar";

	private int speech_frontend_markerSpeechLeaderFrames=50;
	private int speech_frontend_markerSpeechTrailer=100;
	private int speech_frontend_markerSpeechLeader=100;
	private int speech_frontend_markerEndSilenceTime=200;
	private int speech_frontend_markerStartSpeechTime=200;
	private double speech_frontend_classifierMinSignal=0.0;
	private double speech_frontend_classifierThreshold=10.0;
	private double speech_fronend_classifierAdjustment=0.003;
	private int speech_frontend_frameLengthMs=10;
	private boolean speech_recorder_isBigEndian=false;
	private boolean speech_recorder_isSigned=true;
	private boolean speech_recorder_captureUtts=false;


	private String speech_recorder_filePath="c/temp";
	private boolean speech_recorder_IsComplete=false;
	private int speech_recorder_bitsPerSample=16;


	private double speech_am_loader_distFloor=0.0;

	
	
    /**
     * Startup.
     */
    public void startup() {
        logger.info("Starting up the sphinx rec engine Factory...");
        props = loadProperties("speech.properties");
        parseProps(props);
        
        Enumeration e = props.propertyNames();

        while (e.hasMoreElements()) {
          String key = (String) e.nextElement();
          System.out.println(key + " -- " + props.getProperty(key));
        }
    }

    private void parseProps(Properties props2) {

    	String prop;
    	
    	speech_dictionary_dictionaryPath = (String) props.get("speech.dictionary.dictionaryPath");
    	speech_dictionary_fillerPath = (String) props.get("speech.dictionary.fillerPath");
    	speech_lm_languageModelFile = (String) props.get("speech.lm.languageModelFile");

    	//model loader
    	speech_am_spokentech_en_loader_Location = (String) props.get("speech.am.spokentech.en.loader.Location");     
    	speech_am_spokentech_en_loader_ModelDefinition = (String)props.get("speech.am.spokentech.en.loader.ModelDefinition");
    	speech_am_spokentech_en_loader_DataLocation = (String)props.get("speech.am.spokentech.en.loader.DataLocation");

    	prop = (String)props.get("speech.am.loader.mixtureWeightFloor");
    	try {
    		speech_am_loader_mixtureWeightFloor = Float.valueOf(prop.trim()).floatValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech_am_loader_mixtureWeightFloor");
    	} 

    	prop = (String) props.get("speech.am.loader.varianceFloor");
    	try {
    		speech_am_loader_varianceFloor = Float.valueOf(prop.trim()).floatValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.am.loader.varianceFloor");
    	}

    	prop = (String)props.get("speech.am.loader.useCDUnits");
    	speech_am_loader_useCDUnits = Boolean.parseBoolean(prop);

    	//dictionary
    	prop = (String) props.get("speech.dictionary.addSilEndingPronunciation");
    	speech_dictionary_addSilEndingPronunciation = Boolean.parseBoolean(prop);

    	speech_dictionary_wordReplacement = (String) props.get("speech.dictionary.wordReplacement");


    	prop = (String) props.get("speech.dictionary.createMissingWords");
    	speech_dictionary_allowMissingWords =Boolean.parseBoolean(prop);

    	//large trigram model
    	speech_lm_format = (String) props.get("speech.lm.format");

    	prop = (String) props.get("speech.am.loader.varianceFloor");
    	try {
    		speech_lm_maxTrigramCacheSize = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.am.loader.varianceFloor");
    	}

    	prop = (String) props.get("speech.lm.maxBigramCacheSize");
    	try {
    		speech_lm_maxBigramCacheSize = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.lm.maxBigramCacheSize");
    	}

    	prop = (String) props.get("speech.lm.clearCachesAfterUtterance");
    	speech_lm_clearCachesAfterUtterance =Boolean.parseBoolean(prop);

    	prop = (String) props.get("speech.lm.maxDepth");
    	try {
    		speech_lm_maxDepth = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.lm.maxDepth");
    	}

    	prop = (String) props.get("speech.lm.applyLanguageWeightAndWip");
    	speech_lm_applyLanguageWeightAndWip = Boolean.parseBoolean(prop);

    	prop = (String) props.get("speech.lm.languageWeight");
    	try {
    		speech_lm_languageWeight = Float.valueOf(prop.trim()).floatValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.lm.languageWeight");
    	}

    	prop = (String) props.get("speech.lm.wordInsertionProbability");
    	try {
    		speech_lm_wordInsertionProbability = Float.valueOf(prop.trim()).floatValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.am.loader.varianceFloor");
    	}
    	prop = (String) props.get("speech.lm.unigramWeight");
    	try {
    		speech_lm_unigramWeight= Float.valueOf(prop.trim()).floatValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.lm.unigramWeight");
    	}

    	prop = (String) props.get("speech.lm.fullSmear"); 
    	speech_lm_fullSmear = Boolean.parseBoolean(prop);

    	//linguist
    	prop = (String) props.get("speech.linguist.fullWordHistories"); 
    	speech_linguist_fullWordHistories = Boolean.parseBoolean(prop);

    	prop = (String) props.get("speech.linguist.wantUnigramSmear"); 
    	speech_linguist_wantUnigramSmear = Boolean.parseBoolean(prop);

    	prop = (String) props.get("speech.linguist.lmwordInsertionProbability"); 
    	try {
    		speech_linguist_lmwordInsertionProbability = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.linguist.lmwordInsertionProbability");
    	}

    	prop = (String) props.get("speech.linguist.lmsilenceInsertionProbability");
    	try {
    		speech_linguist_lmsilenceInsertionProbability = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.linguist.lmsilenceInsertionProbability");
    	}
    	prop = (String) props.get("speech.linguist.lmfillerInsertionProbability"); 
    	try {
    		speech_linguist_lmfillerInsertionProbability = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.linguist.lmfillerInsertionProbability");
    	}
    	prop = (String) props.get("speech.linguist.lmunitInsertionProbability"); 
    	try {
    		speech_linguist_lmunitInsertionProbability = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.linguist.lmunitInsertionProbability");
    	}

    	prop = (String) props.get("speech.linguist.lmlanguageWeight");  
    	try {
    		speech_linguist_lmlanguageWeight = Float.valueOf(prop.trim()).floatValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.linguist.lmlanguageWeight");
    	}

    	prop = (String) props.get("speech.linguist.addFillerWords"); 
    	speech_linguist_addFillerWords = Boolean.parseBoolean(prop);

    	prop = (String) props.get("speech.linguist.generateUnitStates"); 
    	speech_linguist_generateUnitStates = Boolean.parseBoolean(prop);

    	prop = (String) props.get("speech.linguist.smear"); 
    	try {
    		speech_linguist_smear = Float.valueOf(prop.trim()).floatValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.linguist.smear");
    	}
    	prop = (String) props.get("speech.linguist.cachesize"); 
    	try {
    		speech_linguist_cachesize = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.linguist.cachesize");
    	}

    	//scorer
    	prop = (String) props.get("speech.scorer.minScoreablesPerThread");
    	try {
    		speech_scorer_minScoreablesPerThread = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.scorer.minScoreablesPerThread");
    	}
    	prop = (String) props.get("speech.scorer.cpuRelative");
    	speech_scorer_cpuRelative = Boolean.parseBoolean(prop);

    	prop = (String) props.get("speech.scorer.numThreads");
    	try {
    		speech_scorer_numThreads = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.scorer.numThreads");
    	}

    	prop = (String) props.get("speech.scorer.threadPriority");
    	try {
    		speech_scorer_threadPriority = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.scorer.threadPriority");
    	}

    	//search manager
   

    	prop = (String) props.get("speech.search.showTokenCount");
    	boolean speech_search_showTokenCount = Boolean.parseBoolean(prop);

    	prop = (String) props.get("speech.search.growSkipInterval");
    	try {
    		speech_search_growSkipInterval = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.search.growSkipInterval");
    	}

    	prop = (String) props.get("speech.search.checkStateOrder");
    	speech_search_checkStateOrder = Boolean.parseBoolean(prop);

    	prop = (String) props.get("speech.search.buildWordLattice");
    	speech_search_buildWordLattice = Boolean.parseBoolean(prop);

    	prop = (String) props.get("speech.search.maxLatticeEdges");
    	try {
    		speech_search_maxLatticeEdges = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.am.loader.varianceFloor");
    	}

    	prop = (String) props.get("speech.search.acousticLookahead");
    	try {
    		speech_search_acousticLookahead = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.search.acousticLookahead");
    	}

    	prop = (String) props.get("speech.search.keepAllTokens");
    	speech_search_keepAllTokens = Boolean.parseBoolean(prop);

    	prop = (String) props.get("speech.confidence.dumpLattice");
    	speech_confidence_dumpLattice = Boolean.parseBoolean(prop);
    	prop = (String) props.get("speech.confidence.dumpSausage");
    	speech_confidence_dumpSausage = Boolean.parseBoolean(prop);
    	
    	
     	prop = (String) props.get("speech.grammar.showGrammar");
     	speech_grammar_showGrammar = Boolean.parseBoolean(prop);
     	prop = (String) props.get("speech.grammar.optimizeGrammar");
     	speech_grammar_optimizeGrammar = Boolean.parseBoolean(prop);
    	prop = (String) props.get("speech.grammar.addSilenceWords");
    	speech_grammar_addSilenceWords = Boolean.parseBoolean(prop);
    	prop = (String) props.get("speech.grammar.addFillerWords");
    	speech_grammar_addFillerWords = Boolean.parseBoolean(prop);
    	

    	prop = (String) props.get("speech.am.loader.distFloor");
    
    		 
        	prop = (String) props.get("speech.am.loader.distFloor");
        	try {
        		speech_am_loader_distFloor = Double.valueOf(prop.trim()).doubleValue();
        	} catch (NumberFormatException nfe){
        		logger.warn("Using default value for speech_am_loader_distFloor");
        	}
    
    	prop = (String) props.get("speech.grammar.wordInsertionProbability");
    	try {
    		speech_grammar_wordInsertionProbability = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.grammar.wordInsertionProbability");
    	}
    	
    	prop = (String) props.get("speech.grammar.silenceInsertionProbability");
    	try {
    		speech_grammar_silenceInsertionProbability = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.grammar.silenceInsertionProbability");
    	}
    	
    	prop = (String) props.get("speech.grammar.fillerInsertionProbability");
    	try {
    		speech_grammar_fillerInsertionProbability = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.grammar.fillerInsertionProbability");
    	}
    	
    	prop = (String) props.get("speech.grammar.unitInsertionProbability");
    	try {
    		speech_grammar_unitInsertionProbability = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.grammar.unitInsertionProbability");
    	}
    	
    	prop = (String) props.get("speech.grammar.languageWeight");
    	try {
    		speech_grammar_languageWeight = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.grammar.languageWeight");
    	}
    	
    	
       	prop = (String) props.get("speech.grammar.dumpGStates");
       	speech_grammar_dumpGStates = Boolean.parseBoolean(prop);
     	prop = (String) props.get("speech.grammar.showCompilationProgress");
     	speech_grammar_showCompilationProgress = Boolean.parseBoolean(prop);
    	prop = (String) props.get("speech.grammar.spreadWordProbabilitiesAcrossPronunciations");
    	speech_grammar_spreadWordProbabilitiesAcrossPronunciations = Boolean.parseBoolean(prop);
    	prop = (String) props.get("speech.grammar.addOutOfGrammarBranch");
    	speech_grammar_addOutOfGrammarBranch = Boolean.parseBoolean(prop);

    	prop = (String) props.get("speech.grammar.outOfGrammarProbability");
    	try {
    		speech_grammar_outOfGrammarProbability = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.grammar.outOfGrammarProbability");
    	}
    	prop = (String) props.get("speech.grammar.phoneInsertionProbability");
    	try {
    		speech_grammar_phoneInsertionProbability = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.grammar.phoneInsertionProbability");
    	}
    	
    	speech_grammarLocation = (String) props.get("speech.grammarLocation");
    	speech_baseGrammarDir = (String) props.get("speech.baseGrammarDir");
    	speech_grammarName = (String) props.get("speech.grammarName");

    	
    	speech_recordingDir = (String) props.get("speech.recordingDir");

       	prop = (String) props.get("speech.recordingEnabled");
       	speech_recordingEnabled = Boolean.parseBoolean(prop);


    	
    	prop = (String) props.get("speech.absoluteBeamWidth");
    	try {
    		speech_absoluteBeamWidth = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.absoluteBeamWidth");
    	}

    	
    	prop = (String) props.get("speech.relativeBeamWidth");
    	try {
    		speech_relativeBeamWidth = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for sspeech.relativeBeamWidth");
    	}
    
    	prop = (String) props.get("speech.absoluteBeamWidth2");
    	try {
    		speech_absoluteBeamWidth2 = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.absoluteBeamWidth2");
    	}


    	
    	prop = (String) props.get("speech.relativeBeamWidth2");
    	try {
    		speech_relativeBeamWidth2 = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.relativeBeamWidth2");
    	}
    	
    	prop = (String) props.get("speech.absoluteWordBeamWidth2");
    	try {
    		speech_absoluteWordBeamWidth2 = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.absoluteWordBeamWidth2");
    	}
    	
    	prop = (String) props.get("speech.relativeWordBeamWidth2");
    	try {
    		speech_relativeWordBeamWidth2 = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.relativeWordBeamWidth2");
    	}
    
    	prop = (String) props.get("speech.frontend.frameLengthMs");
    	try {
    		speech_frontend_frameLengthMs = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.absoluteWordBeamWidth2");
    	}
    	
    	prop = (String) props.get("speech.fronend.classifierAdjustment");
    	try {
    		speech_fronend_classifierAdjustment = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.relativeWordBeamWidth2");
    	}
    	
    	
    
    	prop = (String) props.get("speech.frontend.classifierThreshold");
    	try {
    		speech_frontend_classifierThreshold = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.relativeWordBeamWidth2");
    	}
    	

    	prop = (String) props.get("speech.frontend.classifierMinSignal");
    	try {
    		speech_frontend_classifierMinSignal = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.frontend.classifierMinSignal");
    	}

    	
    	prop = (String) props.get("speech.frontend.markerStartSpeechTime");
    	try {
    		speech_frontend_markerStartSpeechTime = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.frontend.markerStartSpeechTime");
    	}
    	
    
    	prop = (String) props.get("speech.frontend.markerEndSilenceTime");
    	try {
    		speech_frontend_markerEndSilenceTime = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.frontend.markerEndSilenceTime");
    	}



    	prop = (String) props.get("speech.frontend.markerSpeechLeader");
    	try {
    		speech_frontend_markerSpeechLeader = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.frontend.markerSpeechLeader");
    	}
    	
    	prop = (String) props.get("speech.frontend.markerSpeechLeaderFrames");
    	try {
    		speech_frontend_markerSpeechLeaderFrames = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.frontend.markerSpeechLeaderFrames");
    	}
    	
    	prop = (String) props.get("speech.frontend.markerSpeechTrailer");
    	try {
    		speech_frontend_markerSpeechTrailer = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.frontend.markerSpeechTrailer");
    	}
    	
    	prop = (String) props.get("speech.relativeWordBeamWidth2");
    	try {
    		speech_relativeWordBeamWidth2 = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.relativeWordBeamWidth2");
    	}

    	prop = (String) props.get("speech.relativeWordBeamWidth2");
    	try {
    		speech_relativeWordBeamWidth2 = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.relativeWordBeamWidth2");
    	}

    	prop = (String) props.get("speech.relativeWordBeamWidth2");
    	try {
    		speech_relativeWordBeamWidth2 = Double.valueOf(prop.trim()).doubleValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.relativeWordBeamWidth2");
    	}
    	
    	prop = (String) props.get("speech.absoluteWordBeamWidth2");
    	try {
    		speech_absoluteWordBeamWidth2 = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.absoluteWordBeamWidth2");
    	}
    	
    	prop = (String) props.get("speech.absoluteWordBeamWidth2");
    	try {
    		speech_absoluteWordBeamWidth2 = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.absoluteWordBeamWidth2");
    	}

    	speech_recorder_filePath = (String) props.get("speech.recorder.filePath");
    	prop = (String) props.get("speech.recorder.IsComplete");
    	speech_recorder_IsComplete = Boolean.parseBoolean(prop);


    	prop = (String) props.get("speech.recorder.bitsPerSample");
    	try {
    		speech_recorder_bitsPerSample = Integer.valueOf(prop.trim()).intValue();
    	} catch (NumberFormatException nfe){
    		logger.warn("Using default value for speech.recorder.bitsPerSample");
    	}
    	
    	
       	prop = (String) props.get("speech.recorder.isBigEndian");
       	speech_recorder_isBigEndian = Boolean.parseBoolean(prop);

       	prop = (String) props.get("speech.recorder.isSigned");
       	speech_recorder_isSigned = Boolean.parseBoolean(prop);

       	prop = (String) props.get("speech.recorder.captureUtts");
      	speech_recorder_captureUtts = Boolean.parseBoolean(prop);

    	
    }

	public RecEngine createSphinxRecEngine(boolean grammar) {

	    SphinxRecEngine recEngine = null;
	    if (grammar) {
	    	 recEngine = createGrammarSphinxRecEngine();
	    } else {
	    	 recEngine = createLmSphinxRecEngine() ;
	    }

	    return recEngine;
    }
    
	private SphinxRecEngine createLmSphinxRecEngine() {

		UnitManager unitManager = new UnitManager();
		LogMath logMath = new LogMath(1.0001f, true);
		SimplePruner pruner = new SimplePruner();


		List<DataProcessor> pipeline = new ArrayList<DataProcessor>();
		List<Monitor> monitors = new ArrayList<Monitor>();


		URL wordDictUrl = null;
		URL fillerDictUrl = null;
		URL lmUrl = null;
		URL modelLocation = null;


		try {
			wordDictUrl = new URL(dict);
			fillerDictUrl = new URL(fillerDict);
			lmUrl =  new URL(lm);
			modelLocation = new URL(amLoc);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
/* SAL
		Sphinx3Loader modelLoader = new Sphinx3Loader(speech_am_voxforge_en_loader_Location, 
				speech_am_voxforge_en_loader_ModelDefinition, speech_am_voxforge_en_loader_DataLocation, 
				logMath, unitManager, 
				speech_am_loader_distFloor, 
				speech_am_loader_mixtureWeightFloor, 
				speech_am_loader_varianceFloor, 
				speech_am_loader_useCDUnits);
*/
		Dictionary dictionary = new FastDictionary(wordDictUrl, fillerDictUrl, new ArrayList<URL>(), false, "<sil>", false, false, unitManager);

		String useComp = (String) props.get("speech.am.useComposites");
//SAL		TiedStateAcousticModel model = new TiedStateAcousticModel(modelLoader, unitManager, true);

		LargeTrigramModel ltm = new LargeTrigramModel("DMP", 
				lmUrl, 
				null, 100000, 
				50000, false, 
				3, logMath, dictionary,
				false, languageWeight, 
				wordInsertionProbability, 0.5f, false);
/* SAL
		Linguist linguist = new LexTreeLinguist(model, logMath, unitManager,
				ltm, dictionary, 
				false, true, 
				wordInsertionProbability, silenceInsertionProbability,
				0.02, 1.0, 
				languageWeight, false, true,  
				1.0f, 0);
*/
		
		audioFileDataSource = new AudioFileDataSource(3200,null);
		pipeline.add(audioFileDataSource);
		//pipeline.add(new DataDumper());
		pipeline.add(new DataBlocker(10));
		pipeline.add(new SpeechClassifier(10, 0.003f, 10, 0));
		pipeline.add(new SpeechMarker(200, 500, 100, 50, 100, 13.0));
		pipeline.add(new NonSpeechDataFilter());
		pipeline.add(new Preemphasizer(0.97));
		pipeline.add(new RaisedCosineWindower(0.46, 25.625f, 10.0f));
		pipeline.add(new DiscreteFourierTransform(-1, false));
		//pipeline.add(new MelFrequencyFilterBank(200.0, 3500.0, 31));  //8khz
		//pipeline.add(new DiscreteCosineTransform(31, 13)); //8khz
		pipeline.add(new MelFrequencyFilterBank(130.0, 6800.0, 40));
		pipeline.add(new DiscreteCosineTransform(40, 13));
		pipeline.add(new BatchCMN());
		pipeline.add(new DeltasFeatureExtractor(3));
// SAL		pipeline.add(new FeatureTransform(modelLoader));
		FrontEnd frontEnd = new FrontEnd(pipeline);
	

		ThreadedAcousticScorer scorer = new ThreadedAcousticScorer(frontEnd, null, speech_scorer_minScoreablesPerThread, 
				speech_scorer_cpuRelative, speech_scorer_numThreads, Thread.NORM_PRIORITY);

		ArrayList<ActiveListFactory> list = new ArrayList<ActiveListFactory>();
		list.add(new PartitionActiveListFactory(speech_absoluteBeamWidth2, speech_relativeBeamWidth2, logMath));
		// was 21, 1e-25
		list.add(new WordActiveListFactory(speech_absoluteWordBeamWidth2, speech_relativeWordBeamWidth2, logMath, 0, 1));
		list.add(new WordActiveListFactory(speech_absoluteWordBeamWidth2, speech_relativeWordBeamWidth2, logMath, 0, 1));
		// abs was 25000
		list.add(new PartitionActiveListFactory(speech_absoluteBeamWidth2, speech_relativeBeamWidth2, logMath));
		list.add(new PartitionActiveListFactory(speech_absoluteBeamWidth2, speech_relativeBeamWidth2, logMath));
		list.add(new PartitionActiveListFactory(speech_absoluteBeamWidth2, speech_relativeBeamWidth2, logMath));

		SimpleActiveListManager activeListManager = new SimpleActiveListManager(list, false);

/* SAL
		SearchManager searchManager = new WordPruningBreadthFirstSearchManager(logMath, linguist, pruner,
				scorer, activeListManager,
				false, speech_relativeBeamWidth2,
				0,
				false, true,
				100, 1.7f,
				true);
*/
//SAL		Decoder decoder = new Decoder(searchManager, false, false, new ArrayList<ResultListener>(), 100000);
//SAL		Recognizer recognizer = new Recognizer(decoder, monitors);
//SAL		monitors.add(new MemoryTracker(recognizer, true, false));
//SAL		monitors.add(new SpeedTracker(recognizer, frontEnd, true, false, false, true));

		GrammarManager grammarManager = new GrammarManager();
		ConfidenceScorer confidenceScorer = new MAPConfidenceScorer(speech_linguist_lmlanguageWeight, speech_confidence_dumpLattice, speech_confidence_dumpSausage);
		ModelPools modelPool = new ModelPools();

		//SphinxRecEngine sphinxRecEngine = new SphinxRecEngine(recognizer, grammarManager,confidenceScorer,logMath,recordingDir,recordingFlag,modelPool);

		return null;  //SAL sphinxRecEngine;
	}

	private SphinxRecEngine createGrammarSphinxRecEngine() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
     * Shutdown.
     */
    public void shutdown() {
        logger.info("Shutting down the main Server...");    
    }




    
    public class LmFactory extends BasePoolableObjectFactory {
		@Override
		public Object makeObject() throws Exception {
			// TODO Auto-generated method stub
			return createSphinxRecEngine(false);
		}
		
	    // when an object is returned to the pool,  
	    // we'll clear it out 
	    public void passivateObject(Object obj) { 
	
	    } 
	    // for all other commons pool methods, the no-op  
	    // implementation in BasePoolableObjectFactory 
	    // will suffice 
    }
    
    public class GrammarFactory extends BasePoolableObjectFactory {
		@Override
		public Object makeObject() throws Exception {
			// TODO Auto-generated method stub
			return createSphinxRecEngine(true);
		}
		
	    // when an object is returned to the pool,  
	    // we'll clear it out 
	    public void passivateObject(Object obj) { 
	
	    } 
	    // for all other commons pool methods, the no-op  
	    // implementation in BasePoolableObjectFactory 
	    // will suffice 
    }

	public PoolableObjectFactory createLmPoolFactory() {

		return new LmFactory();
	}
	
	public PoolableObjectFactory createGrammarPoolFactory() {

		return new GrammarFactory();
	}
	
	
	
	public static Properties loadProperties (String name, ClassLoader loader) {
        if (name == null)
            throw new IllegalArgumentException ("null input: name");
        
        if (name.startsWith ("/"))
            name = name.substring (1);  
        Properties result = null;
        try {
	        InputStream in = null;
	
	        if (loader == null) loader = ClassLoader.getSystemClassLoader ();                
	  
	        in = loader.getResourceAsStream (name);
	        if (in != null) {
	            result = new Properties ();
	            result.load (in); // Can throw IOException
	        }
        } catch (Exception e) {
        	e.printStackTrace();
		}
      
        return result;
    }
    
    /**
     * A convenience overload of {@link #loadProperties(String, ClassLoader)}
     * that uses the current thread's context classloader.
     */
    public static Properties loadProperties (final String name) {
        return loadProperties (name,
            Thread.currentThread ().getContextClassLoader ());
    }
        

	
    
}
