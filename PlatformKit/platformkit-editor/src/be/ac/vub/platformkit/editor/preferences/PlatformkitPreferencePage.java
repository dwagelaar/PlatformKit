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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import be.ac.vub.platformkit.kb.IOntologiesFactory;
import be.ac.vub.platformkit.logging.PlatformkitLogger;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * <p>
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * </p><p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 * </p>
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformkitPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private ComboFieldEditor kb;
	private DynamicComboFieldEditor kbrs;

	public PlatformkitPreferencePage() {
		super(GRID);
		setPreferenceStore(PlatformkitEditorPlugin.getPlugin().getPreferenceStore());
		setDescription(
				PlatformkitEditorPlugin.getPlugin().getString("PlatformkitPreferencePage.description")); //$NON-NLS-1$
	}

	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		String[][] kbs = getKBs();
		kb = new ComboFieldEditor(
				PreferenceConstants.P_KB, 
				PlatformkitEditorPlugin.getPlugin().getString("PlatformkitPreferencePage.kbImpl"),
				kbs,
				getFieldEditorParent()); //$NON-NLS-1$
		addField(kb);

		kbrs = new DynamicComboFieldEditor(
				PreferenceConstants.P_KBRS, 
				PlatformkitEditorPlugin.getPlugin().getString("PlatformkitPreferencePage.owlReasoner"),
				getFieldEditorParent()); //$NON-NLS-1$
		updateKBRS(getPreferenceStore().getString(PreferenceConstants.P_KB));
		addField(kbrs);

		StringFieldEditor reasonerUrl = new StringFieldEditor(
				PreferenceConstants.P_DIG_URL, 
				PlatformkitEditorPlugin.getPlugin().getString("PlatformkitPreferencePage.digUrl"),
				getFieldEditorParent()) {

			/* (non-Javadoc)
			 * @see org.eclipse.jface.preference.StringFieldEditor#doCheckState()
			 */
			protected boolean doCheckState() {
				try {
					new URL(getStringValue());
				} catch (MalformedURLException e) {
					return false;
				}
				return true;
			}
		}; //$NON-NLS-1$
		reasonerUrl.setEmptyStringAllowed(false);
		reasonerUrl.setErrorMessage(
				PlatformkitEditorPlugin.getPlugin().getString("PlatformkitPreferencePage.digUrlError")); //$NON-NLS-1$
		addField(reasonerUrl);

		String[][] lvls = new String[][] {
				createComboEntry(Level.OFF),
				createComboEntry(Level.SEVERE),
				createComboEntry(Level.WARNING),
				createComboEntry(Level.INFO),
				createComboEntry(Level.FINE),
				createComboEntry(Level.FINER),
				createComboEntry(Level.FINEST),
				createComboEntry(Level.ALL)
		};
		ComboFieldEditor logLevel = new ComboFieldEditor(
				PreferenceConstants.P_LOG_LEVEL, 
				PlatformkitEditorPlugin.getPlugin().getString("PlatformkitPreferencePage.logLevel"),
				lvls,
				getFieldEditorParent()); //$NON-NLS-1$
		addField(logLevel);
	}
	
	/**
	 * @param level
	 * @return A preference combo field entry for level.
	 */
	private String[] createComboEntry(Level level) {
		return new String[] { level.getLocalizedName(), level.toString() };
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	/**
	 * @return A {@link ComboFieldEditor} string array with the registered knowledgebases.
	 */
	private static String[][] getKBs() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();		
		IExtensionPoint point = registry.getExtensionPoint(IOntologiesFactory.KB_EXT_POINT);
		IExtension[] extensions = point.getExtensions();
		String[][] kbs = new String[extensions.length][2];
		for (int i = 0; i < extensions.length; i++) {
			IExtension extension = extensions[i];
			for (IConfigurationElement element : extension.getConfigurationElements()) {
				kbs[i][0] = element.getAttribute("name"); //$NON-NLS-1$
				kbs[i][1] = element.getAttribute("factory"); //$NON-NLS-1$
			}
		}
		return kbs;
	}

	/**
	 * Updates the kbrs combo box with the new selection of reasoners
	 * @param factory the {@link IOntologiesFactory} for which to retrieve the supported reasoners
	 */
	private void updateKBRS(String factory) {
        kbrs.clearEntries();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(IOntologiesFactory.KB_EXT_POINT);
        for (IExtension extension : point.getExtensions()) {
            for (IConfigurationElement element : extension.getConfigurationElements()) {
            	if (factory.equals(element.getAttribute("factory"))) {
            		for (IConfigurationElement child : element.getChildren()) {
            			kbrs.addEntry(
            					child.getAttribute("name"), 
            					child.getAttribute("id"));
            		}
            	}
            }
        }
        kbrs.load();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		boolean ok = super.performOk();
		if (ok) {
			// Directly apply new log level
			String logLevel = getPreferenceStore().getString(PreferenceConstants.P_LOG_LEVEL);
			Logger logger = Logger.getLogger(PlatformkitLogger.LOGGER);
			logger.setLevel(Level.parse(logLevel));
			logger.info(String.format(
					PlatformkitEditorPlugin.getPlugin().getString("logLevelSetTo"), 
					logLevel)); //$NON-NLS-1$
		}
		return ok;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		if (event.getProperty().equals(FieldEditor.VALUE) && event.getSource() == kb) {
			updateKBRS((String) event.getNewValue());
		}
	}

}