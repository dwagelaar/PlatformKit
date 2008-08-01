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

/*
 * Created on Jan 3, 2005
 */
package org.mindswap.pellet.jena;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphListener;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.compose.MultiUnion;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * An extension to MultiUnion graph that may return repeated triples from find
 * function. When big composite graphs are being loaded to Pellet, filtering
 * results to return unique triples is slowing things down whereas having
 * triples repeated has less impact on the performance.
 * 
 * @author Evren Sirin
 */
public class DisjointMultiUnion extends MultiUnion implements GraphListener {
	private boolean			listenChanges;
	private boolean			deletion	= false;

	private Set				changedGraphs;

	// flag for incremental changes - currently only ABox additions
	// are supported and type assertions must be atomic
	private boolean			unsupportedChange;

	// KB object - used for incremental ABox changes
	private KnowledgeBase	kb;

	// OWLLoader object - used for incremental ABox changes
	private OWLLoader		loader;

	public DisjointMultiUnion() {
		this( false, null, null );
	}

	public DisjointMultiUnion(boolean listenChanges, KnowledgeBase kb, OWLLoader loader) {
		super();

		changedGraphs = new HashSet();
		this.listenChanges = listenChanges;

		this.kb = kb;
		unsupportedChange = false;
		this.loader = loader;
	}

	public DisjointMultiUnion(Graph graph) {
		this( true, null, null );

		addGraph( graph );
	}

	public DisjointMultiUnion(Graph graph, KnowledgeBase kb, OWLLoader loader) {
		this( false, kb, loader );

		addGraph( graph );
	}

	// public DisjointMultiUnion(Graph[] graphs) {
	// for (int i = 0; i < graphs.length; i++) {
	// addGraph( graphs[i] );
	// }
	// }

	public boolean isEmpty() {
		for( Iterator i = m_subGraphs.iterator(); i.hasNext(); ) {
			if( !((Graph) i.next()).isEmpty() ) {
				return false;
			}
		}

		return true;
	}

	public ExtendedIterator graphBaseFind(TripleMatch t) {
		Iterator graphs = m_subGraphs.iterator();

		if( !graphs.hasNext() )
			return NullIterator.instance;

		// create a MultiIterator with the first graph's results
		Graph firstG = (Graph) graphs.next();
		ExtendedIterator i = new MultiIterator( firstG.find( t ) );

		// now add the rest of the chain
		while( graphs.hasNext() ) {
			Graph nextG = (Graph) graphs.next();
			i = i.andThen( nextG.find( t ) );
		}

		// this graph does not support .remove function
		return i; // UniqueExtendedIterator.create( i );
	}

	public void addGraph(Graph graph) {
		if( !m_subGraphs.contains( graph ) ) {
			if( graph instanceof MultiUnion ) {
				MultiUnion union = ((MultiUnion) graph);
				if( union.getBaseGraph() != null )
					addGraph( union.getBaseGraph() );

				for( Iterator i = union.getSubGraphs().iterator(); i.hasNext(); )
					addGraph( (Graph) i.next() );
			}
			else if( graph instanceof InfGraph ) {
				addGraph( ((InfGraph) graph).getRawGraph() );
			}
			else {
				m_subGraphs.add( graph );
				if( listenChanges )
					graph.getEventManager().register( this );
			}
		}
	}

	public void releaseListeners() {
		for( Iterator graphs = m_subGraphs.iterator(); graphs.hasNext(); ) {
			Graph graph = (Graph) graphs.next();
			graph.getEventManager().unregister( this );
		}
	}

	public void notifyAddTriple(Graph g, Triple t) {
		// check that incremental updates are supported
		boolean canUseInc = canUseIncAdd()
			? isABoxChange( t )
			: false;

		if( canUseInc ) {
			addABoxChange( t );
		}
		else
			changedGraphs.add( g );
	}

	public void notifyAddArray(Graph g, Triple[] triples) {
		// check that incremental updates are supported
		boolean canUseInc = canUseIncAdd()
			? isABoxChange( triples )
			: false;

		if( canUseInc ) {
			for( int i = 0; i < triples.length; i++ )
				addABoxChange( triples[i] );
		}
		else
			changedGraphs.add( g );
	}

	public void notifyAddList(Graph g, List triples) {
		// check that incremental updates are supported
		boolean canUseInc = canUseIncAdd()
			? isABoxChange( triples )
			: false;

		if( canUseInc ) {
			for( int i = 0; i < triples.size(); i++ )
				addABoxChange( (Triple) triples.get( i ) );
		}
		else
			changedGraphs.add( g );
	}

	public void notifyAddIterator(Graph g, Iterator it) {
		// check that incremental updates are supported
		boolean canUseInc = canUseIncAdd()
			? isABoxChange( it )
			: false;

		if( canUseInc ) {
			while( it.hasNext() )
				addABoxChange( (Triple) it.next() );
		}
		else
			changedGraphs.add( g );
	}

	public void notifyAddGraph(Graph g, Graph added) {
		// check that incremental updates are supported
		boolean canUseInc = canUseIncAdd()
			? isABoxChange( added )
			: false;

		if( canUseInc ) {
			for( Iterator i = added.find( Triple.ANY ); i.hasNext(); )
				addABoxChange( (Triple) i.next() );
		}
		else
			changedGraphs.add( g );
	}

	public void notifyDeleteTriple(Graph g, Triple t) {
		deletion = true;
	}

	public void notifyDeleteList(Graph g, List L) {
		deletion |= !L.isEmpty();
	}

	public void notifyDeleteArray(Graph g, Triple[] triples) {
		deletion |= (triples.length > 0);
	}

	public void notifyDeleteIterator(Graph g, Iterator it) {
		deletion |= it.hasNext();
	}

