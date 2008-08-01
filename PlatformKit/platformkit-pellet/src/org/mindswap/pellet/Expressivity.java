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
 * Created on Aug 30, 2004
 */
package org.mindswap.pellet;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.output.ATermBaseVisitor;
import org.mindswap.pellet.output.ATermVisitor;
import org.mindswap.pellet.tbox.TBox;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Pair;

import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;

/**
 * @author Evren Sirin
 */
public class Expressivity {
	KnowledgeBase			kb;

	/**
	 * not (owl:complementOf) is used directly or indirectly
	 */
	private boolean			hasNegation			= false;

	/**
	 * An inverse property has been defined or a property has been defined as
	 * InverseFunctional
	 */
	private boolean			hasInverse			= false;
	private boolean			hasFunctionality	= false;
	private boolean			hasCardinality		= false;
	private boolean			hasCardinalityQ		= false;
	private boolean			hasFunctionalityD	= false;
	private boolean			hasCardinalityD		= false;
	private boolean			hasTransitivity		= false;
	private boolean			hasRoleHierarchy	= false;
	private boolean			hasReflexivity		= false;
	private boolean			hasIrreflexivity	= false;
	private boolean			hasDisjointRoles	= false;
	private boolean			hasAntiSymmetry		= false;
	private boolean			hasComplexSubRoles	= false;
	private boolean			hasDatatype			= false;

	private boolean			hasKeys				= false;

	private boolean			hasDomain;
	private boolean			hasRange;

	/**
	 * The set of individuals in the ABox that have been used as nominals, i.e.
	 * in an owl:oneOf enumeration or target of owl:hasValue restriction
	 */
	private Set<ATermAppl>	nominals			= new HashSet<ATermAppl>();

	private Visitor			visitor;

	class Visitor extends ATermBaseVisitor implements ATermVisitor {
		public void visitTerm(ATermAppl term) {
		}

		void visitRole(ATermAppl p) {
			if( !ATermUtils.isPrimitive( p ) )
				hasInverse = true;
		}

		public void visitAnd(ATermAppl term) {
			visitList( (ATermList) term.getArgument( 0 ) );
		}

		public void visitOr(ATermAppl term) {
			hasNegation = true;
			visitList( (ATermList) term.getArgument( 0 ) );
		}

		public void visitNot(ATermAppl term) {
			hasNegation = true;
			visit( (ATermAppl) term.getArgument( 0 ) );
		}

		public void visitSome(ATermAppl term) {
			visitRole( (ATermAppl) term.getArgument( 0 ) );
			visit( (ATermAppl) term.getArgument( 1 ) );
		}

		public void visitAll(ATermAppl term) {
			visitRole( (ATermAppl) term.getArgument( 0 ) );
			visit( (ATermAppl) term.getArgument( 1 ) );
		}

		public void visitCard(ATermAppl term) {
			visitMin( term );
			visitMax( term );
		}

		public void visitMin(ATermAppl term) {
			visitRole( (ATermAppl) term.getArgument( 0 ) );
			int cardinality = ((ATermInt) term.getArgument( 1 )).getInt();
			ATermAppl c = (ATermAppl) term.getArgument( 2 );
			if( !ATermUtils.isTop( c ) )
				hasCardinalityQ = true;
			else if( cardinality > 2 ) {
				hasCardinality = true;
				if( kb.getRole( term.getArgument( 0 ) ).isDatatypeRole() )
					hasCardinalityD = true;
			}
			else if( cardinality > 0 ) {
				hasFunctionality = true;
				if( kb.getRole( term.getArgument( 0 ) ).isDatatypeRole() )
					hasFunctionalityD = true;
			}
		}

		public void visitMax(ATermAppl term) {
			visitRole( (ATermAppl) term.getArgument( 0 ) );
			int cardinality = ((ATermInt) term.getArgument( 1 )).getInt();
			ATermAppl c = (ATermAppl) term.getArgument( 2 );
			if( !ATermUtils.isTop( c ) )
				hasCardinalityQ = true;
			else if( cardinality > 1 )
				hasCardinality = true;
			else if( cardinality > 0 )
				hasFunctionality = true;
		}

		public void visitHasValue(ATermAppl term) {
			visitRole( (ATermAppl) term.getArgument( 0 ) );
			visitValue( (ATermAppl) term.getArgument( 1 ) );
		}

