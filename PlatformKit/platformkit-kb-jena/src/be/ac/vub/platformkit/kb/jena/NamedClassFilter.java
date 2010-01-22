package be.ac.vub.platformkit.kb.jena;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.util.iterator.Filter;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * Accepts all named OntClass objects which are not OWL:Thing or OWL:Nothing
 * @author dennis
 *
 */
public class NamedClassFilter extends Filter<OntClass> {

    /**
     * @see Filter#accept(java.lang.Object)
     */
    public boolean accept(OntClass o) {
        return isNamedClass((OntClass) o);
    }
    
    /**
     * @param c
     * @return True if c is a named class.
     */
    public final static boolean isNamedClass(OntClass c) {
        String uri = c.getURI();
        if (uri == null) { return false; }
        if (uri.equals(OWL.Thing.getURI())) { return false; }
        if (uri.equals(OWL.Nothing.getURI())) { return false; }
        return true;
    }

}
