package org.mindswap.pellet;

import java.util.Map;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Bool;

import aterm.ATermAppl;

/**
 * <p>
 * Title: Concept Cache
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 * 
 * @author Ron Alford
 */
public interface ConceptCache extends Map<ATermAppl, CachedNode> {
	/**
	 * Get the maximum number of non-primitive concepts allowed in the cache
	 * 
	 * @return
	 */
	public int getMaxSize();

	/**
	 * Set the maximum number of non-primitive concepts allowed in the cache
	 * 
	 * @return
	 */
	public void setMaxSize(int maxSize);

	/**
	 * Get the satisfiability status of a concept as a three-value boolean.
	 * 
	 * @param c
	 * @return
	 */
	public Bool getSat(ATermAppl c);

	/**
	 * Put an incomplete
	 * 
	 * @param c
	 * @param isSatisfiable
	 * @return
	 */
	public boolean putSat(ATermAppl c, boolean isSatisfiable);

	/**
	 * Remove the cached model for this concept.
	 * 
	 * @param c
	 * @param removeComplete
	 *            If false only incomplete models will be removed
	 * @return
	 */
	public Object remove(ATermAppl c, boolean removeComplete);
}
