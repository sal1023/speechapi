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
public class SpringSphinxRecEngineFactory  implements BeanFactoryAware {  

    private static Logger logger = Logger.getLogger(SpringSphinxRecEngineFactory.class);
	private BeanFactory beanFactory;


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
