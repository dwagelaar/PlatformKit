// The MIT License
//
// Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

package org.mindswap.pellet.taxonomy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.output.OutputFormatter;
import org.mindswap.pellet.output.TaxonomyPrinter;
import org.mindswap.pellet.output.TreeTaxonomyPrinter;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Bool;

import aterm.ATermAppl;
import aterm.ATermList;

/*
 * Created on Aug 13, 2003
 */

/**
 * @author Evren Sirin
 */
public class Taxonomy {
	/**
	 * @deprecated Edit log4j.properties instead to turn on debugging
	 */
	public static boolean					DEBUG			= false;
	/**
	 * @deprecated Edit log4j.properties instead to turn on debugging
	 */
	public static boolean					DETAILED_DEBUG	= false;

	public static final Log					log				= LogFactory.getLog( Taxonomy.class );

	public static final boolean				SUB				= true;
	public static final boolean				SUPER			= false;

	public static final boolean				TOP_DOWN		= true;
	public static final boolean				BOTTOM_UP		= false;

	protected Map<ATermAppl, TaxonomyNode>	nodes;

	protected TaxonomyNode					TOP_NODE;
	protected TaxonomyNode					BOTTOM_NODE;

	protected TaxonomyPrinter				printer;

	private boolean							hideAnonTerms	= true;

	public Taxonomy() {
		this( null, false );
	}

	public Taxonomy(Collection<ATermAppl> classes) {
		this( classes, false );
	}
	
	public Taxonomy(boolean hideTopBottom) {
		this( null, hideTopBottom );
	}
	
	public Taxonomy(Collection<ATermAppl> classes, boolean hideTopBottom) {
		printer = new TreeTaxonomyPrinter();
		nodes = new HashMap<ATermAppl, TaxonomyNode>();

		TOP_NODE = addNode( ATermUtils.TOP );
		BOTTOM_NODE = addNode( ATermUtils.BOTTOM );
		TOP_NODE.setHidden( hideTopBottom );
		BOTTOM_NODE.setHidden( hideTopBottom );
		
		if( classes == null ) {
			TOP_NODE.addSub( BOTTOM_NODE );
		}
		else {
			ArrayList<TaxonomyNode> nodes = new ArrayList<TaxonomyNode>( classes.size() );
			for( ATermAppl c : classes ) {
				TaxonomyNode node = addNode( c );
				node.getSupers().add( TOP_NODE );
				node.getSubs().add( BOTTOM_NODE );
				nodes.add( node );
			}
			TOP_NODE.setSubs( nodes );
			BOTTOM_NODE.setSupers( (List) nodes.clone() );
		}
				
		// precaution to avoid creating an invalid taxonomy is now done by
		// calling assertValid function because the taxonomy might be invalid
		// during the merge operation but it is guaranteed to be valid after
		// the merge is completed. so we check for validity at the very end
		// TOP_NODE.setSupers( Collections.EMPTY_LIST );
		// BOTTOM_NODE.setSubs( Collections.EMPTY_LIST );
	}	

	public void assertValid() {
		assert TOP_NODE.getSupers().isEmpty() : "Top node in the taxonomy has parents";
		assert BOTTOM_NODE.getSubs().isEmpty() : "Bottom node in the taxonomy has children";
	}

	public TaxonomyNode getBottom() {
		return BOTTOM_NODE;
	}

	public TaxonomyNode getTop() {
		return TOP_NODE;
	}

	public Set<ATermAppl> getClasses() {
		return nodes.keySet();
	}

	public Collection<TaxonomyNode> getNodes() {
		return nodes.values();
	}

	public boolean contains(ATermAppl c) {
		return nodes.containsKey( c );
	}

	public TaxonomyNode addNode(ATermAppl c) {
		boolean hide = hideAnonTerms && !ATermUtils.isPrimitive( c );
		TaxonomyNode node = new TaxonomyNode( c, hide );
		nodes.put( c, node );

		return node;
	}

