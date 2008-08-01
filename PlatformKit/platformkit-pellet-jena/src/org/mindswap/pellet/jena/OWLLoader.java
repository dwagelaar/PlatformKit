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

package org.mindswap.pellet.jena;

import java.util.ArrayList;
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
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.Role;
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.datatypes.DatatypeReasoner;
import org.mindswap.pellet.datatypes.UnknownDatatype;
import org.mindswap.pellet.datatypes.XSDAtomicType;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.jena.vocabulary.OWL_1_1;
import org.mindswap.pellet.jena.vocabulary.SWRL;
import org.mindswap.pellet.rete.Constant;
import org.mindswap.pellet.rete.Rule;
import org.mindswap.pellet.rete.Term;
import org.mindswap.pellet.rete.Variable;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;
import org.mindswap.pellet.utils.QNameProvider;
import org.mindswap.pellet.utils.Timer;
import org.mindswap.pellet.utils.URIUtils;
import org.mindswap.pellet.utils.progress.ProgressMonitor;
import org.mindswap.pellet.utils.progress.SilentProgressMonitor;

import aterm.ATermAppl;
import aterm.ATermList;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class OWLLoader {
	protected static Log			log										= LogFactory
																					.getLog( OWLLoader.class );

	final public static Map<String, String> BUILT_IN_NS								= new HashMap<String, String>();
	static {
		BUILT_IN_NS.put( Namespaces.OWL, "OWL" );
		BUILT_IN_NS.put( Namespaces.OWL_1_1, "OWL 1.1" );
		BUILT_IN_NS.put( Namespaces.RDF, "RDF" );
		BUILT_IN_NS.put( Namespaces.RDFS, "RDFS" );
		BUILT_IN_NS.put( Namespaces.XSD, "XSD" );
	}

	/*
	 * predicates related to restrictions (owl:onProperty, owl:allValuesFrom,
	 * etc.) are preprocessed before all the triples are processed. these
	 * predicates are stored in the following list so processTriples function
	 * can ignore the triples with these predicates
	 */
	final public static Property[]	SKIP_PROPS								= new Property[] {
			RDF.type, RDF.first, RDF.rest, RDF.subject, RDF.predicate, RDF.object,

			OWL.imports, OWL.onProperty, OWL.hasValue, OWL.allValuesFrom, OWL.someValuesFrom,
			OWL.minCardinality, OWL.maxCardinality, OWL.cardinality, OWL.versionInfo,
			OWL.backwardCompatibleWith, OWL.incompatibleWith, OWL.priorVersion,

			OWL_1_1.onClass, OWL_1_1.onDataRange, OWL_1_1.minInclusive, OWL_1_1.minExclusive,
			OWL_1_1.maxInclusive, OWL_1_1.maxExclusive, OWL_1_1.minLength, OWL_1_1.maxLength,
			OWL_1_1.length, OWL_1_1.totalDigits, OWL_1_1.fractionDigits, OWL_1_1.dataComplementOf,

			SWRL.argument1, SWRL.argument2, SWRL.body, SWRL.head, SWRL.builtin,
			SWRL.classPredicate, SWRL.propertyPredicate,					};

	final public static Resource[]	SKIP_TYPES								= new Resource[] {
			RDF.List, RDF.Statement,

			OWL.Restriction, OWL.AllDifferent, OWL.Ontology,

			OWL_1_1.SelfRestriction,

			SWRL.ClassAtom, SWRL.IndividualPropertyAtom, SWRL.DatavaluedPropertyAtom,
			SWRL.Variable, SWRL.Builtin, SWRL.SameIndividualsAtom, SWRL.DifferentIndividualsAtom,
			SWRL.DataRangeAtom, SWRL.AtomList								};

	final static String[]			TYPES									= {
			"Class", "Individual", "Object Property", "Datatype Property", "Datatype" };

	final static Integer			SKIP									= new Integer( 0 );

	/*
	 * BUILTIN TYPES Consecutive values are used to ensure that the switch
	 * statement in processTypes function will be compiled into the efficient
	 * tableswitch bytecode rather than lookupswitch
	 */

	final static int				RDF_Property							= 1;

	final static int				RDFS_Class								= 2;
	final static int				RDFS_Datatype							= 3;

	final static int				OWL_Thing								= 4;
	final static int				OWL_Nothing								= 5;
	final static int				OWL_Class								= 6;
	final static int				OWL_ObjectProperty						= 7;
	final static int				OWL_DatatypeProperty					= 8;
	final static int				OWL_FunctionalProperty					= 9;
	final static int				OWL_InverseFunctionalProperty			= 10;
	final static int				OWL_TransitiveProperty					= 11;
	final static int				OWL_SymmetricProperty					= 12;
	final static int				OWL_AnnotationProperty					= 13;
	final static int				OWL_DataRange							= 14;

	final static int				OWL11_ReflexiveProperty					= 20;
	final static int				OWL11_IrreflexiveProperty				= 21;
	final static int				OWL11_AntisymmetricProperty				= 22;
	final static int				OWL11_NegativeObjectPropertyAssertion	= 23;
	final static int				OWL11_NegativeDataPropertyAssertion		= 24;

	final static int				ECONN_LinkProperty						= 50;
	final static int				ECONN_ForeignIndividual					= 51;
	final static int				ECONN_ForeignClass						= 52;
	final static int				ECONN_ForeignObjectProperty				= 53;
	final static int				ECONN_ForeignDatatypeProperty			= 54;
	final static int				ECONN_ForeignLinkProperty				= 55;

	final static int				SWRL_Imp								= 60;

	/*
	 * BUILTIN PROPERTIES Consecutive values are used to ensure that the switch
	 * statement in processTriples function will be compiled into the efficient
	 * tableswitch bytecode rather than lookupswitch
	 */

	final static int				RDFS_subClassOf							= 25;
	final static int				RDFS_subPropertyOf						= 26;
	final static int				RDFS_domain								= 27;
	final static int				RDFS_range								= 28;

	final static int				OWL_unionOf								= 29;
	final static int				OWL_sameAs								= 30;
	final static int				OWL_oneOf								= 31;
	final static int				OWL_inverseOf							= 32;
	final static int				OWL_intersectionOf						= 33;
	final static int				OWL_equivalentProperty					= 34;
	final static int				OWL_equivalentClass						= 35;
	final static int				OWL_distinctMembers						= 36;
	final static int				OWL_disjointWith						= 37;
	final static int				OWL_differentFrom						= 38;
	final static int				OWL_complementOf						= 39;

	final static int				OWL11_disjointUnionOf					= 40;
	final static int				OWL11_disjointObjectProperties			= 41;
	final static int				OWL11_disjointDataProperties			= 42;

	/*
	 * Only simple properties can be used in cardinality restrictions,
	 * disjointness axioms, irreflexivity and antisymmetry axioms. The following
	 * constants will be used to identify why a certain property should be
	 * treated as simple
	 */
	final static Object				SELF									= "self restriction";
	final static Object				CARDINALITY								= "cardinality restriction";
	final static Object				IRREFLEXIVE								= "irreflexivity axiom";
	final static Object				ANTI_SYM								= "antisymmetry axiom";
	final static Object				DISJOINT								= "disjointness axioms";

	static Map<Node, Integer>		KEYWORDS								= new HashMap<Node, Integer>();
	static {
		for( int i = 0; i < SKIP_TYPES.length; i++ ) {
			KEYWORDS.put( SKIP_TYPES[i].asNode(), SKIP );
		}

		for( int i = 0; i < SKIP_PROPS.length; i++ ) {
			KEYWORDS.put( SKIP_PROPS[i].asNode(), SKIP );
		}

		if( PelletOptions.IGNORE_DEPRECATED_TERMS ) {
			KEYWORDS.put( OWL.DeprecatedClass.asNode(), SKIP );
			KEYWORDS.put( OWL.DeprecatedProperty.asNode(), SKIP );
		}
		else {
			KEYWORDS.put( OWL.DeprecatedClass.asNode(), new Integer( OWL_Class ) );
			KEYWORDS.put( OWL.DeprecatedProperty.asNode(), new Integer( RDF_Property ) );
		}

		KEYWORDS.put( RDF.Property.asNode(), new Integer( RDF_Property ) );

		KEYWORDS.put( RDFS.Class.asNode(), new Integer( RDFS_Class ) );
		KEYWORDS.put( RDFS.Datatype.asNode(), new Integer( RDFS_Datatype ) );

		KEYWORDS.put( OWL.Thing.asNode(), new Integer( OWL_Thing ) );
		KEYWORDS.put( OWL.Nothing.asNode(), new Integer( OWL_Nothing ) );
		KEYWORDS.put( OWL.Class.asNode(), new Integer( OWL_Class ) );
		KEYWORDS.put( OWL.ObjectProperty.asNode(), new Integer( OWL_ObjectProperty ) );
		KEYWORDS.put( OWL.DatatypeProperty.asNode(), new Integer( OWL_DatatypeProperty ) );
		KEYWORDS.put( OWL.FunctionalProperty.asNode(), new Integer( OWL_FunctionalProperty ) );
		KEYWORDS.put( OWL.InverseFunctionalProperty.asNode(), new Integer(
				OWL_InverseFunctionalProperty ) );
		KEYWORDS.put( OWL.TransitiveProperty.asNode(), new Integer( OWL_TransitiveProperty ) );
		KEYWORDS.put( OWL.SymmetricProperty.asNode(), new Integer( OWL_SymmetricProperty ) );
		KEYWORDS.put( OWL.AnnotationProperty.asNode(), new Integer( OWL_AnnotationProperty ) );
		KEYWORDS.put( OWL.DataRange.asNode(), new Integer( OWL_DataRange ) );

		KEYWORDS.put( OWL_1_1.ReflexiveProperty.asNode(), new Integer( OWL11_ReflexiveProperty ) );
		KEYWORDS
				.put( OWL_1_1.IrreflexiveProperty.asNode(), new Integer( OWL11_IrreflexiveProperty ) );
		KEYWORDS.put( OWL_1_1.AntisymmetricProperty.asNode(), new Integer(
				OWL11_AntisymmetricProperty ) );

		KEYWORDS.put( RDFS.subClassOf.asNode(), new Integer( RDFS_subClassOf ) );
		KEYWORDS.put( RDFS.subPropertyOf.asNode(), new Integer( RDFS_subPropertyOf ) );
		KEYWORDS.put( RDFS.domain.asNode(), new Integer( RDFS_domain ) );
		KEYWORDS.put( RDFS.range.asNode(), new Integer( RDFS_range ) );

		KEYWORDS.put( OWL.unionOf.asNode(), new Integer( OWL_unionOf ) );
		KEYWORDS.put( OWL.sameAs.asNode(), new Integer( OWL_sameAs ) );
		KEYWORDS.put( OWL.oneOf.asNode(), new Integer( OWL_oneOf ) );
		KEYWORDS.put( OWL.inverseOf.asNode(), new Integer( OWL_inverseOf ) );
		KEYWORDS.put( OWL.intersectionOf.asNode(), new Integer( OWL_intersectionOf ) );
		KEYWORDS.put( OWL.equivalentProperty.asNode(), new Integer( OWL_equivalentProperty ) );
		KEYWORDS.put( OWL.equivalentClass.asNode(), new Integer( OWL_equivalentClass ) );
		KEYWORDS.put( OWL.distinctMembers.asNode(), new Integer( OWL_distinctMembers ) );
		KEYWORDS.put( OWL.disjointWith.asNode(), new Integer( OWL_disjointWith ) );
		KEYWORDS.put( OWL.differentFrom.asNode(), new Integer( OWL_differentFrom ) );
		KEYWORDS.put( OWL.complementOf.asNode(), new Integer( OWL_complementOf ) );

		KEYWORDS.put( OWL_1_1.disjointUnionOf.asNode(), new Integer( OWL11_disjointUnionOf ) );
		KEYWORDS.put( OWL_1_1.disjointObjectProperties.asNode(), new Integer(
				OWL11_disjointObjectProperties ) );
		KEYWORDS.put( OWL_1_1.disjointDataProperties.asNode(), new Integer(
				OWL11_disjointDataProperties ) );
		KEYWORDS.put( OWL_1_1.NegativeObjectPropertyAssertion.asNode(), new Integer(
				OWL11_NegativeObjectPropertyAssertion ) );
		KEYWORDS.put( OWL_1_1.NegativeDataPropertyAssertion.asNode(), new Integer(
				OWL11_NegativeDataPropertyAssertion ) );

		KEYWORDS.put( SWRL.Imp.asNode(), new Integer( SWRL_Imp ) );
		KEYWORDS.put( SWRL.Imp.asNode(), new Integer( SWRL_Imp ) );

		// KEYWORDS.put( .asNode(), new Integer( ) );

	}

	final static int				CLASS									= 0x00;
	final static int				INDIVIDUAL								= 0x01;
	final static int				OBJ_PROP								= 0x02;
	final static int				DT_PROP									= 0x04;
	final static int				ANT_PROP								= 0x08;
	final static int				ONT_PROP								= 0x0F;
	final static int				DATATYPE								= 0x10;
	final static int				LINK_PROP								= 0x12;

	public static QNameProvider		qnames									= new QNameProvider();

	private KnowledgeBase			kb;

	private Graph					graph;

	private Map						terms;

	private Map						lists;

	private Map						restrictions;

	private Map						simpleProperties;

	// Added for Foreign Classes
	private Map						resourceLinkTypes;

	private Set<String>				unsupportedFeatures;

	private ProgressMonitor			monitor									= new SilentProgressMonitor();

	public OWLLoader() {
		clear();
	}

	public void setProgressMonitor(ProgressMonitor monitor) {
		if( monitor == null )
			this.monitor = new SilentProgressMonitor();
		else
			this.monitor = monitor;
	}

	public void setGraph(Graph graph) {
		this.graph = graph;
	}

	public Graph getGraph() {
		return graph;
	}

	/**
	 * @deprecated Use {@link #getUnpportedFeatures()}
	 */
	public Set<String> getWarnings() {
		return unsupportedFeatures;
	}

	/**
	 * Returns the unsupported axioms ignored by the loader.
	 * 
	 * @return
	 */
	public Set<String> getUnpportedFeatures() {
		return unsupportedFeatures;
	}
	
	private void addSimpleProperty( ATermAppl p, Object why ) {
		simpleProperties.put( p, why );
		Role role = kb.getRBox().getRole( p );		
		role.setForceSimple( true );
	}

	private void addUnsupportedFeature(String msg) {
		if( !PelletOptions.IGNORE_UNSUPPORTED_AXIOMS )
			throw new UnsupportedFeatureException( msg );

		if( unsupportedFeatures.add( msg ) )
			log.warn( "Unsupported axiom: " + msg );
	}

	public void clear() {
		terms = new HashMap();
		terms.put( OWL.Thing.asNode(), ATermUtils.TOP );
		terms.put( OWL.Nothing.asNode(), ATermUtils.BOTTOM );

		lists = new HashMap();
		lists.put( RDF.nil.asNode(), ATermUtils.EMPTY_LIST );

		restrictions = new HashMap();

		simpleProperties = new HashMap();

		resourceLinkTypes = new HashMap();

		unsupportedFeatures = new HashSet();
	}

	private Node getObject(Node subj, Node pred) {
		Iterator all = graph.find( subj, pred, null );
		if( all.hasNext() ) {
			Triple triple = (Triple) all.next();

			return triple.getObject();
		}
		else
			return null;
	}

	private boolean hasObject(Node subj, Node pred) {
		return graph.find( subj, pred, null ).hasNext();
	}

	private boolean hasObject(Node subj, Node pred, Node obj) {
		return graph.contains( subj, pred, obj );
	}

	public ATermList createList(Node node) {
		if( lists.containsKey( node ) )
			return (ATermList) lists.get( node );

		Node first = getObject( node, RDF.first.asNode() );
		monitor.incrementProgress();

		Node rest = getObject( node, RDF.rest.asNode() );
		monitor.incrementProgress();

		if( first == null || rest == null ) {
			addUnsupportedFeature( "Invalid list structure: List " + node + " does not have a "
					+ (first == null
						? "rdf:first"
						: "rdf:rest") + " property. Ignoring rest of the list." );
			return ATermUtils.EMPTY_LIST;
		}

		ATermAppl firstNode = node2term( first );
		if( firstNode == null ) {
			addUnsupportedFeature( "Invalid list structure: List " + node
					+ " does not have a valid " + "rdf:first property. Ignoring rest of the list." );
			return ATermUtils.EMPTY_LIST;
		}

		ATermList restList = createList( rest );
		ATermList list = ATermUtils.makeList( firstNode, restList );

		lists.put( node, list );

		return list;
	}

	public ATermAppl createRestriction(Node node) throws UnsupportedFeatureException {
		ATermAppl aTerm = ATermUtils.TOP;

		Node p = getObject( node, OWL.onProperty.asNode() );
		monitor.incrementProgress();

		// TODO warning message: no owl:onProperty
		if( p == null )
			return aTerm;

		ATermAppl pt = node2term( p );
		// defineProperty(pt);

		// TODO warning message: multiple owl:onProperty
		Node o = null;
		if( graph.contains( node, RDF.type.asNode(), OWL_1_1.SelfRestriction.asNode() ) ) {
			monitor.incrementProgress();

			aTerm = ATermUtils.makeSelf( pt );

			defineObjectProperty( pt );
			addSimpleProperty( pt, SELF );
		}
		else if( (o = getObject( node, OWL.hasValue.asNode() )) != null ) {
			monitor.incrementProgress();

			if( PelletOptions.USE_PSEUDO_NOMINALS ) {
				if( o.isLiteral() ) {
					aTerm = ATermUtils.makeMin( pt, 1, ATermUtils.TOP_LIT );
				}
				else {
					ATermAppl ind = ATermUtils.makeTermAppl( o.getURI() );
					ATermAppl nom = ATermUtils.makeTermAppl( o.getURI() + "_nom" );

					defineClass( nom );
					defineIndividual( ind );
					kb.addType( ind, nom );

					aTerm = ATermUtils.makeSomeValues( pt, nom );
				}
			}
			else {
				ATermAppl ot = node2term( o );

				if( o.isLiteral() )
					defineDatatypeProperty( pt );
				else
					defineObjectProperty( pt );

				aTerm = ATermUtils.makeHasValue( pt, ot );
			}
		}
		else if( (o = getObject( node, OWL.allValuesFrom.asNode() )) != null ) {
			monitor.incrementProgress();

			ATermAppl ot = node2term( o );

			if( kb.isClass( ot ) )
				defineObjectProperty( pt );
			else if( kb.isDatatype( ot ) )
				defineDatatypeProperty( pt );

			aTerm = ATermUtils.makeAllValues( pt, ot );
		}
		else if( (o = getObject( node, OWL.someValuesFrom.asNode() )) != null ) {
			monitor.incrementProgress();

			ATermAppl ot = node2term( o );

			if( kb.isClass( ot ) )
				defineObjectProperty( pt );
			else if( kb.isDatatype( ot ) )
				defineDatatypeProperty( pt );

			aTerm = ATermUtils.makeSomeValues( pt, ot );
		}
		else if( (o = getObject( node, OWL.minCardinality.asNode() )) != null ) {
			monitor.incrementProgress();

			try {
				ATermAppl c = null;
				Node qualification = null;
				if( (qualification = getObject( node, OWL_1_1.onClass.asNode() )) != null ) {
					monitor.incrementProgress();
					c = node2term( qualification );
					defineObjectProperty( pt );
				}
				else if( (qualification = getObject( node, OWL_1_1.onDataRange.asNode() )) != null ) {
					monitor.incrementProgress();
					c = node2term( qualification );
					defineDatatypeProperty( pt );
				}
				else {
					int propType = guessPropertyType( pt, p );
					if( propType == Role.OBJECT )
						c = ATermUtils.TOP;
					else if( propType == Role.DATATYPE )
						c = ATermUtils.TOP_LIT;
					else {
						defineObjectProperty( pt );
						c = ATermUtils.TOP;
					}
				}

				int cardinality = Integer.parseInt( o.getLiteral().getLexicalForm() );
				aTerm = ATermUtils.makeMin( pt, cardinality, c );

				addSimpleProperty( pt, CARDINALITY );
			} catch( Exception ex ) {
				addUnsupportedFeature( "Invalid value for the min cardinality restriction: " + o );
			}
		}
		else if( (o = getObject( node, OWL.maxCardinality.asNode() )) != null ) {
			monitor.incrementProgress();

			try {
				ATermAppl c = null;
				Node qualification = null;
				if( (qualification = getObject( node, OWL_1_1.onClass.asNode() )) != null ) {
					monitor.incrementProgress();
					c = node2term( qualification );
					defineObjectProperty( pt );
				}
				else if( (qualification = getObject( node, OWL_1_1.onDataRange.asNode() )) != null ) {
					monitor.incrementProgress();
					c = node2term( qualification );
					defineDatatypeProperty( pt );
				}
				else {
					int propType = guessPropertyType( pt, p );
					if( propType == Role.OBJECT )
						c = ATermUtils.TOP;
					else if( propType == Role.DATATYPE )
						c = ATermUtils.TOP_LIT;
					else {
						defineObjectProperty( pt );
						c = ATermUtils.TOP;
					}
				}

				int cardinality = Integer.parseInt( o.getLiteral().getLexicalForm() );
				aTerm = ATermUtils.makeMax( pt, cardinality, c );

				addSimpleProperty( pt, CARDINALITY );
			} catch( Exception ex ) {
				addUnsupportedFeature( "Invalid value for the max cardinality restriction: " + o );
			}
		}
		else if( (o = getObject( node, OWL.cardinality.asNode() )) != null ) {
			monitor.incrementProgress();

			try {
				ATermAppl c = null;
				Node qualification = null;
				if( (qualification = getObject( node, OWL_1_1.onClass.asNode() )) != null ) {
					monitor.incrementProgress();
					c = node2term( qualification );
					defineObjectProperty( pt );
				}
				else if( (qualification = getObject( node, OWL_1_1.onDataRange.asNode() )) != null ) {
					monitor.incrementProgress();
					c = node2term( qualification );
					defineDatatypeProperty( pt );
				}
				else {
					int propType = guessPropertyType( pt, p );
					if( propType == Role.OBJECT )
						c = ATermUtils.TOP;
					else if( propType == Role.DATATYPE )
						c = ATermUtils.TOP_LIT;
					else {
						defineObjectProperty( pt );
						c = ATermUtils.TOP;
					}
				}

				int cardinality = Integer.parseInt( o.getLiteral().getLexicalForm() );
				aTerm = ATermUtils.makeCard( pt, cardinality, c );

				addSimpleProperty( pt, CARDINALITY );
			} catch( Exception ex ) {
				addUnsupportedFeature( "Invalid value for the cardinality restriction: " + o );
			}
		}

		else {
			// TODO print warning message (invalid restriction type)
			addUnsupportedFeature( "Ignoring invalid cardinality restriction on " + p );
		}

		return aTerm;
	}

	public ATermAppl node2term(Node node) {
		ATermAppl aTerm = (ATermAppl) terms.get( node );

		if( aTerm == null ) {
			if( node.isLiteral() ) {
				LiteralLabel label = node.getLiteral();

				String value = label.getLexicalForm();
				String datatypeURI = label.getDatatypeURI();
				String lang = label.language();

				if( datatypeURI != null )
					aTerm = ATermUtils.makeTypedLiteral( value, datatypeURI );
				else
					aTerm = ATermUtils.makePlainLiteral( value, lang );
			}
			else if( hasObject( node, OWL.onProperty.asNode() ) ) {
				aTerm = createRestriction( node );
				restrictions.put( node, aTerm );
			}
			else if( node.isBlank() ) {
				Node o = null;
				if( (o = getObject( node, OWL.intersectionOf.asNode() )) != null ) {
					monitor.incrementProgress();
					ATermList list = createList( o );
					aTerm = ATermUtils.makeAnd( list );
				}
				else if( (o = getObject( node, OWL.unionOf.asNode() )) != null ) {
					monitor.incrementProgress();
					ATermList list = createList( o );
					aTerm = ATermUtils.makeOr( list );
				}
				else if( (o = getObject( node, OWL_1_1.disjointUnionOf.asNode() )) != null ) {
					monitor.incrementProgress();
					ATermList list = createList( o );
					aTerm = ATermUtils.makeOr( list );
				}
				else if( (o = getObject( node, OWL.oneOf.asNode() )) != null ) {
					monitor.incrementProgress();
					ATermList list = createList( o );
					ATermList result = ATermUtils.EMPTY_LIST;
					if( list.isEmpty() )
						aTerm = ATermUtils.BOTTOM;
					else {
						boolean isDataRange = ATermUtils.isLiteral( (ATermAppl) list.getFirst() );
						for( ATermList l = list; !l.isEmpty(); l = l.getNext() ) {
							ATermAppl c = (ATermAppl) l.getFirst();
							if( PelletOptions.USE_PSEUDO_NOMINALS && !isDataRange ) {
								ATermAppl nominal = ATermUtils.makeTermAppl( c.getName()
										+ "_nominal" );
								result = result.insert( nominal );

								defineIndividual( c );
								defineClass( nominal );
								kb.addType( c, nominal );
							}
							else {
								ATermAppl nominal = ATermUtils.makeValue( c );
								result = result.insert( nominal );
							}
						}

						aTerm = ATermUtils.makeOr( result );
					}
				}
				else if( (o = getObject( node, OWL.complementOf.asNode() )) != null ) {
					monitor.incrementProgress();
					ATermAppl complement = node2term( o );
					aTerm = ATermUtils.makeNot( complement );
				}
				else {
					String bNode = PelletOptions.BNODE + node.getBlankNodeId();
					aTerm = ATermUtils.makeTermAppl( bNode );
				}
			}
			else {
				String uri = node.getURI();

				if( PelletOptions.USE_LOCAL_NAME ) {
					if( uri.startsWith( Namespaces.XSD ) )
						aTerm = ATermUtils.makeTermAppl( uri );
					else
						aTerm = ATermUtils.makeTermAppl( URIUtils.getLocalName( uri ) );
				}
				else if( PelletOptions.USE_QNAME ) {
					if( uri.startsWith( Namespaces.XSD ) )
						aTerm = ATermUtils.makeTermAppl( uri );
					else
						aTerm = ATermUtils.makeTermAppl( qnames.shortForm( uri ) );
				}
				else
					aTerm = ATermUtils.makeTermAppl( uri );
			}

			terms.put( node, aTerm );
		}

		return aTerm;
	}

	private Datatype defineDataRange(Node s) {
		String name = node2term( s ).getName();

		DatatypeReasoner dtReasoner = kb.getDatatypeReasoner();
		Datatype datatype = UnknownDatatype.instance;
		Node definition = null;

		if( dtReasoner.isDefined( name ) )
			datatype = dtReasoner.getDatatype( name );
		else if( (definition = getObject( s, OWL.oneOf.asNode() )) != null ) {
			ATermList list = createList( definition );
			datatype = kb.getDatatypeReasoner().enumeration( ATermUtils.listToSet( list ) );
		}
		else if( (definition = getObject( s, OWL_1_1.complementOf.asNode() )) != null ) {
			datatype = dtReasoner.negate( defineDataRange( definition ) );
		}
		else if( (definition = getObject( s, OWL_1_1.dataComplementOf.asNode() )) != null ) {
			datatype = dtReasoner.negate( defineDataRange( definition ) );
		}
		else if( (definition = getObject( s, OWL_1_1.onDataRange.asNode() )) != null ) {
			Datatype d = defineDataRange( definition );

			if( d instanceof XSDAtomicType ) {
				XSDAtomicType xsdType = (XSDAtomicType) d;

				Node facetValue;
				boolean noFacet = true;
				if( (facetValue = getObject( s, OWL_1_1.minInclusive.asNode() )) != null ) {
					Object value = xsdType.getValue( facetValue.getLiteralLexicalForm(), facetValue
							.getLiteralDatatypeURI() );
					if( value != null ) {
						xsdType = xsdType.restrictMinInclusive( value );
						noFacet = false;
					}
					else
						addUnsupportedFeature( "Ignoring invalid facet value " + facetValue
								+ " for " + definition );
				}
				if( (facetValue = getObject( s, OWL_1_1.maxInclusive.asNode() )) != null ) {
					Object value = xsdType.getValue( facetValue.getLiteralLexicalForm(), facetValue
							.getLiteralDatatypeURI() );
					if( value != null ) {
						xsdType = xsdType.restrictMaxInclusive( value );
						noFacet = false;
					}
					else
						addUnsupportedFeature( "Ignoring invalid facet value " + facetValue
								+ " for " + definition );
				}
				if( (facetValue = getObject( s, OWL_1_1.minExclusive.asNode() )) != null ) {
					Object value = xsdType.getValue( facetValue.getLiteralLexicalForm(), facetValue
							.getLiteralDatatypeURI() );
					if( value != null ) {
						xsdType = xsdType.restrictMinExclusive( value );
						noFacet = false;
					}
					else
						addUnsupportedFeature( "Ignoring invalid facet value " + facetValue
								+ " for data range " + definition );
				}
				if( (facetValue = getObject( s, OWL_1_1.maxExclusive.asNode() )) != null ) {
					Object value = xsdType.getValue( facetValue.getLiteralLexicalForm(), facetValue
							.getLiteralDatatypeURI() );
					if( value != null ) {
						xsdType = xsdType.restrictMaxExclusive( value );
						noFacet = false;
					}
					else
						addUnsupportedFeature( "Ignoring invalid facet value " + facetValue
								+ " for " + definition );
				}
				if( (facetValue = getObject( s, OWL_1_1.totalDigits.asNode() )) != null ) {
					try {
						int value = Integer.parseInt( facetValue.getLiteralLexicalForm() );
						xsdType = xsdType.restrictTotalDigits( value );
						noFacet = false;
					} catch( NumberFormatException e ) {
						addUnsupportedFeature( "Ignoring invalid value " + facetValue
								+ " for facet totalDigits in the definition of " + definition );
					}
				}
				if( (facetValue = getObject( s, OWL_1_1.fractionDigits.asNode() )) != null ) {
					try {
						int value = Integer.parseInt( facetValue.getLiteralLexicalForm() );
						xsdType = xsdType.restrictFractionDigits( value );
						noFacet = false;
					} catch( NumberFormatException e ) {
						addUnsupportedFeature( "Ignoring invalid value " + facetValue
								+ " for facet totalDigits in the definition of " + definition );
					}
				}
				if( (facetValue = getObject( s, OWL_1_1.pattern.asNode() )) != null ) {
					String value = facetValue.getLiteralLexicalForm();
					xsdType = xsdType.restrictPattern( value );
					noFacet = false;
				}

				if( noFacet )
					addUnsupportedFeature( "A data range is defined without XSD facet restrictions "
							+ s );

				datatype = xsdType;
			}
			else {
				addUnsupportedFeature( "A restriction is defined on an unknown XSD type "
						+ definition );
			}
		}
		else {
			addUnsupportedFeature( name + " is not a valid data range description" );
		}

		kb.addDatatype( name, datatype );

		return datatype;
	}

	private void defineRule(Node node) {
		List head = parseAtomList( getObject( node, SWRL.head.asNode() ) );
		List body = parseAtomList( getObject( node, SWRL.body.asNode() ) );

		if( head == null || body == null ) {
			addUnsupportedFeature( "Ignoring SWRL rule: " + node );

			return;
		}

		for( Iterator i = head.iterator(); i.hasNext(); ) {
			List singletonHead = Collections.singletonList( i.next() );

			Rule rule = new Rule( body, singletonHead );
			kb.addRule( rule );
		}
	}

	private Term createRuleTerm(Node node) {
		if( !node.isURI() ) {
			return null;
		}
		if( hasObject( node, RDF.type.asNode(), SWRL.Variable.asNode() ) ) {
			return new Variable( node.getURI() );
		}
		else {
			return new Constant( node.getURI() );
		}
	}

	private List parseAtomList(Node atomList) {
		List atoms = new ArrayList();

		while( !atomList.equals( RDF.nil.asNode() ) ) {
			String atomType = "unsupported atom";
			Node atom = getObject( atomList, RDF.first.asNode() );

			Term subj = null, pred = null, obj = null;
			if( hasObject( atom, RDF.type.asNode(), SWRL.ClassAtom.asNode() ) ) {
				atomType = "ClassAtom";
				subj = createRuleTerm( getObject( atom, SWRL.argument1.asNode() ) );
				pred = Constant.TYPE;
				obj = createRuleTerm( getObject( atom, SWRL.classPredicate.asNode() ) );
			}
			else if( hasObject( atom, RDF.type.asNode(), SWRL.IndividualPropertyAtom.asNode() ) ) {
				atomType = "IndividualPropertyAtom";
				subj = createRuleTerm( getObject( atom, SWRL.argument1.asNode() ) );
				pred = createRuleTerm( getObject( atom, SWRL.propertyPredicate.asNode() ) );
				obj = createRuleTerm( getObject( atom, SWRL.argument2.asNode() ) );
			}
			else if( hasObject( atom, RDF.type.asNode(), SWRL.DifferentIndividualsAtom.asNode() ) ) {
				atomType = "DifferentIndividualsAtom";
				subj = createRuleTerm( getObject( atom, SWRL.argument1.asNode() ) );
				pred = Constant.DIFF_FROM;
				obj = createRuleTerm( getObject( atom, SWRL.argument2.asNode() ) );
			}
			else if( hasObject( atom, RDF.type.asNode(), SWRL.SameIndividualsAtom.asNode() ) ) {
				atomType = "SameIndividualsAtom";
				subj = createRuleTerm( getObject( atom, SWRL.argument1.asNode() ) );
				pred = Constant.SAME_AS;
				obj = createRuleTerm( getObject( atom, SWRL.argument2.asNode() ) );
			}
			else if( hasObject( atom, RDF.type.asNode(), SWRL.DatavaluedPropertyAtom.asNode() ) ) {
				atomType = "DatavaluedPropertyAtom";
				// not supported, do nothing
			}
			else if( hasObject( atom, RDF.type.asNode(), SWRL.BuiltinAtom.asNode() ) ) {
				atomType = "BuiltinAtom";
				// not supported, do nothing
			}

			if( subj == null || pred == null || obj == null ) {
				addUnsupportedFeature( "Ignoring SWRL " + atomType + ": " + atom );
				return null;
			}

			atoms.add( new org.mindswap.pellet.rete.Triple( subj, pred, obj ) );

			atomList = getObject( atomList, RDF.rest.asNode() );
		}

		return atoms;
	}

	private boolean addNegatedAssertion(Node stmt, boolean object) {
		Node s = getObject( stmt, RDF.subject.asNode() );
		if( s == null ) {
			addUnsupportedFeature( "Negated property value is missing rdf:subject value" );
			return false;
		}

		Node p = getObject( stmt, RDF.predicate.asNode() );
		if( p == null ) {
			addUnsupportedFeature( "Negated property value is missing rdf:predicate value" );
			return false;
		}

		Node o = getObject( stmt, RDF.object.asNode() );
		if( o == null ) {
			addUnsupportedFeature( "Negated property value is missing rdf:object value" );
			return false;
		}

		ATermAppl st = node2term( s );
		ATermAppl pt = node2term( p );
		ATermAppl ot = node2term( o );

		defineIndividual( st );
		if( object )
			defineObjectProperty( pt );
		else
			defineDatatypeProperty( pt );
		if( object )
			defineIndividual( ot );

		if( !kb.addNegatedPropertyValue( pt, st, ot ) ) {
			addUnsupportedFeature( "Skipping invalid negated property value " + stmt );
			return false;
		}

		return true;
	}

	private boolean defineClass(ATermAppl c) {
		if( ATermUtils.isPrimitive( c ) ) {
			kb.addClass( c );
			return true;
		}
		else
			return ATermUtils.isComplexClass( c );
	}

	private boolean defineDatatype(ATermAppl datatypeURI) {
		kb.addDatatype( datatypeURI );

		return true;
	}

	private boolean loadDatatype(ATermAppl datatypeURI) {
		if( PelletOptions.AUTO_XML_SCHEMA_LOADING )
			kb.loadDatatype( datatypeURI );
		else
			defineDatatype( datatypeURI );

		return true;
	}

	/**
	 * There are two properties that are used in a subPropertyOf or
	 * equivalentProperty axiom. If one of them is defined as an Object (or
	 * Data) Property the other should also be defined as an Object (or Data)
	 * Property
	 * 
	 * @param p1
	 * @param p2
	 * @return
	 */
	private boolean defineProperties(ATermAppl p1, ATermAppl p2) {
		int type1 = kb.getPropertyType( p1 );
		int type2 = kb.getPropertyType( p2 );
		if( type1 != type2 ) {
			if( type1 == Role.UNTYPED ) {
				if( type2 == Role.OBJECT )
					defineObjectProperty( p1 );
				else if( type2 == Role.DATATYPE )
					defineDatatypeProperty( p1 );
			}
			else if( type2 == Role.UNTYPED ) {
				if( type1 == Role.OBJECT )
					defineObjectProperty( p2 );
				else if( type1 == Role.DATATYPE )
					defineDatatypeProperty( p2 );
			}
			else {
				// addWarning("Properties " + p1 + ", " + p2
				// + " are related but first is " + Role.TYPES[type1]
				// + "Property and second is " + Role.TYPES[type2]);
				return false;
			}
		}
		else if( type1 == Role.UNTYPED ) {
			defineProperty( p1 );
			defineProperty( p2 );
		}

		return true;
	}

	private boolean defineObjectProperty(ATermAppl c) {
		if( !ATermUtils.isPrimitive( c ) )
			return false;

		return kb.addObjectProperty( c );
	}

	private boolean defineDatatypeProperty(ATermAppl c) {
		if( !ATermUtils.isPrimitive( c ) )
			return false;

		return kb.addDatatypeProperty( c );
	}

	private boolean defineAnnotationProperty(ATermAppl c) {
		if( !ATermUtils.isPrimitive( c ) )
			return false;

		return kb.addAnnotationProperty( c );
	}

	private boolean defineOntologyProperty(ATermAppl c) {
		if( !ATermUtils.isPrimitive( c ) )
			return false;

		kb.addOntologyProperty( c );
		return true;
	}

	private boolean defineProperty(ATermAppl c) {
		if( !ATermUtils.isPrimitive( c ) )
			return false;

		kb.addProperty( c );
		return true;
	}

	private boolean defineIndividual(ATermAppl c) {
		if( !ATermUtils.isPrimitive( c ) )
			return false;

		kb.addIndividual( c );
		return true;
	}

	private int guessPropertyType(ATermAppl p, Node prop) {
		int roleType = kb.getPropertyType( p );
		if( roleType != Role.UNTYPED )
			return roleType;
		
		defineProperty( p );
		
		Iterator i = graph.find( prop, RDF.type.asNode(), null );
		while( i.hasNext() ) {
			Triple stmt = (Triple) i.next();
			Node o = stmt.getObject();

			if( o.equals( OWL.ObjectProperty.asNode() ) )
				return Role.OBJECT;
			else if( o.equals( OWL.DatatypeProperty.asNode() ) )
				return Role.DATATYPE;
			else
				return Role.ANNOTATION;
		}

		return Role.UNTYPED;
	}

	private void processTypes(Graph model) throws UnsupportedFeatureException {
		Iterator i = model.find( null, RDF.type.asNode(), null );
		while( i.hasNext() ) {
			Triple stmt = (Triple) i.next();
			Node o = stmt.getObject();

			Integer keyword = KEYWORDS.get( o );

			if( keyword == SKIP )
				continue;

			monitor.incrementProgress();

			Node s = stmt.getSubject();
			ATermAppl st = node2term( s );

			if( keyword == null ) {
				if( PelletOptions.FREEZE_BUILTIN_NAMESPACES && o.isURI() ) {
					String nameSpace = o.getNameSpace();
					if( nameSpace != null ) {
						String builtin = (String) BUILT_IN_NS.get( nameSpace );
						if( builtin != null ) {
							addUnsupportedFeature( "Ignoring triple with unknown term from "
									+ builtin + " namespace: " + stmt );
							continue;
						}
					}
				}

				ATermAppl ot = node2term( o );

				defineIndividual( st );
				defineClass( ot );
				kb.addType( st, ot );

				continue;
			}

			switch ( keyword.intValue() ) {

			case RDF_Property:
				defineProperty( st );
				break;

			case RDFS_Class:
				defineClass( st );
				break;

			case RDFS_Datatype:
				loadDatatype( st );
				break;

			case OWL_Class:
				defineClass( st );
				break;

			case OWL_Thing:
				defineIndividual( st );
				break;

			case OWL_Nothing:
				defineIndividual( st );
				kb.addType( st, ATermUtils.BOTTOM );
				break;

			case OWL_ObjectProperty:
				if( !defineObjectProperty( st ) ) {
					addUnsupportedFeature( "Property " + st
							+ " is defined both as an ObjectProperty and a "
							+ Role.TYPES[kb.getPropertyType( st )] + "Property" );
				}
				break;

			case OWL_DatatypeProperty:
				if( !defineDatatypeProperty( st ) )
					addUnsupportedFeature( "Property " + st
							+ " is defined both as a DatatypeProperty and a "
							+ Role.TYPES[kb.getPropertyType( st )] + "Property" );
				break;

			case OWL_FunctionalProperty:
				defineProperty( st );
				kb.addFunctionalProperty( st );
				addSimpleProperty( st, CARDINALITY );
				break;

			case OWL_InverseFunctionalProperty:
				if( defineProperty( st ) ) {
					kb.addInverseFunctionalProperty( st );
					addSimpleProperty( st, CARDINALITY );
				}
				else
					addUnsupportedFeature( "Ignoring InverseFunctionalProperty axiom for " + st
							+ " (" + Role.TYPES[kb.getPropertyType( st )] + "Property)" );
				break;

			case OWL_TransitiveProperty:
				if( defineObjectProperty( st ) ) {
					kb.addTransitiveProperty( st );
				}
				else
					addUnsupportedFeature( "Ignoring TransitiveProperty axiom for " + st + " ("
							+ Role.TYPES[kb.getPropertyType( st )] + "Property)" );

				break;

			case OWL_SymmetricProperty:
				if( defineObjectProperty( st ) )
					kb.addSymmetricProperty( st );
				else
					addUnsupportedFeature( "Ignoring SymmetricProperty axiom for " + st + " ("
							+ Role.TYPES[kb.getPropertyType( st )] + "Property)" );
				break;

			case OWL_AnnotationProperty:
				if( !defineAnnotationProperty( st ) ) {
					addUnsupportedFeature( "Property " + st
							+ " is defined both as an AnnotationProperty and a "
							+ Role.TYPES[kb.getPropertyType( st )] + "Property" );
				}
				break;

			case OWL_DataRange:
				defineDataRange( s );
				break;

			case OWL11_ReflexiveProperty:
				if( defineObjectProperty( st ) )
					kb.addReflexiveProperty( st );
				else
					addUnsupportedFeature( "Ignoring ReflexiveProperty axiom for " + st + " ("
							+ Role.TYPES[kb.getPropertyType( st )] + "Property)" );
				break;

			case OWL11_IrreflexiveProperty:
				if( defineObjectProperty( st ) ) {
					kb.addIrreflexiveProperty( st );
					addSimpleProperty( st, IRREFLEXIVE );
				}
				else
					addUnsupportedFeature( "Ignoring IrreflexiveProperty axiom for " + st + " ("
							+ Role.TYPES[kb.getPropertyType( st )] + "Property)" );
				break;

			case OWL11_AntisymmetricProperty:
				if( defineObjectProperty( st ) ) {
					kb.addAntisymmetricProperty( st );
					addSimpleProperty( st, ANTI_SYM );
				}
				else
					addUnsupportedFeature( "Ignoring AntisymmetricProperty axiom for " + st + " ("
							+ Role.TYPES[kb.getPropertyType( st )] + "Property)" );
				break;

			case OWL11_NegativeObjectPropertyAssertion:
				addNegatedAssertion( s, true );
				break;

			case OWL11_NegativeDataPropertyAssertion:
				addNegatedAssertion( s, false );
				break;

			case SWRL_Imp:
				if( PelletOptions.DL_SAFE_RULES )
					defineRule( s );
				break;

			default:
				throw new InternalReasonerException( "Unexpected term: " + o );
			}
		}
	}

	private void processTriples(Graph graph) throws UnsupportedFeatureException {
		for( Iterator i = graph.find( Triple.ANY ); i.hasNext(); ) {
			Triple triple = (Triple) i.next();
			Node p = triple.getPredicate();

			Integer keyword = (Integer) KEYWORDS.get( p );

			if( keyword == SKIP ) {
				continue;
			}

			monitor.incrementProgress();

			Node s = triple.getSubject();
			Node o = triple.getObject();

			ATermAppl st = node2term( s );
			ATermAppl ot = node2term( o );

			if( keyword == null ) {
				ATermAppl pt = node2term( p );
				Role role = kb.getProperty( pt );
				int type = (role == null)
					? Role.UNTYPED
					: role.getType();

				if( type == Role.ANNOTATION ) {
					continue;
				}

				if( PelletOptions.FREEZE_BUILTIN_NAMESPACES ) {
					String nameSpace = p.getNameSpace();
					if( nameSpace != null ) {
						String builtin = (String) BUILT_IN_NS.get( nameSpace );
						if( builtin != null ) {
							addUnsupportedFeature( "Ignoring triple with unknown property from "
									+ builtin + " namespace: " + triple );
							continue;
						}
					}
				}

				if( o.isLiteral() ) {
					if( defineDatatypeProperty( pt ) ) {
						String datatypeURI = ((ATermAppl) ot.getArgument( 2 )).getName();

						if( defineIndividual( st ) ) {
							defineDatatypeProperty( pt );
							if( !datatypeURI.equals( "" ) )
								defineDatatype( ATermUtils.makeTermAppl( datatypeURI ) );

							kb.addPropertyValue( pt, st, ot );
						}
						else if( type == Role.UNTYPED )
							defineAnnotationProperty( pt );
						else
							addUnsupportedFeature( "Ignoring ObjectProperty used with a class expression: "
									+ triple );
					}
					else
						addUnsupportedFeature( "Ignoring literal value used with ObjectProperty : "
								+ triple );
				}
				else {
					if( !defineObjectProperty( pt ) )
						addUnsupportedFeature( "Ignoring object value used with DatatypeProperty: "
								+ triple );
					else if( !defineIndividual( st ) )
						addUnsupportedFeature( "Ignoring class expression used in subject position: "
								+ triple );
					else if( !defineIndividual( ot ) )
						addUnsupportedFeature( "Ignoring class expression used in object position: "
								+ triple );
					else
						kb.addPropertyValue( pt, st, ot );
				}
				continue;
			}

			switch ( keyword.intValue() ) {

			case RDFS_subClassOf:
				if( !defineClass( st ) )
					addUnsupportedFeature( "Ignoring subClassOf axiom because the subject is not a class "
							+ st + " rdfs:subClassOf " + ot );
				else if( !defineClass( ot ) )
					addUnsupportedFeature( "Ignoring subClassOf axiom because the object is not a class "
							+ st + " rdfs:subClassOf " + ot );
				else
					kb.addSubClass( st, ot );
				break;

			case RDFS_subPropertyOf:
				if( hasObject( s, RDF.first.asNode() ) ) {
					ATermList list = createList( s );
					kb.addSubProperty( list, ot );
				}
				else if( defineProperties( st, ot ) )
					kb.addSubProperty( st, ot );
				else
					addUnsupportedFeature( "Ignoring subproperty axiom between " + st + " ("
							+ Role.TYPES[kb.getPropertyType( st )] + "Property) and " + ot + " ("
							+ Role.TYPES[kb.getPropertyType( ot )] + "Property)" );
				break;

			case RDFS_domain:
				if( kb.isAnnotationProperty( st ) ) {
					addUnsupportedFeature( "Ignoring domain axiom for AnnotationProperty " + st );
				}
				else {
					defineProperty( st );
					defineClass( ot );
					kb.addDomain( st, ot );
				}
				break;

			case RDFS_range:
				if( kb.isAnnotationProperty( st ) ) {
					addUnsupportedFeature( "Ignoring range axiom for AnnotationProperty " + st );
					break;
				}

				if( kb.isDatatype( ot ) )
					defineDatatypeProperty( st );
				else if( kb.isClass( ot ) )
					defineObjectProperty( st );
				else
					defineProperty( st );

				if( kb.isDatatypeProperty( st ) )
					defineDatatype( ot );
				else if( kb.isObjectProperty( st ) )
					defineClass( ot );

				kb.addRange( st, ot );

				break;

			case OWL_intersectionOf:
				ATermList list = createList( o );

				for( ATermList l = list; !l.isEmpty(); l = l.getNext() ) {
					ATermAppl c = (ATermAppl) l.getFirst();
					// For Econnections. owl:ForeignClass
					if( !resourceLinkTypes.containsKey( c ) )
						defineClass( c );
				} // for

				defineClass( st );
				ATermAppl conjunction = ATermUtils.makeAnd( list );

				kb.addEquivalentClass( st, conjunction );
				break;

			case OWL_unionOf:
				list = createList( o );

				defineClass( st );
				ATermAppl disjunction = ATermUtils.makeOr( list );
				kb.addEquivalentClass( st, disjunction );

				break;

			case OWL11_disjointUnionOf:
				list = createList( o );

				kb.addDisjointClasses( list );

				defineClass( st );
				disjunction = ATermUtils.makeOr( list );
				kb.addEquivalentClass( st, disjunction );

				break;

			case OWL_complementOf:
				if( !kb.isDatatype( st ) ) {
					// For Econnections. owl:ForeignClass
					if( !resourceLinkTypes.containsKey( st ) )
						defineClass( st );
					if( !resourceLinkTypes.containsKey( st ) )
						defineClass( ot );

					kb.addComplementClass( st, ot );
				}
				break;

			case OWL_equivalentClass:
				if( !defineClass( st ) )
					addUnsupportedFeature( "Ignoring equivalentClass axiom because the subject is not a class "
							+ st + " owl:equivalentClass " + ot );
				else if( !defineClass( ot ) )
					addUnsupportedFeature( "Ignoring equivalentClass axiom because the object is not a class "
							+ st + " owl:equivalentClass " + ot );
				else
					kb.addEquivalentClass( st, ot );
				break;

			case OWL_disjointWith:
				// if( kb.isProperty( st ) || kb.isProperty( ot ) ) {
				// if( !defineObjectProperty( st ) || !defineObjectProperty( ot
				// ) )
				// addWarning("Ignoring disjointWith axiom between " +
				// st + " and " + ot + "");
				//                        
				// kb.addDisjointProperty(st, ot);
				// }
				// else
				if( !defineClass( st ) )
					addUnsupportedFeature( "Ignoring disjointWith axiom because the subject is not a class "
							+ st + " owl:disjointWith " + ot );
				else if( !defineClass( ot ) )
					addUnsupportedFeature( "Ignoring disjointWith axiom because the object is not a class "
							+ st + " owl:disjointWith " + ot );
				else {
					kb.addDisjointClass( st, ot );
				}
				break;

			case OWL11_disjointObjectProperties:
				if( defineObjectProperty( st ) && defineObjectProperty( ot ) ) {
					kb.addDisjointProperty( st, ot );
					addSimpleProperty( st, DISJOINT );
					addSimpleProperty( ot, DISJOINT );
				}
				else
					addUnsupportedFeature( "Ignoring subproperty axiom between " + st + " ("
							+ Role.TYPES[kb.getPropertyType( st )] + "Property) and " + ot + " ("
							+ Role.TYPES[kb.getPropertyType( ot )] + "Property)" );
				break;

			case OWL11_disjointDataProperties:
				if( defineDatatypeProperty( st ) && defineDatatypeProperty( ot ) ) {
					kb.addDisjointProperty( st, ot );
					// no need to add to simplePreperties because datatype
					// properties
					// by default are simple properties
				}
				else
					addUnsupportedFeature( "Ignoring subproperty axiom between " + st + " ("
							+ Role.TYPES[kb.getPropertyType( st )] + "Property) and " + ot + " ("
							+ Role.TYPES[kb.getPropertyType( ot )] + "Property)" );
				break;

			case OWL_equivalentProperty:
				if( defineProperties( st, ot ) )
					kb.addEquivalentProperty( st, ot );
				else
					addUnsupportedFeature( "Ignoring equivalent property axiom between " + st
							+ " (" + Role.TYPES[kb.getPropertyType( st )] + "Property) and " + ot
							+ " (" + Role.TYPES[kb.getPropertyType( ot )] + "Property)" );

				break;

			case OWL_inverseOf:
				if( defineObjectProperty( st ) && defineObjectProperty( ot ) )
					kb.addInverseProperty( st, ot );
				else
					addUnsupportedFeature( "Ignoring inverseOf axiom between " + st + " ("
							+ Role.TYPES[kb.getPropertyType( st )] + "Property) and " + ot + " ("
							+ Role.TYPES[kb.getPropertyType( ot )] + "Property)" );

				break;

			case OWL_sameAs:
				if( defineIndividual( st ) && defineIndividual( ot ) )
					kb.addSame( st, ot );
				else
					addUnsupportedFeature( "Ignoring sameAs axiom between " + st + " and " + ot );
				break;

			case OWL_differentFrom:
				if( defineIndividual( st ) && defineIndividual( ot ) )
					kb.addDifferent( st, ot );
				else
					addUnsupportedFeature( "Ignoring differentFrom axiom between " + st + " and "
							+ ot );
				break;

			case OWL_distinctMembers:
				List result = new ArrayList();
				list = createList( o );

				for( ATermList l = list; !l.isEmpty(); l = l.getNext() ) {
					ATermAppl c = (ATermAppl) l.getFirst();
					defineIndividual( c );
					result.add( c );
				} // for
				for( int k = 0; k < result.size(); k++ ) {
					for( int j = k + 1; j < result.size(); j++ ) {
						kb.addDifferent( (ATermAppl) result.get( k ), (ATermAppl) result.get( j ) );
					} // for
				} // for
				break;

			case OWL_oneOf:
				ATermList resultList = ATermUtils.EMPTY_LIST;

				if( kb.isDatatype( st ) )
					continue;

				// assert the subject is a class
				defineClass( st );

				disjunction = null;
				list = createList( o );
				if( o.equals( RDF.nil ) )
					disjunction = ATermUtils.BOTTOM;
				else {
					for( ATermList l = list; !l.isEmpty(); l = l.getNext() ) {
						ATermAppl c = (ATermAppl) l.getFirst();

						if( PelletOptions.USE_PSEUDO_NOMINALS ) {
							ATermAppl nominal = ATermUtils.makeTermAppl( c.getName() + "_nominal" );
							resultList = resultList.insert( nominal );

							defineClass( nominal );
							defineIndividual( c );
							kb.addType( c, nominal );
						}
						else {
							defineIndividual( c );

							resultList = resultList.insert( ATermUtils.makeValue( c ) );
						}
					}
					disjunction = ATermUtils.makeOr( resultList );
				}
				kb.addEquivalentClass( st, disjunction );
				break;

			// TODO ontology properties
			// else if (kb.getProperty(pt).getType() == Role.ONTOLOGY) {
			// Resource r = (Resource) o;
			// Hashtable props = getOntologyDefinition(s);
			// Vector propList = (Vector) props.get(p);
			// if (propList == null)
			// propList = new Vector();
			// propList.add(o);
			// props.put(p, propList);
			// }

			default:
				throw new InternalReasonerException( "Unrecognized term: " + p );

			}
		}
	}

	private void processUntypedResources() {
		Iterator i = restrictions.keySet().iterator();
		while( i.hasNext() ) {
			Node node = (Node) i.next();
			Node o = null;
			if( (o = getObject( node, OWL.onProperty.asNode() )) != null ) {
				ATermAppl prop = node2term( o );

				defineProperty( prop );

				if( kb.isDatatypeProperty( prop ) ) {
					if( (o = getObject( node, OWL.someValuesFrom.asNode() )) != null ) {
						defineDatatype( node2term( o ) );
					}
					else if( (o = getObject( node, OWL.allValuesFrom.asNode() )) != null ) {
						defineDatatype( node2term( o ) );
					}
				}
			}

			if( (o = getObject( node, OWL.hasValue.asNode() )) != null ) {
				if( !o.isLiteral() )
					defineIndividual( node2term( o ) );
			}
		}

		i = new ArrayList( kb.getRBox().getRoles() ).iterator();
		while( i.hasNext() ) {
			Role r = (Role) i.next();

			Object why = simpleProperties.get( r.getName() );
			if( why != null ) {
				String msg = null;
				if( r.isTransitive() ) {
					msg = "transitivity axiom";
				}
				else if( r.hasComplexSubRole() ) {
					msg = "complex sub property axiom";
				}

				if( msg != null ) {
					msg = "Ignoring " + msg + " due to an existing " + why + " for property " + r;
					addUnsupportedFeature( msg );
					r.removeSubRoleChains();
				}
			}

			if( r.isUntypedRole() ) {
				/*
				 * Untyped roles are made object properties unless they have
				 * datatype super or sub-roles
				 */
				boolean rangeToDatatype = false;
				MultiIterator j = new MultiIterator( r.getSubRoles().iterator(), r.getSuperRoles()
						.iterator() );
				while( j.hasNext() ) {
					Role sub = (Role) j.next();
					switch ( sub.getType() ) {
					case Role.OBJECT:
						defineObjectProperty( r.getName() );
						break;
					case Role.DATATYPE:
						defineDatatypeProperty( r.getName() );
						rangeToDatatype = true;
						break;
					default:
						continue;
					}
				}

				/*
				 * If a typing assumption has been made, carry over to any
				 * untyped range entity
				 */
				Set<ATermAppl> ranges = r.getRanges();
				if( ranges != null ) {
					if( rangeToDatatype ) {
						for( ATermAppl range : ranges ) {
							if( (range.getAFun().getArity() == 0) && (!kb.isDatatype( range )) )
								defineDatatype( range );
						}
					}
					else {
						for( ATermAppl range : ranges ) {
							if( (range.getAFun().getArity() == 0) && (!kb.isClass( range )) )
								defineClass( range );
						}
					}
				}
			}
		}
	}

	// private void processMultitypedResources() {
	// Iterator i = resourceTypes.entrySet().iterator();
	// while(i.hasNext()) {
	// Map.Entry entry = (Map.Entry) i.next();
	// ATermAppl term = (ATermAppl) entry.getKey();
	// int type = ((Integer) entry.getValue()).intValue();
	//
	// Set types = new HashSet();
	// for(int bit = 0; bit < 8; bit++) {
	// if((type & (1 << bit)) == 1) types.add(TYPES[bit]);
	// }
	//
	// if(types.size() > 1)
	// addWarning("URI " + term.getName()
	// + " has been defined/used as " + types);
	// }
	// }

	private void setKB(KnowledgeBase kb) {
		this.kb = kb;
	}

	public void load(Graph graph, KnowledgeBase kb) throws UnsupportedFeatureException {
		Timer timer = kb.timers.startTimer( "load" );

		monitor.setProgressTitle( "Loading" );
		monitor.setProgressLength( graph.size() );
		monitor.taskStarted();

		clear();

		setGraph( graph );
		setKB( kb );

		resourceLinkTypes = new HashMap();

		defineAnnotationProperty( node2term( RDFS.label.asNode() ) );
		defineAnnotationProperty( node2term( RDFS.comment.asNode() ) );
		defineAnnotationProperty( node2term( RDFS.seeAlso.asNode() ) );
		defineAnnotationProperty( node2term( RDFS.isDefinedBy.asNode() ) );
		defineAnnotationProperty( node2term( OWL.versionInfo.asNode() ) );
		defineAnnotationProperty( node2term( DC.title.asNode() ) );
		defineAnnotationProperty( node2term( DC.description.asNode() ) );
		defineOntologyProperty( node2term( OWL.backwardCompatibleWith.asNode() ) );
		defineOntologyProperty( node2term( OWL.priorVersion.asNode() ) );
		defineOntologyProperty( node2term( OWL.incompatibleWith.asNode() ) );

		processTypes( graph );

		processTriples( graph );

		processUntypedResources();

		monitor.taskFinished();

		timer.stop();
	}

}
