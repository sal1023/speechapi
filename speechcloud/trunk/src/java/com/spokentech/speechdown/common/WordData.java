package com.spokentech.speechdown.common;

public class WordData {
	

	private String word;
	private double confidence;
	private double startTime;
	private double stopTime;
	private String pronunciation;
	
	public WordData() {
    }
	
	/**
     * @return the word
     */
    public String getWord() {
    	return word;
    }
	/**
     * @param word the word to set
     */
    public void setWord(String word) {
    	this.word = word;
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
     * @return the startTime
     */
    public double getStartTime() {
    	return startTime;
    }
	/**
     * @param startTime the startTime to set
     */
    public void setStartTime(double startTime) {
    	this.startTime = startTime;
    }
	/**
     * @return the stopTime
     */
    public double getStopTime() {
    	return stopTime;
    }
	/**
     * @param stopTime the stopTime to set
     */
    public void setStopTime(double stopTime) {
    	this.stopTime = stopTime;
    }
	/**
     * @return the pronunciation
     */
    public String getPronunciation() {
    	return pronunciation;
    }
	/**
     * @param pronunciation the pronunciation to set
     */
    public void setPronunciation(String pronunciation) {
    	this.pronunciation = pronunciation;
    }

	public String toString() {
		
		return (word +" Confidence: "+confidence+" ("+startTime+","+stopTime+") ["+pronunciation+"]");

	}
}
