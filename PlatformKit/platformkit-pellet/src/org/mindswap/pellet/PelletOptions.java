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
 * Created on May 6, 2004
 */
package org.mindswap.pellet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.progress.ConsoleProgressMonitor;
import org.mindswap.pellet.utils.progress.ProgressMonitor;
import org.mindswap.pellet.utils.progress.SilentProgressMonitor;
import org.mindswap.pellet.utils.progress.SwingProgressMonitor;

/**
 * This class contains options used throughout different modules of the
 * reasoner. Setting one of the values should have effect in the behavior of the
 * reasoner regardless of whether it is based on Jena or OWL-API (though some
 * options are applicable only in one implementation). Some of these options are
 * to control experimental extensions to the reasoner and may be removed in
 * future releases as these features are completely tested and integrated.
 * 
 * @author Evren Sirin
 */
public class PelletOptions {
	public final static Log	log	= LogFactory.getLog( PelletOptions.class );

	private interface EnumFactory<T> {
		public T create();
	}

	public enum MonitorType implements EnumFactory<ProgressMonitor> {

		CONSOLE(ConsoleProgressMonitor.class), SWING(SwingProgressMonitor.class),
		NONE(SilentProgressMonitor.class);

		private final Class<? extends ProgressMonitor>	c;

		private MonitorType(Class<? extends ProgressMonitor> c) {
			this.c = c;
		}

		public ProgressMonitor create() {
			try {
				return c.newInstance();
			} catch( InstantiationException e ) {
				throw new InternalReasonerException( e );
			} catch( IllegalAccessException e ) {
				throw new InternalReasonerException( e );
			}
		}
	}

	private static void load(URL configFile) {
		log.info( "Reading Pellet configuration file " + configFile );

		Properties properties = new Properties();
		try {
			properties.load( configFile.openStream() );

			USE_UNIQUE_NAME_ASSUMPTION = getBooleanProperty( properties,
					"USE_UNIQUE_NAME_ASSUMPTION", USE_UNIQUE_NAME_ASSUMPTION );

			USE_PSEUDO_NOMINALS = getBooleanProperty( properties, "USE_PSEUDO_NOMINALS",
					USE_PSEUDO_NOMINALS );

			USE_CLASSIFICATION_MONITOR = getEnumProperty( properties, "USE_CLASSIFICATION_MONITOR",
					USE_CLASSIFICATION_MONITOR );

			REALIZE_INDIVIDUAL_AT_A_TIME = getBooleanProperty( properties,
					"REALIZE_INDIVIDUAL_AT_A_TIME", REALIZE_INDIVIDUAL_AT_A_TIME );

			FREEZE_BUILTIN_NAMESPACES = getBooleanProperty( properties,
					"FREEZE_BUILTIN_NAMESPACES", FREEZE_BUILTIN_NAMESPACES );

			IGNORE_DEPRECATED_TERMS = getBooleanProperty( properties, "IGNORE_DEPRECATED_TERMS",
					IGNORE_DEPRECATED_TERMS );

			IGNORE_UNSUPPORTED_AXIOMS = getBooleanProperty( properties,
					"IGNORE_UNSUPPORTED_AXIOMS", IGNORE_UNSUPPORTED_AXIOMS );

			DL_SAFE_RULES = getBooleanProperty( properties, "DL_SAFE_RULES", DL_SAFE_RULES );

			USE_CACHING = getBooleanProperty( properties, "USE_CACHING", USE_CACHING );

			USE_ADVANCED_CACHING = getBooleanProperty( properties, "USE_ADVANCED_CACHING", USE_ADVANCED_CACHING );

			SAMPLING_RATIO = getDoubleProperty( properties, "SAMPLING_RATIO", SAMPLING_RATIO );
			
			FULL_SIZE_ESTIMATE = getBooleanProperty( properties, "FULL_SIZE_ESTIMATE", FULL_SIZE_ESTIMATE );
			
		} catch( FileNotFoundException e ) {
			log.error( "Pellet configuration file cannot be found" );
		} catch( IOException e ) {
			log.error( "I/O error while reading Pellet configuration file" );
		}
	}

