package org.mindswap.pellet.owlapi;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owl.model.OWLAntiSymmetricObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLAxiomAnnotationAxiom;
import org.semanticweb.owl.model.OWLAxiomVisitor;
import org.semanticweb.owl.model.OWLClassAssertionAxiom;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owl.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owl.model.OWLDataSubPropertyAxiom;
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
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyChainSubPropertyAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;
import org.semanticweb.owl.model.OWLObjectPropertyInverse;
import org.semanticweb.owl.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owl.model.OWLObjectSubPropertyAxiom;
import org.semanticweb.owl.model.OWLOntologyAnnotationAxiom;
import org.semanticweb.owl.model.OWLPropertyExpression;
import org.semanticweb.owl.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLSameIndividualsAxiom;
import org.semanticweb.owl.model.OWLSubClassAxiom;
import org.semanticweb.owl.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owl.model.SWRLRule;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2006
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 * 
 * @author Evren Sirin
 */
public class EntailmentChecker implements OWLAxiomVisitor {
	public static Log	log			= LogFactory.getLog( EntailmentChecker.class );

	private Reasoner	reasoner;
	private boolean		isEntailed	= false;

	public EntailmentChecker(Reasoner reasoner) {
		this.reasoner = reasoner;
	}

	public boolean isEntailed(OWLAxiom axiom) {
		isEntailed = false;

		axiom.accept( this );

		return isEntailed;
	}

	private OWLObjectProperty _getProperty(OWLObjectPropertyExpression pe) {
		while( pe.isAnonymous() )
			pe = ((OWLObjectPropertyInverse) pe).getInverse();

		return (OWLObjectProperty) pe;
	}

	private OWLPropertyExpression _normalize(OWLPropertyExpression pe) {
		OWLPropertyExpression inverse = null;
		boolean returnInv = false;

		while( pe.isAnonymous() ) {
			inverse = pe;
			pe = ((OWLObjectPropertyInverse) pe).getInverse();
			returnInv = !returnInv;
		}

		return returnInv
			? inverse
			: pe;
	}

	public void visit(OWLSubClassAxiom axiom) {
		isEntailed = reasoner.isSubClassOf( axiom.getSubClass(), axiom.getSuperClass() );
	}

