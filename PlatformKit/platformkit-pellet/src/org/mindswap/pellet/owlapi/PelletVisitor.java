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

package org.mindswap.pellet.owlapi;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.Role;
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.datatypes.DatatypeReasoner;
import org.mindswap.pellet.datatypes.UnknownDatatype;
import org.mindswap.pellet.datatypes.XSDAtomicType;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.rete.Constant;
import org.mindswap.pellet.rete.Rule;
import org.mindswap.pellet.rete.Term;
import org.mindswap.pellet.rete.Triple;
import org.mindswap.pellet.rete.Variable;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Comparators;
import org.mindswap.pellet.utils.MultiValueMap;
import org.mindswap.pellet.utils.progress.ProgressMonitor;
import org.semanticweb.owl.model.OWLAntiSymmetricObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLAxiomAnnotationAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLClassAssertionAxiom;
import org.semanticweb.owl.model.OWLConstant;
import org.semanticweb.owl.model.OWLConstantAnnotation;
import org.semanticweb.owl.model.OWLDataAllRestriction;
import org.semanticweb.owl.model.OWLDataComplementOf;
import org.semanticweb.owl.model.OWLDataExactCardinalityRestriction;
import org.semanticweb.owl.model.OWLDataMaxCardinalityRestriction;
import org.semanticweb.owl.model.OWLDataMinCardinalityRestriction;
import org.semanticweb.owl.model.OWLDataOneOf;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owl.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owl.model.OWLDataRangeFacetRestriction;
import org.semanticweb.owl.model.OWLDataRangeRestriction;
import org.semanticweb.owl.model.OWLDataSomeRestriction;
import org.semanticweb.owl.model.OWLDataSubPropertyAxiom;
import org.semanticweb.owl.model.OWLDataType;
import org.semanticweb.owl.model.OWLDataValueRestriction;
import org.semanticweb.owl.model.OWLDeclarationAxiom;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owl.model.OWLDisjointClassesAxiom;
import org.semanticweb.owl.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owl.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLDisjointUnionAxiom;
import org.semanticweb.owl.model.OWLEntityAnnotationAxiom;
import org.semanticweb.owl.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owl.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owl.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owl.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLImportsDeclaration;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLObjectAllRestriction;
import org.semanticweb.owl.model.OWLObjectAnnotation;
import org.semanticweb.owl.model.OWLObjectComplementOf;
import org.semanticweb.owl.model.OWLObjectExactCardinalityRestriction;
import org.semanticweb.owl.model.OWLObjectIntersectionOf;
import org.semanticweb.owl.model.OWLObjectMaxCardinalityRestriction;
import org.semanticweb.owl.model.OWLObjectMinCardinalityRestriction;
import org.semanticweb.owl.model.OWLObjectOneOf;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyChainSubPropertyAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;
import org.semanticweb.owl.model.OWLObjectPropertyInverse;
import org.semanticweb.owl.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owl.model.OWLObjectSelfRestriction;
import org.semanticweb.owl.model.OWLObjectSomeRestriction;
import org.semanticweb.owl.model.OWLObjectSubPropertyAxiom;
import org.semanticweb.owl.model.OWLObjectUnionOf;
import org.semanticweb.owl.model.OWLObjectValueRestriction;
import org.semanticweb.owl.model.OWLObjectVisitor;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyAnnotationAxiom;
import org.semanticweb.owl.model.OWLProperty;
import org.semanticweb.owl.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLSameIndividualsAxiom;
import org.semanticweb.owl.model.OWLSubClassAxiom;
import org.semanticweb.owl.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLTypedConstant;
import org.semanticweb.owl.model.OWLUntypedConstant;
import org.semanticweb.owl.model.SWRLAtom;
import org.semanticweb.owl.model.SWRLAtomConstantObject;
import org.semanticweb.owl.model.SWRLAtomDVariable;
import org.semanticweb.owl.model.SWRLAtomIObject;
import org.semanticweb.owl.model.SWRLAtomIVariable;
import org.semanticweb.owl.model.SWRLAtomIndividualObject;
import org.semanticweb.owl.model.SWRLBuiltInAtom;
import org.semanticweb.owl.model.SWRLClassAtom;
import org.semanticweb.owl.model.SWRLDataRangeAtom;
import org.semanticweb.owl.model.SWRLDataValuedPropertyAtom;
import org.semanticweb.owl.model.SWRLDifferentFromAtom;
import org.semanticweb.owl.model.SWRLObjectPropertyAtom;
import org.semanticweb.owl.model.SWRLRule;
import org.semanticweb.owl.model.SWRLSameAsAtom;
import org.semanticweb.owl.vocab.OWLRDFVocabulary;
import org.semanticweb.owl.vocab.OWLRestrictedDataRangeFacetVocabulary;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;

