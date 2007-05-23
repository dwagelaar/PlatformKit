package be.ac.vub.platformkit.kb;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public class BaseOntologyProvider implements IOntologyProvider {
	
	private static final String[] ontologies = new String[] {
		"ontology/codamos_2005_01/Units.owl",
		"ontology/codamos_2006_01/Environment.owl",
		"ontology/codamos_2006_01/Platform.owl",
		"ontology/codamos_2006_01/Service.owl",
		"ontology/codamos_2006_01/User.owl",
		"ontology/codamos_2006_01/Context.owl",
		"ontology/codamos_2007_01/Platform.owl",

		"ontology/codamos_2006_01/Corba.owl",
		"ontology/codamos_2006_01/Java.owl",
		"ontology/codamos_2006_01/OperatingSystems.owl",
		"ontology/davy_2006_01/Component.owl",
		"ontology/davy_2006_01/Draco.owl",
		"ontology/codamos_2007_01/Java.owl"
	};
	
	protected static Bundle bundle = Platform.getBundle("be.ac.vub.platformkit");
    protected static Logger logger = Logger.getLogger(Ontologies.LOGGER);
	
	public static BaseOntologyProvider INSTANCE = new BaseOntologyProvider();

	public InputStream[] getOntologies() throws IOException {
		InputStream[] streams = new InputStream[ontologies.length];
		for (int i = 0; i < ontologies.length; i++) {
			streams[i] = bundle.getResource(ontologies[i]).openStream();
			logger.fine("Providing ontology " + ontologies[i] + " as " + streams[i]);
		}
		return streams;
	}

}
