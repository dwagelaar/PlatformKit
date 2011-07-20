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
package be.ac.vub.platformkit.java.jobs;

import java.io.IOException;
import java.util.Collections;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.m2m.atl.emftvm.EmftvmFactory;
import org.eclipse.m2m.atl.emftvm.Model;

import be.ac.vub.jar2uml.JarToUML;
import be.ac.vub.platformkit.java.PlatformkitJavaResources;

/**
 * Operation for creating compatibility reports from Jar files
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class JarFileCompatJob extends CompatJob {

	/**
	 * Creates a new {@link JarFileCompatJob}.
	 */
	public JarFileCompatJob() {
		super();
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.java.popup.actions.CompatAction#runAction(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void runAction(IProgressMonitor monitor) throws Exception {
        JarFileCompatJobRunner runner = new JarFileCompatJobRunner();
		runActionWithRunner(monitor, runner, STEPS+3);
	}

	public class JarFileCompatJobRunner extends CompatJobRunner {

		/* (non-Javadoc)
		 * @see be.ac.vub.platformkit.java.popup.actions.CompatAction.CompatActionRunner#loadDepsModel(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public void loadDepsModel(IProgressMonitor monitor)
				throws CoreException, IOException {
			//
			// Steps 1+2+3
			//
			subTask(monitor, PlatformkitJavaResources.getString("creatingDepsModel")); //$NON-NLS-1$
			JarToUML jarToUML = new JarToUML();
			jarToUML.setIncludeFeatures(true);
			jarToUML.setIncludeInstructionReferences(true);
			jarToUML.setFilter(null);
			jarToUML.setDependenciesOnly(true);
			final IFile jarFile = (IFile) getInput();
	        final IFile file = jarFile.getWorkspace().getRoot().getFile(
	        		jarFile.getFullPath().removeFileExtension().addFileExtension("deps.uml")); //$NON-NLS-1$
	        Assert.isNotNull(file);
			setFile(file);
	        final IPath path = file.getFullPath();
	        jarToUML.addJar(new JarFile(jarFile.getLocation().toFile()));
			jarToUML.setOutputFile(path.toString());
			jarToUML.setOutputModelName(path.removeFileExtension().lastSegment());
			jarToUML.setMonitor(new SubProgressMonitor(monitor, 2));
			jarToUML.run();
			if (jarToUML.isRunComplete()) {
				jarToUML.getModel().eResource().save(Collections.EMPTY_MAP);
			}
			worked(monitor, PlatformkitJavaResources.getString("createdDepsModel")); //$NON-NLS-1$
			//
			// Step 4
			//
			subTask(monitor, PlatformkitJavaResources.getString("loadingDepsModel")); //$NON-NLS-1$
			final Model deps = EmftvmFactory.eINSTANCE.createModel();
			deps.setResource(jarToUML.getModel().eResource());
			setDeps(deps);
			setCrPath(file.getProjectRelativePath()
					.removeFileExtension()
					.removeFileExtension()
					.addFileExtension("cr.uml")); //$NON-NLS-1$
			worked(monitor, PlatformkitJavaResources.getString("loadedDepsModel")); //$NON-NLS-1$
		}

	}

}
