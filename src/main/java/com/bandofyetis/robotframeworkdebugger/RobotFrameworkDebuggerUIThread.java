/*
 * @author Nik Spilka
 * @version 1.0
 *
 * Copyright (c) 2014
 * Do not reproduce without permission in writing.
 * All rights reserved.
 */

package com.bandofyetis.robotframeworkdebugger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;




import com.bandofyetis.robotframeworkdebugger.RobotFrameworkDebugContext.ContextType;

public class RobotFrameworkDebuggerUIThread implements Runnable{

	protected Shell 		shell;
	private Display 		display;
	private Object 			stepLock;

	private TabFolder 		tabFolder;
	private Label 			lblTestSuiteValue;
	private Label 			lblTestCaseValue;
	private Label 			lblKeywordValue;
	private Label 			lblArgumentsValue;
	private Table 			tblCallStackItem;
	private Table 			tblVariables;
	private StyledText 		textDocumentation;
	private RobotFrameworkDebugger controller;
	private Table 			tblBreakpoints;
	private Text 			textBreakpoint;
	private Table 			tblTestResults;
	private ToolItem 		tltmStepOver;
	private ToolItem 		tltmStepInto;
	private ToolItem 		tltmResume;
	private MenuItem 		mntmRemoveBreakpoint;
	private PreferencesDialog prefsDialog;
	private Image			imgCurrentBreakpoint;

	/**
	 * Logger
	 */
	static final Logger log = Logger.getLogger(RobotFrameworkDebuggerUIThread.class);


	/**
	 * Open the window.
	 * @wbp.parser.entryPoint
	 */
	public void run() {
		display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Constructor
	 * @param controller The Robot Framework Debugger instance that controls the window in the MVC model
	 * @param stepLock The object that the debugger locks on when pausing execution of keywords
	 */
	public RobotFrameworkDebuggerUIThread(RobotFrameworkDebugger controller, Object stepLock){
		this.controller = controller;
		this.stepLock = stepLock;
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell(SWT.SHELL_TRIM & (~SWT.RESIZE));
		shell.setSize(530, 505);
		shell.setText("Robot Framework Debugger");
		shell.setLayout(new GridLayout(1, false));

		// On close window kill RobotFramework Test
		shell.addShellListener(new ShellAdapter() {
			public void shellClosed(ShellEvent e) {
				System.exit(0);
			}
		});
		createToolbar();
		createLabelPanel();

		prefsDialog = new PreferencesDialog(shell, SWT.NONE);

		createTabFolder();
		createCallStackTab();
		createVariablesTab();
		createDocumentationTab();
		createBreakpointsTab();
		createTestResultsTab();
	}

	/**
	 * Create the toolbar
	 */
	protected void createToolbar(){
		ToolBar toolBar = new ToolBar(shell, SWT.FLAT | SWT.RIGHT);
		toolBar.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));

