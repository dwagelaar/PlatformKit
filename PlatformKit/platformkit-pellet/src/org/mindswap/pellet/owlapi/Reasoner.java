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
 * Created on Jan 10, 2004
 */
package org.mindswap.pellet.owlapi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.utils.ATermUtils;
import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLConstant;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataPropertyExpression;
import org.semanticweb.owl.model.OWLDataRange;
import org.semanticweb.owl.model.OWLDataType;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObject;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChange;
import org.semanticweb.owl.model.OWLOntologyChangeListener;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLProperty;
import org.semanticweb.owl.model.OWLRuntimeException;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 */
public class Reasoner implements OWLReasoner, OWLOntologyChangeListener {
	public static Log			log					= LogFactory.getLog( Reasoner.class );

	private static final long	serialVersionUID	= 8438190652175258123L;

	protected KnowledgeBase		kb;

	private PelletLoader		loader;

	private OWLOntologyManager	manager;

	private interface EntityMapper<T extends OWLObject> {
		public T map(ATermAppl term);
	}

	private EntityMapper<OWLIndividual>		IND_MAPPER		= new EntityMapper<OWLIndividual>() {
																public OWLIndividual map(
																		ATermAppl term) {
																	return manager
																			.getOWLDataFactory()
																			.getOWLIndividual(
																					uri( term ) );
																}
															};

	private EntityMapper<OWLConstant>		LIT_MAPPER		= new EntityMapper<OWLConstant>() {
																public OWLConstant map(
																		ATermAppl term) {
																	String value = ((ATermAppl) term
																			.getArgument( 0 ))
																			.getName();
																	String lang = ((ATermAppl) term
																			.getArgument( 1 ))
																			.getName();
																	ATermAppl datatypeURI = ((ATermAppl) term
																			.getArgument( 2 ));

																	if( !datatypeURI
																			.equals( ATermUtils.EMPTY ) ) {
																		OWLDataType datatype = DT_MAPPER
																				.map( datatypeURI );
																		return manager
																				.getOWLDataFactory()
																				.getOWLTypedConstant(
																						value,
																						datatype );
																	}
																	else if( lang.equals( "" ) )
																		return manager
																				.getOWLDataFactory()
																				.getOWLUntypedConstant(
																						value );
																	else
																		return manager
																				.getOWLDataFactory()
																				.getOWLUntypedConstant(
																						value, lang );
																}
															};

	private EntityMapper<OWLObjectProperty>	OP_MAPPER		= new EntityMapper<OWLObjectProperty>() {
																public OWLObjectProperty map(
																		ATermAppl term) {
																	return manager
																			.getOWLDataFactory()
																			.getOWLObjectProperty(
																					uri( term ) );
																}
															};

	private EntityMapper<OWLDataProperty>	DP_MAPPER		= new EntityMapper<OWLDataProperty>() {
																public OWLDataProperty map(
																		ATermAppl term) {
																	return manager
																			.getOWLDataFactory()
																			.getOWLDataProperty(
																					uri( term ) );
																}
															};

	private EntityMapper<OWLDataType>		DT_MAPPER		= new EntityMapper<OWLDataType>() {
																public OWLDataType map(
																		ATermAppl term) {
																	return manager
																			.getOWLDataFactory()
																			.getOWLDataType(
																					uri( term ) );
																}
															};

	private EntityMapper<OWLClass>			CLASS_MAPPER	= new EntityMapper<OWLClass>() {
																public OWLClass map(ATermAppl term) {
																	if( term
																			.equals( ATermUtils.TOP ) )
																		return manager
																				.getOWLDataFactory()
																				.getOWLThing();
																	else if( term
																			.equals( ATermUtils.BOTTOM ) )
																		return manager
																				.getOWLDataFactory()
																				.getOWLNothing();
																	else
																		return manager
																				.getOWLDataFactory()
																				.getOWLClass(
																						uri( term ) );
																}
															};

	private static URI uri(ATermAppl term) {
		if( term.getArity() != 0 )
			throw new OWLRuntimeException( "Trying to convert an anonymous term " + term );

		try {
			return new URI( term.getName() );
		} catch( URISyntaxException x ) {
			throw new OWLRuntimeException( "Cannot create URI from term " + x );
		}
	}

	/**
	 * @deprecated Use {@link #Reasoner(OWLOntologyManager)} instead
	 */
	public Reasoner() {
		this( null );
	}

	public Reasoner(OWLOntologyManager manager) {
		this.kb = new KnowledgeBase();
		this.loader = new PelletLoader( kb );
		this.manager = manager;

		loader.setManager( manager );
	}

	/**
	 * @deprecated Use {@link #getIndividuals(OWLDescription, boolean)} with
	 *             <code>false</code>
	 */
	public Set allInstancesOf(OWLClass c) {
		return getIndividuals( c, false );
	}

	/**
	 * @deprecated Use {@link #getTypes(OWLIndividual, boolean)} with
	 *             <code>false</code>
	 */
	public Set allTypesOf(OWLIndividual ind) {
		return getTypes( ind, false );
	}

