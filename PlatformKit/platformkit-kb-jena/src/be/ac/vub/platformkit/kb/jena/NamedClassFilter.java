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

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.util.iterator.Filter;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * Accepts all named OntClass objects which are not OWL:Thing or OWL:Nothing
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class NamedClassFilter extends Filter<OntClass> {

	/**
	 * @see Filter#accept(java.lang.Object)
	 */
	public boolean accept(OntClass o) {
		return isNamedClass((OntClass) o);
	}

	/**
	 * @param c
	 * @return True if c is a named class.
	 */
	public final static boolean isNamedClass(OntClass c) {
		final String uri = c.getURI();
		if (uri == null) { return false; }
		if (uri.equals(OWL.Thing.getURI())) { return false; }
		if (uri.equals(OWL.Nothing.getURI())) { return false; }
		return true;
	}

}
