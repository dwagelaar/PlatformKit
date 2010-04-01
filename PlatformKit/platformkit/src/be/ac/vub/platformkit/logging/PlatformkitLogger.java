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
package be.ac.vub.platformkit.logging;

import java.util.logging.Logger;

/**
 * Centralised logging functionality.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformkitLogger {

	public static final String LOGGER = "be.ac.vub.platformkit"; //$NON-NLS-1$

	public static Logger logger = Logger.getLogger(LOGGER);

	/**
	 * Not meant to be instantiated.
	 */
	private PlatformkitLogger() {
		super();
	}

}
