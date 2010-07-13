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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.editor.preferences.PreferenceInitializer;
import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.util.OntException;
import be.ac.vub.platformkit.logging.PlatformkitLogger;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * Operation for pre-classifying the taxonomy of ontology classes for a
 * given PlatformKit {@link ConstraintSpace} model.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class ClassifyTaxonomyJob extends ConstraintSpaceJob {

	private static final int OUTPUTSIZE = 512*1024; // 512 KB

	/**
	 * Creates a new {@link ClassifyTaxonomyJob}.
	 */
	public ClassifyTaxonomyJob() {
		super(PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.name")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.jobs.ProgressMonitorJob#runAction(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void runAction(IProgressMonitor monitor) throws Exception {
		beginTask(monitor, getName(), 9);
		//
		// 1
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("loadingSourceOnt")); //$NON-NLS-1$
		// Don't use existing knowledgebase, since it may be pre-classified
		IOntologies ont = PreferenceInitializer.getPreferredOntologyFactory().createIOntologies();
		ConstraintSpace space = getSpace();
		space.setKnowledgeBase(ont);
		space.init(false);
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("loadedSourceOnt")); //$NON-NLS-1$
		//
		// 2
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.creatingIntersectUnion")); //$NON-NLS-1$
		//create intersection set before reasoning
		ConstraintSet is = space.getIntersectionSet();
		is.getIntersection();
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.createdIntersectUnion")); //$NON-NLS-1$
		//
		// 3
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("attachingDlReasoner")); //$NON-NLS-1$
		attachDLReasoner(monitor, ont);
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("attachedDlReasoner")); //$NON-NLS-1$
		//
		// 4
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.checkingConsist")); //$NON-NLS-1$
		ont.checkConsistency();
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.checkedConsist")); //$NON-NLS-1$
		//
		// 5
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.buildingHierarchy")); //$NON-NLS-1$
		buildHierarchyMap(monitor, ont);
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.builtHierarchy")); //$NON-NLS-1$
		//
		// 6
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("detachingReasoner")); //$NON-NLS-1$
		ont.detachReasoner();
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("detachedReasoner")); //$NON-NLS-1$
		//
		// 7
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.pruningHierarchy")); //$NON-NLS-1$
		pruneHierarchyMap(monitor, ont);
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.prunedHierarchy")); //$NON-NLS-1$
		//
		// 8
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.updatingHierarchy")); //$NON-NLS-1$
		updateHierarchy(monitor, ont);
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.updatedHierarchy")); //$NON-NLS-1$
		//
		// 9
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.writingOntology")); //$NON-NLS-1$
		writeOntology(ont, space.eResource().getURI());
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.writtenOntology")); //$NON-NLS-1$
	}

	/**
	 * Operation for building a hierarchy map for an ontology.
	 * @param monitor
	 * @param ont
	 * @throws OntException 
	 */
	protected void buildHierarchyMap(IProgressMonitor monitor, IOntologies ont) throws OntException {
		List<IOntClass> namedClasses = ont.getLocalNamedClasses();
		for (int i = 0; i < namedClasses.size(); i++) {
			try {
				IOntClass c = namedClasses.get(i);
				ont.buildHierarchyMap(c);
			} catch (OntException nfe) {
				PlatformkitEditorPlugin.INSTANCE.log(
						nfe.getMessage(), IStatus.WARNING, nfe);
			} finally {
				checkCanceled(monitor);
			}
		}
	}

	/**
	 * Operation for pruning the generated hierarchy map for an ontology.
	 * @param monitor
	 * @param ont
	 * @throws OntException 
	 */
	protected void pruneHierarchyMap(IProgressMonitor monitor, IOntologies ont) throws OntException {
		List<IOntClass> namedClasses = ont.getLocalNamedClasses();
		for (int i = 0; i < namedClasses.size(); i++) {
			try {
				IOntClass c = namedClasses.get(i);
				ont.pruneHierarchyMap(c);
			} catch (OntException nfe) {
				PlatformkitEditorPlugin.INSTANCE.log(
						nfe.getMessage(), IStatus.WARNING, nfe);
			} finally {
				checkCanceled(monitor);
			}
		}
	}

	/**
	 * Operation for updating the asserted class hierarchy for an ontology.
	 * @param monitor
	 * @param ont
	 * @throws OntException 
	 */
	protected void updateHierarchy(IProgressMonitor monitor, IOntologies ont) throws OntException {
		List<IOntClass> namedClasses = ont.getLocalNamedClasses();
		for (int i = 0; i < namedClasses.size(); i++) {
			try {
				IOntClass c = namedClasses.get(i);
				ont.updateHierarchy(c);
			} catch (OntException nfe) {
				PlatformkitEditorPlugin.INSTANCE.log(
						nfe.getMessage(), IStatus.WARNING, nfe);
			} finally {
				checkCanceled(monitor);
			}
		}
	}

	/**
	 * Writes the ontology for the given resource URI.
	 * @param ont
	 * @param uri
	 * @throws CoreException
	 * @throws IOException
	 * @throws OntException 
	 */
	protected void writeOntology(IOntologies ont, URI uri)
	throws CoreException, IOException, OntException {
		IPath platformkitPath = new Path(uri.toPlatformString(true));
		Assert.isNotNull(platformkitPath);
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(platformkitPath);
		Assert.isNotNull(file);
		Path path = new Path(
				file.getFullPath().removeFileExtension().
				addFileExtension("inferred").
				addFileExtension("owl").lastSegment()); //$NON-NLS-1$ //$NON-NLS-2$
		IContainer cont = file.getParent();
		IFile dest = cont.getFile(path);
		PlatformkitLogger.logger.info(String.format(
				PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.writingOntologyTo"), 
				dest.getLocation())); //$NON-NLS-1$
		ByteArrayOutputStream output = new ByteArrayOutputStream(OUTPUTSIZE);
		ont.saveOntology(output);
		if (dest.exists()) {
			dest.setContents(new ByteArrayInputStream(output.toByteArray()), 
					true, true, null);
		} else {
			dest.create(new ByteArrayInputStream(output.toByteArray()), 
					true, null);
		}
		output.close();
		PlatformkitLogger.logger.info(String.format(
				PlatformkitEditorPlugin.getPlugin().getString("ClassifyTaxonomyJob.writtenOntologyTo"), 
				dest.getLocation())); //$NON-NLS-1$
	}

}
