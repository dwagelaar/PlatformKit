package be.ac.vub.platformkit.kb.util;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.util.iterator.Filter;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * Accepts all named OntClass objects which are not OWL:Thing or OWL:Nothing
 * @author dennis
 *
 */
public class NamedClassFilter extends Filter {

    /**
     * @see Filter#accept(java.lang.Object)
     */
    public boolean accept(Object o) {
        if (!(o instanceof OntClass)) { return false; }
        return isNamedClass((OntClass) o);
    }
    
    /**
     * @param c
     * @return True if c is a named class.
     */
    public static boolean isNamedClass(OntClass c) {
        String uri = c.getURI();
        if (uri == null) { return false; }
        if (uri.equals(OWL.Thing.getURI())) { return false; }
        if (uri.equals(OWL.Nothing.getURI())) { return false; }
        return true;
    }

}