	public void addEquivalentNode(ATermAppl c, TaxonomyNode node) {
		boolean hide = !ATermUtils.isPrimitive( c );

		if( !hide )
			node.addEquivalent( c );

		nodes.put( c, node );
	}

	public TaxonomyNode getNode(ATermAppl c) {
		return nodes.get( c );
	}

	public void removeNode(TaxonomyNode node) {
		node.disconnect();

		nodes.remove( node.getName() );
	}

	/**
	 * Returns all the instances of concept c. If TOP concept is used every
	 * individual in the knowledge base will be returned
	 * 
	 * @param c
	 *            Class whose instances are returned
	 * @return A set of ATerm objects
	 */
	public Set<ATermAppl> getInstances(ATermAppl c) {
		return getInstances( c, false );
	}

	/**
	 * Returns the instances of class c. Depending on the second parameter the
	 * resulting list will include all or only the direct instances, i.e. if the
	 * individual is not type of any other class that is a subclass of c.
	 * 
	 * @param c
	 *            Class whose instances are found
	 * @param direct
	 *            If true return only the direct instances, otherwise return all
	 *            the instances
	 * @return A set of ATerm objects
	 */
	public Set<ATermAppl> getInstances(ATermAppl c, boolean direct) {
		TaxonomyNode node = nodes.get( c );

		if( node == null )
			throw new RuntimeException( c + " is an unknown class!" );

		Set<ATermAppl> instances = null;
		
		if( direct ) {
			instances = node.getInstances();
		}
		else {
			instances = new HashSet<ATermAppl>();
			getInstancesHelper( node, instances );
		}
		
		return instances;
	}
	
	private void getInstancesHelper( TaxonomyNode node, Set<ATermAppl> instances ) {
		instances.addAll( node.getInstances() );
		
		for( TaxonomyNode sub : node.getSubs() ) 
			getInstancesHelper( sub, instances );	
	}
	
	public boolean isType(ATermAppl ind, ATermAppl c) {
		TaxonomyNode node = nodes.get( c );

		if( node == null )
			throw new RuntimeException( c + " is an unknown class!" );

		return isType( node, ind );
	}
	
	private boolean isType( TaxonomyNode node, ATermAppl ind ) {
		if( node.getInstances().contains( ind ) )
			return true;
		
		for( TaxonomyNode sub : node.getSubs() ) 
			if( isType( sub, ind ) )
				return true;
		
		return false;
	}

	/**
	 * Get the set of explanations associated with a subsumption relationship
	 * 
	 * @param sub
	 *            Subclass ( sub [= sup )
	 * @param sup
	 *            Superclass
	 * @return Set of explanations, null if none are present or sub/sup not in
	 *         taxonomy
	 */
	public Set<Set<ATermAppl>> getSuperExplanations(ATermAppl sub, ATermAppl sup) {
		TaxonomyNode subNode = nodes.get( sub );
		if( subNode == null )
			return null;

		TaxonomyNode supNode = nodes.get( sup );
		if( supNode == null )
			return null;

		// if there are some equivalences involved then we cannot trust the
		// cached
		// explanation. this might be overly pessimistic but ensures correctness
		if( subNode.getEquivalents().size() > 1 || supNode.getEquivalents().size() > 1 )
			return null;

		return subNode.getSuperExplanations( supNode );
	}

	/**
	 * Checks if x is equivalent to y
	 * 
	 * @param x
	 *            Name of the first class
	 * @param y
	 *            Name of the second class
	 * @return true if x is equivalent to y
	 */
	public Bool isEquivalent(ATermAppl x, ATermAppl y) {
		TaxonomyNode nodeX = nodes.get( x );
		TaxonomyNode nodeY = nodes.get( y );

		if( nodeX == null || nodeY == null )
			return Bool.UNKNOWN;
		else if( nodeX.equals( nodeY ) )
			return Bool.TRUE;
		else
			return Bool.FALSE;
	}

