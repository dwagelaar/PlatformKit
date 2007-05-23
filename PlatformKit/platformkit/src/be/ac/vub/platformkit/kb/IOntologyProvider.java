package be.ac.vub.platformkit.kb;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for Eclipse plugins that provide PlatformKit ontologies.
 * @author dennis
 *
 */
public interface IOntologyProvider {

	/**
	 * @return An array of {@link InputStream} objects that provide the ontology's contents.
	 * @throws IOException if one or more streams to ontologies could not be opened.
	 */
	InputStream[] getOntologies() throws IOException;
	
}
