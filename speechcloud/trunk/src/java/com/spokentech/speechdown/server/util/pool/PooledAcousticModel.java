package com.spokentech.speechdown.server.util.pool;

import java.io.IOException;

import edu.cmu.sphinx.linguist.acoustic.AcousticModel;

public class PooledAcousticModel {
	
	private String id;
	private String language;
	private int sampleRate;
	private AcousticModel model;
	
	
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getSampleRate() {
		return sampleRate;
	}
	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}
	public AcousticModel getModel() {
		//allocate the model when used (if already allocated it is not reallocated -- that code is in model)
		//TODO: should not rely on that here -- some models may not be lazy allocaters
		try {
			model.allocate();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return model;
	}
	public void setModel(AcousticModel model) {
		this.model = model;
	}
	
}
