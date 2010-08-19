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
package be.ac.vub.platformkit.kb;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;

import be.ac.vub.platformkit.PlatformkitResources;
import be.ac.vub.platformkit.kb.util.BundleSwitch;
import be.ac.vub.platformkit.logging.PlatformkitLogger;

/**
 * Provider for default PlatformKit ontologies
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class BaseOntologyProvider implements IOntologyProvider {

	private static final String[] ontologies = new String[] {
		"platformkit_2010_1/platform.owl",
		"platformkit_2010_1/io.owl",
		"platformkit_2010_1/resource.owl",
		"platformkit_2010_1/isa.owl",
		"platformkit_2010_1/os.owl",
		"platformkit_2010_1/arm.owl",
		"platformkit_2010_1/x86.owl"
	};

	protected static Bundle bundle = BundleSwitch.getBundle(BaseOntologyProvider.class);
	protected static Logger logger = Logger.getLogger(PlatformkitLogger.LOGGER);

	public static BaseOntologyProvider INSTANCE = new BaseOntologyProvider();

	public InputStream[] getOntologies() throws IOException {
		InputStream[] streams = new InputStream[ontologies.length];
		for (int i = 0; i < ontologies.length; i++) {
			URL resource = null;
			if (bundle == null) {
				resource = BaseOntologyProvider.class.getResource("../../../../../" + ontologies[i]); //$NON-NLS-1$
			} else {
				resource = bundle.getResource("ontology/" + ontologies[i]); //$NON-NLS-1$
			}
			assert resource != null;
			streams[i] = resource.openStream();
			logger.fine(String.format(
					PlatformkitResources.getString("BaseOntologyProvider.providingOntology"), 
					ontologies[i], 
					streams[i])); //$NON-NLS-1$
		}
		return streams;
	}

}
