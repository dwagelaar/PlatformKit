package be.ac.vub.platformkit.presentation.jobs;

import java.io.InputStream;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.provider.ItemProviderAdapter;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.editor.preferences.PreferenceInitializer;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.presentation.util.IEObjectValidator;
import be.ac.vub.platformkit.presentation.util.PlatformEValidator;
import be.ac.vub.platformkit.presentation.util.PlatformKitActionUtil;
import be.ac.vub.platformkit.presentation.util.PlatformKitEObjectValidator;
import be.ac.vub.platformkit.presentation.util.PlatformKitException;
import be.ac.vub.platformkit.presentation.util.PlatformEValidator.Registry;
import be.ac.vub.platformkit.presentation.util.provider.PlatformKitItemProviderAdapter;

/**
 * Operation to profile the options in an EMF editor
 * against a platform instance specification.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class ProfileJob extends ConstraintSpaceJob {

	private EObject selectedObject;

	/**
	 * Creates a new {@link ProfileJob}.
	 */
	public ProfileJob() {
		super("Profiling EMF editor against platform instance");
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.jobs.ProgressMonitorJob#runAction(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void runAction(IProgressMonitor monitor) throws Exception {
		beginTask(monitor, getName(), 8);
		//
		// 1
		//
		subTask(monitor, "Searching for constraint space...");
		URI platformkitURI = findPlatformKitURI();
		ConstraintSpace space = PlatformKitActionUtil.getCachedConstraintSpace(platformkitURI);
		worked(monitor, "Searched for constraint space");
		if (space == null) {
			//
			// 2
			//
			subTask(monitor, "Loading Platformkit model...");
			ResourceSet resourceSet = new ResourceSetImpl();
			Resource platformkit = resourceSet.getResource(platformkitURI, true);
			worked(monitor, "Loaded Platformkit model");
			//
			// 3
			//
			monitor.subTask("Loading source ontologies...");
			if (platformkit.getContents().size() == 0) {
				throw new PlatformKitException("Resource " + platformkit + " is empty");
			}
			space = (ConstraintSpace) platformkit.getContents().get(0);
			IOntologies ont = PreferenceInitializer.getPreferredOntologyFactory().createIOntologies();
			space.setKnowledgeBase(ont);
			if (!space.init(true)) {
				throw new PlatformKitException(
				"Ontologies not pre-classified - Choose 'Classify Taxonomy' first");
			}
			PlatformKitActionUtil.setCachedConstraintSpace(platformkitURI, space);
			worked(monitor, "Loaded source ontologies");
			//
			// 4
			//
			subTask(monitor, "Attaching DL reasoner...");
			attachDLReasoner(monitor, ont);
			worked(monitor, "Attached DL reasoner");
		} else {
			//
			// 2, 3, 4
			//
			monitor.subTask("Using cached constraint space");
			worked(monitor, null);
			worked(monitor, null);
			worked(monitor, null);
		}
		//
		// 5
		//
		subTask(monitor, "Retrieving platform instance specifications...");
		Resource res = getSelectedObject().eResource();
		if (loadPlatformInstances(space)) {
			worked(monitor, "Retrieved platform instance specifications");
			//
			// 6
			//
			subTask(monitor, "Retrieving intersection set...");
			ConstraintSet is = space.getIntersectionSet();
			is.getIntersection();
			worked(monitor, "Retrieved intersection set");
			//
			// 7
			//
			subTask(monitor, "Determining (in-)valid constraint sets...");
			IEObjectValidator validator = new PlatformKitEObjectValidator(space);
			Registry.INSTANCE.put(res, validator);
			worked(monitor, "Determined (in-)valid constraint sets");
			//
			// 8
			//
			subTask(monitor, "Profiling editor...");
			updateAllObjects(res);
			worked(monitor, "Profiled editor");
		} else {
			worked(monitor, "No platform instance specifications found");
			//
			// 6
			//
			subTask(monitor, "Removing platform instance specifications...");
			worked(monitor, "Removed platform instance specifcations");
			//
			// 7
			//
			monitor.subTask("Determining (in-)valid constraint sets...");
			Registry.INSTANCE.remove(res);
			worked(monitor, "Determined (in-)valid constraint sets");
			//
			// 8
			//
			monitor.subTask("Profiling editor...");
			updateAllObjects(res);
			worked(monitor, "Profiled editor");
		}
	}

	/**
	 * Finds the PlatformKit model URI via the meta-model of the selected object.
	 * @return The PlatformKit model URI.
	 */
	protected URI findPlatformKitURI() {
		EObject object = getSelectedObject();
		Assert.isNotNull(object);
		logger.info(object.eAdapters().toString());
		Resource res = object.eResource();
		Assert.isNotNull(res);
		EObject root = (EObject) res.getContents().get(0);
		Assert.isNotNull(root);
		Resource rootRes = root.eClass().eResource();
		Assert.isNotNull(rootRes);
		URI resURI = rootRes.getURI();
		URI platformkitURI = resURI.trimFileExtension().appendFileExtension("platformkit");
		logger.info("Platformkit URI = " + platformkitURI.toString());
		return platformkitURI;
	}

	/**
	 * Loads the platform specification ontologies specified by
	 * {@link #getPlatformInstanceSources()}.
	 * @param space
	 * @return True if any platform specification was loaded.
	 * @throws CoreException
	 */
	protected boolean loadPlatformInstances(ConstraintSpace space) throws CoreException {
		boolean loaded = false;
		IOntologies ont = space.getKnowledgeBase();
		Object[] sources = getPlatformInstanceSources();
		if (sources != null) {
			for (int i = 0; i < sources.length; i++) {
				if (sources[i] instanceof IFile) {
					IFile file = (IFile) sources[i];
					ont.loadInstances(file.getContents());
					loaded = true;
				} else if (sources[i] instanceof InputStream) {
					ont.loadInstances((InputStream) sources[i]);
					loaded = true;
				}
			}
		}
		return loaded;
	}

	/**
	 * Updates all {@link EObject}s in res.
	 * @param res
	 */
	protected void updateAllObjects(Resource res) {
		IEObjectValidator validator = (IEObjectValidator) Registry.INSTANCE.get(res);
		for (Iterator<EObject> it = res.getAllContents(); it.hasNext();) {
			EObject object = it.next();
			updateObject((EObject) object, validator);
			registerEValidator((EObject) object);
		}
	}

	/**
	 * Updates the given {@link EObject} with a wrapper and the given validator.
	 * @param object
	 * @param validator
	 */
	protected void updateObject(EObject object, IEObjectValidator validator) {
		Assert.isNotNull(object);
		EditingDomain editingDomain = getEditingDomain();
		for (int i = 0; i < object.eAdapters().size(); i++) {
			Object adapter = object.eAdapters().get(i);
			if ((adapter instanceof ItemProviderAdapter) && (editingDomain instanceof AdapterFactoryEditingDomain)) {
				PlatformKitItemProviderAdapter wrapper =
					new PlatformKitItemProviderAdapter(
							(ItemProviderAdapter) adapter, 
							((AdapterFactoryEditingDomain) editingDomain).getAdapterFactory());
				logger.info("Created wrapper adapter for " + object.toString());
				object.eAdapters().set(i, wrapper);
				adapter = wrapper;
			}
			if (adapter instanceof PlatformKitItemProviderAdapter) {
				PlatformKitItemProviderAdapter pkAdapter = (PlatformKitItemProviderAdapter) adapter;
				if ((pkAdapter.getValidator() != validator)) {
					pkAdapter.setValidator(validator);
					logger.info("Attached new validator to wrapper " + pkAdapter.toString());
				}
			}
		}
		if (validator == null) {
			return;
		}
	}

	/**
	 * Registers the {@link PlatformEValidator} for object's meta-model.
	 * @param object
	 */
	protected void registerEValidator(EObject object) {
		Assert.isNotNull(object);
		EPackage pack = object.eClass().getEPackage();
		Assert.isNotNull(pack);
		EValidator orig = EValidator.Registry.INSTANCE.getEValidator(pack);
		if (!(orig instanceof PlatformEValidator)) {
			EValidator eValidator = new PlatformEValidator(orig);
			EValidator.Registry.INSTANCE.put(pack, eValidator);
			logger.info("Registered new PlatformEValidator for " + pack.getNsURI());
		}
	}

	/**
	 * @return the selectedObject
	 */
	public EObject getSelectedObject() {
		return selectedObject;
	}

	/**
	 * @param selectedObject the selectedObject to set
	 */
	public void setSelectedObject(EObject selectedObject) {
		this.selectedObject = selectedObject;
	}

}
