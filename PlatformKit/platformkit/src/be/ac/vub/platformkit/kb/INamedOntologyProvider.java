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

/**
 * Interface for Eclipse plugins that provide PlatformKit ontologies with names.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public interface INamedOntologyProvider extends IOntologyProvider {

	/**
	 * @return An array of {@link String} objects that provide the names of the ontologies.
	 * The returned array must have the same amount of elements as the array returned by
	 * {@link IOntologyProvider#getOntologies()}.
	 */
	String[] getOntologyNames();

}
