package com.spokentech.speechdown.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

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
import com.spokentech.speechdown.server.recog.RecEngine;
import com.spokentech.speechdown.server.util.pool.AbstractPoolableObjectFactory;

public class RecognizerService {

    static Logger _logger = Logger.getLogger(RecognizerService.class);
    
    static private final String AUDIO_INPUT_GRAMMAR_PREFIX = "audio-";
    static private final String FEATURE_INPUT_GRAMMAR_PREFIX = "feature-";
    static private final String AUDIO_INPUT_LM_PREFIX = "audio-lm-";
    static private final String FEATURE_INPUT_LM_PREFIX = "feature-lm-";
    static private final String AUDIO_INPUT_LM_WITH_EP_PREFIX = "audio-lm-ep-";
    
    private ObjectPool _audioRecognizerPool;
    private ObjectPool _featureRecognizerPool;
    
    private ObjectPool _audioLmRecognizerPool;
    private ObjectPool _featureLmRecognizerPool;
    
    private ObjectPool _audioLmEpRecognizerPool;
    
	private int featureInputPoolSize;   
	private int audioInputPoolSize;
	
	private int featureLMInputPoolSize;   
	private int audioLMInputPoolSize;
	
	private int audioLMEPInputPoolSize; 
	
	/**
     * @return the audioLMEPInputPoolSize
     */
    public int getAudioLMEPInputPoolSize() {
    	return audioLMEPInputPoolSize;
    }

	/**
     * @param audioLMEPInputPoolSize the audioLMEPInputPoolSize to set
     */
    public void setAudioLMEPInputPoolSize(int audioLMEPInputPoolSize) {
    	this.audioLMEPInputPoolSize = audioLMEPInputPoolSize;
    }

	/**
     * @return the featureLMInputPoolSize
     */
    public int getFeatureLMInputPoolSize() {
    	return featureLMInputPoolSize;
    }

	/**
     * @param featureLMInputPoolSize the featureLMInputPoolSize to set
     */
    public void setFeatureLMInputPoolSize(int featureLMInputPoolSize) {
    	this.featureLMInputPoolSize = featureLMInputPoolSize;
    }

	/**
     * @return the audioLMInputPoolSize
     */
    public int getAudioLMInputPoolSize() {
    	return audioLMInputPoolSize;
    }

	/**
     * @param audioLMInputPoolSize the audioLMInputPoolSize to set
     */
    public void setAudioLMInputPoolSize(int audioLMInputPoolSize) {
    	this.audioLMInputPoolSize = audioLMInputPoolSize;
    }


  
	private AbstractPoolableObjectFactory recEngineFactory;
    
	/**
     * @return the featureInputPoolSize
     */
    public int getFeatureInputPoolSize() {
    	return featureInputPoolSize;
    }

	/**
     * @param featureInputPoolSize the featureInputPoolSize to set
     */
    public void setFeatureInputPoolSize(int featureInputPoolSize) {
    	this.featureInputPoolSize = featureInputPoolSize;
    }

	/**
     * @return the audioInputPoolSize
     */
    public int getAudioInputPoolSize() {
    	return audioInputPoolSize;
    }

	/**
     * @param audioInputPoolSize the audioInputPoolSize to set
     */
    public void setAudioInputPoolSize(int audioInputPoolSize) {
    	this.audioInputPoolSize = audioInputPoolSize;
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
	    	_audioRecognizerPool =  recEngineFactory.createObjectPool(audioInputPoolSize,AUDIO_INPUT_GRAMMAR_PREFIX);
	    	_featureRecognizerPool =  recEngineFactory.createObjectPool(featureInputPoolSize,FEATURE_INPUT_GRAMMAR_PREFIX);
	    	_audioLmRecognizerPool =  recEngineFactory.createObjectPool(audioLMInputPoolSize,AUDIO_INPUT_LM_PREFIX);
	    	_featureLmRecognizerPool =  recEngineFactory.createObjectPool(featureLMInputPoolSize,FEATURE_INPUT_LM_PREFIX);
	    	_audioLmEpRecognizerPool =  recEngineFactory.createObjectPool(audioLMEPInputPoolSize,AUDIO_INPUT_LM_WITH_EP_PREFIX);
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
            rengine = (RecEngine) _audioRecognizerPool.borrowObject();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        
        RecognitionResult results = rengine.recognize(as,grammar);
        
        try {
	        _audioRecognizerPool.returnObject(rengine);
        } catch (Exception e) {
	        // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
	    return results;
    }

	//grammar method
	public RecognitionResult Recognize(InputStream as, String grammar, String mimeType, int sampleRate, boolean bigEndian, int bytesPerValue, Encoding encoding) {

		_logger.debug("Before borrow" + System.currentTimeMillis());
		
        ObjectPool pool = _audioRecognizerPool;
    	if ((mimeType.equals("audio/x-wav")) ||
	        (mimeType.equals("audio/x-s4audio"))) {
    		pool = _audioRecognizerPool;
    	} else if (mimeType.equals("audio/x-s4feature")) {
    		pool = _featureRecognizerPool;
    	} else {
    		_logger.warn("Unrecognized mimeType: "+mimeType);  
    	}
    	
        RecEngine rengine = null;
        try {

            rengine = (RecEngine) pool.borrowObject();
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        
        _logger.debug("After borrow" + System.currentTimeMillis());
	
        
        RecognitionResult results = rengine.recognize(as,mimeType,grammar,sampleRate,bigEndian,bytesPerValue,encoding);
        
        try {
	        pool.returnObject(rengine);
        } catch (Exception e) {
	        // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
	    return results;		
	}

	
	//language model method (no grammar)
	public String Recognize(InputStream as, String mimeType, int sampleRate, boolean bigEndian, int bytesPerValue, Encoding encoding) {

		_logger.debug("Before borrow" + System.currentTimeMillis());
		
        ObjectPool pool = _audioRecognizerPool;
    	if ((mimeType.equals("audio/x-wav")) ||
	        (mimeType.equals("audio/x-s4audio"))) {
    		pool = _audioLmRecognizerPool;
    	} else if (mimeType.equals("audio/x-s4feature")) {
    		pool = _featureLmRecognizerPool;
    	} else {
    		_logger.warn("Unrecognized mimeType: "+mimeType);  
    	}
    	
        RecEngine rengine = null;
        try {

            rengine = (RecEngine) pool.borrowObject();
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        
        _logger.debug("After borrow" + System.currentTimeMillis());
	
        
        String results = rengine.recognize(as,mimeType,sampleRate,bigEndian,bytesPerValue,encoding);
        
        try {
	        pool.returnObject(rengine);
        } catch (Exception e) {
	        // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
	    return results;	
    }

	public String Transcribe(InputStream audio, String mimeType, int sampleRate, boolean bigEndian,
            int bytesPerValue, Encoding encoding, PrintWriter out) {
		_logger.debug("Before borrow" + System.currentTimeMillis());
		
        ObjectPool pool = _audioLmEpRecognizerPool;
    	if (!mimeType.equals("audio/x-wav")) {
    		_logger.warn("Unrecognized mimeType: "+mimeType);  
    	}
    	
        RecEngine rengine = null;
        try {

            rengine = (RecEngine) pool.borrowObject();
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        
        _logger.debug("After borrow" + System.currentTimeMillis());
	
        
        String results = rengine.transcribe(audio,mimeType,sampleRate,bigEndian,bytesPerValue,encoding,out);
        
        try {
	        pool.returnObject(rengine);
        } catch (Exception e) {
	        // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
	    return results;	
    }
    
}
