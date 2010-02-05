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
import java.util.logging.Logger;

import junit.framework.Assert;

import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.model.OWLClass;

import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntologies;

/**
 * {@link IOntClass} adapter for {@link OWLClass}.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class OWLClassAdapter implements IOntClass {

	protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);
	protected OWLClass model;
	protected OWLAPIOntologies ontologies;

	/**
	 * Creates a new {@link OWLClassAdapter}.
	 * @param model
	 * @param ontologies
	 */
	public OWLClassAdapter(OWLClass model, OWLAPIOntologies ontologies) {
		Assert.assertNotNull(model);
		Assert.assertNotNull(ontologies);
		this.model = model;
		this.ontologies = ontologies;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return model.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntClass#hasEquivalentClass(be.ac.vub.platformkit.kb.IOntClass)
	 */
	public boolean hasEquivalentClass(IOntClass c) throws ClassCastException {
		final OWLReasoner reasoner = ontologies.reasoner;
		if (reasoner != null) {
			try {
				return reasoner.isEquivalentClass(((OWLClassAdapter)c).model, model);
			} catch (OWLReasonerException e) {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntClass#hasSubClass(be.ac.vub.platformkit.kb.IOntClass)
	 */
	public boolean hasSubClass(IOntClass c) throws ClassCastException {
		final OWLReasoner reasoner = ontologies.reasoner;
		if (reasoner != null) {
			try {
				return reasoner.isSubClassOf(((OWLClassAdapter)c).model, model);
			} catch (OWLReasonerException e) {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntClass#hasSuperClass(be.ac.vub.platformkit.kb.IOntClass)
	 */
	public boolean hasSuperClass(IOntClass c) throws ClassCastException {
		final OWLReasoner reasoner = ontologies.reasoner;
		if (reasoner != null) {
			try {
				return reasoner.isSubClassOf(model, ((OWLClassAdapter)c).model);
			} catch (OWLReasonerException e) {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntClass#hasInstances()
	 */
	public boolean hasInstances() {
		final OWLReasoner reasoner = ontologies.reasoner;
		if (reasoner != null) {
			try {
				return !reasoner.getIndividuals(model, false).isEmpty();
			} catch (OWLReasonerException e) {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
		return false;
	}

}
