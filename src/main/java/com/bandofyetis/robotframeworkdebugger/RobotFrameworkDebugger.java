package com.bandofyetis.robotframeworkdebugger;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.bandofyetis.robotframeworkdebugger.RobotFrameworkDebugContext.ContextType;

/**
 * A class that implements a visual debugger via the Robot Framework's listener mechanism
 * @author nspilka
 *
 */
public class RobotFrameworkDebugger {

	public static final String LOG_EXPECTED_KEYWORD_OBJECT = "Context Stack corrupted. Expected KEYWORD Object";
	static final String LOG_EXPECTED_TEST_SUITE_OBJECT = "Context Stack corrupted. Expected TEST_SUITE Object";
	static final String LOG_EXPECTED_TEST_CASE_OBJECT = "Context Stack corrupted. Expected TEST_CASE Object";

	// An enumeration representing the way tests are being executed
	public enum StepMode{
		STEP_OVER,
		STEP_INTO,
		RUN_TO_BREAKPOINT
	}

	// logger
	static final Logger log = Logger.getLogger(RobotFrameworkDebugger.class);
	public static final int ROBOT_LISTENER_API_VERSION = 2;

	// A lock used to lock the text execution while the user performs code investigation
    private Object stepLock;

    // The current step mode
    private StepMode stepMode;

    // In Step Over mode the end keyword we are stepping over to...
    private String stepTarget;

    // All of the registeRed breakpoints
    private List<String> breakpoints;

    // Index in the list of the current breakpoint, so we can highlight it
    private int currentBreakpointIndex = -1;

    // A stack of debug contexts representing the current execution with respect to suites, tests and keywords
    private Stack<RobotFrameworkDebugContext> contextStack;

    // The ui thread
	final RobotFrameworkDebuggerUIThread gui;

	// Stop at the end of every test (controlled by preferences)
	boolean stopAtTestEnd = true;

	// Does the debugger have a gui?  Default to true.  False for testing.  Don't do waits if no gui to release
	private boolean hasGUI = false;

	/**
	 * The standard constructor
	 * @throws InterruptedException if the gui thread is interrupted
	 * @throws IOException if the properties file cannot be loaded (we package it in the jar)
	 */
	public RobotFrameworkDebugger() throws InterruptedException, IOException {
		this(true);
	}

	RobotFrameworkDebugger(boolean initGraphics)  throws InterruptedException, IOException{
		stepLock = new Object();
		stepMode = StepMode.STEP_INTO;
		stepTarget = null;
		breakpoints = new LinkedList<String>();
		contextStack = new Stack<RobotFrameworkDebugContext>();

		// push a context for the suite
		RobotFrameworkDebugContext context = new RobotFrameworkDebugContext();
		context.setContextType(ContextType.TEST_SUITE);
		contextStack.push(context);

		gui = new RobotFrameworkDebuggerUIThread(this, stepLock);
		this.hasGUI = initGraphics;

		if (initGraphics){
			Thread t = new Thread(gui);
			t.start();

			// Give UI a second to start
			Thread.sleep(1000);
		}
	}


    /**
     * Start suite listener method (defined by Robot Framework Listener API)
     * @param name the name of the test suite
     * @param attrs an attribute map for the test suite
     */
	public void startSuite(String name, Map<String, Object> attrs)  {
		// get and setup context for the suite
		RobotFrameworkDebugContext context =  contextStack.peek();
		if (context.getContextType() != ContextType.TEST_SUITE){
			log.error(LOG_EXPECTED_TEST_SUITE_OBJECT);
		}
		context.setItemName(name);
		context.clearVariables();
		context.setItemAttribs(attrs);

		// push a context for the test case
		RobotFrameworkDebugContext tcContext = new RobotFrameworkDebugContext();
		tcContext.setContextType(ContextType.TEST_CASE);
		contextStack.push(tcContext);

		// we have a context now, so update gui and enable buttons
		updateGUI();
		enableControlButtons(true);
    }


