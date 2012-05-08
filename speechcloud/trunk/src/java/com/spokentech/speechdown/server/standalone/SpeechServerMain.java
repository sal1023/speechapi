/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.standalone;


import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.spokentech.speechdown.server.recog.RecEngine;
import com.spokentech.speechdown.server.util.pool.SpringSphinxRecEngineFactory;


// TODO: Auto-generated Javadoc
/**
 * Main program for the speech server.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SpeechServerMain implements BeanFactoryAware {

    private static Logger logger = Logger.getLogger(SpeechServerMain.class);
	private Map<String, SpeechThread> recognizers = new HashMap<String, SpeechThread>();   
	private int poolSize;
	private String outputFormat;

	private BeanFactory beanFactory;
	public static ClassPathXmlApplicationContext context;	

    private SpringSphinxRecEngineFactory sphinxRecEngineFactory;

	/**
     * Startup.
     */
    public void startup() {
        logger.info("Starting up the main Server...");
   


		//Create a set of recognizers and the supporting components 
        for (int i=1; i<=poolSize; i++) { 
        	
        	
            RecEngine recEngine = sphinxRecEngineFactory.createSphinxRecEngine(false);

  		   	SpeechWorker speechWorker =(SpeechWorker) this.beanFactory.getBean("speechWorker");

  		    SpeechThread worker = new SpeechThread(recEngine,speechWorker,outputFormat);

        	String id = Integer.toString(i);
        	recognizers.put(id, worker);
           (new Thread(worker)).start();
        } 
        try {
    	    Thread.sleep(1000000);
        } catch (InterruptedException e) {
    	    // TODO Auto-generated catch block
    	    e.printStackTrace();
        }
    }


    
    /**
     * Shutdown.
     */
    public void shutdown() {
        logger.info("Shutting down the main Server...");    
    }
 

    /**
     * @return the poolSize
     */
    public int getPoolSize() {
    	return poolSize;
    }

	/**
     * @param poolSize the poolSize to set
     */
    public void setPoolSize(int poolSize) {
    	this.poolSize = poolSize;
    }
    
	/**
     * @return the outputFormat
     */
    public String getOutputFormat() {
    	return outputFormat;
    }



	/**
     * @param outputFormat the outputFormat to set
     */
    public void setOutputFormat(String outputFormat) {
    	this.outputFormat = outputFormat;
    }
    
    /**
     * The main method.
     * 
     * @param args the arguments
     */
    public static void main(String[] args) {
       context = new ClassPathXmlApplicationContext(args[0]);
    }

	@Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
	      this.beanFactory = beanFactory;	    
    }
	


    
    /**
     * @return the sphinxRecEngineFactory
     */
    public SpringSphinxRecEngineFactory getSphinxRecEngineFactory() {
    	return sphinxRecEngineFactory;
    }

	/**
     * @param sphinxRecEngineFactory the sphinxRecEngineFactory to set
     */
    public void setSphinxRecEngineFactory(SpringSphinxRecEngineFactory sphinxRecEngineFactory) {
    	this.sphinxRecEngineFactory = sphinxRecEngineFactory;
    }
    
}
