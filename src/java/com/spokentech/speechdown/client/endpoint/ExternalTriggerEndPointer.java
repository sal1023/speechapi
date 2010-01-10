package com.spokentech.speechdown.client.endpoint;

import java.io.IOException;
import org.apache.log4j.Logger;

public class ExternalTriggerEndPointer extends EndPointerBase {

	private static Logger _logger = Logger.getLogger(ExternalTriggerEndPointer.class);
	
    private long totalSamplesRead = 0;
    protected int bytesPerRead = 3200;
    
    public void doEndpointing() {

        final int bytesToRead = 3200;
        byte[] samplesBuffer = new byte[bytesToRead];
        
    	while ((!speechEnded) && (!streamEndReached)) {
    		_logger.info("trying to read: " + samplesBuffer.length);
    		//int numBytesRead = astream.read(data, 0, data.length);
    		
            int read = 0;
            int totalRead = 0;

            do {
                try {
	                read = astream.read(samplesBuffer, totalRead, bytesToRead - totalRead);
                } catch (IOException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                }
                if (read > 0) {
                    totalRead += read;
                }
            } while (read != -1 && totalRead < bytesToRead);
            if (totalRead <= 0) {
                closeDataStream();
                speechEnded = true;
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
                closeDataStream();
            }
            
    		_logger.info(" ...read: " + totalRead +"flags (started/ended/streamEnded: "+speechStarted+speechEnded+streamEndReached);
    
    		if (totalRead > 0) {
    			//_logger.info(speechStarted+" "+speechEnded);   			
    			if (speechStarted) {
    				//always write the entire buffer (even if we find the end we can send a little extra back to the recognizer or if we found the start inside this
    				// buffer a little silence in the beginning should not hurt)
    				_logger.info("Writing "+totalRead + "bytes");
    				try {
    					ostream.write(samplesBuffer, 0, totalRead);
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
    			}
    		}
    	}

		_logger.info("Done! "+ totalSamplesRead);
		
		// close the input stream
		closeDataStream();
		
		// Close the output stream. 
		try {
			ostream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 	
    }

	@Override
    public boolean requiresServerSideEndPointing() {
	    return true;
    }



}
