/*
 * Created on May 25, 2005
 */
package org.mindswap.pellet.owlapi;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.output.ATermBaseVisitor;
import org.mindswap.pellet.output.ATermVisitor;
import org.semanticweb.owl.model.OWLConstant;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataType;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObject;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLProperty;

import aterm.ATermAppl;
import aterm.ATermList;

/**
 * Converts concepts expressed as ATerms to OWL-API structures.
 * 
 * @author Evren Sirin
 */
public class ConceptConverter extends ATermBaseVisitor implements ATermVisitor {
	private OWLOntology		ont;
	private OWLDataFactory	factory;
	private OWLObject		obj;
	private Set				set;

	public ConceptConverter(OWLOntology ont, OWLDataFactory factory) {
		this.ont = ont;
		this.factory = factory;
	}

	public OWLObject convert(ATermAppl term) {
		obj = null;

		visit( term );

		return obj;
	}

	public OWLObject getResult() {
		return obj;
	}

	public void visitTerm(ATermAppl term) {
		obj = null;

		URI uri = URI.create( term.getName() );
		if( ont.containsClassReference( uri ) )
			obj = factory.getOWLClass( uri );
		else if( ont.containsObjectPropertyReference( uri ) )
			obj = factory.getOWLObjectProperty( uri );
		else if( ont.containsDataPropertyReference( uri ) )
			obj = factory.getOWLDataProperty( uri );
		else if( ont.containsIndividualReference( uri ) )
			obj = factory.getOWLIndividual( uri );
		else if( ont.containsDataTypeReference( uri ) )
			obj = factory.getOWLDataType( uri );

		if( obj == null )
			throw new InternalReasonerException( "Ontology does not contain: " + term );
	}

	public void visitAnd(ATermAppl term) {
		visitList( (ATermList) term.getArgument( 0 ) );

		obj = factory.getOWLObjectIntersectionOf( set );

	}

	public void visitOr(ATermAppl term) {
		visitList( (ATermList) term.getArgument( 0 ) );

		obj = factory.getOWLObjectUnionOf( set );

	}

	public void visitNot(ATermAppl term) {
		visit( (ATermAppl) term.getArgument( 0 ) );

		obj = factory.getOWLObjectComplementOf( (OWLDescription) obj );
	}

	public void visitSome(ATermAppl term) {
		visitTerm( (ATermAppl) term.getArgument( 0 ) );
		OWLProperty prop = (OWLProperty) obj;

		visit( (ATermAppl) term.getArgument( 1 ) );

		if( prop instanceof OWLObjectProperty ) {
			OWLDescription desc = (OWLDescription) obj;

			obj = factory.getOWLObjectSomeRestriction( (OWLObjectProperty) prop, desc );
		}
		else {
			OWLDataType datatype = (OWLDataType) obj;

			obj = factory.getOWLDataSomeRestriction( (OWLDataProperty) prop, datatype );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mindswap.pellet.output.ATermVisitor#visitAll(aterm.ATermAppl)
	 */
	public void visitAll(ATermAppl term) {
		visitTerm( (ATermAppl) term.getArgument( 0 ) );
		OWLProperty prop = (OWLProperty) obj;

		visit( (ATermAppl) term.getArgument( 1 ) );

		if( prop instanceof OWLObjectProperty ) {
			OWLDescription desc = (OWLDescription) obj;

			obj = factory.getOWLObjectAllRestriction( (OWLObjectProperty) prop, desc );
		}
		else {
			OWLDataType datatype = (OWLDataType) obj;

			obj = factory.getOWLDataAllRestriction( (OWLDataProperty) prop, datatype );
		}

	}

	public void visitMin(ATermAppl term) {
		visitTerm( (ATermAppl) term.getArgument( 0 ) );
		OWLProperty prop = (OWLProperty) obj;

		int cardinality = Integer.parseInt( term.getArgument( 1 ).toString() );

		if( prop instanceof OWLObjectProperty ) {
			obj = factory.getOWLObjectMinCardinalityRestriction( (OWLObjectProperty) prop,
					cardinality );
		}
		else {
			obj = factory.getOWLDataMinCardinalityRestriction( (OWLDataProperty) prop, cardinality );
		}
	}

	public void visitCard(ATermAppl term) {
		visitTerm( (ATermAppl) term.getArgument( 0 ) );
		OWLProperty prop = (OWLProperty) obj;

		int cardinality = Integer.parseInt( term.getArgument( 1 ).toString() );

		if( prop instanceof OWLObjectProperty ) {
			obj = factory.getOWLObjectExactCardinalityRestriction( (OWLObjectProperty) prop,
					cardinality );
		}
		else {
			obj = factory.getOWLDataExactCardinalityRestriction( (OWLDataProperty) prop,
					cardinality );
		}
	}

	public void visitMax(ATermAppl term) {
		visitTerm( (ATermAppl) term.getArgument( 0 ) );
		OWLProperty prop = (OWLProperty) obj;

		int cardinality = Integer.parseInt( term.getArgument( 1 ).toString() );

		if( prop instanceof OWLObjectProperty ) {
			obj = factory.getOWLObjectMaxCardinalityRestriction( (OWLObjectProperty) prop,
					cardinality );
		}
		else {
			obj = factory.getOWLDataMaxCardinalityRestriction( (OWLDataProperty) prop, cardinality );
		}
	}

	public void visitHasValue(ATermAppl term) {
		visitTerm( (ATermAppl) term.getArgument( 0 ) );
		OWLProperty prop = (OWLProperty) obj;

		visit( (ATermAppl) term.getArgument( 1 ) );

		if( prop instanceof OWLObjectProperty ) {
			OWLIndividual ind = (OWLIndividual) obj;

			obj = factory.getOWLObjectValueRestriction( (OWLObjectProperty) prop, ind );
		}
		else {
			OWLConstant dataVal = (OWLConstant) obj;

			obj = factory.getOWLDataValueRestriction( (OWLDataProperty) prop, dataVal );
		}
	}

	public void visitValue(ATermAppl term) {
		visit( (ATermAppl) term.getArgument( 0 ) );
	}

	public void visitSelf(ATermAppl term) {
		visitTerm( (ATermAppl) term.getArgument( 0 ) );
		OWLObjectProperty prop = (OWLObjectProperty) obj;

		obj = factory.getOWLObjectSelfRestriction( prop );

	}

	public void visitOneOf(ATermAppl term) {
		visitList( (ATermList) term.getArgument( 0 ) );

		obj = factory.getOWLObjectOneOf( set );

	}

	public void visitLiteral(ATermAppl term) {
		// literal(lexicalValue, language, datatypeURI)

		String lexValue = ((ATermAppl) term.getArgument( 0 )).toString();
		String lang = ((ATermAppl) term.getArgument( 1 )).toString();
		URI dtypeURI = URI.create( ((ATermAppl) term.getArgument( 2 )).toString() );
		if( dtypeURI != null ) {
			OWLDataType datatype = factory.getOWLDataType( dtypeURI );
			obj = factory.getOWLTypedConstant( lexValue, datatype );
		}
		else if( lang.equals( "" ) )
			obj = factory.getOWLUntypedConstant( lexValue, lang );
		else
			obj = factory.getOWLUntypedConstant( lexValue );

	}

	public void visitList(ATermList list) {
		this.set = null;
		Set elements = new HashSet();
		while( !list.isEmpty() ) {
			ATermAppl term = (ATermAppl) list.getFirst();
			visit( term );
			if( obj == null )
				return;
			elements.add( obj );
			list = list.getNext();
		}
		this.set = elements;
	}
}
