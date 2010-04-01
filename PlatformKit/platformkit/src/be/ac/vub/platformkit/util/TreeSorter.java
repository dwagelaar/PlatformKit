/*******************************************************************************
 * Copyright (c) 2005-2010 Dennis Wagelaar, Vrije Universiteit Brussel.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dennis Wagelaar, Vrije Universiteit Brussel
 *******************************************************************************/
package be.ac.vub.platformkit.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import be.ac.vub.platformkit.PlatformkitResources;
import be.ac.vub.platformkit.logging.PlatformkitLogger;

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
public class TreeSorter<T> {

	private Comparator<T> comp;

	/**
	 * Creates a ClusteredSorter.
	 * @param comp
	 * @throws IllegalArgumentException if comp is null
	 */
	public TreeSorter(Comparator<T> comp)
	throws IllegalArgumentException {
		if (comp == null) {
			throw new IllegalArgumentException(
					PlatformkitResources.getString("TreeSorter.nullComparator")); //$NON-NLS-1$
		}
		this.comp = comp;
	}

	/**
	 * Sorts the list by repeatedly removing the smallest elements ("root" elements).
	 * @param list
	 */
	public void sort(List<T> list) {
		List<T> sorted = new ArrayList<T>();
		while (!list.isEmpty()) {
			T removed = removeRootElement(list);
			Assert.assertNotNull(
					PlatformkitResources.getString("TreeSorter.removeAssertion"), 
					removed); //$NON-NLS-1$
			sorted.add(removed);
		}
		list.addAll(sorted);
	}

	/**
	 * Removes the root (i.e. smallest) element from the list.
	 * @param list
	 * @return the root element.
	 */
	private T removeRootElement(List<T> list) {
		for (Iterator<T> ls = list.iterator(); ls.hasNext();) {
			T element = ls.next();
			if (isRootElement(element, list)) {
				ls.remove();
				PlatformkitLogger.logger.info(String.format(
						PlatformkitResources.getString("TreeSorter.rootElementRemoved"), 
						element)); //$NON-NLS-1$
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
	private boolean isRootElement(T obj, List<T> list) {
		for (Iterator<T> ls = list.iterator(); ls.hasNext();) {
			T element = ls.next();
			try {
				if (comp.compare(obj, element) > 0) {
					PlatformkitLogger.logger.fine(String.format(
							PlatformkitResources.getString("TreeSorter.isNotRoot"), 
							obj, 
							element)); //$NON-NLS-1$
					return false;
				}
			} catch (ClassCastException e) {
				PlatformkitLogger.logger.fine(String.format(
						PlatformkitResources.getString("TreeSorter.notComparable"), 
						obj, 
						element)); //$NON-NLS-1$
			}
		}
		return true;
	}
}