	/**
	 * Use {@link #getAncestorClasses(OWLDescription)}
	 * 
	 * @deprecated
	 */
	public Set ancestorClassesOf(OWLClass c) {
		return getAncestorClasses( c );
	}

	/**
	 * Use {@link #getAncestorClasses(OWLDescription)}
	 * 
	 * @deprecated
	 */
	public Set ancestorClassesOf(OWLDescription c) {
		return getAncestorClasses( c );
	}

	/**
	 * Use {@link #getAncestorProperties(OWLObjectProperty)}
	 * 
	 * @deprecated
	 */
	public Set ancestorPropertiesOf(OWLProperty p) {
		if( p instanceof OWLObjectProperty )
			return getAncestorProperties( (OWLObjectProperty) p );
		else if( p instanceof OWLDataProperty )
			return getAncestorProperties( (OWLDataProperty) p );

		throw new AssertionError( p + " is not an Object or Data property" );
	}

	public void classify() {
		kb.classify();
	}

	public void clearOntologies() {
		loader.clear();
	}

	/**
	 * @deprecated Use {@link #getComplementClasses(OWLDescription)}
	 */
	public Set complementClassesOf(OWLDescription c) {
		return getComplementClasses( c );
	}

	/**
	 * @deprecated Use {@link #getDescendantClasses(OWLDescription)}
	 */
	public Set descendantClassesOf(OWLClass c) {
		return getDescendantClasses( c );
	}

	/**
	 * Use {@link #getDescendantClasses(OWLDescription)}
	 * 
	 * @deprecated
	 */
	public Set descendantClassesOf(OWLDescription c) {
		return getDescendantClasses( c );
	}

	/**
	 * @deprecated Use {@link #getDescendantSubProperties(OWLDataProperty)}
	 */
	public Set descendantPropertiesOf(OWLProperty p) {
		if( p instanceof OWLObjectProperty )
			return getDescendantProperties( (OWLObjectProperty) p );
		else if( p instanceof OWLDataProperty )
			return getDescendantProperties( (OWLDataProperty) p );

		throw new AssertionError( p + " is not an Object or Data property" );
	}

	/**
	 * @deprecated Use {@link #getDisjointClasses(OWLDescription)} instead
	 */
	public Set disjointClassesOf(OWLDescription c) {
		return getDisjointClasses( c );
	}

	public void dispose() {
		kb = null;
	}

	/**
	 * Use {@link #getDomains(OWLObjectProperty)}
	 * 
	 * @deprecated
	 */
	public Set domainsOf(OWLProperty p) {
		if( p instanceof OWLObjectProperty )
			return getDomains( (OWLObjectProperty) p );
		else if( p instanceof OWLDataProperty )
			return getDomains( (OWLDataProperty) p );

		throw new AssertionError( p + " is not an Object or Data property" );
	}

	/**
	 * @deprecated Use {@link #getEquivalentClasses(OWLDescription)}
	 */
	public Set equivalentClassesOf(OWLClass c) {
		return getEquivalentClasses( c );
	}

	/**
	 * @deprecated Use {@link #getEquivalentClasses(OWLDescription)}
	 */
	public Set equivalentClassesOf(OWLDescription c) {
		return toOWLEntitySet( kb.getEquivalentClasses( loader.term( c ) ), CLASS_MAPPER );
	}

	/**
	 * @deprecated Use {@link #getEquivalentProperties(OWLProperty)} instead
	 */
	public Set equivalentPropertiesOf(OWLProperty p) {
		return getEquivalentProperties( p );
	}

	public Set<Set<OWLClass>> getAncestorClasses(OWLDescription c) {
		return toOWLEntitySetOfSet( kb.getSuperClasses( loader.term( c ) ), CLASS_MAPPER );
	}

	public Set<Set<OWLDataProperty>> getAncestorProperties(OWLDataProperty p) {
		return toOWLEntitySetOfSet( kb.getSuperProperties( loader.term( p ) ), DP_MAPPER );
	}

	public Set<Set<OWLObjectProperty>> getAncestorProperties(OWLObjectProperty p) {
		return toOWLEntitySetOfSet( kb.getSuperProperties( loader.term( p ) ), OP_MAPPER );
	}

	/**
	 * @deprecated Use {@link OWLDataFactory} functions instead
	 */
	public OWLClass getClass(URI uri) {
		return manager.getOWLDataFactory().getOWLClass( uri );
	}

	/**
	 * Return the set of all named classes defined in any of the ontologies
	 * loaded in the reasoner.
	 * 
	 * @return set of OWLClass objects
	 */
	public Set<OWLClass> getClasses() {
		return toOWLEntitySet( kb.getClasses(), CLASS_MAPPER );
	}

	public Set<OWLClass> getComplementClasses(OWLDescription c) {
		return toOWLEntitySet( kb.getComplements( loader.term( c ) ), CLASS_MAPPER );
	}

	public Set<OWLDataProperty> getDataProperties() {
		return toOWLEntitySet( kb.getDataProperties(), DP_MAPPER );
	}

