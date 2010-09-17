/*******************************************************************************
 * Copyright (c) 2005-2010 Dennis Wagelaar, Vrije Universiteit Brussel.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dennis Wagelaar, Vrije Universiteit Brussel
 *******************************************************************************/
package be.ac.vub.platformkit.java.ui.dialogs;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import be.ac.vub.platformkit.java.PlatformkitJavaResources;
import be.ac.vub.platformkit.ui.dialogs.PlatformKitTreeSelectionDialog;

/**
 * Platform API selection dialog 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformAPISelectionDialog extends PlatformKitTreeSelectionDialog {

	private boolean createOntology;

	/**
	 * Creates a new PlatformAPISelectionDialog
	 * @param parent
	 * @param labelProvider
	 * @param contentProvider
	 */
	public PlatformAPISelectionDialog(Shell parent, ILabelProvider labelProvider,
			ITreeContentProvider contentProvider) {
		super(parent, labelProvider, contentProvider);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.CheckedTreeSelectionDialog#createTreeViewer(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected CheckboxTreeViewer createTreeViewer(Composite parent) {
		CheckboxTreeViewer treeViewer = super.createTreeViewer(parent);
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				dlgPart.checkSingleLeafOnly(event);
			}
		});
		return treeViewer;
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.ui.dialogs.PlatformKitTreeSelectionDialog#createSelectionButtons(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createSelectionButtons(Composite composite) {
		final Composite btnControl = super.createSelectionButtons(composite);
        btnControl.setLayoutData(GridDataFactory.swtDefaults().create());
        btnControl.setLayout(new FormLayout());
		final Button createOntology = new Button(btnControl, SWT.CHECK);
		final FormData createOntologyData = new FormData();
		createOntologyData.top = new FormAttachment(0, 0);
		createOntologyData.left = new FormAttachment(0, 0);
		createOntologyData.right = new FormAttachment(100, 0);
		createOntology.setLayoutData(createOntologyData);
		createOntology.setText(PlatformkitJavaResources.getString("PlatformAPISelectionDialog.createOntology")); //$NON-NLS-1$
		createOntology.setSelection(isCreateOntology());
		createOntology.addSelectionListener(new SelectionAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				setCreateOntology(createOntology.getSelection());
			}
		});
		return btnControl;
	}

	/**
	 * @return whether or not the createOntology option is selected
	 */
	public boolean isCreateOntology() {
		return createOntology;
	}

	/**
	 * @param createOntology the createOntology to set
	 */
	protected void setCreateOntology(boolean createOntology) {
		this.createOntology = createOntology;
	}

}