	/**
	 * Checks if x has an ancestor y.
	 * 
	 * @param x
	 *            Name of the node
	 * @param y
	 *            Name of the ancestor ode
	 * @return true if x has an ancestor y
	 */
	public Bool isSubNodeOf(ATermAppl x, ATermAppl y) {
		TaxonomyNode nodeX = nodes.get( x );
		TaxonomyNode nodeY = nodes.get( y );

		if( nodeX == null || nodeY == null )
			return Bool.UNKNOWN;
		else if( nodeX.equals( nodeY ) )
			return Bool.TRUE;

		if( nodeX.isHidden() ) {
			if( nodeY.isHidden() )
				return Bool.UNKNOWN;
			else
				return getSupers( x, false, true ).contains( y )
					? Bool.TRUE
					: Bool.FALSE;
		}
		else
			return getSubs( y, false, true ).contains( x )
				? Bool.TRUE
				: Bool.FALSE;
	}

	/**
	 * Returns all the (named) subclasses of class c. The class c itself is not
	 * included in the list but all the other classes that are equivalent to c
	 * are put into the list. Also note that the returned list will always have
	 * at least one element, that is the BOTTOM concept. By definition BOTTOM
	 * concept is subclass of every concept. This function is equivalent to
	 * calling getSubClasses(c, true).
	 * 
	 * @param c
	 *            class whose subclasses are returned
	 * @return A set of sets, where each set in the collection represents an
	 *         equivalence class. The elements of the inner class are ATermAppl
	 *         objects.
	 */
	public Set<Set<ATermAppl>> getSubs(ATermAppl c) {
		return getSubs( c, false );
	}

	/**
	 * Returns the (named) subclasses of class c. Depending on the second
	 * parameter the resulting list will include either all subclasses or only
	 * the direct subclasses. A class d is a direct subclass of c iff
	 * <ol>
	 * <li>d is subclass of c</li>
	 * <li>there is no other class x different from c and d such that x is
	 * subclass of c and d is subclass of x</li>
	 * </ol>
	 * The class c itself is not included in the list but all the other classes
	 * that are sameAs c are put into the list. Also note that the returned list
	 * will always have at least one element. The list will either include one
	 * other concept from the hierarchy or the BOTTOM concept if no other class
	 * is subsumed by c. By definition BOTTOM concept is subclass of every
	 * concept.
	 * 
	 * @param c
	 *            Class whose subclasses are found
	 * @param direct
	 *            If true return only direct subclasses elese return all the
	 *            subclasses
	 * @return A set of sets, where each set in the collection represents an
	 *         equivalence class. The elements of the inner class are ATermAppl
	 *         objects.
	 */
	public Set<Set<ATermAppl>> getSubs(ATermAppl c, boolean direct) {
		return getSubSupers( c, direct, SUB );
	}

	/**
	 * Returns all the superclasses (implicitly or explicitly defined) of class
	 * c. The class c itself is not included in the list. but all the other
	 * classes that are sameAs c are put into the list. Also note that the
	 * returned list will always have at least one element, that is TOP concept.
	 * By definition TOP concept is superclass of every concept. This function
	 * is equivalent to calling getSuperClasses(c, true).
	 * 
	 * @param c
	 *            class whose superclasses are returned
	 * @return A set of sets, where each set in the collection represents an
	 *         equivalence class. The elements of the inner class are ATermAppl
	 *         objects.
	 */
	public Set<Set<ATermAppl>> getSupers(ATermAppl c) {
		return getSupers( c, false );
	}

	public Set getSupers(ATermAppl c, boolean direct, boolean flat) {
		return getSubSupers( c, direct, SUPER, flat );
	}

	public Set getSubs(ATermAppl c, boolean direct, boolean flat) {
		return getSubSupers( c, direct, SUB, flat );
	}

