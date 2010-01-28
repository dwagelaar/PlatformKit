package be.ac.vub.platformkit.java.jobs;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.m2m.atl.core.ATLCoreException;

import be.ac.vub.jar2uml.JarToUML;

/**
 * Operation for creating compatibility reports from Java projects
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class JavaProjectCompatJob extends CompatJob {

	/**
	 * Creates a new {@link JavaProjectCompatJob}.
	 */
	public JavaProjectCompatJob() {
		super();
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.java.popup.actions.CompatAction#runAction(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void runAction(IProgressMonitor monitor) throws Exception {
        checkAndSwitchStrategy();
        JavaProjectCompatJobRunner runner = new JavaProjectCompatJobRunner();
		runActionWithRunner(monitor, runner, 7);
	}

	public class JavaProjectCompatJobRunner extends CompatJobRunner {

		/* (non-Javadoc)
		 * @see be.ac.vub.platformkit.java.popup.actions.CompatAction.CompatActionRunner#loadDepsModel(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public void loadDepsModel(IProgressMonitor monitor)
				throws ATLCoreException, CoreException, IOException {
			subTask(monitor, "Creating dependency model...");
			JarToUML jarToUML = new JarToUML();
			jarToUML.setIncludeFeatures(true);
			jarToUML.setIncludeInstructionReferences(true);
			jarToUML.setFilter(null);
			jarToUML.setDependenciesOnly(true);
			final IJavaProject jproject = (IJavaProject) getInput();
			final IProject project = jproject.getProject();
	        if (!project.isAccessible()) {
	        	throw new IllegalArgumentException("Project " + project.getName() + " not accessible");
	        }
	        final IFile file = project.getFile(project.getName() + ".deps.uml");
	        Assert.isNotNull(file);
			setFile(file);
	        final IPath path = file.getFullPath();
	        jarToUML.addPaths(jproject, true);
			jarToUML.setOutputFile(path.toString());
			jarToUML.setOutputModelName(path.removeFileExtension().lastSegment());
			jarToUML.setMonitor(monitor);
			jarToUML.run();
			if (jarToUML.isRunComplete()) {
				jarToUML.getModel().eResource().save(Collections.EMPTY_MAP);
			}
			worked(monitor, "Created dependency model");
			subTask(monitor, "Loading dependency model...");
			setDeps(modelLoader.loadDEPSModel(getUml2(), jarToUML.getModel().eResource()));
			setCrPath(file.getProjectRelativePath()
					.removeFileExtension()
					.removeFileExtension()
					.addFileExtension("cr.uml"));
			worked(monitor, "Loaded dependency model");
		}

	}

}
