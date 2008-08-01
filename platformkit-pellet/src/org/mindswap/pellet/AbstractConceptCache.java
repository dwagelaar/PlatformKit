package org.mindswap.pellet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Bool;

import aterm.ATermAppl;

/**
 * <p>
 * Title:
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
 * @author Evren Sirin
 */
public abstract class AbstractConceptCache implements ConceptCache {
	private int	maxSize;

	/**
	 * Creates an empty cache with at most <code>maxSize</code> elements which
	 * are neither named or negations of names.
	 * 
	 * @param maxSize
	 */
	public AbstractConceptCache(int maxSize) {
		this.maxSize = maxSize;
	}
	
	protected boolean isFull() {
		return size() == maxSize;
	}

	public Bool getSat(ATermAppl c) {
		CachedNode cached = get( c );
		return cached == null
			? Bool.UNKNOWN
			: Bool.create( !cached.isBottom() );
	}

	public boolean putSat(ATermAppl c, boolean isSatisfiable) {
		CachedNode cached = get( c );
		if( cached != null ) {
			if( isSatisfiable != !cached.isBottom() )
				throw new InternalReasonerException( "Caching inconsistent results for " + c );
			return false;
		}
		else if( isSatisfiable ) {
			put( c, CachedNode.createSatisfiableNode() );
		}
		else {
			ATermAppl notC = ATermUtils.negate( c );

			put( c, CachedNode.createBottomNode() );
			put( notC, CachedNode.createTopNode() );

		}

		return true;
	}

	public CachedNode remove(ATermAppl c, boolean removeComplete) {
		CachedNode cached = get( c );
		if( cached != null && (removeComplete || cached.isIncomplete()) )
			remove( c );

		return cached;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}
}