	private static boolean getBooleanProperty(Properties properties, String property,
			boolean defaultValue) {
		String value = properties.getProperty( property );

		if( value != null ) {
			value = value.trim();
			if( value.equalsIgnoreCase( "true" ) )
				return true;
			else if( value.equalsIgnoreCase( "false" ) )
				return false;
			else
				log.error( "Ignoring invalid value (" + value + ") for the configuration option "
						+ property );
		}

		return defaultValue;
	}

	private static double getDoubleProperty(Properties properties, String property,
			double defaultValue) {
		String value = properties.getProperty( property );
		double doubleValue = defaultValue;
		
		if( value != null ) {
			try {
				doubleValue = Double.parseDouble( value );
			} catch( NumberFormatException e ) {
				log.error( "Ignoring invalid double value (" + value + ") for the configuration option "
						+ property );
			} 
		}

		return doubleValue;
	}
	
	private static <T extends Enum<T>> T getEnumProperty(Properties properties, String property,
			T defaultValue) {
		String value = properties.getProperty( property );

		if( value != null ) {
			value = value.trim().toUpperCase();
			try {
				return Enum.valueOf( defaultValue.getDeclaringClass(), value );
			} catch( IllegalArgumentException e ) {
				log.error( "Ignoring invalid value (" + value + ") for the configuration option "
						+ property );
			}
		}

		return defaultValue;
	}

	/**
	 * When this option is set completion will go on even if a clash is detected
	 * until the completion graph is saturated. Turning this option has very
	 * severe performance effect and right now is only used for experimental
	 * purposes to generate explanations.
	 * <p>
	 * <b>*********** DO NOT CHANGE THE VALUE OF THIS OPTION **************</b>
	 */
	public static boolean		SATURATE_TABLEAU					= false;

	/**
	 * This option tells Pellet to treat every individual with a distinct URI to
	 * be different from each other. This is against the semantics of OWL but is
	 * much more efficient than adding an <code><owl:AllDifferent></code>
	 * definition with all the individuals. This option does not affect b-nodes,
	 * they can still be inferred to be same.
	 */
	public static boolean		USE_UNIQUE_NAME_ASSUMPTION			= false;

	/**
	 * @deprecated According to SPARQL semantics all variables are distinguished
	 *             by definition and bnodes in the query are non-distinguished
	 *             variables so this option is not used anymore
	 */
	public static boolean		TREAT_ALL_VARS_DISTINGUISHED		= false;

	/**
	 * Sort the disjuncts based on the statistics
	 */
	public static boolean		USE_DISJUNCT_SORTING				= true && !SATURATE_TABLEAU;

	public static MonitorType	USE_CLASSIFICATION_MONITOR			= MonitorType.NONE;

	public static final String	NO_SORTING							= "NO";
	public static final String	OLDEST_FIRST						= "OLDEST_FIRST";
	public static String		USE_DISJUNCTION_SORTING				= OLDEST_FIRST;

	/**
	 * When this option is enabled all entities (classes, properties,
	 * individuals) are identified using local names rather than full URI's.
	 * This makes the debugging messages shorter and easier to inspect by eye.
	 * This options should be used with care because it is relatively easy to
	 * have local names with different namespaces clash.
	 * <p>
	 * <b>*** This option should only be used for debugging purposes. ***</b>
	 */
	public static boolean		USE_LOCAL_NAME						= false;
	public static boolean		USE_QNAME							= false;

	/**
	 * TBox absorption will be used to move some of the General Inclusion Axioms
	 * (GCI) from Tg to Tu.
	 */
	public static boolean		USE_ABSORPTION						= true;

	/**
	 * Absorb TBox axioms into domain/range restrictions in RBox
	 */
	public static boolean		USE_ROLE_ABSORPTION					= true;

	/**
	 * Absorb TBox axioms about nominals into ABox assertions
	 */
	public static boolean		USE_NOMINAL_ABSORPTION				= true;

	public static boolean		USE_HASVALUE_ABSORPTION				= true;

	/**
	 * Optimization specific to Econnections
	 */
	public static boolean		USE_OPTIMIZEDINDIVIDUALS			= false;

