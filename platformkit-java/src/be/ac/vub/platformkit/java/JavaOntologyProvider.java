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
package be.ac.vub.platformkit.java;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.IOntologyProvider;

/**
 * Java ontology provider
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class JavaOntologyProvider implements IOntologyProvider {

	private static final String[] ontologies = new String[] {
		"codamos_2007_01/j2me-midp-1_0-api.owl",
		"codamos_2007_01/j2me-midp-2_0-api.owl",
		"codamos_2007_01/personaljava-1_1-api.owl",
		"codamos_2007_01/jdk-1_1-api.owl",
		"codamos_2007_01/j2se-1_2-api.owl",
		"codamos_2007_01/j2me-pp-1_0-api.owl",
		"codamos_2007_01/j2se-1_3-api.owl",
		"codamos_2007_01/j2me-pp-1_1-api.owl",
		"codamos_2007_01/j2se-1_4-api.owl",
		"codamos_2007_01/j2se-1_5-api.owl",
		"codamos_2007_01/j2se-1_6-api.owl",
		"codamos_2007_01/JavaAPI.owl",
		"platformkit_2009_01/swt-3_0.owl",
		"platformkit_2009_01/swt-3_1.owl",
		"platformkit_2009_01/swt-3_2.owl",
		"platformkit_2009_01/swt-3_3.owl",
		"platformkit_2009_01/swt-3_4.owl",
		"platformkit_2009_01/swt-3_5.owl",
		"platformkit_2009_01/SWTAPI.owl"
	}; //$NON-NLS-1$

	protected static Bundle bundle = Platform.getBundle("be.ac.vub.platformkit.java"); //$NON-NLS-1$
	protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);

	public static JavaOntologyProvider INSTANCE = new JavaOntologyProvider();

	public InputStream[] getOntologies() throws IOException {
		InputStream[] streams = new InputStream[ontologies.length];
		for (int i = 0; i < ontologies.length; i++) {
			URL resource = null;
			if (bundle == null) {
				resource = JavaOntologyProvider.class.getResource("../../../../../" + ontologies[i]); //$NON-NLS-1$
			} else {
				resource = bundle.getResource("ontology/" + ontologies[i]); //$NON-NLS-1$
			}
			Assert.assertNotNull(resource);
			streams[i] = resource.openStream();
			logger.fine(String.format(
					PlatformkitJavaResources.getString("JavaOntologyProvider.providingOntAs"),
					ontologies[i], 
					streams[i])); //$NON-NLS-1$
		}
		return streams;
	}

}