	/**
	 * @deprecated Use {@link OWLDataFactory} functions instead
	 */
	public OWLDataProperty getDataProperty(URI uri) {
		return manager.getOWLDataFactory().getOWLDataProperty( uri );
	}

	public Map getDataPropertyRelationships(OWLIndividual individual) {
		Map<OWLDataProperty, Set<OWLConstant>> values = new HashMap<OWLDataProperty, Set<OWLConstant>>();
		Set dataProps = getDataProperties();
		for( Iterator i = dataProps.iterator(); i.hasNext(); ) {
			OWLDataProperty prop = (OWLDataProperty) i.next();
			Set<OWLConstant> set = getRelatedValues( individual, prop );
			if( !set.isEmpty() )
				values.put( prop, set );
		}

		return values;
	}

	/**
	 * @deprecated Use {@link #getDataPropertyRelationships(OWLIndividual)}
	 *             instead
	 */
	public Map getDataPropertyValues(OWLIndividual ind) {
		return getDataPropertyRelationships( ind );
	}

	public Set<Set<OWLClass>> getDescendantClasses(OWLDescription c) {
		return toOWLEntitySetOfSet( kb.getSubClasses( loader.term( c ) ), CLASS_MAPPER );
	}

	public Set<Set<OWLDataProperty>> getDescendantProperties(OWLDataProperty p) {
		return toOWLEntitySetOfSet( kb.getSubProperties( loader.term( p ), true ), DP_MAPPER );
	}

	public Set<Set<OWLObjectProperty>> getDescendantProperties(OWLObjectProperty p) {
		return toOWLEntitySetOfSet( kb.getSubProperties( loader.term( p ), true ), OP_MAPPER );
	}

	public Set getDifferentFromIndividuals(OWLIndividual ind) {
		return toOWLEntitySet( kb.getDifferents( loader.term( ind ) ), IND_MAPPER );
	}

	public Set<Set<OWLClass>> getDisjointClasses(OWLDescription c) {
		return toOWLEntitySetOfSet( kb.getDisjoints( loader.term( c ) ), CLASS_MAPPER );
	}

	public Set getDomains(OWLDataProperty p) {
		return toOWLEntitySet( kb.getDomains( loader.term( p ) ), CLASS_MAPPER );
	}

	public Set getDomains(OWLObjectProperty p) {
		return toOWLEntitySet( kb.getDomains( loader.term( p ) ), CLASS_MAPPER );
	}

	public Set<OWLClass> getEquivalentClasses(OWLDescription c) {
		return toOWLEntitySet( kb.getEquivalentClasses( loader.term( c ) ), CLASS_MAPPER );

	}

	public Set<OWLDataProperty> getEquivalentProperties(OWLDataProperty p) {
		return toOWLEntitySet( kb.getEquivalentProperties( loader.term( p ) ), DP_MAPPER );
	}

	public Set<OWLObjectProperty> getEquivalentProperties(OWLObjectProperty p) {
		return toOWLEntitySet( kb.getEquivalentProperties( loader.term( p ) ), OP_MAPPER );
	}

	public Set getEquivalentProperties(OWLProperty p) {
		if( p instanceof OWLObjectProperty )
			return getEquivalentProperties( (OWLObjectProperty) p );
		else if( p instanceof OWLDataProperty )
			return getEquivalentProperties( (OWLDataProperty) p );

		throw new AssertionError( p + " is not an Object or Data property" );
	}

	public Set<OWLClass> getInconsistentClasses() {
		return toOWLEntitySet( kb.getEquivalentClasses( ATermUtils.BOTTOM ), CLASS_MAPPER );
	}

	/**
	 * @deprecated Use {@link OWLDataFactory} functions instead
	 */
	public OWLIndividual getIndividual(URI uri) {
		return manager.getOWLDataFactory().getOWLIndividual( uri );
	}

	/**
	 * Return the set of all individuals defined in any of the ontologies loaded
	 * in the reasoner.
	 * 
	 * @return set of OWLIndividual objects
	 */
	public Set<OWLIndividual> getIndividuals() {
		return toOWLEntitySet( kb.getIndividuals(), IND_MAPPER );
	}

	/**
	 * Returns all or only direct instances of a concept expression
	 */
	public Set<OWLIndividual> getIndividuals(OWLDescription clsC, boolean direct) {
		return toOWLEntitySet( kb.getInstances( loader.term( clsC ), direct ), IND_MAPPER );
	}

	public Set<Set<OWLObjectProperty>> getInverseProperties(OWLObjectProperty prop) {
		return Collections.singleton( toOWLEntitySet( kb.getInverses( loader.term( prop ) ),
				OP_MAPPER ) );
	}

	/**
	 * @return Returns the kb.
	 */
	public KnowledgeBase getKB() {
		return kb;
	}

	public Set<OWLOntology> getLoadedOntologies() {
		return loader.getOntologies();
	}

	public PelletLoader getLoader() {
		return loader;
	}

	public OWLOntologyManager getManager() {
		return manager;
	}

	public Set<OWLObjectProperty> getObjectProperties() {
		return toOWLEntitySet( kb.getObjectProperties(), OP_MAPPER );
	}

