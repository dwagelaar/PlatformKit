package be.ac.vub.platformkit.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import junit.framework.Assert;
import be.ac.vub.platformkit.kb.Ontologies;

/**
 * Sorts the list by repeatedly removing the first smallest element ("root" element).
 * Can deal with incomparable elements (partially ordered lists), since it only looks
 * for elements that are guaranteed smaller than the current element when comparing.
 * Preserves existing list order where possible. 
 * 
 * In Java, the Arrays.sort() methods use mergesort or a tuned quicksort depending
 * on the datatypes and for implementation efficiency switch to insertion sort when
 * fewer than seven array elements are being sorted. These algorithms are only
 * applicable to totally ordered lists, however.
 * 
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
            Object removed = removeRootElement(list);
            Assert.assertNotNull("Remove at least one element == false", removed);
            sorted.add(removed);
        }
        list.addAll(sorted);
    }

    /**
     * Removes the root (i.e. smallest) element from the list.
     * @param list
     * @return the root element.
     */
    private Object removeRootElement(List list) {
        for (Iterator ls = list.iterator(); ls.hasNext();) {
            Object element = ls.next();
            if (isRootElement(element, list)) {
                ls.remove();
                logger.info("Root element removed: " + element);
                return element;
            }
        }
        return null;
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
