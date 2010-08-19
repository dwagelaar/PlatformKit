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
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.m2m.atl.common.ATLLogger;
import org.eclipse.m2m.atl.core.ATLCoreException;
import org.eclipse.m2m.atl.core.IExtractor;
import org.eclipse.m2m.atl.core.IModel;
import org.eclipse.m2m.atl.core.IReferenceModel;
import org.eclipse.m2m.atl.core.launch.ILauncher;

import be.ac.vub.platformkit.editor.preferences.PreferenceConstants;
import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;
import be.ac.vub.platformkit.java.PlatformkitJavaResources;
import be.ac.vub.platformkit.java.popup.util.ATLUtil;
import be.ac.vub.platformkit.jobs.ProgressMonitorJob;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * Operation for creating compatibility reports for UML dependency models 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class CompatJob extends ProgressMonitorJob {

	/**
	 * Adds PlatformKit log handler to ATL logger.
	 */
	public static void addATLLogHandler() {
		final Handler pkHandler = PlatformkitEditorPlugin.getHandler();
		for (Handler handler : ATLLogger.getLogger().getHandlers()) {
			if (pkHandler == handler) {
				return; // handler already added
			}
		}
		ATLLogger.getLogger().addHandler(pkHandler);
	}

	/**
	 * Removes PlatformKit log handler from ATL logger.
	 */
	public static void removeATLLogHandler() {
		ATLLogger.getLogger().removeHandler(PlatformkitEditorPlugin.getHandler());
	}

	/**
	 * Creates a new {@link CompatJob}.
	 */
	public CompatJob() {
		super(PlatformkitJavaResources.getString("CompatJob.name")); //$NON-NLS-1$
		vmoptions.put("printExecutionTime", "true"); //$NON-NLS-1$
		vmoptions.put("allowInterModelReferences", "true"); //$NON-NLS-1$
		vmoptions.put("supportUML2Stereotypes", "true"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.java.jobs.ProgressMonitorJob#runAction(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void runAction(IProgressMonitor monitor) throws Exception {
		checkAndSwitchStrategy();
		CompatJobRunner runner = new CompatJobRunner();
		runActionWithRunner(monitor, runner, 5);
	}

	/**
	 * Defines model loading strategy interface and behaviour
	 */
	public abstract class ModelLoadingStrategy {

		private String atlVMName;
		protected ATLUtil atlUtil;

		/**
		 * Creates a new ModelLoadingStrategy
		 * @param atlVMName
		 * @throws ATLCoreException
		 */
		public ModelLoadingStrategy(String atlVMName) throws ATLCoreException {
			Assert.isNotNull(atlVMName);
			this.atlVMName = atlVMName;
			atlUtil = new ATLUtil(atlVMName);
		}

		/**
		 * @return The UML2 metamodel
		 * @throws IOException 
		 * @throws ATLCoreException 
		 */
		public IReferenceModel getUML2() throws ATLCoreException, IOException {
			return atlUtil.loadRefModel(UML_MM.openStream(), "UML2", UML_MM.toString(), MODEL_HANDLER); //$NON-NLS-1$
		}

		/**
		 * @param uml2 The UML2 metamodel
		 * @return The CompatibilityReport profile model
		 * @throws IOException 
		 * @throws ATLCoreException 
		 */
		public IModel getCRProfile(IReferenceModel uml2) throws ATLCoreException, IOException {
			return atlUtil.loadModel(uml2, CR_PROF, "CR", CR_PROF); //$NON-NLS-1$
		}

		/**
		 * @param uml2 The UML2 metamodel
		 * @param emf_uri The EMF URI to load from
		 * @return the loaded 'IN' model
		 * @throws ATLCoreException 
		 */
		public abstract IModel loadINModel(IReferenceModel uml2, URI emf_uri) throws ATLCoreException;

		/**
		 * @param uml2 The UML2 metamodel
		 * @param path The path to the source model
		 * @return the loaded 'DEPS' model
		 * @throws ATLCoreException 
		 */
		public IModel loadDEPSModel(IReferenceModel uml2,
				String path) throws ATLCoreException {
			return atlUtil.loadModel(uml2, path, "DEPS", path); //$NON-NLS-1$
		}

		/**
		 * @param uml2 The UML2 metamodel
		 * @param resource The resource containing the source model
		 * @return the loaded 'DEPS' model
		 * @throws ATLCoreException 
		 */
		public IModel loadDEPSModel(IReferenceModel uml2,
				Resource resource) throws ATLCoreException {
			return atlUtil.loadModel(uml2, resource, "DEPS"); //$NON-NLS-1$
		}

		/**
		 * @param uml2 The UML2 metamodel
		 * @param path The future path of the model
		 * @return the new 'REPORT' model
		 * @throws ATLCoreException 
		 */
		public IModel createOUTModel(IReferenceModel uml2, String path) throws ATLCoreException {
			return atlUtil.newModel(uml2, "REPORT", path); //$NON-NLS-1$
		}

		/**
		 * @param atlVMName
		 * @return True if this ModelLoadingStrategy is valid for the given ATL VM name.
		 */
		public boolean isValidFor(String atlVMName, boolean useCache) {
			return this.atlVMName.equals(atlVMName);
		}

		/**
		 * @return an ATL launcher
		 * @throws ATLCoreException 
		 */
		public ILauncher getAtlLauncher() throws ATLCoreException {
			return atlUtil.getLauncher();
		}

		/**
		 * @return an ATL extractor
		 */
		public IExtractor getAtlExtractor() {
			return atlUtil.getExtractor();
		}

		/**
		 * Flushes internal ATL objects and models
		 * @throws ATLCoreException
		 */
		public void flush() throws ATLCoreException {
			atlUtil = new ATLUtil(atlVMName);
		}
	}

	private class SimpleModelLoadingStrategy extends ModelLoadingStrategy {

		public SimpleModelLoadingStrategy(String atlVMName) throws ATLCoreException {
			super(atlVMName);
		}

		@Override
		public IModel loadINModel(IReferenceModel uml2, URI emfUri) throws ATLCoreException {
			return atlUtil.loadModel(uml2, emfUri.toString(), "IN", emfUri.toString()); //$NON-NLS-1$
		}

		/* (non-Javadoc)
		 * @see be.ac.vub.platformkit.java.popup.actions.CompatAction.ModelLoadingStrategy#isValidFor(java.lang.String, boolean)
		 */
		@Override
		public boolean isValidFor(String atlVMName, boolean useCache) {
			return super.isValidFor(atlVMName, useCache) && !useCache;
		}
	}

	private class CachingModelLoadingStrategy extends ModelLoadingStrategy {

		protected Map<URI,SoftReference<IModel>> inModelCache = new HashMap<URI,SoftReference<IModel>>();

		public CachingModelLoadingStrategy(String atlVMName) throws ATLCoreException {
			super(atlVMName);
		}

		@Override
		public IModel loadINModel(IReferenceModel uml2, URI emfUri) throws ATLCoreException {
			IModel in = null;
			if (inModelCache.containsKey(emfUri)) {
				in = inModelCache.get(emfUri).get();
			}
			if (in == null) {
				in = atlUtil.loadModel(uml2, emfUri.toString(), "IN", emfUri.toString()); //$NON-NLS-1$
				inModelCache.put(emfUri, new SoftReference<IModel>(in));
			}
			return in;
		}

		/* (non-Javadoc)
		 * @see be.ac.vub.platformkit.java.popup.actions.CompatAction.ModelLoadingStrategy#isValidFor(java.lang.String, boolean)
		 */
		@Override
		public boolean isValidFor(String atlVMName, boolean useCache) {
			return super.isValidFor(atlVMName, useCache) && useCache;
		}
	}

	/**
	 * Worker class for {@link CompatJob}.
	 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
	 */
	public class CompatJobRunner {

		private IReferenceModel uml2;
		private IModel crProfile;
		private IModel deps;
		private IModel in;
		private IModel report;
		private IFile file;
		private String crLocation;
		private IPath crPath;

		/**
		 * @return the uml2
		 */
		public IReferenceModel getUml2() {
			return uml2;
		}
		/**
		 * @param uml2 the uml2 to set
		 */
		protected void setUml2(IReferenceModel uml2) {
			this.uml2 = uml2;
		}
		/**
		 * @return the crProfile
		 */
		public IModel getCrProfile() {
			return crProfile;
		}
		/**
		 * @param crProfile the crProfile to set
		 */
		protected void setCrProfile(IModel crProfile) {
			this.crProfile = crProfile;
		}
		/**
		 * @return the deps
		 */
		public IModel getDeps() {
			return deps;
		}
		/**
		 * @param deps the deps to set
		 */
		protected void setDeps(IModel deps) {
			this.deps = deps;
		}
		/**
		 * @return the in
		 */
		public IModel getIn() {
			return in;
		}
		/**
		 * @param in the in to set
		 */
		protected void setIn(IModel in) {
			this.in = in;
		}
		/**
		 * @return the report
		 */
		public IModel getReport() {
			return report;
		}
		/**
		 * @param report the report to set
		 */
		protected void setReport(IModel report) {
			this.report = report;
		}

		/**
		 * @return the file
		 */
		public IFile getFile() {
			return file;
		}
		/**
		 * @param file the file to set
		 */
		protected void setFile(IFile file) {
			this.file = file;
			setInputName(file.getName());
		}
		/**
		 * @return the crPath
		 */
		protected IPath getCrPath() {
			return crPath;
		}
		/**
		 * @param crPath the crPath to set
		 */
		protected void setCrPath(IPath crPath) {
			this.crPath = crPath;
			setCrLocation("platform:/resource/" + getFile().getProject().getName() + "/" + crPath.toString()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		/**
		 * @return the crLocation
		 */
		public String getCrLocation() {
			return crLocation;
		}
		/**
		 * @param crLocation the crLocation to set
		 */
		protected void setCrLocation(String crLocation) {
			this.crLocation = crLocation;
		}
		/**
		 * Loads UML2 metamodel
		 * @param monitor
		 * @throws IOException 
		 * @throws ATLCoreException 
		 */

		public void loadUml2(IProgressMonitor monitor) throws ATLCoreException, IOException {
			subTask(monitor, PlatformkitJavaResources.getString("CompatJob.loadingUml2")); //$NON-NLS-1$
			setUml2(modelLoader.getUML2());
			worked(monitor, PlatformkitJavaResources.getString("CompatJob.loadedUml2")); //$NON-NLS-1$
		}

		/**
		 * Loads CompatiblityReport profile
		 * @param monitor
		 * @throws IOException 
		 * @throws ATLCoreException 
		 */
		public void loadCRProfile(IProgressMonitor monitor) throws ATLCoreException, IOException {
			subTask(monitor, PlatformkitJavaResources.getString("CompatJob.loadingCRProfile")); //$NON-NLS-1$
			setCrProfile(modelLoader.getCRProfile(getUml2()));
			worked(monitor, PlatformkitJavaResources.getString("CompatJob.loadedCRProfile")); //$NON-NLS-1$
		}

		/**
		 * Loads dependency model
		 * @param monitor
		 * @throws IOException 
		 * @throws ATLCoreException 
		 */
		public void loadDepsModel(IProgressMonitor monitor) 
		throws ATLCoreException, CoreException, IOException {
			subTask(monitor, PlatformkitJavaResources.getString("loadingDepsModel")); //$NON-NLS-1$
			setFile((IFile) getInput());
			final IFile file = getFile();
			Assert.isNotNull(file);
			final String fileLocation = "platform:/resource/" + file.getProject().getName() + "/" + file.getProjectRelativePath().toString(); //$NON-NLS-1$ //$NON-NLS-2$
			setDeps(modelLoader.loadDEPSModel(getUml2(), fileLocation));
			setCrPath(file.getProjectRelativePath()
					.removeFileExtension()
					.removeFileExtension()
					.addFileExtension("cr.uml")); //$NON-NLS-1$
			worked(monitor, PlatformkitJavaResources.getString("loadedDepsModel")); //$NON-NLS-1$
		}

		/**
		 * Loads API model
		 * @param monitor
		 * @param apiName The API name to display
		 * @param emf_uri The API model URI to load from
		 * @throws IOException 
		 * @throws ATLCoreException 
		 */
		public void loadAPIModel(IProgressMonitor monitor, String apiName, URI emf_uri) throws ATLCoreException, CoreException {
			subTask(monitor, String.format(
					PlatformkitJavaResources.getString("CompatJob.loadingApiModel"), 
					apiName)); //$NON-NLS-1$
			setIn(modelLoader.loadINModel(getUml2(), emf_uri));
			worked(monitor, PlatformkitJavaResources.getString("CompatJob.loadedApiModel")); //$NON-NLS-1$
		}

		/**
		 * Performs a single run of the UML2CompatbilityReport transformation
		 * @param monitor
		 * @return The transformation result
		 * @throws ATLCoreException 
		 * @throws CoreException 
		 * @throws IOException 
		 * @throws InvocationTargetException 
		 * @throws IllegalAccessException 
		 * @throws NoSuchMethodException 
		 * @throws IllegalArgumentException 
		 * @throws SecurityException 
		 * @throws ClassCastException 
		 */
		public boolean run(IProgressMonitor monitor) throws ATLCoreException, CoreException, IOException, ClassCastException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
			subTask(monitor, PlatformkitJavaResources.getString("CompatJob.creatingCR")); //$NON-NLS-1$
			modelLoader.flush(); // report must be in new resource set
			setReport(modelLoader.createOUTModel(getUml2(), getCrLocation()));
			final ILauncher launcher = modelLoader.getAtlLauncher();
			launcher.addInModel(getCrProfile(), "CR", "UML2"); //$NON-NLS-1$ //$NON-NLS-2$
			launcher.addInModel(getDeps(), "DEPS", "UML2"); //$NON-NLS-1$ //$NON-NLS-2$
			launcher.addInModel(getIn(), "IN", "UML2"); //$NON-NLS-1$ //$NON-NLS-2$
			launcher.addOutModel(getReport(), "REPORT", "UML2"); //$NON-NLS-1$ //$NON-NLS-2$
			launcher.addLibrary("UML2", uml2Lib.openStream()); //$NON-NLS-1$
			launcher.addLibrary("UML2Comparison", uml2Comparison.openStream()); //$NON-NLS-1$
			Object result = launcher.launch(ILauncher.RUN_MODE, monitor, vmoptions, uml2CompatibilityReport.openStream());
			worked(monitor, PlatformkitJavaResources.getString("CompatJob.createdCR")); //$NON-NLS-1$
			return ATLUtil.getBooleanValue(result);
		}

		/**
		 * Merges current compatibility report with otherReport by intersection
		 * @param monitor
		 * @param otherReport
		 * @throws ATLCoreException
		 * @throws CoreException
		 * @throws IOException
		 * @throws ClassCastException
		 * @throws SecurityException
		 * @throws IllegalArgumentException
		 * @throws NoSuchMethodException
		 * @throws IllegalAccessException
		 * @throws InvocationTargetException
		 */
		public void mergeReport(IProgressMonitor monitor, IModel otherReport) throws ATLCoreException, CoreException, IOException, ClassCastException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
			subTask(monitor, PlatformkitJavaResources.getString("CompatJob.mergingCRs")); //$NON-NLS-1$
			IModel report = getReport();
			if ((otherReport == null) || (report == null)) {
				throw new IllegalArgumentException(
						PlatformkitJavaResources.getString("CompatJob.cannotMergeNull")); //$NON-NLS-1$
			}
			modelLoader.flush(); // report must be in new resource set
			setReport(modelLoader.createOUTModel(getUml2(), getCrLocation()));
			final ILauncher launcher = modelLoader.getAtlLauncher();
			launcher.addInModel(report, "IN", "UML2"); //$NON-NLS-1$ //$NON-NLS-2$
			launcher.addInModel(otherReport, "MERGE", "UML2"); //$NON-NLS-1$ //$NON-NLS-2$
			launcher.addOutModel(getReport(), "OUT", "UML2"); //$NON-NLS-1$ //$NON-NLS-2$
			launcher.addLibrary("UML2", uml2Lib.openStream()); //$NON-NLS-1$
			launcher.launch(ILauncher.RUN_MODE, monitor, vmoptions, uml2Copy.openStream(), uml2CRMerge.openStream());
			worked(monitor, PlatformkitJavaResources.getString("CompatJob.mergedCRs"));
		}

		/**
		 * Prunes the current compatibility report
		 * @param monitor
		 * @return True if the pruned report is empty (i.e. the result is compatible)
		 * @throws ATLCoreException
		 * @throws CoreException
		 * @throws IOException
		 * @throws ClassCastException
		 * @throws SecurityException
		 * @throws IllegalArgumentException
		 * @throws NoSuchMethodException
		 * @throws IllegalAccessException
		 * @throws InvocationTargetException
		 */
		public boolean pruneReport(IProgressMonitor monitor) throws ATLCoreException, CoreException, IOException, ClassCastException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
			subTask(monitor, PlatformkitJavaResources.getString("CompatJob.pruningCR")); //$NON-NLS-1$
			IModel report = getReport();
			if (report == null) {
				throw new IllegalArgumentException(
						PlatformkitJavaResources.getString("CompatJob.cannotPruneNull")); //$NON-NLS-1$
			}
			modelLoader.flush(); // report must be in new resource set
			setReport(modelLoader.createOUTModel(getUml2(), getCrLocation()));
			final ILauncher launcher = modelLoader.getAtlLauncher();
			launcher.addInModel(report, "IN", "UML2"); //$NON-NLS-1$ //$NON-NLS-2$
			launcher.addOutModel(getReport(), "OUT", "UML2"); //$NON-NLS-1$ //$NON-NLS-2$
			Object result = launcher.launch(ILauncher.RUN_MODE, monitor, vmoptions, uml2Copy.openStream(), uml2CRPrune.openStream());
			worked(monitor, PlatformkitJavaResources.getString("CompatJob.prunedCR")); //$NON-NLS-1$
			return ATLUtil.getBooleanValue(result);
		}

		/**
		 * Saves the report model
		 * @param monitor
		 * @throws ATLCoreException 
		 * @throws CoreException 
		 */
		public void saveReport(IProgressMonitor monitor) throws ATLCoreException, CoreException {
			subTask(monitor, PlatformkitJavaResources.getString("CompatJob.savingCR")); //$NON-NLS-1$
			modelLoader.getAtlExtractor().extract(getReport(), getCrLocation());
			getFile().getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
			worked(monitor, PlatformkitJavaResources.getString("CompatJob.savedCR")); //$NON-NLS-1$
		}

		/**
		 * @return The file that contains the report model
		 */
		public IFile getReportFile() {
			return getFile().getProject().getFile(getCrPath());
		}
	}

	private static final URL UML_MM  = PlatformkitJavaPlugin.getPlugin().getBundle().getResource("metamodels/UMLProfiles.ecore"); //$NON-NLS-1$
	private static final String CR_PROF = "http://soft.vub.ac.be/platformkit-java/CompatibilityReport.profile.uml"; //$NON-NLS-1$
	private static final String MODEL_HANDLER = "UML2"; //$NON-NLS-1$

	protected final URL uml2CompatibilityReport = 
		PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2CompatibilityReport.asm"); //$NON-NLS-1$
	protected final URL uml2Comparison = 
		PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2Comparison.asm"); //$NON-NLS-1$
	protected final URL uml2Lib = 
		PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2.asm"); //$NON-NLS-1$
	protected final URL uml2Copy = 
		PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2Copy.asm"); //$NON-NLS-1$
	protected final URL uml2CRMerge = 
		PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2CRMerge.asm"); //$NON-NLS-1$
	protected final URL uml2CRPrune = 
		PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2CRPrune.asm"); //$NON-NLS-1$
	protected final Map<String, Object> vmoptions = new HashMap<String, Object>();

	private Object input;
	private URI[] emfUris;
	private ILabelProvider emfUriLabels;
	private IFile outputFile;
	private String apiList;
	private String inputName;

	protected IPreferenceStore store = PlatformkitEditorPlugin.getPlugin().getPreferenceStore();
	protected ModelLoadingStrategy modelLoader = null;

	/**
	 * Runs the main action using the given runner object
	 * @param monitor
	 * @param runner
	 * @param steps
	 * @throws Exception
	 */
	protected void runActionWithRunner(IProgressMonitor monitor, CompatJobRunner runner, int steps)
	throws Exception {
		outputFile = null;
		URI[] emf_uris = getEmfUris();
		ILabelProvider labels = getEmfUriLabels();
		if (emf_uris == null || emf_uris.length == 0) {
			return; //cancel
		}
		beginTask(monitor, getName(), steps + emf_uris.length * 3);
		addATLLogHandler();
		//
		// Step 1
		//
		runner.loadUml2(monitor);
		//
		// Step 2
		//
		runner.loadCRProfile(monitor);
		//
		// Step 3
		//
		runner.loadDepsModel(monitor);
		StringBuffer apiList = null;
		IModel lastReport = null;
		boolean compatible = true;
		for (URI emf_uri : emf_uris) {
			//
			// Step 4+n
			//
			String apiName = labels.getText(emf_uri);
			runner.loadAPIModel(monitor, apiName, emf_uri);
			//
			// Step 5+n
			//
			boolean thisCompatible = runner.run(monitor);
			//
			// Step 6+n
			//
			compatible &= thisCompatible;
			if (!thisCompatible) {
				if (lastReport != null) {
					runner.mergeReport(monitor, lastReport);
				} else {
					worked(monitor, null);
				}
				lastReport = runner.getReport();
			} else {
				worked(monitor, null);
			}
			if (apiList == null) {
				apiList = new StringBuffer();
				apiList.append(apiName);
			} else {
				apiList.append(", "); //$NON-NLS-1$
				apiList.append(apiName);
			}
		}
		//
		// Step 7
		//
		setApiList(apiList.toString());
		if (!compatible) {
			compatible = runner.pruneReport(monitor);
		} else {
			worked(monitor, null);
		}
		//
		// Step 8
		//
		if (!compatible) {
			runner.saveReport(monitor);
			outputFile = runner.getReportFile();
		} else {
			worked(monitor, null);
		}
		worked(monitor, PlatformkitJavaResources.getString("CompatJob.finished")); //$NON-NLS-1$
	}

	/**
	 * Checks the current model loading strategy and updates it if necessary
	 * @throws ATLCoreException
	 */
	protected void checkAndSwitchStrategy() throws ATLCoreException {
		final boolean useCache = store.getBoolean(PreferenceConstants.P_CACHE_API);
		final String atlVMName = store.getString(PreferenceConstants.P_ATLVM);
		if (atlVMName == null || atlVMName.equals("")) {
			throw new ATLCoreException(
					PlatformkitJavaResources.getString("CompatJob.noAtlVmChosen")); //$NON-NLS-1$
		}
		if (modelLoader != null && modelLoader.isValidFor(atlVMName, useCache)) {
			return;
		}
		if (useCache) {
			modelLoader = new CachingModelLoadingStrategy(atlVMName);
		} else {
			modelLoader = new SimpleModelLoadingStrategy(atlVMName);
		}
		Assert.isNotNull(modelLoader);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void finallyCleanup() {
		try {
			removeATLLogHandler();
			modelLoader.flush();
		} catch (ATLCoreException e) {
			PlatformkitJavaPlugin.getPlugin().report(e);
		}
	}

	/**
	 * @return the job input
	 */
	public Object getInput() {
		return input;
	}

	/**
	 * @param input the job input to set
	 */
	public void setInput(Object input) {
		this.input = input;
	}

	/**
	 * @return the emf_uris
	 */
	public URI[] getEmfUris() {
		return emfUris;
	}

	/**
	 * @param emfUris the emf_uris to set
	 */
	public void setEmfUris(URI[] emfUris) {
		this.emfUris = emfUris;
	}

	/**
	 * @return the emfUriLabels
	 */
	public ILabelProvider getEmfUriLabels() {
		return emfUriLabels;
	}

	/**
	 * @param emfUriLabels the emfUriLabels to set
	 */
	public void setEmfUriLabels(ILabelProvider emfUriLabels) {
		this.emfUriLabels = emfUriLabels;
	}

	/**
	 * @param outputFile The output file to set
	 */
	protected void setOutputFile(IFile outputFile) {
		this.outputFile = outputFile;
	}

	/**
	 * @return The output file
	 */
	public IFile getOutputFile() {
		return outputFile;
	}

	/**
	 * @return the apiList
	 */
	public String getApiList() {
		return apiList;
	}

	/**
	 * @param apiList the apiList to set
	 */
	protected void setApiList(String apiList) {
		this.apiList = apiList;
	}

	/**
	 * @param inputName the inputName to set
	 */
	protected void setInputName(String inputName) {
		this.inputName = inputName;
	}

	/**
	 * @return the inputName
	 */
	public String getInputName() {
		return inputName;
	}

}
