package be.ac.vub.platformkit.util;

import java.util.Comparator;
import java.util.logging.Logger;

import junit.framework.Assert;
import be.ac.vub.platformkit.Constraint;
import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntologies;

/**
 * Compares {@link Constraint} objects, such that the more specific (subclass) constraint is
 * considered smaller (MOST_SPECIFIC_FIRST) or greater (LEAST_SPECIFIC_FIRST) than
 * the less specific (superclass) constraint. If both constraints are equivalent,
 * they are considered equally specific. Ifno subclass relationship can be determined,
 * an exception is thrown.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class HierarchyComparator implements Comparator<Constraint> {
    public static final int MOST_SPECIFIC_FIRST = -1;
    public static final int LEAST_SPECIFIC_FIRST = 1;
    
    private Logger logger = Logger.getLogger(IOntologies.LOGGER);
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
     * @see Comparator#compare(Object, Object)
     * @throws ClassCastException if no order can be determined.
     */
    public int compare(Constraint arg0, Constraint arg1)
    throws ClassCastException {
        IOntClass c0 = arg0.getOntClass();
        IOntClass c1 = arg1.getOntClass();
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
