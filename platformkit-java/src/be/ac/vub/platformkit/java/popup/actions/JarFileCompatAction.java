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

import be.ac.vub.platformkit.java.PlatformkitJavaResources;
import be.ac.vub.platformkit.java.actions.ShowCompatResultAction;
import be.ac.vub.platformkit.java.jobs.JarFileCompatJob;

/**
 * Context menu action for creating compatibility reports from Jar files
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class JarFileCompatAction extends CompatAction {

	/**
	 * Creates a new {@link JarFileCompatAction}.
	 */
	public JarFileCompatAction() {
		super();
		job = new JarFileCompatJob();
		showResultAction = new ShowCompatResultAction(
				job, 
				PlatformkitJavaResources.getString("resultText")); //$NON-NLS-1$
		initJob();
	}

}
