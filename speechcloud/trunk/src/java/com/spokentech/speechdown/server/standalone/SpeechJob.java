package com.spokentech.speechdown.server.standalone;

import java.io.InputStream;
import java.net.URL;



public class SpeechJob {
	
	private String id;
	private String requestor;
	private int priority;	
	private InputStream inputStream;
	private URL url;
	
	/**
     * @return the url
     */
    public URL getUrl() {
    	return url;
    }
	/**
     * @param url the url to set
     */
    public void setUrl(URL url) {
    	this.url = url;
    }
	/**
     * @return the id
     */
    public String getId() {
    	return id;
    }
	/**
     * @param id the id to set
     */
    public void setId(String id) {
    	this.id = id;
    }
	/**
     * @return the requestor
     */
    public String getRequestor() {
    	return requestor;
    }
	/**
     * @param requestor the requestor to set
     */
    public void setRequestor(String requestor) {
    	this.requestor = requestor;
    }
	/**
     * @return the priority
     */
    public int getPriority() {
    	return priority;
    }
	/**
     * @param priority the priority to set
     */
    public void setPriority(int priority) {
    	this.priority = priority;
    }

	
	/**
     * @return the inputStream
     */
    public InputStream getInputStream() {
    	return inputStream;
    }
	/**
     * @param inputStream the inputStream to set
     */
    public void setInputStream(InputStream inputStream) {
    	this.inputStream = inputStream;
    }


}
