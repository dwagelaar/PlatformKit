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
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.m2m.atl.common.ATLLogger;
import org.eclipse.m2m.atl.core.ATLCoreException;
import org.eclipse.m2m.atl.core.IExtractor;
import org.eclipse.m2m.atl.core.IModel;
import org.eclipse.m2m.atl.core.IReferenceModel;
import org.eclipse.m2m.atl.core.launch.ILauncher;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.UMLPackage;

import be.ac.vub.jar2uml.AddInferredTagSwitch;
import be.ac.vub.jar2uml.FindContainedClassifierSwitch;
import be.ac.vub.jar2uml.JarToUML;
import be.ac.vub.jar2uml.MergeModel;
import be.ac.vub.platformkit.Constraint;
import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.PlatformkitFactory;
import be.ac.vub.platformkit.editor.preferences.PreferenceConstants;
import be.ac.vub.platformkit.editor.preferences.PreferenceInitializer;
import be.ac.vub.platformkit.io.IFileOutputStream;
import be.ac.vub.platformkit.java.JavaOntologyProvider;
import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;
import be.ac.vub.platformkit.java.PlatformkitJavaResources;
import be.ac.vub.platformkit.java.popup.util.ATLUtil;
import be.ac.vub.platformkit.jobs.ProgressMonitorJob;
import be.ac.vub.platformkit.kb.BaseOntologyProvider;
import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntModel;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.IOntologyProvider;
import be.ac.vub.platformkit.kb.util.OntException;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * Operation for creating compatibility reports for UML dependency models 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class CompatJob extends ProgressMonitorJob {

	public static final int STEPS = 9;

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
	 * @param umlPack
	 * @return <code>true</code> iff umlPack contains non-inferred {@link Classifier}s
	 */
	public static final boolean containsClassifiers(final Package umlPack) {
		if (AddInferredTagSwitch.isInferred(umlPack)) {
			return false;
		}
		for (PackageableElement element : umlPack.getPackagedElements()) {
			if (element instanceof Classifier && !AddInferredTagSwitch.isInferred(element)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Finds the root {@link Model} in model, or creates a new one if not found.
	 * @param model
	 * @return the root {@link Model} in model
	 */
	public static final Model findRootModel(final IModel model) {
		for (Object o : model.getElementsByType(UMLPackage.eINSTANCE.getModel())) {
			return (Model) o;
		}
		return (Model) model.newElement(UMLPackage.eINSTANCE.getModel());
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
		runActionWithRunner(monitor, runner, STEPS);
	}

	/**
	 * Defines model loading strategy interface and behaviour.
	 */
	public class ModelLoadingStrategy {

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
		 * @param emfUri The EMF {@link URI} to load from
		 * @return the loaded 'IN' model
		 * @throws ATLCoreException 
		 */
		public IModel loadINModel(IReferenceModel uml2, URI emfUri) throws ATLCoreException {
			return atlUtil.loadModel(uml2, emfUri.toString(), "IN", emfUri.toString()); //$NON-NLS-1$
		}

		/**
		 * @param uml2 The UML2 metamodel
		 * @param res The EMF {@link Resource} to load from
		 * @return the loaded 'IN' model
		 * @throws ATLCoreException 
		 */
		public IModel loadINModelFromResource(IReferenceModel uml2, Resource res) throws ATLCoreException {
			return atlUtil.loadModel(uml2, res, "IN"); //$NON-NLS-1$
		}

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
		public boolean isValidFor(String atlVMName) {
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
		private ConstraintSpace constraintSpace;
		private Map<URI, Set<String>> providedPackages = new HashMap<URI, Set<String>>();
		private Map<String, Set<URI>> packageProviders = new HashMap<String, Set<URI>>();

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
			final String fileName = file.getName();
			setInputName(fileName.substring(0, fileName.indexOf('.'))); // file basename
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
		 * @return the constraintSpace
		 */
		public ConstraintSpace getConstraintSpace() {
			return constraintSpace;
		}
		/**
		 * @param constraintSpace the constraintSpace to set
		 */
		protected void setConstraintSpace(ConstraintSpace constraintSpace) {
			this.constraintSpace = constraintSpace;
		}
		/**
		 * @return the {@link IFile} of the Platformkit model, based on the file path
		 * @see #getFile()
		 */
		protected IFile getPkFile() {
			final IFile file = getFile();
			assert file != null;
			return file.getProject().getFile(
					file.getProjectRelativePath()
					.removeFileExtension()
					.removeFileExtension()
					.addFileExtension("platformkit").toString()); //$NON-NLS-1$
		}
		/**
		 * @return the EMF URI of the Platformkit model, based on the file path
		 * @see #getFile()
		 */
		protected URI getPkLocation() {
			final IFile file = getPkFile();
			assert file != null;
			final StringBuffer pkFileLocation = new StringBuffer();
			pkFileLocation.append(file.getProject().getName());
			pkFileLocation.append("/"); //$NON-NLS-1$
			pkFileLocation.append(file.getProjectRelativePath());
			return URI.createPlatformResourceURI(pkFileLocation.toString(), true);
		}
		/**
		 * @return the EMF {@link Resource} of the Platformkit model, based on the file path
		 * @see #getFile()
		 */
		protected Resource getPkResource() {
			final ResourceSet rs = new ResourceSetImpl();
			Resource res;
			if (getPkFile().exists()) {
				res = rs.getResource(getPkLocation(), true);
			} else {
				res = rs.createResource(getPkLocation());
			}
			return res;
		}
		/**
		 * @return the ontology file, based on the file path
		 * @see #getFile()
		 */
		protected IFile getOntFile() {
			final IFile file = getFile();
			assert file != null;
			final IPath ontPath = file.getProjectRelativePath()
				.removeFileExtension()
				.removeFileExtension()
				.addFileExtension("owl");
			return file.getProject().getFile(ontPath);
		}
		/**
		 * @param emf_uri
		 * @return the qualified names of the packages provided by the API model with emf_uri
		 */
		public Set<String> getProvidedPackages(URI emf_uri) {
			Set<String> packages = providedPackages.get(emf_uri);
			if (packages == null) {
				packages = new HashSet<String>();
				providedPackages.put(emf_uri, packages);
			}
			return packages;
		}
		/**
		 * @param packName the UML qualified name of the package
		 * @return the EMF URIs of the API models that provide the package with the given qualified name
		 */
		public Set<URI> getPackageProviders(String packName) {
			Set<URI> emf_uris = packageProviders.get(packName);
			if (emf_uris == null) {
				emf_uris = new HashSet<URI>();
				packageProviders.put(packName, emf_uris);
			}
			return emf_uris;
		}

		/**
		 * Adds the qualified names of all packages contained in apiModel to
		 * the set of qualified names of packages provided by the API model with emf_uri,
		 * and adds emf_uri to the package providers of each package.
		 * @param emf_uri
		 * @param apiModel
		 * @see #getPackageProviders(String)
		 * @see #getProvidedPackages(URI)
		 */
		protected void addAllProvidedPackages(URI emf_uri, IModel apiModel) {
			final Set<?> packages = apiModel.getElementsByType(UMLPackage.eINSTANCE.getPackage());
			final Set<String> provided = getProvidedPackages(emf_uri);
			for (Object pack : packages) {
				assert pack instanceof Package;
				if (containsClassifiers((Package) pack)) {
					String packName = JarToUML.qualifiedName((Package) pack);
					provided.add(packName);
					getPackageProviders(packName).add(emf_uri);
				}
			}
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
			assert file != null;
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
			final IModel load = modelLoader.loadINModel(getUml2(), emf_uri);
			final IModel in = getIn();
			if (in == null) {
				setIn(load);
				monitor.worked(2);
			} else {
				final MergeModel mergeModel = new MergeModel();
				final Model base = findRootModel(in);
				final Model merge = findRootModel(load);
				assert base != null;
				assert merge != null;
				mergeModel.setBaseModel(base);
				mergeModel.setMergeModel(merge);
				mergeModel.setMonitor(new SubProgressMonitor(monitor, 2));
				mergeModel.run();
				setIn(modelLoader.loadINModelFromResource(getUml2(), base.eResource()));
			}
			addAllProvidedPackages(emf_uri, load);
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
		 * Loads the Platformkit model for the set file and sets the {@link ConstraintSpace} from the model.
		 * @param monitor
		 * @throws CoreException 
		 * @throws IOException 
		 * @throws OntException 
		 * @see #getFile()
		 * @see #getConstraintSpace()
		 */
		public void loadPlatformkitModel(IProgressMonitor monitor) throws IOException, CoreException, OntException {
			subTask(monitor, PlatformkitJavaResources.getString("CompatJob.loadingPK")); //$NON-NLS-1$
			final Resource res = getPkResource();
			assert res != null;
			ConstraintSpace space = null;
			for (EObject object : res.getContents()) {
				if (object instanceof ConstraintSpace) {
					space = (ConstraintSpace) object;
					break;
				}
			}
			final IOntologies kb = PreferenceInitializer.getPreferredOntologyFactory().createIOntologies();
			if (space == null || !getOntFile().exists()) {
				final String platformConstraintURI = createInitialOntology(kb, monitor);
				space = createInitialConstraintSpace(platformConstraintURI);
				res.getContents().add(space);
				res.save(Collections.emptyMap());
			}
			space.setKnowledgeBase(kb);
			space.init(false);
			setConstraintSpace(space);
			worked(monitor, PlatformkitJavaResources.getString("CompatJob.loadedPK")); //$NON-NLS-1$
		}

		/**
		 * Creates an initial platform dependency constraint ontology
		 * @param kb the knowledge base object to use
		 * @param monitor the progress monitor
		 * @return the platform dependency constraint class URI
		 * @throws OntException 
		 * @throws IOException 
		 */
		protected String createInitialOntology(final IOntologies kb, final IProgressMonitor monitor)
		throws OntException, IOException {
			final IFile ontFile = getOntFile();
			final IOntModel ont = kb.createNewOntology(IOntologies.DEPS_BASE_NS + ontFile.getName());
			final IOntModel platform = kb.getLocalOntology(BaseOntologyProvider.PLATFORM_NS);
			final IOntModel isa = kb.getLocalOntology(BaseOntologyProvider.ISA_NS);
			final IOntModel java = kb.getLocalOntology(JavaOntologyProvider.JAVA_NS);
			kb.attachTransitiveReasoner();
			// Create JavaBytecode constraint class
			final IOntClass jbcClass = isa.getOntClass(BaseOntologyProvider.JAVA_BYTECODE_URI);
			assert jbcClass != null;
			final String jbcConstraintURI = ont.getNsURI() + "#" + getInputName() + "JavaBytecode"; //$NON-NLS-1$ //$NON-NLS-2$
			final IOntClass jbcConstraintClass = ont.createSomeRestriction(
					jbcConstraintURI,
					jbcClass,
					null,
					null);
			// Create JavaVM constraint class
			final IOntClass jvmClass = java.getOntClass(JavaOntologyProvider.JAVA_VM_URI);
			assert jvmClass != null;
			final String jvmConstraintURI = ont.getNsURI() + "#" + getInputName() + "JavaVM"; //$NON-NLS-1$ //$NON-NLS-2$
			final List<IOntClass> jvmConstraintRange = new ArrayList<IOntClass>();
			jvmConstraintRange.add(jbcConstraintClass);
			final IOntClass jvmConstraintClass = ont.createSomeRestriction(
					jvmConstraintURI,
					jvmClass,
					BaseOntologyProvider.IMPLEMENTS_INTERFACE_URI,
					jvmConstraintRange.iterator());
			// Create JRE constraint class
			final IOntClass jreClass = java.getOntClass(JavaOntologyProvider.JRE_URI);
			assert jreClass != null;
			final String jreConstraintURI = ont.getNsURI() + "#" + getInputName() + "JRE"; //$NON-NLS-1$ //$NON-NLS-2$
			final List<IOntClass> jreConstraintRange = new ArrayList<IOntClass>();
			jreConstraintRange.add(jvmConstraintClass);
			final IOntClass jreConstraintClass = ont.createSomeRestriction(
					jreConstraintURI,
					jreClass,
					BaseOntologyProvider.PROVIDES_FEATURE_URI,
					jreConstraintRange.iterator());
			// Create Platform constraint class
			final IOntClass platformClass = platform.getOntClass(BaseOntologyProvider.PLATFORM_URI);
			assert platformClass != null;
			final String platformConstraintURI = ont.getNsURI() + "#" + getInputName() + "Platform"; //$NON-NLS-1$ //$NON-NLS-2$
			final List<IOntClass> platformConstraintRange = new ArrayList<IOntClass>();
			platformConstraintRange.add(jreConstraintClass);
			ont.createSomeRestriction(
					platformConstraintURI,
					platformClass,
					BaseOntologyProvider.PROVIDES_FEATURE_URI,
					platformConstraintRange.iterator());
			kb.detachReasoner();
			// Write ontology to file
			final IFileOutputStream out = new IFileOutputStream(ontFile, new SubProgressMonitor(monitor, 0));
			ont.save(out);
			out.close();
			kb.unloadOntology(ont);
			return platformConstraintURI;
		}

		/**
		 * Creates an initial {@link ConstraintSpace} object, given the platform dependency constraint class URI.
		 * @param platformConstraintURI
		 * @return the initial {@link ConstraintSpace}
		 */
		protected ConstraintSpace createInitialConstraintSpace(final String platformConstraintURI) {
			final ConstraintSpace space = PlatformkitFactory.eINSTANCE.createConstraintSpace();
			final IFile ontFile = getOntFile();
			space.getOntology().add(ontFile.getName());
			final ConstraintSet cs = PlatformkitFactory.eINSTANCE.createConstraintSet();
			space.getConstraintSet().add(cs);
			cs.setName(getInputName());
			final Constraint c = PlatformkitFactory.eINSTANCE.createConstraint();
			cs.getConstraint().add(c);
			c.setOntClassURI(platformConstraintURI);
			return space;
		}

		/**
		 * Updates/creates the platform dependency ontology.
		 * @param monitor
		 * @throws IOException 
		 * @throws OntException 
		 * @throws CoreException 
		 */
		public void updateOntology(IProgressMonitor monitor) throws IOException, OntException, CoreException {
			subTask(monitor, PlatformkitJavaResources.getString("CompatJob.updatingOnt")); //$NON-NLS-1$
			final ConstraintSpace space = getConstraintSpace();
			final Constraint constraint = space.getConstraintSet().get(0).getConstraint().get(0);
			final IOntologies kb = space.getKnowledgeBase();
			final IOntModel ont = kb.loadSingleOnt(getOntFile().getContents());
			// Create/update JavaBytecode constraint class
			final IOntModel isa = kb.getLocalOntology(BaseOntologyProvider.ISA_NS);
			final IOntClass jbcClass = isa.getOntClass(BaseOntologyProvider.JAVA_BYTECODE_URI);
			assert jbcClass != null;
			EAnnotation jbcAnn = findJavaBytecodeAnnotation();
			assert jbcAnn != null;
			final EMap<String,String> jbcDetails = jbcAnn.getDetails();
			final String jbcConstraintURI = ont.getNsURI() + "#" + getInputName() + "JavaBytecode"; //$NON-NLS-1$ //$NON-NLS-2$
			final IOntClass jbcConstraintClass = ont.createMinInclusiveRestriction(
					jbcConstraintURI,
					jbcClass,
					JavaOntologyProvider.MAJOR_VERSION_NUMBER_URI,
					IOntologyProvider.INTEGER_URI,
					jbcDetails.get(JarToUML.MAJOR_BYTECODE_FORMAT_VERSION));
			if (!Boolean.parseBoolean(jbcDetails.get(JarToUML.PREVERIFIED))) {
				ont.createHasValueRestriction(
						jbcConstraintURI,
						jbcClass,
						JavaOntologyProvider.PREVERIFIED_URI,
						IOntologyProvider.BOOLEAN_URI,
						Boolean.FALSE.toString());
			}
			// Create/update JavaVM constraint class
			final IOntModel java = kb.getLocalOntology(JavaOntologyProvider.JAVA_NS);
			final IOntClass jvmClass = java.getOntClass(JavaOntologyProvider.JAVA_VM_URI);
			assert jvmClass != null;
			final String jvmConstraintURI = ont.getNsURI() + "#" + getInputName() + "JavaVM"; //$NON-NLS-1$ //$NON-NLS-2$
			final List<IOntClass> jvmConstraintRange = new ArrayList<IOntClass>();
			jvmConstraintRange.add(jbcConstraintClass);
			kb.attachTransitiveReasoner();
			final IOntClass jvmConstraintClass = ont.createSomeRestriction(
					jvmConstraintURI,
					jvmClass,
					BaseOntologyProvider.IMPLEMENTS_INTERFACE_URI,
					jvmConstraintRange.iterator());
			// Create/update JRE constraint class
			final List<Package> compatPacks = findCompatiblePackages(monitor);
			final List<IOntClass> rangeClasses = new ArrayList<IOntClass>();
			for (Package pack : compatPacks) {
				String packName = JarToUML.qualifiedName((Package) pack);
				for (URI emf_uri : getPackageProviders(packName)) {
					String ont_uri = getOntURI(emf_uri);
					IOntModel javaOnt = kb.getLocalOntology(ont_uri);
					IOntClass javaLibrary = javaOnt.getOntClass(ont_uri + "#" + JavaOntologyProvider.toJavaLibaryName(packName));
					assert javaLibrary != null;
					rangeClasses.add(javaLibrary);
				}
			}
			rangeClasses.add(jvmConstraintClass);
			final IOntClass jreClass = java.getOntClass(JavaOntologyProvider.JRE_URI);
			final String jreConstraintURI = ont.getNsURI() + "#" + getInputName() + "JRE";
			final IOntClass jreConstraint = ont.createSomeRestriction(
					jreConstraintURI,
					jreClass,
					BaseOntologyProvider.PROVIDES_FEATURE_URI, 
					rangeClasses.iterator());
			// Update Platform constraint class
			final IOntModel platform = kb.getLocalOntology(BaseOntologyProvider.PLATFORM_NS);
			final IOntClass platformClass = platform.getOntClass(BaseOntologyProvider.PLATFORM_URI);
			final List<IOntClass> jreConstraintList = new ArrayList<IOntClass>();
			jreConstraintList.add(jreConstraint);
			ont.createSomeRestriction(
					constraint.getOntClassURI(),
					platformClass,
					BaseOntologyProvider.PROVIDES_FEATURE_URI, 
					jreConstraintList.iterator());
			kb.detachReasoner();
			IFileOutputStream out = new IFileOutputStream(getOntFile(), new SubProgressMonitor(monitor, 0));
			ont.save(out);
			out.close();
			kb.unloadOntology(ont);
			worked(monitor, PlatformkitJavaResources.getString("CompatJob.updatedOnt")); //$NON-NLS-1$
		}

		/**
		 * @param monitor
		 * @return all classifier-containing packages in the dependency model that are compatible according to the compatibility report
		 */
		protected List<Package> findCompatiblePackages(IProgressMonitor monitor) {
			final List<Package> result = new ArrayList<Package>();
			final IModel deps = getDeps();
			final IModel cr = getReport();
			final Model crRoot = (Model) cr.getElementsByType(UMLPackage.eINSTANCE.getModel()).iterator().next();
			final FindContainedClassifierSwitch findClassifier = new FindContainedClassifierSwitch();
			for (Object pack : deps.getElementsByType(UMLPackage.eINSTANCE.getPackage())) {
				Package umlPack = (Package) pack;
				if (containsClassifiers(umlPack)) {
					String javaName = JarToUML.qualifiedName(umlPack).replace("::", ".");
					Package crPack = findClassifier.findPackage(crRoot, javaName, false);
					if (crPack == null ||
							(crPack.getAppliedStereotype("CompatibilityReport::Incompatible") == null
							&& crPack.getAppliedStereotype("CompatibilityReport::Missing") == null)) {
						//compatibility report does not contain <<Incompatible>> or <<Missing>> version of this package
						//=> one of the API models provides this package
						result.add(umlPack);
					}
				}
				checkCanceled(monitor);
			}
			return result;
		}

		/**
		 * @return the {@link EAnnotation} that contains the required Java bytecode format information
		 */
		protected EAnnotation findJavaBytecodeAnnotation() {
			final IModel deps = getDeps();
			final Model depsRoot = (Model) deps.getElementsByType(UMLPackage.eINSTANCE.getModel()).iterator().next();
			return depsRoot.getEAnnotation(JarToUML.EANNOTATION);
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
	protected final URL uml2CRPrune = 
		PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2CRPrune.asm"); //$NON-NLS-1$
	protected final Map<String, Object> vmoptions = new HashMap<String, Object>();

	private Object input;
	private URI[] emfUris;
	private ILabelProvider emfUriLabels;
	private IFile outputFile;
	private String apiList;
	private String inputName;
	private Map<URI, String> ontURIs;
	private boolean createOntology;

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
		final boolean createOntology = isCreateOntology();
		//amount of tasks = default + 3 * amount of API models - 2 if no ontology is created
		beginTask(monitor, getName(), steps + emf_uris.length*3 - (createOntology ? 0 : 2));
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
		for (URI emf_uri : emf_uris) {
			//
			// Step n+3
			//
			String apiName = labels.getText(emf_uri);
			runner.loadAPIModel(monitor, apiName, emf_uri);
			if (apiList == null) {
				apiList = new StringBuffer();
				apiList.append(apiName);
			} else {
				apiList.append(", "); //$NON-NLS-1$
				apiList.append(apiName);
			}
		}
		//
		// Step 4
		//
		final boolean compatible = runner.run(monitor);
		//
		// Step 5
		//
		setApiList(apiList.toString());
		if (!compatible) {
			runner.pruneReport(monitor);
		} else {
			worked(monitor, null);
		}
		//
		// Step 6
		//
		if (!compatible) {
			runner.saveReport(monitor);
			outputFile = runner.getReportFile();
		} else {
			worked(monitor, null);
		}
		if (createOntology) {
			//
			// Step 7
			//
			runner.loadPlatformkitModel(monitor);
			//
			// Step 8
			//
			runner.updateOntology(monitor);
		}
		//
		// Step 7 or 9
		//
		worked(monitor, PlatformkitJavaResources.getString("CompatJob.finished")); //$NON-NLS-1$
	}

	/**
	 * Checks the current model loading strategy and updates it if necessary
	 * @throws ATLCoreException
	 */
	protected void checkAndSwitchStrategy() throws ATLCoreException {
		final String atlVMName = store.getString(PreferenceConstants.P_ATLVM);
		if (atlVMName == null || atlVMName.equals("")) {
			throw new ATLCoreException(
					PlatformkitJavaResources.getString("CompatJob.noAtlVmChosen")); //$NON-NLS-1$
		}
		if (modelLoader != null && modelLoader.isValidFor(atlVMName)) {
			return;
		}
		modelLoader = new ModelLoadingStrategy(atlVMName);
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

	/**
	 * @param emf_uri
	 * @return The ontology namespace URI for the platform API model with emf_uri
	 * @throws IOException
	 */
	public String getOntURI(URI emf_uri) throws IOException {
		if (ontURIs == null) {
			ontURIs = new HashMap<URI, String>();
			final IExtensionRegistry registry = Platform.getExtensionRegistry();
			if (registry == null) {
				throw new IOException(PlatformkitJavaResources.getString("registryNotFound")); //$NON-NLS-1$
			}
			final IExtensionPoint point = registry.getExtensionPoint(PlatformkitJavaPlugin.PLATFORMAPI_EXT_POINT);
			final IExtension[] extensions = point.getExtensions();
			for (int i = 0 ; i < extensions.length ; i++) {
				IConfigurationElement[] elements = extensions[i].getConfigurationElements();
				for (int j = 0 ; j < elements.length ; j++) {
					try {
						URI this_emf_uri = URI.createURI(elements[j].getAttribute("emf_uri")); //$NON-NLS-1$
						String this_ont_uri = elements[j].getAttribute("ont_uri"); //$NON-NLS-1$
						ontURIs.put(this_emf_uri, this_ont_uri);
					} catch (IllegalArgumentException e) {
						throw new IOException(e.getLocalizedMessage());
					}
				}
			}
		}
		return ontURIs.get(emf_uri);
	}

	/**
	 * @return whether or not to create/update a platform dependency ontology
	 */
	public boolean isCreateOntology() {
		return createOntology;
	}

	/**
	 * Sets whether or not to create/update a platform dependency ontology.
	 * @param createOntology the createOntology to set
	 */
	public void setCreateOntology(boolean createOntology) {
		this.createOntology = createOntology;
	}

}
