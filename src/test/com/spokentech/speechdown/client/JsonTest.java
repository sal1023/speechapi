/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.client;




import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.spokentech.speechdown.common.Utterance;




import junit.framework.TestCase;

public class JsonTest extends TestCase {

    private static Logger _logger = Logger.getLogger(JsonTest.class);
	private Gson gson;
    
    		
    		
	     protected void setUp() {
	 	    gson = new Gson();
	    
	     }

	 
	    
	    public void testPlay() {
	    	//String result = read();
        	//Utterance u = gson.fromJson(result, Utterance.class);   

	    }
	    
	  
  
}



