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

/**
 * Interface for ontology repository factories 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public interface IOntologiesFactory {

	public static final String KB_EXT_POINT = "be.ac.vub.platformkit.knowledgebase"; //$NON-NLS-1$

	/**
	 * Creates a new {@link IOntologies} object.
	 * @return The created {@link IOntologies} object.
	 * @throws IOException
	 */
	public abstract IOntologies createIOntologies() throws IOException;

}
