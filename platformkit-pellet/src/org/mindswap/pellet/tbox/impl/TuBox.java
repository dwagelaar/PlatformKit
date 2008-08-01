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

package org.mindswap.pellet.tbox.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.mindswap.pellet.DependencySet;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.tbox.TBox;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Pair;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;

public class TuBox extends TBoxBase {
	/*
	 * Private Variables
	 */
	protected Map													unfoldedcache		= new HashMap();
	protected Set													unfoldMisses		= new HashSet();
	static int														resets				= 0;
	static int														createtime			= 0;

	private Map<ATermAppl, List<Pair<ATermAppl, Set<ATermAppl>>>>	unfoldingMap;

	private Collection<ATermAppl>									termsToNormalize	= null;

	/*
	 * Constructors
	 */

	public TuBox(KnowledgeBase kb) {
		super( kb );
	}

	public boolean addDef(ATermAppl axiom) {
		boolean added = false;

		ATermAppl name = (ATermAppl) axiom.getArgument( 0 );
		TermDefinition td = getTD( name );
		if( td == null ) {
			td = new TermDefinition();
			termhash.put( name, td );
		}

		added = td.addDef( axiom );

		if( added && termsToNormalize != null )
			termsToNormalize.add( name );		

		return added;
	}

	public boolean removeDef(ATermAppl axiom) {
		boolean removed = super.removeDef( axiom );

		if( removed && termsToNormalize != null )
			termsToNormalize.add( (ATermAppl) axiom.getArgument( 0 ) );		

		return removed;
	}
	
	public void updateDef(ATermAppl axiom) {
		ATermAppl c = (ATermAppl) axiom.getArgument( 0 );
		if( ATermUtils.isPrimitive( c ) )
			termsToNormalize.add( c );
	}

	public List<Pair<ATermAppl, Set<ATermAppl>>> unfold(ATermAppl c) {
		return unfoldingMap.get( c );
	}

	/**
	 * Normalize all the definitions in the Tu
	 */
	public void normalize(TBox tbox) {
		if( termsToNormalize == null ) {
			termsToNormalize = termhash.keySet();
			unfoldingMap = new HashMap<ATermAppl, List<Pair<ATermAppl, Set<ATermAppl>>>>();
		}
		else if( log.isDebugEnabled() ) {
			log.debug( "Normalizing " + termsToNormalize );
		}

		for( ATermAppl c : termsToNormalize ) {
			TermDefinition td = termhash.get( c );
			ATermAppl notC = ATermUtils.makeNot( c );

			List<Pair<ATermAppl, Set<ATermAppl>>> unfoldC = new ArrayList<Pair<ATermAppl, Set<ATermAppl>>>();

			if( !td.getEqClassAxioms().isEmpty() ) {
				List<Pair<ATermAppl, Set<ATermAppl>>> unfoldNotC = new ArrayList<Pair<ATermAppl, Set<ATermAppl>>>();

				for( ATermAppl eqClassAxiom : td.getEqClassAxioms() ) {
					ATermAppl unfolded = (ATermAppl) eqClassAxiom.getArgument( 1 );
					Set<ATermAppl> ds = tbox.getAxiomExplanation( eqClassAxiom );

					ATermAppl normalized = ATermUtils.normalize( unfolded );
					ATermAppl normalizedNot = ATermUtils.negate( normalized );

					unfoldC.add( new Pair<ATermAppl, Set<ATermAppl>>( normalized, ds ) );
					unfoldNotC.add( new Pair<ATermAppl, Set<ATermAppl>>( normalizedNot, ds ) );
				}

				unfoldingMap.put( notC, unfoldNotC );
			}
			else
				unfoldingMap.remove( notC );

			for( ATermAppl subClassAxiom : td.getSubClassAxioms() ) {
				ATermAppl unfolded = (ATermAppl) subClassAxiom.getArgument( 1 );
				Set<ATermAppl> ds = tbox.getAxiomExplanation( subClassAxiom );

				ATermAppl normalized = ATermUtils.normalize( unfolded );
				unfoldC.add( new Pair<ATermAppl, Set<ATermAppl>>( normalized, ds ) );
			}

			if( !unfoldC.isEmpty() )
				unfoldingMap.put( c, unfoldC );
			else
				unfoldingMap.remove( c );
		}

		termsToNormalize = new HashSet<ATermAppl>();
//		termsToNormalize = null;

		if( PelletOptions.USE_ROLE_ABSORPTION )
			absorbRanges();
	}

