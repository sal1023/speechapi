/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.client.endpoint;

import java.io.IOException;
import org.apache.log4j.Logger;

public class ExternalTriggerEndPointer extends EndPointerBase {

	

	private static Logger _logger = Logger.getLogger(ExternalTriggerEndPointer.class);
	
    private long totalSamplesRead = 0;
 
    
	public ExternalTriggerEndPointer(int bufferSize) {
	    super(bufferSize);
    }
	
	   
	public ExternalTriggerEndPointer() {
	    super();
    }

    
    public void doEndpointing() {

        //final int  = 32000;
        byte[] samplesBuffer = new byte[bytesToRead];
        
    	while ((!speechEnded) || (!streamEndReached)) {
    		_logger.debug("trying to read: " + samplesBuffer.length);
    		//int numBytesRead = astream.read(data, 0, data.length);
    		
            int read = 0;
            int totalRead = 0;

            do {
                try {
	                read = astream.read(samplesBuffer, totalRead, bytesToRead - totalRead);
                } catch (IOException e) {
	                // TODO Auto-generated catch block
                	streamEndReached = true;
	                e.printStackTrace();
                }
                if (read > 0) {
                    totalRead += read;
                }
            } while (read != -1 && totalRead < bytesToRead && (!streamEndReached));
            if (totalRead <= 0) {
                //closeInputDataStream();
            	streamEndReached = true;
            }
            // shrink incomplete frames
            if (totalRead < bytesToRead) {
                totalRead = (totalRead % 2 == 0)
                        ? totalRead + 2
                        : totalRead + 3;
                byte[] shrinkedBuffer = new byte[totalRead];
                System
                        .arraycopy(samplesBuffer, 0, shrinkedBuffer, 0,
                                totalRead);
                samplesBuffer = shrinkedBuffer;
                //closeInputDataStream();
            }
            
    		_logger.debug(" ...read: " + totalRead +"flags (started/ended/streamEnded: "+speechStarted+speechEnded+streamEndReached);
    
    		if (totalRead > 0) {
    			//_logger.debug(speechStarted+" "+speechEnded);   			
    			if (speechStarted) {
    				//always write the entire buffer (even if we find the end we can send a little extra back to the recognizer or if we found the start inside this
    				// buffer a little silence in the beginning should not hurt)
    				_logger.debug("Writing "+totalRead + "bytes");
    				try {
    					ostream.write(samplesBuffer, 0, totalRead);
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
    			}
    		}
    	}

		_logger.debug("Done! "+ totalSamplesRead);
		speechStarted = false;
		speechEnded = false;
		streamEndReached = false;
		
		
		// close the input stream
		//closeInputDataStream();
		//closeOutputDataStream();

		// Close the output stream. 
		try {
			ostream.flush();
			_logger.debug("flushed ostream!");
			ostream.close();
			_logger.debug("Closed ostream!");
			ostream = null;
		} catch (IOException e) {
			e.printStackTrace();
		} 	
    }

	@Override
    public boolean requiresServerSideEndPointing() {
	    return false;
    }



}
