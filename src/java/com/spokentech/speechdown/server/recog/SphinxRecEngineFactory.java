/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.recog;


import java.net.MalformedURLException;
import java.net.URL;

import edu.cmu.sphinx.util.props.ConfigurationManager;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

import com.spokentech.speechdown.server.util.pool.AbstractPoolableObjectFactory;
import com.spokentech.speechdown.server.util.pool.ObjectPoolUtil;
import com.spokentech.speechdown.server.util.pool.PoolableObject;

/**
 * Serves to create a pool of {@link org.speechforge.cairo.PoolableSphinxRecEngine.recog.sphinx.SphinxRecEngine} instances.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class SphinxRecEngineFactory extends AbstractPoolableObjectFactory {

    private static Logger _logger = Logger.getLogger(SphinxRecEngineFactory.class);

    private  ConfigurationManager _cm;
    private GrammarManager grammarManager;
	private String sphinxConfigFile;
    private URL sphinxConfigUrl = null;
    private int id = 1;
    private String prefixId;
    
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

	private String recordingFilePath;
	private boolean recordingEnabled;
    

    /**
     * @return the sphinxConfigFile
     */
    public String getSphinxConfigFile() {
    	return sphinxConfigFile;
    }

	/**
     * @param sphinxConfigFile the sphinxConfigFile to set
     */
    public void setSphinxConfigFile(String sphinxConfigFile) {
    	this.sphinxConfigFile = sphinxConfigFile;
    }
    

	/**
     * @return the grammarManager
     */
    public GrammarManager getGrammarManager() {
    	return grammarManager;
    }

	/**
     * @param grammarManager the grammarManager to set
     */
    public void setGrammarManager(GrammarManager grammarManager) {
    	this.grammarManager = grammarManager;
    }
    

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
     */
    @Override
    public PoolableObject makeObject() throws Exception {

    	PoolableSphinxRecEngine s =  new PoolableSphinxRecEngine(_cm, grammarManager,prefixId, id++,recordingFilePath,recordingEnabled);
    	return s;
    }

    /**
     * TODOC
     * @param sphinxConfigURL
     * @param instances
     * @return
     * @throws InstantiationException if initializing the object pool triggers an exception.
     */
    public  ObjectPool createObjectPool(int instances,String prefixId)
      throws InstantiationException {
    	_logger.info("Creating a pool with prefix: "+prefixId+" of size: "+instances);
        
    	this.prefixId = prefixId;
    	this.id=1;
        if (_logger.isDebugEnabled()) {
            _logger.debug("creating new rec engine pool... instances: " + instances);
        }

        //PoolableObjectFactory factory = new SphinxRecEngineFactory(sphinxConfigURL);
        GenericObjectPool.Config config = ObjectPoolUtil.getGenericObjectPoolConfig(instances);

        ObjectPool objectPool = new GenericObjectPool(this, config);
        initPool(objectPool);
        return objectPool;
    }
    
	public void startup() {
		_logger.info("Starting up the sphinx rec engine Factory");

		try {
			sphinxConfigUrl = new URL(sphinxConfigFile);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

       if (sphinxConfigUrl == null) {
            throw new RuntimeException("Sphinx config file not found!");
       }
   	   _logger.info("sphinx config url "+ sphinxConfigUrl.getPath());
       _cm = new ConfigurationManager(sphinxConfigUrl);
	}
		
	public void shutdown() {
		_logger.info("Shutting down the sphinx rec engine Factory");
	}

}