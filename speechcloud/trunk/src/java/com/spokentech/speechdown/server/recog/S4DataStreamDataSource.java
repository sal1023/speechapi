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
                System.out.println("data is null");
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
            System.out.println("datastream is null");
        }
   

        getTimer().stop();
        return output;
    }

    private void showSignals(Data data) {

        if (data instanceof SpeechStartSignal) {
            _logger.info("<<<<<<<<<<<<<<< SpeechStartSignal encountered!");
        } else if (data instanceof SpeechEndSignal) {
            _logger.info("<<<<<<<<<<<<<<< SpeechEndSignal encountered!");
        } else if (data instanceof DataStartSignal) {
            _logger.info("<<<<<<<<<<<<<<< DataStartSignal encountered!");
        } else if (data instanceof DataEndSignal) {
            _logger.info(">>>>>>>>>>>>>>> DataEndSignal encountered!");
        }

    }
    
    
    private void showData(Data data) {

        if (data instanceof DoubleData) {
        	DoubleData dd = (DoubleData) data;
        	_logger.info(dd.toString());
        } else if (data instanceof FloatData) {
        	FloatData fd = (FloatData) data;
        	_logger.info("FloatData: " + fd.getSampleRate() + "Hz, first sample #: " +
                    fd.getFirstSampleNumber() + ", collect time: " + fd.getCollectTime());
        }
    }

    private void closeDataStream() throws IOException {
    	System.out.println("Closing data stream");
        if (dataStream != null) {
            dataStream.close();
        }
    }

}

