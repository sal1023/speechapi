/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.common.sphinx;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;

import com.spokentech.speechdown.common.SpeechEventListener;


/**

 * Monitors a stream of speech data being processed and broadcasts start-of-speech and end-of-speech events.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class SpeechDataMonitor extends BaseDataProcessor {

    private static Logger _logger = Logger.getLogger(SpeechDataMonitor.class.getName());

    private SpeechEventListener _speechEventListener = null;
    
    private boolean endFlag = false;
    
    private long start;

    /**
     * TODOC
     */
    public SpeechDataMonitor() {
        super();
        // TODO Auto-generated constructor stub
        //sal_logger.setLevel(Level.INFO);
        // The root logger's handlers default to INFO. We have to
        // crank them up. We could crank up only some of them
        // if we wanted, but we will turn them all up.
        //sal Handler[] handlers =  Logger.getLogger( "" ).getHandlers();
        //sal for ( int index = 0; index < handlers.length; index++ ) {
        //sal   handlers[index].setLevel( Level.INFO );
        // sal}


    }
    
    public void setSpeechEventListener(SpeechEventListener speechEventListener) {
        _speechEventListener = speechEventListener;
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.frontend.BaseDataProcessor#getData()
     */
    @Override
    public Data getData() throws DataProcessingException {
    	//if (endFlag) {
    		//endFlag = false;
    		//long duration = 0;
    		//DataEndSignal end = new DataEndSignal(duration);
        	//long time = System.currentTimeMillis();
            //_logger.fine(time+"  >>>>>>>>>>>>>>> Inserted DataEndSignal encountered! "+(time-start)+"  "+ (duration));

    		//return end;
    	//}
        Data data = getPredecessor().getData();

        if (data instanceof SpeechStartSignal) {
            broadcastSpeechStartSignal();

        } else if (data instanceof SpeechEndSignal) {
            broadcastSpeechEndSignal();
            //endFlag = true;
        } else if (data instanceof DataStartSignal) {
        	start = System.currentTimeMillis();
        	 _logger.fine(start+ " <<<<<<<<<<<<<<< DataStartSignal encountered!");
        } else if (data instanceof DataEndSignal) {
        	long time = System.currentTimeMillis();
        	 _logger.fine(time+"  >>>>>>>>>>>>>>> DataEndSignal encountered! "+(time-start));
        }
        return data;
    }
    
    private void broadcastSpeechStartSignal() {
    	long time = System.currentTimeMillis();
    	 _logger.fine(time+"  *************** SpeechStartSignal encountered! "+(time-start));
        if (_speechEventListener != null) {
            _speechEventListener.speechStarted();
        }
    }

    private void broadcastSpeechEndSignal() {
    	long time = System.currentTimeMillis();
    	 _logger.fine(time+ "  *************** SpeechEndSignal encountered!  "+(time-start));
        if (_speechEventListener != null) {
            _speechEventListener.speechEnded();
        }
    }

}
