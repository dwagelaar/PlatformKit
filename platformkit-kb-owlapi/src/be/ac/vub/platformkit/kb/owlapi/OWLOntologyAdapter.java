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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owl.model.AddAxiom;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLObjectIntersectionOf;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChangeException;
import org.semanticweb.owl.model.OWLOntologyManager;

import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntModel;
import be.ac.vub.platformkit.kb.util.OntException;

/**
 * {@link IOntModel} adapter for {@link OWLOntology}.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class OWLOntologyAdapter implements IOntModel {

	private OWLOntology model;
	private OWLAPIOntologies ontologies;

	/**
	 * Creates a new {@link OWLOntologyAdapter}.
	 * @param model
	 * @param ontologies
	 */
	public OWLOntologyAdapter(OWLOntology model, OWLAPIOntologies ontologies) {
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
	 * @see be.ac.vub.platformkit.kb.IOntModel#createIntersectionClass(java.lang.String, java.util.Iterator)
	 */
	public IOntClass createIntersectionClass(String uri,
			Iterator<IOntClass> members) throws OntException {
		final OWLOntologyManager mgr = getOntologies().getMgr();
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		try {
			URI classURI = new URI(uri);
			OWLClass intsecClass = factory.getOWLClass(classURI);
			Set<OWLClass> operands = new HashSet<OWLClass>();
			while (members.hasNext()) {
				OWLClassAdapter c = (OWLClassAdapter) members.next();
				assert c != null;
				operands.add(c.getModel());
			}
			OWLObjectIntersectionOf intersection = factory.getOWLObjectIntersectionOf(operands);
			OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(intsecClass, intersection);
			AddAxiom addAxiom = new AddAxiom(getModel(), axiom);
			mgr.applyChange(addAxiom);
			return new OWLClassAdapter(intsecClass, getOntologies());
		} catch (URISyntaxException e) {
			throw new OntException(e);
		} catch (OWLOntologyChangeException e) {
			throw new OntException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#getOntClass(java.lang.String)
	 */
	public IOntClass getOntClass(String uri) throws OntException {
		final OWLAPIOntologies ontologies = getOntologies();
		final OWLOntologyManager mgr = ontologies.getMgr();
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		try {
			URI classURI = new URI(uri);
			if (getModel().containsClassReference(classURI)) {
				OWLClass owlClass = factory.getOWLClass(classURI);
				return new OWLClassAdapter(owlClass, ontologies);
			}
		} catch (URISyntaxException e) {
			throw new OntException(e);
		}
		return null;
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

	/**
	 * @param model the model to set
	 */
	protected void setModel(OWLOntology model) {
		this.model = model;
	}

	/**
	 * @return the model
	 */
	protected OWLOntology getModel() {
		return model;
	}

}
