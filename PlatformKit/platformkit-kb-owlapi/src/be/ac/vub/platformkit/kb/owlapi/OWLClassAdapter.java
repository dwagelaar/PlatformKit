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
package be.ac.vub.platformkit.kb.owlapi;

import java.util.logging.Level;

import junit.framework.Assert;

import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.model.OWLClass;

import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.logging.PlatformkitLogger;

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
		Assert.assertNotNull(model);
		Assert.assertNotNull(ontologies);
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
			try {
				return reasoner.isEquivalentClass(((OWLClassAdapter)c).getModel(), getModel());
			} catch (OWLReasonerException e) {
				PlatformkitLogger.logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
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
			try {
				return reasoner.isSubClassOf(((OWLClassAdapter)c).getModel(), getModel());
			} catch (OWLReasonerException e) {
				PlatformkitLogger.logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
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
			try {
				return reasoner.isSubClassOf(getModel(), ((OWLClassAdapter)c).getModel());
			} catch (OWLReasonerException e) {
				PlatformkitLogger.logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
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
			try {
				return !reasoner.getIndividuals(getModel(), false).isEmpty();
			} catch (OWLReasonerException e) {
				PlatformkitLogger.logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
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
