/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.activation.DataHandler;
import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.speech.recognition.GrammarException;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;
import org.apache.log4j.Logger;
import org.jvnet.staxex.StreamingDataHandler;

import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.Utterance;
import com.spokentech.speechdown.common.Utterance.OutputFormat;
import com.spokentech.speechdown.server.domain.SpeechRequestDTO;
import com.spokentech.speechdown.server.recog.RecEngine;

import com.spokentech.speechdown.server.util.pool.SphinxRecEngineFactory;

public class RecognizerService {

    static Logger _logger = Logger.getLogger(RecognizerService.class);

    static private final String NO_PREFIX = "";
    static private final String LM_PREFIX = "lm-";
    static private final String GRAMMAR_PREFIX = "grammar-";
    
	private SphinxRecEngineFactory recEngineFactory;
	
	private ObjectPool _lmRecognizerPool;
	private ObjectPool _grammarRecognizerPool;
    //private ObjectPool _recognizerPool;
	
	private int lmPoolSize;
	private int grammarPoolSize;
 
	public int getGrammarPoolSize() {
		return grammarPoolSize;
	}

	public void setGrammarPoolSize(int grammarPoolSize) {
		this.grammarPoolSize = grammarPoolSize;
	}

	/**
     * @return the poolSize
     */
    public int getLmPoolSize() {
    	return lmPoolSize;
    }

	/**
     * @param poolSize the poolSize to set
     */
    public void setLmPoolSize(int poolSize) {
    	this.lmPoolSize = poolSize;
    }

	/**
     * @return the recEngineFactory
     */
    public SphinxRecEngineFactory getRecEngineFactory() {
    	return recEngineFactory;
    }

	/**
     * @param recEngineFactory the recEngineFactory to set
     */
    public void setRecEngineFactory(SphinxRecEngineFactory recEngineFactory) {
    	this.recEngineFactory = recEngineFactory;
    }


	public void startup() {
		
		GenericObjectPool.Config config = new GenericObjectPool.Config();
		config.maxActive                        = lmPoolSize;
	    config.maxIdle                          = -1;
        config.maxWait                          = 200;
        config.minEvictableIdleTimeMillis       = -1;
        config.minIdle                          = config.maxActive;
        config.numTestsPerEvictionRun           = -1;
        //config.softMinEvictableIdleTimeMillis   = -1;
        config.testOnBorrow                     = false;
        config.testOnReturn                     = false;
        config.testWhileIdle                    = false;
        config.timeBetweenEvictionRunsMillis    = -1;
        config.whenExhaustedAction              = GenericObjectPool.WHEN_EXHAUSTED_FAIL;
		
    	_lmRecognizerPool =  new GenericObjectPool(recEngineFactory.createLmPoolFactory() ,config);
    	
		config.maxActive                        = grammarPoolSize;
    	_grammarRecognizerPool = new GenericObjectPool(recEngineFactory.createGrammarPoolFactory(),config);
 
    	
    	 /**
         * Initializes an {@link org.apache.commons.pool.ObjectPool} by borrowing each object from the
         * pool (thereby triggering activation) and then returning all the objects back to the pool. 
         * @param pool the object pool to be initialized.
         * @throws InstantiationException if borrowing (or returning) an object from the pool triggers an exception.
         */
        try {
            List<Object> objects = new ArrayList<Object>();
            while (true) try {
                objects.add(_lmRecognizerPool.borrowObject());
            } catch (NoSuchElementException e){
                // ignore, max active reached
                break;
            }
            for (Object obj : objects) {
            	_lmRecognizerPool.returnObject(obj);
            }
        } catch (Exception e) {
            try {
            	_lmRecognizerPool.close();
            } catch (Exception e1) {
                _logger.warn("Encounter expception while attempting to close object pool!", e1);
            }
            e.printStackTrace();
            //throw (InstantiationException) new InstantiationException(e.getMessage()).initCause(e);
        }
        try {
            List<Object> objects = new ArrayList<Object>();
            while (true) try {
                objects.add(_grammarRecognizerPool.borrowObject());
            } catch (NoSuchElementException e){
                // ignore, max active reached
                break;
            }
            for (Object obj : objects) {
            	_grammarRecognizerPool.returnObject(obj);
            }
        } catch (Exception e) {
            try {
            	_grammarRecognizerPool.close();
            } catch (Exception e1) {
                _logger.warn("Encounter expception while attempting to close object pool!", e1);
            }
            e.printStackTrace();
            //throw (InstantiationException) new InstantiationException(e.getMessage()).initCause(e);
        }
    	
    }
	