	/**
	 * Start test listener method (defined by Robot Framework Listener API)
	 * @param name name of the test
	 * @param attrs map of the test's attributes
	 */
    public void startTest(String name, Map<String, Object> attrs)  {
    	// Check the top of the stack
    	RobotFrameworkDebugContext context = contextStack.peek();
    	if (context.getContextType() != ContextType.TEST_CASE){
			log.error(LOG_EXPECTED_TEST_CASE_OBJECT);
		}
    	// set name and attributes for the test case
    	context.setItemName(name);
    	context.setItemAttribs(attrs);

    	// clear variables from the last test
        context.clearVariables();

        // Now push a new keyword level object for all keywords in this test
        RobotFrameworkDebugContext kwContext = new RobotFrameworkDebugContext();
		kwContext.setContextType(ContextType.KEYWORD);
		contextStack.push(kwContext);

		updateGUI();
    }

    /**
     * End test listener method (defined by Robot Framework Listener API)
     * @param name the test that is finishing
     * @param attrs a map of attributes for the test
     * @throws InterruptedException
     */
    public void endTest(String name, Map<String, Object> attrs) throws InterruptedException{
    	 currentBreakpointIndex = -1;

    	// test is over, update the ui.  We may pause on test finishing
    	updateGUI();

    	// Check top of stack and make sure we have a keyword object
    	RobotFrameworkDebugContext context = contextStack.peek();
    	if (context.getContextType() != ContextType.KEYWORD){
			log.error(LOG_EXPECTED_KEYWORD_OBJECT);
		}
    	// pop off the old keyword object, we'll put a new one on when the next test starts
    	contextStack.pop();

    	// Apply status attribute to the gui
        String status = attrs.get("status").toString();
        if (hasGUI){
        	gui.setTestResult(name, status);
        }

        // if stop at end of test is set, stop when test finishes
    	if (stopAtTestEnd){
    		stepTarget = null;
    		stepMode = StepMode.STEP_INTO;

    		if (hasGUI){
	    		synchronized (stepLock) {
	    			stepLock.wait();
	    		}
    		}
    	}
    }

    /**
     * End suite listener method (defined by Robot Framework Listener API)
     * @param name name of the suite that is ending
     * @param attrs a map of attributes for the suite
     * @throws IOException
     */
    public void endSuite(String name, Map<String, Object> attrs) {
    	RobotFrameworkDebugContext context = contextStack.peek();
    	if (context.getContextType() != ContextType.TEST_CASE){
			log.error(LOG_EXPECTED_TEST_CASE_OBJECT);
		}
    	// pop off old test case object, we'll put a new one on when the next test suite starts
    	contextStack.pop();

    }

    /**
     * close listener method (defined by Robot Framework Listener API) - called when Robot Framework closes
     *
     * @throws InterruptedException
     */
    public void close() throws InterruptedException {

    	if (hasGUI){
    		gui.clearTestData();
    		enableControlButtons(false);
	    	synchronized (stepLock) {
				stepLock.wait();
			}
    	}
    	System.exit(0);
    }

    /**
     * Start Keyword listener method (defined by Robot Framework Listener API) - called before keyword is executed
     * @param name the name of the keyword about to be executed
     * @param attrs a map of attributes for the keyword
     * @throws InterruptedException
     */
    public void startKeyword(String name, Map<String, Object> attrs) throws InterruptedException{
    	currentBreakpointIndex = -1;

    	// Check for keyword context, then set
    	RobotFrameworkDebugContext context = contextStack.peek();
    	if (context.getContextType() != ContextType.KEYWORD){
			log.error(LOG_EXPECTED_KEYWORD_OBJECT);
		}
    	context.setItemName(name);
    	context.setItemAttribs(attrs);
    	context.incrementLineNumber();

    	// If we should break, then wait
    	if ((stepMode == StepMode.STEP_INTO) || (shouldBreak(name))){
    		// only update if we hit a breakpoint

    		if (hasGUI){
    			updateGUI();
    			synchronized (stepLock) {
    				stepLock.wait();
    			}
    		}
    	}

    	// push on an element for any child keywords
        RobotFrameworkDebugContext childContext = new RobotFrameworkDebugContext();
        childContext.setContextType(ContextType.KEYWORD);
		contextStack.push(childContext);

    }