	/**
	 * Returns the (named) superclasses of class c. Depending on the second
	 * parameter the resulting list will include either all or only the direct
	 * superclasses. A class d is a direct superclass of c iff
	 * <ol>
	 * <li> d is superclass of c </li>
	 * <li> there is no other class x such that x is superclass of c and d is
	 * superclass of x </li>
	 * </ol>
	 * The class c itself is not included in the list but all the other classes
	 * that are sameAs c are put into the list. Also note that the returned list
	 * will always have at least one element. The list will either include one
	 * other concept from the hierarchy or the TOP concept if no other class
	 * subsumes c. By definition TOP concept is superclass of every concept.
	 * 
	 * @param c
	 *            Class whose subclasses are found
	 * @param direct
	 *            If true return all the superclasses else return only direct
	 *            superclasses
	 * @return A set of sets, where each set in the collection represents an
	 *         equivalence class. The elements of the inner class are ATermAppl
	 *         objects.
	 */
	public Set<Set<ATermAppl>> getSupers(ATermAppl c, boolean direct) {
		return getSubSupers( c, direct, SUPER );
	}

	public Set getSubSupers(ATermAppl c, boolean direct, boolean subOrSuper, boolean flat) {
		if( flat )
			return getFlattenedSubSupers( c, direct, subOrSuper );
		else
			return getSubSupers( c, direct, subOrSuper );
	}

	public Set<Set<ATermAppl>> getSubSupers(ATermAppl c, boolean direct, boolean subOrSuper) {
		TaxonomyNode node = nodes.get( c );

		if( node == null )
			return Collections.emptySet();

		Set<Set<ATermAppl>> result = new HashSet<Set<ATermAppl>>();

		List<TaxonomyNode> visit = new ArrayList<TaxonomyNode>();
		visit.addAll( (subOrSuper == SUB)
			? node.getSubs()
			: node.getSupers() );

		for( int i = 0; i < visit.size(); i++ ) {
			node = visit.get( i );

			if( node.isHidden() )
				continue;

			Set<ATermAppl> add = new HashSet<ATermAppl>( node.getEquivalents() );
			if( hideAnonTerms ) {
				removeAnonTerms( add );
			}
			if( !add.isEmpty() ) {
				result.add( add );
			}

			if( !direct )
				visit.addAll( (subOrSuper == SUB)
					? node.getSubs()
					: node.getSupers() );
		}

		return result;
	}

	public Set<ATermAppl> getFlattenedSubSupers(ATermAppl c, boolean direct, boolean subOrSuper) {
		TaxonomyNode node = nodes.get( c );

		Set<ATermAppl> result = new HashSet<ATermAppl>();

		List<TaxonomyNode> visit = new ArrayList<TaxonomyNode>();
		visit.addAll( (subOrSuper == SUB)
			? node.getSubs()
			: node.getSupers() );

		for( int i = 0; i < visit.size(); i++ ) {
			node = visit.get( i );

			if( node.isHidden() )
				continue;

			Set<ATermAppl> add = node.getEquivalents();
			result.addAll( add );

			if( !direct )
				visit.addAll( (subOrSuper == SUB)
					? node.getSubs()
					: node.getSupers() );
		}
		
		if( hideAnonTerms ) {
			removeAnonTerms( result );
		}

		return result;
	}

	/**
	 * Returns all the classes that are equivalent to class c. Class c itself is
	 * NOT included in the result.
	 * 
	 * @param c
	 *            class whose equivalent classes are found
	 * @return A set of ATerm objects
	 */
	public Set<ATermAppl> getEquivalents(ATermAppl c) {
		TaxonomyNode node = nodes.get( c );

		if( node == null )
			throw new RuntimeException( c + " is an unknown class!" );

		if( node.isHidden() )
			return Collections.emptySet();

		Set<ATermAppl> result = new HashSet<ATermAppl>( node.getEquivalents() );
		result.remove( c );
		if( hideAnonTerms ) {
			removeAnonTerms( result );
		}

		return result;
	}

