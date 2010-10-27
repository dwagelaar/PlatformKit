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
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.jobs.ProfileJob;
import be.ac.vub.platformkit.ui.util.PlatformSpecDialogRunnable;

/**
 * Profiles the options in an EMF editor
 * against a platform instance specification.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class Profile extends ViewerFilterAction {

	protected ProfileJob job;

	/**
	 * Creates a new {@link Profile}.
	 */
	public Profile() {
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
		job = new ProfileJob();
		job.addJobChangeListener(new JobChangeAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.core.runtime.jobs.JobChangeAdapter#done(org.eclipse.core.runtime.jobs.IJobChangeEvent)
			 */
			@Override
			public void done(IJobChangeEvent event) {
				// force editor to refresh new child/sibling menu
				if (part instanceof ISelectionProvider) {
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
						public void run() {
							final ISelectionProvider sp = (ISelectionProvider) part;
							sp.setSelection(sp.getSelection());
						}
					});
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		PlatformSpecDialogRunnable dlg = new PlatformSpecDialogRunnable();
		dlg.setInstruction(PlatformkitEditorPlugin.getPlugin().getString("Profile.dlgInstruction")); //$NON-NLS-1$
		if (getFilter() != null) {
			dlg.setFilter(getFilter());
		}
		PlatformkitEditorPlugin.getPlugin().getWorkbench().getDisplay().syncExec(dlg);
		// run operation
		job.setPlatformInstanceSources(dlg.getSelection());
		job.setSelectedObject((EObject) ((IStructuredSelection) selection).getFirstElement());
		job.setEditingDomain(editingDomain);
		job.setUser(true);
	    final URI modelUri = job.getSelectedObject().eResource().getURI();
	    if (modelUri.isPlatformResource()) {
	    	final IResource modelRes = ResourcesPlugin.getWorkspace().getRoot().findMember(modelUri.toPlatformString(true));
	    	assert modelRes != null;
		    job.setRule(modelRes.getParent()); //lock containing folder
	    }
		// lock editor
		IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) 
		part.getSite().getAdapter(IWorkbenchSiteProgressService.class);
		siteService.schedule(job);
	}

}
