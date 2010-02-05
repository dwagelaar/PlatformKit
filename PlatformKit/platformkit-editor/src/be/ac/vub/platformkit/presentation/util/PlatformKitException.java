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
package be.ac.vub.platformkit.presentation.util;

/**
 * Class for PlatformKit-related exceptions.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformKitException extends Exception {

	private static final long serialVersionUID = -1161236491030369161L;

	/**
	 * Creates a new {@link PlatformKitException}.
	 */
	public PlatformKitException() {
		super();
	}

	/**
	 * Creates a new {@link PlatformKitException}.
	 * @param arg0
	 */
	public PlatformKitException(String arg0) {
		super(arg0);
	}

	/**
	 * Creates a new {@link PlatformKitException}.
	 * @param arg0
	 */
	public PlatformKitException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * Creates a new {@link PlatformKitException}.
	 * @param arg0
	 * @param arg1
	 */
	public PlatformKitException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