    /**
     * End Keyword listener method (defined by Robot Framework Listener API) - called after a keyword finishes executing
     * @param name the name of the keyword that just finished executing
     * @param attributes a map of attributes for the keyword
     */
	public void endKeyword(String name, Map<String, Object> attributes){
		// Check that the child context is on top of the stack
		RobotFrameworkDebugContext context = contextStack.peek();
		if (context.getContextType() != ContextType.KEYWORD){
			log.error(LOG_EXPECTED_KEYWORD_OBJECT);
		}

		// pop the child context off the stack
    	contextStack.pop();


    	// If we were stepping over, and this was the keyword we were stepping over,
    	// move back into STEP_INTO mode where we step keyword by keyword
    	if ((stepMode == StepMode.STEP_OVER) && (name.equals(stepTarget))){
    		stepTarget = null;
    		stepMode = StepMode.STEP_INTO;
    	}


    }

	/**
	 * Log Message listener method (defined by Robot Framework Listener API) - called when a message is written to the log
	 * @param msg A map of the messages being written to the log
	 */
    public void logMessage(Map<String, Object> msg){
    	String message = (String) msg.get("message");

    	// If a variable is being logged, get its value
    	if ((message.startsWith("${")) 	|| (message.startsWith("@{"))){
            //Split into name and value
    		String[] variableList = message.split("=");
    		if (variableList.length >= 2){
    			// we need to set the variable in the context that is second from the top (ie. not the context of
    			// this context's child)
    			RobotFrameworkDebugContext top = contextStack.pop();
    			contextStack.peek().updateVariable(variableList[0].trim(), variableList[1].trim());
    			// push the old top back on
    			contextStack.push(top);
    		}
    	}
    }

    /**
     * Put the debugger in StepOver mode
     */
    public synchronized void setStepOver (){
    	stepMode = StepMode.STEP_OVER;

    	// get the keyword name for the top item on the stack.  The child element shouldn't be created yet
    	RobotFrameworkDebugContext context = contextStack.peek();

    	// if this is something we can step over, set it up, otherwise, stay in STEP_INTO mode
    	if (context.getContextType() == ContextType.KEYWORD && !(context.getItemName().isEmpty())){
    		stepTarget = contextStack.peek().getItemName();
    	}
    	else{
    		stepMode = StepMode.STEP_INTO;
    	}
    }

    /**
     * Put the debugger in StepInto mode
     */
    public synchronized void setStepInto(){
    	stepMode = StepMode.STEP_INTO;
    	stepTarget = null;
    }

    /**
     * Put the debugger in run to breakpoint mode
     */
	public synchronized void setRunToBreakpoint() {

		stepMode = StepMode.RUN_TO_BREAKPOINT;
    	stepTarget = null;
	}

    /**
     * Class level method for testing
     * @return the current step mode
     */
    StepMode  getStepMode(){
    	return stepMode;
    }

    /**
     * Class level method for testing
     * @return the current step Target
     */
    String getStepTarget(){
    	return stepTarget;
    }

    /**
     * Class level method for testing
     * @return context stack
     */
	Stack<RobotFrameworkDebugContext> getContextStack(){
		return contextStack;
	}

	/**
	 * Gets the index of the breakpoint we are currently stopped on
	 * @return the currentBreakpointIndex (-1) if there is no index
	 */
	public int getCurrentBreakpointIndex() {
		return currentBreakpointIndex;
	}

	/**
	 * Gets the list of all breakpoints
	 * @return list of all breakpoints
	 */
	public List<String> getBreakpoints() {
		return breakpoints;
	}

	/**
	 * Get the string representing the currentBreakpoint
	 * @return the currentBreakpoint string or null if none exists
	 */
	public String getCurrentBreakpointAsString(){
		if (currentBreakpointIndex == -1){
			return null;
		}
		else{
			return breakpoints.get(currentBreakpointIndex);
		}
	}

