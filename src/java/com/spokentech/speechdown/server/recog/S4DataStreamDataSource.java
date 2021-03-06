/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.recog;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.util.props.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.common.AFormat;

public class S4DataStreamDataSource extends BaseDataProcessor implements StreamDataSource {
	private static Logger _logger = Logger.getLogger(S4DataStreamDataSource.class);


    private InputStream dataStream;
    private ObjectInputStream s;
    private long totalValues = 0;
    private long sampleRate;


    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        //Logger logger = ps.getLogger();
        initialize();
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    
    /* (non-Javadoc)
     * @see com.spokentech.speechdown.server.recog.StreamDataSource#setInputStream(java.io.InputStream)
     */
    public void setInputStream(InputStream inputStream, String streamName) {
        this.dataStream = inputStream;
   
        try {
	        s = new ObjectInputStream(inputStream);
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
        
    }
    

    /* (non-Javadoc)
     * @see com.spokentech.speechdown.server.recog.StreamDataSource#getData()
     */
    public Data getData() throws DataProcessingException {
        getTimer().start();
        Data output = null;
    
        if (dataStream != null) {	
            try {
                output = (Data)s.readObject();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


            if (output instanceof DataEndSignal) {
                try {
                    closeDataStream();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            
            if (output == null) {
                _logger.debug("data is null");
                try {
                    closeDataStream();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
               showSignals(output);
               showData(output);
            }
        } else {
            _logger.debug("datastream is null");
        }
   

        getTimer().stop();
        totalValues = totalValues + getDataLength(output);
        //_logger.info("Total read in this frame: "+totalValues);
        
        return output;
    }

    long sstart;
    long sstop; 
    long dstart;
    long dstop; 
    private void showSignals(Data data) {

        if (data instanceof SpeechStartSignal) {
        	 sstart = System.currentTimeMillis();
            _logger.debug("<<<<<<<<<<<<<<< SpeechStartSignal encountered at "+ sstart);
        } else if (data instanceof SpeechEndSignal) {
       	     sstop = System.currentTimeMillis();
            _logger.debug("<<<<<<<<<<<<<<< SpeechEndSignal encountered at " + sstop + "it took " +(sstop-sstart) + "length of audio: "+ (totalValues*1000)/sampleRate);
        } else if (data instanceof DataStartSignal) {
       	     dstart = System.currentTimeMillis();
            _logger.debug("<<<<<<<<<<<<<<< DataStartSignal encountered at "+ dstart);
        } else if (data instanceof DataEndSignal) {
      	     dstop = System.currentTimeMillis();
            _logger.debug(">>>>>>>>>>>>>>> DataEndSignal encountered at " + dstop + "it took " +(dstop-dstart) + "length of audio: "+ (totalValues*1000)/sampleRate);
        }

    }
    
    
    private long getSampleRate(Data data) {
    	long sampleRate = 0;
        if (data instanceof DoubleData) {
        	DoubleData dd = (DoubleData) data;

        	sampleRate = dd.getSampleRate();
        } else if (data instanceof FloatData) {
        	FloatData fd = (FloatData) data;

        	sampleRate = fd.getSampleRate();
        }
        return sampleRate;
    } 
    private long getDataLength(Data data) {
    	long len = 0;
        if (data instanceof DoubleData) {
        	DoubleData dd = (DoubleData) data;
        	len = dd.getValues().length;
        } else if (data instanceof FloatData) {
        	FloatData fd = (FloatData) data;
        	len = fd.getValues().length;
        }
        return len;
    }
        
    private void showData(Data data) {

    	long len = 0;
        if (data instanceof DoubleData) {
        	DoubleData dd = (DoubleData) data;
        	_logger.trace(dd.toString());
        } else if (data instanceof FloatData) {
        	FloatData fd = (FloatData) data;
        	len = fd.getValues().length;
        	_logger.trace("FloatData: " + fd.getSampleRate() + "Hz, first sample #: " +
                    fd.getFirstSampleNumber() + ", collect time: " + fd.getCollectTime());
        }
        if (sampleRate == 0)
           sampleRate = getSampleRate(data);
    }

    public void closeDataStream() throws IOException {
    	_logger.debug("Closing data stream");
        if (dataStream != null) {
            dataStream.close();
        }
    }

	@Override
    public long getLengthInMs() {
		if (sampleRate == 0) {
			return 0;
	    } else {
	       return (1000*totalValues)/sampleRate;
	    }
    }

	@Override
    public void setInputStream(InputStream inputStream, String streamName, AFormat format) {
        setInputStream(inputStream,streamName);
        
        
    }

}

