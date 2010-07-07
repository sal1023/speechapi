/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.client;

import java.util.logging.Logger;

import com.spokentech.speechdown.common.SpeechEventListener;
import com.spokentech.speechdown.common.Utterance;


/**
 * Delegates method calls to an underlying {@link com.spokentech.speechdown.common.recog.SpeechEventListener} implementation.
 * Can be subclassed to intercept calls to the decorated object.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class SpeechEventListenerDecorator implements SpeechEventListener {

    private static Logger _logger = Logger.getLogger(SpeechEventListenerDecorator.class.getName());

    private SpeechEventListener _speechEventListener;

    /**
     * TODOC
     * @param recogListener 
     */
    public SpeechEventListenerDecorator(SpeechEventListener recogListener) {
        _speechEventListener = recogListener;
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.recog.RecogListener#speechStarted()
     */
    public void speechStarted() {
        _logger.fine("speechStarted()");
        if (_speechEventListener != null) {
            _speechEventListener.speechStarted();
        }
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.recog.RecogListener#noInputTimeout()
     */
    public void noInputTimeout() {
        _logger.fine("noInputTimeout()");
        if (_speechEventListener != null) {
            _speechEventListener.noInputTimeout();
        }
    }

	@Override
    public void speechEnded() {
        _logger.fine("speechEnded()");
        if (_speechEventListener != null) {
            _speechEventListener.speechEnded();
        }
    }

	@Override
    public void recognitionComplete(Utterance rr) {
        _logger.fine("***recognition complete()");
        if (_speechEventListener != null) {
            _speechEventListener.recognitionComplete(rr);
        }
    }

}