	/**
	 * Use dependency directed backjumping
	 */
	public static boolean		USE_BACKJUMPING						= !SATURATE_TABLEAU & true;

	/**
	 * Check the cardinality restrictions on datatype properties and handle
	 * inverse functional datatype properties
	 */
	public static boolean		USE_FULL_DATATYPE_REASONING			= true;

	/**
	 * Whenever an rdfs:Datatype is seen, Pellet can try to retrieve and parse
	 * the XML Schema document automatically. Otherwise, the datatype will be
	 * treated as an unknown datatype
	 */
	public static boolean		AUTO_XML_SCHEMA_LOADING				= false;

	/**
	 * Cache the pseudo models for named classes and individuals.
	 */
	public static boolean		USE_CACHING							= true;

	/**
	 * Cache the pseudo models for anonymous classes. Used inside
	 * EmptySHNStrategy to prevent the expansion of completion graph nodes whose
	 * satisfiability status is already cached.
	 */
	public static boolean		USE_ADVANCED_CACHING				= true;

	/**
	 * To decide if individual <code>i</code> has type class <code>c</code>
	 * check if the edges from cached model of <code>c</code> to nominal nodes
	 * also exists for the cached model of <code>i</code>.
	 */
	public static boolean		CHECK_NOMINAL_EDGES					= true;


	/**
	 * When a consistency check starts in ABox use the cached pseudo model as
	 * the starting point rather than the original ABox. Since all the branching
	 * information is already stored in the pseudo model, this should be
	 * logically equivalent but much faster
	 */
	public static boolean		USE_PSEUDO_MODEL					= true;

	/**
	 * Treat nominals (classes defined by enumeration) as named atomic concepts
	 * rather than individual names. Turning this option improves the
	 * performance but soundness and completeness cannot be established.
	 */
	public static boolean		USE_PSEUDO_NOMINALS					= false;

	/**
	 * This option is mainly used for debugging and causes the reasoner to
	 * ignore all inverse properties including inverseOf,
	 * InverseFunctionalProperty and SymmetricProperty definitions.
	 */
	public static boolean		IGNORE_INVERSES						= false;

	/**
	 * Dynamically find the best completion strategy for the KB. If disabled
	 * SHION strategy will be used for all the ontologies.
	 */
	public static boolean		USE_COMPLETION_STRATEGY				= !SATURATE_TABLEAU & true;

	/**
	 * Use semantic branching, i.e. add the negation of a disjunct when the next
	 * branch is being tried
	 */
	public static boolean		USE_SEMANTIC_BRANCHING				= !SATURATE_TABLEAU & true;

	/**
	 * The default strategy used for ABox completion. If this values is set,
	 * this strategy will be used for all the KB's regardless of the
	 * expressivity.
	 * <p>
	 * <b>*********** DO NOT CHANGE THE VALUE OF THIS OPTION **************</b>
	 */
	public static Class			DEFAULT_COMPLETION_STRATEGY			= null;

	/**
	 * Print the size of the TBox and ABox after parsing.
	 * 
	 * @deprecated Set KnowledgeBase debug level to info
	 */
	public static boolean		PRINT_SIZE							= false;

	/**
	 * Prefix to be added to bnode identifiers
	 */
	public static final String	BNODE								= "bNode";

	/**
	 * Prefix to be added to anonymous individuals tableaux algorithm creates
	 */
	public static final String	ANON								= "anon";

	/**
	 * When doing a satisfiability check for a concept, do not copy the
	 * individuals even if there are nominals in the KB until you hit a nominal
	 * rule application.
	 */
	public static boolean		COPY_ON_WRITE						= true;

	/**
	 * Control the behavior if a function such as kb.getInstances(),
	 * kb.getTypes(), kb.getPropertyValues() is called with a parameter that is
	 * an undefined class, property or individual. If this option is set to false
	 * then an exception is thrown each time this occurs, if true set the
	 * corresponding function returns a false value (or an empty set where
	 * appropriate).
	 */
	public static boolean		SILENT_UNDEFINED_ENTITY_HANDLING	= true;

