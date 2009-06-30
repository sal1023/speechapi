package com.spokentech.speechdown.server;


import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.activation.DataHandler;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.speech.recognition.GrammarException;

import org.apache.commons.pool.ObjectPool;
import org.apache.log4j.Logger;
import org.jvnet.staxex.StreamingDataHandler;


import com.spokentech.speechdown.common.RecognitionResult;
import com.spokentech.speechdown.server.recog.AudioStreamDataSource;
import com.spokentech.speechdown.server.recog.GrammarLocation;
import com.spokentech.speechdown.server.recog.GrammarManager;
import com.spokentech.speechdown.server.recog.RecEngine;
import com.spokentech.speechdown.server.recog.SphinxRecEngineFactory;
import com.spokentech.speechdown.server.tts.SynthEngine;
import com.spokentech.speechdown.server.util.pool.AbstractPoolableObjectFactory;

import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;


public class RecognizerService {

    static Logger _logger = Logger.getLogger(RecognizerService.class);
	
    private ObjectPool _recognizerPool;
	private int poolSize;   
	
	private AbstractPoolableObjectFactory recEngineFactory;

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

	public void startup() {
	   try {
	    	_recognizerPool =  recEngineFactory.createObjectPool(poolSize);
        } catch (InstantiationException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }   
    }
	
	public void shutdown() {
	
	}
	
	
	public RecognitionResult Recognize(DataHandler audio, String grammar) throws GrammarException, IOException {
		   
		
		_logger.info("audio attachment content type: "+ audio.getContentType());
		_logger.info("audio attachment name: "+ audio.getName());
		_logger.info("audio attachment class name: "+audio.getClass().getCanonicalName());
    	InputStream stream = null;
    	AudioInputStream as = null;
        try {
            if (audio instanceof StreamingDataHandler) {
            	_logger.info("Data handler is instance of streamdatdaHandler");
                stream = ((StreamingDataHandler) audio).readOnce();
            } else {
            	_logger.info("Data hancler is not streamdatahandler");
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

        RecognitionResult results = Recognize(as, grammar);
        
        return results;		
	}

	public  RecognitionResult Recognize(AudioInputStream as, String grammar) {
	    RecEngine rengine = null;
        try {
            rengine = (RecEngine) _recognizerPool.borrowObject();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        
        RecognitionResult results = rengine.recognize(as,grammar);
        
        try {
	        _recognizerPool.returnObject(rengine);
        } catch (Exception e) {
	        // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
	    return results;
    }

	public RecognitionResult Recognize(InputStream as, String grammar, int sampleRate, boolean bigEndian, int bytesPerValue, Encoding encoding) {
	    RecEngine rengine = null;
        try {
            rengine = (RecEngine) _recognizerPool.borrowObject();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        
        RecognitionResult results = rengine.recognize(as,grammar,sampleRate,bigEndian,bytesPerValue,encoding);
        
        try {
	        _recognizerPool.returnObject(rengine);
        } catch (Exception e) {
	        // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
	    return results;		
	}
    
}
