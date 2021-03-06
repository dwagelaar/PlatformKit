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
package be.ac.vub.platformkit.java.popup.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.progress.IProgressConstants;

import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;
import be.ac.vub.platformkit.java.PlatformkitJavaResources;
import be.ac.vub.platformkit.java.actions.ShowCompatResultAction;
import be.ac.vub.platformkit.java.jobs.CompatJob;
import be.ac.vub.platformkit.java.ui.util.PlatformAPIDialogRunnable;
import be.ac.vub.platformkit.presentation.popup.action.SelectionAction;

/**
 * Context menu action for creating compatibility reports for UML dependency models 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class CompatAction extends SelectionAction {

	/**
	 * Adds action to the list of actions to run after job is done.
	 * @param job
	 * @param action
	 */
	public static void addDoneAction(final Job job, final IAction action) {
		job.setProperty(IProgressConstants.ACTION_PROPERTY, action);
		job.addJobChangeListener(new JobChangeAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.core.runtime.jobs.JobChangeAdapter#done(org.eclipse.core.runtime.jobs.IJobChangeEvent)
			 */
			@Override
			public void done(IJobChangeEvent event) {
				action.run();
			}
		});
	}

	/**
	 * @return A new {@link CompatJob}.
	 */
	protected CompatJob createCompatJob() {
		return new CompatJob();
	}

	/**
	 * @return the workspace resource to lock during the operation, or <code>null</code> if no workspace locking is required
	 */
	protected IResource getLockingResource() {
		final Object res = ((IStructuredSelection) selection).getFirstElement();
		if (res instanceof IFile) {
			return ((IFile) res).getParent();
		}
		return null;
	}

	/**
	 * @param job
	 * @return A new {@link ShowCompatResultAction}.
	 */
	protected ShowCompatResultAction createShowResultAction(CompatJob job) {
		return new ShowCompatResultAction(
				job, 
				PlatformkitJavaResources.getString("resultText")); //$NON-NLS-1$
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		final CompatJob job = createCompatJob();
		final IAction showResultAction = createShowResultAction(job);
		addDoneAction(job, showResultAction);
		// Select platform API dialog
		final PlatformAPIDialogRunnable paDlg = new PlatformAPIDialogRunnable();
		paDlg.setTitle(PlatformkitJavaResources.getString("CompatAction.dlgTitle")); //$NON-NLS-1$
		paDlg.setMessage(PlatformkitJavaResources.getString("CompatAction.dlgMessage")); //$NON-NLS-1$
		paDlg.setInstruction(PlatformkitJavaResources.getString("CompatAction.dlgInstruction")); //$NON-NLS-1$
		PlatformkitJavaPlugin.getPlugin().getWorkbench().getDisplay().syncExec(paDlg);
		final URI[] emf_uris = paDlg.getResult();
		if (emf_uris == null || emf_uris.length == 0) {
			return; //cancel
		}
		// run operation
		job.setCreateOntology(paDlg.isCreateOntology());
		job.setInput(((IStructuredSelection) selection).getFirstElement());
		job.setEmfUris(emf_uris);
		job.setEmfUriLabels(paDlg.getLabels());
		job.setUser(true);
		job.schedule();
	}

}