/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.common.sphinx;

import java.util.logging.Logger;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;



/**

 * Identity Stage.  Input is unchanged and sent to output (identity operator).
 *
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class IdentityStage extends BaseDataProcessor {

    private static Logger _logger = Logger.getLogger(IdentityStage.class.getName());


    /**
     * TODOC
     */
    public IdentityStage() {
        super();
        // TODO Auto-generated constructor stub
    }
    

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.frontend.BaseDataProcessor#getData()
     */
    @Override
    public Data getData() throws DataProcessingException {
        Data data = getPredecessor().getData();
        showSignals(data);
        showData(data);
        return data;
    }

    private void showSignals(Data data) {

        if (data instanceof SpeechStartSignal) {
            _logger.fine("identity<<<<<<<<<<<<<<< SpeechStartSignal encountered!");
        } else if (data instanceof SpeechEndSignal) {
            _logger.fine("identity <<<<<<<<<<<<<<< SpeechEndSignal encountered!");
        } else if (data instanceof DataStartSignal) {
            _logger.fine("identity <<<<<<<<<<<<<<< DataStartSignal encountered!");
        } else if (data instanceof DataEndSignal) {
            _logger.fine("identity >>>>>>>>>>>>>>> DataEndSignal encountered!");
        }

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
        	_logger.fine("Sending " + d.length + " values.  "+d[0]+ " "+d[d.length-1]);
        	//for (float val: d) {
        	//	_logger.debug(val);
        	//}
        }
    }
    
    
}
