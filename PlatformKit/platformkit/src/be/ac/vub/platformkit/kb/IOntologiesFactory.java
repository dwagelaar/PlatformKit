package be.ac.vub.platformkit.kb;

import java.io.IOException;

/**
 * Interface for ontology repository factories 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public interface IOntologiesFactory {

	public static final String KB_EXT_POINT = "be.ac.vub.platformkit.knowledgebase";

	/**
	 * Creates a new {@link IOntologies} object.
	 * @return The created {@link IOntologies} object.
	 * @throws IOException
	 */
	public abstract IOntologies createIOntologies() throws IOException;
	
}
