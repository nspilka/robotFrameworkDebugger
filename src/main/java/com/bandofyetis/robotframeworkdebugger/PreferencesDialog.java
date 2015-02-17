package com.bandofyetis.robotframeworkdebugger;

import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/**
 * A class representing a preferences dialog
 * 
 * @author nspilka
 *
 */
public class PreferencesDialog extends Dialog {

	protected Shell shlPreferences;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public PreferencesDialog(Shell parent, int style) {
		super(parent, style);
		setText("Preferences");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public void open() {
		createContents();
		shlPreferences.open();
		shlPreferences.layout();
		Display display = getParent().getDisplay();
		while (!shlPreferences.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shlPreferences = new Shell(getParent(), SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL);
		shlPreferences.setSize(449, 186);
		shlPreferences.setText("Preferences");
		shlPreferences.setLayout(new FormLayout());
		
		Button btnCheckStopAtStartOfTests = new Button(shlPreferences, SWT.CHECK);
		btnCheckStopAtStartOfTests.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		FormData fd_btnCheckButton = new FormData();
		fd_btnCheckButton.right = new FormAttachment(0, 391);
		fd_btnCheckButton.top = new FormAttachment(0, 10);
		fd_btnCheckButton.left = new FormAttachment(0, 10);
		btnCheckStopAtStartOfTests.setLayoutData(fd_btnCheckButton);
		btnCheckStopAtStartOfTests.setText("Break execution at the start of each test");
		
		Button btnOk = new Button(shlPreferences, SWT.NONE);
		FormData fd_btnOk = new FormData();
		fd_btnOk.left = new FormAttachment(0, 291);
		btnOk.setLayoutData(fd_btnOk);
		btnOk.setText("OK");
		btnOk.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					shlPreferences.close();
				}
		    });

		
		Button btnCancel = new Button(shlPreferences, SWT.NONE);
		fd_btnOk.top = new FormAttachment(btnCancel, 0, SWT.TOP);
		fd_btnOk.right = new FormAttachment(btnCancel, -6);
		FormData fd_btnCancel = new FormData();
		fd_btnCancel.bottom = new FormAttachment(100, -10);
		fd_btnCancel.left = new FormAttachment(0, 365);
		fd_btnCancel.right = new FormAttachment(100, -10);
		btnCancel.setLayoutData(fd_btnCancel);
		btnCancel.setText("Cancel");
		btnCancel.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				shlPreferences.close();
			}
	    });
		
	    // Set the OK button as the default, so
	    // user can type input and press Enter
	    // to dismiss
		shlPreferences.setDefaultButton(btnOk);

		

	}
}