	/**
	 * Control the realization strategy where we loop over individuals or
	 * concepts. When this flag is set we loop over each individual and find the
	 * most specific type for that individual by traversing the class hierarchy.
	 * If this flag is not set we traverse the class hierarchy and for each
	 * concept find the instances. Then any individual that is also an instance
	 * of a subclass is removed. Both techniques have advantages and
	 * disadvantages. Best performance depends on the ontology characteristics.
	 */
	public static boolean		REALIZE_INDIVIDUAL_AT_A_TIME		= true;

	/**
	 * Validate ABox structure during completion (Should be used only for
	 * debugging purposes).
	 */
	public static boolean		VALIDATE_ABOX						= false;

	/**
	 * Print completion graph after each iteration (Should be used only for
	 * debugging purposes).
	 */
	public static boolean		PRINT_ABOX							= false;

	public static final boolean	DEPTH_FIRST							= true;
	public static final boolean	BREADTH_FIRST						= false;

	/**
	 * Keep ABox assertions in the KB so they can be accessed later. Currently
	 * not used by the reasoner but could be useful for outside applications.
	 */
	public static boolean		KEEP_ABOX_ASSERTIONS				= false;

	public static boolean		SEARCH_TYPE							= DEPTH_FIRST;

	public static boolean		USE_BINARY_INSTANCE_RETRIEVAL		= true;

	/**
	 * Use optimized blocking even for SHOIN. It is not clear that using this
	 * blocking method would be sound or complete. It is here just for
	 * experimental purposes.
	 * <p>
	 * <b>*********** DO NOT CHANGE THE VALUE OF THIS OPTION **************</b>
	 */
	public static boolean		FORCE_OPTIMIZED_BLOCKING			= false;

	/**
	 * @deprecated Query is always split to ensure correctness
	 */
	public static boolean		SPLIT_QUERY							= true;

	/**
	 * Remove query atoms that are trivially entailed by other atoms. For
	 * example, the query <blockquote>
	 * <code>query(x, y) :- Person(x), worksAt(x, y), Organization(y)</code>
	 * </blockquote> can be simplified to <blockquote>
	 * <code>query(x, y) :- worksAt(x, y)</code> </blockquote> if the domain
	 * of <code>worksAt</code> is <code>Person</code> and the range is
	 * <code>Organization</code>.
	 */
	public static boolean		SIMPLIFY_QUERY						= true;

	/**
	 * @deprecated Set SAMPLING_RATIO to 0 to disable query reordering
	 */
	public static boolean		REORDER_QUERY						= false;

	/**
	 * The ratio of individuals that will be inspected while generating the size
	 * estimate. The query reordering optimization uses size estimates for classes 
	 * and properties to estimate the cost of a certain query ordering. The size
	 * estimates are computed by random sampling. Increasing the sampling ratio 
	 * yields more accurate results but is very costly for large ABoxes. 
	 */
	public static double		SAMPLING_RATIO						= 0.2;
	
	/**
	 * This option controls if the size estimates for all the classes and properties
	 * in a KB will be computed fully when the PelletQueryExecution object is created.
	 */
	public static boolean		FULL_SIZE_ESTIMATE					= false;

	public static boolean		CACHE_RETRIEVAL						= false;

	public static boolean		USE_TRACING							= false;

	public static String		DEFAULT_CONFIGURATION_FILE			= "pellet.properties";

	public static boolean		DOUBLE_CHECK_ENTAILMENTS			= false;

	/**
	 * With this option all triples that contains an unrecognized term from RDF,
	 * RDF-S, OWL, OWL 1.1, or XSD namespaces will be ignored.
	 */
	public static boolean		FREEZE_BUILTIN_NAMESPACES			= true;

	/**
	 * This option causes all classes and properties defined as deprecated
	 * (using <code>owl:DeprecetedClass</code> or
	 * <code>owl:DeprecetedProperty</code>) to be ignored. If turned off,
	 * these will be treated as ordinary classes and properties. Note that, even
	 * if this option is turned on deprecated entities used in ordinary axioms
	 * will be added to the KB.
	 */
	public static boolean		IGNORE_DEPRECATED_TERMS				= true;