/**
 * PelletVisitor
 */

public class PelletVisitor implements OWLObjectVisitor {
	private static final long											serialVersionUID	= 8211773146996997500L;

	public static Log													log					= LogFactory
																									.getLog( PelletVisitor.class );

	private KnowledgeBase												kb;

	private ATermAppl													term;

	private Term														swrlTerm;

	private Triple														swrlTriple;

	private ProgressMonitor												monitor;

	private boolean														addAxioms;

	private boolean														reloadRequired;

	private Set<OWLAxiom>												unsupportedAxioms;

	/*
	 * Only simple properties can be used in cardinality restrictions,
	 * disjointness axioms, irreflexivity and antisymmetry axioms. The following
	 * constants will be used to identify why a certain property should be
	 * treated as simple
	 */
	private MultiValueMap<OWLObjectProperty, OWLObjectPropertyAxiom>	compositePropertyAxioms;
	private Set<OWLObjectProperty>										simpleProperties;

	public PelletVisitor(KnowledgeBase kb) {
		this.kb = kb;

		clear();
	}

	/**
	 * Clear the visitor cache about simple properties. Should be called before
	 * a reload.
	 */
	public void clear() {
		unsupportedAxioms = new HashSet<OWLAxiom>();
		compositePropertyAxioms = new MultiValueMap<OWLObjectProperty, OWLObjectPropertyAxiom>();
		simpleProperties = new HashSet<OWLObjectProperty>();
	}

	private void addUnsupportedAxiom(OWLAxiom axiom) {
		if( !PelletOptions.IGNORE_UNSUPPORTED_AXIOMS )
			throw new UnsupportedFeatureException( "Axiom: " + axiom );

		if( unsupportedAxioms.add( axiom ) )
			log.warn( "Ignoring unsupported axiom: " + axiom );
	}

	private OWLObjectProperty getNamedProperty(OWLObjectPropertyExpression ope) {
		if( ope.isAnonymous() )
			return getNamedProperty( ((OWLObjectPropertyInverse) ope).getInverse() );
		else
			return ope.asOWLObjectProperty();
	}

	private void addSimpleProperty(OWLObjectPropertyExpression ope) {
		OWLObjectProperty prop = getNamedProperty( ope );		
		simpleProperties.add( prop );
		
		prop.accept( this );
		Role role = kb.getRBox().getRole( term );		
		role.setForceSimple( true );
	}

	void verify() {
		for( Map.Entry<OWLObjectProperty, Set<OWLObjectPropertyAxiom>> entry : compositePropertyAxioms
				.entrySet() ) {
			OWLObjectProperty nonSimpleProperty = entry.getKey();

			if( !simpleProperties.contains( nonSimpleProperty ) )
				continue;

			Set<OWLObjectPropertyAxiom> axioms = entry.getValue();
			for( OWLObjectPropertyAxiom axiom : axioms )
				addUnsupportedAxiom( axiom );

			ATermAppl name = ATermUtils.term( nonSimpleProperty.getURI() );
			Role role = kb.getRBox().getRole( name );
			role.removeSubRoleChains();
		}
	}

	public void setAddAxiom(boolean addAxioms) {
		this.addAxioms = addAxioms;
	}

	public boolean isReloadRequired() {
		return reloadRequired;
	}

	public ATermAppl result() {
		return term;
	}

	/**
	 * Reset the visitor state about created terms. Should be called before
	 * every visit so terms created earlier will not affect the future results.
	 */
	public void reset() {
		term = null;
		reloadRequired = false;
	}

	public void visit(OWLClass c) {
		URI uri = c.getURI();

		if( uri.equals( OWLRDFVocabulary.OWL_THING.getURI() ) )
			term = ATermUtils.TOP;
		else if( uri.equals( OWLRDFVocabulary.OWL_NOTHING.getURI() ) )
			term = ATermUtils.BOTTOM;
		else
			term = ATermUtils.term( uri );

		if( addAxioms )
			kb.addClass( term );
	}

	public void visit(OWLIndividual ind) {
		term = ATermUtils.term( ind.getURI() );

		if( addAxioms )
			kb.addIndividual( term );
	}

	public void visit(OWLObjectProperty prop) {
		term = ATermUtils.term( prop.getURI() );

		if( addAxioms )
			kb.addObjectProperty( term );
	}

