/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.domain;

import java.util.HashSet;
import java.util.Set;

public class User {
	
	private Long id;
    private int age;
    private String firstname;
    private String lastname;

	private Set requests = new HashSet();

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
     * @return the age
     */
    public int getAge() {
    	return age;
    }

	/**
     * @param age the age to set
     */
    public void setAge(int age) {
    	this.age = age;
    }

	/**
     * @return the firstname
     */
    public String getFirstname() {
    	return firstname;
    }

	/**
     * @param firstname the firstname to set
     */
    public void setFirstname(String firstname) {
    	this.firstname = firstname;
    }

	/**
     * @return the lastname
     */
    public String getLastname() {
    	return lastname;
    }

	/**
     * @param lastname the lastname to set
     */
    public void setLastname(String lastname) {
    	this.lastname = lastname;
    }



	    public Set getRequests() {
	        return requests;
	    }

	    public void setRequests(Set requests) {
	        this.requests = requests;
	    }

}
