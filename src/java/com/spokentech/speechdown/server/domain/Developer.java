package com.spokentech.speechdown.server.domain;

import java.util.HashSet;
import java.util.Set;

public class Developer {
	
	private Long id;

    private String firstname;
    private String lastname;
	private String companyname;
    private String email;
    private String title;
	private Set requests = new HashSet();
	
    /**
     * @return the companyname
     */
    public String getCompanyname() {
    	return companyname;
    }

	/**
     * @param companyname the companyname to set
     */
    public void setCompanyname(String companyname) {
    	this.companyname = companyname;
    }

	/**
     * @return the email
     */
    public String getEmail() {
    	return email;
    }

	/**
     * @param email the email to set
     */
    public void setEmail(String email) {
    	this.email = email;
    }

	/**
     * @return the title
     */
    public String getTitle() {
    	return title;
    }

	/**
     * @param title the title to set
     */
    public void setTitle(String title) {
    	this.title = title;
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