	public void visit(OWLObjectPropertyInverse propInv) {
		propInv.getInverse().accept( this );
		ATermAppl p = term;

		term = ATermUtils.makeInv( p );
	}

	public void visit(OWLDataProperty prop) {
		term = ATermUtils.term( prop.getURI() );

		if( addAxioms )
			kb.addDatatypeProperty( term );
	}

	public void visit(OWLTypedConstant constant) {
		String lexicalValue = constant.getLiteral();
		constant.getDataType().accept( this );
		ATerm datatype = term;

		term = ATermUtils.makeTypedLiteral( lexicalValue, datatype.toString() );
	}

	public void visit(OWLUntypedConstant constant) {
		String lexicalValue = constant.getLiteral();
		String lang = constant.getLang();

		if( lang != null )
			term = ATermUtils.makePlainLiteral( lexicalValue, lang );
		else
			term = ATermUtils.makePlainLiteral( lexicalValue );
	}

	public void visit(OWLDataType ocdt) {
		term = ATermUtils.term( ocdt.getURI() );

		if( PelletOptions.AUTO_XML_SCHEMA_LOADING )
			kb.loadDatatype( term );
		else
			kb.addDatatype( term );
	}

	public void visit(OWLObjectIntersectionOf and) {
		Set<OWLDescription> operands = and.getOperands();
		ATerm[] terms = new ATerm[operands.size()];
		int size = 0;
		for( OWLDescription desc : operands ) {
			desc.accept( this );
			terms[size++] = term;
		}
		// create a sorted set of terms so we will have a stable
		// concept creation and removal using this concept will work
		ATermList setOfTerms = size > 0
			? ATermUtils.toSet( terms, size )
			: ATermUtils.EMPTY_LIST;
		term = ATermUtils.makeAnd( setOfTerms );
	}

	public void visit(OWLObjectUnionOf or) {
		Set<OWLDescription> operands = or.getOperands();
		ATerm[] terms = new ATerm[operands.size()];
		int size = 0;
		for( OWLDescription desc : operands ) {
			desc.accept( this );
			terms[size++] = term;
		}
		// create a sorted set of terms so we will have a stable
		// concept creation and removal using this concept will work
		ATermList setOfTerms = size > 0
			? ATermUtils.toSet( terms, size )
			: ATermUtils.EMPTY_LIST;
		term = ATermUtils.makeOr( setOfTerms );
	}

	public void visit(OWLObjectComplementOf not) {
		OWLDescription desc = not.getOperand();
		desc.accept( this );

		term = ATermUtils.makeNot( term );
	}

	public void visit(OWLObjectOneOf enumeration) {
		Set<OWLIndividual> operands = enumeration.getIndividuals();
		ATerm[] terms = new ATerm[operands.size()];
		int size = 0;
		for( OWLIndividual ind : operands ) {
			ind.accept( this );
			terms[size++] = ATermUtils.makeValue( term );
		}
		// create a sorted set of terms so we will have a stable
		// concept creation and removal using this concept will work
		ATermList setOfTerms = size > 0
			? ATermUtils.toSet( terms, size )
			: ATermUtils.EMPTY_LIST;
		term = ATermUtils.makeOr( setOfTerms );
	}

	public void visit(OWLObjectSomeRestriction restriction) {
		restriction.getProperty().accept( this );
		ATerm p = term;
		restriction.getFiller().accept( this );
		ATerm c = term;

		term = ATermUtils.makeSomeValues( p, c );
	}

	public void visit(OWLObjectAllRestriction restriction) {
		restriction.getProperty().accept( this );
		ATerm p = term;
		restriction.getFiller().accept( this );
		ATerm c = term;

		term = ATermUtils.makeAllValues( p, c );
	}

	public void visit(OWLObjectValueRestriction restriction) {
		restriction.getProperty().accept( this );
		ATerm p = term;
		restriction.getValue().accept( this );
		ATermAppl ind = term;

		term = ATermUtils.makeHasValue( p, ind );
	}

	public void visit(OWLObjectExactCardinalityRestriction restriction) {
		addSimpleProperty( restriction.getProperty() );

		restriction.getProperty().accept( this );
		ATerm p = term;
		int n = restriction.getCardinality();
		restriction.getFiller().accept( this );
		ATermAppl desc = term;

		term = ATermUtils.makeCard( p, n, desc );
	}

	public void visit(OWLObjectMaxCardinalityRestriction restriction) {
		addSimpleProperty( restriction.getProperty() );

		restriction.getProperty().accept( this );
		ATerm p = term;
		int n = restriction.getCardinality();
		restriction.getFiller().accept( this );
		ATermAppl desc = term;

		term = ATermUtils.makeMax( p, n, desc );

	}