	/**
	 * @deprecated Use {@link OWLDataFactory} functions instead
	 */
	public OWLObjectProperty getObjectProperty(URI uri) {
		return manager.getOWLDataFactory().getOWLObjectProperty( uri );
	}

	public Map<OWLObjectProperty, Set<OWLIndividual>> getObjectPropertyRelationships(
			OWLIndividual individual) {
		Map<OWLObjectProperty, Set<OWLIndividual>> values = new HashMap<OWLObjectProperty, Set<OWLIndividual>>();
		Set<OWLObjectProperty> objProps = getObjectProperties();
		for( OWLObjectProperty prop : objProps ) {
			Set<OWLIndividual> set = getRelatedIndividuals( individual, prop );
			if( !set.isEmpty() )
				values.put( prop, set );
		}

		return values;
	}

	/**
	 * @deprecated Use {@link #getObjectPropertyRelationships(OWLIndividual)}
	 *             instead
	 */
	public Map getObjectPropertyValues(OWLIndividual ind) {
		return getObjectPropertyRelationships( ind );
	}

	/**
	 * Returns the set of all loaded ontologies.
	 * 
	 * @deprecated Use {@link #getLoadedOntologies()} instead
	 */
	public Set getOntologies() {
		return getLoadedOntologies();
	}

	/**
	 * @deprecated Use {@link #getOWLOntology()}
	 */
	public OWLOntology getOntology() {
		return null;
	}

	/**
	 * Return all the object and data properties defined in the loaded
	 * ontologies
	 */
	public Set<OWLProperty> getProperties() {
		Set<OWLProperty> properties = new HashSet<OWLProperty>();
		properties.addAll( getObjectProperties() );
		properties.addAll( getDataProperties() );

		return properties;
	}

	/**
	 * @deprecated Use
	 *             {@link #getRelatedValue(OWLIndividual, OWLDataPropertyExpression)}
	 *             instead
	 */
	public OWLConstant getPropertyValue(OWLIndividual ind, OWLDataProperty prop) {
		return getRelatedValue( ind, prop );
	}

	/**
	 * @deprecated Use
	 *             {@link #getRelatedIndividual(OWLIndividual, OWLObjectPropertyExpression)}
	 *             instead
	 */
	public OWLIndividual getPropertyValue(OWLIndividual ind, OWLObjectProperty prop) {
		return getRelatedIndividual( ind, prop );
	}

	public Map<OWLIndividual, Set<OWLConstant>> getPropertyValues(OWLDataProperty prop) {
		Map<OWLIndividual, Set<OWLConstant>> map = new HashMap<OWLIndividual, Set<OWLConstant>>();
		ATermAppl p = loader.term( prop );
		Collection candidates = kb.retrieveIndividualsWithProperty( p );
		for( Iterator i = candidates.iterator(); i.hasNext(); ) {
			ATermAppl candidate = (ATermAppl) i.next();
			List<ATermAppl> list = kb.getDataPropertyValues( p, candidate );
			if( list.isEmpty() )
				continue;

			OWLIndividual subj = IND_MAPPER.map( candidate );
			Set<OWLConstant> objects = toOWLEntitySet( list, LIT_MAPPER );

			map.put( subj, objects );
		}

		return map;
	}

	/**
	 * @deprecated Use
	 *             {@link #getRelatedValues(OWLIndividual, OWLDataPropertyExpression)}
	 *             instead
	 */
	public Set getPropertyValues(OWLIndividual ind, OWLDataProperty prop) {
		return getRelatedValues( ind, prop );
	}

	/**
	 * @deprecated Use
	 *             {@link #getRelatedIndividuals(OWLIndividual, OWLObjectPropertyExpression)}
	 *             instead
	 */
	public Set getPropertyValues(OWLIndividual ind, OWLObjectProperty prop) {
		return getRelatedIndividuals( ind, prop );
	}

	public Set getPropertyValues(OWLIndividual ind, OWLProperty prop) {
		if( prop instanceof OWLObjectProperty )
			return getRelatedIndividuals( ind, (OWLObjectProperty) prop );
		else if( prop instanceof OWLDataProperty )
			return getRelatedValues( ind, (OWLDataProperty) prop );

		throw new AssertionError( "Property " + prop + " is neither data nor object property!" );
	}

	public Map<OWLIndividual, Set<OWLIndividual>> getPropertyValues(OWLObjectProperty prop) {
		Map<OWLIndividual, Set<OWLIndividual>> result = new HashMap<OWLIndividual, Set<OWLIndividual>>();
		ATermAppl p = loader.term( prop );

		Map<ATermAppl, List<ATermAppl>> values = kb.getPropertyValues( p );
		for( Iterator i = values.keySet().iterator(); i.hasNext(); ) {
			ATermAppl subjTerm = (ATermAppl) i.next();

			List<ATermAppl> objTerms = values.get( subjTerm );

			OWLIndividual subj = IND_MAPPER.map( subjTerm );

			Set<OWLIndividual> objects = toOWLEntitySet( objTerms, IND_MAPPER );

			result.put( subj, objects );
		}

		return result;
	}

