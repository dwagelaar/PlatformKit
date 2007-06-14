package be.ac.vub.platformkit.editor.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

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
	}

}
