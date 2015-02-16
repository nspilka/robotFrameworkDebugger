/*
 * @author Nik Spilka
 * @version 1.0
 *
 * Copyright (c) 2014
 * Do not reproduce without permission in writing.
 * All rights reserved.
 */

package com.bandofyetis.robotframeworkdebugger;

import java.util.HashMap;
import java.util.Map;


/** 
 * A class representing the debug context.  
 * 
 * A debug context can represent a keyword, test case or test suite
 * 
 * @author nspilka
 *
 */
public class RobotFrameworkDebugContext {
	
	// The type of context
	public enum ContextType{
		TEST_SUITE,
		TEST_CASE,
		KEYWORD
	}
	
	// Text corresponding to the ContextType enum
	static final String TEST_SUITE_TEXT = "Test Suite";
	static final String TEST_CASE_TEXT = "Test Case";
	static final String KEYWORD_TEXT = "Keyword";
	
	// name of the item that is being represented by the context (ie. keyword name, testcase name, etc)
	private String itemName;
	private ContextType contextType;	
	private Map<String, Object> itemAttribs;
	
	// A map of variables defined and scoped to this context
	private Map<String, String> variables;

	// The line number (if applicable for this context element)
	private int lineNumber;
		
	/**
	 * Standard constructor
	 */
	public RobotFrameworkDebugContext() {
		super();
		this.itemName = "";
		this.lineNumber = 0;
		this.contextType = ContextType.KEYWORD;		
		this.setItemAttribs(null);
		variables = new HashMap<String,String>();
	}
	
	/**
	 * Returns the name of this context item
	 * @return a string representing the item name
	 */
	public String getItemName() {
		return itemName;
	}
	
	/** 
	 * Sets the name of this context item
	 * @param itemName the item name for this context
	 */
	public void setItemName(String itemName) {
		this.itemName = itemName;
	}
	
	/**
	 * Get the context type of this debug context
	 * @return a ContextType enumeration object representing this context type
	 */
	public ContextType getContextType() {
		return contextType;
	}
	
	/**
	 * Get the context type in a human readable format
	 * @return a string indicating the context type
	 */
	public String getContextTypeString() {
		if (contextType == ContextType.TEST_SUITE){
			return TEST_SUITE_TEXT;
		}
		else if (contextType == ContextType.TEST_CASE){
			return TEST_CASE_TEXT;
		}
		else{
			return KEYWORD_TEXT;
		}
		
	}	
	
	/**
	 * Set the context type for this debugging context
	 * @param contextType a ContextType enumeration object representing
	 * the context type to set
	 */
	public void setContextType(ContextType contextType) {
		this.contextType = contextType;
	}

	/**
	 * Increment the current line number by one
	 */
	public void incrementLineNumber(){
		lineNumber += 1;
	}
	
	/**
	 * Get the current line number
	 * @return an integer representing the current line number
	 */
	public int getLineNumber(){
		return lineNumber;
	}
	
	/**
	 * Get the attributes map for this debug context
	 * @return a map of this elements attributes
	 */
	public Map<String, Object> getItemAttribs() {
		return itemAttribs;
	}
	
	/**
	 * Set the attribute map for this context item
	 * @param itemAttribs a map of strings to objects representing the debug contexts
	 * attributes
	 */
	public void setItemAttribs(Map<String, Object> itemAttribs) {
		this.itemAttribs = itemAttribs;
	}

	/**
	 * Remove all variables from this context
	 */
	public void clearVariables() {		
		variables.clear();
	}

	/**
	 * Update or add a variable to the debug context
	 * @param name the name of the variable
	 * @param value the value of this variable
	 */
	public void updateVariable(String name, String value) {
		variables.put(name, value);		
	}

	/**
	 * Get all variables from the debug context 
	 * @return a map of string keys and values representing the variables defined in this context
	 */
	public Map<String, String> getVariables() {
		return variables;
	}
}
