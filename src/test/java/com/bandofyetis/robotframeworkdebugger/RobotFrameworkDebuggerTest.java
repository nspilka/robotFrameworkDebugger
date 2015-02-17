package com.bandofyetis.robotframeworkdebugger;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bandofyetis.robotframeworkdebugger.RobotFrameworkDebugContext;
import com.bandofyetis.robotframeworkdebugger.RobotFrameworkDebugger;
import com.bandofyetis.robotframeworkdebugger.RobotFrameworkDebugContext.ContextType;
import com.bandofyetis.robotframeworkdebugger.RobotFrameworkDebugger.StepMode;


public class RobotFrameworkDebuggerTest {

	private static final int [] unsortedArray = {7,5,2,14,3,6};
	private static final int [] sortedArray   = {2,3,5,6,7,14};
	
	private static Vector<Appender> appenders;
	private static 	StringWriter writer;
	
	private RobotFrameworkDebugger debugger;
	private boolean additivity;
	
	@BeforeClass
	public static void setUpOnce() {
		  appenders = new Vector<Appender>(2);
		  // 1. just a printout appender:
		  appenders.add(new ConsoleAppender(new PatternLayout("%d [%t] %-5p %c - %m%n")));
		  // 2. the appender to test against:
		  writer = new StringWriter();
		  appenders.add(new WriterAppender(new PatternLayout("%p, %m%n"),writer));
		}
	
	@Before
	public void setUp() throws InterruptedException, IOException {
	  // Unit Under Test:
	  debugger = new RobotFrameworkDebugger(false);
	  // setting test appenders:
	  for (Appender appender : appenders) {
		  RobotFrameworkDebugger.log.addAppender(appender);
	  }
	  // saving additivity and turning it off:
	  additivity = RobotFrameworkDebugger.log.getAdditivity();
	  RobotFrameworkDebugger.log.setAdditivity(false);
	}

	@After
	public void tearDown() {
	  debugger = null;
	  for (Appender appender : appenders) {
		  RobotFrameworkDebugger.log.removeAppender(appender);
	  }
	  RobotFrameworkDebugger.log.setAdditivity(additivity);
	}

	
	@Test
	public void testStartSuite() throws InterruptedException, IOException {
		
		debugger.getContextStack().clear();
		RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.TEST_SUITE);
		rootContext.setItemName("testSuite");
		debugger.getContextStack().push(rootContext);
        
		debugger.startSuite("mySuite", null);
		
		Stack<RobotFrameworkDebugContext> ctxtStack = debugger.getContextStack();
		assertEquals(2,ctxtStack.size());
		RobotFrameworkDebugContext kdc = ctxtStack.pop();
		assertTrue(kdc.getContextType() == ContextType.TEST_CASE);
		