	/**
	 * This option controls the behavior of Pellet while an ontology is being
	 * loaded. Some axioms, e.g. cardinality restrictions on transitive
	 * properties, is not supported by Pellet. If an axiom is used in the input
	 * ontology Pellet can just ignore that axiom (and print a warning) or
	 * simply throw an exception at the time that axiom is added to the KB.
	 * Default behavior is to ignore unsupported axioms.
	 */
	public static boolean		IGNORE_UNSUPPORTED_AXIOMS			= true;

	/**
	 * This option tells the reasoner to enable support for DL-safe rules
	 * (encoded in SWRL). If the value is set to ture then the rules will be
	 * taken into account during reasoning. Otherwise, rules will simply be
	 * ignored by the reasoner. Note that, some SWRL features such as
	 * DatavaluedPropertyAtom and BuiltinAtom is not supported. The behavior for
	 * what happens when rules containing such atoms is controlled by the
	 * {@link #IGNORE_UNSUPPORTED_AXIOMS} option, e.g. such rules can be ignored
	 * or reasoner can throw an exception.
	 */
	public static boolean		DL_SAFE_RULES						= true;

	static {
		String configFile = System.getProperty( "pellet.configuration" );

		URL url = null;

		// if the user has not specified the pellet.configuration
		// property, we search for the file "pellet.properties"
		if( configFile == null ) {
			url = PelletOptions.class.getClassLoader().getResource( DEFAULT_CONFIGURATION_FILE );
		}
		else {
			try {
				url = new URL( configFile );
			} catch( MalformedURLException ex ) {
				ex.printStackTrace();

				// so, resource is not a URL:
				// attempt to get the resource from the class path
				url = PelletOptions.class.getClassLoader().getResource( configFile );
			}

			if( url == null )
				log.error( "Cannot file Pellet configuration file " + configFile );
		}

		if( url != null )
			load( url );
	}

	/**
	 * Flag set if the completion queue should be utilized. This optimization
	 * will introduce memory overhead but will (in some cases) dramatically
	 * reduce reasoning time. Rather than iterating over all individuals during
	 * the completion strategy, only those which need to have the rules fired
	 * are selected for rule applications.
	 */
	public static boolean		USE_COMPLETION_QUEUE				= false;
	
	
	
	
	/**
	 * During backjumping use dependency set information to restore node labels
	 * rather than restoring the label exactly to the previous state.
	 */
	public static boolean		USE_SMART_RESTORE					= true && !USE_COMPLETION_QUEUE;
	
	
	

	/**
	 * Flag set if incremental consistency checking should be used. Currently it
	 * can only be used on KBs with SHIQ or SHOQ expressivity
	 */
	public static boolean		USE_INCREMENTAL_CONSISTENCY			= false && USE_COMPLETION_QUEUE;

	/**
	 * Flag set if incremental support for deletions should be used. Currently
	 * it can only be used on KBs with SHIQ or SHOQ expressivity. This flag is
	 * used as incremental deletions introduces memory overhead, which may not
	 * be suitable for some KBs
	 */
	public static boolean		USE_INCREMENTAL_DELETION			= true
																			&& USE_INCREMENTAL_CONSISTENCY
																			&& USE_TRACING;

	/**
	 * Flag if the completion queue should be maintained through incremental
	 * deletions. It can be the case that a removal of a syntactic assertion
	 * will require a queue element to be removed, as it is no longer
	 * applicable. If this is set to false then a simple check before each rule
	 * is fired will be performed - if the ds for the label is null, then the
	 * rule will not be fired. If this is set to true and tracing is on, then
	 * the queue will be maintained through deletions. TODO: Note currently the
	 * queue maintenance is not implemented, so this should always be FALSE!
	 * <p>
	 * <b>*********** DO NOT CHANGE THE VALUE OF THIS OPTION **************</b>
	 */
	public static boolean		MAINTAIN_COMPLETION_QUEUE			= false && USE_TRACING
																			&& USE_COMPLETION_QUEUE;

	/**
	 * Use (if applicable) special optimization for completely defined (CD)
	 * concepts during classification.
	 */
	public static boolean		USE_CD_CLASSIFICATION				= true;

}
