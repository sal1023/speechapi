/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.util.pool;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.decoder.search.ActiveListFactory;
import edu.cmu.sphinx.decoder.search.ActiveListManager;
import edu.cmu.sphinx.decoder.search.SearchManager;
import edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager;
import edu.cmu.sphinx.decoder.search.WordPruningBreadthFirstSearchManager;
import edu.cmu.sphinx.instrumentation.Monitor;
import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.linguist.Linguist;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;

import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.ConfidenceScorer;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.PropertyException;


// TODO: Auto-generated Javadoc
/**
 * Main program for the speech server.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SphinxRecEngineFactory  implements BeanFactoryAware {  

    private static Logger logger = Logger.getLogger(SphinxRecEngineFactory.class);
	private BeanFactory beanFactory;
	
	//s4 rec engine properties (constructor args)
    private String recordingFilePath = "";
    private boolean recordingEnabled =false;  
	
    //searchmanager properties (constructor args)
    private boolean showTokenCount= true;
    private double relativeWordBeamWidth = 0;
    private int growSkipInterval = 0;
    private boolean checkStateOrder=true;
    private boolean buildWordLattice=true;
    private int maxLatticeEdges=1000;
    private float acousticLookaheadFrames=1.0f;
    private boolean keepAllTokens=false;

    //Decoder properties (constructor args)
    private boolean fireNonFinalResults =false;
    private boolean autoAllocate=false;;
    private int featureBlockSize = 1000;


    /**
     * Startup.
     */
    public void startup() {
        logger.info("Starting up the sphinx rec engine Factory...");

    }

	public RecEngine createSphinxRecEngine(boolean grammar) {

	    SphinxRecEngine recEngine = null;
	    if (grammar) {
	    	 recEngine =(SphinxRecEngine) this.beanFactory.getBean("s4RecEngineGrammar");
	    } else {
	    	 recEngine =(SphinxRecEngine) this.beanFactory.getBean("s4RecEngineLm");
	    }

	
	    return recEngine;
    }
    
    /**
     * Shutdown.
     */
    public void shutdown() {
        logger.info("Shutting down the main Server...");    
    }


	@Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
	      this.beanFactory = beanFactory;	    
    }

	/**
     * @return the showTokenCount
     */
    public boolean isShowTokenCount() {
    	return showTokenCount;
    }

	/**
     * @param showTokenCount the showTokenCount to set
     */
    public void setShowTokenCount(boolean showTokenCount) {
    	this.showTokenCount = showTokenCount;
    }

	/**
     * @return the relativeWordBeamWidth
     */
    public double getRelativeWordBeamWidth() {
    	return relativeWordBeamWidth;
    }

	/**
     * @param relativeWordBeamWidth the relativeWordBeamWidth to set
     */
    public void setRelativeWordBeamWidth(double relativeWordBeamWidth) {
    	this.relativeWordBeamWidth = relativeWordBeamWidth;
    }

	/**
     * @return the growSkipInterval
     */
    public int getGrowSkipInterval() {
    	return growSkipInterval;
    }

	/**
     * @param growSkipInterval the growSkipInterval to set
     */
    public void setGrowSkipInterval(int growSkipInterval) {
    	this.growSkipInterval = growSkipInterval;
    }

	/**
     * @return the checkStateOrder
     */
    public boolean isCheckStateOrder() {
    	return checkStateOrder;
    }

	/**
     * @param checkStateOrder the checkStateOrder to set
     */
    public void setCheckStateOrder(boolean checkStateOrder) {
    	this.checkStateOrder = checkStateOrder;
    }

	/**
     * @return the buildWordLattice
     */
    public boolean isBuildWordLattice() {
    	return buildWordLattice;
    }

	/**
     * @param buildWordLattice the buildWordLattice to set
     */
    public void setBuildWordLattice(boolean buildWordLattice) {
    	this.buildWordLattice = buildWordLattice;
    }

	/**
     * @return the maxLatticeEdges
     */
    public int getMaxLatticeEdges() {
    	return maxLatticeEdges;
    }

	/**
     * @param maxLatticeEdges the maxLatticeEdges to set
     */
    public void setMaxLatticeEdges(int maxLatticeEdges) {
    	this.maxLatticeEdges = maxLatticeEdges;
    }

	/**
     * @return the acousticLookaheadFrames
     */
    public float getAcousticLookaheadFrames() {
    	return acousticLookaheadFrames;
    }

	/**
     * @param acousticLookaheadFrames the acousticLookaheadFrames to set
     */
    public void setAcousticLookaheadFrames(float acousticLookaheadFrames) {
    	this.acousticLookaheadFrames = acousticLookaheadFrames;
    }

	/**
     * @return the keepAllTokens
     */
    public boolean isKeepAllTokens() {
    	return keepAllTokens;
    }

	/**
     * @param keepAllTokens the keepAllTokens to set
     */
    public void setKeepAllTokens(boolean keepAllTokens) {
    	this.keepAllTokens = keepAllTokens;
    }

	/**
     * @return the fireNonFinalResults
     */
    public boolean isFireNonFinalResults() {
    	return fireNonFinalResults;
    }

	/**
     * @param fireNonFinalResults the fireNonFinalResults to set
     */
    public void setFireNonFinalResults(boolean fireNonFinalResults) {
    	this.fireNonFinalResults = fireNonFinalResults;
    }

	/**
     * @return the autoAllocate
     */
    public boolean isAutoAllocate() {
    	return autoAllocate;
    }

	/**
     * @param autoAllocate the autoAllocate to set
     */
    public void setAutoAllocate(boolean autoAllocate) {
    	this.autoAllocate = autoAllocate;
    }

	/**
     * @return the featureBlockSize
     */
    public int getFeatureBlockSize() {
    	return featureBlockSize;
    }

	/**
     * @param featureBlockSize the featureBlockSize to set
     */
    public void setFeatureBlockSize(int featureBlockSize) {
    	this.featureBlockSize = featureBlockSize;
    }

	/**
     * @return the recordingFilePath
     */
    public String getRecordingFilePath() {
    	return recordingFilePath;
    }

	/**
     * @param recordingFilePath the recordingFilePath to set
     */
    public void setRecordingFilePath(String recordingFilePath) {
    	this.recordingFilePath = recordingFilePath;
    }

	/**
     * @return the recordingEnabled
     */
    public boolean isRecordingEnabled() {
    	return recordingEnabled;
    }

	/**
     * @param recordingEnabled the recordingEnabled to set
     */
    public void setRecordingEnabled(boolean recordingEnabled) {
    	this.recordingEnabled = recordingEnabled;
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
    
}