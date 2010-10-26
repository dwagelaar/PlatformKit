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
package be.ac.vub.platformkit.kb.owlapi3;

import java.io.IOException;

import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.IOntologiesFactory;

/**
 * {@link IOntologiesFactory} for {@link OWLAPIOntologies}.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class OWLAPIOntologiesFactory implements IOntologiesFactory {

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologiesFactory#createIOntologies()
	 */
	public IOntologies createIOntologies() throws IOException {
		return new OWLAPIOntologies();
	}

}
