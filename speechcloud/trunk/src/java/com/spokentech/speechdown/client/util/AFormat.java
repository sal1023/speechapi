package com.spokentech.speechdown.client.util;

public class AFormat {	
	
	String encoding;

	double sampleRate;
	int sampleSizeInBits;
	int channels;
	boolean bigEndian;
	boolean signed;
	int frameSizeInBytes;
	double frameRate;	


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


	/**
     * @return the sampleRate
     */
    public double getSampleRate() {
    	return sampleRate;
    }


	/**
     * @param sampleRate the sampleRate to set
     */
    public void setSampleRate(double sampleRate) {
    	this.sampleRate = sampleRate;
    }


	/**
     * @return the sampleSizeInBits
     */
    public int getSampleSizeInBits() {
    	return sampleSizeInBits;
    }


	/**
     * @param sampleSizeInBits the sampleSizeInBits to set
     */
    public void setSampleSizeInBits(int sampleSizeInBits) {
    	this.sampleSizeInBits = sampleSizeInBits;
    }


	/**
     * @return the channels
     */
    public int getChannels() {
    	return channels;
    }


	/**
     * @param channels the channels to set
     */
    public void setChannels(int channels) {
    	this.channels = channels;
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
     * @return the signed
     */
    public boolean isSigned() {
    	return signed;
    }


	/**
     * @param signed the signed to set
     */
    public void setSigned(boolean signed) {
    	this.signed = signed;
    }


	/**
     * @return the frameSizeInBytes
     */
    public int getFrameSizeInBytes() {
    	return frameSizeInBytes;
    }


	/**
     * @param frameSizeInBytes the frameSizeInBytes to set
     */
    public void setFrameSizeInBytes(int frameSizeInBytes) {
    	this.frameSizeInBytes = frameSizeInBytes;
    }


	/**
     * @return the frameRate
     */
    public double getFrameRate() {
    	return frameRate;
    }


	/**
     * @param frameRate the frameRate to set
     */
    public void setFrameRate(double frameRate) {
    	this.frameRate = frameRate;
    }


	public AFormat(String encoding, double sampleRate, int sampleSizeInBits, int channels, boolean bigEndian,
            boolean signed, int frameSizeInBytes, double frameRate) {
	    super();
	    this.encoding = encoding;
	    this.sampleRate = sampleRate;
	    this.sampleSizeInBits = sampleSizeInBits;
	    this.channels = channels;
	    this.bigEndian = bigEndian;
	    this.signed = signed;
	    this.frameSizeInBytes = frameSizeInBytes;
	    this.frameRate = frameRate;
    }

	    
}