	/**
	 * Returns all the classes that are equivalent to class c. Class c itself is
	 * included in the result.
	 * 
	 * @param c
	 *            class whose equivalent classes are found
	 * @return A set of ATerm objects
	 */
	public Set<ATermAppl> getAllEquivalents(ATermAppl c) {
		TaxonomyNode node = nodes.get( c );

		if( node == null )
			throw new RuntimeException( c + " is an unknown class!" );

		if( node.isHidden() )
			return Collections.emptySet();

		Set<ATermAppl> result = new HashSet<ATermAppl>( node.getEquivalents() );
		if( hideAnonTerms ) {
			removeAnonTerms( result );
		}

		return result;
	}

	private void removeAnonTerms(Set<ATermAppl> terms) {
		for( Iterator i = terms.iterator(); i.hasNext(); ) {
			ATermAppl term = (ATermAppl) i.next();
			if( !ATermUtils.isPrimitive( term ) && term != ATermUtils.BOTTOM )
				i.remove();
		}
	}

	/**
	 * Get all the direct classes individual belongs to.
	 * 
	 * @param ind
	 *            An individual name
	 * @return A set of sets, where each set in the collection represents an
	 *         equivalence class. The elements of the inner class are ATermAppl
	 *         objects.
	 */
	public Set<Set<ATermAppl>> getDirectTypes(ATermAppl ind) {
		Set<Set<ATermAppl>> result = new HashSet<Set<ATermAppl>>();

		for( TaxonomyNode node : nodes.values() ) {
			if( node.getInstances().contains( ind ) )
				result.add( node.getEquivalents() );
		}

		return result;
	}

	/**
	 * Get all the named classes individual belongs to. The result is returned
	 * as a set of sets where each
	 * 
	 * @param ind
	 *            An individual name
	 * @return A set of sets, where each set in the collection represents an
	 *         equivalence class. The elements of the inner class are ATermAppl
	 *         objects.
	 */
	public Set<Set<ATermAppl>> getTypes(ATermAppl ind) {
		Set<Set<ATermAppl>> result = new HashSet<Set<ATermAppl>>();

		for( Iterator i = nodes.values().iterator(); i.hasNext(); ) {
			TaxonomyNode node = (TaxonomyNode) i.next();

			if( node.getInstances().contains( ind ) ) {
				result.add( node.getEquivalents() );

				Set<Set<ATermAppl>> supers = getSupers( node.getName() );
				result.addAll( supers );
			}

		}

		return result;
	}

	/**
	 * Returns the classes individual belongs to. Depending on the second
	 * parameter the resulting list will include either all types or only the
	 * direct types.
	 * 
	 * @param ind
	 *            An individual name
	 * @param direct
	 *            If true return only the direct types, otherwise return all
	 *            types
	 * @return A set of sets, where each set in the collection represents an
	 *         equivalence class. The elements of the inner class are ATermAppl
	 *         objects.
	 */
	public Set<Set<ATermAppl>> getTypes(ATermAppl ind, boolean direct) {
		if( direct )
			return getDirectTypes( ind );
		else
			return getTypes( ind );
	}

