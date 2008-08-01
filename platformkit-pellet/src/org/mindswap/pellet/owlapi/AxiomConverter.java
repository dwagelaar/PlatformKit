/*
 * Created on May 25, 2005
 */
package org.mindswap.pellet.owlapi;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.ABox;
import org.mindswap.pellet.utils.ATermUtils;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataPropertyExpression;
import org.semanticweb.owl.model.OWLDataRange;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLEntity;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLProperty;

import aterm.ATermAppl;
import aterm.ATermList;

/**
 * <p>Title: AxiomConverter</p>
 *
 * <p>Description: Converts axioms expressed as ATerms to OWL-API structures.</p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: Clark & Parsia, LLC. <http://www.clarkparsia.com></p>
 *
 * @author Evren Sirin
 */
public class AxiomConverter {
	public static Log			log	= LogFactory.getLog( ABox.class );

	private ConceptConverter	converter;
	private OWLDataFactory		factory;

	public AxiomConverter(OWLOntology ont, OWLDataFactory factory) {
		this.factory = factory;
		this.converter = new ConceptConverter( ont, factory );
	}

	public OWLAxiom convert(ATermAppl term) {
		OWLAxiom axiom = null;

		if( term.getAFun().equals( ATermUtils.SAMEFUN ) ) {
			OWLDescription c1 = (OWLDescription) converter.convert( (ATermAppl) term
					.getArgument( 0 ) );
			OWLDescription c2 = (OWLDescription) converter.convert( (ATermAppl) term
					.getArgument( 1 ) );

			Set<OWLDescription> descriptions = new HashSet<OWLDescription>();
			descriptions.add( c1 );
			descriptions.add( c2 );

			if( c1 != null && c2 != null )
				axiom = factory.getOWLEquivalentClassesAxiom( descriptions );
		}
		else if( term.getAFun().equals( ATermUtils.SUBFUN ) ) {
			OWLDescription c1 = (OWLDescription) converter.convert( (ATermAppl) term
					.getArgument( 0 ) );
			OWLDescription c2 = (OWLDescription) converter.convert( (ATermAppl) term
					.getArgument( 1 ) );

			if( c1 != null && c2 != null )
				axiom = factory.getOWLSubClassAxiom( c1, c2 );
		}
		else if( term.getAFun().equals( ATermUtils.DISJOINTSFUN ) ) {
			Set<OWLDescription> descriptions = new HashSet<OWLDescription>();

			ATermList concepts = (ATermList) term.getArgument( 0 );
			for( ; !concepts.isEmpty(); concepts = concepts.getNext() ) {
				ATermAppl concept = (ATermAppl) concepts.getFirst();
				OWLDescription c = (OWLDescription) converter.convert( concept );
				if( c == null )
					break;

				descriptions.add( c );
			}

			if( concepts.isEmpty() )
				axiom = factory.getOWLDisjointClassesAxiom( descriptions );
		}
		else if( term.getAFun().equals( ATermUtils.DISJOINTFUN ) ) {
			OWLDescription c1 = (OWLDescription) converter.convert( (ATermAppl) term
					.getArgument( 0 ) );
			OWLDescription c2 = (OWLDescription) converter.convert( (ATermAppl) term
					.getArgument( 1 ) );

			Set<OWLDescription> descriptions = new HashSet<OWLDescription>();
			descriptions.add( c1 );
			descriptions.add( c2 );

			if( c1 != null && c2 != null )
				axiom = factory.getOWLDisjointClassesAxiom( descriptions );
		}
		else if( term.getAFun().equals( ATermUtils.SUBPROPFUN ) ) {
			OWLObjectProperty p1 = (OWLObjectProperty) converter.convert( (ATermAppl) term
					.getArgument( 0 ) );
			OWLObjectProperty p2 = (OWLObjectProperty) converter.convert( (ATermAppl) term
					.getArgument( 1 ) );

			if( p1 != null && p2 != null )
				axiom = factory.getOWLSubObjectPropertyAxiom( p1, p2 );
		}
		else if( term.getAFun().equals( ATermUtils.DOMAINFUN ) ) {
			OWLProperty p = (OWLProperty) converter.convert( (ATermAppl) term.getArgument( 0 ) );
			OWLDescription c = (OWLDescription) converter
					.convert( (ATermAppl) term.getArgument( 1 ) );

			if( c != null && p != null ) {
				if( p instanceof OWLObjectProperty )
					axiom = factory.getOWLObjectPropertyDomainAxiom(
							(OWLObjectPropertyExpression) p, c );
				else
					axiom = factory
							.getOWLDataPropertyDomainAxiom( (OWLDataPropertyExpression) p, c );
			}
		}
		else if( term.getAFun().equals( ATermUtils.RANGEFUN ) ) {
			OWLEntity e = (OWLEntity) converter.convert( (ATermAppl) term.getArgument( 1 ) );
			if( e != null ) {
				if( e instanceof OWLDescription ) {
					OWLObjectProperty p = (OWLObjectProperty) converter.convert( (ATermAppl) term
							.getArgument( 0 ) );
					if( p != null )
						axiom = factory.getOWLObjectPropertyRangeAxiom( p, (OWLDescription) e );
				}
				else {
					OWLDataProperty p = (OWLDataProperty) converter.convert( (ATermAppl) term
							.getArgument( 0 ) );
					if( p != null )
						axiom = factory.getOWLDataPropertyRangeAxiom( p, (OWLDataRange) e );
				}
			}
		}
		else if( term.getAFun().equals( ATermUtils.INVPROPFUN ) ) {
			OWLObjectProperty p1 = (OWLObjectProperty) converter.convert( (ATermAppl) term
					.getArgument( 0 ) );
			OWLObjectProperty p2 = (OWLObjectProperty) converter.convert( (ATermAppl) term
					.getArgument( 1 ) );

			if( p1 != null && p2 != null )
				axiom = factory.getOWLInverseObjectPropertiesAxiom( p1, p2 );
		}
		else if( term.getAFun().equals( ATermUtils.TRANSITIVEFUN ) ) {
			OWLObjectProperty p = (OWLObjectProperty) converter.convert( (ATermAppl) term
					.getArgument( 0 ) );

			if( p != null )
				axiom = factory.getOWLTransitiveObjectPropertyAxiom( p );
		}
		else if( term.getAFun().equals( ATermUtils.FUNCTIONALFUN ) ) {
			OWLProperty p = (OWLProperty) converter.convert( (ATermAppl) term.getArgument( 0 ) );

			if( p != null ) {
				if( p instanceof OWLObjectProperty )
					axiom = factory
							.getOWLFunctionalObjectPropertyAxiom( (OWLObjectPropertyExpression) p );
				else
					axiom = factory
							.getOWLFunctionalDataPropertyAxiom( (OWLDataPropertyExpression) p );
			}
		}
		else if( term.getAFun().equals( ATermUtils.TYPEFUN ) ) {
			OWLIndividual i = (OWLIndividual) converter.convert( (ATermAppl) term.getArgument( 0 ) );
			OWLDescription c = (OWLDescription) converter
					.convert( (ATermAppl) term.getArgument( 1 ) );

			if( i != null && c != null )
				axiom = factory.getOWLClassAssertionAxiom( i, c );
		}

		if( axiom == null )
			log.warn( "Cannot convert to OWLAPI: " + term );

		return axiom;
	}
}