	public Map getPropertyValues(OWLProperty prop) {
		if( prop instanceof OWLObjectProperty )
			return getPropertyValues( (OWLObjectProperty) prop );
		else if( prop instanceof OWLDataProperty )
			return getPropertyValues( (OWLDataProperty) prop );

		throw new AssertionError( "Property " + prop + " is neither data nor object property!" );
	}

	public Set getRanges(OWLDataProperty p) {
		return toOWLEntitySet( kb.getRanges( loader.term( p ) ), DT_MAPPER );
	}

	public Set getRanges(OWLObjectProperty p) {
		return toOWLEntitySet( kb.getRanges( loader.term( p ) ), CLASS_MAPPER );
	}

	public OWLIndividual getRelatedIndividual(OWLIndividual subject,
			OWLObjectPropertyExpression property) {
		Set values = getRelatedIndividuals( subject, property );
		return values.isEmpty()
			? null
			: (OWLIndividual) values.iterator().next();
	}

	public Set<OWLIndividual> getRelatedIndividuals(OWLIndividual subject,
			OWLObjectPropertyExpression property) {
		return toOWLEntitySet( kb.getObjectPropertyValues( loader.term( property ), loader
				.term( subject ) ), IND_MAPPER );
	}

	public OWLConstant getRelatedValue(OWLIndividual subject, OWLDataPropertyExpression property) {
		Set values = getRelatedValues( subject, property );
		return values.isEmpty()
			? null
			: (OWLConstant) values.iterator().next();
	}

	public Set<OWLConstant> getRelatedValues(OWLIndividual subject,
			OWLDataPropertyExpression property) {
		return toOWLEntitySet( kb.getDataPropertyValues( loader.term( property ), loader
				.term( subject ) ), LIT_MAPPER );
	}

	/**
	 * Return a set of sameAs individuals given a specific individual based on
	 * axioms in the ontology
	 * 
	 * @param ind -
	 *            specific individual to test
	 * @return
	 * @throws OWLException
	 */
	public Set getSameAsIndividuals(OWLIndividual ind) {
		return toOWLEntitySet( kb.getSames( loader.term( ind ) ), IND_MAPPER );
	}

	public Set<Set<OWLClass>> getSubClasses(OWLDescription c) {
		return toOWLEntitySetOfSet( kb.getSubClasses( loader.term( c ), true ), CLASS_MAPPER );
	}

	public Set<Set<OWLDataProperty>> getSubProperties(OWLDataProperty p) {
		return toOWLEntitySetOfSet( kb.getSubProperties( loader.term( p ), true ), DP_MAPPER );
	}

	public Set<Set<OWLObjectProperty>> getSubProperties(OWLObjectProperty p) {
		return toOWLEntitySetOfSet( kb.getSubProperties( loader.term( p ), true ), OP_MAPPER );
	}

	public Set<Set<OWLClass>> getSuperClasses(OWLDescription c) {
		return toOWLEntitySetOfSet( kb.getSuperClasses( loader.term( c ), true ), CLASS_MAPPER );
	}

	public Set<Set<OWLDataProperty>> getSuperProperties(OWLDataProperty p) {
		return toOWLEntitySetOfSet( kb.getSuperProperties( loader.term( p ), true ), DP_MAPPER );
	}

	public Set<Set<OWLObjectProperty>> getSuperProperties(OWLObjectProperty p) {
		return toOWLEntitySetOfSet( kb.getSuperProperties( loader.term( p ), true ), OP_MAPPER );
	}

	/**
	 * Return the named class that this individual is a direct type of. If there
	 * is more than one such class first one is returned.
	 * 
	 * @param ind
	 * @return OWLClass
	 * @throws OWLException
	 */
	public OWLClass getType(OWLIndividual ind) {
		Set types = getTypes( ind );

		// this is a set of sets so get the first set
		types = types.isEmpty()
			? types
			: (Set) types.iterator().next();

		return types.isEmpty()
			? null
			: (OWLClass) types.iterator().next();
	}

	public Set<Set<OWLClass>> getTypes(OWLIndividual individual) {
		return toOWLEntitySetOfSet( kb.getTypes( loader.term( individual ), true ), CLASS_MAPPER );
	}

	/**
	 * Returns all the named classes that this individual belongs. This returns
	 * a set of sets where each set is an equivalent class
	 * 
	 * @param ind
	 * @return Set of OWLDescription objects
	 * @throws OWLException
	 */
	public Set<Set<OWLClass>> getTypes(OWLIndividual ind, boolean direct) {
		return toOWLEntitySetOfSet( kb.getTypes( loader.term( ind ), direct ), CLASS_MAPPER );
	}

	public boolean hasDataPropertyRelationship(OWLIndividual subject,
			OWLDataPropertyExpression property, OWLConstant object) {
		return kb.hasPropertyValue( loader.term( subject ), loader.term( property ), loader
				.term( object ) );
	}

