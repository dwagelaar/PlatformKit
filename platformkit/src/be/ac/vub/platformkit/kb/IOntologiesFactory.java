package be.ac.vub.platformkit.kb;

import java.io.IOException;

public interface IOntologiesFactory {

	public static final String KB_EXT_POINT = "be.ac.vub.platformkit.knowledgebase";

	/**
	 * Creates a new {@link IOntologies} object.
	 * @return The created {@link IOntologies} object.
	 * @throws IOException
	 */
	public abstract IOntologies createIOntologies() throws IOException;
	
}
