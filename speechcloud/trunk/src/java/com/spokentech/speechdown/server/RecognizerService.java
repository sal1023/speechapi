package com.spokentech.speechdown.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.activation.DataHandler;
import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.speech.recognition.GrammarException;
import org.apache.commons.pool.ObjectPool;
import org.apache.log4j.Logger;
import org.jvnet.staxex.StreamingDataHandler;

import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.Utterance;
import com.spokentech.speechdown.common.Utterance.OutputFormat;
import com.spokentech.speechdown.server.domain.SpeechRequestDTO;
import com.spokentech.speechdown.server.recog.RecEngine;
import com.spokentech.speechdown.server.util.pool.AbstractPoolableObjectFactory;

public class RecognizerService {

    static Logger _logger = Logger.getLogger(RecognizerService.class);

    static private final String NO_PREFIX = "";
    static private final String LM_PREFIX = "lm-";
    static private final String GRAMMAR_PREFIX = "grammar-";
    
	private AbstractPoolableObjectFactory recEngineFactory;
	
	private ObjectPool _lmRecognizerPool;
	private ObjectPool _grammarRecognizerPool;
    //private ObjectPool _recognizerPool;
	
	private int poolSize;
 
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
     * @return the recEngineFactory
     */
    public AbstractPoolableObjectFactory getRecEngineFactory() {
    	return recEngineFactory;
    }

	/**
     * @param recEngineFactory the recEngineFactory to set
     */
    public void setRecEngineFactory(AbstractPoolableObjectFactory recEngineFactory) {
    	this.recEngineFactory = recEngineFactory;
    }


	public void startup() {
	   try {
	    	//_recognizerPool =  recEngineFactory.createObjectPool(poolSize,NO_PREFIX);
	    	_lmRecognizerPool =  recEngineFactory.createObjectPool(poolSize,LM_PREFIX);
	    	_grammarRecognizerPool =  recEngineFactory.createObjectPool(poolSize,GRAMMAR_PREFIX);

        } catch (InstantiationException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
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
