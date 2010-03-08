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
package be.ac.vub.platformkit.registry.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import be.ac.vub.platformkit.PlatformkitResources;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.IOntologyProvider;
import be.ac.vub.platformkit.registry.IPlatformkitRegistry;

/**
 * Wrapper class for getting information from the Eclipse {@link IExtensionRegistry}.
 * For internal use only.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public final class EclipsePlatformkitRegistry implements IPlatformkitRegistry {

	/**
	 * @return The registered {@link IOntologyProvider}s, if the Eclipse extension registry is available.
	 */
	public IOntologyProvider[] getOntologyProviders() {
		List<IOntologyProvider> ontProviders = new ArrayList<IOntologyProvider>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		if (registry == null) {
			throw new UnsupportedOperationException(
					PlatformkitResources.getString("ExtensionRegistry.registryNotFound")); //$NON-NLS-1$
		}
		IExtensionPoint point = registry.getExtensionPoint(IOntologies.ONTOLOGY_EXT_POINT);
		for (IExtension extension : point.getExtensions()) {
			for (IConfigurationElement element : extension.getConfigurationElements()) {
				try {
					IOntologyProvider provider = (IOntologyProvider)
					element.createExecutableExtension("provider"); //$NON-NLS-1$
					ontProviders.add(provider);
				} catch (CoreException e) {
					RuntimeException re = new RuntimeException(e.getLocalizedMessage());
					re.initCause(e);
					throw re;
				}
			}
		}
		return ontProviders.toArray(new IOntologyProvider[ontProviders.size()]);
	}

}
