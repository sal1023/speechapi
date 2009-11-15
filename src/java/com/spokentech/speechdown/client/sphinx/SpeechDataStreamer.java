package com.spokentech.speechdown.client.sphinx;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Map;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import org.apache.log4j.Logger;



/**

 * Takes a Sphinx4 frontend stream and streams it to a java output stream
 *
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SpeechDataStreamer  extends Thread{

    private static Logger _logger = Logger.getLogger(SpeechDataStreamer.class);
    
    private BaseDataProcessor frontEnd;
	private OutputStream out;
    private ObjectOutputStream dout;
	/**
     * TODOC
     */
    public SpeechDataStreamer() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    
    private void showSignals(Data data) {

        if (data instanceof SpeechStartSignal) {
            _logger.debug("streamer <<<<<<<<<<<<<<< SpeechStartSignal encountered!");
        } else if (data instanceof SpeechEndSignal) {
            _logger.debug("streamer <<<<<<<<<<<<<<< SpeechEndSignal encountered!");
        } else if (data instanceof DataStartSignal) {
            _logger.debug("streamer <<<<<<<<<<<<<<< DataStartSignal encountered!");
            infoDataStartSignal((DataStartSignal) data);
        } else if (data instanceof DataEndSignal) {
            _logger.debug("streamer >>>>>>>>>>>>>>> DataEndSignal encountered!");
        }

    }

    /** Handles the first element in a feature-stream. */
    private void infoDataStartSignal(DataStartSignal dataStartSignal) {
        Map<String, Object> dataProps = dataStartSignal.getProps();
        if (dataProps.containsKey(DataStartSignal.SPEECH_TAGGED_FEATURE_STREAM))
           _logger.info("SPEECH TAG FEATURE STREAM: "+dataProps.get(DataStartSignal.SPEECH_TAGGED_FEATURE_STREAM));
    }
    
    
    
    private void showData(Data data) {

        if (data instanceof DoubleData) {
        	DoubleData dd = (DoubleData) data;
        	double[] d = dd.getValues();

        	_logger.debug(dd.toString());
        	_logger.debug("Sending " + d.length + " values.  "+d[0]+ " "+d[d.length-1]);
        } else if (data instanceof FloatData) {
        	FloatData fd = (FloatData) data;
        	_logger.debug("FloatData: " + fd.getSampleRate() + "Hz, first sample #: " +
                    fd.getFirstSampleNumber() + ", collect time: " + fd.getCollectTime());
        	float[] d = fd.getValues();
        	_logger.debug("Sending " + d.length + " values.");
        	//for (float val: d) {
        	//	_logger.info(val);
        	//}
        }
    }

    public void startStreaming(BaseDataProcessor frontEnd, OutputStream out) throws IOException {

    	this.frontEnd = frontEnd;
    	this.out = out;
        this.dout = new ObjectOutputStream(out);
    	_logger.info("startStreaming...");
        start();
    }


    
    public void run() {

    	_logger.debug("start stream pulling");
    	boolean moreData = true;
    	while (moreData) {

    		Data data = frontEnd.getData();
    		showSignals(data);
    		showData(data);
    		//_logger.debug("SDS: "+data);
 
    		if (data != null) {
    			try {
    				dout.writeObject(data);
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    		} else {
    			_logger.info("Null data");
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
    	
    





}