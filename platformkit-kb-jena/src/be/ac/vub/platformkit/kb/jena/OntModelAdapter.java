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

import java.util.Iterator;

import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntModel;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.RDFList;

/**
 * {@link IOntModel} adapter for {@link OntModel}. 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class OntModelAdapter implements IOntModel {

	/**
	 * Unwraps the {@link OntClass}es from the {@link IOntClass}es.
	 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
	 */
	private static class OntClassIterator implements Iterator<OntClass> {
		private Iterator<IOntClass> inner;

		/**
		 * Creates a new {@link OntClassIterator}.
		 * @param inner
		 */
		public OntClassIterator(Iterator<IOntClass> inner) {
			assert inner != null;
			this.inner = inner;
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return inner.hasNext();
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public OntClass next() {
			return ((OntClassAdapter) inner.next()).getModel();
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			inner.remove();
		}

	}

	private OntModel model;

	/**
	 * Creates a new {@link OntModelAdapter}.
	 * @param model
	 */
	public OntModelAdapter(OntModel model) {
		assert model != null;
		this.setModel(model);
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
	 * @see be.ac.vub.platformkit.kb.IOntModel#getOntClass(java.lang.String)
	 */
	public IOntClass getOntClass(String uri){
		OntClass c = getModel().getOntClass(uri);
		if (c != null) {
			return new OntClassAdapter(c);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#createIntersectionClass(java.lang.String, java.util.Iterator)
	 */
	public IOntClass createIntersectionClass(String uri, Iterator<IOntClass> members) {
		final RDFList constraints = getModel().createList(new OntClassIterator(members));
		return new OntClassAdapter(getModel().createIntersectionClass(uri, constraints));
	}

	/**
	 * @param model the model to set
	 */
	protected void setModel(OntModel model) {
		this.model = model;
	}

	/**
	 * @return the model
	 */
	protected OntModel getModel() {
		return model;
	}

}
