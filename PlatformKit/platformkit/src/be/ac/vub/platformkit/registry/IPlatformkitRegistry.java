package be.ac.vub.platformkit.registry;

import be.ac.vub.platformkit.kb.IOntologyProvider;

public interface IPlatformkitRegistry {

	/**
	 * @return The registered {@link IOntologyProvider}s, if any.
	 */
	public IOntologyProvider[] getOntologyProviders();

}