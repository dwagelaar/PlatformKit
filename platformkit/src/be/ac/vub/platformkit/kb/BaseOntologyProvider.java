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

import junit.framework.Assert;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import be.ac.vub.platformkit.PlatformkitResources;

/**
 * Provider for default PlatformKit ontologies
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class BaseOntologyProvider implements IOntologyProvider {

	private static final String[] ontologies = new String[] {
		"codamos_2005_01/Units.owl",
		"codamos_2006_01/Environment.owl",
		"codamos_2006_01/Platform.owl",
		"codamos_2006_01/Service.owl",
		"codamos_2006_01/User.owl",
		"codamos_2006_01/Context.owl",
		"codamos_2007_01/Platform.owl",

		"codamos_2006_01/Corba.owl",
		"codamos_2006_01/Java.owl",
		"codamos_2006_01/OperatingSystems.owl",
		"davy_2006_01/Component.owl",
		"davy_2006_01/Draco.owl",
		"codamos_2007_01/PackageManagers.owl",
		"codamos_2007_01/Java.owl"
	};

	protected static Bundle bundle = Platform.getBundle("be.ac.vub.platformkit"); //$NON-NLS-1$
	protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);

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
			Assert.assertNotNull(resource);
			streams[i] = resource.openStream();
			logger.fine(String.format(
					PlatformkitResources.getString("BaseOntologyProvider.providingOntology"), 
					ontologies[i], 
					streams[i])); //$NON-NLS-1$
		}
		return streams;
	}

}
