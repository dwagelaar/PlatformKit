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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 * Eclipse-independent class for retrieving the OSGi {@link Bundle} of a {@link Class}. 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public final class BundleSwitch {

	/**
	 * @param clazz
	 * @return The {@link Bundle} that contains clazz.
	 */
	public static Bundle getBundle(Class<?> clazz) {
		return getBundle(clazz.getClassLoader());
	}

	/**
	 * @param bundleRef
	 * @return The {@link Bundle} for bundleRef.
	 */
	public static Bundle getBundle(BundleReference bundleRef) {
		return bundleRef.getBundle();
	}

	/**
	 * @param loader
	 * @return If loader is a {@link BundleReference}, the {@link Bundle}, otherwise <code>null</code>.
	 */
	public static Bundle getBundle(ClassLoader loader) {
		if (loader instanceof BundleReference) {
			return getBundle((BundleReference) loader);
		} else {
			return null;
		}
	}

}
