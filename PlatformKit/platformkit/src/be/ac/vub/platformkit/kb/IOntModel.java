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
package be.ac.vub.platformkit.kb;

import java.io.OutputStream;
import java.util.Iterator;

import be.ac.vub.platformkit.kb.util.OntException;

/**
 * Interface for ontology models
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public interface IOntModel {

	/**
	 * <p>
	 * Answer a resource that represents a class description node in this model. If a resource
	 * with the given uri exists in the model, and can be viewed as an IOntClass, return the
	 * IOntClass facet, otherwise return null.
	 * </p>
	 * @param uri The uri for the class node, or null for an anonymous class.
	 * @return An IOntClass resource or null.
	 * @throws OntException 
	 */
	public IOntClass getOntClass(String uri) throws OntException;

	/**
	 * <p>Answer a resource representing the class that is the intersection of the given list of class descriptions.</p>
	 * @param uri The URI of the new intersection class, or null for an anonymous class description.
	 * @param members A list of resources denoting the classes that comprise the intersection
	 * @return An intersection class description
	 * @throws OntException if any backing ontology classes for members cannot be found
	 */
	public IOntClass createIntersectionClass(String uri, Iterator<IOntClass> members) throws OntException;

	/**
	 * Saves this ontology to out.
	 * @param out
	 * @throws OntException
	 */
	public void save(OutputStream out) throws OntException;

	/**
	 * @return the namespace URI of this ontology
	 */
	public String getNsURI();

	/**
	 * Creates an ontology class that is equivalent to the object property restriction to the given range on the given property.
	 * Updates any existing restriction class with the given uri to:
	 * <ul>
	 * <li>include any new property restriction ranges;
	 * <li>weaken down existing property restriction ranges to the given range, if it is a wider range.
	 * </ul>
	 * Requires transitive (or better) reasoner.
	 * @see IOntologies#attachTransitiveReasoner()
	 * @param uri the ontology class URI
	 * @param superClass the ontology superclass, if any
	 * @param propertyURI the URI of the property to restrict
	 * @param range the restriction range classes
	 * @return the property restriction ontology class
	 * @throws OntException
	 */
	public IOntClass createSomeRestriction(String uri, IOntClass superClass, 
			String propertyURI, Iterator<IOntClass> range) throws OntException;

	/**
	 * Creates an ontology class that is equivalent to the "MinInclusive" data property restriction to the given value.
	 * Updates any existing restriction class with the given uri.
	 * @param uri the ontology class URI
	 * @param superClass the ontology superclass, if any
	 * @param propertyURI the URI of the property to restrict
	 * @param datatypeURI the URI of the data type
	 * @param value the data value of the given data type
	 * @return the property restriction ontology class
	 * @throws OntException 
	 */
	public IOntClass createMinInclusiveRestriction(String uri, IOntClass superClass, 
			String propertyURI, String datatypeURI, String value) throws OntException;

	/**
	 * Creates an ontology class that is equivalent to the "HasValue" data property restriction to the given value.
	 * Updates any existing restriction class with the given uri.
	 * @param uri the ontology class URI
	 * @param superClass the ontology superclass, if any
	 * @param propertyURI the URI of the property to restrict
	 * @param datatypeURI the URI of the data type
	 * @param value the data value of the given data type
	 * @return the property restriction ontology class
	 * @throws OntException
	 */
	public IOntClass createHasValueRestriction(String uri, IOntClass superClass, 
			String propertyURI, String datatypeURI, String value) throws OntException;

}
