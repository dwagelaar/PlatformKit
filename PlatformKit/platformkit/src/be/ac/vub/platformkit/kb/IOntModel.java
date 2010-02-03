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

import java.util.Iterator;

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
	 */
	public IOntClass getOntClass(String uri);

	/**
	 * <p>Answer a resource representing the class that is the intersection of the given list of class descriptions.</p>
	 * @param uri The URI of the new intersection class, or null for an anonymous class description.
	 * @param members A list of resources denoting the classes that comprise the intersection
	 * @return An intersection class description
	 */
	public IOntClass createIntersectionClass(String uri, Iterator<IOntClass> members);

}
