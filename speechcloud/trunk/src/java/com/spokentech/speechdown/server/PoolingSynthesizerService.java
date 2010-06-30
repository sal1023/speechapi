package com.spokentech.speechdown.server;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFileFormat.Type;

import org.apache.commons.pool.ObjectPool;
import org.apache.log4j.Logger;

import com.spokentech.speechdown.server.domain.SpeechRequestDTO;
import com.spokentech.speechdown.server.tts.SynthEngine;
import com.spokentech.speechdown.server.util.pool.AbstractPoolableObjectFactory;

public class PoolingSynthesizerService implements SynthesizerService {
	
    static Logger _logger = Logger.getLogger(PoolingSynthesizerService.class);
	
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

    /* (non-Javadoc)
     * @see com.spokentech.speechdown.server.SynthesizerService#ttsURL(java.lang.String, javax.sound.sampled.AudioFormat, javax.sound.sampled.AudioFileFormat.Type)
     */
    public String ttsURL(String text, AudioFormat format, String mime) {
 
    	File ttsFile = ttsFile(text,format,mime);
        
        String url = prefix+ttsFile.getName();
        

        return url;
    }

    /* (non-Javadoc)
     * @see com.spokentech.speechdown.server.SynthesizerService#ttsFile(java.lang.String, javax.sound.sampled.AudioFormat, javax.sound.sampled.AudioFileFormat.Type)
     */
    public File ttsFile(String text, AudioFormat format, String mime) {
        SynthEngine synth = null;
        // borrow prompt generator
        try {
            synth = (SynthEngine) _synthesizerPool.borrowObject();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        
        	AudioFileFormat.Type fileType= AudioFileFormat.Type.WAVE;
			if (mime.equals("audio/x-wav")) {
				fileType = AudioFileFormat.Type.WAVE; 
			} else if (mime.equals("audio/x-au")) {
				fileType = AudioFileFormat.Type.AU; 
			//} else if (mime.equals("audio/mpeg")) {
			//	fileType = AudioFileFormat.Type.; 
			} else {
				_logger.warn("Unsupported fileformat: "+mime);
			}
        

        // generate prompt
        File ttsFile = synth.generateAudio(text, promptDir,format,fileType);
        _logger.debug("Generated audio file: "+promptDir.getAbsolutePath() );
        
        try {
        	_synthesizerPool.returnObject(synth);
        } catch (Exception e) {
	        // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }

        return ttsFile;
    }
    
    
     /* (non-Javadoc)
     * @see com.spokentech.speechdown.server.SynthesizerService#streamTTS(java.lang.String, javax.sound.sampled.AudioFormat, javax.sound.sampled.AudioFileFormat.Type, java.lang.String, java.io.OutputStream)
     */
    public void streamTTS(String text, AudioFormat format, String mime, String voice, OutputStream out,SpeechRequestDTO hr) {

    	 File ttsFile = ttsFile(text,format,mime);

    	 try {
    		 DataInputStream in = new DataInputStream(new FileInputStream(ttsFile));

    		 byte[] buf = new byte[4 * 1024];  // 4K buffer
    		 int bytesRead;
    		 while ((bytesRead = in.read(buf)) != -1) {

    			 out.write(buf, 0, bytesRead);

    		 }
        	 in.close();
    	 } catch (IOException e) {
    		 // TODO Auto-generated catch block
    		 e.printStackTrace();
    	 }

     }
    
}
