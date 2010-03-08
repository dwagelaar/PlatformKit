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
package be.ac.vub.platformkit.registry;

import be.ac.vub.platformkit.kb.IOntologyProvider;

/**
 * Utility class for retrieving registered objects. Works outside Eclipse.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public final class PlatformkitRegistry implements IPlatformkitRegistry {
	
	/**
	 * Singleton instance.
	 */
	public static PlatformkitRegistry INSTANCE = new PlatformkitRegistry();
	
	protected IPlatformkitRegistry internalRegistry;
	
	/**
	 * Creates a new {@link PlatformkitRegistry}.
	 */
	private PlatformkitRegistry() {
		super();
		internalRegistry = createInternalRegistry();
	}

	/**
	 * @return The registered {@link IOntologyProvider}s, if any.
	 */
	public IOntologyProvider[] getOntologyProviders() {
		if (internalRegistry != null) {
			return internalRegistry.getOntologyProviders();
		} else {
			return new IOntologyProvider[0];
		}
	}
	
	/**
	 * @return A new internal registry object, if found, <code>null</code> otherwise.
	 */
	private IPlatformkitRegistry createInternalRegistry() {
		try {
			Class<IPlatformkitRegistry> internalRegistryClass = (Class<IPlatformkitRegistry>)
				Class.forName("be.ac.vub.platformkit.registry.internal.EclipsePlatformkitRegistry"); //$NON-NLS-1$
			return internalRegistryClass.newInstance();
		} catch (Exception e) {
			return null;
		}
	}

}
