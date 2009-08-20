/*
 * Cairo - Open source framework for control of speech media resources.
 *
 * Copyright (C) 2005-2006 SpeechForge - http://www.speechforge.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contact: ngodfredsen@users.sourceforge.net
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
 * Serves to create a pool of {@link org.speechforge.cairo.server.recog.sphinx.SphinxRecEngine} instances.
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

        return new SphinxRecEngine(_cm, grammarManager,prefixId, id++);
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