		public void visitValue(ATermAppl term) {
			ATermAppl nom = (ATermAppl) term.getArgument( 0 );
			if( !ATermUtils.isLiteral( nom ) )
				nominals.add( nom );
		}

		public void visitOneOf(ATermAppl term) {
			hasNegation = true;
			visitList( (ATermList) term.getArgument( 0 ) );
		}

		public void visitLiteral(ATermAppl term) {
			// nothing to do here
		}

		public void visitSelf(ATermAppl term) {
			hasReflexivity = true;
			hasIrreflexivity = true;
		}

		public void visitSubClass(ATermAppl term) {
			throw new InternalReasonerException( "This function should never be called!" );
		}
	}

	Expressivity(KnowledgeBase kb) {
		this.kb = kb;

		nominals = new HashSet<ATermAppl>();

		visitor = new Visitor();
	}

	private Expressivity(Expressivity expr) {
		this.kb = expr.kb;
		this.visitor = new Visitor();

		hasNegation = expr.hasNegation;
		hasInverse = expr.hasInverse;
		hasDatatype = expr.hasDatatype;
		hasCardinality = expr.hasCardinality;
		hasCardinalityQ = expr.hasCardinalityQ;
		hasFunctionality = expr.hasFunctionality;
		hasTransitivity = expr.hasTransitivity;
		hasRoleHierarchy = expr.hasRoleHierarchy;
		hasReflexivity = expr.hasReflexivity;
		hasIrreflexivity = expr.hasIrreflexivity;
		hasAntiSymmetry = expr.hasAntiSymmetry;
		hasDisjointRoles = expr.hasDisjointRoles;
		hasComplexSubRoles = expr.hasComplexSubRoles;
		hasFunctionalityD = expr.hasFunctionalityD;
		hasCardinalityD = expr.hasCardinalityD;
		hasDomain = expr.hasDomain;
		hasRange = expr.hasRange;

		nominals = new HashSet<ATermAppl>( expr.nominals );
	}

	/**
	 * Returns the expressivity of the KB.
	 */
	public void compute() {
		processIndividuals();
		processClasses();
		processRoles();
	}

	/**
	 * Return the expressivity of the KB combined with one additional concept.
	 * 
	 * @param c
	 *            The additional concept considered in expressivity computation
	 * @return
	 */
	public Expressivity compute(ATermAppl c) {
		if( c == null )
			return this;

		Expressivity expressivity = new Expressivity( this );
		expressivity.visitor.visit( c );

		return expressivity;
	}

	public String toString() {
		String dl = "";

		if( hasNegation )
			dl = "ALC";
		else
			dl = "AL";

		if( hasTransitivity )
			dl += "R+";

		if( dl.equals( "ALCR+" ) )
			dl = "S";

		if( hasComplexSubRoles )
			dl = "SR";
		else if( hasRoleHierarchy )
			dl += "H";

		if( hasNominal() )
			dl += "O";

		if( hasInverse )
			dl += "I";

		if( hasCardinalityQ )
			dl += "Q";
		else if( hasCardinality )
			dl += "N";
		else if( hasFunctionality )
			dl += "F";

		if( hasDatatype ) {
			if( hasKeys )
				dl += "(Dk)";
			else
				dl += "(D)";
		}

		return dl;
	}

	protected void processClasses() {
		TBox tbox = kb.getTBox();

		List<Pair<ATermAppl, Set<ATermAppl>>> UC = tbox.getUC();
		if( UC != null ) {
			hasNegation = true;
			for( Pair<ATermAppl, Set<ATermAppl>> pair : UC )
				visitor.visit( pair.first );
		}

		for( ATermAppl c : kb.getAllClasses() ) {
			List<Pair<ATermAppl, Set<ATermAppl>>> unfoldC = tbox.unfold( c );
			if( unfoldC == null )
				continue;
			for( Pair<ATermAppl, Set<ATermAppl>> pair : unfoldC )
				visitor.visit( pair.first );
		}
	}

	protected void processIndividuals() {
		Iterator i = kb.getABox().getIndIterator();
		while( i.hasNext() ) {
			Individual ind = (Individual) i.next();
			ATermAppl nominal = ATermUtils.makeValue( ind.getName() );
			Iterator j = ind.getTypes().iterator();
			while( j.hasNext() ) {
				ATermAppl term = (ATermAppl) j.next();

				if( term.equals( nominal ) )
					continue;
				visitor.visit( term );
			}
		}
	}

