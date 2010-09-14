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

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for Eclipse plugins that provide PlatformKit ontologies.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public interface IOntologyProvider {

	public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";

	public static final String INTEGER_URI = XSD_NS + "#integer";
	public static final String BOOLEAN_URI = XSD_NS + "#boolean";
	public static final String FLOAT_URI = XSD_NS + "#float";
	public static final String STRING_URI = XSD_NS + "#string";

	/**
	 * @return An array of {@link InputStream} objects that provide the ontology's contents.
	 * @throws IOException if one or more streams to ontologies could not be opened.
	 */
	InputStream[] getOntologies() throws IOException;

}
