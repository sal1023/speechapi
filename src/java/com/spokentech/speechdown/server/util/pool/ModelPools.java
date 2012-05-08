/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.util.pool;

import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.sphinx.linguist.dictionary.FastDictionary;
import edu.cmu.sphinx.linguist.language.ngram.large.LargeTrigramModel;


// TODO: Auto-generated Javadoc
/**
 * Model Pools
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class ModelPools  {  

    private static Logger logger = Logger.getLogger(ModelPools.class);

    private Map<String,PooledAcousticModel> acoustic;
    private Map<String,LargeTrigramModel> language;
	private Map<String,FastDictionary> dictionary;

	/**
     * Startup.
     */
    public void startup() {
        logger.info("Starting up the sphinx model Factory...");
    }
    
    /**
     * Shutdown.
     */
    public void shutdown() {
        logger.info("Shutting down the main Server...");    
    }

	public void setAcoustic(Map<String,PooledAcousticModel> acoustic) {
		this.acoustic = acoustic;

		logger.info("**** a model size "+acoustic.size());
		for (Map.Entry<String, PooledAcousticModel> entry : acoustic.entrySet()) {
		    String key = entry.getKey();
		    PooledAcousticModel value = entry.getValue();
		    logger.info(key+ " "+value.getSampleRate()+ " "+value.getModel().toString());
		}


	}

	public Map<String,PooledAcousticModel> getAcoustic() {
		return acoustic;
	}

    
    public Map<String, LargeTrigramModel> getLanguage() {
		return language;
	}

	public void setLanguage(Map<String, LargeTrigramModel> language) {
		this.language = language;
	}

	public Map<String, FastDictionary> getDictionary() {
		return dictionary;
	}

	public void setDictionary(Map<String, FastDictionary> dictionary) {
		this.dictionary = dictionary;
	}

	
}
