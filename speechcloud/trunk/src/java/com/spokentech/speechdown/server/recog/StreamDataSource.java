package com.spokentech.speechdown.server.recog;

import java.io.IOException;
import java.io.InputStream;

import com.spokentech.speechdown.client.util.AFormat;


import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;

public interface StreamDataSource {

	/**
	 * Sets the InputStream from which this StreamDataSource reads.
	 *
	 * @param inputStream the InputStream from which audio data comes
	 * @param streamName  the name of the InputStream
	 * @throws IOException 
	 */
	public void setInputStream(InputStream inputStream, String streamName);

	/**
	 * Sets the InputStream from which this StreamDataSource reads.
	 *
	 * @param inputStream the InputStream from which audio data comes
	 * @param streamName  the name of the InputStream
	 * @throws IOException 
	 */
	public void setInputStream(InputStream inputStream, String streamName, AFormat format);

	/**
	 * Reads and returns the next Data from the InputStream of StreamDataSource, return null if no data is read and end
	 * of file is reached.
	 *
	 * @return the next Data or <code>null</code> if none is available
	 * @throws edu.cmu.sphinx.frontend.DataProcessingException
	 *          if there is a data processing error
	 */
	public Data getData() throws DataProcessingException;
	
    public void closeDataStream() throws IOException;
    
    public long getLengthInMs();

}