	/**
	 * Sort the nodes in the taxonomy using topological ordering starting from
	 * top to bottom.
	 * 
	 * @param includeEquivalents
	 *            If false the equivalents in a node will be ignored and only
	 *            the name of the node will be added to the result
	 * @return List of node names sorted in topological ordering
	 */
	public List<ATermAppl> topologocialSort(boolean includeEquivalents) {
		Map<TaxonomyNode, Integer> degrees = new HashMap<TaxonomyNode, Integer>();
		Set<TaxonomyNode> nodesPending = new LinkedHashSet<TaxonomyNode>();
		Set<TaxonomyNode> nodesLeft = new HashSet<TaxonomyNode>();
		List<ATermAppl> nodesSorted = new ArrayList<ATermAppl>();

		log.debug( "Topological sort..." );

		for( Iterator i = nodes.values().iterator(); i.hasNext(); ) {
			TaxonomyNode node = (TaxonomyNode) i.next();
			nodesLeft.add( node );
			int degree = node.getSupers().size();
			if( degree == 0 ) {
				nodesPending.add( node );
				degrees.put( node, 0 );
			}
			else
				degrees.put( node, new Integer( degree ) );
		}

		if( nodesPending.size() != 1 )
			throw new InternalReasonerException( "More than one node with no incoming edges "
					+ nodesPending );

		for( int i = 0, size = nodesLeft.size(); i < size; i++ ) {
			if( nodesPending.isEmpty() )
				throw new InternalReasonerException( "Cycle detected in the taxonomy!" );

			TaxonomyNode node = nodesPending.iterator().next();

			int deg = degrees.get( node );
			if( deg != 0 )
				throw new InternalReasonerException( "Cycle detected in the taxonomy " + node + " "
						+ deg + " " + nodesSorted.size() + " " + nodes.size() );

			nodesPending.remove( node );
			nodesLeft.remove( node );
			if( includeEquivalents )
				nodesSorted.addAll( node.getEquivalents() );
			else
				nodesSorted.add( node.getName() );

			for( Iterator j = node.getSubs().iterator(); j.hasNext(); ) {
				TaxonomyNode sub = (TaxonomyNode) j.next();
				int degree = degrees.get( sub );
				if( degree == 1 ) {
					nodesPending.add( sub );
					degrees.put( sub, 0 );
				}
				else
					degrees.put( sub, degree - 1 );
			}
		}

		if( !nodesLeft.isEmpty() )
			throw new InternalReasonerException( "Failed to sort elements: " + nodesLeft );

		log.debug( "done" );

		return nodesSorted;
	}

	/**
	 * Walk through the super nodes of the given node and when a cycle is
	 * detected merge all the nodes in that path
	 * 
	 * @param node
	 */
	public void removeCycles(TaxonomyNode node) {
		if( !nodes.get( node.getName() ).equals( node ) )
			throw new InternalReasonerException( "This node does not exist in the taxonomy: "
					+ node.getName() );
		removeCycles( node, new ArrayList<TaxonomyNode>() );
	}

	/**
	 * Given a node and (a possibly empty) path of sub nodes, remove cycles by
	 * merging all the nodes in the path.
	 * 
	 * @param node
	 * @param path
	 * @return
	 */
	private boolean removeCycles(TaxonomyNode node, List<TaxonomyNode> path) {
		// cycle detected
		if( path.contains( node ) ) {
			mergeNodes( path );
			return true;
		}
		else {
			// no cycle yet, add this node to the path and continue
			path.add( node );

			List supers = node.getSupers();
			for( int i = 0; i < supers.size(); ) {
				TaxonomyNode sup = (TaxonomyNode) supers.get( i );
				// remove cycles involving super node
				removeCycles( sup, path );
				// remove the super from the path
				path.remove( sup );
				// if the super has been removed then no need
				// to increment the index
				if( i < supers.size() && supers.get( i ).equals( sup ) )
					i++;
			}
			return false;
		}
	}

	public void merge(TaxonomyNode node1, TaxonomyNode node2) {
		List<TaxonomyNode> mergeList = new ArrayList<TaxonomyNode>( 2 );
		mergeList.add( node1 );
		mergeList.add( node2 );

		TaxonomyNode node = mergeNodes( mergeList );

		removeCycles( node );
	}

	private TaxonomyNode mergeExternal(TaxonomyNode node1, TaxonomyNode node2) {
		List<TaxonomyNode> mergeList = new ArrayList<TaxonomyNode>( 2 );
		mergeList.add( node1 );
		mergeList.add( node2 );
		TaxonomyNode mergedNode = mergeNodesExternal( mergeList );
		return mergedNode;
	}

