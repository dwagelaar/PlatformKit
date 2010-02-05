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
package be.ac.vub.platformkit.ui.dialogs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;

import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.util.INamedOntologyProvider;

/**
 * Dialog for selecting a platform instance specification.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformSpecDialog extends ListDialog {

	protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);

	private boolean builtinSelected = true;

	protected PlatformKitDialogPart dlgPart = new PlatformKitDialogPart();

	/**
	 * Creates a new {@link PlatformSpecDialog}.
	 * @param parentShell
	 * @throws IOException
	 */
	public PlatformSpecDialog(Shell parentShell) throws IOException {
		super(parentShell);
		setContentProvider(new ArrayContentProvider());
		initPlatformSpecList();
	}

	/**
	 * Creates the list of built-in platform instances.
	 * @throws IOException
	 */
	private void initPlatformSpecList() throws IOException {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		if (registry == null) {
			logger.warning(PlatformkitEditorPlugin.getPlugin().getString("PlatformSpecDialog.registryNotFound")); //$NON-NLS-1$
			return;
		}
		final List<InputStream> content = new ArrayList<InputStream>();
		final Map<InputStream, String> labels = new HashMap<InputStream, String>();
		IExtensionPoint point = registry.getExtensionPoint(PlatformkitEditorPlugin.PLATFORMSPEC_EXT_POINT);
		IExtension[] extensions = point.getExtensions();
		for (int i = 0 ; i < extensions.length ; i++) {
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			for (int j = 0 ; j < elements.length ; j++) {
				try {
					INamedOntologyProvider provider = (INamedOntologyProvider)
					elements[j].createExecutableExtension("provider"); //$NON-NLS-1$
					String[] names = provider.getOntologyNames();
					InputStream[] streams = provider.getOntologies();
					for (int k = 0; k < streams.length; k++) {
						content.add(streams[k]);
						labels.put(streams[k], names[k]);
					}
				} catch (CoreException e) {
					throw new IOException(e.getLocalizedMessage());
				}
			}
		}
		setInput(content);
		setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (labels.containsKey(element)) {
					return labels.get(element).toString();
				} else {
					return super.getText(element);
				}
			}
		});
		if (content.size() > 0) {
			setInitialSelections(new Object[] {content.get(0)});
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.ListDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = dlgPart.createDialogArea(parent);
		Composite panel = (Composite) super.createDialogArea(composite);
		dlgPart.setContainedAreaLayoutData(panel);

		Button builtinBtn = new Button(panel, SWT.RADIO);
		builtinBtn.setText("Built-in platform specification");
		builtinBtn.setSelection(true);

		Button fromFileBtn = new Button(panel, SWT.RADIO);
		fromFileBtn.setText("Platform specification from file");

		builtinBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getTableViewer().getControl().setEnabled(true);
				builtinSelected = true;
			}
		});

		fromFileBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getTableViewer().getControl().setEnabled(false);
				builtinSelected = false;
			}
		});

		return panel;
	}

	/**
	 * @return True if built-in platform specification is selected, false otherwise.
	 */
	public boolean isBuiltinSelected() {
		return builtinSelected;
	}

	/**
	 * @return True if platform specification from file is selected, false otherwise.
	 */
	public boolean isFromFileSelected() {
		return !builtinSelected;
	}

	/**
	 * Sets the title area text
	 * @param title
	 */
	public void setTitleAreaText(String title) {
		dlgPart.setTitleAreaText(title);
	}

	/**
	 * Sets the title area message
	 * @param message
	 */
	public void setTitleAreaMessage(String message) {
		dlgPart.setTitleAreaMessage(message);
	}

	/**
	 * @return the title area text
	 */
	public String getTitleAreaText() {
		return dlgPart.getTitleAreaText();
	}

	/**
	 * @return the title area message
	 */
	public String getTitleAreaMessage() {
		return dlgPart.getTitleAreaMessage();
	}

}
