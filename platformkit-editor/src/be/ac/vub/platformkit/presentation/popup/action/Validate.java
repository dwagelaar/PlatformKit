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
package be.ac.vub.platformkit.presentation.popup.action;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.jobs.ValidateJob;
import be.ac.vub.platformkit.ui.util.MessageDialogRunnable;
import be.ac.vub.platformkit.ui.util.PlatformSpecDialogRunnable;

/**
 * Validate the options in a PlatformKit constraint space model
 * against platform instances.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class Validate extends ViewerFilterAction {

	protected ValidateJob job;

	/**
	 * Creates a new {@link Validate}.
	 */
	public Validate() {
		super();
		setFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IResource) {
					IResource resource = (IResource) element;
					if (resource.getType() == IResource.FILE) {
						return resource.getFileExtension().toLowerCase().equals("owl"); //$NON-NLS-1$
					}
					return true;
				}
				return false;
			}
		});
		job = new ValidateJob();
		job.addJobChangeListener(new JobChangeAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.core.runtime.jobs.JobChangeAdapter#done(org.eclipse.core.runtime.jobs.IJobChangeEvent)
			 */
			@Override
			public void done(IJobChangeEvent event) {
				showReport(job);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		PlatformSpecDialogRunnable dlg = new PlatformSpecDialogRunnable();
		if (getFilter() != null) {
			dlg.setFilter(getFilter());
		}
		PlatformkitEditorPlugin.getPlugin().getWorkbench().getDisplay().syncExec(dlg);
		if (dlg.getReturnCode() != Window.OK) {
			return;
		}
		// run operation
		job.setPlatformInstanceSources(dlg.getSelection());
		job.setSpace((ConstraintSpace) ((IStructuredSelection) selection).getFirstElement());
		job.setUser(true);
	    final URI spaceUri = job.getSpace().eResource().getURI();
	    if (spaceUri.isPlatformResource()) {
	    	final IResource spaceRes = ResourcesPlugin.getWorkspace().getRoot().findMember(spaceUri.toPlatformString(true));
	    	assert spaceRes != null;
		    job.setRule(spaceRes.getParent()); //lock containing folder
	    }
		// lock editor
		IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) 
		part.getSite().getAdapter(IWorkbenchSiteProgressService.class);
		siteService.schedule(job);
	}

	/**
	 * Displays a message dialog with the job report.
	 * @param job
	 */
	private void showReport(ValidateJob job) {
		final String report = job.getReport();
		if (report != null) {
			MessageDialogRunnable dlg = new MessageDialogRunnable(
					PlatformkitEditorPlugin.getPlugin().getString("Validate.reportDlgTitle"), 
					report); //$NON-NLS-1$
			PlatformkitEditorPlugin.getPlugin().getWorkbench().getDisplay().syncExec(dlg);
		}
	}

}
