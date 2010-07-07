/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.common;

import java.util.List;
import java.util.logging.Logger;
import com.spokentech.speechdown.common.rule.RuleMatch;

/**
 * Represents the result of a completed recognition request.
 *
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class Utterance {


	private static Logger _logger = Logger.getLogger(Utterance.class.getName());
	
    
    public enum OutputFormat {
        text, json, nlsml,xml 
    }
	
    
    protected final static String OUTOFGRAMMAR = "<unk>";
    


	private String rCode = "Success";
	private String rMessage = null;
	private boolean oog;
    private String text;
    private double confidence;
    private List<RuleMatch> ruleMatches;   
    private List<WordData> words;

    public Utterance() {
    }

	/**
     * @return the rCode
     */
    public String getRCode() {
    	return rCode;
    }


	/**
     * @param code the rCode to set
     */
    public void setRCode(String code) {
    	rCode = code;
    }


	/**
     * @return the rMessage
     */
    public String getRMessage() {
    	return rMessage;
    }


	/**
     * @param message the rMessage to set
     */
    public void setRMessage(String message) {
    	rMessage = message;
    }
    
    /**
     * @return the oog
     */
    public boolean isOog() {
    	return oog;
    }

	/**
     * @param oog the oog to set
     */
    public void setOog(boolean oog) {
    	this.oog = oog;
    }

	/**
     * @return the text
     */
    public String getText() {
    	return text;
    }

	/**
     * @param text the text to set
     */
    public void setText(String text) {
    	this.text = text;
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
     * @return the ruleMatches
     */
    public List<RuleMatch> getRuleMatches() {
    	return ruleMatches;
    }

	/**
     * @param ruleMatches the ruleMatches to set
     */
    public void setRuleMatches(List<RuleMatch> ruleMatches) {
    	this.ruleMatches = ruleMatches;
    }

	/**
     * @return the words
     */
    public List<WordData> getWords() {
    	return words;
    }

	/**
     * @param words the words to set
     */
    public void setWords(List<WordData> words) {
    	this.words = words;
    }


    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.append(text);
        builder.append("\n");
        builder.append(rCode);
        builder.append("\n");
        builder.append(rMessage);
        builder.append("\n");
        builder.append(confidence);
        builder.append("\n");
        builder.append(oog);
        builder.append("\n");
        for (RuleMatch rm : ruleMatches) {
            builder.append("   ");
            builder.append(rm.getRule());
            builder.append(" : ");
            builder.append(rm.getTag());
            builder.append("\n");
        }
        for (WordData wd : words) {
            builder.append("   ");
            builder.append(wd.getWord());
            builder.append(" : ");
            builder.append(wd.getPronunciation());
            builder.append(" : ");
            builder.append(wd.getStartTime());
            builder.append(" : ");
            builder.append(wd.getStopTime());
            builder.append(" : ");
            builder.append(wd.getConfidence());
            builder.append("\n");
        }

        return (builder.toString());
    	
    }
   
}