	private TaxonomyNode mergeNodesExternal(List mergeList) {
		if( log.isTraceEnabled() )
			log.trace( "Merge " + mergeList );

		if( mergeList.size() == 1 )
			log.warn( "Merge one node?" );

		TaxonomyNode node = null;
		if( mergeList.contains( TOP_NODE ) ) {
			node = TOP_NODE;
		}
		else if( mergeList.contains( BOTTOM_NODE ) ) {
			node = BOTTOM_NODE;
		}
		else
			node = (TaxonomyNode) mergeList.get( 0 );

		Set<TaxonomyNode> merged = new HashSet<TaxonomyNode>();
		merged.add( node );

		for( Iterator i = mergeList.iterator(); i.hasNext(); ) {
			TaxonomyNode other = (TaxonomyNode) i.next();

			if( merged.contains( other ) )
				continue;
			else
				merged.add( other );

			for( Iterator j = other.getSubs().iterator(); j.hasNext(); ) {
				TaxonomyNode sub = (TaxonomyNode) j.next();
				if( !mergeList.contains( sub ) )
					node.addSub( sub );
			}

			for( Iterator j = other.getSupers().iterator(); j.hasNext(); ) {
				TaxonomyNode sup = (TaxonomyNode) j.next();
				if( !mergeList.contains( sup ) )
					sup.addSub( node );
			}

			// removeNode( other );

			for( Iterator j = other.getEquivalents().iterator(); j.hasNext(); ) {
				ATermAppl c = (ATermAppl) j.next();
				addEquivalentNode( c, node );
			}
		}
		return node;
	}

	private TaxonomyNode mergeNodes(List<TaxonomyNode> mergeList) {
		if( log.isTraceEnabled() )
			log.trace( "Merge " + mergeList );

		if( mergeList.size() == 1 )
			log.warn( "Merge one node?" );

		TaxonomyNode node = null;
		if( mergeList.contains( TOP_NODE ) ) {
			node = TOP_NODE;
		}
		else if( mergeList.contains( BOTTOM_NODE ) ) {
			node = BOTTOM_NODE;
		}
		else
			node = mergeList.get( 0 );

		Set<TaxonomyNode> merged = new HashSet<TaxonomyNode>();
		merged.add( node );

		for( TaxonomyNode other : mergeList ) {

			if( merged.contains( other ) )
				continue;
			else
				merged.add( other );

			for( TaxonomyNode sub : other.getSubs() ) {
				if( !mergeList.contains( sub ) )
					node.addSub( sub );
			}

			for( TaxonomyNode sup : other.getSupers() ) {
				if( !mergeList.contains( sup ) ) {
					sup.addSub( node );
					Set<Set<ATermAppl>> exps = other.getSuperExplanations( sup );
					if( exps != null )
						for( Set<ATermAppl> exp : exps )
							node.addSuperExplanation( sup, exp );
				}
			}

			removeNode( other );

			for( ATermAppl c : other.getEquivalents() ) {
				addEquivalentNode( c, node );
			}

		}

		return node;
	}

	/**
	 * Given a list of concepts, find all the Least Common Ancestors (LCA). Note
	 * that a taxonomy is DAG not a tree so we do not have a unique LCA but a
	 * set of LCA. The function might return a singleton list that contains TOP
	 * if there are no lower level nodes that satisfy the LCA condition.
	 * 
	 * @param names
	 * @return
	 */
	public List computeLCA(ATermList list) {
		// FIXME does not work when one of the elements is an ancestor of the
		// rest
		// TODO what to do with equivalent classes?
		// TODO improve efficiency

		// argument to retrieve all supers (not just direct ones)
		boolean allSupers = false;
		// argument to retrieve supers in a flat set
		boolean flat = true;

		// get the first concept
		ATermAppl c = (ATermAppl) list.getFirst();

		// add all its ancestor as possible LCA candidates
		List ancestors = new ArrayList( getSupers( c, allSupers, flat ) );

		for( ; !list.isEmpty(); list = list.getNext() ) {
			c = (ATermAppl) list.getFirst();

			// take the intersection of possible candidates to get rid of
			// uncommon ancestors
			ancestors.retainAll( getSupers( c, allSupers, flat ) );

			// we hit TOP so no need to continue
			if( ancestors.size() == 1 ) {
				ATermUtils.assertTrue( ancestors.contains( ATermUtils.TOP ) );
				return ancestors;
			}
		}

		Set toBeRemoved = new HashSet();

		// we have all common ancestors now remove the ones that have
		// descendants in the list
		for( Iterator i = ancestors.iterator(); i.hasNext(); ) {
			c = (ATermAppl) i.next();

			if( toBeRemoved.contains( c ) )
				continue;

			Set supers = getSupers( c, allSupers, flat );
			toBeRemoved.addAll( supers );
		}

		ancestors.removeAll( toBeRemoved );

		return ancestors;
	}

