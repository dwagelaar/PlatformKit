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
package be.ac.vub.platformkit.ui.util;

import java.io.IOException;

import org.eclipse.jface.window.Window;

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.ui.dialogs.PlatformSpecDialog;

/**
 * Wraps a {@link PlatformSpecDialog} in a {@link Runnable}.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformSpecDialogRunnable extends FileDialogRunnable {

	/**
	 * Creates a new {@link PlatformSpecDialogRunnable}.
	 */
	public PlatformSpecDialogRunnable() {
		super();
		setTitle(PlatformkitEditorPlugin.getPlugin().getString("PlatformSpecDialogRunnable.dlgAreaTitle")); //$NON-NLS-1$
		setMessage(PlatformkitEditorPlugin.getPlugin().getString("PlatformSpecDialogRunnable.dlgMessage")); //$NON-NLS-1$
		setInstruction(PlatformkitEditorPlugin.getPlugin().getString("PlatformSpecDialogRunnable.dlgInstruction")); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.ui.util.FileDialogRunnable#run()
	 */
	@Override
	public void run() {
		try {
			PlatformSpecDialog dlg = new PlatformSpecDialog(
					PlatformkitEditorPlugin.INSTANCE.getShell());
			dlg.setTitle(PlatformkitEditorPlugin.getPlugin().getString("PlatformSpecDialogRunnable.dlgTitle")); //$NON-NLS-1$
			dlg.setTitleAreaText(getTitle());
			dlg.setTitleAreaMessage(getMessage());
			dlg.setMessage(getInstruction());
			dlg.open();
			setReturnCode(dlg.getReturnCode());
			if (dlg.getReturnCode() == Window.OK) {
				if (dlg.isFromFileSelected()) {
					super.run();
				} else {
					setSelection(dlg.getResult());
				}
			}
		} catch (IOException e) {
			PlatformkitEditorPlugin.report(e);
		}
	}

}
