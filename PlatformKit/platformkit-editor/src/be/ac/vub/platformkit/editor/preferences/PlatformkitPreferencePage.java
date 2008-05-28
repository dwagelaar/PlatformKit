package be.ac.vub.platformkit.editor.preferences;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.m2m.atl.adt.launching.AtlVM;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import be.ac.vub.platformkit.kb.IOntologiesFactory;
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
		setDescription("PlatformKit preferences:\n");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		RadioGroupFieldEditor reasoner = new RadioGroupFieldEditor(
				PreferenceConstants.P_REASONER,
				"OWL DL reasoner",
				1,
				new String[][] { 
						{ "&Built-in Pellet reasoner", PreferenceConstants.P_BUILTIN }, 
						{ "&DIG reasoner", PreferenceConstants.P_DIG } },
				getFieldEditorParent(),
                true);
        addField(reasoner);

        Composite reasonerUrlContainer = new Composite(
                reasoner.getRadioBoxControl(getFieldEditorParent()), 
                SWT.NONE);
        reasonerUrlContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
		StringFieldEditor reasonerUrl = new StringFieldEditor(
				PreferenceConstants.P_DIG_URL, "\tDIG reasoner &URL:",
				reasonerUrlContainer) {

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

		String[][] kbs = getKBs();
		ComboFieldEditor kb = new ComboFieldEditor(
				PreferenceConstants.P_KB, "&OWL knowledgebase implementation:",
				kbs,
				getFieldEditorParent());
		addField(kb);

		String[] vms = AtlVM.getVMs();
		String[][] vmss = new String[vms.length][2];
		for (int i = 0; i < vms.length; i++) {
			vmss[i][0] = vms[i];
			vmss[i][1] = vms[i];
		}
		ComboFieldEditor atlVM = new ComboFieldEditor(
				PreferenceConstants.P_ATLVM, "&ATL Virtual Machine:",
				vmss,
				getFieldEditorParent());
		addField(atlVM);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	private static String[][] getKBs() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();		
		IExtensionPoint point = registry.getExtensionPoint(IOntologiesFactory.KB_EXT_POINT);
		IExtension[] extensions = point.getExtensions();
		String[][] kbs = new String[extensions.length][2];
		for (int i = 0; i < extensions.length; i++) {
			IExtension extension = extensions[i];
			for (IConfigurationElement element : extension.getConfigurationElements()) {
				kbs[i][0] = element.getAttribute("name");//$NON-NLS-1$
				kbs[i][1] = element.getAttribute("factory");//$NON-NLS-1$
			}
		}
		return kbs;
	}

}