		tltmStepOver = new ToolItem(toolBar, SWT.NONE);
		tltmStepOver.setEnabled(false);
		InputStream in = getClass().getClassLoader().getResourceAsStream("arrow_turn_right.png");
		if(in != null){
			tltmStepOver.setImage(new Image(display, in));
		}
		tltmStepOver.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				controller.setStepOver();
				synchronized (stepLock) { stepLock.notify(); }
			}
		});
		tltmStepOver.setText("Step Over");

		tltmStepInto = new ToolItem(toolBar, SWT.NONE);
		tltmStepInto.setEnabled(false);
		in = getClass().getClassLoader().getResourceAsStream("arrow_down.png");
		if(in != null){
			tltmStepInto.setImage(new Image(display, in));
		}
		tltmStepInto.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				controller.setStepInto();
				synchronized (stepLock) { stepLock.notify(); }
			}
		});
		tltmStepInto.setText("Step Into");

		tltmResume = new ToolItem(toolBar, SWT.NONE);
		tltmResume.setEnabled(false);
		in = getClass().getClassLoader().getResourceAsStream("control_play_blue.png");
		if(in != null){
			tltmResume.setImage(new Image(display, in));
		}
		tltmResume.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				controller.setRunToBreakpoint();
				synchronized (stepLock) { stepLock.notify(); }
			}
		});
		tltmResume.setText("Resume");

		ToolItem tltmStop = new ToolItem(toolBar, SWT.NONE);
		in = getClass().getClassLoader().getResourceAsStream("control_stop_blue.png");
		if(in != null){
			tltmStop.setImage(new Image(display, in));
		}
		tltmStop.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// wake anything up that may be waiting
				synchronized (stepLock) { stepLock.notify(); }
				System.exit(0);
			}
		});
		tltmStop.setText("Stop");

		ToolItem tltmPreferences = new ToolItem(toolBar, SWT.NONE);
		tltmPreferences.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				prefsDialog.open();
			}
		});
		in = getClass().getClassLoader().getResourceAsStream("application_form_edit.png");
		if(in != null){
			tltmPreferences.setImage(new Image(display, in));
		}
		tltmPreferences.setText("Preferences");
	}

	/**
	 * Create the label panel above the tabs that indicates which test, suite and keyword are running
	 */
	protected void createLabelPanel(){
		Composite compositeTestSuiteRow = new Composite(shell, SWT.NONE);
		compositeTestSuiteRow.setLayout(new RowLayout(SWT.HORIZONTAL));
		GridData gd_compositeTestSuiteRow = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_compositeTestSuiteRow.widthHint = 495;
		compositeTestSuiteRow.setLayoutData(gd_compositeTestSuiteRow);

		Label lblTestSuiteName = new Label(compositeTestSuiteRow, SWT.NONE);
		lblTestSuiteName.setLayoutData(new RowData(73, 15));
		lblTestSuiteName.setText("Test Suite:");

		lblTestSuiteValue = new Label(compositeTestSuiteRow, SWT.NONE);
		lblTestSuiteValue.setLayoutData(new RowData(350, SWT.DEFAULT));

		Composite compositeTestCaseRow = new Composite(shell, SWT.NONE);
		compositeTestCaseRow.setLayout(new RowLayout(SWT.HORIZONTAL));

		Label lblTestCase = new Label(compositeTestCaseRow, SWT.NONE);
		lblTestCase.setLayoutData(new RowData(73, 15));
		lblTestCase.setText("Test Case:");

		lblTestCaseValue = new Label(compositeTestCaseRow, SWT.NONE);
		lblTestCaseValue.setLayoutData(new RowData(367, SWT.DEFAULT));

		Composite compositeKeywordRow = new Composite(shell, SWT.NONE);
		compositeKeywordRow.setLayout(new RowLayout(SWT.HORIZONTAL));

		Label lblKeywordName = new Label(compositeKeywordRow, SWT.NONE);
		lblKeywordName.setLayoutData(new RowData(73, 15));
		lblKeywordName.setText("Keyword:");

		lblKeywordValue = new Label(compositeKeywordRow, SWT.NONE);
		lblKeywordValue.setLayoutData(new RowData(367, -1));

		Composite compositeArgumentsRow = new Composite(shell, SWT.NONE);
		compositeArgumentsRow.setLayout(new RowLayout(SWT.HORIZONTAL));

		Label lblArgumentsName = new Label(compositeArgumentsRow, SWT.NONE);
		lblArgumentsName.setLayoutData(new RowData(73, 15));
		lblArgumentsName.setText("Arguments:");

		lblArgumentsValue = new Label(compositeArgumentsRow, SWT.NONE);
		lblArgumentsValue.setLayoutData(new RowData(367, -1));
	}

	/**
	 * Create tab folder - the container that holds all of the tabs
	 */
	private void createTabFolder(){
		tabFolder = new TabFolder(shell, SWT.NONE);
		GridData gd_tabFolder = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_tabFolder.widthHint = 495;
		gd_tabFolder.heightHint = 289;
		tabFolder.setLayoutData(gd_tabFolder);
	}

	/**
	 * Create the callstack tab
	 */
	private void createCallStackTab(){
		TabItem tbtmCallStack = new TabItem(tabFolder, SWT.NONE);
		tbtmCallStack.setText("Call Stack");

		tblCallStackItem = new Table(tabFolder, SWT.BORDER | SWT.FULL_SELECTION);
		tblCallStackItem.setHeaderVisible(true);
		tbtmCallStack.setControl(tblCallStackItem);
		tblCallStackItem.setLinesVisible(true);

		TableColumn tblclmnCallstackType = new TableColumn(tblCallStackItem, SWT.NONE);
		tblclmnCallstackType.setText("Type");
		tblclmnCallstackType.setWidth(100);

		TableColumn tblclmnCallStack = new TableColumn(tblCallStackItem, SWT.NONE);
		tblclmnCallStack.setText("Name");
		tblclmnCallStack.setWidth(300);

		TableColumn tblclmnLineNumber = new TableColumn(tblCallStackItem, SWT.LEFT);
		tblclmnLineNumber.setWidth(90);
		tblclmnLineNumber.setText("Line Number");
	}

	/**
	 * Create the variables tab
	 */
	private void createVariablesTab(){
		TabItem tbtmVariables = new TabItem(tabFolder, SWT.NONE);
		tbtmVariables.setText("Variables");

		tblVariables = new Table(tabFolder, SWT.BORDER | SWT.FULL_SELECTION);
		tblVariables.setHeaderVisible(true);
		tbtmVariables.setControl(tblVariables);
		tblVariables.setLinesVisible(true);

		TableColumn tblclmnVariableName = new TableColumn(tblVariables, SWT.NONE);
		tblclmnVariableName.setWidth(100);
		tblclmnVariableName.setText("Variable Name");

		TableColumn tblclmnValue = new TableColumn(tblVariables, SWT.NONE);
		tblclmnValue.setWidth(389);
		tblclmnValue.setText("Value");
	}

	/**
	 * Create the documentation tab
	 */
	private void createDocumentationTab(){
		TabItem tbtmDocumentation = new TabItem(tabFolder, SWT.NONE);
		tbtmDocumentation.setText("Documentation");

		textDocumentation = new StyledText(tabFolder, SWT.BORDER);
		textDocumentation.setDoubleClickEnabled(false);
		textDocumentation.setEditable(false);
		tbtmDocumentation.setControl(textDocumentation);
	}

	/**
	 * Create the breakpoints tab
	 */
	private void createBreakpointsTab(){
		InputStream in = getClass().getClassLoader().getResourceAsStream("arrow_right.png");
		if(in != null){
			imgCurrentBreakpoint = new Image(display, in);
		}
		else{
			imgCurrentBreakpoint = null;
		}
		TabItem tbtmBreakpoints = new TabItem(tabFolder, SWT.NONE);
		tbtmBreakpoints.setText("Breakpoints");

		Composite breakpointComposite = new Composite(tabFolder, SWT.NONE);
		tbtmBreakpoints.setControl(breakpointComposite);
		breakpointComposite.setLayout(new GridLayout(1, false));

		tblBreakpoints = new Table(breakpointComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblBreakpoints.setLinesVisible(true);
		tblBreakpoints.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		TableColumn tblclmnImage = new TableColumn(tblBreakpoints, SWT.CENTER);
		tblclmnImage.setResizable(false);
		tblclmnImage.setWidth(20);
		tblclmnImage.setText("image");

		TableColumn tblclmnBreakpoints = new TableColumn(tblBreakpoints, SWT.NONE);
		tblclmnBreakpoints.setResizable(false);
		tblclmnBreakpoints.setWidth(461);
		tblclmnBreakpoints.setText("Breakpoint");

		Menu menu = new Menu(shell, SWT.POP_UP);

		mntmRemoveBreakpoint = new MenuItem(menu, SWT.PUSH);
		mntmRemoveBreakpoint.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					int[] indices = tblBreakpoints.getSelectionIndices();
					controller.removeBreakpoints(indices);
					// do an inline update until the next full table redraw
			    	tblBreakpoints.remove(tblBreakpoints.getSelectionIndices());
			    }
		    });
		in = getClass().getClassLoader().getResourceAsStream("delete.png");
		if(in != null){
			mntmRemoveBreakpoint.setImage(new Image(display, in));
		}
		mntmRemoveBreakpoint.setText("Delete Breakpoints");


		menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				int numSelected = tblBreakpoints.getSelectionCount();
				if (numSelected == 0){
					mntmRemoveBreakpoint.setText("Delete Breakpoint");
					mntmRemoveBreakpoint.setEnabled(false);
					return;
				}
				else{
					mntmRemoveBreakpoint.setEnabled(true);
				}

				// Handle grammar
				if (numSelected == 1){
					mntmRemoveBreakpoint.setText("Delete Breakpoint");
				}
				else{
					mntmRemoveBreakpoint.setText("Delete Breakpoints");
				}
			}
		});
		tblBreakpoints.setMenu(menu);

		// Create new breakpoint dialog and button
		Composite breakpointEntryComposite = new Composite(breakpointComposite, SWT.NONE);
		breakpointEntryComposite.setLayout(new GridLayout(2, false));
		GridData gd_breakpointEntryComposite = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_breakpointEntryComposite.widthHint = 482;
		breakpointEntryComposite.setLayoutData(gd_breakpointEntryComposite);

		textBreakpoint = new Text(breakpointEntryComposite, SWT.BORDER);
		textBreakpoint.setToolTipText("Enter part of the keyword name to break on");
		GridData gd_textBreakpoint = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_textBreakpoint.widthHint = 296;
		textBreakpoint.setLayoutData(gd_textBreakpoint);
		textBreakpoint.addListener(SWT.DefaultSelection, new Listener() {				
				public void handleEvent(Event e) {
		    	  String breakpointText = textBreakpoint.getText();
		    	  if (breakpointText != null && breakpointText.length() > 0){
		    		  controller.addBreakpoint(breakpointText);
		    		  textBreakpoint.setText("");
		    	  }
		        }


		      });


		Button btnAddBreakpoint = new Button(breakpointEntryComposite, SWT.NONE);
		btnAddBreakpoint.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String breakpointText = textBreakpoint.getText();
		    	  if (breakpointText != null && breakpointText.length() > 0){
		    		  controller.addBreakpoint(breakpointText);
		    		  textBreakpoint.setText("");
		    	  }
			}
		});
		btnAddBreakpoint.setText("Add  Breakpoint");
	}

	/**
	 * Create the test results tab
	 */
	private void createTestResultsTab(){
		TabItem tbtmTestresults = new TabItem(tabFolder, SWT.NONE);
		tbtmTestresults.setText("TestResults");

		tblTestResults = new Table(tabFolder, SWT.BORDER | SWT.FULL_SELECTION);
		tbtmTestresults.setControl(tblTestResults);
		tblTestResults.setHeaderVisible(true);
		tblTestResults.setLinesVisible(true);

		TableColumn tblclmTestName = new TableColumn(tblTestResults, SWT.NONE);
		tblclmTestName.setWidth(395);
		tblclmTestName.setText("Test Name");

		TableColumn tblclmnResult = new TableColumn(tblTestResults, SWT.NONE);
		tblclmnResult.setWidth(100);
		tblclmnResult.setText("Result");
	}

	/**
	 * Clear all tabs and labels (Don't clear breakpoints or test results -- they persist)
	 */
	private void clearAll(){
		lblTestSuiteValue.setText("");
		lblTestCaseValue.setText("");
		lblKeywordValue.setText("");
		lblArgumentsValue.setText("");
		textDocumentation.setText("");

		tblVariables.removeAll();
		tblCallStackItem.removeAll();
	}

	/**
	 * A hook to clear the data with the thread running in the context of the GUI
	 */
	public synchronized void clearTestData()  {
		if (display == null || display.isDisposed())
			return;
		display.asyncExec(new Runnable() {
			public void run() {
				clearAll();
			}
		});
	}

	/**
	 * Update the GUI based on the model (ie the stack of debug contexts)
	 * @param contextStack - A stack of debug contexts used to update the gui.  Represents the state of the running test suite
	 */
	public synchronized void update(final Stack<RobotFrameworkDebugContext> contextStack)  {
		if (display == null || display.isDisposed())
			return;
		display.asyncExec(new Runnable() {
			public void run() {
				clearAll();

				// A map for variables that handles scoping of variables
				Map<String, String> variables = new HashMap<String, String>();

				// Traverse stack from bottom to top
				for (Iterator<RobotFrameworkDebugContext> it = contextStack.iterator(); it.hasNext(); ) {

					RobotFrameworkDebugContext context = it.next();

					// set labels based on stack
					if (context.getContextType() == ContextType.TEST_SUITE){
						lblTestSuiteValue.setText(context.getItemName());
					}
					else if (context.getContextType() == ContextType.TEST_CASE){
						lblTestCaseValue.setText(context.getItemName());
					}

					// if it is the last keyword, take the data from it
					else if (!it.hasNext()){
						// must be a keyword
						lblKeywordValue.setText(context.getItemName());
						Map<String, Object> keywordAttrs = context.getItemAttribs();
						String strArgs = "";
						String strDocs = "";
						if (keywordAttrs != null){
							List args = (List)keywordAttrs.get("args");
					        for (int i=0; i < args.size(); i++) {
					          	strArgs += (i>0?", ":"")+args.get(i);
					        }
							lblArgumentsValue.setText(strArgs);

							strDocs = (String)keywordAttrs.get("doc");
							if (strDocs != null){
								textDocumentation.setText(strDocs);
							}
							else{
								textDocumentation.setText("");
							}
						}
					}

					// Now add the context variables to our variables map - since we are traversing from
					// bottom to top, if 2 variables have the same name, the scope closest to the top of the stack is
					// displayed
					variables.putAll(context.getVariables());
				}

				// Update callstack table - items are added to table in order listed!  So we can't roll it into the loop
				// create table items for the callstack table
				int stackSize = contextStack.size();
				final TableItem[] callstackItems = new TableItem[stackSize];
				for (int pos = stackSize -1; pos >= 0; pos--){
					RobotFrameworkDebugContext context = contextStack.elementAt(pos);
					// add to the call stack items
					int lineNumber = context.getLineNumber();
					callstackItems[pos] = new TableItem(tblCallStackItem,SWT.NONE);
					callstackItems[pos].setText(
							new String[] {
								(String) context.getContextTypeString(),
								(String) context.getItemName(),
								(lineNumber > 0) ? Integer.toString(lineNumber):""});
				}

				// Update test Variable table
				List<Set<Entry<String,Object>>> testVarList = CollectionHelpers.sortMapByKey(variables);
				final TableItem[] items = new TableItem[testVarList.size()];

				int i=0;
				for (Iterator it = testVarList.iterator(); it.hasNext(); ) {
					Map.Entry entry = (Map.Entry)it.next();
					items[i] = new TableItem(tblVariables,SWT.NONE);
					items[i].setText(
							new String[] {
								(String) entry.getKey(),
								(String) entry.getValue()});
					i++;
			    }

				//Update the breakpoints table
				updateAllBreakpoints(controller.getBreakpoints());

			}
		});
	}

	/**
	 * Set the state of the "stepping and running" buttons
	 * @param bEnabled true if enabled, false if disabled
	 */
	public synchronized void setControlButtonsEnabled(final boolean bEnabled)  {

		if (display == null || display.isDisposed())
			return;
		display.asyncExec(new Runnable() {
			public void run() {
				tltmStepOver.setEnabled(bEnabled);
				tltmStepInto.setEnabled(bEnabled);
				tltmResume.setEnabled(bEnabled);
			}
		});
	}

	/**
	 * Set a new test result in the results tab
	 *
	 * @param testName name of the test
	 * @param result a String "PASS" or "FAIL"
	 */
	public synchronized void setTestResult(final String testName, final String result)  {

		if (display == null || display.isDisposed())
			return;
		display.asyncExec(new Runnable() {
			public void run() {
				final Color red = display.getSystemColor(SWT.COLOR_RED);
				final Color green = display.getSystemColor(SWT.COLOR_DARK_GREEN);

				final TableItem testResult = new TableItem(tblTestResults, SWT.NONE);
				testResult.setText(new String[] {testName, result});
				if (result.equalsIgnoreCase("PASS")){
					testResult.setForeground(green);
				}
				else{
					testResult.setForeground(red);
				}
			}
		});
	}

	/**
	 * Replace the data in the breakpoints tab with list of breakpoints passed in
	 * @param breakpoints the breakpoints to update the tab with
	 */
	public void updateBreakpoints(final List<String> breakpoints) {
		if (display == null || display.isDisposed())
			return;
		display.asyncExec(new Runnable() {
			public void run() {
				updateAllBreakpoints(breakpoints);
			}
		});

	}

	/**
	 * Private function to handle breakpoint update.  Can be called from update or updateBreakpoints
	 * @param breakpoints the breakpoints to update the tab with
	 */
	private void updateAllBreakpoints(List<String> breakpoints){
		int currentBreakpointIndex = controller.getCurrentBreakpointIndex();
		tblBreakpoints.removeAll();
		final TableItem[] items = new TableItem[breakpoints.size()];

		int i=0;
		for (Iterator<String> it = breakpoints.iterator(); it.hasNext(); ) {
			String breakpoint = it.next();
			items[i] = new TableItem(tblBreakpoints,SWT.NONE);
			items[i].setText(
					new String[] {
						"",
						(String) breakpoint
						});
			if (i == currentBreakpointIndex){
				if(imgCurrentBreakpoint != null){
					items[i].setImage(imgCurrentBreakpoint);
				}
			}
			i++;
	    }
	}

}
