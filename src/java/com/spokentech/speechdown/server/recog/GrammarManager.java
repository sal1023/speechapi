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

	/**
     * @return the baseGrammarDir
     */
    public String getBaseGrammarDir() {
    	return baseGrammarDir;
    }

	/**
     * @param baseGrammarDir the baseGrammarDir to set
     */
    public void setBaseGrammarDir(String baseGrammarDir) {
    	this.baseGrammarDir = baseGrammarDir;
    }

	private String grammarDir;
    private String baseGrammarDir;


    /**
     * TODOC
     * @param channelID 
     * @param baseGrammarDir 
     */
    public void startup() {
    	
        _grammarDir = new File(baseGrammarDir, grammarDir);
        
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
            _grammarDirUrl = _grammarDir.toURL();
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
