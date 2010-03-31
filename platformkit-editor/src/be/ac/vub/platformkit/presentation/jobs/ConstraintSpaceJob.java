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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.preference.IPreferenceStore;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.PlatformkitPackage;
import be.ac.vub.platformkit.editor.preferences.PreferenceConstants;
import be.ac.vub.platformkit.jobs.ProgressMonitorJob;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.util.OntException;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * General base class for all jobs targeting a {@link ConstraintSpace}
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public abstract class ConstraintSpaceJob extends ProgressMonitorJob {

	private ConstraintSpace space;
	private Object[] platformInstanceSources;
	private EditingDomain editingDomain;

	/**
	 * Creates a new {@link ConstraintSpaceJob}.
	 * @param name
	 */
	public ConstraintSpaceJob(String name) {
		super(name);
	}

	/**
	 * Attaches either a DIG reasoner or the built-in Pellet reasoner.
	 * @param monitor
	 * @param ont
	 * @throws OntException 
	 */
	protected void attachDLReasoner(IProgressMonitor monitor, IOntologies ont) throws OntException {
		IPreferenceStore store = PlatformkitEditorPlugin.getPlugin()
		.getPreferenceStore();
		String reasoner = store.getString(PreferenceConstants.P_REASONER);
		if (PreferenceConstants.P_DIG.equals(reasoner)) {
			String url = store.getString(PreferenceConstants.P_DIG_URL);
			ont.setReasonerUrl(url);
			ont.attachDIGReasoner();
		} else {
			ont.attachPelletReasoner();
		}
	}

	/**
	 * @param constraintSets
	 * @return a Command that adds the constraint sets to the constraint space.
	 */
	protected Command createAddConstraintSetCommand(EList<ConstraintSet> constraintSets) {
		Assert.isNotNull(constraintSets);
		AddCommand cmd = new AddCommand(
				getEditingDomain(), 
				getSpace(), 
				PlatformkitPackage.eINSTANCE.getConstraintSpace_ConstraintSet(), 
				constraintSets);
		return cmd;
	}

	/**
	 * @param ontologies
	 * @return a Command that adds the ontologies to the constraint space.
	 */
	protected Command createAddOntologyCommand(EList<String> ontologies) {
		Assert.isNotNull(ontologies);
		AddCommand cmd = new AddCommand(
				getEditingDomain(), 
				getSpace(), 
				PlatformkitPackage.eINSTANCE.getConstraintSpace_Ontology(), 
				ontologies);
		return cmd;
	}

	/**
	 * @param constraintSets
	 * @return a Command that removes the constraint sets from the constraint space.
	 */
	protected Command createRemoveConstraintSetCommand(EList<ConstraintSet> constraintSets) {
		Assert.isNotNull(constraintSets);
		RemoveCommand cmd = new RemoveCommand(
				getEditingDomain(), 
				getSpace(), 
				PlatformkitPackage.eINSTANCE.getConstraintSpace_ConstraintSet(), 
				constraintSets);
		return cmd;
	}

	/**
	 * @return the space
	 */
	public ConstraintSpace getSpace() {
		return space;
	}

	/**
	 * @param space the space to set
	 */
	public void setSpace(ConstraintSpace space) {
		this.space = space;
	}

	/**
	 * @return the platformInstanceSources
	 */
	public Object[] getPlatformInstanceSources() {
		return platformInstanceSources;
	}

	/**
	 * @param platformInstanceSources the platformInstanceSources to set
	 */
	public void setPlatformInstanceSources(Object[] platformInstanceSources) {
		this.platformInstanceSources = platformInstanceSources;
	}

	/**
	 * @return the editingDomain
	 */
	public EditingDomain getEditingDomain() {
		return editingDomain;
	}

	/**
	 * @param editingDomain the editingDomain to set
	 */
	public void setEditingDomain(EditingDomain editingDomain) {
		this.editingDomain = editingDomain;
	}

}