	/**
	 * Find the index of a breakpoint in the list of breakpoints
	 * @param breakpointText
	 * @return the index of the breakpoint in the breakpoint list
	 */
	public int findBreakpoint(String breakpointText){
		if (breakpointText == null || breakpointText.isEmpty()){
			return -1;
		}
		for (int i = 0; i < breakpoints.size(); i++){
			String curBreakpoint = breakpoints.get(i);
			if (curBreakpoint.equals(breakpointText)){
				return i;
			}
		}
		return -1;
	}

	/**
	 * Method to set the current breakpoint index.  Package level access for testing
	 * @param index the breakpoint index
	 */
	void setCurrentBreakpointIndex(int index) throws IndexOutOfBoundsException{
		if (index >= -1 && index < breakpoints.size())
			currentBreakpointIndex = index;
		else{
			currentBreakpointIndex  = -1;
			throw new IndexOutOfBoundsException();
		}
	}
	/**
	 * Add a breakpoint to the list of breakpoint regular expressions
	 * @param breakpointText the regular expression to add to the breakpoint list
	 */
	public synchronized void addBreakpoint(String breakpointText) {
		String currentBreakpoint = getCurrentBreakpointAsString();
		breakpoints.add(breakpointText);
		Collections.sort(breakpoints, String.CASE_INSENSITIVE_ORDER);
		currentBreakpointIndex = findBreakpoint(currentBreakpoint);
		updateBreakpoints();
	}

	/**
	 * Remove breakpoints based on the indices passed in
	 * @param indices an array of indices to be removed, not necessarily sorted
	 */
	public synchronized void removeBreakpoints(int[] indices) {
		if (indices.length == 0) return;

		String currentBreakpoint = getCurrentBreakpointAsString();

		// copy and sort indices
		int [] newIndices = new int [indices.length];
		System.arraycopy (indices, 0, newIndices, 0, indices.length);
		sort (newIndices);

		// Check array for duplicates
		for (int i=0; i < (newIndices.length - 1); i++){
			if (newIndices[i] == newIndices[i+1]){
				throw new IllegalArgumentException("indices array cannot contain duplicates");
			}
		}

		// go backwards to ensure the right elements get deleted as the Breakpoints list is shrunk
		for (int i = newIndices.length-1; i >= 0; i--){
			breakpoints.remove(indices[i]);
		}
		currentBreakpointIndex = findBreakpoint(currentBreakpoint);
		updateBreakpoints();
	}

	/**
	 * Check if the given name matches a breakpoint
	 * @param name the name to check against the list of breakpoint regular expressions
	 * @return true if name mathes a regex in the breakpoints list
	 */
	private boolean shouldBreak(String name) {
		int ct = 0;

		// Go through the list of breakpoints
		for (Iterator<String> it = breakpoints.iterator(); it.hasNext(); ) {
			String breakpoint = it.next();
			// add a .* to the front of the regex since we get keyword names as ${a} = the keyword
			// add a .* to the end since we ask them to enter part of the keyword name to break on
			if (name.matches(".*"+breakpoint+".*")){
				currentBreakpointIndex = ct;
				return true;
			}
			ct++;
		}
		return false;
	}

	/**
	 * Do a shell sort on an array of integers
	 *
	 * @param items the integer array to be sorted
	 */
	void sort (int [] items) {
		/* Shell Sort from K&R, pg 108 */
		int length = items.length;
		for (int gap=length/2; gap>0; gap/=2) {
			for (int i=gap; i<length; i++) {
				for (int j=i-gap; j>=0; j-=gap) {
			   		if (items [j] > items [j + gap]) {
						int swap = items [j];
						items [j] = items [j + gap];
						items [j + gap] = swap;
			   		}
		    	}
		    }
		}
	}

	/**
	 * Update the GUI if we have one
	 */
	private void updateGUI(){
		if (hasGUI){
			gui.update(contextStack);
		}
	}

	/**
	 * Update breakpoints if a gui is running
	 */
	private void updateBreakpoints(){
		if (hasGUI){
			gui.updateBreakpoints(breakpoints);

		}
	}

	/**
	 * Enable/Disable control buttons if a gui is running
	 * @param bEnable true if they should be enabled, false to disable
	 */
	private void enableControlButtons(boolean bEnable){
		if(hasGUI){
			gui.setControlButtonsEnabled(bEnable);
		}
	}


}

