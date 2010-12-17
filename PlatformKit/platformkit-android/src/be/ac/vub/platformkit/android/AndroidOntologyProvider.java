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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.osgi.framework.Bundle;

import be.ac.vub.platformkit.kb.IOntologyProvider;
import be.ac.vub.platformkit.kb.util.BundleSwitch;
import be.ac.vub.platformkit.logging.PlatformkitLogger;

/**
 * Java JRE plug-in ontology provider.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class AndroidOntologyProvider implements IOntologyProvider {

	private static final String[] ontologies = new String[] {
		"platformkit_2010_1/android-1_5.owl",
		"platformkit_2010_1/android-1_6.owl",
		"platformkit_2010_1/android-2_1.owl",
		"platformkit_2010_1/android-2_2.owl",
		"platformkit_2010_1/android-2_3.owl"
	}; //$NON-NLS-1$

	protected static Bundle bundle = BundleSwitch.getBundle(AndroidOntologyProvider.class);

	public static AndroidOntologyProvider INSTANCE = new AndroidOntologyProvider();

	public InputStream[] getOntologies() throws IOException {
		InputStream[] streams = new InputStream[ontologies.length];
		for (int i = 0; i < ontologies.length; i++) {
			URL resource = null;
			if (bundle == null) {
				resource = AndroidOntologyProvider.class.getResource("../../../../../" + ontologies[i]); //$NON-NLS-1$
			} else {
				resource = bundle.getResource("ontology/" + ontologies[i]); //$NON-NLS-1$
			}
			assert resource != null;
			streams[i] = resource.openStream();
			PlatformkitLogger.logger.fine(String.format(
					PlatformkitAndroidResources.getString("AndroidOntologyProvider.providingOntAs"),
					ontologies[i], 
					streams[i])); //$NON-NLS-1$
		}
		return streams;
	}

}
