package be.ac.vub.platformkit.java;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import be.ac.vub.platformkit.kb.IOntologyProvider;
import be.ac.vub.platformkit.kb.Ontologies;

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
		"codamos_2007_01/JavaAPI.owl"
	};
	
	protected static Bundle bundle = Platform.getBundle("be.ac.vub.platformkit.java");
    protected static Logger logger = Logger.getLogger(Ontologies.LOGGER);

	public static JavaOntologyProvider INSTANCE = new JavaOntologyProvider();

	public InputStream[] getOntologies() throws IOException {
		InputStream[] streams = new InputStream[ontologies.length];
		for (int i = 0; i < ontologies.length; i++) {
			URL resource = null;
			if (bundle == null) {
				resource = JavaOntologyProvider.class.getResource("../../../../../" + ontologies[i]);
			} else {
				resource = bundle.getResource("ontology/" + ontologies[i]);
			}
			Assert.assertNotNull(resource);
			streams[i] = resource.openStream();
			logger.fine("Providing ontology " + ontologies[i] + " as " + streams[i]);
		}
		return streams;
	}

}
