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
import java.util.logging.Logger;
import edu.cmu.sphinx.result.Result;
import com.spokentech.speechdown.common.rule.RuleMatch;

/**
 * Represents the result of a completed recognition request.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RecognitionResult {

    private static Logger _logger = Logger.getLogger(RecognitionResult.class.getName());

	protected  boolean json;
    
    protected final static String tagRuleDelimiter = ":";
    protected final static String OUTOFGRAMMAR = "<unk>";
    protected boolean oog;

    protected Result _rawResult;
    protected String _text;
    protected List<RuleMatch> _ruleMatches;
    
    protected boolean cflag = false;

	protected double confidence;

	protected boolean noGrammar = false;
	
	
    /**
     * @return the cflag
     */
    public boolean isCflag() {
    	return cflag;
    }

	/**
     * @param cflag the cflag to set
     */
    public void setCflag(boolean cflag) {
    	this.cflag = cflag;
    }
    
    /**
     * @return the confidence
     */
    public double getConfidence() {
    	return confidence;
    }


	/**
     * @param confidence the confidence to set
     */
    public void setConfidence(double confidence) {
    	this.confidence = confidence;
    }

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
     * @return Returns the original result.
     */
    public Result getRawResult() {
        return _rawResult;
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
    		
        	// TODO: quick and dirty approach to passing back confidence
        	if (cflag) {
		        StringBuilder sb = new StringBuilder("**");
		        sb.append(_text);
		        sb.append('<').append("confidence");
		        sb.append(':').append(Double.toString(confidence)).append('>');
		        return sb.toString();
        	} else {
		        StringBuilder sb = new StringBuilder(_text);
		        if (_ruleMatches != null) {
		            for (RuleMatch ruleMatch : _ruleMatches) {
		                sb.append('<').append(ruleMatch.getRule());
		                sb.append(':').append(ruleMatch.getTag()).append('>');
		            }
		        }
		        return sb.toString();
        	}
    	} else {
    		return ("null text result");
    	}
    }

    public static  RecognitionResult constructResultFromJSONString(String inputString)  {

    	
    	//TODO:  This is a quick test, should integrate this recog result with the json result.  right now, just putting it in the string
        RecognitionResult result = new RecognitionResult();
    	result._text = inputString;	
    	result.json = true;
    	return result;
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
        System.out.println(inputString);
        //TODO: remove this quick and dirty method of passing back the confidence score
        if(inputString.startsWith("**")) {
            int firstBracketIndex =inputString.indexOf("<");
            if (firstBracketIndex >2) {
                result._text = inputString.substring(2, firstBracketIndex);      //raw result are at the begining before the first ruleMatch
                _logger.fine("--> "+result._text);
                if (result == null)
                    throw new InvalidRecognitionResultException();
                
                String theTags = inputString.substring(inputString.indexOf("<"));
                theTags = theTags.trim();
                _logger.fine(theTags);
                String ruleMatches[] = theTags.split("<|>|><");
                _logger.fine("number of rule matches: " + ruleMatches.length);
                
                for (int i=0; i<ruleMatches.length;i++) {
                    _logger.fine("**** "+ i + "th **** " + ruleMatches[i]);
                    //if ((ruleMatches[i].length() > 3) &&(ruleMatches[i].contains(tagRuleDelimiter)) ){
                    if (ruleMatches[i].length() > 3  ){
                        _logger.fine(" confidence tag in position # "+i+"  " +ruleMatches[i]);
                       String rule[] = ruleMatches[i].split(tagRuleDelimiter);
                       if (rule.length == 2 ) {
                    	   result.setCflag(true);
                    	   result.setConfidence(Double.parseDouble(rule[1]));
                          _logger.fine("Confidence value in position # "+i+"  " + rule.length+ " "+ruleMatches[i]);
                       } else {
                           _logger.fine(" Invalid rule match # "+i+"  " + rule.length+ " "+ruleMatches[i]);
                           throw new InvalidRecognitionResultException();
                       }
                    } else {
                        _logger.fine("Bad Tag Rule In Result1: "+ruleMatches[i]);
                    }
                }   
            }
            return result;
        }
        
        

        int firstBracketIndex =inputString.indexOf("<");
        if (firstBracketIndex >0) {
            result._text = inputString.substring(0, firstBracketIndex);      //raw result are at the begining before the first ruleMatch
            if (result == null)
                throw new InvalidRecognitionResultException();
            _logger.fine(result._text);
            String theTags = inputString.substring(inputString.indexOf("<"));
            theTags = theTags.trim();
            _logger.fine(theTags);
            String ruleMatches[] = theTags.split("<|>|><");
            _logger.fine("number of rule matches: " + ruleMatches.length);
            for (int i=0; i<ruleMatches.length;i++) {
                _logger.fine("**** "+ i + "th **** " + ruleMatches[i]);
                //if ((ruleMatches[i].length() > 3) &&(ruleMatches[i].contains(tagRuleDelimiter)) ){
                if (ruleMatches[i].length() > 3  ){
                    _logger.fine(" rule match # "+i+"  " +ruleMatches[i]);
                   String rule[] = ruleMatches[i].split(tagRuleDelimiter);
                   if (rule.length == 2 ) {
                      result._ruleMatches.add(new RuleMatch(rule[0],rule[1]));
                      _logger.fine(" rule match # "+i+"  " + rule.length+ " "+ruleMatches[i]);
                   } else {
                       _logger.fine(" Invalid rule match # "+i+"  " + rule.length+ " "+ruleMatches[i]);
                       throw new InvalidRecognitionResultException();
                   }
                } else {
                    _logger.fine("Bad Tag Rule In Result: "+ruleMatches[i]);
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
