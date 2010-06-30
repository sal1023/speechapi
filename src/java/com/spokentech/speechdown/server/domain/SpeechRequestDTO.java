package com.spokentech.speechdown.server.domain;

import java.util.Date;

public class SpeechRequestDTO {
	
	private Long id;
    private String protocol;
    private String scheme;	
    private String method;
    private String contextPath;	
    private String remoteAddr ;
    private String remoteHost;
    private int remotePort;
    private String localAddr ;
    private String localName ;
    private int localPort;
	private String locale;
    private Date date;
	private  SynthRequest synth;
    private  RecogRequest recog;
	private String developerId;
	private String userId;
	private String devDefined;
       
	/**
     * @return the devDefined
     */
    public String getDevDefined() {
    	return devDefined;
    }
	/**
     * @param devDefined the devDefined to set
     */
    public void setDevDefined(String devDefined) {
    	this.devDefined = devDefined;
    }
	/**
     * @return the developerId
     */
    public String getDeveloperId() {
    	return developerId;
    }
	/**
     * @param developerId the developerId to set
     */
    public void setDeveloperId(String developerId) {
    	this.developerId = developerId;
    }
	/**
     * @return the userId
     */
    public String getUserId() {
    	return userId;
    }
	/**
     * @param userId the userId to set
     */
    public void setUserId(String userId) {
    	this.userId = userId;
    }
 
    /**
     * @return the synth
     */
    public SynthRequest getSynth() {
    	return synth;
    }
	/**
     * @param synth the synth to set
     */
    public void setSynth(SynthRequest synth) {
    	this.synth = synth;
    }
	/**
     * @return the recog
     */
    public RecogRequest getRecog() {
    	return recog;
    }
	/**
     * @param recog the recog to set
     */
    public void setRecog(RecogRequest recog) {
    	this.recog = recog;
    }

    /**
     * @return the remotePort
     */
    public int getRemotePort() {
    	return remotePort;
    }
	/**
     * @return the localPort
     */
    public int getLocalPort() {
    	return localPort;
    }

	
    /**
     * @return the id
     */
    public Long getId() {
    	return id;
    }
	/**
     * @param remotePort the remotePort to set
     */
    public void setRemotePort(int remotePort) {
    	this.remotePort = remotePort;
    }
	/**
     * @param localPort the localPort to set
     */
    public void setLocalPort(int localPort) {
    	this.localPort = localPort;
    }
	/**
     * @param id the id to set
     */
    public void setId(Long id) {
    	this.id = id;
    }
	/**
     * @return the protocol
     */
    public String getProtocol() {
    	return protocol;
    }
	/**
     * @param protocol the protocol to set
     */
    public void setProtocol(String protocol) {
    	this.protocol = protocol;
    }
	/**
     * @return the scheme
     */
    public String getScheme() {
    	return scheme;
    }
	/**
     * @param scheme the scheme to set
     */
    public void setScheme(String scheme) {
    	this.scheme = scheme;
    }
	/**
     * @return the method
     */
    public String getMethod() {
    	return method;
    }
	/**
     * @param method the method to set
     */
    public void setMethod(String method) {
    	this.method = method;
    }
	/**
     * @return the contextPath
     */
    public String getContextPath() {
    	return contextPath;
    }
	/**
     * @param contextPath the contextPath to set
     */
    public void setContextPath(String contextPath) {
    	this.contextPath = contextPath;
    }
	/**
     * @return the remoteAddr
     */
    public String getRemoteAddr() {
    	return remoteAddr;
    }
	/**
     * @param remoteAddr the remoteAddr to set
     */
    public void setRemoteAddr(String remoteAddr) {
    	this.remoteAddr = remoteAddr;
    }
	/**
     * @return the remoteHost
     */
    public String getRemoteHost() {
    	return remoteHost;
    }
	/**
     * @param remoteHost the remoteHost to set
     */
    public void setRemoteHost(String remoteHost) {
    	this.remoteHost = remoteHost;
    }

	/**
     * @return the localAddr
     */
    public String getLocalAddr() {
    	return localAddr;
    }
	/**
     * @param localAddr the localAddr to set
     */
    public void setLocalAddr(String localAddr) {
    	this.localAddr = localAddr;
    }
	/**
     * @return the localName
     */
    public String getLocalName() {
    	return localName;
    }
	/**
     * @param localName the localName to set
     */
    public void setLocalName(String localName) {
    	this.localName = localName;
    }
	/**
     * @return the locale
     */
    public String getLocale() {
    	return locale;
    }
	/**
     * @param loacale the locale to set
     */
    public void setLocale(String locale) {
    	this.locale = locale;
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

}
	
	