	public boolean hasDomain(OWLDataProperty p, OWLDescription c) {
		return kb.hasDomain( loader.term( p ), loader.term( c ) );
	}

	public boolean hasDomain(OWLObjectProperty p, OWLDescription c) {
		return kb.hasDomain( loader.term( p ), loader.term( c ) );
	}

	public boolean hasObjectPropertyRelationship(OWLIndividual subject,
			OWLObjectPropertyExpression property, OWLIndividual object) {
		return kb.hasPropertyValue( loader.term( subject ), loader.term( property ), loader
				.term( object ) );
	}

	/**
	 * @deprecated Use
	 *             {@link #hasDataPropertyRelationship(OWLIndividual, OWLDataPropertyExpression, OWLConstant)}
	 *             instead
	 */
	public boolean hasPropertyValue(OWLIndividual subj, OWLDataProperty prop, OWLConstant obj) {
		return hasDataPropertyRelationship( subj, prop, obj );
	}

	/**
	 * @deprecated Use
	 *             {@link #hasObjectPropertyRelationship(OWLIndividual, OWLObjectPropertyExpression, OWLIndividual)}
	 *             instead
	 */
	public boolean hasPropertyValue(OWLIndividual subj, OWLObjectProperty prop, OWLIndividual obj) {
		return hasObjectPropertyRelationship( subj, prop, obj );
	}

	public boolean hasRange(OWLDataProperty p, OWLDataRange d) {
		return kb.hasRange( loader.term( p ), loader.term( d ) );
	}

	public boolean hasRange(OWLObjectProperty p, OWLDescription c) {
		return kb.hasRange( loader.term( p ), loader.term( c ) );
	}

	/**
	 * Checks if the given individual is an instance of the given type
	 */
	public boolean hasType(OWLIndividual individual, OWLDescription type) {
		return kb.isType( loader.term( individual ), loader.term( type ) );
	}

	/**
	 * Checks if the given individual is a direct or indirect instance of the
	 * given type
	 */
	public boolean hasType(OWLIndividual individual, OWLDescription type, boolean direct)
			throws OWLReasonerException {
		if( direct )
			return getTypes( individual, direct ).contains( type );
		else
			return hasType( individual, type );
	}

	/**
	 * @deprecated Use {@link #getIndividuals(OWLDescription, boolean)} with
	 *             <code>true</code>
	 */
	public Set instancesOf(OWLClass c) {
		return getIndividuals( c, false );
	}

	/**
	 * @deprecated Use {@link #getIndividuals(OWLDescription, boolean)} with
	 *             <code>true</code>
	 */
	public Set instancesOf(OWLDescription d) {
		return getIndividuals( d, false );
	}

	/**
	 * @deprecated Use {@link #getInverseProperties(OWLObjectProperty)} instead
	 */
	public Set inversePropertiesOf(OWLObjectProperty prop) {
		return getInverseProperties( prop );
	}

	public boolean isAntiSymmetric(OWLObjectProperty p) {
		return kb.isAntisymmetricProperty( loader.term( p ) );
	}

	public boolean isClassified() {
		return kb.isClassified();
	}

	public boolean isComplementOf(OWLDescription c1, OWLDescription c2) {
		return kb.isComplement( loader.term( c1 ), loader.term( c2 ) );
	}

	/**
	 * Returns true if the loaded ontology is consistent.
	 * 
	 * @param c
	 * @return
	 * @throws OWLException
	 */
	public boolean isConsistent() {
		return kb.isConsistent();
	}

	/**
	 * @deprecated Use {@link #isSatisfiable(OWLDescription)} instead
	 */
	public boolean isConsistent(OWLDescription d) {
		return isSatisfiable( d );
	}

	public boolean isConsistent(OWLOntology ontology) {
		setOntology( ontology );

		return isConsistent();
	}

	public boolean isDefined(OWLClass cls) {
		ATermAppl term = loader.term( cls );

		return kb.isClass( term );
	}

	public boolean isDefined(OWLDataProperty prop) {
		ATermAppl term = loader.term( prop );

		return kb.isDatatypeProperty( term );
	}

	public boolean isDefined(OWLIndividual ind) {
		ATermAppl term = loader.term( ind );

		return kb.isObjectProperty( term );
	}

	public boolean isDefined(OWLObjectProperty prop) {
		ATermAppl term = loader.term( prop );

		return kb.isObjectProperty( term );
	}

	/**
	 * Test if two individuals are owl:DifferentFrom each other.
	 * 
	 * @return
	 * @throws OWLException
	 */
	public boolean isDifferentFrom(OWLIndividual ind1, OWLIndividual ind2) {
		return kb.isDifferentFrom( loader.term( ind1 ), loader.term( ind2 ) );
	}

	public boolean isDisjointWith(OWLDataProperty p1, OWLDataProperty p2) {
		return kb.isDisjoint( loader.term( p1 ), loader.term( p2 ) );
	}

	public boolean isDisjointWith(OWLDescription c1, OWLDescription c2) {
		return kb.isDisjointClass( loader.term( c1 ), loader.term( c2 ) );
	}

