package com.spokentech.speechdown.client.sphinx;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;


/**

 * Takes a Sphinx4 frontend stream and streams it to a java output stream
 *
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SpeechDataStreamer  extends Thread{

    private static Logger _logger = Logger.getLogger(SpeechDataStreamer.class.getName());
    
    private BaseDataProcessor frontEnd;
	private OutputStream out;
    private ObjectOutputStream dout;
    
    boolean dataEnded = false;
    boolean speechEnded = false;
    
	/**
     * TODOC
     */
    public SpeechDataStreamer() {
        super();
        // TODO Auto-generated constructor stub
        _logger.setLevel(Level.FINE);
        
    }
    
    
    private void showSignals(Data data) {

        if (data instanceof SpeechStartSignal) {
            _logger.info("streamer <<<<<<<<<<<<<<< SpeechStartSignal encountered!");
        } else if (data instanceof SpeechEndSignal) {
            _logger.info("streamer <<<<<<<<<<<<<<< SpeechEndSignal encountered!");
            speechEnded = true;
        } else if (data instanceof DataStartSignal) {
            _logger.info("streamer <<<<<<<<<<<<<<< DataStartSignal encountered!");
            infoDataStartSignal((DataStartSignal) data);
        } else if (data instanceof DataEndSignal) {
            _logger.info("streamer >>>>>>>>>>>>>>> DataEndSignal encountered!");
            dataEnded = true;
        }

    }

    /** Handles the first element in a feature-stream. */
    private void infoDataStartSignal(DataStartSignal dataStartSignal) {
        Map<String, Object> dataProps = dataStartSignal.getProps();
        if (dataProps.containsKey(DataStartSignal.SPEECH_TAGGED_FEATURE_STREAM))
           _logger.fine("SPEECH TAG FEATURE STREAM: "+dataProps.get(DataStartSignal.SPEECH_TAGGED_FEATURE_STREAM));
    }
    
    
    
    private void showData(Data data) {

        if (data instanceof DoubleData) {
        	DoubleData dd = (DoubleData) data;
        	double[] d = dd.getValues();

        	_logger.fine(dd.toString());
        	_logger.fine("Sending " + d.length + " values.  "+d[0]+ " "+d[d.length-1]);
        } else if (data instanceof FloatData) {
        	FloatData fd = (FloatData) data;
        	_logger.fine("FloatData: " + fd.getSampleRate() + "Hz, first sample #: " +
                    fd.getFirstSampleNumber() + ", collect time: " + fd.getCollectTime());
        	float[] d = fd.getValues();
        	_logger.fine("Sending " + d.length + " values.");
        	//for (float val: d) {
        	//	_logger.info(val);
        	//}
        }
    }

    public void startStreaming(BaseDataProcessor frontEnd, OutputStream out) throws IOException {

    	this.frontEnd = frontEnd;
    	this.out = out;
        this.dout = new ObjectOutputStream(out);
		this.dout.flush();
    	_logger.info("startStreaming...");
        start();
    }


    
    public void run() {
    	long t1 = 0;
    	long t2 = 0;
    	long t3 = 0;
    	_logger.info("start stream pulling");
    	boolean moreData = true;
    	while (moreData) {
   		    t1 = System.nanoTime();
    		Data data = frontEnd.getData();
    		showSignals(data);
    		showData(data);
    		t3 = t2;
    		t2 = System.nanoTime();
    		_logger.fine("Data Streamer: time to get from front end: "+(t2-t1) + "  time to write to stream"+ (t1-t3));
 
    		if (data != null) {
    			try {
    				dout.writeObject(data);

    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    			if (speechEnded || dataEnded) {
        			moreData=false;
        			speechEnded = false;
        			dataEnded = false;
        			closeOutputStream();
    			}
    		} else {
    			_logger.info("Null data");
    			moreData=false;
    			closeOutputStream();
    		}
    		
       		/*if (data instanceof DataEndSignal) {
    			moreData=false;
    			try {
    				dout.flush();
					out.flush();
	                dout.close();
	                out.close();
                } catch (IOException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                }
    		}*/
    	}
    }
    	
    private void closeOutputStream() {
		try {
			dout.flush();
			out.flush();
	        dout.close();
	        out.close();
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }
    }




}
