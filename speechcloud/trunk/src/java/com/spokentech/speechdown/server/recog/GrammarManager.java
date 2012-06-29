/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.recog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Manages the storage and retrieval of temporary grammar files on the file system.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class GrammarManager {

    //private Map<String, GrammarLocation> _grammars = new HashMap<String, GrammarLocation>();

    private File _grammarDir;
    private URL _grammarDirUrl;
    
    /**
     * @return the grammarDir
     */
    public String getGrammarDir() {
    	return grammarDir;
    }

	/**
     * @param grammarDir the grammarDir to set
     */
    public void setGrammarDir(String grammarDir) {
    	this.grammarDir = grammarDir;
    }

    public GrammarManager() {
    }
	public GrammarManager(String baseGrammarDir, String grammarDir) {
		this.grammarDir = grammarDir;
		this.baseDir = baseGrammarDir;
		startup();
	}

	/**
     * @return the baseGrammarDir
     */
    public String getBaseDir() {
    	return baseDir;
    }

	/**
     * @param baseGrammarDir the baseGrammarDir to set
     */
    public void setBaseDir(String baseGrammarDir) {
    	this.baseDir = baseGrammarDir;
    }

	private String grammarDir;
    private String baseDir;


    /**
     * TODOC
     * @param channelID 
     * @param baseGrammarDir 
     */
    public void startup() {
    	
    	if (baseDir == null) {
    		String baseGrammarDir = System.getProperty("java.io.tmpdir");
    		if ( !(baseGrammarDir.endsWith("/") || baseGrammarDir.endsWith("\\")) )
    			baseGrammarDir = baseGrammarDir + System.getProperty("file.separator");
    	}
        _grammarDir = new File(baseDir, grammarDir);
        
        // create directory if it does not exist
        if (!_grammarDir.exists()) {
            if (!_grammarDir.mkdirs()) {
                throw new IllegalArgumentException(
                    "Could not create directory: " + _grammarDir.getAbsolutePath());
            }
        }

        // make sure dir is actually a directory
        if (!_grammarDir.isDirectory()) {
            throw new IllegalArgumentException(
                "File specified was not a directory: " + _grammarDir.getAbsolutePath());
        }
        
   

        try {
            _grammarDirUrl = new URL("file:///"+_grammarDir.getAbsolutePath());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Specified directory not valid: " + _grammarDir.getAbsolutePath(), e);
        }
    }
    
	public void shutdown() {
		
	}
    
    /**
     * TODOC
     * @param grammarID
     * @param grammarText
     * @return
     * @throws IOException
     */
    // NOTE: could reduce sync scope but not necessary since generally single threaded access
    public synchronized GrammarLocation saveGrammar(String grammarText)
      throws IOException {

        // generate grammar name and location
        String grammarName = Long.toString(System.currentTimeMillis());
        GrammarLocation location = new GrammarLocation(_grammarDirUrl, grammarName);

        // write grammar to filesystem
        File grammarFile = new File(_grammarDir, location.getFilename());
        FileWriter fw = null;
        try {
            fw = new FileWriter(grammarFile);
            fw.write(grammarText);
        }catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	fw.flush();
            fw.close();
        }

        //if (grammarID != null && grammarID.length() > 0) {
        //    // store for future reference in session
        //    _grammars.put(grammarID, location);
        //}

        return location;
    }

    /**
     * TODOC
     * @param grammarID
     * @return
     */
    //public synchronized GrammarLocation getGrammarLocation(String grammarID) {
    //    return _grammars.get(grammarID);
    //}
    
}
