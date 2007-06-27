package be.ac.vub.platformkit.presentation.util;

import be.ac.vub.platformkit.kb.IOntologyProvider;

public interface INamedOntologyProvider extends IOntologyProvider {

    /**
     * @return An array of {@link String} objects that provide the names of the ontologies.
     * The returned array must have the same amount of elements as the array returned by
     * {@link IOntologyProvider#getOntologies()}.
     */
    String[] getOntologyNames();
    
}
