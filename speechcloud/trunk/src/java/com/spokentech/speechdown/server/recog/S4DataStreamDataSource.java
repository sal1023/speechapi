/*
 * Copyright 1999-2002 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package com.spokentech.speechdown.server.recog;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.frontend.util.AudioFileProcessListener;
import edu.cmu.sphinx.frontend.util.DataUtil;
import edu.cmu.sphinx.util.props.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;


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
     * @see com.spokentech.speechdown.server.recog.StreamDataSource#setInputStream(java.io.InputStream, java.lang.String, int, boolean, int, javax.sound.sampled.AudioFormat.Encoding)
     */
    public void setInputStream(InputStream inputStream, String streamName, int sampleRate, boolean bigEndian, int bytesPerValue, AudioFormat.Encoding encoding) {
	        setInputStream(inputStream,streamName);
    
        
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
        
        return output;
    }

    private void showSignals(Data data) {

        if (data instanceof SpeechStartSignal) {
            _logger.debug("<<<<<<<<<<<<<<< SpeechStartSignal encountered!");
        } else if (data instanceof SpeechEndSignal) {
            _logger.debug("<<<<<<<<<<<<<<< SpeechEndSignal encountered!");
        } else if (data instanceof DataStartSignal) {
             sampleRate = getSampleRate(data);
            _logger.debug("<<<<<<<<<<<<<<< DataStartSignal encountered!");
        } else if (data instanceof DataEndSignal) {
            _logger.debug(">>>>>>>>>>>>>>> DataEndSignal encountered!");
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
        	_logger.debug(dd.toString());
        } else if (data instanceof FloatData) {
        	FloatData fd = (FloatData) data;
        	len = fd.getValues().length;
        	_logger.debug("FloatData: " + fd.getSampleRate() + "Hz, first sample #: " +
                    fd.getFirstSampleNumber() + ", collect time: " + fd.getCollectTime());
        }

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

}

