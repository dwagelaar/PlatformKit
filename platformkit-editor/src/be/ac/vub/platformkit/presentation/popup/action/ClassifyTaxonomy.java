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
import be.ac.vub.platformkit.presentation.jobs.ClassifyTaxonomyJob;

/**
 * Pre-classifies the taxonomy of ontology classes for a
 * given Platformkit {@link ConstraintSpace} model.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class ClassifyTaxonomy extends ObjectSelectionAction {

	protected ClassifyTaxonomyJob job;
	
    /**
	 * Creates a new {@link ClassifyTaxonomy}.
	 */
	public ClassifyTaxonomy() {
		super();
		job = new ClassifyTaxonomyJob();
	}
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
	public void run(IAction action) {
	    // run operation
    	job.setSpace((ConstraintSpace) ((IStructuredSelection) selection).getFirstElement());
	    job.setUser(true);
	    // lock editor
	    IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) 
	    	part.getSite().getAdapter(IWorkbenchSiteProgressService.class);
	    siteService.schedule(job);
    }
    
}