	public void notifyDeleteGraph(Graph g, Graph removed) {
		deletion = true;
	}

	public void notifyEvent(Graph source, Object value) {
		deletion = true;
	}

	public boolean isStatementDeleted() {
		return deletion;
	}

	public void resetChanged() {
		// reset change flag if the kb exists
		unsupportedChange = false;

		deletion = false;
		changedGraphs.clear();
	}

	public DisjointMultiUnion minus(DisjointMultiUnion other) {
		if( !m_subGraphs.containsAll( other.m_subGraphs ) )
			return null;

		DisjointMultiUnion diff = new DisjointMultiUnion();
		for( Iterator graphs = m_subGraphs.iterator(); graphs.hasNext(); ) {
			Graph g = (Graph) graphs.next();
			if( !other.m_subGraphs.contains( g ) || other.changedGraphs.contains( g ) )
				diff.addGraph( g );
		}

		return diff;
	}

	/**
	 * Checks if the given triple is an ABox assertion. Currently, only type
	 * assertions with atomic concepts are detected and property assertions
	 * 
	 * @param t
	 * @return
	 */
	public boolean isABoxChange(Triple t) {
		Node o = t.getObject();
		Node p = t.getPredicate();

		// detect if this is a supported ABox type assertion
		if( p.getURI().equals( RDF.type.toString() ) ) {
			// check if the object is a bnode to detect complex concepts
			if( o.isBlank() ) {
				unsupportedChange = true;
				return false;
			}

			// check that the object is an atomic concept that exists in the KB
			ATermAppl object = ATermUtils.makeTermAppl( o.getURI() );
			if( !kb.getClasses().contains( object ) ) {
				unsupportedChange = true;
				return false;
			}

			// Note: we do not check if the subject already exists,
			// as it could be a newly added individual

		}
		else {
			// detect ABox property assertions
			ATermAppl prop = ATermUtils.makeTermAppl( p.getURI() );

			// check if the role is this is a defined role
			if( !kb.isProperty( prop ) ) {
				unsupportedChange = true;
				return false;
			}

			// Note: we do not check if the subject and object already exists,
			// as they
			// could be a newly added individuals
		}

		return true;
	}

	/**
	 * Check if the array of triples are all ABox assertions Currently, only
	 * type assertions with atomic concepts are detected
	 * 
	 * @param triples
	 * @return
	 */
	public boolean isABoxChange(Triple[] triples) {
		boolean satisfied = true;

		// inspect each triple and check if it is an abox assertion
		for( int i = 0; i < triples.length; i++ ) {
			Triple t = triples[i];

			if( !isABoxChange( t ) ) {
				satisfied = false;
				break;
			}
		}

		return satisfied;
	}

	/**
	 * Check if the list of triples are all ABox assertions Currently, only type
	 * assertions with atomic concepts are detected
	 * 
	 * @param triples
	 * @return
	 */
	public boolean isABoxChange(List triples) {
		boolean satisfied = true;

		// inspect each triple and check if it is an abox assertion
		for( int i = 0; i < triples.size(); i++ ) {
			Triple t = (Triple) triples.get( i );

			if( !isABoxChange( t ) ) {
				satisfied = false;
				break;
			}
		}

		return satisfied;
	}

	/**
	 * Check if the iterator of triples are all ABox assertions Currently, only
	 * type assertions with atomic concepts are detected
	 * 
	 * @param triples
	 * @return
	 */
	public boolean isABoxChange(Iterator triples) {
		boolean satisfied = true;

		// inspect each triple and check if it is an abox assertion
		while( triples.hasNext() ) {
			Triple t = (Triple) triples.next();

			if( !isABoxChange( t ) ) {
				satisfied = false;
				break;
			}
		}

		return satisfied;
	}

	/**
	 * Check if the iterator of triples are all ABox assertions Currently, only
	 * type assertions with atomic concepts are detected
	 * 
	 * @param triples
	 * @return
	 */
	public boolean isABoxChange(Graph graph) {
		boolean satisfied = true;

		// inspect each triple and check if it is an abox assertion
		for( Iterator i = graph.find( Triple.ANY ); i.hasNext(); ) {
			Triple t = (Triple) i.next();

			if( !isABoxChange( t ) ) {
				satisfied = false;
				break;
			}
		}

		return satisfied;
	}

	/**
	 * Checks flags for incremental adding to the KB
	 * 
	 * @return
	 */
	public boolean canUseIncAdd() {
		// check if a previous triple has been received that was not an abox
		// change and that
		// kb and loader have been set
		if( kb == null || unsupportedChange == true || loader == null )
			return false;

		return true;
	}

	/**
	 * Adds an ABox change to the KB
	 * 
	 * @param t
	 */
	public void addABoxChange(Triple t) {
		// Convert the Jena nodes to ATermAppl
		ATermAppl s = loader.node2term( t.getSubject() );
		ATermAppl p = loader.node2term( t.getPredicate() );
		ATermAppl o = loader.node2term( t.getObject() );

		// check if this is a type assertion
		if( p.toString().equals( RDF.type.toString() ) ) {
			// check if this is a new individual
			if( !kb.getIndividuals().contains( s ) )
				kb.addIndividual( s );

			// add the type
			kb.addType( s, o );
		}
		else {
			// check if the subject is a new individual
			if( !kb.getIndividuals().contains( s ) )
				kb.addIndividual( s );

			// check if the object is a new individual
			if( !t.getObject().isLiteral() && !kb.getIndividuals().contains( o ) )
				kb.addIndividual( o );

			// add the property value
			kb.addPropertyValue( p, s, o );
		}
	}

	/**
	 * Set the OWLLoader
	 * 
	 * @param loader
	 */
	public void setLoader(OWLLoader loader) {
		this.loader = loader;
	}

}
