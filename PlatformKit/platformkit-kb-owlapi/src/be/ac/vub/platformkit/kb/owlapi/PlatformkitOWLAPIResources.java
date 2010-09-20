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
package be.ac.vub.platformkit.kb.owlapi;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Provides shared resources for PlatformKit OWLAPI knowledgebase.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformkitOWLAPIResources {

	private static final ResourceBundle resourceBundle =
		ResourceBundle.getBundle("be.ac.vub.platformkit.kb.owlapi.messages"); //$NON-NLS-1$

	/**
	 * Not meant to be instantiated.
	 */
	private PlatformkitOWLAPIResources() {
		super();
	}

	/**
	 * @param key
	 * @return The (translated) string for the given key, or the key if not available.
	 */
	public static String getString(String key) {
		try {
			return resourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * @return the resourcebundle
	 */
	public static ResourceBundle getResourcebundle() {
		return resourceBundle;
	}

}