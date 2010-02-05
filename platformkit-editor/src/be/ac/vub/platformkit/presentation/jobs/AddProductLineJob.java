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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * Operation to add product line meta-classes, described in an annotated DSL,
 * to the PlatformKit constraint space model.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class AddProductLineJob extends AddConstraintSetsJob {

	/**
	 * Creates a new {@link AddProductLineJob}.
	 */
	public AddProductLineJob() {
		super(PlatformkitEditorPlugin.getPlugin().getString("AddProductLineJob.name")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.presentation.jobs.AddConstraintSetsJob#addConstraintSet(org.eclipse.emf.ecore.resource.Resource, org.eclipse.emf.common.util.EList)
	 */
	@Override
	protected void addConstraintSet(Resource source, EList<ConstraintSet> constraintSets) {
		TreeIterator<EObject> contents = source.getAllContents();
		while (contents.hasNext()) {
			EObject current = contents.next();
			addMetaObjectConstraintSet(current, constraintSets);
		}
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.presentation.jobs.AddConstraintSetsJob#addOntologies(org.eclipse.emf.ecore.resource.Resource, org.eclipse.emf.common.util.EList)
	 */
	@Override
	protected void addOntologies(Resource source, EList<String> ontologies) {
		TreeIterator<EObject> contents = source.getAllContents();
		while (contents.hasNext()) {
			EObject current = contents.next();
			addMetaObjectOntologies(current, ontologies);
		}
	}

	/**
	 * Searches the metaobject for platform constraint annotations and adds a ConstraintSet object to the list.
	 * @param object The metaobject.
	 * @param constraintSets The list of ontologies to add to.
	 */
	protected void addMetaObjectConstraintSet(EObject object, EList<ConstraintSet> constraintSets) {
		ConstraintSet set = createMetaObjectConstraintSet(object);
		if (set != null) {
			constraintSets.add(set);
		}
	}

	/**
	 * @return A new ConstraintSet that reflects the given metaobject, provided that the metaobject actually has constraint annotations, null otherwise.
	 * @param object
	 */
	protected ConstraintSet createMetaObjectConstraintSet(EObject object) {
		EList<EAnnotation> annotations = getEAnnotations(object);
		if (annotations == null) {
			return null;
		}
		Set<String> constraints = new HashSet<String>();
		ConstraintSet set = factory.createConstraintSet();
		Object target = getIDFrom(object);
		if (target != null) {
			set.setName(target.toString());
		}
		addConstraints(annotations, constraints, set);
		if (set.getConstraint().isEmpty()) {
			return null;
		} else {
			return set;
		}
	}

}
