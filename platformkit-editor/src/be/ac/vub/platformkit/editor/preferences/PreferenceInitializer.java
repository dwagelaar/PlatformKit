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
package be.ac.vub.platformkit.editor.preferences;

import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.m2m.atl.core.service.CoreService;

import be.ac.vub.platformkit.kb.IOntologiesFactory;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * Class used to initialize default preference values.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = PlatformkitEditorPlugin.getPlugin()
				.getPreferenceStore();
		store.setDefault(PreferenceConstants.P_REASONER, "builtin"); //$NON-NLS-1$
		store.setDefault(PreferenceConstants.P_DIG_URL, "http://localhost:8081"); //$NON-NLS-1$
		store.setDefault(PreferenceConstants.P_KB, "be.ac.vub.platformkit.kb.owlapi.OWLAPIOntologiesFactory"); //$NON-NLS-1$
		store.setDefault(PreferenceConstants.P_ATLVM, CoreService.getLaunchersNames()[0]);
		store.setDefault(PreferenceConstants.P_CACHE_API, true);
		store.setDefault(PreferenceConstants.P_LOG_LEVEL, Level.INFO.toString());
	}
	
	public static IOntologiesFactory getPreferredOntologyFactory() throws CoreException {
		IPreferenceStore store = PlatformkitEditorPlugin.getPlugin()
				.getPreferenceStore();
		String prefFactory = store.getString(PreferenceConstants.P_KB);
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(IOntologiesFactory.KB_EXT_POINT);
        for (IExtension extension : point.getExtensions()) {
            for (IConfigurationElement element : extension.getConfigurationElements()) {
            	if (prefFactory.equals(element.getAttribute("factory"))) {
                    IOntologiesFactory factory = (IOntologiesFactory)
            				element.createExecutableExtension("factory");
                    return factory;
            	}
            }
        }
        IStatus status = new Status(
        		IStatus.ERROR, 
        		PlatformkitEditorPlugin.getPlugin().getBundle().getSymbolicName(),
        		PlatformkitEditorPlugin.getPlugin().getString("PreferenceInitializer.noPrefKbError")); //$NON-NLS-1$
        throw new CoreException(status);
	}

}
