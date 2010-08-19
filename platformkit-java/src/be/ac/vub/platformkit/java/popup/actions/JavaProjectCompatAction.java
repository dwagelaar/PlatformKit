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

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.IStructuredSelection;

import be.ac.vub.platformkit.java.jobs.CompatJob;
import be.ac.vub.platformkit.java.jobs.JavaProjectCompatJob;

/**
 * Context menu action for creating compatibility reports from Java projects
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class JavaProjectCompatAction extends CompatAction {

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.java.popup.actions.CompatAction#createCompatJob()
	 */
	@Override
	protected CompatJob createCompatJob() {
		return new JavaProjectCompatJob();
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.java.popup.actions.CompatAction#getLockingResource()
	 */
	@Override
	protected IResource getLockingResource() {
		final Object res = ((IStructuredSelection) selection).getFirstElement();
		if (res instanceof IJavaProject) {
			return ((IJavaProject) res).getProject();
		}
		return super.getLockingResource();
	}

}
