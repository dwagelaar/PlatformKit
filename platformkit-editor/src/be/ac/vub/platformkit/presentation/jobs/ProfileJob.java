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
import be.ac.vub.platformkit.kb.util.OntException;
import be.ac.vub.platformkit.logging.PlatformkitLogger;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.util.ConstraintSpaceCache;
import be.ac.vub.platformkit.presentation.util.IEObjectValidator;
import be.ac.vub.platformkit.presentation.util.PlatformEValidator;
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

	protected ConstraintSpaceCache cache = new ConstraintSpaceCache();
	
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
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.searchingCS")); //$NON-NLS-1$
		URI platformkitURI = findPlatformKitURI();
		URI inferredOwlURI = platformkitURI.trimFileExtension().appendFileExtension("inferred.owl"); //$NON-NLS-1$
		ConstraintSpace space = cache.get(inferredOwlURI);
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.searchedCS")); //$NON-NLS-1$
		if (space == null) {
			//
			// 2
			//
			subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.loadingPKmodel")); //$NON-NLS-1$
			ResourceSet resourceSet = new ResourceSetImpl();
			Resource platformkit = resourceSet.getResource(platformkitURI, true);
			worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.loadedPKmodel")); //$NON-NLS-1$
			//
			// 3
			//
			subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("loadingSourceOnt")); //$NON-NLS-1$
			if (platformkit.getContents().size() == 0) {
				throw new PlatformKitException(String.format(
						PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.resourceEmpty"), 
						platformkit)); //$NON-NLS-1$
			}
			space = (ConstraintSpace) platformkit.getContents().get(0);
			IOntologies ont = PreferenceInitializer.getPreferredOntologyFactory().createIOntologies();
			selectReasoner(ont);
			space.setKnowledgeBase(ont);
			if (!space.init(true)) {
				throw new PlatformKitException(
				PlatformkitEditorPlugin.getPlugin().getString("ontNotPreclassified")); //$NON-NLS-1$
			}
			cache.put(inferredOwlURI, space);
			worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("loadedSourceOnt")); //$NON-NLS-1$
			//
			// 4
			//
			subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("attachingDlReasoner")); //$NON-NLS-1$
			ont.attachDLReasoner();
			worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("attachedDlReasoner")); //$NON-NLS-1$
		} else {
			//
			// 2, 3, 4
			//
			subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.usingCachedCS")); //$NON-NLS-1$
			selectReasoner(space.getKnowledgeBase());
			worked(monitor, null);
			worked(monitor, null);
			worked(monitor, null);
		}
		//
		// 5
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("retrievingPlatformInstance")); //$NON-NLS-1$
		Resource res = getSelectedObject().eResource();
		if (loadPlatformInstances(space)) {
			worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("retrievedPlatformInstance")); //$NON-NLS-1$
			//
			// 6
			//
			subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("retrievingIS")); //$NON-NLS-1$
			ConstraintSet is = space.getIntersectionSet();
			is.getIntersection();
			worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("retrievedIS")); //$NON-NLS-1$
			//
			// 7
			//
			subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.determiningInValidCS")); //$NON-NLS-1$
			IEObjectValidator validator = new PlatformKitEObjectValidator(space);
			Registry.INSTANCE.put(res, validator);
			worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.determinedInValidCS")); //$NON-NLS-1$
			//
			// 8
			//
			subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.profiling")); //$NON-NLS-1$
			updateAllObjects(res);
			worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.profiled")); //$NON-NLS-1$
		} else {
			worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.noPlatformInstanceFound")); //$NON-NLS-1$
			//
			// 6
			//
			subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.removingPlatformInstance")); //$NON-NLS-1$
			worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.removedPlatformInstance")); //$NON-NLS-1$
			//
			// 7
			//
			subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.determiningInValidCS")); //$NON-NLS-1$
			Registry.INSTANCE.remove(res);
			worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.determinedInValidCS")); //$NON-NLS-1$
			//
			// 8
			//
			subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.profiling")); //$NON-NLS-1$
			updateAllObjects(res);
			worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.profiled")); //$NON-NLS-1$
		}
	}

	/**
	 * Finds the PlatformKit model URI via the meta-model of the selected object.
	 * @return The PlatformKit model URI.
	 */
	protected URI findPlatformKitURI() {
		EObject object = getSelectedObject();
		PlatformkitLogger.logger.info(object.eAdapters().toString());
		Resource res = object.eResource();
		EObject root = (EObject) res.getContents().get(0);
		Resource rootRes = root.eClass().eResource();
		URI resURI = rootRes.getURI();
		URI platformkitURI = resURI.trimFileExtension().appendFileExtension("platformkit"); //$NON-NLS-1$
		PlatformkitLogger.logger.info(String.format(
				PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.pkUriIs"), 
				platformkitURI)); //$NON-NLS-1$
		return platformkitURI;
	}

	/**
	 * Loads the platform specification ontologies specified by
	 * {@link #getPlatformInstanceSources()}.
	 * @param space
	 * @return True if any platform specification was loaded.
	 * @throws CoreException
	 * @throws OntException 
	 */
	protected boolean loadPlatformInstances(ConstraintSpace space) throws CoreException, OntException {
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
				PlatformkitLogger.logger.info(String.format(
						PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.createdWrapperFor"), 
						object)); //$NON-NLS-1$
				object.eAdapters().set(i, wrapper);
				adapter = wrapper;
			}
			if (adapter instanceof PlatformKitItemProviderAdapter) {
				PlatformKitItemProviderAdapter pkAdapter = (PlatformKitItemProviderAdapter) adapter;
				if ((pkAdapter.getValidator() != validator)) {
					pkAdapter.setValidator(validator);
					PlatformkitLogger.logger.info(String.format(
							PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.attachedNewValidatorTo"), 
							pkAdapter)); //$NON-NLS-1$
				}
			}
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
			PlatformkitLogger.logger.info(String.format(
					PlatformkitEditorPlugin.getPlugin().getString("ProfileJob.registeredNewEValidatorFor"), 
					pack.getNsURI())); //$NON-NLS-1$
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
