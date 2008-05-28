package be.ac.vub.platformkit.kb;

import java.io.IOException;

import be.ac.vub.platformkit.kb.owlapi.OWLAPIOntologiesFactory;

public interface IOntologiesFactory {

	public static final IOntologiesFactory INSTANCE = new OWLAPIOntologiesFactory();
	
	/**
	 * Creates a new {@link IOntologies} object.
	 * @return The created {@link IOntologies} object.
	 * @throws IOException
	 */
	public abstract IOntologies createIOntologies() throws IOException;
	
}
