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
import java.util.List;

import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

import edu.cmu.sphinx.result.Result;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.common.rule.RuleMatch;
import com.spokentech.speechdown.common.rule.SimpleNLRuleHandler;

/**
 * Represents the result of a completed recognition request.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RecognitionResult {

    private static Logger _logger = Logger.getLogger(RecognitionResult.class);
    
    private final static String tagRuleDelimiter = ":";
    private final static String OUTOFGRAMMAR = "<unk>";
    private boolean oog;

    private Result _rawResult;
    private RuleGrammar _ruleGrammar;
    private String _text;
    private List<RuleMatch> _ruleMatches;

    private boolean noGrammar = false;
    
    /**
     * @return the noGrammar
     */
    public boolean isNoGrammar() {
    	return noGrammar;
    }



	/**
     * TODOC
     */
    public RecognitionResult() { 
        _ruleMatches = new ArrayList<RuleMatch>();
        _text = new String();
    }
    

    
    /**
     * TODOC
     * @param rawResult
     * @throws NullPointerException
     */
    public RecognitionResult(String textResult) {
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
    public RecognitionResult(String textResult, RuleGrammar ruleGrammar) throws NullPointerException {
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
    public RecognitionResult(Result rawResult, RuleGrammar ruleGrammar) throws NullPointerException {

        setNewResult(rawResult,ruleGrammar);
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
                _logger.warn("GrammarException encountered!", e);
            }
        }
    }
 
    

    /**
     * TODOC
     * @return Returns the original result.
     */
    public Result getRawResult() {
        return _rawResult;
    }

    /**
     * TODOC
     * @return
     */
    public RuleGrammar getRuleGrammar() {
        return _ruleGrammar;
    }

    /**
     * TODOC
     * @return
     */
    public String getText() {
        return _text;
    }

    /**
     * TODOC
     * @return
     */
    public List<RuleMatch> getRuleMatches() {
        return _ruleMatches;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
    	if (_text != null) {
	        StringBuilder sb = new StringBuilder(_text);
	        if (_ruleMatches != null) {
	            for (RuleMatch ruleMatch : _ruleMatches) {
	                sb.append('<').append(ruleMatch.getRule());
	                sb.append(':').append(ruleMatch.getTag()).append('>');
	            }
	        }
	        return sb.toString();
    	} else {
    		return ("null text result");
    	}
    }

    
    /**
     * TODOC
     * @param String representation of this object (generated by the toString() method)
     * @return RecognitionResult
     */
    public static  RecognitionResult constructResultFromString(String inputString) throws InvalidRecognitionResultException {
        


        if (inputString == null)
            throw new InvalidRecognitionResultException();
        
        RecognitionResult result = new RecognitionResult();
        if(inputString.trim().equals(OUTOFGRAMMAR)) {
            result.oog = true;
            result._text = "out of grammar";
            return result;
        }
        inputString = inputString.trim();
        int firstBracketIndex =inputString.indexOf("<");
        if (firstBracketIndex >0) {
            result._text = inputString.substring(0, firstBracketIndex);      //raw result are at the begining before the first ruleMatch
            if (result == null)
                throw new InvalidRecognitionResultException();
            _logger.debug(result._text);
            String theTags = inputString.substring(inputString.indexOf("<"));
            theTags = theTags.trim();
            _logger.debug(theTags);
            String ruleMatches[] = theTags.split("<|>|><");
            _logger.debug("number of rule matches: " + ruleMatches.length);
            for (int i=0; i<ruleMatches.length;i++) {
                _logger.debug("**** "+ i + "th **** " + ruleMatches[i]);
                //if ((ruleMatches[i].length() > 3) &&(ruleMatches[i].contains(tagRuleDelimiter)) ){
                if (ruleMatches[i].length() > 3  ){
                    _logger.debug(" rule match # "+i+"  " +ruleMatches[i]);
                   String rule[] = ruleMatches[i].split(tagRuleDelimiter);
                   if (rule.length == 2 ) {
                      result._ruleMatches.add(new RuleMatch(rule[0],rule[1]));
                      _logger.debug(" rule match # "+i+"  " + rule.length+ " "+ruleMatches[i]);
                   } else {
                       _logger.debug(" Invalid rule match # "+i+"  " + rule.length+ " "+ruleMatches[i]);
                       throw new InvalidRecognitionResultException();
                   }
                } else {
                    _logger.debug("Bad Tag Rule In Result: "+ruleMatches[i]);
                }
            }
            
        //there is no rule to match (just return the raw result
        } else {
            result._text = inputString;   
        }
        return result;
    }
    
    /**
     * TODOC
     * @return
     */
    public boolean isOutOfGrammar() {
        return oog;
    }
    
}