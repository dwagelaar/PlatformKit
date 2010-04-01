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
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.editor.preferences.PreferenceInitializer;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.util.OntException;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.util.PlatformKitException;

/**
 * Operation to validate the options in a PlatformKit constraint space model
 * against platform instances.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class ValidateJob extends ConstraintSpaceJob {

	private String report;

	/**
	 * Creates a new {@link ValidateJob}.
	 */
	public ValidateJob() {
		super(PlatformkitEditorPlugin.getPlugin().getString("ValidateJob.name")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.jobs.ProgressMonitorJob#runAction(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void runAction(IProgressMonitor monitor) throws Exception {
		beginTask(monitor, getName(), 7);
		ConstraintSpace space = getSpace();
		IOntologies ont = space.getKnowledgeBase();
		//
		// 1
		//
		if (ont == null) {
			subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("loadingSourceOnt")); //$NON-NLS-1$
			ont = PreferenceInitializer.getPreferredOntologyFactory().createIOntologies();
			space.setKnowledgeBase(ont);
			if (!space.init(true)) {
				throw new PlatformKitException(
						PlatformkitEditorPlugin.getPlugin().getString("ontNotPreclassified")); //$NON-NLS-1$
			}
			worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("loadedSourceOnt")); //$NON-NLS-1$
		} else {
			subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("usingPreloadedOnt")); //$NON-NLS-1$
			worked(monitor, null);
		}
		//
		// 2
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("attachingDlReasoner")); //$NON-NLS-1$
		attachDLReasoner(monitor, ont);
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("attachedDlReasoner")); //$NON-NLS-1$
		//
		// 3
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("retrievingPlatformInstance")); //$NON-NLS-1$
		if (!loadPlatformInstances()) {
			throw new IllegalArgumentException("Must specify at least one platform instance for validation");
		}
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("retrievedPlatformInstance")); //$NON-NLS-1$
		//
		// 4
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("retrievingIS")); //$NON-NLS-1$
		ConstraintSet is = space.getIntersectionSet();
		is.getIntersection();
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("retrievedIS")); //$NON-NLS-1$
		//
		// 5
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ValidateJob.determiningValidCS")); //$NON-NLS-1$
		List<ConstraintSet> valid = space.getValid();
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ValidateJob.determinedValidCS")); //$NON-NLS-1$
		//
		// 6
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("detachingReasoner")); //$NON-NLS-1$
		ont.detachReasoner();
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("detachedReasoner")); //$NON-NLS-1$
		//
		// 7
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("ValidateJob.creatingReport")); //$NON-NLS-1$
		setReport(createReport(valid));
		ont.unloadInstances();
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("ValidateJob.createdReport")); //$NON-NLS-1$
	}

	/**
	 * Loads the platform specification ontologies specified by
	 * {@link #getPlatformInstanceSources()}.
	 * @return True if any platform specification was loaded.
	 * @throws CoreException
	 * @throws OntException 
	 */
	protected boolean loadPlatformInstances() throws CoreException, OntException {
		boolean loaded = false;
		IOntologies ont = getSpace().getKnowledgeBase();
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
	 * @param valid
	 * @return a report on the valid options.
	 * @throws IllegalArgumentException
	 */
	protected String createReport(List<ConstraintSet> valid)
	throws IllegalArgumentException {
		StringBuffer msg = new StringBuffer();
		msg.append(PlatformkitEditorPlugin.getPlugin().getString("ValidateJob.validCS")); //$NON-NLS-1$
		msg.append('\n'); msg.append('\n');
		Assert.isNotNull(valid);
		for (int i = 0; i < valid.size(); i++) {
			ConstraintSet list = valid.get(i);
			msg.append(" - "); //$NON-NLS-1$
			msg.append(list.getName());
			msg.append('\n');
		}
		return msg.toString();
	}

	/**
	 * @return the report
	 */
	public String getReport() {
		return report;
	}

	/**
	 * @param report the report to set
	 */
	protected void setReport(String report) {
		this.report = report;
	}

}
