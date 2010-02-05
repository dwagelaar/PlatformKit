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
package be.ac.vub.platformkit.kb.jena;

import junit.framework.Assert;
import be.ac.vub.platformkit.kb.IOntClass;

import com.hp.hpl.jena.ontology.OntClass;

/**
 * {@link IOntClass} adapter for {@link OntClass}.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class OntClassAdapter implements IOntClass {

	protected OntClass model;

	/**
	 * Creates a new {@link OntClassAdapter}.
	 * @param model
	 */
	public OntClassAdapter(OntClass model) {
		Assert.assertNotNull(model);
		this.model = model;
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
		return model.hasEquivalentClass(((OntClassAdapter)c).model);
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntClass#hasSubClass(be.ac.vub.platformkit.kb.IOntClass)
	 */
	public boolean hasSubClass(IOntClass c) throws ClassCastException {
		return model.hasSubClass(((OntClassAdapter)c).model);
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntClass#hasSuperClass(be.ac.vub.platformkit.kb.IOntClass)
	 */
	public boolean hasSuperClass(IOntClass c) throws ClassCastException {
		return model.hasSuperClass(((OntClassAdapter)c).model);
	}

	/**
	 * Adds c as an equivalent OWL class.
	 * @param c
	 * @throws ClassCastException
	 */
	public void addEquivalentClass(IOntClass c) throws ClassCastException {
		model.addEquivalentClass(((OntClassAdapter)c).model);
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntClass#hasInstances()
	 */
	public boolean hasInstances() {
		return model.listInstances().hasNext();
	}

}