	public void visit(OWLObjectMinCardinalityRestriction restriction) {
		addSimpleProperty( restriction.getProperty() );

		restriction.getProperty().accept( this );
		ATerm p = term;
		int n = restriction.getCardinality();
		restriction.getFiller().accept( this );
		ATermAppl desc = term;

		term = ATermUtils.makeMin( p, n, desc );
	}

	public void visit(OWLDataExactCardinalityRestriction restriction) {
		restriction.getProperty().accept( this );
		ATerm p = term;
		int n = restriction.getCardinality();
		restriction.getFiller().accept( this );
		ATermAppl desc = term;

		term = ATermUtils.makeCard( p, n, desc );
	}

	public void visit(OWLDataMaxCardinalityRestriction restriction) {
		restriction.getProperty().accept( this );
		ATerm p = term;
		int n = restriction.getCardinality();
		restriction.getFiller().accept( this );
		ATermAppl desc = term;

		term = ATermUtils.makeMax( p, n, desc );
	}

	public void visit(OWLDataMinCardinalityRestriction restriction) {
		restriction.getProperty().accept( this );
		ATerm p = term;
		int n = restriction.getCardinality();
		restriction.getFiller().accept( this );
		ATermAppl desc = term;

		term = ATermUtils.makeMin( p, n, desc );
	}

	public void visit(OWLEquivalentClassesAxiom axiom) {
		Set<OWLDescription> descriptions = axiom.getDescriptions();
		int size = descriptions.size();
		if( size > 1 ) {
			ATermAppl[] terms = new ATermAppl[size];
			int index = 0;
			for( OWLDescription desc : descriptions ) {
				desc.accept( this );
				terms[index++] = term;
			}
			Arrays.sort( terms, 0, size, Comparators.hashCodeComparator );

			ATermAppl c1 = terms[0];

			for( int i = 1; i < terms.length; i++ ) {
				ATermAppl c2 = terms[i];

				if( addAxioms ) {
					kb.addEquivalentClass( c1, c2 );
				}
				else {
					// create the equivalence axiom
					ATermAppl sameAxiom = ATermUtils.makeSame( c1, c2 );

					// if removal fails we need to reload
					reloadRequired = !kb.removeAxiom( sameAxiom );
					// if removal is required there is no point to continue
					if( reloadRequired )
						return;
				}
			}
		}
	}

	public void visit(OWLDisjointClassesAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		Set<OWLDescription> descriptions = axiom.getDescriptions();
		int size = descriptions.size();
		if( size > 1 ) {
			ATermAppl[] terms = new ATermAppl[size];
			int index = 0;
			for( OWLDescription desc : descriptions ) {
				desc.accept( this );
				terms[index++] = term;
			}
			Arrays.sort( terms, 0, size, Comparators.hashCodeComparator );

			ATermList list = ATermUtils.toSet( terms, size );
			kb.addDisjointClasses( list );
		}
	}

	public void visit(OWLSubClassAxiom axiom) {
		axiom.getSubClass().accept( this );
		ATermAppl c1 = term;
		axiom.getSuperClass().accept( this );
		ATermAppl c2 = term;

		if( addAxioms ) {
			kb.addSubClass( c1, c2 );
		}
		else {
			// create the TBox axiom to remove
			ATermAppl subAxiom = ATermUtils.makeSub( c1, c2 );
			// reload is required if remove fails
			reloadRequired = !kb.removeAxiom( subAxiom );
		}
	}

