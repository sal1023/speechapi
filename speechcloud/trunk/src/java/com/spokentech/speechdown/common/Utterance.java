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
        text, json, nlsml 
    }
	
    
    protected final static String OUTOFGRAMMAR = "<unk>";
    
	private boolean oog;
    private String text;
    private double confidence;
    private List<RuleMatch> ruleMatches;   
    private List<WordData> words;

    public Utterance() {
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



 
   
}
