/*
 * Cairo - Open source framework for control of speech media resources.
 *
 * Copyright (C) 2005-2006 SpeechForge - http://www.speechforge.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contact: ngodfredsen@users.sourceforge.net
 *
 */
package com.spokentech.speechdown.client;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.common.SpeechEventListener;


/**
 * Delegates method calls to an underlying {@link com.spokentech.speechdown.common.recog.SpeechEventListener} implementation.
 * Can be subclassed to intercept calls to the decorated object.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class SpeechEventListenerDecorator implements SpeechEventListener {

    private static Logger _logger = Logger.getLogger(SpeechEventListenerDecorator.class);

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
        _logger.debug("speechStarted()");
        if (_speechEventListener != null) {
            _speechEventListener.speechStarted();
        }
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.recog.RecogListener#noInputTimeout()
     */
    public void noInputTimeout() {
        _logger.debug("noInputTimeout()");
        if (_speechEventListener != null) {
            _speechEventListener.noInputTimeout();
        }
    }

	@Override
    public void speechEnded() {
        _logger.debug("speechStarted()");
        if (_speechEventListener != null) {
            _speechEventListener.speechEnded();
        }
    }

}
