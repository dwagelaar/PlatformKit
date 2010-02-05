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
import java.util.logging.Logger;

import junit.framework.Assert;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.presentation.util.INamedOntologyProvider;

public class ExamplesOntologyProvider implements INamedOntologyProvider {

	protected static Bundle bundle = Platform.getBundle("be.ac.vub.platformkit.examples"); //$NON-NLS-1$
	protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);

	private static final String[] ontologies = new String[] {
		"codamos_2007_01/Generic/JDK1.1PC.owl",
		"codamos_2007_01/Generic/JDK1.2PC.owl",
		"codamos_2007_01/Generic/JDK1.3PC.owl",
		"codamos_2007_01/Generic/JDK1.4PC.owl",
		"codamos_2007_01/Generic/JDK1.5PC.owl",
		"codamos_2007_01/Generic/JDK1.6PC.owl",
		"codamos_2007_01/Generic/PersonalJava1.1PocketPC.owl",
		"codamos_2007_01/Generic/J2MEPP1.0PocketPC.owl",
		"codamos_2007_01/Generic/J2MEPP1.1PocketPC.owl",
		"codamos_2007_01/Generic/J2MEMIDP1.0Phone.owl",
		"codamos_2007_01/Generic/J2MEMIDP2.0Phone.owl",
		"codamos_2007_01/Sharp/ZaurusSL-C1000PP.owl",
		"codamos_2007_01/Sharp/ZaurusSL-C1000Jeode.owl",
		"codamos_2007_01/Siemens/CX70v.owl",
		"codamos_2007_01/Nokia/NokiaN800Jalimo.owl"
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
			Assert.assertNotNull(streams[i]);
			logger.fine(String.format(
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
