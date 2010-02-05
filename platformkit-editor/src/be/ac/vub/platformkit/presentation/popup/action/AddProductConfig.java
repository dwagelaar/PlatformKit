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

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.jobs.AddProductConfigJob;

/**
 * Adds a product configuration, described in an annotated DSL,
 * to the PlatformKit constraint space model.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class AddProductConfig extends AddConstraintSets {

	/**
	 * Creates a new {@link AddProductConfig}.
	 */
	public AddProductConfig() {
		super(PlatformkitEditorPlugin.getPlugin().getString("AddProductConfig.name")); //$NON-NLS-1$
		job = new AddProductConfigJob();
		//TODO filter doesn't work, since normal EMF plugins aren't here
		//        setFilter(new ViewerFilter() {
		//            private Map extensions = Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap();
		//            public boolean select(Viewer viewer, Object parentElement, Object element) {
		//                if (element instanceof IResource) {
		//                    IResource resource = (IResource) element;
		//                    if (resource.getType() == IResource.FILE) {
		//                    	logger.info("filter using map " + extensions);
		//                    	logger.info("filter found " + resource);
		//                        return extensions.get(resource.getFileExtension()) != null;
		//                    }
		//                    return true;
		//                }
		//                return false;
		//            }
		//        });
	}

}