	/**
	 * Added for incremental reasoning. Given an aterm corresponding to an
	 * individual and concept, the expressivity is updated accordingly.
	 */
	protected void processIndividual(ATermAppl i, ATermAppl concept) {
		ATermAppl nominal = ATermUtils.makeValue( i );

		if( concept.equals( nominal ) )
			return;
		visitor.visit( concept );
	}

	protected void processRoles() {
		for( Iterator i = kb.getRBox().getRoles().iterator(); i.hasNext(); ) {
			Role r = (Role) i.next();
			if( r.isDatatypeRole() ) {
				hasDatatype = true;
				if( r.isInverseFunctional() )
					hasKeys = true;
			}

			if( r.isAnon() ) {
				for( Role subRole : (Set<Role>) r.getSubRoles() ) {
					if( !subRole.isAnon() )
						hasInverse = true;
				}
			}
			// InverseFunctionalProperty declaration may mean that a named
			// property has an anonymous inverse property which is functional
			// The following condition checks this case
			if( r.isAnon() && r.isFunctional() )
				hasInverse = true;
			if( r.isFunctional() )
				hasFunctionality = true;
			if( r.isTransitive() )
				hasTransitivity = true;
			if( r.isReflexive() )
				hasReflexivity = true;
			if( r.isIrreflexive() )
				hasIrreflexivity = true;
			if( r.isAntisymmetric() )
				hasAntiSymmetry = true;
			if( !r.getDisjointRoles().isEmpty() )
				hasDisjointRoles = true;
			if( r.hasComplexSubRole() ) 
				hasComplexSubRoles = true;			

			// Each property has itself included in the subroles set. We need
			// at least two properties in the set to conclude there is a role
			// hierarchy defined in the ontology
			if( r.getSubRoles().size() > 1 )
				hasRoleHierarchy = true;

			ATermAppl domain = r.getDomain();
			if( domain != null ) {
				hasDomain |= !domain.equals( ATermUtils.TOP );
				visitor.visit( domain );
			}

			ATermAppl range = r.getRange();
			if( range != null ) {
				hasRange |= !range.equals( ATermUtils.TOP );
				visitor.visit( range );
			}
		}
	}

	/**
	 * @return Returns the hasCardinality.
	 */
	public boolean hasCardinality() {
		return hasCardinality;
	}

	/**
	 * @return Returns the hasCardinality.
	 */
	public boolean hasCardinalityQ() {
		return hasCardinalityQ;
	}

	/**
	 * @return Returns the hasFunctionality.
	 */
	public boolean hasFunctionality() {
		return hasFunctionality;
	}

	/**
	 * @return Returns the hasDatatype.
	 */
	public boolean hasDatatype() {
		return hasDatatype;
	}

	/**
	 * Returns true if a cardinality restriction (greater than 1) is defined on
	 * any datatype property
	 */
	public boolean hasCardinalityD() {
		return hasCardinalityD;
	}

	/**
	 * Returns true if a cardinality restriction (less than or equal to 1) is
	 * defined on any datatype property
	 */
	public boolean hasFunctionalityD() {
		return hasFunctionalityD;
	}

	/**
	 * @return Returns the hasInverse.
	 */
	public boolean hasInverse() {
		return hasInverse;
	}

	/**
	 * @return Returns the hasNegation.
	 */
	public boolean hasNegation() {
		return hasNegation;
	}

	/**
	 * @return Returns the hasNominal.
	 */
	public boolean hasNominal() {
		return !nominals.isEmpty();
	}

	/**
	 * @return Returns the hasRoleHierarchy.
	 */
	public boolean hasRoleHierarchy() {
		return hasRoleHierarchy;
	}

	public boolean hasReflexivity() {
		return hasReflexivity;
	}

	public boolean hasIrreflexivity() {
		return hasIrreflexivity;
	}

	public boolean hasAntiSymmmetry() {
		return hasAntiSymmetry;
	}
	
	public boolean hasDisjointRoles() {
		return hasDisjointRoles;
	}
	
	public boolean hasComplexSubRoles() {
		return hasComplexSubRoles;
	}

	/**
	 * @return Returns the hasTransitivity.
	 */
	public boolean hasTransitivity() {
		return hasTransitivity;
	}

	public boolean hasDomain() {
		return hasDomain;
	}

	public boolean hasRange() {
		return hasRange;
	}

	public Set getNominals() {
		return nominals;
	}

	public boolean hasKeys() {
		return hasKeys;
	}
}
