package be.ac.vub.platformkit.editor.preferences;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class PlatformkitPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {
	
	public PlatformkitPreferencePage() {
		super(GRID);
		setPreferenceStore(PlatformkitEditorPlugin.getPlugin().getPreferenceStore());
		setDescription("PlatformKit preferences");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		Group reasonerGroup = new Group(getFieldEditorParent(), SWT.SHADOW_ETCHED_IN);
		reasonerGroup.setText("OWL DL reasoner");
		reasonerGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		addField(new RadioGroupFieldEditor(
				PreferenceConstants.P_REASONER,
				"Select OWL DL reasoner to use:",
				1,
				new String[][] { 
						{ "&Built-in Pellet 1.3 reasoner", "builtin" }, 
						{ "&DIG reasoner", "dig" } },
				reasonerGroup));

		StringFieldEditor reasonerUrl = new StringFieldEditor(
				PreferenceConstants.P_DIG_URL, "\tDIG reasoner &URL:",
				reasonerGroup) {

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
		};
		reasonerUrl.setEmptyStringAllowed(false);
		reasonerUrl.setErrorMessage("Invalid DIG reasoner URL");
		addField(reasonerUrl);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

}