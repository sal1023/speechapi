/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.domain;

import java.util.Date;

public class SynthRequest {


	private Long id;
    
    private Date date;
	private  String  mimeType;
	private  String  voice;
	private  String  text;
	private int sampleRate;
	private boolean bigEndian;
	private int bytesPerValue;
	private String encoding ;
	private long wallTime;
	private long streamLen;
	
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
     * @return the mimeType
     */
    public String getMimeType() {
    	return mimeType;
    }
	/**
     * @param mimeType the mimeType to set
     */
    public void setMimeType(String mimeType) {
    	this.mimeType = mimeType;
    }
	/**
     * @return the voice
     */
    public String getVoice() {
    	return voice;
    }
	/**
     * @param voice the voice to set
     */
    public void setVoice(String voice) {
    	this.voice = voice;
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
     * @return the sampleRate
     */
    public int getSampleRate() {
    	return sampleRate;
    }
	/**
     * @param sampleRate the sampleRate to set
     */
    public void setSampleRate(int sampleRate) {
    	this.sampleRate = sampleRate;
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
