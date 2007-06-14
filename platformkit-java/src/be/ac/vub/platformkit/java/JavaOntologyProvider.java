package be.ac.vub.platformkit.java;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import be.ac.vub.platformkit.kb.IOntologyProvider;
import be.ac.vub.platformkit.kb.Ontologies;

public class JavaOntologyProvider implements IOntologyProvider {

	private static final String[] ontologies = new String[] {
		"ontology/codamos_2007_01/j2me-midp-1_0-api.owl",
		"ontology/codamos_2007_01/personaljava-1_1-api.owl",
		"ontology/codamos_2007_01/jdk-1_1-api.owl",
		"ontology/codamos_2007_01/j2se-1_2-api.owl",
		"ontology/codamos_2007_01/j2me-pp-1_0-api.owl",
		"ontology/codamos_2007_01/j2se-1_3-api.owl",
		"ontology/codamos_2007_01/j2se-1_4-api.owl",
		"ontology/codamos_2007_01/j2se-1_5-api.owl"
	};
	
	protected static Bundle bundle = Platform.getBundle("be.ac.vub.platformkit.java");
    protected static Logger logger = Logger.getLogger(Ontologies.LOGGER);

	public InputStream[] getOntologies() throws IOException {
		InputStream[] streams = new InputStream[ontologies.length];
		for (int i = 0; i < ontologies.length; i++) {
			streams[i] = bundle.getResource(ontologies[i]).openStream();
			logger.fine("Providing ontology " + ontologies[i] + " as " + streams[i]);
		}
		return streams;
	}

}
