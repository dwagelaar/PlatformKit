package org.mindswap.pellet.tbox.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.tbox.TBox;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.MultiValueMap;
import org.mindswap.pellet.utils.Pair;

import aterm.ATermAppl;

/**
 * <p>
 * Title: Implementation of TBox interface to generate explanations efficiently
 * and correctly.
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
public class TBoxExpImpl implements TBox {
	public static Log									log				= LogFactory
																				.getLog( TBox.class );

	protected KnowledgeBase								kb;

	protected Set<ATermAppl>							classes			= new HashSet<ATermAppl>();
	private Set<ATermAppl>								allClasses;

	private MultiValueMap<ATermAppl, Set<ATermAppl>>	tboxAxioms		= new MultiValueMap<ATermAppl, Set<ATermAppl>>();
	private MultiValueMap<ATermAppl, ATermAppl>			reverseExplain	= new MultiValueMap<ATermAppl, ATermAppl>();


	public TuBox										Tu				= null;
	public TgBox										Tg				= null;

	/*
	 * Constructors
	 */

	public TBoxExpImpl(KnowledgeBase kb) {
		this.kb = kb;

		Tu = new TuBox( kb );
		Tg = new TgBox( kb );

		this.kb = kb;
	}

	public Set<ATermAppl> getAllClasses() {
		if( allClasses == null ) {
			allClasses = new HashSet<ATermAppl>( classes );
			allClasses.add( ATermUtils.TOP );
			allClasses.add( ATermUtils.BOTTOM );
		}
		return allClasses;
	}

	public Set<Set<ATermAppl>> getAxiomExplanations(ATermAppl axiom) {
		return tboxAxioms.get( axiom );
	}

	public Set<ATermAppl> getAxiomExplanation(ATermAppl axiom) {
		Set<Set<ATermAppl>> explains = tboxAxioms.get( axiom );

		if( explains == null || explains.isEmpty() ) {
			log.warn( "No explanation for " + axiom );
		}

		// we won't be generating multiple explanations using axiom
		// tracing so we just pick one explanation. the other option
		// would be to return the union of all explanations which
		// would cause Pellet to return non-minimal explanations sets
		Set<ATermAppl> explain = explains.iterator().next();
		// Set<ATermAppl> explain = SetUtils.union( explains );

		return explain;
	}

	public boolean addAxiomExplanation(ATermAppl axiom, Set<ATermAppl> explain) {
		if( log.isDebugEnabled() )
			log.debug( "Axiom: " + axiom + "\nExplanation: " + explain );

		boolean added = tboxAxioms.add( axiom, explain );

		if( added ) {
			for( ATermAppl explainAxiom : explain ) {
				if( !axiom.equals( explainAxiom ) )
					reverseExplain.add( explainAxiom, axiom );
			}
		}

		return added;
	}

	public boolean addAxiom(ATermAppl axiom, Set<ATermAppl> explain) {
		// absorb nominals on the fly because sometimes they might end up in the
		// Tu directly without going into Tg which is still less effective than
		// absorbing
		if( PelletOptions.USE_NOMINAL_ABSORPTION || PelletOptions.USE_PSEUDO_NOMINALS ) {
			if( axiom.getAFun().equals( ATermUtils.SAMEFUN ) ) {
				ATermAppl c1 = (ATermAppl) axiom.getArgument( 0 );
				ATermAppl c2 = (ATermAppl) axiom.getArgument( 1 );

				// the first concept is oneOF
				if( ATermUtils.isOneOf( c1 ) ) {
					// absorb SubClassOf(c1,c2)
					Tg.absorbOneOf( c1, c2, explain );
					// the second concept is oneOf
					if( ATermUtils.isOneOf( c2 ) ) {
						// absorb SubClassOf(c2,c1)
						Tg.absorbOneOf( c2, c1, explain );
						// axioms completely absorbed so return
						return true;
					}
					else {
						// SubClassOf(c2,c1) is not absorbed so continue with
						// addAxiom function
						axiom = ATermUtils.makeSub( c2, c1 );
					}
				}
				else if( ATermUtils.isOneOf( c2 ) ) {
					// absorb SubClassOf(c2,c1)
					Tg.absorbOneOf( c2, c1, explain );
					// SubClassOf(c1,c2) is not absorbed so continue with
					// addAxiom function
					axiom = ATermUtils.makeSub( c1, c2 );
				}
			}
			else if( axiom.getAFun().equals( ATermUtils.SUBFUN ) ) {
				ATermAppl sub = (ATermAppl) axiom.getArgument( 0 );

				if( ATermUtils.isOneOf( sub ) ) {
					ATermAppl sup = (ATermAppl) axiom.getArgument( 1 );
					Tg.absorbOneOf( sub, sup, explain );
					return true;
				}
			}
		}

		return addAxiom( axiom, explain, false );
	}

	public boolean addAxiom(ATermAppl axiom, Set<ATermAppl> explain, boolean forceAddition) {
		boolean added = addAxiomExplanation( axiom, explain );

		if( added || forceAddition ) {
			if( !Tu.addIfUnfoldable( axiom ) ) {
				if( axiom.getName().equals( ATermUtils.SAME ) ) {
					// Try reversing the term if it is a 'same' construct
					ATermAppl name = (ATermAppl) axiom.getArgument( 0 );
					ATermAppl desc = (ATermAppl) axiom.getArgument( 1 );
					ATermAppl reversedAxiom = ATermUtils.makeSame( desc, name );

					if( !Tu.addIfUnfoldable( reversedAxiom ) )
						Tg.addDef( axiom );
					else
						addAxiomExplanation( reversedAxiom, explain );
				}
				else {
					Tg.addDef( axiom );
				}
			}
		}

		return added;
	}

	public boolean removeAxiom(ATermAppl axiom) {
		if( !PelletOptions.USE_TRACING ) {
			if( log.isDebugEnabled() )
				log.debug( "Cannot remove axioms when PelletOptions.USE_TRACING is false" );
			return false;
		}

		if( Tg.absorbedOutsideTBox.contains( axiom ) ) {
			if( log.isDebugEnabled() )
				log.debug( "Cannot remove axioms that have been absorbed outside TBox" );
			return false;
		}

		Set<ATermAppl> sideEffects = new HashSet<ATermAppl>();
		boolean removed = removeExplanation( axiom, axiom, sideEffects );

		// an axiom might be effectively removed as a side-effect of another
		// removal. For example, the axioms:
		// same(A,or(C,D)), sub(A,B)
		// after absorption become
		// same(A,or(C,D)), sub(C,B), sub(C,B)
		// and removing same(A,or(C,D)) removes sub(C,B) and sub(C,B) with
		// the side effect that sub(A,B) is also removed
		for( ATermAppl readdAxiom : sideEffects ) {
			Set<Set<ATermAppl>> explanations = tboxAxioms.get( readdAxiom );
			// if the axiom is really removed (and not just side-effected)
			// then there wouldn't be any explanation and we shouldn't readd
			if( explanations != null ) {
				Iterator<Set<ATermAppl>> i = explanations.iterator();
				addAxiom( readdAxiom, i.next(), true );
				while( i.hasNext() )
					addAxiomExplanation( readdAxiom, i.next() );
			}
		}

		return removed;
	}

	private boolean removeExplanation(ATermAppl dependantAxiom, ATermAppl explanationAxiom,
			Set<ATermAppl> sideEffects) {
		boolean removed = false;

		if( !PelletOptions.USE_TRACING ) {
			if( log.isDebugEnabled() )
				log.debug( "Cannot remove axioms when PelletOptions.USE_TRACING is false" );
			return false;
		}

		if( log.isDebugEnabled() )
			log.debug( "Removing " + explanationAxiom );

		// this axiom is being removed so it cannot support any other axiom
		reverseExplain.remove( explanationAxiom, dependantAxiom );

		Set<Set<ATermAppl>> explains = tboxAxioms.get( dependantAxiom );
		Set<Set<ATermAppl>> newExplains = new HashSet<Set<ATermAppl>>();

		if( explains != null ) {
			for( Set<ATermAppl> explain : explains ) {
				if( !explain.contains( explanationAxiom ) )
					newExplains.add( explain );
				else {
					sideEffects.addAll( explain );
					sideEffects.remove( explanationAxiom );
				}
			}
		}

		if( !newExplains.isEmpty() ) {
			// there are still other axioms supporting this axiom so it won't be
			// removed but we still need to update the explanations
			tboxAxioms.put( dependantAxiom, newExplains );

			// also make sure the concept on the left hand side is normalized
			Tu.updateDef( dependantAxiom );

			// there is no need for a reload
			return true;
		}

		// there is no other explanation for this dependant axiom so
		// we can safely remove it
		tboxAxioms.remove( dependantAxiom );

		// remove the axiom fom Tu and Tg
		removed |= Tu.removeDef( dependantAxiom );
		removed |= Tg.removeDef( dependantAxiom );

		// find if this axiom supports any other axiom
		Set<ATermAppl> otherDependants = reverseExplain.remove( dependantAxiom );
		if( otherDependants != null ) {
			for( ATermAppl otherDependant : otherDependants ) {
				// remove this axiom from any explanation it contributes to

				if( otherDependant.equals( dependantAxiom ) )
					continue;

				removed |= removeExplanation( otherDependant, dependantAxiom, sideEffects );
			}
		}

		return removed;
	}

	public Collection<ATermAppl> getAxioms() {
		return tboxAxioms.keySet();
	}

	public boolean containsAxiom(ATermAppl axiom) {
		return tboxAxioms.containsKey( axiom );
	}

	public void split() {
	}

	public void absorb() {
		Tg.absorb( Tu, this );
	}

	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append( "Tu: [\n" );
		for( ATermAppl c : getClasses() ) {
			List<Pair<ATermAppl, Set<ATermAppl>>> unfoldedList = Tu.unfold( c );
			if( unfoldedList != null ) {
				str.append( c ).append( " -> " );
				for( Pair<ATermAppl, Set<ATermAppl>> pair : unfoldedList )
					str.append( pair.first ).append( ", " );
				str.append( "\n" );
			}
			ATermAppl notC = ATermUtils.makeNot( c );
			unfoldedList = Tu.unfold( notC );
			if( unfoldedList != null ) {
				str.append( notC ).append( " -> " );
				for( Pair<ATermAppl, Set<ATermAppl>> pair : unfoldedList )
					str.append( pair.first ).append( ", " );
				str.append( "\n" );
			}
		}
		if( getUC() != null ) {
			str.append( "\nTg: [\n" );
			for( Pair<ATermAppl, Set<ATermAppl>> pair : getUC() )
				str.append( pair.first ).append( ", " );
			str.append( "\n" );
		}
		str.append( "]\nExplain: [\n" );
		for( ATermAppl axiom : tboxAxioms.keySet() ) {
			str.append( axiom ).append( " -> " ).append( tboxAxioms.get( axiom ) ).append( "\n" );
		}
		str.append( "]\nReverseExplain: [\n" );
		for( ATermAppl axiom : reverseExplain.keySet() ) {
			str.append( axiom ).append( " -> " ).append( reverseExplain.get( axiom ) )
					.append( "\n" );
		}
		str.append( "]\n" );
		return str.toString();
	}

	/**
	 * @return Returns the UC.
	 */
	public List<Pair<ATermAppl, Set<ATermAppl>>> getUC() {
		if( Tg == null )
			return null;

		return Tg.getUC();
	}

	public boolean addClass(ATermAppl term) {
		boolean added = classes.add( term );
		
		if( added ) 
			allClasses = null;		
		
		return added;
	}

	public Set<ATermAppl> getClasses() {
		return classes;
	}

	public Collection<ATermAppl> getAxioms(ATermAppl term) {
		List<ATermAppl> axioms = new ArrayList<ATermAppl>();
		TermDefinition def = Tg.getTD( term );
		if( def != null ) {
			axioms.addAll( def.getSubClassAxioms() );
			axioms.addAll( def.getEqClassAxioms() );
		}
		def = Tu.getTD( term );
		if( def != null ) {
			axioms.addAll( def.getSubClassAxioms() );
			axioms.addAll( def.getEqClassAxioms() );
		}

		return axioms;
	}

	public void normalize() {
		Tu.normalize( this );
	}

	public void internalize() {
		Tg.internalize( this );

		if( log.isDebugEnabled() )
			log.debug( this );
	}

	public List<Pair<ATermAppl, Set<ATermAppl>>> unfold(ATermAppl c) {
		return Tu.unfold( c );
	}
}
