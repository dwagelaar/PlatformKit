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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.presentation.jobs.ConstraintSpaceJob;
import be.ac.vub.platformkit.presentation.jobs.SortPlatformkitModelJob;

/**
 * Abstract action for sorting the constraint sets in a PlatformKit model.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public abstract class SortPlatformkitModel extends ObjectSelectionAction {

	protected ConstraintSpaceJob job;

	/**
	 * Creates a new {@link SortPlatformkitModel}
	 * @param mode {@link SortPlatformkitModelJob#MOST_SPECIFIC} or {@link SortPlatformkitModelJob#LEAST_SPECIFIC}.
	 * @throws IllegalArgumentException
	 */
	public SortPlatformkitModel(int mode) throws IllegalArgumentException {
		super();
		job = new SortPlatformkitModelJob(mode);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		// run operation
		job.setEditingDomain(editingDomain);
		job.setSpace((ConstraintSpace) ((IStructuredSelection) selection).getFirstElement());
		job.setUser(true);
		// lock editor
		IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) 
		part.getSite().getAdapter(IWorkbenchSiteProgressService.class);
		siteService.schedule(job);
	}
}
