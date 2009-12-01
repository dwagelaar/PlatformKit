package be.ac.vub.platformkit.editor.preferences;

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

import be.ac.vub.platformkit.kb.IOntologiesFactory;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * Class used to initialize default preference values.
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
		store.setDefault(PreferenceConstants.P_REASONER, "builtin");
		store.setDefault(PreferenceConstants.P_DIG_URL, "http://localhost:8081");
		store.setDefault(PreferenceConstants.P_KB, "be.ac.vub.platformkit.kb.owlapi.OWLAPIOntologiesFactory");
		store.setDefault(PreferenceConstants.P_CACHE_API, true);
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
        		"no preferred knowledgebase found");
        throw new CoreException(status);
	}

}
