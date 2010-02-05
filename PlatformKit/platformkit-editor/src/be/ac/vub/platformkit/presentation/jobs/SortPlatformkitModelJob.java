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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.editor.preferences.PreferenceInitializer;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.util.PlatformKitException;

/**
 * Operation for sorting the constraint sets in a PlatformKit model.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class SortPlatformkitModelJob extends ConstraintSpaceJob {

	/**
	 * Most-specific first mode.
	 */
	public static final int MOST_SPECIFIC = 0;
	/**
	 * Least-specific first mode.
	 */
	public static final int LEAST_SPECIFIC = 1;

	private int mode;
	private String title;

	/**
	 * Creates a new {@link SortPlatformkitModelJob}.
	 * @param mode {@link #MOST_SPECIFIC} or {@link #LEAST_SPECIFIC}.
	 * @throws IllegalArgumentException 
	 */
	public SortPlatformkitModelJob(int mode) throws IllegalArgumentException {
		super(PlatformkitEditorPlugin.getPlugin().getString("SortPlatformkitModelJob.name")); //$NON-NLS-1$
		setMode(mode);
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.jobs.ProgressMonitorJob#runAction(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void runAction(IProgressMonitor monitor) throws Exception {
		beginTask(monitor, PlatformkitEditorPlugin.getPlugin().getString(
				"SortPlatformkitModelJob.beginTask", 
				new Object[]{getName(), getTitle()}), 6); //$NON-NLS-1$
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
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("SortPlatformkitModelJob.attachingTransReasoner")); //$NON-NLS-1$
		ont.attachTransitiveReasoner();
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("SortPlatformkitModelJob.attachedTransReasoner")); //$NON-NLS-1$
		//
		// 3
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("retrievingIS")); //$NON-NLS-1$
		ConstraintSet is = space.getIntersectionSet();
		is.getIntersection();
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("retrievedIS")); //$NON-NLS-1$
		//
		// 4
		//
		EList<ConstraintSet> specific;
		switch (getMode()) {
		case MOST_SPECIFIC:
			subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("SortPlatformkitModelJob.determiningMostSpecific")); //$NON-NLS-1$
			specific = space.getMostSpecific(false);
			break;
		case LEAST_SPECIFIC:
			subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("SortPlatformkitModelJob.determiningLeastSpecific")); //$NON-NLS-1$
			specific = space.getLeastSpecific(false);
			break;
		default: throw new IllegalArgumentException(PlatformkitEditorPlugin.getPlugin().getString("SortPlatformkitModelJob.invalidMode")); //$NON-NLS-1$
		}
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("SortPlatformkitModelJob.determinedLeastOrMost")); //$NON-NLS-1$
		//
		// 5
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("detachingReasoner")); //$NON-NLS-1$
		ont.detachReasoner();
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("detachedReasoner")); //$NON-NLS-1$
		//
		// 6
		//
		subTask(monitor, PlatformkitEditorPlugin.getPlugin().getString("SortPlatformkitModelJob.sortingCS")); //$NON-NLS-1$
		//cannot run in compound command
		getEditingDomain().getCommandStack().execute(createRemoveConstraintSetCommand(space.getConstraintSet()));
		getEditingDomain().getCommandStack().execute(createAddConstraintSetCommand(specific));
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("SortPlatformkitModelJob.sortedCS")); //$NON-NLS-1$
	}

	/**
	 * @return {@link #MOST_SPECIFIC} or {@link #LEAST_SPECIFIC}.
	 */
	public int getMode() {
		return mode;
	}

	/**
	 * @param mode {@link #MOST_SPECIFIC} or {@link #LEAST_SPECIFIC}.
	 * @throws IllegalArgumentException
	 */
	public void setMode(int mode) throws IllegalArgumentException {
		this.mode = mode;
		switch (mode) {
		case MOST_SPECIFIC:
			setTitle(PlatformkitEditorPlugin.getPlugin().getString("SortPlatformkitModelJob.mostSpecificFirst")); //$NON-NLS-1$
			break;
		case LEAST_SPECIFIC:
			setTitle(PlatformkitEditorPlugin.getPlugin().getString("SortPlatformkitModelJob.leastSpecificFirst")); //$NON-NLS-1$
			break;
		default: throw new IllegalArgumentException(
				PlatformkitEditorPlugin.getPlugin().getString("SortPlatformkitModelJob.invalidMode")); //$NON-NLS-1$
		}
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	protected void setTitle(String title) {
		this.title = title;
	}

}
