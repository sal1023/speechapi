/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.client;

/**
 * Defines the methods required for handling prompt play events.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public interface PromptPlayListener {

    /**
     * TODOC
     */
    public void playCompleted();

    /**
     * TODOC
     */
    public void playInterrupted();

    /**
     * TODOC
     */
    public void playFailed(Exception cause);

}