	public boolean isDisjointWith(OWLObjectProperty p1, OWLObjectProperty p2) {
		return kb.isDisjointProperty( loader.term( p1 ), loader.term( p2 ) );
	}

	public boolean isEntailed(OWLOntology ont) {
		return isEntailed( ont.getAxioms() );
	}

	public boolean isEntailed(Set axioms) {
		if( axioms.isEmpty() ) {
			log.warn( "Empty ontologies are entailed by any premise document!" );
		}
		else {
			EntailmentChecker entailmentChecker = new EntailmentChecker( this );
			for( Iterator i = axioms.iterator(); i.hasNext(); ) {
				OWLAxiom axiom = (OWLAxiom) i.next();

				if( !entailmentChecker.isEntailed( axiom ) ) {
					log.warn( "Axiom not entailed: (" + axiom + ")" );
					return false;
				}
			}
		}

		return true;
	}

	public boolean isEntailed(OWLAxiom axiom) {
		EntailmentChecker entailmentChecker = new EntailmentChecker( this );

		return entailmentChecker.isEntailed( axiom );
	}

	public boolean isEquivalentClass(OWLDescription c1, OWLDescription c2) {
		return kb.isEquivalentClass( loader.term( c1 ), loader.term( c2 ) );
	}

	public boolean isEquivalentProperty(OWLDataProperty p1, OWLDataProperty p2) {
		return kb.isEquivalentProperty( loader.term( p1 ), loader.term( p2 ) );
	}

	public boolean isEquivalentProperty(OWLObjectProperty p1, OWLObjectProperty p2) {
		return kb.isEquivalentProperty( loader.term( p1 ), loader.term( p2 ) );
	}

	public boolean isFunctional(OWLDataProperty p) {
		return kb.isFunctionalProperty( loader.term( p ) );
	}

	public boolean isFunctional(OWLObjectProperty p) {
		return kb.isFunctionalProperty( loader.term( p ) );
	}

	/**
	 * @deprecated Use {@link #hasType(OWLIndividual, OWLDescription)} instead
	 */
	public boolean isInstanceOf(OWLIndividual ind, OWLClass c) {
		return hasType( ind, c );
	}

	/**
	 * @deprecated Use {@link #hasType(OWLIndividual, OWLDescription)} instead
	 */
	public boolean isInstanceOf(OWLIndividual ind, OWLDescription d) {
		return hasType( ind, d );
	}

	public boolean isInverseFunctional(OWLObjectProperty p) {
		return kb.isInverseFunctionalProperty( loader.term( p ) );
	}

	public boolean isIrreflexive(OWLObjectProperty p) {
		return kb.isIrreflexiveProperty( loader.term( p ) );
	}

	public boolean isRealised() throws OWLReasonerException {
		return kb.isRealized();
	}

	public boolean isReflexive(OWLObjectProperty p) {
		return kb.isReflexiveProperty( loader.term( p ) );
	}

	/**
	 * Test if two individuals are owl:DifferentFrom each other.
	 * 
	 * @return
	 * @throws OWLException
	 */
	public boolean isSameAs(OWLIndividual ind1, OWLIndividual ind2) {
		return kb.isSameAs( loader.term( ind1 ), loader.term( ind2 ) );
	}

	/**
	 * Returns true if the given class is satisfiable.
	 * 
	 * @param c
	 * @return
	 * @throws OWLException
	 */
	public boolean isSatisfiable(OWLDescription d) {
		if( !kb.isConsistent() )
			return false;

		return kb.isSatisfiable( loader.term( d ) );
	}

	public boolean isSubClassOf(OWLDescription c1, OWLDescription c2) {
		return kb.isSubClassOf( loader.term( c1 ), loader.term( c2 ) );
	}

	public boolean isSubPropertyOf(OWLDataProperty c1, OWLDataProperty c2) {
		return kb.isSubPropertyOf( loader.term( c1 ), loader.term( c2 ) );
	}

	public boolean isSubPropertyOf(OWLObjectProperty c1, OWLObjectProperty c2) {
		return kb.isSubPropertyOf( loader.term( c1 ), loader.term( c2 ) );
	}

	public boolean isSubTypeOf(OWLDataType d1, OWLDataType d2) {
		return kb.isSubClassOf( loader.term( d1 ), loader.term( d2 ) );
	}

	public boolean isSymmetric(OWLObjectProperty p) {
		return kb.isSymmetricProperty( loader.term( p ) );
	}

	public boolean isTransitive(OWLObjectProperty p) {
		return kb.isTransitiveProperty( loader.term( p ) );
	}

	public void loadOntologies(Set<OWLOntology> ontologies) {
		if( manager == null ) {
			log
					.warn( "Cannot load an ontology without an ontology manager. Use setManager(OWLOntologyManager) first." );
			return;
		}

		loader.load( ontologies );
	}

	/**
	 * @deprecated Use {@link #loadOntology(OWLOntology)} instead
	 */
	public void loadOntologies(OWLOntology ontology) {
		loadOntology( ontology );
	}

	public void loadOntology(OWLOntology ontology) {
		loadOntologies( Collections.singleton( ontology ) );
	}

