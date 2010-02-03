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
package be.ac.vub.platformkit.kb.util;

/**
 * PlatformKit ontology-related exception 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class OntException extends Exception {

	private static final long serialVersionUID = -1850996948968329662L;

	public OntException() {
		super();
	}

	public OntException(String message) {
		super(message);
	}

	public OntException(Throwable cause) {
		super(cause);
	}

	public OntException(String message, Throwable cause) {
		super(message, cause);
	}

}
