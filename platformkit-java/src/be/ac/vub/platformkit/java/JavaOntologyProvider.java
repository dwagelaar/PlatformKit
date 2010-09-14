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
import java.util.StringTokenizer;

import org.osgi.framework.Bundle;

import be.ac.vub.platformkit.kb.IOntologyProvider;
import be.ac.vub.platformkit.kb.util.BundleSwitch;
import be.ac.vub.platformkit.logging.PlatformkitLogger;

/**
 * Java ontology provider
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class JavaOntologyProvider implements IOntologyProvider {

	public static final String JAVA_NS = "http://soft.vub.ac.be/platformkit/2010/1/java.owl";

	public static final String JRE_URI = JAVA_NS + "#JRE";
	public static final String JAVA_CLASS_LIBRARY_URI = JAVA_NS + "#JavaClassLibrary";
	public static final String JAVA_VM_URI = JAVA_NS + "#JavaVM";

	public static final String MAJOR_VERSION_NUMBER_URI = JAVA_NS + "#majorVersionNumber";
	public static final String PREVERIFIED_URI = JAVA_NS + "#preverified";

	private static final String[] ontologies = new String[] {
		"platformkit_2010_1/java.owl",

		"platformkit_2010_1/j2me-midp-1_0.owl",
		"platformkit_2010_1/j2me-midp-2_0.owl",
		"platformkit_2010_1/personaljava-1_1.owl",
		"platformkit_2010_1/jdk-1_1.owl",
		"platformkit_2010_1/j2se-1_2.owl",
		"platformkit_2010_1/j2me-pp-1_0.owl",
		"platformkit_2010_1/j2se-1_3.owl",
		"platformkit_2010_1/j2me-pp-1_1.owl",
		"platformkit_2010_1/j2se-1_4.owl",
		"platformkit_2010_1/j2se-5_0.owl",
		"platformkit_2010_1/java-se-6.owl",
		"platformkit_2010_1/java-re.owl",

		"platformkit_2010_1/swt-3_0.owl",
		"platformkit_2010_1/swt-3_1.owl",
		"platformkit_2010_1/swt-3_2.owl",
		"platformkit_2010_1/swt-3_3.owl",
		"platformkit_2010_1/swt-3_4.owl",
		"platformkit_2010_1/swt-3_5.owl",
		"platformkit_2010_1/swt.owl"
	}; //$NON-NLS-1$

	protected static Bundle bundle = BundleSwitch.getBundle(JavaOntologyProvider.class);

	public static JavaOntologyProvider INSTANCE = new JavaOntologyProvider();

	/**
	 * Translates the qualified UML package name into its JavaLibrary ontology class representation.
	 * @param umlQualifiedPackageName the qualified UML package name
	 * @return the JavaLibrary ontology class that represents the UML package
	 */
	public static String toJavaLibaryName(String umlQualifiedPackageName) {
		StringTokenizer st = new StringTokenizer(umlQualifiedPackageName, "::"); //$NON-NLS-1$
		StringBuffer sb = new StringBuffer();
		while (st.hasMoreTokens()) {
			String name = st.nextToken();
			sb.append(name.substring(0, 1).toUpperCase());
			sb.append(name.substring(1));
		}
		sb.append("ClassLibrary"); //$NON-NLS-1$
		return sb.toString();
	}

	public InputStream[] getOntologies() throws IOException {
		InputStream[] streams = new InputStream[ontologies.length];
		for (int i = 0; i < ontologies.length; i++) {
			URL resource = null;
			if (bundle == null) {
				resource = JavaOntologyProvider.class.getResource("../../../../../" + ontologies[i]); //$NON-NLS-1$
			} else {
				resource = bundle.getResource("ontology/" + ontologies[i]); //$NON-NLS-1$
			}
			assert resource != null;
			streams[i] = resource.openStream();
			PlatformkitLogger.logger.fine(String.format(
					PlatformkitJavaResources.getString("JavaOntologyProvider.providingOntAs"),
					ontologies[i], 
					streams[i])); //$NON-NLS-1$
		}
		return streams;
	}

}