	/**
	 * Listens to ontology changes and refreshes the underlying KB. Applies some
	 * of the ontology changes incrementally but applies full reload if
	 * incremental update cannot be handled for one of the changes. All
	 * additions can be handled incrementally but removal of some axioms cannot
	 * be handled. Note that, the incremental processing here is meant to refer
	 * only loading and not reasoning, i.e. it is different from the incremental
	 * reasoning support provided by Pellet.
	 */
	public void ontologiesChanged(List<? extends OWLOntologyChange> changes) {
		boolean changesApplied = loader.applyChanges( changes );

		if( !changesApplied ) {
			refresh();
		}
	}

	/**
	 * Use {@link #getRanges(OWLObjectProperty)}
	 * 
	 * @deprecated
	 */
	public Set rangesOf(OWLProperty p) {
		if( p instanceof OWLObjectProperty )
			return getRanges( (OWLObjectProperty) p );
		else if( p instanceof OWLDataProperty )
			return getRanges( (OWLDataProperty) p );

		throw new AssertionError( p + " is not an Object or Data property" );
	}

	public void realise() throws OWLReasonerException {
		kb.realize();
	}

	public void refresh() {
		loader.reload();
	}

	/**
	 * @deprecated Use {@link #refresh()} instead
	 */
	public void refreshOntology() {
		refresh();
	}

	/**
	 * @deprecated Preferred method of setting the ontology manager is using the
	 *             constructor {@link #Reasoner(OWLOntologyManager)}
	 */
	public void setManager(OWLOntologyManager manager) {
		this.manager = manager;
		loader.setManager( manager );
	}

	/**
	 * This will first clear the ontologies and then load the give ontology with
	 * is imports
	 */
	public void setOntology(OWLOntology ontology) {
		clearOntologies();
		loadOntologies( Collections.singleton( ontology ) );
	}

	/**
	 * Use {@link #getSubClasses(OWLDescription)}
	 * 
	 * @deprecated
	 */
	public Set subClassesOf(OWLClass c) {
		return getSubClasses( c );
	}

	/**
	 * Use {@link #getSubClasses(OWLDescription)}
	 * 
	 * @deprecated
	 */
	public Set subClassesOf(OWLDescription c) {
		return getSubClasses( c );
	}

	/**
	 * Use {@link #getSubProperties(OWLObjectProperty)}
	 * 
	 * @deprecated
	 */
	public Set subPropertiesOf(OWLProperty p) {
		if( p instanceof OWLObjectProperty )
			return getSubProperties( (OWLObjectProperty) p );
		else if( p instanceof OWLDataProperty )
			return getSubProperties( (OWLDataProperty) p );

		throw new AssertionError( p + " is not an Object or Data property" );
	}

	/**
	 * Use {@link #getSuperClasses(OWLDescription)}
	 * 
	 * @deprecated
	 */
	public Set superClassesOf(OWLClass c) {
		return getSuperClasses( c );
	}

	/**
	 * Use {@link #getSuperClasses(OWLDescription)}
	 * 
	 * @deprecated
	 */
	public Set superClassesOf(OWLDescription c) {
		return getSuperClasses( c );
	}

	/**
	 * Use {@link #getSuperProperties(OWLObjectProperty)}
	 * 
	 * @deprecated
	 */
	public Set superPropertiesOf(OWLProperty p) {
		if( p instanceof OWLObjectProperty )
			return getSuperProperties( (OWLObjectProperty) p );
		else if( p instanceof OWLDataProperty )
			return getSuperProperties( (OWLDataProperty) p );

		throw new AssertionError( p + " is not an Object or Data property" );
	}

	private <T extends OWLObject> Set<Set<T>> toOWLEntitySetOfSet(Set<Set<ATermAppl>> setOfTerms,
			EntityMapper<T> mapper) {
		Set<Set<T>> results = new HashSet<Set<T>>();
		for( Set<ATermAppl> terms : setOfTerms ) {
			Set<T> entitySet = toOWLEntitySet( terms, mapper );
			if( !entitySet.isEmpty() )
				results.add( entitySet );
		}

		return results;
	}

	private <T extends OWLObject> Set<T> toOWLEntitySet(Collection<ATermAppl> terms,
			EntityMapper<T> mapper) {
		Set<T> results = new HashSet<T>();
		for( ATermAppl term : terms )
			results.add( mapper.map( term ) );

		return results;
	}

	/**
	 * @deprecated Use {@link #getType(OWLIndividual)} instead
	 */
	public OWLClass typeOf(OWLIndividual ind) {
		return getType( ind );
	}

	/**
	 * @deprecated Use {@link #getTypes(OWLIndividual, boolean)} with
	 *             <code>true</code>
	 */
	public Set typesOf(OWLIndividual ind) {
		return getTypes( ind );
	}

	public void unloadOntologies(Set<OWLOntology> ontologies) {
		loader.unload( ontologies );

		refresh();
	}

	public void unloadOntology(OWLOntology ontology) {
		unloadOntologies( Collections.singleton( ontology ) );
	}
}
