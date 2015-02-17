package com.bandofyetis.robotframeworkdebugger;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.bandofyetis.robotframeworkdebugger.RobotFrameworkDebugContext;
import com.bandofyetis.robotframeworkdebugger.RobotFrameworkDebugContext.ContextType;

public class RobotFrameworkDebugContextTest {

	private final String name1 = "NewItem";
	private final String name2 = "New Item2";
	
	private final String value1 = "Value1";
	private final String value2 = "Value2";
	private final String value3 = "Value3";
	
	@Test
	public void testGetItemName() {
		RobotFrameworkDebugContext kdc = new RobotFrameworkDebugContext();
		assertEquals("",kdc.getItemName());
		kdc.setItemName(name1);
		assertEquals(name1, kdc.getItemName());
		kdc.setItemName(name2);
		assertEquals(name2, kdc.getItemName());
	}
	
	@Test
	public void testSetItemName() {
		RobotFrameworkDebugContext kdc = new RobotFrameworkDebugContext();
		assertEquals("",kdc.getItemName());
		kdc.setItemName(name1);
		assertEquals(name1, kdc.getItemName());
		kdc.setItemName(name2);
		assertEquals(name2, kdc.getItemName());
	}
	
	@Test
	public void testGetContextType() {
		RobotFrameworkDebugContext kdc = new RobotFrameworkDebugContext();
		// Default type is keyword
		assertEquals(ContextType.KEYWORD, kdc.getContextType());
		kdc.setContextType(ContextType.TEST_CASE);
		assertEquals(ContextType.TEST_CASE, kdc.getContextType());
	}
	
	@Test
	public void testGetContextTypeString() {
		RobotFrameworkDebugContext kdc = new RobotFrameworkDebugContext();
		// Default type is keyword
		assertEquals(RobotFrameworkDebugContext.KEYWORD_TEXT, kdc.getContextTypeString());
		kdc.setContextType(ContextType.TEST_CASE);
		assertEquals(RobotFrameworkDebugContext.TEST_CASE_TEXT, kdc.getContextTypeString());
		kdc.setContextType(ContextType.TEST_SUITE);
		assertEquals(RobotFrameworkDebugContext.TEST_SUITE_TEXT, kdc.getContextTypeString());
		// Set back to keyword
		kdc.setContextType(ContextType.KEYWORD);
		assertEquals(RobotFrameworkDebugContext.KEYWORD_TEXT, kdc.getContextTypeString());
	}	
	
	@Test
	public void testSetContextType() {
		RobotFrameworkDebugContext kdc = new RobotFrameworkDebugContext();
		// Default type is keyword
		assertEquals(ContextType.KEYWORD, kdc.getContextType());
		kdc.setContextType(ContextType.TEST_CASE);
		assertEquals(ContextType.TEST_CASE, kdc.getContextType());
		kdc.setContextType(ContextType.TEST_SUITE);
		assertEquals(RobotFrameworkDebugContext.TEST_SUITE_TEXT, kdc.getContextTypeString());
		kdc.setContextType(ContextType.KEYWORD);
		assertEquals(RobotFrameworkDebugContext.KEYWORD_TEXT, kdc.getContextTypeString());
	}

	@Test
	public void testIncrementLineNumber(){
		RobotFrameworkDebugContext kdc = new RobotFrameworkDebugContext();
		assertEquals(0,kdc.getLineNumber());
		kdc.incrementLineNumber();
		assertEquals(1,kdc.getLineNumber());
		kdc.incrementLineNumber();
		assertEquals(2,kdc.getLineNumber());
	}

	@Test
	public void testGetLineNumber(){
		RobotFrameworkDebugContext kdc = new RobotFrameworkDebugContext();
		assertEquals(0,kdc.getLineNumber());
		kdc.incrementLineNumber();
		assertEquals(1,kdc.getLineNumber());
		kdc.incrementLineNumber();
		assertEquals(2,kdc.getLineNumber());
	}
	@Test
	public void testGetAndSetItemAttribs() {
		RobotFrameworkDebugContext kdc = new RobotFrameworkDebugContext();
		Map<String, Object> attrs = kdc.getItemAttribs();
		assertNull(attrs);
		Map<String, Object> attrMapIn = createAttrMap();
		kdc.setItemAttribs(attrMapIn);
		Map<String, Object> attrMapOut = kdc.getItemAttribs();
		assertEquals(value1, (String)attrMapOut.get(name1));
		assertEquals(value2, (String)attrMapOut.get(name2));
	}
	
	private Map<String,Object> createAttrMap(){
		Map<String,Object> returnMap = new HashMap<String,Object>();
		returnMap.put(name1,value1);
		returnMap.put(name2, value2);
		return returnMap;
	}


	@Test
	public void testClearVariables() {		
		RobotFrameworkDebugContext kdc = new RobotFrameworkDebugContext();
		kdc.updateVariable(name1, value1);
		kdc.updateVariable(name2, value2);
		Map<String,String> vars = kdc.getVariables();
		assertEquals(2,vars.size());
		kdc.clearVariables();
		Map<String,String> vars2 = kdc.getVariables();
		assertEquals(0,vars2.size());
	}

	@Test
	public void testUpdateVariable() {
		RobotFrameworkDebugContext kdc = new RobotFrameworkDebugContext();
		kdc.updateVariable(name1, value1);
		kdc.updateVariable(name2, value2);
		Map<String,String> vars = kdc.getVariables();
		assertTrue(vars.containsKey(name1));
		assertTrue(vars.containsKey(name2));
		assertEquals(value1, vars.get(name1));
		assertEquals(value2, vars.get(name2));
		kdc.updateVariable(name1, value3);
		assertEquals(value3, vars.get(name1));
	}

	@Test
	public void testGetVariables() {
		RobotFrameworkDebugContext kdc = new RobotFrameworkDebugContext();
		Map<String,String> vars_e = kdc.getVariables();
		assertEquals(0,vars_e.size());
		kdc.updateVariable(name1, value1);
		kdc.updateVariable(name2, value2);
		Map<String,String> vars = kdc.getVariables();
		assertTrue(vars.containsKey(name1));
		assertTrue(vars.containsKey(name2));
	}
}