	public void print() {
		printer.print( this );
	}

	public void print(OutputFormatter out) {
		printer.print( this, out );
	}

	public Taxonomy merge(Taxonomy tax) {
		Taxonomy newtax = new Taxonomy();
		Map conversion = new HashMap();
		newtax = (Taxonomy) tax.clone();
		Map auxMap = new HashMap();
		Iterator i = this.nodes.keySet().iterator();
		while( i.hasNext() ) {
			ATermAppl a = (ATermAppl) i.next();
			if( !newtax.contains( a ) ) {
				TaxonomyNode oldnode = (TaxonomyNode) nodes.get( a );
				TaxonomyNode newnode;
				newnode = oldnode.copy( conversion );
				newtax.nodes.put( a, newnode );
			}
			else {
				TaxonomyNode oldnode2 = (TaxonomyNode) newtax.nodes.get( a );
				TaxonomyNode oldnode = (TaxonomyNode) nodes.get( a );
				TaxonomyNode newnode = mergeExternal( oldnode, oldnode2 );
				auxMap.put( a, newnode );

			}
		}
		Iterator it = auxMap.keySet().iterator();
		while( it.hasNext() ) {
			ATermAppl a = (ATermAppl) it.next();
			TaxonomyNode oldnode = (TaxonomyNode) auxMap.get( a );
			newtax.nodes.remove( a );
			newtax.nodes.put( a, oldnode );
		}
		return newtax;
	}

	public int compareTaxonomy(Taxonomy tax) {
		int discrepancies = 0;
		Iterator i = this.nodes.keySet().iterator();
		while( i.hasNext() ) {
			ATermAppl a = (ATermAppl) i.next();
			if( !tax.contains( a ) ) {
				return discrepancies++;
			}
			else {
				TaxonomyNode node1 = (TaxonomyNode) nodes.get( a );
				TaxonomyNode node2 = (TaxonomyNode) tax.nodes.get( a );
				if( !node1.compareTo( node2 ) )
					discrepancies++;
			}
		}
		return discrepancies;
	}

	public Object clone() {
		Map conversion = new HashMap();
		Iterator i = nodes.keySet().iterator();
		Taxonomy newtax = new Taxonomy();
		newtax.nodes = new HashMap();
		while( i.hasNext() ) {
			ATermAppl a = (ATermAppl) i.next();
			TaxonomyNode oldnode = (TaxonomyNode) nodes.get( a );
			TaxonomyNode newnode;
			if( conversion.containsKey( oldnode ) ) {
				newnode = (TaxonomyNode) conversion.get( oldnode );
				oldnode.copy( newnode, conversion );
			}
			else {
				newnode = oldnode.copy( conversion );
			}
			newtax.nodes.put( a, newnode );
		}
		newtax.TOP_NODE = (TaxonomyNode) conversion.get( TOP_NODE );
		newtax.BOTTOM_NODE = (TaxonomyNode) conversion.get( BOTTOM_NODE );
		return newtax;
	}

	public boolean isHideAnonTerms() {
		return hideAnonTerms;
	}

	public void setHideAnonTerms(boolean hideAnonTerms) {
		this.hideAnonTerms = hideAnonTerms;
	}
}
