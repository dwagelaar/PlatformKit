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
package be.ac.vub.platformkit.kb.owlapi3;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import be.ac.vub.platformkit.kb.IOntClass;

/**
 * {@link IOntClass} adapter for {@link OWLClass}.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class OWLClassAdapter implements IOntClass {

	private OWLClass model;
	private OWLAPIOntologies ontologies;

	/**
	 * Creates a new {@link OWLClassAdapter}.
	 * @param model
	 * @param ontologies
	 */
	public OWLClassAdapter(OWLClass model, OWLAPIOntologies ontologies) {
		assert model != null;
		assert ontologies != null;
		this.setModel(model);
		this.setOntologies(ontologies);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getModel().toString();
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntClass#hasEquivalentClass(be.ac.vub.platformkit.kb.IOntClass)
	 */
	public boolean hasEquivalentClass(IOntClass c) throws ClassCastException {
		final OWLReasoner reasoner = getOntologies().getReasoner();
		if (reasoner != null) {
			final Node<OWLClass> ecs = reasoner.getEquivalentClasses(getModel());
			return ecs.contains(((OWLClassAdapter)c).getModel());
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntClass#hasSubClass(be.ac.vub.platformkit.kb.IOntClass)
	 */
	public boolean hasSubClass(IOntClass c) throws ClassCastException {
		final OWLReasoner reasoner = getOntologies().getReasoner();
		if (reasoner != null) {
			final NodeSet<OWLClass> scs = reasoner.getSubClasses(getModel(), false);
			return scs.containsEntity(((OWLClassAdapter)c).getModel());
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntClass#hasSuperClass(be.ac.vub.platformkit.kb.IOntClass)
	 */
	public boolean hasSuperClass(IOntClass c) throws ClassCastException {
		final OWLReasoner reasoner = getOntologies().getReasoner();
		if (reasoner != null) {
			final NodeSet<OWLClass> scs = reasoner.getSuperClasses(getModel(), false);
			return scs.containsEntity(((OWLClassAdapter)c).getModel());
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntClass#hasInstances()
	 */
	public boolean hasInstances() {
		final OWLReasoner reasoner = getOntologies().getReasoner();
		if (reasoner != null) {
			return !reasoner.getInstances(getModel(), false).isEmpty();
		}
		return false;
	}

	/**
	 * @param model the model to set
	 */
	protected void setModel(OWLClass model) {
		this.model = model;
	}

	/**
	 * @return the model
	 */
	protected OWLClass getModel() {
		return model;
	}

	/**
	 * @param ontologies the ontologies to set
	 */
	protected void setOntologies(OWLAPIOntologies ontologies) {
		this.ontologies = ontologies;
	}

	/**
	 * @return the ontologies
	 */
	protected OWLAPIOntologies getOntologies() {
		return ontologies;
	}

}
