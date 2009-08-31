package com.spokentech.speechdown.server;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;

import org.apache.commons.pool.ObjectPool;
import org.apache.log4j.Logger;
import com.spokentech.speechdown.server.tts.SynthEngine;
import com.spokentech.speechdown.server.util.pool.AbstractPoolableObjectFactory;

public class SynthesizerService {
	
    static Logger _logger = Logger.getLogger(SynthesizerService.class);
	
    private ObjectPool _synthesizerPool;  
	private File promptDir;
	private int poolSize;
    private String prefix;   
	private AbstractPoolableObjectFactory synthEngineFactory;
     
    /**
     * @return the synthEngineFactory
     */
    public AbstractPoolableObjectFactory getSynthEngineFactory() {
    	return synthEngineFactory;
    }

	/**
     * @param synthEngineFactory the synthEngineFactory to set
     */
    public void setSynthEngineFactory(AbstractPoolableObjectFactory synthEngineFactory) {
    	this.synthEngineFactory = synthEngineFactory;
    }

	/**
     * @return the promptDir
     */
    public File getPromptDir() {
    	return promptDir;
    }

	/**
     * @param promptDir the promptDir to set
     */
    public void setPromptDir(File promptDir) {
    	this.promptDir = promptDir;
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
     * @return the prefix
     */
    public String getPrefix() {
    	return prefix;
    }

	/**
     * @param prefix the prefix to set
     */
    public void setPrefix(String prefix) {
    	this.prefix = prefix;
    }


	public void startup() {
		System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
	    try {
	    	String prefix = null;  //not needed for synthesizer pool (but needed for recognizer pools)
	    	_synthesizerPool = synthEngineFactory.createObjectPool(poolSize,prefix);
        } catch (InstantiationException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }  
    }
	
	public void shutdown() {
		
	}

    public String ttsURL(String text, AudioFormat format, AudioFileFormat.Type fileType) {
 
    	File ttsFile = ttsFile(text,format,fileType);
        
        String url = prefix+ttsFile.getName();
        

        return url;
    }

    public File ttsFile(String text, AudioFormat format, AudioFileFormat.Type fileType) {
        SynthEngine synth = null;
        // borrow prompt generator
        try {
            synth = (SynthEngine) _synthesizerPool.borrowObject();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }

        // generate prompt
        File ttsFile = synth.generateAudio(text, promptDir,format,fileType);
        
        try {
        	_synthesizerPool.returnObject(synth);
        } catch (Exception e) {
	        // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }

        return ttsFile;
    }
    
    
}
