/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.tts;



import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

import com.spokentech.speechdown.server.util.pool.AbstractPoolableObjectFactory;
import com.spokentech.speechdown.server.util.pool.ObjectPoolUtil;
import com.spokentech.speechdown.server.util.pool.PoolableObject;

/**
 * Serves to create a pool of {@link org.FreettsSynthEngine.cairo.server.tts.PromptGenerator} instances.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class FreettsSynthEngineFactory extends AbstractPoolableObjectFactory {

    private static Logger _logger = Logger.getLogger(FreettsSynthEngineFactory.class);

    private String voiceName;

    /**
     * @return the voiceName
     */
    public String getVoiceName() {
    	return voiceName;
    }

	/**
     * @param voiceName the voiceName to set
     */
    public void setVoiceName(String voiceName) {
    	this.voiceName = voiceName;
    }

	/**
     * TODOC
     * @param voiceName
     */
    public FreettsSynthEngineFactory() {

    }

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
     */
    @Override
    public PoolableObject makeObject() throws Exception {
        return new FreettsSynthEngine(voiceName);
    }

    /**
     * TODOC
     * @param instances
     * @return
     */
    public  ObjectPool createObjectPool(int instances,String prefix)
      throws InstantiationException {

        if (_logger.isDebugEnabled()) {
            _logger.debug("creating new prompt generator pool... instances: " + instances);
        }

        //PoolableObjectFactory factory = new FreettsSynthEngineFactory(voiceName);

        // TODO: adapt config to prompt generator constraints
        GenericObjectPool.Config config = ObjectPoolUtil.getGenericObjectPoolConfig(instances);
        ObjectPool objectPool = new GenericObjectPool(this, config);
        initPool(objectPool);
        return objectPool;
    }
    
	public void startup() {
		_logger.info("Starting up the freetts synth Factory");

	}
		
	public void shutdown() {
		_logger.info("Shutting down the freetts synth Factory");
	}

}