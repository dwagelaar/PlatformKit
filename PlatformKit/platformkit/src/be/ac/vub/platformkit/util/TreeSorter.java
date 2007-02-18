package be.ac.vub.platformkit.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import junit.framework.Assert;
import be.ac.vub.platformkit.kb.Ontologies;

/**
 * Sorts the list by repeatedly removing the smallest elements ("root" elements).
 * Can deal with incomparable elements, since it only looks for elements that are
 * guaranteed smaller than the current element when comparing.
 * @author dennis
 */
public class TreeSorter {
    private Logger logger = Logger.getLogger(Ontologies.LOGGER);
    private Comparator comp;

    /**
     * Creates a ClusteredSorter.
     * @param comp
     * @throws IllegalArgumentException if comp is null
     */
    public TreeSorter(Comparator comp)
    throws IllegalArgumentException {
        if (comp == null) {
            throw new IllegalArgumentException("Null comparator");
        }
        this.comp = comp;
    }
    
    /**
     * Sorts the list by repeatedly removing the smallest elements ("root" elements).
     * @param list
     */
    public void sort(List list) {
        List sorted = new ArrayList();
        while (!list.isEmpty()) {
            List removed = removeRootElements(list);
            Assert.assertFalse(removed.isEmpty());
            sorted.addAll(removed);
        }
        list.addAll(sorted);
    }

    /**
     * Removes the root (i.e. smallest) elements from the list.
     * @param list
     * @return the root elements.
     */
    private List removeRootElements(List list) {
        List rootElements = new ArrayList();
        for (Iterator ls = list.iterator(); ls.hasNext();) {
            Object element = ls.next();
            if (isRootElement(element, list)) {
                rootElements.add(element);
                ls.remove();
            }
        }
        logger.info("Root elements removed: " + rootElements.toString());
        Assert.assertTrue(rootElements.size() >= Math.min(1, list.size()));
        return rootElements;
    }
    
    /**
     * @param obj
     * @param list
     * @return True if obj is a "root" element in list.
     */
    private boolean isRootElement(Object obj, List list) {
        for (Iterator ls = list.iterator(); ls.hasNext();) {
            Object element = ls.next();
            try {
                if (comp.compare(obj, element) > 0) {
                    logger.fine(obj + " not root; greater than " + element);
                    return false;
                }
            } catch (ClassCastException e) {
                logger.fine(obj + " and " + element + " not comparable");
            }
        }
        return true;
    }
}
