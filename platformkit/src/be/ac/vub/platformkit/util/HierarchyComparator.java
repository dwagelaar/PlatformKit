package be.ac.vub.platformkit.util;

import java.util.Comparator;
import java.util.logging.Logger;

import junit.framework.Assert;

import be.ac.vub.platformkit.Constraint;
import be.ac.vub.platformkit.kb.Ontologies;

import com.hp.hpl.jena.ontology.OntClass;

/**
 * Compares {@link Constraint} objects, such that the more specific (subclass) constraint is
 * considered smaller (MOST_SPECIFIC_FIRST) or greater (LEAST_SPECIFIC_FIRST) than
 * the less specific (superclass) constraint. If both constraints are equivalent or no subclass
 * relationship can be determined, they are considered equally specific.
 * @author dennis
 */
public class HierarchyComparator implements Comparator {
    public static final int MOST_SPECIFIC_FIRST = -1;
    public static final int LEAST_SPECIFIC_FIRST = 1;
    
    private Logger logger = Logger.getLogger(Ontologies.LOGGER);
    private int mode;

    /**
     * Creates a HierarchyComparator.
     * @param mode MOST_SPECIFIC_FIRST or LEAST_SPECIFIC_FIRST.
     * @throws IllegalArgumentException if illegal mode is given.
     */
    public HierarchyComparator(int mode)
    throws IllegalArgumentException {
        super();
        if (Math.abs(mode) != 1) {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }
        this.mode = mode;
    }
    
    /**
     * @see Comparator#compare(T, T)
     * @throws ClassCastException if something else than Constraint objects are compared
     */
    public int compare(Object arg0, Object arg1)
    throws ClassCastException {
        OntClass c0 = ((Constraint) arg0).getOntClass();
        OntClass c1 = ((Constraint) arg1).getOntClass();
        Assert.assertNotNull(c0);
        Assert.assertNotNull(c1);
        if (c0.equals(c1) || c0.hasEquivalentClass(c1)) {
            logger.fine(c0 + " equivalent to " + c1);
            return 0;
        }
        if (c0.hasSuperClass(c1)) {
            logger.fine(c0 + " subclass of " + c1);
            return 1 * mode;
        }
        if (c0.hasSubClass(c1)) {
            logger.fine(c0 + " superclass of " + c1);
            return -1 * mode;
        }
        logger.fine(c0 + " orthogonal to " + c1);
        throw new ClassCastException("Cannot determine order for " + c0 + " and " + c1);
    }
    
    /**
     * @see Comparator#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj instanceof HierarchyComparator) {
            return ((HierarchyComparator) obj).mode == mode;
        } else {
            return false;
        }
    }

}
