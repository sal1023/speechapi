package com.spokentech.speechdown.server.domain;

import java.util.Date;

public class RecogRequest {
    private Long id;

    private Date date;
	private int sampleRate;
	private boolean continuous ;
	private boolean cmnBatch ;
	private boolean lm;
	private boolean endPointing ;
	private boolean bigEndian;
	private int bytesPerValue;
	private String encoding ;
	private String ContentType;
	private String grammar;
	private String rawResults;
	private String pronunciation;
	private String tags;
	private long wallTime;
	private long streamLen;
	private String audioUri;
	
	//private  HttpRequest httpRequest;
	
	/**
     * @return the httpRequest
     */
    //public HttpRequest getHttpRequest() {
    //	return httpRequest;
    //}
	/**
     * @param httpRequest the httpRequest to set
     */
    //public void setHttpRequest(HttpRequest httpRequest) {
    //	this.httpRequest = httpRequest;
    //}
	/**
     * @return the audioUri
     */
    public String getAudioUri() {
    	return audioUri;
    }
	/**
     * @param audioUri the audioUri to set
     */
    public void setAudioUri(String audioUri) {
    	this.audioUri = audioUri;
    }
	/**
     * @return the rawResults
     */
    public String getRawResults() {
    	return rawResults;
    }
	/**
     * @param rawResults the rawResults to set
     */
    public void setRawResults(String rawResults) {
    	this.rawResults = rawResults;
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
	/**
     * @return the tags
     */
    public String getTags() {
    	return tags;
    }
	/**
     * @param tags the tags to set
     */
    public void setTags(String tags) {
    	this.tags = tags;
    }
	/**
     * @return the wallTime
     */
    public long getWallTime() {
    	return wallTime;
    }
	/**
     * @param wallTime the wallTime to set
     */
    public void setWallTime(long wallTime) {
    	this.wallTime = wallTime;
    }
	/**
     * @return the streamLen
     */
    public long getStreamLen() {
    	return streamLen;
    }
	/**
     * @param streamLen the streamLen to set
     */
    public void setStreamLen(long streamLen) {
    	this.streamLen = streamLen;
    }

	

	
	/**
     * @return the id
     */
    public Long getId() {
    	return id;
    }
	/**
     * @param id the id to set
     */
    public void setId(Long id) {
    	this.id = id;
    }
    
    /**
     * @return the date
     */
    public Date getDate() {
    	return date;
    }
	/**
     * @param date the date to set
     */
    public void setDate(Date date) {
    	this.date = date;
    }
    
	/**
     * @return the sampleRate
     */
    public int getSampleRate() {
    	return sampleRate;
    }
	/**
     * @return the lm
     */
    public boolean isLm() {
    	return lm;
    }
	/**
     * @param lm the lm to set
     */
    public void setLm(boolean lm) {
    	this.lm = lm;
    }
	/**
     * @return the contentType
     */
    public String getContentType() {
    	return ContentType;
    }
	/**
     * @param contentType the contentType to set
     */
    public void setContentType(String contentType) {
    	ContentType = contentType;
    }
	/**
     * @return the grammar
     */
    public String getGrammar() {
    	return grammar;
    }
	/**
     * @param grammar the grammar to set
     */
    public void setGrammar(String grammar) {
    	this.grammar = grammar;
    }
	/**
     * @param sampleRate the sampleRate to set
     */
    public void setSampleRate(int sampleRate) {
    	this.sampleRate = sampleRate;
    }
	/**
     * @return the continuous
     */
    public boolean isContinuous() {
    	return continuous;
    }
	/**
     * @param continuous the continuous to set
     */
    public void setContinuous(boolean continuous) {
    	this.continuous = continuous;
    }
	/**
     * @return the cmnBatch
     */
    public boolean isCmnBatch() {
    	return cmnBatch;
    }
	/**
     * @param cmnBatch the cmnBatch to set
     */
    public void setCmnBatch(boolean cmnBatch) {
    	this.cmnBatch = cmnBatch;
    }
	/**
     * @return the endPointing
     */
    public boolean isEndPointing() {
    	return endPointing;
    }
	/**
     * @param endPointing the endPointing to set
     */
    public void setEndPointing(boolean endPointing) {
    	this.endPointing = endPointing;
    }
	/**
     * @return the bigEndian
     */
    public boolean isBigEndian() {
    	return bigEndian;
    }
	/**
     * @param bigEndian the bigEndian to set
     */
    public void setBigEndian(boolean bigEndian) {
    	this.bigEndian = bigEndian;
    }
	/**
     * @return the bytesPerValue
     */
    public int getBytesPerValue() {
    	return bytesPerValue;
    }
	/**
     * @param bytesPerValue the bytesPerValue to set
     */
    public void setBytesPerValue(int bytesPerValue) {
    	this.bytesPerValue = bytesPerValue;
    }
	/**
     * @return the encoding
     */
    public String getEncoding() {
    	return encoding;
    }
	/**
     * @param encoding the encoding to set
     */
    public void setEncoding(String encoding) {
    	this.encoding = encoding;
    }
}