		kdc = ctxtStack.pop();
		assertTrue(kdc.getContextType() == ContextType.TEST_SUITE);
		assertEquals("mySuite", kdc.getItemName());
	}

	@Test
	public void testStartSuiteNotValid() throws InterruptedException, IOException {
		// zero the log trap
		writer.getBuffer().setLength(0);
		RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.TEST_CASE);
		debugger.getContextStack().push(rootContext);
		debugger.startSuite("suite2",null);
		assertTrue(writer.toString(), writer.toString().contains(RobotFrameworkDebugger.LOG_EXPECTED_TEST_SUITE_OBJECT));
		writer.getBuffer().setLength(0);		
	}
	
	@Test
	public void testStartTest() throws InterruptedException, IOException {			
		RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.TEST_CASE);
		rootContext.setItemName("test1");
		rootContext.getVariables().put("var1", "val1");
		assertTrue(rootContext.getVariables().size() > 0);
		debugger.getContextStack().push(rootContext);
        
		debugger.startTest("test2",null);
        RobotFrameworkDebugContext kwContext = debugger.getContextStack().pop();
        RobotFrameworkDebugContext tcContext = debugger.getContextStack().pop();
        assertEquals(ContextType.KEYWORD, kwContext.getContextType());
        assertEquals(0, tcContext.getVariables().size());
        assertNull(tcContext.getItemAttribs());
        assertEquals("test2", tcContext.getItemName());
	}

	@Test
	public void testStartTestNotValid() throws InterruptedException, IOException {
		// zero the log trap
		writer.getBuffer().setLength(0);
		RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.KEYWORD);
		debugger.getContextStack().push(rootContext);
		debugger.startTest("test2",null);
		assertTrue(writer.toString(), writer.toString().contains(RobotFrameworkDebugger.LOG_EXPECTED_TEST_CASE_OBJECT));
		writer.getBuffer().setLength(0);		
	}

	@Test
	public void testEndTestNoStopAtTestEnd() throws InterruptedException, IOException {
		
		debugger.stopAtTestEnd = false;
		debugger.getContextStack().clear();
		
		Map<String,Object> attrs = new HashMap<String, Object>();
		attrs.put("status", "PASS");
		
		RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.TEST_CASE);
		rootContext.setItemName("test1");
		debugger.getContextStack().push(rootContext);
        
        RobotFrameworkDebugContext kwContext = new RobotFrameworkDebugContext();
        kwContext.setContextType(ContextType.KEYWORD);
        kwContext.setItemName("kw1");
        debugger.getContextStack().push(kwContext);
        
        debugger.endTest("test1", attrs);
        assertEquals(-1, debugger.getCurrentBreakpointIndex());
        assertEquals(1,debugger.getContextStack().size());
        assertEquals("test1", debugger.getContextStack().peek().getItemName());
        assertEquals(ContextType.TEST_CASE, debugger.getContextStack().peek().getContextType());
	}
	
	@Test
	public void testEndTestNoStopAtTestEndNotValid() throws InterruptedException, IOException {
		
		debugger.stopAtTestEnd = false;
		debugger.getContextStack().clear();
		
		Map<String,Object> attrs = new HashMap<String, Object>();
		attrs.put("status", "PASS");
		
		RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.TEST_CASE);
		rootContext.setItemName("test1");
		debugger.getContextStack().push(rootContext);
        
        RobotFrameworkDebugContext kwContext = new RobotFrameworkDebugContext();
        kwContext.setContextType(ContextType.TEST_CASE);
        kwContext.setItemName("kw1");
        debugger.getContextStack().push(kwContext);
        
        debugger.endTest("test1", attrs);
        assertTrue(writer.toString(), writer.toString().contains(RobotFrameworkDebugger.LOG_EXPECTED_KEYWORD_OBJECT));
	}

	@Test
	public void testEndTestStopAtTestEnd() throws InterruptedException, IOException {

		debugger.getContextStack().clear();
		debugger.stopAtTestEnd = true;
        
        Map<String,Object> attrs = new HashMap<String, Object>();
		attrs.put("status", "PASS");
		
		
        RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.TEST_CASE);
		rootContext.setItemName("test2");
		debugger.getContextStack().push(rootContext);
        
        RobotFrameworkDebugContext kwContext = new RobotFrameworkDebugContext();
        kwContext.setContextType(ContextType.KEYWORD);
        kwContext.setItemName("kw1");
        debugger.getContextStack().push(kwContext);
        
        debugger.endTest("test2", attrs);        
        assertEquals(-1, debugger.getCurrentBreakpointIndex());
        assertEquals(1,debugger.getContextStack().size());
        assertEquals("test2", debugger.getContextStack().peek().getItemName());
        assertEquals(ContextType.TEST_CASE, debugger.getContextStack().peek().getContextType());
        assertEquals(StepMode.STEP_INTO, debugger.getStepMode());
        assertNull(debugger.getStepTarget());
	}
	@Test
	public void testEndSuite() throws InterruptedException, IOException {		
		debugger.getContextStack().clear();
		RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.TEST_SUITE);
		rootContext.setItemName("testSuite");
        debugger.getContextStack().push(rootContext);
        
		RobotFrameworkDebugContext childContext = new RobotFrameworkDebugContext();
        childContext.setContextType(ContextType.TEST_CASE);
		childContext.setItemName("testCase");
        debugger.getContextStack().push(childContext);
        
        assertEquals(2,debugger.getContextStack().size());
       
        debugger.endSuite("testSuite", null);
        assertEquals(1, debugger.getContextStack().size());
	}
	
	@Test
	public void testEndSuiteNotValid() throws InterruptedException, IOException {		
		debugger.getContextStack().clear();
		RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.TEST_SUITE);
		rootContext.setItemName("testSuite");
        debugger.getContextStack().push(rootContext);
        
		RobotFrameworkDebugContext childContext = new RobotFrameworkDebugContext();
        childContext.setContextType(ContextType.KEYWORD);
		childContext.setItemName("testCase");
        debugger.getContextStack().push(childContext);
        
        assertEquals(2,debugger.getContextStack().size());
       
        debugger.endSuite("testSuite", null);
        assertTrue(writer.toString(), writer.toString().contains(RobotFrameworkDebugger.LOG_EXPECTED_TEST_CASE_OBJECT));
	}

	@Test
	public void testStartKeyword() throws InterruptedException, IOException {		
		debugger.getContextStack().clear();
		RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.KEYWORD);
		rootContext.setItemName("rootKW1");
        debugger.getContextStack().push(rootContext);
        
        assertEquals(1,debugger.getContextStack().size());
        assertEquals(0, rootContext.getLineNumber());
        
        debugger.startKeyword("rootKW2", null);
        assertEquals(2, debugger.getContextStack().size());
        assertEquals(-1, debugger.getCurrentBreakpointIndex());
        
        RobotFrameworkDebugContext childKW = debugger.getContextStack().pop();
        RobotFrameworkDebugContext rootKW = debugger.getContextStack().pop();
        
        assertEquals(ContextType.KEYWORD, childKW.getContextType());
        assertEquals(ContextType.KEYWORD, rootKW.getContextType());
        assertNull(rootKW.getItemAttribs());
        assertEquals("rootKW2", rootKW.getItemName());
        assertEquals(1, rootKW.getLineNumber());
        
	}

	@Test
	public void testStartKeywordNotValid() throws InterruptedException, IOException {		
		debugger.getContextStack().clear();
		RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.TEST_CASE);
		rootContext.setItemName("rootKW1");
        debugger.getContextStack().push(rootContext);
        
        assertEquals(1,debugger.getContextStack().size());
        assertEquals(0, rootContext.getLineNumber());
        
        debugger.startKeyword("rootKW2", null);
        assertTrue(writer.toString(), writer.toString().contains(RobotFrameworkDebugger.LOG_EXPECTED_KEYWORD_OBJECT));        
	}
	
	@Test
	public void testEndKeywordNoStepOver() throws InterruptedException, IOException {
		
		debugger.stopAtTestEnd = false;
		debugger.getContextStack().clear();
		
		RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.KEYWORD);
		rootContext.setItemName("rootKeyword");
        debugger.getContextStack().push(rootContext);
        
        RobotFrameworkDebugContext kwContext = new RobotFrameworkDebugContext();
        kwContext.setContextType(ContextType.KEYWORD);
        kwContext.setItemName("kw1");
        debugger.getContextStack().push(kwContext);
        
        debugger.endKeyword("rootKeyword", null);
        assertEquals(1, debugger.getContextStack().size());
        assertEquals("rootKeyword", debugger.getContextStack().peek().getItemName());        
	}
	
	@Test
	public void testEndKeywordNoStepOverNotValid() throws InterruptedException, IOException {
		
		debugger.stopAtTestEnd = false;
		debugger.getContextStack().clear();
		
		RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.KEYWORD);
		rootContext.setItemName("rootKeyword");
        debugger.getContextStack().push(rootContext);
        
        RobotFrameworkDebugContext kwContext = new RobotFrameworkDebugContext();
        kwContext.setContextType(ContextType.TEST_CASE);
        kwContext.setItemName("kw1");
        debugger.getContextStack().push(kwContext);
        
        debugger.endKeyword("rootKeyword", null);

        assertTrue(writer.toString(), writer.toString().contains(RobotFrameworkDebugger.LOG_EXPECTED_KEYWORD_OBJECT)); 
	}
	
	@Test
	public void testEndKeywordStepOver() throws InterruptedException, IOException {
		
		debugger.stopAtTestEnd = false;
		debugger.getContextStack().clear();
		
		
		RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.KEYWORD);
		rootContext.setItemName("rootKeyword");
        debugger.getContextStack().push(rootContext);
        
        debugger.setStepOver();
        assertEquals("rootKeyword",debugger.getStepTarget());
        
        RobotFrameworkDebugContext kwContext = new RobotFrameworkDebugContext();
        kwContext.setContextType(ContextType.KEYWORD);
        kwContext.setItemName("kw1");
        debugger.getContextStack().push(kwContext);
  
        debugger.endKeyword("rootKeyword", null);
        assertEquals(1, debugger.getContextStack().size());
        assertEquals("rootKeyword", debugger.getContextStack().peek().getItemName());
        assertEquals(StepMode.STEP_INTO, debugger.getStepMode());
        assertNull(debugger.getStepTarget());
        
	}

	@Test
	public void testLogMessage() throws InterruptedException, IOException {
		
		RobotFrameworkDebugContext rootContext = new RobotFrameworkDebugContext();
		rootContext.setContextType(ContextType.KEYWORD);
		rootContext.setItemName("rootKW");
        debugger.getContextStack().push(rootContext);
        
		RobotFrameworkDebugContext childContext = new RobotFrameworkDebugContext();
        childContext.setContextType(ContextType.KEYWORD);
		childContext.setItemName("testKW");
        debugger.getContextStack().push(childContext);
        
        // Not a variable, so should leave nothing in variable list
		Map<String,Object> msg =  new HashMap<String,Object>();
		msg.put("message","Not a var");
		debugger.logMessage(msg);
		assertEquals(0, debugger.getContextStack().peek().getVariables().size());
		
		// a list variable, so should be in variable list
		msg =  new HashMap<String,Object>();
		msg.put("message","@{test1}=[1,2,3]");
		debugger.logMessage(msg);
		// pop off the child context
		debugger.getContextStack().pop();
		Map<String,String> vars = debugger.getContextStack().peek().getVariables();
		assertEquals(1,vars.size());
		assertTrue(vars.containsKey("@{test1}"));
		assertEquals("[1,2,3]", vars.get("@{test1}"));
		// push child context back
		debugger.getContextStack().push(childContext);
		
		// a scalar variable, so should be in variable list
		msg =  new HashMap<String,Object>();
		msg.put("message","${test2}=value2");
		debugger.logMessage(msg);
		// pop off the child context
		debugger.getContextStack().pop();
		vars = debugger.getContextStack().peek().getVariables();
		assertEquals(2,vars.size());
		assertTrue(vars.containsKey("@{test1}"));
		assertEquals("[1,2,3]", vars.get("@{test1}"));
		assertTrue(vars.containsKey("${test2}"));
		assertEquals("value2", vars.get("${test2}"));
	}

	@Test
	public void testSetStepOver() throws InterruptedException, IOException {
		
		
		// With empty stack should give us STEP_INTO
		debugger.setStepOver();
		assertEquals(RobotFrameworkDebugger.StepMode.STEP_INTO, debugger.getStepMode());
		assertNull(debugger.getStepTarget());
		
		// With stack with unnamed keyword, should give us STEP_INTO
		RobotFrameworkDebugContext childContext = new RobotFrameworkDebugContext();
        childContext.setContextType(ContextType.KEYWORD);
		debugger.getContextStack().push(childContext);
		
		debugger.setStepOver();
		assertEquals(RobotFrameworkDebugger.StepMode.STEP_INTO, debugger.getStepMode());
		assertNull(debugger.getStepTarget());
		
		// With non-keyword on stack should give us STEP_INTO
		debugger.getContextStack().clear();
		RobotFrameworkDebugContext childContext2 = new RobotFrameworkDebugContext();
        childContext2.setContextType(ContextType.TEST_CASE);
		debugger.getContextStack().push(childContext2);
		
		debugger.setStepOver();
		assertEquals(RobotFrameworkDebugger.StepMode.STEP_INTO, debugger.getStepMode());
		assertNull(debugger.getStepTarget());
		
		
		// with keyord on stack with name, should give us STEP_OVER
		debugger.getContextStack().clear();
		RobotFrameworkDebugContext childContext3 = new RobotFrameworkDebugContext();
        childContext3.setContextType(ContextType.KEYWORD);
		childContext3.setItemName("testKW");
        debugger.getContextStack().push(childContext3);
		
		debugger.setStepOver();
		assertEquals(RobotFrameworkDebugger.StepMode.STEP_OVER, debugger.getStepMode());
		assertEquals("testKW",debugger.getStepTarget());
		
	}
	

	@Test
	public void testSetStepInto() throws InterruptedException, IOException {			
		debugger.setStepInto();
		assertEquals(RobotFrameworkDebugger.StepMode.STEP_INTO, debugger.getStepMode());
		assertNull(debugger.getStepTarget());
	}

	@Test
	public void testSetRunToBreakpoint() throws InterruptedException, IOException {			
		debugger.setRunToBreakpoint();
		assertEquals(RobotFrameworkDebugger.StepMode.RUN_TO_BREAKPOINT, debugger.getStepMode());
		assertNull(debugger.getStepTarget());
	}


	@Test
	public void testGetBreakpoints() throws InterruptedException, IOException {
		
		List<String> breakpoints = debugger.getBreakpoints();
		
		assertEquals (0, breakpoints.size());
		
		debugger.addBreakpoint("aa");
		debugger.addBreakpoint("bp1");
		debugger.addBreakpoint("cp1");
		
		List<String> breakpoints2 = debugger.getBreakpoints();
		assertEquals (3, breakpoints2.size());
		
		assertTrue(breakpoints2.contains("aa"));
		assertTrue(breakpoints2.contains("bp1"));
		assertTrue(breakpoints2.contains("cp1"));
		assertFalse(breakpoints2.contains("dp1"));
	}

	@Test
	public void testGetCurrentBreakpointAsString() throws InterruptedException, IOException {
		
		
		assertNull (debugger.getCurrentBreakpointAsString());
		
		debugger.addBreakpoint("aa");
		debugger.addBreakpoint("bp1");
		debugger.addBreakpoint("cp1");
		
		debugger.setCurrentBreakpointIndex(1);
		assertEquals("bp1", debugger.getCurrentBreakpointAsString());
		
		debugger.setCurrentBreakpointIndex(-1);
		assertNull (debugger.getCurrentBreakpointAsString());
	
	}


	@Test(expected=IndexOutOfBoundsException.class)
	public void testSetAndGetCurrentBreakpointIndexWithOOBException() throws InterruptedException, IOException, IndexOutOfBoundsException {
		
		assertEquals(-1,debugger.getCurrentBreakpointIndex());
		debugger.setCurrentBreakpointIndex(-4);
	}
	
	@Test(expected=IndexOutOfBoundsException.class)
	public void testSetAndGetCurrentBreakpointIndexWithOOBException2() throws InterruptedException, IOException, IndexOutOfBoundsException {	
		
		assertEquals(-1,debugger.getCurrentBreakpointIndex());
		debugger.setCurrentBreakpointIndex(13);
		assertEquals(-1,debugger.getCurrentBreakpointIndex());
	}
	
	@Test(expected=IndexOutOfBoundsException.class)
	public void testSetAndGetCurrentBreakpointIndexWithOOBException3() throws InterruptedException, IOException, IndexOutOfBoundsException {	
		
		assertEquals(-1,debugger.getCurrentBreakpointIndex());
		debugger.addBreakpoint("aa");
		debugger.addBreakpoint("bp1");
		debugger.addBreakpoint("cp1");
		
		debugger.setCurrentBreakpointIndex(4);
		assertEquals(-1, debugger.getCurrentBreakpointIndex());
	}
	
	@Test
	public void testSetAndGetCurrentBreakpointIndex() throws InterruptedException, IOException, IndexOutOfBoundsException {
		
		assertEquals(-1,debugger.getCurrentBreakpointIndex());
		debugger.addBreakpoint("aa");
		debugger.addBreakpoint("bp1");
		debugger.addBreakpoint("cp1");
		
		debugger.setCurrentBreakpointIndex(1);
		assertEquals(1, debugger.getCurrentBreakpointIndex());
		debugger.setCurrentBreakpointIndex(2);
		assertEquals(2, debugger.getCurrentBreakpointIndex());
		
	}

	@Test
	public void testAddAndFindBreakpoint() throws InterruptedException, IOException {
		
		
		debugger.addBreakpoint("bp1");
		debugger.addBreakpoint("cp1");
		
		assert(debugger.findBreakpoint("bp1") >= 0);
		assert(debugger.findBreakpoint("cp1") > debugger.findBreakpoint("bp1"));
		assert(debugger.findBreakpoint("aaa") == -1);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRemoveBreakpointWithDuplicateIndices() throws InterruptedException, IOException, IndexOutOfBoundsException {	
		
		assertEquals(-1,debugger.getCurrentBreakpointIndex());
		debugger.addBreakpoint("aa");
		debugger.addBreakpoint("bp1");
		debugger.addBreakpoint("cp1");
		
		int[] indices = {1,0,1,2};
		
		debugger.removeBreakpoints(indices);
	}
	
	@Test
	public void testRemoveAndFindBreakpoints() throws InterruptedException, IOException {
		
		
		debugger.addBreakpoint("aa");
		debugger.addBreakpoint("bp1");
		debugger.addBreakpoint("cp1");
		
		assertEquals(0, debugger.findBreakpoint("aa"));
		
		int [] indices = {0};
		debugger.removeBreakpoints(indices);
		
		assertEquals(-1, debugger.findBreakpoint("aa"));
		
		int [] indices2 = {0,1};
		debugger.removeBreakpoints(indices2);
		
		assertEquals (0,debugger.getBreakpoints().size());
		
		debugger.addBreakpoint("aa");
		debugger.addBreakpoint("bp1");
		debugger.addBreakpoint("cp1");
		debugger.addBreakpoint("dp1");
		debugger.addBreakpoint("ep1");
		
		int [] indices3 = {1,3};
		
		debugger.removeBreakpoints(indices3);
		
		assertEquals(-1,debugger.findBreakpoint("bp1"));
		assertEquals(-1,debugger.findBreakpoint("dp1"));
		
		assertEquals(0,debugger.findBreakpoint("aa"));
		assertEquals(1,debugger.findBreakpoint("cp1"));
		assertEquals(2,debugger.findBreakpoint("ep1"));
	}

	@Test
	public void testSort() throws InterruptedException, IOException {
		
		int[] testArray = new int[unsortedArray.length];
		
		System.arraycopy (unsortedArray, 0, testArray, 0, unsortedArray.length);
		debugger.sort(testArray);
		for (int i=0; i<testArray.length; i++){
			assertEquals(sortedArray[i], testArray[i]);
		}
	}

}
