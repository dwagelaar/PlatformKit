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
package be.ac.vub.platformkit.java.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;

import be.ac.vub.platformkit.java.PlatformkitJavaResources;
import be.ac.vub.platformkit.java.jobs.CompatJob;
import be.ac.vub.platformkit.java.ui.util.OpenFileInEditorRunnable;
import be.ac.vub.platformkit.ui.util.MessageDialogRunnable;

/**
 * Shows the result of a {@link CompatJob}.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class ShowCompatResultAction extends Action {

	private CompatJob job;

	/**
	 * Creates a new {@link ShowCompatResultAction}.
	 * @param job
	 */
	public ShowCompatResultAction(CompatJob job) {
		super();
		setJob(job);
	}

	/**
	 * Creates a new {@link ShowCompatResultAction}.
	 * @param job
	 * @param text
	 */
	public ShowCompatResultAction(CompatJob job, String text) {
		super(text);
		setJob(job);
	}

	/**
	 * Creates a new {@link ShowCompatResultAction}.
	 * @param job
	 * @param text
	 * @param image
	 */
	public ShowCompatResultAction(CompatJob job, String text, ImageDescriptor image) {
		super(text, image);
		setJob(job);
	}

	/**
	 * Creates a new {@link ShowCompatResultAction}.
	 * @param job
	 * @param text
	 * @param style
	 */
	public ShowCompatResultAction(CompatJob job, String text, int style) {
		super(text, style);
		setJob(job);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		CompatJob job = getJob();
		IStatus result = job.getResult();
		if (result == null || result.getSeverity() != IStatus.OK) {
			return;
		}
		// show result
		IFile outputFile = job.getOutputFile();
		boolean compatible = outputFile == null;
		int mode;
		String summary;
		if (compatible) {
			summary = String.format(
					PlatformkitJavaResources.getString("ShowCompatResultAction.isCompatibleWith"), 
					job.getInputName(), 
					job.getApiList()); //$NON-NLS-1$
			mode = MessageDialogRunnable.MODE_INFORMATION;
		} else {
			summary = String.format(
					PlatformkitJavaResources.getString("ShowCompatResultAction.isNotCompatibleWith"), 
					job.getInputName(), 
					job.getApiList(), 
					outputFile.getFullPath()); //$NON-NLS-1$
			mode = MessageDialogRunnable.MODE_ERROR;
		}
		String title = String.format(
				PlatformkitJavaResources.getString("ShowCompatResultAction.dlgTitle"), 
				job.getApiList());
		MessageDialogRunnable dlg = new MessageDialogRunnable(title, summary);
		dlg.setMode(mode);
		PlatformUI.getWorkbench().getDisplay().syncExec(dlg);
		// open report in editor
		if (!compatible) {
			PlatformUI.getWorkbench().getDisplay().syncExec(new OpenFileInEditorRunnable(outputFile));
		}
	}

	/**
	 * @return the job
	 */
	public CompatJob getJob() {
		return job;
	}

	/**
	 * @param job the job to set
	 */
	public void setJob(CompatJob job) {
		this.job = job;
	}
}