	private void absorbRanges() {
		List<Pair<ATermAppl, Set<ATermAppl>>> unfoldTop = unfoldingMap.get( ATermUtils.TOP );
		if( unfoldTop == null )
			return;

		List<Pair<ATermAppl, Set<ATermAppl>>> newUnfoldTop = new ArrayList<Pair<ATermAppl, Set<ATermAppl>>>();
		for( Pair<ATermAppl, Set<ATermAppl>> pair : unfoldTop ) {
			ATermAppl unfolded = pair.first;
			Set<ATermAppl> explain = pair.second;

			if( ATermUtils.isAllValues( unfolded ) ) {
				ATerm r = unfolded.getArgument( 0 );
				ATermAppl range = (ATermAppl) unfolded.getArgument( 1 );

				kb.addRange( r, range, new DependencySet( explain ) );
			}
			else if( ATermUtils.isAnd( unfolded ) ) {
				ATermList l = (ATermList) unfolded.getArgument( 0 );
				ATermList newList = ATermUtils.EMPTY_LIST;
				for( ; !l.isEmpty(); l = l.getNext() ) {
					ATermAppl term = (ATermAppl) l.getFirst();
					if( term.getAFun().equals( ATermUtils.ALLFUN ) ) {
						ATerm r = term.getArgument( 0 );
						ATermAppl range = (ATermAppl) term.getArgument( 1 );

						kb.addRange( r, range, new DependencySet( explain ) );
					}
					else
						newList.insert( term );
				}

				if( !newList.isEmpty() ) {
					newUnfoldTop.add( new Pair<ATermAppl, Set<ATermAppl>>( ATermUtils
							.makeAnd( newList ), explain ) );
				}
			}
			else
				newUnfoldTop.add( pair );
		}

		if( newUnfoldTop.isEmpty() )
			unfoldingMap.remove( ATermUtils.TOP );

	}

	/*
	 * Accessor Methods
	 */

	public boolean addIfUnfoldable(ATermAppl term) {
		ATermAppl name = (ATermAppl) term.getArgument( 0 );
		ATermAppl body = (ATermAppl) term.getArgument( 1 );
		TermDefinition td = getTD( name );

		if( !ATermUtils.isPrimitive( name ) )
			return false;

		if( td == null )
			td = new TermDefinition();

		// Basic Checks
		TermDefinition tdcopy = new TermDefinition( td );
		tdcopy.addDef( term );
		if( !tdcopy.isUnique() )
			return false;

		// Loop Checks
		Set dependencies = ATermUtils.findPrimitives( body );
		Set seen = new HashSet();
		if( !td.dependencies.containsAll( dependencies ) ) {
			// Fast check failed
			for( Iterator iter = dependencies.iterator(); iter.hasNext(); ) {
				ATermAppl current = (ATermAppl) iter.next();

				boolean result = findTarget( current, name, seen );
				if( result ) {
					return false;
				}
			}
		}

		boolean added = addDef( term );

		return added;
	}

	/*
	 * Utility methods
	 */

	protected boolean findTarget(ATermAppl term, ATermAppl target, Set seen) {
		List stack = new Vector();
		stack.add( term );

		while( !stack.isEmpty() ) {
			kb.timers.checkTimer( "preprocessing" );
			ATermAppl current = (ATermAppl) stack.remove( 0 );

			if( seen.contains( current ) ) {
				continue;
			}
			seen.add( current );

			if( current.equals( target ) ) {
				return true;
			}

			TermDefinition td = this.getTD( current );
			if( td != null ) {
				// Shortcut
				if( td.dependencies.contains( target ) ) {
					return true;
				}
				stack.addAll( 0, td.dependencies );
			}
		}

		return false;
	}

	public void resetCache() {
		unfoldedcache = new HashMap();
		unfoldMisses = new HashSet();
		resets++;
	}

	public void print() {
		Iterator it = unfoldingMap.entrySet().iterator();
		while( it.hasNext() ) {
			Map.Entry entry = (Map.Entry) it.next();

			System.out.println( entry.getKey() + " -> " + entry.getValue() );
		}
	}
}
