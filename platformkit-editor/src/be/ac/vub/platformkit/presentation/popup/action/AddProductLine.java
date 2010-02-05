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
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.jobs.AddProductLineJob;

/**
 * Adds the metaclass constraints of a product line, described in Ecore,
 * to the PlatformKit constraint space model.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class AddProductLine extends AddConstraintSets {

	/**
	 * Creates a new {@link AddProductLine}.
	 */
	public AddProductLine() {
		super(PlatformkitEditorPlugin.getPlugin().getString("AddProductLine.name")); //$NON-NLS-1$
		job = new AddProductLineJob();
		setFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IResource) {
					IResource resource = (IResource) element;
					if (resource.getType() == IResource.FILE) {
						return resource.getFileExtension().toLowerCase().equals("ecore"); //$NON-NLS-1$
					}
					return true;
				}
				return false;
			}
		});
	}

}