	public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub

	}

	public void visit(OWLAntiSymmetricObjectPropertyAxiom axiom) {
		isEntailed = reasoner.isAntiSymmetric( (OWLObjectProperty) axiom.getProperty() );
	}

	public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
		isEntailed = reasoner.isReflexive( (OWLObjectProperty) axiom.getProperty() );
	}

	public void visit(OWLDisjointClassesAxiom axiom) {
		isEntailed = true;

		ArrayList list = new ArrayList( axiom.getDescriptions() );
		for( int i = 0; i < list.size() - 1; i++ ) {
			OWLDescription head = (OWLDescription) list.get( i );
			for( int j = i + 1; j < list.size() - 1; j++ ) {
				OWLDescription next = (OWLDescription) list.get( j );

				if( !reasoner.isDisjointWith( head, next ) ) {
					isEntailed = false;
					return;
				}
			}
		}
	}

	public void visit(OWLDataPropertyDomainAxiom axiom) {
		isEntailed = reasoner.hasDomain( (OWLDataProperty) axiom.getProperty(), axiom.getDomain() );
	}

	public void visit(OWLImportsDeclaration axiom) {
		isEntailed = true;
		if( log.isDebugEnabled() )
			log.debug( "Ignoring imports declaration " + axiom );
	}

	public void visit(OWLAxiomAnnotationAxiom axiom) {
		isEntailed = true;
		if( log.isDebugEnabled() )
			log.debug( "Ignoring axiom annotation " + axiom );
	}

	public void visit(OWLObjectPropertyDomainAxiom axiom) {
		isEntailed = reasoner
				.hasDomain( (OWLObjectProperty) axiom.getProperty(), axiom.getDomain() );
	}

	public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
		isEntailed = true;

		Iterator i = axiom.getProperties().iterator();
		if( i.hasNext() ) {
			OWLObjectProperty head = (OWLObjectProperty) i.next();

			while( i.hasNext() && isEntailed ) {
				OWLObjectProperty next = (OWLObjectProperty) i.next();

				isEntailed = reasoner.isEquivalentProperty( head, next );
			}
		}
	}

	public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub

	}

	public void visit(OWLDifferentIndividualsAxiom axiom) {
		isEntailed = true;

		ArrayList<OWLIndividual> list = new ArrayList<OWLIndividual>( axiom.getIndividuals() );
		for( int i = 0; i < list.size() - 1; i++ ) {
			OWLIndividual head = list.get( i );
			for( int j = i + 1; j < list.size() - 1; j++ ) {
				OWLIndividual next = list.get( j );

				if( !reasoner.isDifferentFrom( head, next ) ) {
					isEntailed = false;
					return;
				}
			}
		}
	}

	public void visit(OWLDisjointDataPropertiesAxiom axiom) {
		isEntailed = true;

		ArrayList list = new ArrayList( axiom.getProperties() );
		for( int i = 0; i < list.size() - 1; i++ ) {
			OWLDataProperty head = (OWLDataProperty) list.get( i );
			for( int j = i + 1; j < list.size() - 1; j++ ) {
				OWLDataProperty next = (OWLDataProperty) list.get( j );

				if( !reasoner.isDisjointWith( head, next ) ) {
					isEntailed = false;
					return;
				}
			}
		}
	}

	public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
		isEntailed = true;

		ArrayList list = new ArrayList( axiom.getProperties() );
		for( int i = 0; i < list.size() - 1; i++ ) {
			OWLObjectProperty head = (OWLObjectProperty) list.get( i );
			for( int j = i + 1; j < list.size() - 1; j++ ) {
				OWLObjectProperty next = (OWLObjectProperty) list.get( j );

				if( !reasoner.isDisjointWith( head, next ) ) {
					isEntailed = false;
					return;
				}
			}
		}
	}

	public void visit(OWLObjectPropertyRangeAxiom axiom) {
		isEntailed = reasoner.hasRange( (OWLObjectProperty) axiom.getProperty(), axiom.getRange() );
	}

	public void visit(OWLObjectPropertyAssertionAxiom axiom) {
		isEntailed = reasoner.hasObjectPropertyRelationship( axiom.getSubject(), axiom
				.getProperty(), axiom.getObject() );
	}

	public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
		isEntailed = reasoner.isFunctional( (OWLObjectProperty) axiom.getProperty() );
	}

	public void visit(OWLObjectSubPropertyAxiom axiom) {
		isEntailed = reasoner.isSubPropertyOf( (OWLObjectProperty) axiom.getSubProperty(),
				(OWLObjectProperty) axiom.getSuperProperty() );
	}

	public void visit(OWLDisjointUnionAxiom axiom) {
		// TODO Auto-generated method stub

	}

	public void visit(OWLDeclarationAxiom axiom) {
		isEntailed = true;
		if( log.isDebugEnabled() )
			log.debug( "Ignoring declaration " + axiom );
	}

	public void visit(OWLEntityAnnotationAxiom axiom) {
		isEntailed = true;
		if( log.isDebugEnabled() )
			log.debug( "Ignoring entity annotation " + axiom );
	}

	public void visit(OWLOntologyAnnotationAxiom axiom) {
		isEntailed = true;
		if( log.isDebugEnabled() )
			log.debug( "Ignoring ontology annotation " + axiom );
	}

	public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
		isEntailed = reasoner.isSymmetric( (OWLObjectProperty) axiom.getProperty() );
	}

	public void visit(OWLDataPropertyRangeAxiom axiom) {
		isEntailed = reasoner.hasRange( (OWLDataProperty) axiom.getProperty(), axiom.getRange() );
	}

	public void visit(OWLFunctionalDataPropertyAxiom axiom) {
		isEntailed = reasoner.isFunctional( (OWLDataProperty) axiom.getProperty() );
	}

	public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
		Iterator i = axiom.getProperties().iterator();
		if( i.hasNext() ) {
			OWLDataProperty head = (OWLDataProperty) i.next();

			while( i.hasNext() && isEntailed ) {
				OWLDataProperty next = (OWLDataProperty) i.next();

				isEntailed = reasoner.isEquivalentProperty( head, next );
			}
		}
	}

	public void visit(OWLClassAssertionAxiom axiom) {
		OWLIndividual ind = axiom.getIndividual();
		OWLDescription c = axiom.getDescription();

		if( ind.isAnonymous() )
			isEntailed = reasoner.isSatisfiable( c );
		else
			isEntailed = reasoner.hasType( ind, c );
	}

	public void visit(OWLEquivalentClassesAxiom axiom) {
		isEntailed = true;

		Iterator i = axiom.getDescriptions().iterator();
		if( i.hasNext() ) {
			OWLDescription head = (OWLDescription) i.next();

			while( i.hasNext() && isEntailed ) {
				OWLDescription next = (OWLDescription) i.next();

				isEntailed = reasoner.isEquivalentClass( head, next );
			}
		}
	}

	public void visit(OWLDataPropertyAssertionAxiom axiom) {
		isEntailed = reasoner.hasDataPropertyRelationship( axiom.getSubject(), axiom.getProperty(),
				axiom.getObject() );
	}

	public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
		isEntailed = reasoner.isTransitive( (OWLObjectProperty) axiom.getProperty() );
	}

	public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
		isEntailed = reasoner.isIrreflexive( (OWLObjectProperty) axiom.getProperty() );
	}

	public void visit(OWLDataSubPropertyAxiom axiom) {
		isEntailed = reasoner.isSubPropertyOf( (OWLDataProperty) axiom.getSubProperty(),
				(OWLDataProperty) axiom.getSuperProperty() );
	}

	public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
		isEntailed = reasoner.isInverseFunctional( (OWLObjectProperty) axiom.getProperty() );
	}

	public void visit(OWLSameIndividualsAxiom axiom) {
		isEntailed = true;

		Iterator i = axiom.getIndividuals().iterator();
		if( i.hasNext() ) {
			OWLIndividual head = (OWLIndividual) i.next();

			while( i.hasNext() ) {
				OWLIndividual next = (OWLIndividual) i.next();

				if( !reasoner.isSameAs( head, next ) ) {
					isEntailed = false;
					return;
				}
			}
		}
	}

	public void visit(OWLObjectPropertyChainSubPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	public void visit(OWLInverseObjectPropertiesAxiom axiom) {
		isEntailed = reasoner.isSubPropertyOf( (OWLObjectProperty) axiom.getFirstProperty(),
				(OWLObjectProperty) axiom.getSecondProperty() );
	}

	public void visit(SWRLRule rule) {
		// TODO Auto-generated method stub

	}

}
