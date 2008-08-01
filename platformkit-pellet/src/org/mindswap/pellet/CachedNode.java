/*
 * Created on Oct 1, 2005
 */
package org.mindswap.pellet;

import org.mindswap.pellet.utils.ATermUtils;

/**
 * @author Evren Sirin
 */
public class CachedNode {
	// nodes used for cached root for Top and Bottom concepts
	public static final Individual	TOP_IND		= new Individual( ATermUtils.TOP );
	public static final Individual	BOTTOM_IND	= new Individual( ATermUtils.BOTTOM );
	public static final Individual	DUMMY_IND	= new Individual( ATermUtils
														.makeTermAppl( "_DUMMY_" ) );

	Individual						node;
	DependencySet					depends;

	private CachedNode(Individual node, DependencySet depends) {
		this.node = node;
		this.depends = depends.copy();
	}

	public static CachedNode createTopNode() {
		return new CachedNode( TOP_IND, DependencySet.INDEPENDENT );
	}

	public static CachedNode createBottomNode() {
		return new CachedNode( BOTTOM_IND, DependencySet.INDEPENDENT );
	}

	public static CachedNode createSatisfiableNode() {
		return new CachedNode( DUMMY_IND, DependencySet.INDEPENDENT );
	}

	public static CachedNode createNode(Individual node, DependencySet depends) {
		return new CachedNode( node, depends );
	}

	public boolean isIncomplete() {
		return node == DUMMY_IND;
	}

	public boolean isComplete() {
		return node != DUMMY_IND;
	}

	public boolean isTop() {
		return node == TOP_IND;
	}

	public boolean isBottom() {
		return node == BOTTOM_IND;
	}

	public String toString() {
		return node + " " + depends;
	}
}
