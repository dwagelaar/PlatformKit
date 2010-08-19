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
package be.ac.vub.platformkit.examples;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.osgi.framework.Bundle;

import be.ac.vub.platformkit.kb.INamedOntologyProvider;
import be.ac.vub.platformkit.kb.util.BundleSwitch;
import be.ac.vub.platformkit.logging.PlatformkitLogger;

public class ExamplesOntologyProvider implements INamedOntologyProvider {

	protected static Bundle bundle = BundleSwitch.getBundle(ExamplesOntologyProvider.class);
	private static final String[] ontologies = new String[] {
		"platformkit_2010_1/Generic/jdk-1_1-pc.owl",
		"platformkit_2010_1/Generic/j2se-1_2-pc.owl",
		"platformkit_2010_1/Generic/j2se-1_3-pc.owl",
		"platformkit_2010_1/Generic/j2se-1_4-pc.owl",
		"platformkit_2010_1/Generic/j2se-5_0-pc.owl",
		"platformkit_2010_1/Generic/java-se-6-pc.owl",
		"platformkit_2010_1/Generic/personaljava-1_1-pocketpc.owl",
		"platformkit_2010_1/Generic/j2me-pp-1_0-pocketpc.owl",
		"platformkit_2010_1/Generic/j2me-pp-1_1-pocketpc.owl",
		"platformkit_2010_1/Generic/j2me-midp-1_0-phone.owl",
		"platformkit_2010_1/Generic/j2me-midp-2_0-phone.owl",

		"platformkit_2010_1/Sharp/sharp-zaurus-sl-c1000-pp.owl",
		"platformkit_2010_1/Sharp/sharp-zaurus-sl-c1000-jeode.owl",

		"platformkit_2010_1/Siemens/siemens-cx70v.owl",

		"platformkit_2010_1/Nokia/nokia-n800-jalimo.owl"
	}; //$NON-NLS-1$

	private static final String[] names = new String[ontologies.length];
	static {
		for (int i = 0; i < names.length; i++) {
			names[i] = PlatformkitExamplesResources.getString(ontologies[i]);
		}
	}

	/**
	 * The singleton {@link ExamplesOntologyProvider} instance.
	 */
	public static ExamplesOntologyProvider INSTANCE = new ExamplesOntologyProvider();

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologyProvider#getOntologies()
	 */
	public InputStream[] getOntologies() throws IOException {
		InputStream[] streams = new InputStream[ontologies.length];
		for (int i = 0; i < ontologies.length; i++) {
			URL resource = null;
			if (bundle == null) {
				resource = ExamplesOntologyProvider.class.getResource("../../../../../" + ontologies[i]); //$NON-NLS-1$
			} else {
				resource = bundle.getResource(ontologies[i]);
			}
			if (resource == null) {
				streams[i] = new FileInputStream("../platformkit-examples/" + ontologies[i]); //$NON-NLS-1$
			} else {
				streams[i] = resource.openStream();
			}
			assert streams[i] != null;
			PlatformkitLogger.logger.fine(String.format(
					PlatformkitExamplesResources.getString("ExamplesOntologyProvider.providingOntAs"), 
					ontologies[i], 
					streams[i])); //$NON-NLS-1$
		}
		return streams;
	}

	public String[] getOntologyNames() {
		return names;
	}

}