	public void shutdown() {
	
	}
	
	
	public Utterance Recognize(DataHandler audio, String grammar) throws GrammarException, IOException {
		   
		
		_logger.debug("audio attachment content type: "+ audio.getContentType());
		_logger.debug("audio attachment name: "+ audio.getName());
		_logger.debug("audio attachment class name: "+audio.getClass().getCanonicalName());
    	InputStream stream = null;
    	AudioInputStream as = null;
        try {
            if (audio instanceof StreamingDataHandler) {
            	_logger.debug("Data handler is instance of streamdatdaHandler");
                stream = ((StreamingDataHandler) audio).readOnce();
            } else {
            	_logger.debug("Data hancler is not streamdatahandler");
                stream = audio.getInputStream();
            }
	        as = AudioSystem.getAudioInputStream(stream) ;
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }  

        Utterance results = Recognize(as, grammar);
        
        return results;		
	}

	public  Utterance Recognize(AudioInputStream as, String grammar) {
	    RecEngine rengine = null;
        try {
            rengine = (RecEngine) _grammarRecognizerPool.borrowObject();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        
        Utterance results = rengine.recognize(as,grammar);
        
        try {
        	_grammarRecognizerPool.returnObject(rengine);
        } catch (Exception e) {
	        // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
	    return results;
    }

	//grammar method
	public Utterance Recognize(InputStream as, String grammar, String mimeType, AFormat af, OutputFormat outMode, boolean doEndpointing, boolean cmnBatch, SpeechRequestDTO hr) {

		_logger.debug("Before borrow" + System.currentTimeMillis());
		

    	
        RecEngine rengine = null;
        try {

            rengine = (RecEngine) _grammarRecognizerPool.borrowObject();
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        
        _logger.debug("After borrow" + System.currentTimeMillis());
	
        
      
        Utterance results ;
        try {
           results = rengine.recognize(as,mimeType,grammar,af,outMode, doEndpointing, cmnBatch, hr);
        } catch (Exception e) {
	        _logger.warn("Excption occurred while processing recognition requets "+e.getLocalizedMessage());
	        
	        try {
	        	_grammarRecognizerPool.returnObject(rengine);
	        } catch (Exception ee) {
		        // TODO Auto-generated catch block
	            throw new RuntimeException(ee);
	        }
            throw new RuntimeException(e);
        }
        
        
        try {
        	_grammarRecognizerPool.returnObject(rengine);
        } catch (Exception e) {
	        // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
	    return results;		
	}

	
	//language model method (no grammar)
	public Utterance Recognize(InputStream as, String mimeType, AFormat af, OutputFormat outMode, boolean doEndpointing, boolean cmnBatch, SpeechRequestDTO hr) {
		_logger.debug("Before borrow" + System.currentTimeMillis());
		
        RecEngine rengine = null;
        try {

            rengine = (RecEngine) _lmRecognizerPool.borrowObject();
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        
        _logger.debug("After borrow" + System.currentTimeMillis());
	
        Utterance results ;
        try {
           results = rengine.recognize(as,mimeType,af, outMode, doEndpointing, cmnBatch,hr);
        } catch (Exception e) {
	        _logger.warn("Excption occurred while processing LM recognition requets "+e.getLocalizedMessage());

	        //return engine to pool
	        try {
	        	_lmRecognizerPool.returnObject(rengine);
	        } catch (Exception ee) {
		        // TODO Auto-generated catch block
	            throw new RuntimeException(ee);
	        }
            throw new RuntimeException(e);
        }
        
        try {
        	_lmRecognizerPool.returnObject(rengine);
        } catch (Exception e) {
	        // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
	    return results;	
    }

	public String Transcribe(InputStream audio, String mimeType, AFormat af, OutputFormat outMode, PrintWriter out, HttpServletResponse response, SpeechRequestDTO hr) {
		_logger.debug("Before borrow" + System.currentTimeMillis());
		
    	
        RecEngine rengine = null;
        try {

            rengine = (RecEngine) _lmRecognizerPool.borrowObject();
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        
        _logger.debug("After borrow" + System.currentTimeMillis());
	
        String results ;
        try {
             results = rengine.transcribe(audio,mimeType, af,outMode, out,response,  hr);
        } catch (Exception e) {
	        _logger.warn("Excption occurred while processing transcribe requests "+e.getLocalizedMessage());
	        e.printStackTrace();
	        //return the engine to pool
	        try {
	        	_lmRecognizerPool.returnObject(rengine);
	        } catch (Exception ee) {
		        // TODO Auto-generated catch block
	            throw new RuntimeException(ee);
	        }
            throw new RuntimeException(e);
        }

        try {
        	_lmRecognizerPool.returnObject(rengine);
        } catch (Exception e) {
	        // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
	    return results;	
    }
    
}
