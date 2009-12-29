package com.spokentech.speechdown.common.sphinx;

import java.util.logging.Logger;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;

import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;


/**

 * Inserts a start speech right after the start data and a end speech right before the end data signals.  
 *
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class InsertSpeechSignalStage extends BaseDataProcessor {

    private static Logger _logger = Logger.getLogger(InsertSpeechSignalStage.class.getName());
    private Data endData = null;
    private SpeechStartSignal start = null;

    /**
     * TODOC
     */
    public InsertSpeechSignalStage() {
        super();
        // TODO Auto-generated constructor stub
        endData = null;
        start = null;
    }
    

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.frontend.BaseDataProcessor#getData()
     */
    @Override
    public Data getData() throws DataProcessingException {

    	if (start != null) {
    		SpeechStartSignal tmp = start;
    		start = null;
    		return tmp;
    	} else if (endData != null) {
    		Data tmp = endData;
    		endData = null;
    		return tmp;
    	} else {
    		Data data = getPredecessor().getData();
    	
	        if (data instanceof DataStartSignal) {
	        	start = new SpeechStartSignal();
	        } else if (data instanceof DataEndSignal) {
	        	SpeechEndSignal endSpeech = new SpeechEndSignal();
	        	endData = data;
	        	return endSpeech;
	        }
	
	        //showSignals(data);
	        //showData(data);
	        
	        return data;
    	}
    }

 
    
}
