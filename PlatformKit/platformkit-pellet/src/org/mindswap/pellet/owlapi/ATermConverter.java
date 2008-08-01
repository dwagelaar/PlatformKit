/*
 * Created on May 25, 2005
 */
package org.mindswap.pellet.owlapi;


import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLOntology;

/**
 * @deprecated Use {@link ConceptConverter} or {@link AxiomConverter} instead
 */
public class ATermConverter extends ConceptConverter {
    public ATermConverter( OWLOntology ont, OWLDataFactory factory ) {
        super( ont, factory );
    }
}
