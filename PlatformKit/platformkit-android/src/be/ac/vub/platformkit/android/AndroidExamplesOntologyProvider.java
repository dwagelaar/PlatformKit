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
package be.ac.vub.platformkit.android;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.osgi.framework.Bundle;

import be.ac.vub.platformkit.kb.INamedOntologyProvider;
import be.ac.vub.platformkit.kb.util.BundleSwitch;
import be.ac.vub.platformkit.logging.PlatformkitLogger;

/**
 * Provides example platform instance ontologies for the Java JRE plug-in. 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class AndroidExamplesOntologyProvider implements INamedOntologyProvider {

	protected static Bundle bundle = BundleSwitch.getBundle(AndroidExamplesOntologyProvider.class);
	private static final String[] ontologies = new String[] {
		"platformkit_2010_1/Google/android-1_5-phone.owl",
		"platformkit_2010_1/Google/android-1_6-phone.owl",
		"platformkit_2010_1/Google/android-2_1-phone.owl",
		"platformkit_2010_1/Google/android-2_2-phone.owl",
		"platformkit_2010_1/Google/android-2_3-phone.owl",

		"platformkit_2010_1/Google/google-nexus-one.owl",
		"platformkit_2010_1/Google/google-nexus-s.owl",

		"platformkit_2010_1/HTC/htc-hero.owl"
	}; //$NON-NLS-1$

	private static final String[] names = new String[ontologies.length];
	static {
		for (int i = 0; i < names.length; i++) {
			names[i] = PlatformkitAndroidResources.getString(ontologies[i]);
		}
	}

	/**
	 * The singleton {@link ExamplesOntologyProvider} instance.
	 */
	public static AndroidExamplesOntologyProvider INSTANCE = new AndroidExamplesOntologyProvider();

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologyProvider#getOntologies()
	 */
	public InputStream[] getOntologies() throws IOException {
		InputStream[] streams = new InputStream[ontologies.length];
		for (int i = 0; i < ontologies.length; i++) {
			URL resource = null;
			if (bundle == null) {
				resource = AndroidExamplesOntologyProvider.class.getResource("../../../../../ontology-examples/" + ontologies[i]); //$NON-NLS-1$
			} else {
				resource = bundle.getResource("ontology-examples/" + ontologies[i]);
			}
			if (resource == null) {
				streams[i] = new FileInputStream("../platformkit-android/ontology-examples/" + ontologies[i]); //$NON-NLS-1$
			} else {
				streams[i] = resource.openStream();
			}
			assert streams[i] != null;
			PlatformkitLogger.logger.fine(String.format(
					PlatformkitAndroidResources.getString("AndroidExamplesOntologyProvider.providingOntAs"), 
					ontologies[i], 
					streams[i])); //$NON-NLS-1$
		}
		return streams;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.INamedOntologyProvider#getOntologyNames()
	 */
	public String[] getOntologyNames() {
		return names;
	}

}
