// The MIT License
//
// Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin, Clark &
// Parsia, LLC
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 */
public class TermDefinition {
	private List<ATermAppl>	subClassAxioms;
	private List<ATermAppl>	eqClassAxioms;
	private Set<ATermAppl>	seen;
	public Set<ATermAppl>	dependencies;

	public TermDefinition() {
		subClassAxioms = new ArrayList<ATermAppl>();
		eqClassAxioms = new ArrayList<ATermAppl>();
		seen = new HashSet<ATermAppl>();
		updateDependencies();
	}

	public TermDefinition(TermDefinition td) {
		subClassAxioms = new ArrayList<ATermAppl>( td.subClassAxioms );
		eqClassAxioms = new ArrayList<ATermAppl>( td.eqClassAxioms );
		seen = new HashSet<ATermAppl>( td.seen );
		updateDependencies();
	}

	public ATermAppl getName() {
		if( !subClassAxioms.isEmpty() )
			return (ATermAppl) subClassAxioms.get( 0 ).getArgument( 0 );

		if( !eqClassAxioms.isEmpty() )
			return (ATermAppl) eqClassAxioms.get( 0 ).getArgument( 0 );

		return null;
	}

	public boolean addDef(ATermAppl appl) {
		if( seen.contains( appl ) ) {
			return false;
		}
		else {
			seen.add( appl );
		}
		
		boolean added = false;		
		
		String name = appl.getName();
		if( name.equals( ATermUtils.SUB ) ) {
			added = subClassAxioms.add( appl );
		}
		else if( name.equals( ATermUtils.SAME ) ) {
			added = eqClassAxioms.add( appl );
		}
		else {
			throw new RuntimeException( "Cannot add non-definition!" );
		}
		
		if( added )
			updateDependencies();
		
		return added;
	}

	public boolean removeDef(ATermAppl axiom) {
		boolean removed;

		seen.remove( axiom );

		String name = axiom.getName();
		if( name.equals( ATermUtils.SUB ) ) {
			removed = subClassAxioms.remove( axiom );
		}
		else if( name.equals( ATermUtils.SAME ) ) {
			removed = eqClassAxioms.remove( axiom );
		}
		else {
			throw new RuntimeException( "Cannot remove non-definition!" );
		}

		updateDependencies();

		return removed;
	}

	public boolean isPrimitive() {
		return eqClassAxioms.isEmpty();
	}

	public boolean isUnique() {
		int numEqClasses = eqClassAxioms.size();
		if( numEqClasses == 0 || (numEqClasses == 1 && subClassAxioms.isEmpty()) ) {
			return true;
		}
		else {
			return false;
		}
	}

	public List<ATermAppl> getSubClassAxioms() {
		return subClassAxioms;
	}

	public List<ATermAppl> getEqClassAxioms() {
		return eqClassAxioms;
	}

	/**
	 * @deprecated Use {@link #getEqClassAxioms()} instead
	 */
	public List<ATermAppl> getSames() {
		return getEqClassAxioms();
	}

	public String toString() {
		return subClassAxioms + "; " + eqClassAxioms;
	}

	protected void updateDependencies() {
		dependencies = new HashSet<ATermAppl>();
		for( Iterator iter = getSubClassAxioms().iterator(); iter.hasNext(); ) {
			ATermAppl sub = (ATermAppl) iter.next();
			dependencies.addAll( ATermUtils.findPrimitives( (ATermAppl) sub.getArgument( 1 ) ) );
		}
		for( Iterator iter = getEqClassAxioms().iterator(); iter.hasNext(); ) {
			ATermAppl same = (ATermAppl) iter.next();
			dependencies.addAll( ATermUtils.findPrimitives( (ATermAppl) same.getArgument( 1 ) ) );
		}
	}

	public TermDefinition clone() {
		TermDefinition newtd = new TermDefinition();
		newtd.subClassAxioms = subClassAxioms;
		if( eqClassAxioms != null ) {
			newtd.eqClassAxioms = new ArrayList( eqClassAxioms );
		}
		else {
			newtd.eqClassAxioms = null;
		}
		if( seen != null ) {
			newtd.seen = new HashSet( seen );
		}
		else {
			newtd.seen = null;
		}
		newtd.updateDependencies();
		return newtd;
	}
}
