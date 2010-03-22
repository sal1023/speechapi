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
package com.spokentech.speechdown.common;


import java.util.ArrayList;
import java.util.logging.Logger;

import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

import edu.cmu.sphinx.result.Result;

import com.spokentech.speechdown.common.rule.RuleMatch;
import com.spokentech.speechdown.common.rule.SimpleNLRuleHandler;

/**
 * Represents the result of a completed recognition request.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RecognitionResultJsapi extends RecognitionResult{

    private static Logger _logger = Logger.getLogger(RecognitionResultJsapi.class.getName());
    
    private RuleGrammar _ruleGrammar;
 


	/**
     * TODOC
     */
    public RecognitionResultJsapi() { 
        _ruleMatches = new ArrayList<RuleMatch>();
        _text = new String();
    }
    

    
    /**
     * TODOC
     * @param rawResult
     * @throws NullPointerException
     */
    public RecognitionResultJsapi(String textResult) {
    	noGrammar = true;
        _text = textResult;
        _ruleGrammar = null;
        commonInit();
    }
    
    /**
     * TODOC
     * @param rawResult
     * @throws NullPointerException
     */
    public RecognitionResultJsapi(String textResult, RuleGrammar ruleGrammar) throws NullPointerException {
        _text = textResult;
        _ruleGrammar = ruleGrammar;
        noGrammar = false;
        commonInit();
    }


    /**
     * TODOC
     * @param rawResult
     * @throws NullPointerException
     */
    public RecognitionResultJsapi(Result rawResult, RuleGrammar ruleGrammar) throws NullPointerException {

        setNewResult(rawResult,ruleGrammar);
    }
    
    public RecognitionResultJsapi(String textResult, double confidence) {
    	cflag = true;
    	this.confidence = confidence;
    	noGrammar = true;
        _text = textResult;
        _ruleGrammar = null;
        commonInit();
    	
    }



	public void setNewResult(Result r, RuleGrammar ruleGrammar) {
        _rawResult = r;
        _ruleGrammar =ruleGrammar;
        noGrammar = false;
        if (_rawResult != null) {
            _text = _rawResult.getBestFinalResultNoFiller();
            commonInit();
        }
    }
    
    private void commonInit() {
        oog = false;
        if (_text.equals(OUTOFGRAMMAR)) {
            oog = true;
        }
        if (_text != null && (_text = _text.trim()).length() > 0 && _ruleGrammar != null && !oog) {
            try {
                RuleParse ruleParse = _ruleGrammar.parse(_text, null);
                _ruleMatches = SimpleNLRuleHandler.getRuleMatches(ruleParse);
            } catch (GrammarException e) {
                _logger.info("GrammarException encountered! "+ e.getLocalizedMessage());
            }
        }
    }
    
}