	public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		Object[] eqs = axiom.getProperties().toArray();
		for( int i = 0; i < eqs.length; i++ ) {
			for( int j = i + 1; j < eqs.length; j++ ) {
				OWLProperty prop1 = (OWLProperty) eqs[i];
				OWLProperty prop2 = (OWLProperty) eqs[j];
				prop1.accept( this );
				ATermAppl p1 = term;
				prop2.accept( this );
				ATermAppl p2 = term;

				kb.addEquivalentProperty( p1, p2 );
			}
		}
	}

	public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		Object[] eqs = axiom.getProperties().toArray();
		for( int i = 0; i < eqs.length; i++ ) {
			for( int j = i + 1; j < eqs.length; j++ ) {
				OWLProperty prop1 = (OWLProperty) eqs[i];
				OWLProperty prop2 = (OWLProperty) eqs[j];
				prop1.accept( this );
				ATermAppl p1 = term;
				prop2.accept( this );
				ATermAppl p2 = term;

				kb.addEquivalentProperty( p1, p2 );
			}
		}
	}

	public void visit(OWLDifferentIndividualsAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		Object[] inds = axiom.getIndividuals().toArray();
		for( int i = 0; i < inds.length; i++ ) {
			((OWLIndividual) inds[i]).accept( this );
			ATermAppl i1 = term;
			for( int j = i + 1; j < inds.length; j++ ) {
				((OWLIndividual) inds[j]).accept( this );
				ATermAppl i2 = term;

				kb.addDifferent( i1, i2 );
			}
		}
	}

	public void visit(OWLSameIndividualsAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		Iterator eqs = axiom.getIndividuals().iterator();
		if( eqs.hasNext() ) {
			((OWLIndividual) eqs.next()).accept( this );
			ATermAppl i1 = term;

			while( eqs.hasNext() ) {
				((OWLIndividual) eqs.next()).accept( this );
				ATermAppl i2 = term;

				kb.addSame( i1, i2 );
			}
		}
	}

	public void visit(OWLDataOneOf enumeration) {
		ATermList ops = ATermUtils.EMPTY_LIST;
		for( Iterator it = enumeration.getValues().iterator(); it.hasNext(); ) {
			OWLConstant value = (OWLConstant) it.next();
			value.accept( this );
			ops = ops.insert( ATermUtils.makeValue( result() ) );
		}
		term = ATermUtils.makeOr( ops );
	}

	public void visit(OWLDataAllRestriction restriction) {
		restriction.getProperty().accept( this );
		ATerm p = term;
		restriction.getFiller().accept( this );
		ATerm c = term;

		term = ATermUtils.makeAllValues( p, c );
	}

	public void visit(OWLDataSomeRestriction restriction) {
		restriction.getProperty().accept( this );
		ATerm p = term;
		restriction.getFiller().accept( this );
		ATerm c = term;

		term = ATermUtils.makeSomeValues( p, c );
	}

	public void visit(OWLDataValueRestriction restriction) {
		restriction.getProperty().accept( this );
		ATermAppl p = term;
		restriction.getValue().accept( this );
		ATermAppl dv = term;

		term = ATermUtils.makeHasValue( p, dv );
	}

	public void visit(OWLOntology ont) {
		for( OWLAxiom axiom : ont.getAxioms() ) {
			monitor.incrementProgress();

			if( log.isDebugEnabled() )
				log.debug( "Load " + axiom );

			axiom.accept( this );
		}

		if( PelletOptions.DL_SAFE_RULES ) {
			for( OWLAxiom axiom : ont.getRules() ) {
				axiom.accept( this );
			}
		}
	}

	public void visit(OWLObjectSelfRestriction restriction) {
		addSimpleProperty( restriction.getProperty() );

		restriction.getProperty().accept( this );
		ATermAppl p = term;

		term = ATermUtils.makeSelf( p );
	}

	public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		Object[] disjs = axiom.getProperties().toArray();
		for( int i = 0; i < disjs.length - 1; i++ ) {
			OWLObjectProperty prop1 = (OWLObjectProperty) disjs[i];
			addSimpleProperty( prop1 );
			for( int j = i + 1; j < disjs.length; j++ ) {
				OWLObjectProperty prop2 = (OWLObjectProperty) disjs[j];
				addSimpleProperty( prop2 );
				prop1.accept( this );
				ATermAppl p1 = term;
				prop2.accept( this );
				ATermAppl p2 = term;

				kb.addDisjointProperty( p1, p2 );
			}
		}
	}

	public void visit(OWLDisjointDataPropertiesAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		Object[] disjs = axiom.getProperties().toArray();
		for( int i = 0; i < disjs.length; i++ ) {
			for( int j = i + 1; j < disjs.length; j++ ) {
				OWLDescription desc1 = (OWLDescription) disjs[i];
				OWLDescription desc2 = (OWLDescription) disjs[j];
				desc1.accept( this );
				ATermAppl p1 = term;
				desc2.accept( this );
				ATermAppl p2 = term;

				kb.addDisjointProperty( p1, p2 );
			}
		}
	}

	public void visit(OWLObjectPropertyChainSubPropertyAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		compositePropertyAxioms.add( getNamedProperty( axiom.getSuperProperty() ), axiom );

		axiom.getSuperProperty().accept( this );
		ATermAppl prop = result();

		List propChain = axiom.getPropertyChain();
		ATermList chain = ATermUtils.EMPTY_LIST;
		for( int i = propChain.size() - 1; i >= 0; i-- ) {
			OWLObjectProperty p = (OWLObjectProperty) propChain.get( i );

			p.accept( this );
			chain = chain.insert( result() );
		}

		kb.addSubProperty( chain, prop );
	}

	public void visit(OWLDisjointUnionAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getOWLClass().accept( this );
		ATermAppl c = term;

		ATermList classes = ATermUtils.EMPTY_LIST;
		for( Iterator it = axiom.getDescriptions().iterator(); it.hasNext(); ) {
			OWLDescription desc = (OWLDescription) it.next();
			desc.accept( this );
			classes = classes.insert( result() );
		}

		kb.addDisjointClasses( classes );
		kb.addEquivalentClass( c, ATermUtils.makeOr( classes ) );
	}

	public void visit(OWLDataComplementOf node) {
		String name = "Datatype" + node.hashCode();

		DatatypeReasoner dtReasoner = kb.getDatatypeReasoner();
		Datatype datatype = UnknownDatatype.instance;

		node.getDataRange().accept( this );
		Datatype baseDatatype = dtReasoner.getDatatype( term );

		datatype = dtReasoner.negate( baseDatatype );

		kb.addDatatype( name, datatype );
		term = ATermUtils.makeTermAppl( name );
	}

	public void visit(OWLDataRangeRestriction node) {
		String name = "Datatype" + node.hashCode();

		DatatypeReasoner dtReasoner = kb.getDatatypeReasoner();
		Datatype datatype = UnknownDatatype.instance;

		node.getDataRange().accept( this );
		Datatype baseDatatype = dtReasoner.getDatatype( term );

		if( baseDatatype instanceof XSDAtomicType ) {
			XSDAtomicType xsdType = (XSDAtomicType) baseDatatype;

			Set facets = node.getFacetRestrictions();
			for( Iterator i = facets.iterator(); i.hasNext(); ) {
				OWLDataRangeFacetRestriction restr = (OWLDataRangeFacetRestriction) i.next();
				OWLRestrictedDataRangeFacetVocabulary facet = restr.getFacet();
				OWLConstant facetValue = restr.getFacetValue();

				if( facet.equals( OWLRestrictedDataRangeFacetVocabulary.MIN_INCLUSIVE ) ) {
					Object value = xsdType.getPrimitiveType().getValue( facetValue.getLiteral(),
							xsdType.getURI() );
					xsdType = xsdType.restrictMinInclusive( value );
				}
				else if( facet.equals( OWLRestrictedDataRangeFacetVocabulary.MAX_INCLUSIVE ) ) {
					Object value = xsdType.getPrimitiveType().getValue( facetValue.getLiteral(),
							xsdType.getURI() );
					xsdType = xsdType.restrictMaxInclusive( value );
				}
				else if( facet.equals( OWLRestrictedDataRangeFacetVocabulary.MIN_EXCLUSIVE ) ) {
					Object value = xsdType.getPrimitiveType().getValue( facetValue.getLiteral(),
							xsdType.getURI() );
					xsdType = xsdType.restrictMinExclusive( value );
				}
				else if( facet.equals( OWLRestrictedDataRangeFacetVocabulary.MAX_EXCLUSIVE ) ) {
					Object value = xsdType.getPrimitiveType().getValue( facetValue.getLiteral(),
							xsdType.getURI() );
					xsdType = xsdType.restrictMaxExclusive( value );
				}
				else if( facet.equals( OWLRestrictedDataRangeFacetVocabulary.TOTAL_DIGITS ) ) {
					int n = Integer.parseInt( facetValue.getLiteral() );
					xsdType = xsdType.restrictTotalDigits( n );
				}
				else if( facet.equals( OWLRestrictedDataRangeFacetVocabulary.FRACTION_DIGITS ) ) {
					int n = Integer.parseInt( facetValue.getLiteral() );
					xsdType = xsdType.restrictFractionDigits( n );
				}
				else if( facet.equals( OWLRestrictedDataRangeFacetVocabulary.PATTERN ) ) {
					String str = facetValue.getLiteral();
					xsdType = xsdType.restrictPattern( str );
				}
				else {
					log.warn( "Unrecognized facet " + facet );
				}
			}

			datatype = xsdType;
		}
		else
			log.warn( "Unrecognized base datatype " + node.getDataRange() );

		kb.addDatatype( name, datatype );
		term = ATermUtils.makeTermAppl( name );
	}

	public void visit(OWLAntiSymmetricObjectPropertyAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		addSimpleProperty( axiom.getProperty() );

		axiom.getProperty().accept( this );
		ATermAppl p = term;

		kb.addAntisymmetricProperty( p );
	}

	public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getProperty().accept( this );
		ATermAppl p = term;

		kb.addReflexiveProperty( p );
	}

	public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		addSimpleProperty( axiom.getProperty() );

		axiom.getProperty().accept( this );
		ATermAppl p = term;

		kb.addFunctionalProperty( p );
	}

	public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getSubject().accept( this );
		ATermAppl s = term;
		axiom.getProperty().accept( this );
		ATermAppl p = term;
		axiom.getObject().accept( this );
		ATermAppl o = term;

		kb.addNegatedPropertyValue( p, s, o );
	}

	public void visit(OWLDataPropertyDomainAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getProperty().accept( this );
		ATermAppl p = term;
		axiom.getDomain().accept( this );
		ATermAppl c = term;

		kb.addDomain( p, c );
	}

	public void visit(OWLImportsDeclaration axiom) {
		if( log.isDebugEnabled() )
			log.debug( "Ignoring imports declaration: " + axiom );
	}

	public void visit(OWLAxiomAnnotationAxiom axiom) {
		if( log.isDebugEnabled() )
			log.debug( "Ignoring axiom annotation: " + axiom );
	}

	public void visit(OWLObjectPropertyDomainAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getProperty().accept( this );
		ATermAppl p = term;
		axiom.getDomain().accept( this );
		ATermAppl c = term;

		kb.addDomain( p, c );
	}

	public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getSubject().accept( this );
		ATermAppl s = term;
		axiom.getProperty().accept( this );
		ATermAppl p = term;
		axiom.getObject().accept( this );
		ATermAppl o = term;

		kb.addNegatedPropertyValue( p, s, o );
	}

	public void visit(OWLObjectPropertyRangeAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getProperty().accept( this );
		ATermAppl p = term;
		axiom.getRange().accept( this );
		ATermAppl c = term;

		kb.addRange( p, c );
	}

	public void visit(OWLObjectPropertyAssertionAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getSubject().accept( this );
		ATermAppl subj = term;
		axiom.getProperty().accept( this );
		ATermAppl pred = term;
		axiom.getObject().accept( this );
		ATermAppl obj = term;

		kb.addPropertyValue( pred, subj, obj );
	}

	public void visit(OWLObjectSubPropertyAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getSubProperty().accept( this );
		ATermAppl sub = term;
		axiom.getSuperProperty().accept( this );
		ATermAppl sup = term;

		kb.addSubProperty( sub, sup );
	}

	public void visit(OWLDeclarationAxiom axiom) {
		if( log.isDebugEnabled() )
			log.debug( "Ignoring declaration: " + axiom );
	}

	public void visit(OWLEntityAnnotationAxiom axiom) {
		if( log.isDebugEnabled() )
			log.debug( "Ignoring entity annotation: " + axiom );
	}

	public void visit(OWLOntologyAnnotationAxiom axiom) {
		if( log.isDebugEnabled() )
			log.debug( "Ignoring ontology annotation: " + axiom );
	}

	public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getProperty().accept( this );
		ATermAppl p = term;

		kb.addSymmetricProperty( p );
	}

	public void visit(OWLDataPropertyRangeAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getProperty().accept( this );
		ATermAppl p = term;
		axiom.getRange().accept( this );
		ATermAppl c = term;

		kb.addRange( p, c );
	}

	public void visit(OWLFunctionalDataPropertyAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getProperty().accept( this );
		ATermAppl p = term;

		kb.addFunctionalProperty( p );
	}

	public void visit(OWLClassAssertionAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getIndividual().accept( this );
		ATermAppl ind = term;
		axiom.getDescription().accept( this );
		ATermAppl c = term;

		kb.addType( ind, c );
	}

	public void visit(OWLDataPropertyAssertionAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getSubject().accept( this );
		ATermAppl subj = term;
		axiom.getProperty().accept( this );
		ATermAppl pred = term;
		axiom.getObject().accept( this );
		ATermAppl obj = term;

		kb.addPropertyValue( pred, subj, obj );
	}

	public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		compositePropertyAxioms.add( getNamedProperty( axiom.getProperty() ), axiom );

		axiom.getProperty().accept( this );
		ATermAppl p = term;

		kb.addTransitiveProperty( p );
	}

	public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		addSimpleProperty( axiom.getProperty() );

		axiom.getProperty().accept( this );
		ATermAppl p = term;

		kb.addIrreflexiveProperty( p );
	}

	public void visit(OWLDataSubPropertyAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getSubProperty().accept( this );
		ATermAppl p1 = term;
		axiom.getSuperProperty().accept( this );
		ATermAppl p2 = term;

		kb.addSubProperty( p1, p2 );
	}

	public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		addSimpleProperty( axiom.getProperty() );

		axiom.getProperty().accept( this );
		ATermAppl p = term;

		kb.addInverseFunctionalProperty( p );
	}

	public void visit(OWLInverseObjectPropertiesAxiom axiom) {
		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		axiom.getFirstProperty().accept( this );
		ATermAppl p1 = term;
		axiom.getSecondProperty().accept( this );
		ATermAppl p2 = term;

		kb.addInverseProperty( p1, p2 );
	}

	public void visit(OWLDataRangeFacetRestriction node) {
		// nothing to do here
	}

	public void visit(OWLObjectAnnotation annotation) {
		if( log.isDebugEnabled() )
			log.debug( "Ignoring object annotation: " + annotation );
	}

	public void visit(OWLConstantAnnotation annotation) {
		if( log.isDebugEnabled() )
			log.debug( "Ignoring constant annotation: " + annotation );
	}

	public void visit(SWRLRule rule) {
		if( !PelletOptions.DL_SAFE_RULES )
			return;

		if( !addAxioms ) {
			reloadRequired = true;
			return;
		}

		List<Triple> head = parseAtomList( rule.getHead() );
		List<Triple> body = parseAtomList( rule.getBody() );

		if( head == null || body == null ) {
			addUnsupportedAxiom( rule );

			return;
		}

		for( Triple triple : head ) {
			List singletonHead = Collections.singletonList( triple );

			Rule reteRule = new Rule( body, singletonHead );
			kb.addRule( reteRule );
		}
	}

	private List<Triple> parseAtomList(Set<SWRLAtom> atomList) {
		List<Triple> atoms = new ArrayList<Triple>();

		for( SWRLAtom atom : atomList ) {
			atom.accept( this );

			if( swrlTriple == null )
				return null;

			atoms.add( swrlTriple );
		}

		return atoms;
	}

	public void visit(SWRLClassAtom atom) {
		OWLDescription c = atom.getPredicate();
		if( c.isAnonymous() ) {
			swrlTerm = null;
			return;
		}

		SWRLAtomIObject v = atom.getArgument();
		v.accept( this );

		Term subj = swrlTerm;
		Term pred = Constant.TYPE;

		c.accept( this );
		Term obj = new Constant( term.toString() );

		swrlTriple = new Triple( subj, pred, obj );
	}

	public void visit(SWRLDataRangeAtom atom) {
		swrlTriple = null;
	}

	public void visit(SWRLObjectPropertyAtom atom) {
		if( atom.getPredicate().isAnonymous() ) {
			swrlTriple = null;
			return;
		}

		atom.getFirstArgument().accept( this );
		Term subj = swrlTerm;

		atom.getSecondArgument().accept( this );
		Term obj = swrlTerm;

		atom.getPredicate().accept( this );
		Term pred = new Constant( term.toString() );

		swrlTriple = new Triple( subj, pred, obj );
	}

	public void visit(SWRLDataValuedPropertyAtom atom) {
		swrlTriple = null;
	}

	public void visit(SWRLSameAsAtom atom) {
		atom.getFirstArgument().accept( this );
		Term subj = swrlTerm;

		atom.getSecondArgument().accept( this );
		Term obj = swrlTerm;

		Term pred = Constant.SAME_AS;

		swrlTriple = new Triple( subj, pred, obj );
	}

	public void visit(SWRLDifferentFromAtom atom) {
		atom.getFirstArgument().accept( this );
		Term subj = swrlTerm;

		atom.getSecondArgument().accept( this );
		Term obj = swrlTerm;

		Term pred = Constant.DIFF_FROM;

		swrlTriple = new Triple( subj, pred, obj );
	}

	public void visit(SWRLBuiltInAtom atom) {
		swrlTriple = null;
	}

	public void visit(SWRLAtomDVariable dvar) {
		throw new AssertionError( "Encountered SWRLAtomDVariable " + dvar
				+ " but DatavaluePropertyAtom is not supported" );
	}

	public void visit(SWRLAtomIVariable ivar) {
		swrlTerm = new Variable( ivar.getURI().toString() );
	}

	public void visit(SWRLAtomIndividualObject iobj) {
		swrlTerm = new Variable( iobj.getIndividual().getURI().toString() );
	}

	public void visit(SWRLAtomConstantObject cons) {
		throw new AssertionError( "Encountered SWRLAtomConstantObject " + cons
				+ " but DatavaluePropertyAtom is not supported" );
	}

	public ProgressMonitor getMonitor() {
		return monitor;
	}

	public void setMonitor(ProgressMonitor monitor) {
		this.monitor = monitor;
	}

}
