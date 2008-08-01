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
 * Created on May 5, 2004
 */
package org.mindswap.pellet;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
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
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.datatypes.DatatypeReasoner;
import org.mindswap.pellet.exceptions.InconsistentOntologyException;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.exceptions.UndefinedEntityException;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.output.ATermBaseVisitor;
import org.mindswap.pellet.output.OutputFormatter;
//import org.mindswap.pellet.query.QueryEngine;
//import org.mindswap.pellet.query.QueryResults;
import org.mindswap.pellet.rete.Rule;
import org.mindswap.pellet.taxonomy.CDOptimizedTaxonomyBuilder;
import org.mindswap.pellet.taxonomy.Taxonomy;
import org.mindswap.pellet.taxonomy.TaxonomyBuilder;
import org.mindswap.pellet.tbox.TBox;
import org.mindswap.pellet.tbox.TBoxFactory;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Bool;
import org.mindswap.pellet.utils.MultiValueMap;
import org.mindswap.pellet.utils.SizeEstimate;
import org.mindswap.pellet.utils.Timer;
import org.mindswap.pellet.utils.Timers;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;

/**
 * @author Evren Sirin
 */
public class KnowledgeBase {
	public final static Log					log					= LogFactory
																		.getLog( KnowledgeBase.class );

	/**
	 * @deprecated Edit log4j.properties to turn on debugging
	 */
	public static boolean					DEBUG				= false;

	protected ABox							abox;
	protected TBox							tbox;
	protected RBox							rbox;

	private Set<ATermAppl>					individuals;

	protected TaxonomyBuilder				builder;
	protected Taxonomy						taxonomy;
	protected Taxonomy						roleTaxonomy;

	private boolean							consistent;

	private SizeEstimate					estimate;

	protected int							status;

	protected static final int				UNCHANGED			= 0x0000;
	protected static final int				ABOX_CHANGED		= 0x0001;
	protected static final int				TBOX_CHANGED		= 0x0002;
	protected static final int				RBOX_CHANGED		= 0x0004;
	protected static final int				ALL_CHANGED			= 0x0007;
	protected static final int				CONSISTENCY			= 0x0008;
	protected static final int				CLASSIFICATION		= 0x0010;
	protected static final int				REALIZATION			= 0x0020;

	private Map<ATermAppl, Set<ATermAppl>>	instances;

	private Expressivity					expressivity;

	/**
	 * Timers used in various different parts of KB. There may be many different
	 * timers created here depending on the level of debugging or application
	 * requirements. However, there are three major timers that are guaranteed
	 * to exist.
	 * <ul>
	 * <li> <b>main</b> - This is the main timer that exists in any Timers
	 * objects. All the other timers defined in here will have this timer as its
	 * dependant so setting a timeout on this timer will put a limit on every
	 * operation done inside KB.</li>
	 * <li> <b>preprocessing</b> - This is the operation where TBox creation,
	 * absorbtion and normalization is done. It also includes computing
	 * hierarchy of properties in RBox and merging the individuals in ABox if
	 * there are explicit sameAs assertions.</li>
	 * <li> <b>consistency</b> - This is the timer for ABox consistency check.
	 * Putting a timeout will mean that any single consistency check should be
	 * completed in a certain amount of time.</li>
	 * </ul>
	 */
	public Timers							timers				= new Timers();

	private Set<Rule>						rules;

	// !!!!THE FOLLOWING ARE USED FOR INCREMENTAL REASONING!!!!
	// Structure for tracking which assertions are deleted
	protected Set<ATermAppl>				deletedAssertions;

	// Flag used to detect if we have both addition and deletion updates
	// This should be refactored
	protected boolean						aboxDeletion		= false;
	protected boolean						aboxAddition		= false;

	// Index used for abox deletions
	private DependencyIndex					dependencyIndex;

	// set of syntactic assertions
	private Set<ATermAppl>					syntacticAssertions;
	
	public enum AssertionType { TYPE, OBJ_ROLE, DATA_ROLE }
	protected MultiValueMap<AssertionType,ATermAppl> aboxAssertions;

	// !!!!!!!!

	FullyDefinedClassVisitor				fullyDefinedVisitor	= new FullyDefinedClassVisitor();

	class FullyDefinedClassVisitor extends ATermBaseVisitor {

		private boolean	fullyDefined	= true;

		public boolean isFullyDefined(ATermAppl term) {
			fullyDefined = true;
			visit( term );
			return fullyDefined;
		}

		private void visitQCR(ATermAppl term) {
			visitRestr( term );
			if( fullyDefined ) {
				ATermAppl q = (ATermAppl) term.getArgument( 2 );
				if( !isDatatype( q ) )
					this.visit( q );
			}
		}

		private void visitQR(ATermAppl term) {
			visitRestr( term );
			if( fullyDefined ) {
				ATermAppl q = (ATermAppl) term.getArgument( 1 );
				if( !isDatatype( q ) )
					this.visit( q );
			}
		}

		private void visitRestr(ATermAppl term) {
			fullyDefined = fullyDefined && isProperty( term.getArgument( 0 ) );
		}

		@Override
		public void visit(ATermAppl term) {
			if( term.equals( ATermUtils.TOP ) || term.equals( ATermUtils.BOTTOM )
					|| term.equals( ATermUtils.TOP_LIT ) || term.equals( ATermUtils.BOTTOM_LIT ) )
				return;

			super.visit( term );
		}

		public void visitAll(ATermAppl term) {
			visitQR( term );
		}

		public void visitAnd(ATermAppl term) {
			if( fullyDefined )
				visitList( (ATermList) term.getArgument( 0 ) );
		}

		public void visitCard(ATermAppl term) {
			visitQCR( term );
		}

		public void visitHasValue(ATermAppl term) {
			visitQR( term );
		}

		public void visitLiteral(ATermAppl term) {
			return;
		}

		public void visitMax(ATermAppl term) {
			visitQCR( term );
		}

		public void visitMin(ATermAppl term) {
			visitQCR( term );
		}

		public void visitNot(ATermAppl term) {
			this.visit( (ATermAppl) term.getArgument( 0 ) );
		}

		public void visitOneOf(ATermAppl term) {
			if( fullyDefined )
				visitList( (ATermList) term.getArgument( 0 ) );
		}

		public void visitOr(ATermAppl term) {
			if( fullyDefined )
				visitList( (ATermList) term.getArgument( 0 ) );
		}

		public void visitSelf(ATermAppl term) {
			visitRestr( term );
		}

		public void visitSome(ATermAppl term) {
			visitQR( term );
		}

		public void visitTerm(ATermAppl term) {
			fullyDefined = fullyDefined && tbox.getClasses().contains( term );
			if( !fullyDefined )
				return;
		}

		public void visitValue(ATermAppl term) {
			ATermAppl nominal = (ATermAppl) term.getArgument( 0 );
			if( !ATermUtils.isLiteral( nominal ) )
				fullyDefined = fullyDefined && individuals.contains( nominal );
		}

	}

	/**
	 * 
	 */
	public KnowledgeBase() {
		clear();

		timers.createTimer( "preprocessing" );
		timers.createTimer( "consistency" );
		status = ALL_CHANGED;

		deletedAssertions = new HashSet<ATermAppl>();
		dependencyIndex = new DependencyIndex( this );
		syntacticAssertions = new HashSet<ATermAppl>();
		
		aboxAssertions = new MultiValueMap<AssertionType,ATermAppl>();
	}

	/**
	 * Create a KB based on an existing one. New KB has a copy of the ABox but
	 * TBox and RBox is shared between two.
	 * 
	 * @param kb
	 */
	protected KnowledgeBase(KnowledgeBase kb, boolean emptyABox) {
		tbox = kb.tbox;
		rbox = kb.rbox;

		aboxAssertions = new MultiValueMap<AssertionType,ATermAppl>();

		if( emptyABox ) {
			abox = new ABox( this );			
			
			individuals = new HashSet<ATermAppl>();
			instances = new HashMap<ATermAppl, Set<ATermAppl>>();		

			// even though we don't copy the individuals over to the new KB
			// we should still create individuals for the 
			Set nominals = kb.getExpressivity().getNominals();
			for( Iterator i = nominals.iterator(); i.hasNext(); ) {
				ATermAppl nominal = (ATermAppl) i.next();
				addIndividual( nominal );
			}
			
			deletedAssertions = new HashSet<ATermAppl>();
			dependencyIndex = new DependencyIndex( this );
			syntacticAssertions = new HashSet<ATermAppl>();
		}
		else {
			abox = kb.abox.copy();
			
			if( PelletOptions.KEEP_ABOX_ASSERTIONS ) {
				for( AssertionType assertionType : AssertionType.values() ) {
					Set<ATermAppl> assertions = kb.aboxAssertions.get( assertionType );
					if( !assertions.isEmpty() )
						aboxAssertions.put( assertionType, new HashSet<ATermAppl>( assertions ) );
				}
			}		

			individuals = new HashSet<ATermAppl>( kb.individuals );
			instances = new HashMap<ATermAppl, Set<ATermAppl>>( kb.instances );		
			
			// copy deleted assertions
			deletedAssertions = new HashSet<ATermAppl>( kb.deletedAssertions );

			if( PelletOptions.USE_INCREMENTAL_CONSISTENCY && PelletOptions.USE_INCREMENTAL_DELETION ) {
				// copy the dependency index
				dependencyIndex = new DependencyIndex( this, dependencyIndex );
			}

			// copy syntactic assertions
			syntacticAssertions = new HashSet<ATermAppl>( kb.syntacticAssertions );
		}

		expressivity = kb.expressivity;

		status = ALL_CHANGED;

		timers = kb.timers;
		// timers.createTimer("preprocessing");
		// timers.createTimer("consistency");
	}

	public Expressivity getExpressivity() {
		// if we can use incremental reasoning then expressivity has been
		// updated as only the ABox was incrementally changed
		if( canUseIncConsistency() )
			return expressivity;

		prepare();

		return expressivity;
	}

	public void clear() {
		abox = new ABox( this );
		tbox = TBoxFactory.createTBox( this );
		rbox = new RBox();

		expressivity = new Expressivity( this );
		individuals = new HashSet<ATermAppl>();
		
		aboxAssertions = new MultiValueMap<AssertionType,ATermAppl>();

		instances = new HashMap<ATermAppl, Set<ATermAppl>>();
		// typeChecks = new HashMap();

		builder = null;

		status = ALL_CHANGED;
	}

	/**
	 * Create a copy of this KB with a completely new ABox copy but pointing to
	 * the same RBox and TBox.
	 * 
	 * @return A copy of this KB
	 */
	public KnowledgeBase copy() {
		return copy( false );
	}

	/**
	 * Create a copy of this KB. Depending on the value of
	 * <code>emptyABox</code> either a completely new copy of ABox will be
	 * created or the new KB will have an empty ABox. If <code>emptyABox</code> 
	 * parameter is true but the original KB contains nominals in its RBox or 
	 * TBox the new KB will have the 
	 * definition of those individuals (but not )
	 * In either case, the new KB
	 * will point to the same RBox and TBox so changing one KB's RBox or TBox
	 * will affect other. 
	 * 
	 * @param emptyABox If <code>true</code> ABox is not copied to the new KB
	 * @return A copy of this KB
	 */	 
	public KnowledgeBase copy(boolean emptyABox) {
		return new KnowledgeBase( this, emptyABox );
	}

	public void loadKRSS(Reader reader) throws IOException {
		KRSSLoader loader = new KRSSLoader();
		loader.load( reader, this );
	}

	public void addClass(ATermAppl c) {
		if( c.equals( ATermUtils.TOP ) || ATermUtils.isComplexClass( c ) )
			return;

		boolean added = tbox.addClass( c );
		
		if( added ) { 
			status |= TBOX_CHANGED;
			
			if( log.isDebugEnabled() )
				log.debug( "class " + c );
		}
	}

	public void addSubClass(ATermAppl sub, ATermAppl sup) {
		ATermAppl subAxiom = ATermUtils.makeSub( sub, sup );

		Set<ATermAppl> explain = null;
		if( PelletOptions.USE_TRACING )
			explain = Collections.singleton( subAxiom );
		else
			explain = Collections.emptySet();

		addSubClass( sub, sup, explain );
	}

	private void addSubClass(ATermAppl sub, ATermAppl sup, Set<ATermAppl> explain) {
		if( sub.equals( sup ) )
			return;

		status |= TBOX_CHANGED;

		ATermAppl subAxiom = ATermUtils.makeSub( sub, sup );
		tbox.addAxiom( subAxiom, explain );
		if( log.isDebugEnabled() )
			log.debug( "sub-class " + sub + " " + sup );
	}

	/**
	 * @deprecated Use {@link #addEquivalentClass(ATermAppl, ATermAppl)} instead
	 */
	public void addSameClass(ATermAppl c1, ATermAppl c2) {
		addEquivalentClass( c1, c2 );
	}

	public void addEquivalentClass(ATermAppl c1, ATermAppl c2) {
		if( c1.equals( c2 ) )
			return;

		status |= TBOX_CHANGED;

		ATermAppl sameAxiom = ATermUtils.makeSame( c1, c2 );
		Set<ATermAppl> explanation = Collections.emptySet();
		if( PelletOptions.USE_TRACING )
			explanation = Collections.singleton( sameAxiom );

		tbox.addAxiom( sameAxiom, explanation );
		if( log.isDebugEnabled() )
			log.debug( "eq-class " + c1 + " " + c2 );
	}

	public void addDisjointClasses(ATermList classes) {
		Set<ATermAppl> explain = null;
		if( PelletOptions.USE_TRACING )
			explain = Collections.singleton( ATermUtils.makeDisjoints( classes ) );
		else
			explain = Collections.emptySet();

		for( ATermList l1 = classes; !l1.isEmpty(); l1 = l1.getNext() ) {
			ATermAppl c1 = (ATermAppl) l1.getFirst();
			for( ATermList l2 = l1.getNext(); !l2.isEmpty(); l2 = l2.getNext() ) {
				ATermAppl c2 = (ATermAppl) l2.getFirst();
				addDisjointClass( c1, c2, explain );
			}
		}
		if( log.isDebugEnabled() )
			log.debug( "disjoints " + classes );
	}

	public void addDisjointClasses(List classes) {
		addDisjointClasses( ATermUtils.toSet( classes ) );
	}

	public void addDisjointClass(ATermAppl c1, ATermAppl c2) {
		Set<ATermAppl> explain = null;
		if( PelletOptions.USE_TRACING )
			explain = Collections.singleton( ATermUtils.makeDisjoint( c1, c2 ) );
		else
			explain = Collections.emptySet();

		addDisjointClass( c1, c2, explain );
	}

	public void addDisjointClass(ATermAppl c1, ATermAppl c2, Set<ATermAppl> explain) {
		status |= TBOX_CHANGED;

		ATermAppl notC2 = ATermUtils.makeNot( c2 );
		ATermAppl notC1 = ATermUtils.makeNot( c1 );
		tbox.addAxiom( ATermUtils.makeSub( c1, notC2 ), explain );
		tbox.addAxiom( ATermUtils.makeSub( c2, notC1 ), explain );
		if( log.isDebugEnabled() )
			log.debug( "disjoint " + c1 + " " + c2 );
	}

	public void addComplementClass(ATermAppl c1, ATermAppl c2) {
		status |= TBOX_CHANGED;
		ATermAppl notC2 = ATermUtils.makeNot( c2 );

		if( c1.equals( notC2 ) )
			return;

		Set<ATermAppl> ds = null;
		if( PelletOptions.USE_TRACING )
			ds = Collections.singleton( ATermUtils.makeComplement( c1, c2 ) );
		else
			ds = Collections.emptySet();

		ATerm notC1 = ATermUtils.makeNot( c1 );
		tbox.addAxiom( ATermUtils.makeSame( c1, notC2 ), ds );
		tbox.addAxiom( ATermUtils.makeSame( c2, notC1 ), ds );

		if( log.isDebugEnabled() )
			log.debug( "complement " + c1 + " " + c2 );
	}

	/**
	 * Add the value of a DatatypeProperty.
	 * 
	 * @param p
	 *            Datatype Property
	 * @param ind
	 *            Individual value being added to
	 * @param literalValue
	 *            A literal ATerm which should be constructed with one of
	 *            ATermUtils.makeXXXLiteral functions
	 * @deprecated Use addPropertyValue instead
	 */
	public void addDataPropertyValue(ATermAppl p, ATermAppl s, ATermAppl o) {
		addPropertyValue( p, s, o );
	}

	public Individual addIndividual(ATermAppl i) {

		Node node = abox.getNode( i );
		if( node == null ) {
			abox.setSyntacticUpdate( true );
			status |= ABOX_CHANGED;
			node = abox.addIndividual( i );
			individuals.add( i );
			if( log.isDebugEnabled() )
				log.debug( "individual " + i );
			abox.setSyntacticUpdate( false );
		}
		else if( node instanceof Literal )
			throw new UnsupportedFeatureException(
					"Trying to use a literal as an individual. Literal ID: " + i.getName() );

		// set addition flag
		aboxAddition = true;

		// if we can use inc reasoning then update pseudomodel
		if( canUseIncConsistency() ) {
			abox.getPseudoModel().setSyntacticUpdate( true );
			Node pNode = abox.getPseudoModel().getNode( i );
			if( pNode == null ) {
				status |= ABOX_CHANGED;
				// add to pseudomodel - note branch must be temporarily set to 0
				// to ensure that asssertion
				// will not be restored during backtracking
				int branch = abox.getPseudoModel().getBranch();
				abox.getPseudoModel().setBranch( 0 );
				pNode = abox.getPseudoModel().addIndividual( i );
				abox.getPseudoModel().setBranch( branch );

				// need to update the branch node count as this is node has been
				// added
				// other wise during back jumping this node can be removed
				for( int j = 0; j < abox.getPseudoModel().getBranches().size(); j++ ) {
					// get next branch
					((Branch) abox.getPseudoModel().getBranches().get( j )).nodeCount++;
				}
			}
			
			//track updated and new individuals; this is needed for the incremental completion strategey
			abox.updatedIndividuals.add( abox.getPseudoModel().getIndividual( i ) );
			abox.newIndividuals.add( abox.getPseudoModel().getIndividual( i ) );
			abox.getPseudoModel().setSyntacticUpdate( false );
		}

		return (Individual) node;
	}

	public void addType(ATermAppl i, ATermAppl c) {
		ATermAppl typeAxiom = ATermUtils.makeTypeAtom( i, c );
		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( typeAxiom )
			: DependencySet.INDEPENDENT;

		// add type assertion to syntactic assertions and update dependency
		// index
		if( PelletOptions.USE_INCREMENTAL_DELETION ) {
			syntacticAssertions.add( typeAxiom );
			dependencyIndex.addTypeDependency( i, c, ds );
		}
		
		if( PelletOptions.KEEP_ABOX_ASSERTIONS )
			aboxAssertions.add( AssertionType.TYPE, typeAxiom );

		addType( i, c, ds );
	}

	public void addType(ATermAppl i, ATermAppl c, DependencySet ds) {
		status |= ABOX_CHANGED;

		// set addition flag
		aboxAddition = true;

		abox.setSyntacticUpdate( true );
		abox.addType( i, c, ds );
		abox.setSyntacticUpdate( false );

		// if use incremental reasoning then update the cached pseudo model as
		// well
		if( canUseIncConsistency() ) {
			// add this individuals to the affected list - used for inc.
			// consistency checking
			abox.updatedIndividuals.add( abox.getPseudoModel().getIndividual( i ) );
			// as there can possibly be many branches in the pseudomodel, we
			// need a workaround, so temporarily set branch to 0 and then add to
			// the pseudomdel
			int branch = abox.getPseudoModel().getBranch();
			abox.getPseudoModel().setSyntacticUpdate( true );
			abox.getPseudoModel().setBranch( 0 );
			abox.getPseudoModel().addType( i, c );
			abox.getPseudoModel().setBranch( branch );
			abox.getPseudoModel().setSyntacticUpdate( false );

			// incrementally update the expressivity of the KB, so that we do
			// not have to reperform if from scratch!
			updateExpressivity( i, c );
		}

		if( log.isDebugEnabled() )
			log.debug( "type " + i + " " + c );
	}

	public void addSame(ATermAppl i1, ATermAppl i2) {
		status |= ABOX_CHANGED;

		// set addition flag
		aboxAddition = true;

		if( canUseIncConsistency() ) {
			abox.updatedIndividuals.add( abox.getPseudoModel().getIndividual( i1 ) );
			abox.updatedIndividuals.add( abox.getPseudoModel().getIndividual( i2 ) );

			// add to pseudomodel - note branch is not set to zero - this is
			// done in SHOIQIncStrategy, prior
			// to merging nodes
			abox.getPseudoModel().addSame( i1, i2 );
		}

		abox.addSame( i1, i2 );
		if( log.isDebugEnabled() )
			log.debug( "same " + i1 + " " + i2 );
	}

	public void addAllDifferent(ATermList list) {
		status |= ABOX_CHANGED;

		// set addition flag
		aboxAddition = true;

		// if we can use incremental consistency checking then add to
		// pseudomodel
		if( canUseIncConsistency() ) {
			ATermList outer = list;
			// add to updated inds
			while( !outer.isEmpty() ) {
				ATermList inner = outer.getNext();
				while( !inner.isEmpty() ) {
					abox.updatedIndividuals.add( abox.getPseudoModel().getIndividual(
							outer.getFirst() ) );
					abox.updatedIndividuals.add( abox.getPseudoModel().getIndividual(
							inner.getFirst() ) );
					inner = inner.getNext();
				}
				outer = outer.getNext();
			}

			// add to pseudomodel - note branch must be temporarily set to 0 to
			// ensure that asssertion
			// will not be restored during backtracking
			int branch = abox.getPseudoModel().getBranch();
			abox.getPseudoModel().setBranch( 0 );
			// update pseudomodel
			abox.getPseudoModel().addAllDifferent( list );
			abox.getPseudoModel().setBranch( branch );
		}

		abox.addAllDifferent( list );
		if( log.isDebugEnabled() )
			log.debug( "all diff " + list );
	}

	public void addDifferent(ATermAppl i1, ATermAppl i2) {
		status |= ABOX_CHANGED;

		// set addition flag
		aboxAddition = true;

		// if we can use incremental consistency checking then add to
		// pseudomodel
		if( canUseIncConsistency() ) {
			abox.updatedIndividuals.add( abox.getPseudoModel().getIndividual( i1 ) );
			abox.updatedIndividuals.add( abox.getPseudoModel().getIndividual( i2 ) );

			// add to pseudomodel - note branch must be temporarily set to 0 to
			// ensure that asssertion
			// will not be restored during backtracking
			int branch = abox.getPseudoModel().getBranch();
			abox.getPseudoModel().setBranch( 0 );
			abox.getPseudoModel().addDifferent( i1, i2 );
			abox.getPseudoModel().setBranch( branch );
		}

		abox.addDifferent( i1, i2 );
		if( log.isDebugEnabled() )
			log.debug( "diff " + i1 + " " + i2 );
	}

	/**
	 * @deprecated Use addPropertyValue instead
	 */
	public void addObjectPropertyValue(ATermAppl p, ATermAppl s, ATermAppl o) {
		addPropertyValue( p, s, o );
	}

	public boolean addPropertyValue(ATermAppl p, ATermAppl s, ATermAppl o) {
		Individual subj = abox.getIndividual( s );
		Role role = getRole( p );
		Node obj = null;

		if( subj == null ) {
			log.warn( s + " is not a known individual!" );
			return false;
		}

		if( role == null ) {
			log.warn( p + " is not a known property!" );
			return false;
		}
		
		if( !role.isObjectRole() && !role.isDatatypeRole() ) 
			return false;

		ATermAppl propAxiom = ATermUtils.makePropAtom( p, s, o );

		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( propAxiom )
			: DependencySet.INDEPENDENT;

		if( role.isObjectRole() ) {
			obj = abox.getIndividual( o );
			if( obj == null ) {
				if( ATermUtils.isLiteral( o ) ) {
					log.warn( "Ignoring literal value " + o + " for object property " + p );
					return false;
				}
				else {
					log.warn( o + " is not a known individual!" );
					return false;
				}
			}
			if( PelletOptions.KEEP_ABOX_ASSERTIONS )
				aboxAssertions.add( AssertionType.OBJ_ROLE, propAxiom );
		}
		else if( role.isDatatypeRole() ) {
			obj = abox.addLiteral( o, ds );
			if( PelletOptions.KEEP_ABOX_ASSERTIONS )
				aboxAssertions.add( AssertionType.DATA_ROLE, propAxiom );
		}
		
		status |= ABOX_CHANGED;

		// set addition flag
		aboxAddition = true;

		Edge edge = subj.addEdge( role, obj, ds );

		if( PelletOptions.USE_INCREMENTAL_DELETION ) {
			// add to syntactic assertions
			syntacticAssertions.add( propAxiom );

			// add to dependency index
			dependencyIndex.addEdgeDependency( edge, ds );

		}

		// if use inc. reasoning then we need to update the pseudomodel
		if( canUseIncConsistency() ) {
			// add this individual to the affected list
			abox.updatedIndividuals.add( abox.getPseudoModel().getIndividual( s ) );

			// if this is an object property then add the object to the affected
			// list
			if( !role.isDatatypeRole() )
				abox.updatedIndividuals.add( abox.getPseudoModel().getIndividual( o ) );

			if( role.isObjectRole() ) {
				obj = abox.getPseudoModel().getIndividual( o );
				if( obj.isPruned() || obj.isMerged() )
					obj = obj.getSame();

			}
			else if( role.isDatatypeRole() ) {
				obj = abox.getPseudoModel().addLiteral( o );
			}

			// get the subject
			Individual subj2 = abox.getPseudoModel().getIndividual( s );
			if( subj2.isPruned() || subj2.isMerged() )
				subj2 = (Individual) subj2.getSame();

			// generate dependency for new edge
			ds = PelletOptions.USE_TRACING
				? new DependencySet( ATermUtils.makePropAtom( p, s, o ) )
				: DependencySet.INDEPENDENT;

			// add to pseudomodel - note branch must be temporarily set to 0 to
			// ensure that asssertion
			// will not be restored during backtracking
			int branch = abox.getPseudoModel().getBranch();
			abox.getPseudoModel().setBranch( 0 );
			// add the edge
			subj2.addEdge( role, obj, ds );
			abox.getPseudoModel().setBranch( branch );
		}

		if( log.isDebugEnabled() )
			log.debug( "prop-value " + s + " " + p + " " + o );

		return true;
	}

	public boolean addNegatedPropertyValue(ATermAppl p, ATermAppl s, ATermAppl o) {
		status |= ABOX_CHANGED;

		Individual subj = abox.getIndividual( s );
		Role role = getRole( p );
		Node obj = null;

		if( subj == null ) {
			log.warn( s + " is not a known individual!" );
			return false;
		}

		if( role == null ) {
			log.warn( p + " is not a known property!" );
			return false;
		}

		if( role.isObjectRole() ) {
			obj = abox.getIndividual( o );
			if( obj == null ) {
				if( ATermUtils.isLiteral( o ) ) {
					log.warn( "Ignoring literal value " + o + " for object property " + p );
					return false;
				}
				else {
					log.warn( o + " is not a known individual!" );
					return false;
				}
			}
		}
		else if( role.isDatatypeRole() ) {
			obj = abox.addLiteral( o );
		}

		ATermAppl propAxiom = ATermUtils.makeNot( ATermUtils.makePropAtom( p, s, o ) );

		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( propAxiom )
			: DependencySet.INDEPENDENT;

		ATermAppl C = ATermUtils.makeNot( ATermUtils.makeHasValue( p, o ) );
		abox.addType( s, C, ds );

		// add type assertion to syntactic assertions and update dependency
		// index
		if( PelletOptions.USE_INCREMENTAL_DELETION ) {
			syntacticAssertions.add( propAxiom );
			dependencyIndex.addTypeDependency( s, C, ds );
		}

		// if use incremental reasoning then update the cached pseudo model as
		// well
		if( canUseIncConsistency() ) {
			// add this individuals to the affected list - used for inc.
			// consistency checking
			abox.updatedIndividuals.add( abox.getPseudoModel().getIndividual( s ) );
			// add to pseudomodel - note branch must be temporarily set to 0 to
			// ensure that asssertion
			// will not be restored during backtracking
			int branch = abox.getPseudoModel().getBranch();
			abox.getPseudoModel().setSyntacticUpdate( true );
			abox.getPseudoModel().setBranch( 0 );
			abox.getPseudoModel().addType( s, C );
			abox.getPseudoModel().setBranch( branch );
			abox.getPseudoModel().setSyntacticUpdate( false );

			// incrementally update the expressivity of the KB, so that we do
			// not have to reperform if from scratch!
			updateExpressivity( s, C );
		}

		if( log.isDebugEnabled() )
			log.debug( "not-prop-value " + s + " " + p + " " + o );

		return true;
	}

	public void addProperty(ATermAppl p) {
		status |= RBOX_CHANGED;
		rbox.addRole( p );
		if( log.isDebugEnabled() )
			log.debug( "prop " + p );
	}

	/**
	 * Add a new object property. If property was earlier defined to be a
	 * datatype property then this function will simply return without changing
	 * the KB.
	 * 
	 * @param p
	 *            Name of the property
	 * @return True if property is added, false if not
	 */
	public boolean addObjectProperty(ATerm p) {
		boolean exists = getPropertyType( p ) == Role.OBJECT;

		Role role = rbox.addObjectRole( (ATermAppl) p );

		if( !exists ) {
			status |= RBOX_CHANGED;
			if( log.isDebugEnabled() )
				log.debug( "object-prop " + p );
		}
		
		return role != null;
	}

	/**
	 * Add a new object property. If property was earlier defined to be a
	 * datatype property then this function will simply return without changing
	 * the KB.
	 * 
	 * @param p
	 * @return True if property is added, false if not
	 */
	public boolean addDatatypeProperty(ATerm p) {
		boolean exists = getPropertyType( p ) == Role.DATATYPE;

		Role role = rbox.addDatatypeRole( (ATermAppl) p );
		
		if( !exists ) {
			status |= RBOX_CHANGED;
			if( log.isDebugEnabled() )
				log.debug( "data-prop " + p );
		}
		
		return role != null;
	}

	public void addOntologyProperty(ATermAppl p) {
		status |= RBOX_CHANGED;
		rbox.addOntologyRole( p );
		if( log.isDebugEnabled() )
			log.debug( "onto-prop " + p );
	}

	public boolean addAnnotationProperty(ATermAppl p) {
		status |= RBOX_CHANGED;
		Role role = rbox.addAnnotationRole( p );
		if( log.isDebugEnabled() )
			log.debug( "annotation-prop " + p );

		return role != null;
	}

	public void addSubProperty(ATerm sub, ATermAppl sup) {
		status |= RBOX_CHANGED;
		rbox.addSubRole( sub, sup );

		if( log.isDebugEnabled() )
			log.debug( "sub-prop " + sub + " " + sup );
	}

	/**
	 * @deprecated Use {@link #addEquivalentProperty(ATermAppl, ATermAppl)}
	 *             instead
	 */
	public void addSameProperty(ATermAppl p1, ATermAppl p2) {
		addEquivalentProperty( p1, p2 );
	}

	public void addEquivalentProperty(ATermAppl p1, ATermAppl p2) {
		status |= RBOX_CHANGED;
		rbox.addEquivalentRole( p1, p2 );

		if( log.isDebugEnabled() )
			log.debug( "same-prop " + p1 + " " + p2 );
	}

	public void addDisjointProperty(ATermAppl p1, ATermAppl p2) {
		status |= RBOX_CHANGED;
		rbox.addDisjointRole( p1, p2 );

		if( log.isDebugEnabled() )
			log.debug( "dis-prop " + p1 + " " + p2 );
	}

	public void addInverseProperty(ATermAppl p1, ATermAppl p2) {
		if( PelletOptions.IGNORE_INVERSES ) {
			log.warn( "Ignoring inverseOf(" + p1 + " " + p2
					+ ") axiom due to the IGNORE_INVERSES option" );
			return;
		}

		status |= RBOX_CHANGED;

		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( ATermUtils.makeInvProp( p1, p2 ) )
			: DependencySet.INDEPENDENT;

		rbox.addInverseRole( p1, p2, ds );
		if( log.isDebugEnabled() )
			log.debug( "inv-prop " + p1 + " " + p2 );
	}

	public void addTransitiveProperty(ATermAppl p) {
		status |= RBOX_CHANGED;

		Role r = rbox.getDefinedRole( p );

		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( ATermUtils.makeTransitive( p ) )
			: DependencySet.INDEPENDENT;

		// r.setTransitive(true);
		r.addSubRoleChain( ATermUtils.makeList( new ATerm[] { p, p } ), ds );
		if( log.isDebugEnabled() )
			log.debug( "trans-prop " + p );
	}

	public void addSymmetricProperty(ATermAppl p) {
		if( PelletOptions.IGNORE_INVERSES ) {
			log.warn( "Ignoring SymmetricProperty(" + p
					+ ") axiom due to the IGNORE_INVERSES option" );
			return;
		}

		status |= RBOX_CHANGED;

		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( ATermUtils.makeSymmetric( p ) )
			: DependencySet.INDEPENDENT;

		rbox.addInverseRole( p, p, ds );
		if( log.isDebugEnabled() )
			log.debug( "sym-prop " + p );
	}

	public void addAntisymmetricProperty(ATermAppl p) {
		status |= RBOX_CHANGED;
		Role r = rbox.getDefinedRole( p );

		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( ATermUtils.makeAntisymmetric( p ) )
			: DependencySet.INDEPENDENT;

		r.setAntisymmetric( true, ds );
		if( log.isDebugEnabled() )
			log.debug( "anti-sym-prop " + p );
	}

	public void addReflexiveProperty(ATermAppl p) {
		status |= RBOX_CHANGED;
		Role r = rbox.getDefinedRole( p );

		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( ATermUtils.makeReflexive( p ) )
			: DependencySet.INDEPENDENT;

		r.setReflexive( true, ds );
		if( log.isDebugEnabled() )
			log.debug( "reflexive-prop " + p );
	}

	public void addIrreflexiveProperty(ATermAppl p) {
		status |= RBOX_CHANGED;
		Role r = rbox.getDefinedRole( p );

		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( ATermUtils.makeIrreflexive( p ) )
			: DependencySet.INDEPENDENT;

		r.setIrreflexive( true, ds );
		if( log.isDebugEnabled() )
			log.debug( "irreflexive-prop " + p );
	}

	public void addFunctionalProperty(ATermAppl p) {
		status |= RBOX_CHANGED;
		Role r = rbox.getDefinedRole( p );

		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( ATermUtils.makeFunctional( p ) )
			: DependencySet.INDEPENDENT;

		r.setFunctional( true, ds );
		if( log.isDebugEnabled() )
			log.debug( "func-prop " + p );
	}

	public void addInverseFunctionalProperty(ATerm p) {
		if( PelletOptions.IGNORE_INVERSES ) {
			log.warn( "Ignoring InverseFunctionalProperty(" + p
					+ ") axiom due to the IGNORE_INVERSES option" );
			return;
		}

		status |= RBOX_CHANGED;
		Role role = rbox.getDefinedRole( p );

		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( ATermUtils.makeInverseFunctional( p ) )
			: DependencySet.INDEPENDENT;

		role.setInverseFunctional( true, ds );
		if( log.isDebugEnabled() )
			log.debug( "inv-func-prop " + p );
	}

	public void addDomain(ATerm p, ATermAppl c) {
		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( ATermUtils.makeDomain( p, c ) )
			: DependencySet.INDEPENDENT;
		addDomain( p, c, ds );
	}

	/**
	 * For internal use with tracing.
	 * 
	 * @param p
	 * @param c
	 * @param ds
	 */
	public void addDomain(ATerm p, ATermAppl c, DependencySet ds) {
		status |= RBOX_CHANGED;

		Role r = rbox.getDefinedRole( p );

		// TODO Need to do something with the dependency set.
		r.addDomain( c, ds );

		if( log.isDebugEnabled() )
			log.debug( "domain " + p + " " + c + " (" + r.getDomain() + ")" );
	}

	public void addRange(ATerm p, ATermAppl c) {
		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( ATermUtils.makeRange( p, c ) )
			: DependencySet.INDEPENDENT;
		addRange( p, c, ds );
	}

	/**
	 * For internal use with tracing.
	 * 
	 * @param p
	 * @param c
	 * @param ds
	 */
	public void addRange(ATerm p, ATermAppl c, DependencySet ds) {
		status |= RBOX_CHANGED;

		Role r = rbox.getDefinedRole( p );
		// TODO Do something with dependency...
		r.addRange( c, ds );

		if( log.isDebugEnabled() )
			log.debug( "range " + p + " " + c );
	}

	public void addDatatype(ATerm p) {
		DatatypeReasoner dtReasoner = getDatatypeReasoner();
		if( !dtReasoner.isDefined( p.toString() ) ) {
			status |= TBOX_CHANGED;

			dtReasoner.defineUnknownDatatype( p.toString() );
			if( log.isDebugEnabled() )
				log.debug( "datatype " + p );
		}
	}

	public String addDatatype(Datatype datatype) {
		DatatypeReasoner dtReasoner = getDatatypeReasoner();

		status |= TBOX_CHANGED;

		String name = dtReasoner.defineDatatype( datatype );
		if( log.isDebugEnabled() )
			log.debug( "datatype " + name + " " + datatype );

		return name;
	}

	public void addDatatype(String datatypeURI, Datatype datatype) {
		DatatypeReasoner dtReasoner = getDatatypeReasoner();
		if( !dtReasoner.isDefined( datatypeURI ) ) {
			status |= TBOX_CHANGED;

			dtReasoner.defineDatatype( datatypeURI, datatype );
			if( log.isDebugEnabled() )
				log.debug( "datatype " + datatypeURI + " " + datatype );
		}
	}

	public void loadDatatype(ATerm p) {
		DatatypeReasoner dtReasoner = getDatatypeReasoner();
		if( !dtReasoner.isDefined( p.toString() ) ) {
			status |= TBOX_CHANGED;

			dtReasoner.loadUserDefinedDatatype( p.toString() );
			if( log.isDebugEnabled() )
				log.debug( "datatype " + p );
		}
	}

	/**
	 * @deprecated Use {@link #addDatatype(String, Datatype)}
	 */
	public void addDataRange(String datatypeURI, ATermList values) {
		DatatypeReasoner dtReasoner = getDatatypeReasoner();
		if( !dtReasoner.isDefined( datatypeURI.toString() ) ) {
			status |= TBOX_CHANGED;

			Datatype dataRange = dtReasoner.enumeration( ATermUtils.listToSet( values ) );
			getDatatypeReasoner().defineDatatype( datatypeURI.toString(), dataRange );
			if( log.isDebugEnabled() )
				log.debug( "datarange " + datatypeURI.toString() + " " + values );
		}
	}

	public boolean removePropertyValue(ATermAppl p, ATermAppl i1, ATermAppl i2) {

		boolean removed = false;

		// set deletion flag
		aboxDeletion = true;

		Individual subj = abox.getIndividual( i1 );
		Node obj = abox.getNode( i2 );
		Role role = getRole( p );

		if( subj == null ) {
			if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
				throw new UnsupportedFeatureException( i1 + " is not an individual!" );
			else
				return false;
		}

		if( obj == null ) {
			handleUndefinedEntity( i2 + " is not an individual!" );
			return false;
		}

		if( role == null ) {
			handleUndefinedEntity( p + " is not a property!" );
			return false;
		}

		EdgeList edges = subj.getEdgesTo( obj, role );
		for( int i = 0; i < edges.size(); i++ ) {
			Edge edge = edges.edgeAt( i );
			if( edge.getRole().equals( role ) ) {
				subj.removeEdge( edge );
				if( !edge.getTo().getInEdges().removeEdge( edge ) )
					throw new InternalReasonerException( "Trying to remove a non-existing edge "
							+ edge );
				status |= ABOX_CHANGED;
				removed = true;
				break;
			}
		}

		if( log.isDebugEnabled() )
			log.debug( "Remove ObjectPropertyValue " + i1 + " " + p + " " + i2 );

		// if use inc. reasoning then we need to track the deleted assertion.
		// Note that the actual edge will be deleted from the pseudomodel when
		// undo all dependent
		// structures in ABox.isIncConsistent()
		if( canUseIncConsistency() ) {
			// add to deleted assertions
			deletedAssertions.add( ATermUtils.makePropAtom( p, i1, i2 ) );

			// add this individul to the affected list
			abox.updatedIndividuals.add( abox.getPseudoModel().getIndividual( i1 ) );

			// if this is an object propert then add the object to the affected
			// list
			if( !role.isDatatypeRole() )
				abox.updatedIndividuals.add( abox.getPseudoModel().getIndividual( i2 ) );
		}
		
		
		if( PelletOptions.KEEP_ABOX_ASSERTIONS ) {
			ATermAppl propAxiom = ATermUtils.makePropAtom( p, i1, i2 );
			if( ATermUtils.isLiteral( i2 ) )
				aboxAssertions.remove( AssertionType.DATA_ROLE, propAxiom );
			else
				aboxAssertions.remove( AssertionType.OBJ_ROLE, propAxiom );
		}

		return removed;
	}

	public void removeType(ATermAppl ind, ATermAppl c) {
		// set deletion flag
		aboxDeletion = true;

		status |= ABOX_CHANGED;
		Individual subj = abox.getIndividual( ind );

		if( subj == null ) {
			if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
				return;
			else
				throw new UnsupportedFeatureException( ind + " is not an individual!" );
		}

		// CHW- note that i am now normalizeing the concept!
		subj.removeType( ATermUtils.normalize( c ) );
		// subj.removeType(c);

		// if use inc. reasoning then we need to track the deleted assertion.
		// Note that the actual edge will be deleted from the pseudomodel when
		// undo all dependent
		// structures in ABox.isIncConsistent()
		if( canUseIncConsistency() ) {

			// add axiom to deletion set
			deletedAssertions.add( ATermUtils.makeTypeAtom( ind, c ) );

			// add this individuals to the affected list - used for inc.
			// consistency checking
			abox.updatedIndividuals.add( abox.getPseudoModel().getIndividual( ind ) );

			// we may need to update the expressivity here, however so far it
			// does not seem necessary!
			// updateExpressivity(i, c);
		}
		
		if( PelletOptions.KEEP_ABOX_ASSERTIONS ) {
			ATermAppl typeAxiom = ATermUtils.makeTypeAtom( ind, c );
			aboxAssertions.remove( AssertionType.TYPE, typeAxiom );
		}

		if( log.isDebugEnabled() )
			log.debug( "Remove Type " + ind + " " + c );
	}

	public boolean removeAxiom(ATermAppl axiom) {
		boolean removed = false;

		try {
			removed = tbox.removeAxiom( axiom );
		} catch( Exception e ) {
			log.error( "Removal failed for axiom " + axiom, e );
		}

		if( removed )
			status |= TBOX_CHANGED;

		if( log.isDebugEnabled() )
			log.debug( "Remove " + axiom + ": " + removed );

		return removed;
	}

	public void prepare() {
		if( !isChanged() )
			return;

		boolean explain = abox.doExplanation();
		abox.setDoExplanation( true );

		Timer timer = timers.startTimer( "preprocessing" );
		Timer t;

		boolean reuseTaxonomy = (taxonomy != null) && !isTBoxChanged()
				&& (!expressivity.hasNominal() || PelletOptions.USE_PSEUDO_NOMINALS);

		if( isTBoxChanged() ) {
			if( PelletOptions.USE_ABSORPTION ) {
				if( log.isDebugEnabled() )
					log.debug( "Absorbing..." );
				tbox.absorb();
			}

			if( log.isDebugEnabled() )
				log.debug( "Normalizing..." );
			t = timers.startTimer( "normalize" );
			tbox.normalize();
			t.stop();
			if( log.isDebugEnabled() )
				log.debug( "Internalizing..." );
			tbox.internalize();
		}

		if( isRBoxChanged() ) {
			if( log.isDebugEnabled() )
				log.debug( "Role hierarchy..." );
			t = timers.startTimer( "rbox" );
			rbox.prepare();
			t.stop();
		}

		if( log.isDebugEnabled() )
			log.debug( "TBox:\n" + tbox );

		// The preparation of TBox and RBox is finished so we set the
		// status to UNCHANGED now. Expressivity check can only work
		// with prepared KB's
		status = UNCHANGED;

		if( log.isDebugEnabled() )
			log.debug( "Expressivity..." );
		expressivity.compute();

		if( log.isDebugEnabled() )
			log.debug( "ABox init..." );
		instances.clear();
		if( log.isDebugEnabled() )
			log.debug( "done." );

		estimate = new SizeEstimate( this );
		abox.setDoExplanation( explain );

		abox.clearCaches( !reuseTaxonomy );
		abox.cache.setMaxSize( 2 * getClasses().size() );
		if( reuseTaxonomy )
			status |= CLASSIFICATION;
		else
			taxonomy = null;

		timer.stop();

		if( log.isInfoEnabled() ) {
			StringBuffer info = new StringBuffer();
			info.append( "Expressivity: " + expressivity + ", " );
			info.append( "Classes: " + getClasses().size() + " " );
			info.append( "Properties: " + getProperties().size() + " " );
			info.append( "Individuals: " + individuals.size() );
			info.append( " Strategy: " + chooseStrategy( abox ) );
			log.info( info );
		}
	}

	/**
	 * This method is used for incremental reasoning. We do not want to
	 * recompute the expressivity from scratch.
	 */
	public void updateExpressivity(ATermAppl i, ATermAppl c) {

		// if the tbox or rbox changed then we cannot use incremental reasoning!
		if( !isChanged() || isTBoxChanged() || isRBoxChanged() )
			return;

		// update status
		status = UNCHANGED;

		// update expressivity given this individual
		expressivity.processIndividual( i, c );

		// update the size estimate as this could be a new individual
		estimate = new SizeEstimate( this );
	}

	public String getInfo() {
		prepare();

		StringBuffer buffer = new StringBuffer();
		buffer.append( "Expressivity: " + expressivity + " " );
		buffer.append( "Classes: " + getClasses().size() + " " );
		buffer.append( "Properties: " + getProperties().size() + " " );
		buffer.append( "Individuals: " + individuals.size() + " " );
		if( expressivity.hasNominal() )
			buffer.append( "Nominals: " + expressivity.getNominals().size() + " " );
		// if( tbox.getTg().size() > 0 ) {
		// buffer.append("GCIs: " + tbox.getTg().size() );
		// }

		return buffer.toString();
	}

	/**
	 * Returns true if the consistency check has been done and nothing in th KB
	 * has changed after that.
	 */
	public boolean isConsistencyDone() {
		// check if consistency bit is set but none of the change bits
		return (status & (CONSISTENCY | ALL_CHANGED)) == CONSISTENCY;
	}

	/**
	 * Returns true if the classification check has been done and nothing in th
	 * KB has changed after that.
	 */
	public boolean isClassified() {
		return (status & (CLASSIFICATION | ALL_CHANGED)) == CLASSIFICATION;
	}

	public boolean isRealized() {
		return (status & (REALIZATION | ALL_CHANGED)) == REALIZATION;
	}

	public boolean isChanged() {
		return (status & ALL_CHANGED) != 0;
	}

	public boolean isTBoxChanged() {
		return (status & TBOX_CHANGED) != 0;
	}

	public boolean isRBoxChanged() {
		return (status & RBOX_CHANGED) != 0;
	}

	public boolean isABoxChanged() {
		return (status & ABOX_CHANGED) != 0;
	}

	private void consistency() {
		if( isConsistencyDone() )
			return;

		// always turn on explanations for the first consistency check
		boolean explain = abox.doExplanation();
		abox.setDoExplanation( true );

		// only prepare if we are not going to use the incremental consistency
		// checking approach
		if( !canUseIncConsistency() ) {
			prepare();
		}

		Timer timer = timers.startTimer( "consistency" );

		consistent = abox.isConsistent();

		abox.setDoExplanation( explain );

		if( !consistent ) {
			log.warn( "Inconsistent ontology. Reason: " + getExplanation() );
			if( PelletOptions.USE_TRACING )
				log.warn( "ExplanationSet: " + getExplanationSet() );
		}

		timer.stop();

		status |= CONSISTENCY;

		// reset update flags and clear deleted assertions which are tracked
		aboxDeletion = false;
		aboxAddition = false;
		this.deletedAssertions.clear();
	}

	public boolean isConsistent() {
		consistency();

		return consistent;
	}

	public void ensureConsistency() {
		if( !isConsistent() )
			throw new InconsistentOntologyException(
					"Cannot do reasoning with inconsistent ontologies!" );
	}

	public void classify() {
		ensureConsistency();

		if( isClassified() )
			return;

		if( log.isDebugEnabled() )
			log.debug( "Classifying..." );

		Timer timer = timers.startTimer( "classify" );

		builder = getTaxonomyBuilder();

		taxonomy = builder.classify();

		timer.stop();

		// if user canceled return
		if( taxonomy == null ) {
			builder = null;
			return;
		}

		status |= CLASSIFICATION;
	}

	public void realize() {
		if( isRealized() )
			return;

		classify();

		if( !isClassified() )
			return;

		Timer timer = timers.startTimer( "realize" );

		taxonomy = builder.realize();

		timer.stop();

		// if user canceled return
		if( taxonomy == null ) {
			builder = null;
			return;
		}

		status |= REALIZATION;
	}

	/**
	 * Return the set of all named classes. Returned set is unmodifiable!
	 * 
	 * @return
	 */
	public Set<ATermAppl> getClasses() {
		return Collections.unmodifiableSet( tbox.getClasses() );
	}

	/**
	 * Return the set of all named classes including TOP and BOTTOM. Returned
	 * set is modifiable.
	 * 
	 * @return
	 */
	public Set<ATermAppl> getAllClasses() {
		return Collections.unmodifiableSet( tbox.getAllClasses() );
	}

	/**
	 * Return the set of all properties.
	 * 
	 * @return
	 */
	public Set<ATermAppl> getProperties() {
		Set<ATermAppl> set = new HashSet<ATermAppl>();
		Iterator i = rbox.getRoles().iterator();
		while( i.hasNext() ) {
			Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if( ATermUtils.isPrimitive( p ) && (role.isObjectRole() || role.isDatatypeRole()) )
				set.add( p );
		}
		return set;
	}

	/**
	 * Return the set of all object properties.
	 * 
	 * @return
	 */
	public Set<ATermAppl> getObjectProperties() {
		Set<ATermAppl> set = new HashSet<ATermAppl>();
		Iterator i = rbox.getRoles().iterator();
		while( i.hasNext() ) {
			Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if( ATermUtils.isPrimitive( p ) && role.isObjectRole() )
				set.add( p );
		}
		return set;
	}

	public Set<ATermAppl> getTransitiveProperties() {
		Set<ATermAppl> set = new HashSet<ATermAppl>();
		Iterator i = rbox.getRoles().iterator();
		while( i.hasNext() ) {
			Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if( ATermUtils.isPrimitive( p ) && role.isTransitive() )
				set.add( p );
		}
		return set;
	}

	public Set<ATermAppl> getSymmetricProperties() {
		Set<ATermAppl> set = new HashSet<ATermAppl>();
		Iterator i = rbox.getRoles().iterator();
		while( i.hasNext() ) {
			Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if( ATermUtils.isPrimitive( p ) && role.isSymmetric() )
				set.add( p );
		}
		return set;
	}

	public Set<ATermAppl> getAntisymmetricProperties() {
		Set<ATermAppl> set = new HashSet<ATermAppl>();
		Iterator i = rbox.getRoles().iterator();
		while( i.hasNext() ) {
			Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if( ATermUtils.isPrimitive( p ) && role.isAntisymmetric() )
				set.add( p );
		}
		return set;
	}

	public Set<ATermAppl> getReflexiveProperties() {
		Set<ATermAppl> set = new HashSet<ATermAppl>();
		Iterator i = rbox.getRoles().iterator();
		while( i.hasNext() ) {
			Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if( ATermUtils.isPrimitive( p ) && role.isReflexive() )
				set.add( p );
		}
		return set;
	}

	public Set<ATermAppl> getIrreflexiveProperties() {
		Set<ATermAppl> set = new HashSet<ATermAppl>();
		Iterator i = rbox.getRoles().iterator();
		while( i.hasNext() ) {
			Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if( ATermUtils.isPrimitive( p ) && role.isIrreflexive() )
				set.add( p );
		}
		return set;
	}

	public Set<ATermAppl> getFunctionalProperties() {
		Set<ATermAppl> set = new HashSet<ATermAppl>();
		Iterator i = rbox.getRoles().iterator();
		while( i.hasNext() ) {
			Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if( ATermUtils.isPrimitive( p ) && role.isFunctional() )
				set.add( p );
		}
		return set;
	}

	public Set<ATermAppl> getInverseFunctionalProperties() {
		Set<ATermAppl> set = new HashSet<ATermAppl>();
		Iterator i = rbox.getRoles().iterator();
		while( i.hasNext() ) {
			Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if( ATermUtils.isPrimitive( p ) && role.isInverseFunctional() )
				set.add( p );
		}
		return set;
	}

	/**
	 * Return the set of all object properties.
	 * 
	 * @return
	 */
	public Set<ATermAppl> getDataProperties() {
		Set<ATermAppl> set = new HashSet<ATermAppl>();
		Iterator i = rbox.getRoles().iterator();
		while( i.hasNext() ) {
			Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if( ATermUtils.isPrimitive( p ) && role.isDatatypeRole() )
				set.add( p );
		}
		return set;
	}

	/**
	 * Return the set of all individuals. Returned set is unmodifiable!
	 * 
	 * @return
	 */
	public Set<ATermAppl> getIndividuals() {
		return Collections.unmodifiableSet( individuals );
	}

	public Role getProperty(ATerm r) {
		return rbox.getRole( r );
	}

	public int getPropertyType(ATerm r) {
		Role role = getProperty( r );
		return (role == null)
			? Role.UNTYPED
			: role.getType();
	}

	public boolean isClass(ATerm c) {

		if( tbox.getClasses().contains( c ) || c.equals( ATermUtils.TOP ) )
			return true;
		else if( ATermUtils.isComplexClass( c ) ) {
			return fullyDefinedVisitor.isFullyDefined( (ATermAppl) c );
		}
		else
			return false;
	}

	public boolean isProperty(ATerm p) {
		return rbox.isRole( p );
	}

	public boolean isDatatypeProperty(ATerm p) {
		return getPropertyType( p ) == Role.DATATYPE;
	}

	public boolean isObjectProperty(ATerm p) {
		return getPropertyType( p ) == Role.OBJECT;
	}

	public boolean isABoxProperty(ATerm p) {
		int type = getPropertyType( p );
		return (type == Role.OBJECT) || (type == Role.DATATYPE);
	}

	public boolean isAnnotationProperty(ATerm p) {
		return getPropertyType( p ) == Role.ANNOTATION;
	}

	public boolean isOntologyProperty(ATerm p) {
		return getPropertyType( p ) == Role.ONTOLOGY;
	}

	public boolean isIndividual(ATerm ind) {
		return getIndividuals().contains( ind );
	}

	public boolean isTransitiveProperty(ATermAppl r) {
		Role role = getRole( r );

		if( role == null ) {
			handleUndefinedEntity( r + " is not a known property" );
			return false;
		}

		if( role.isTransitive() )
			return true;
		else if( !role.isObjectRole() || role.isFunctional() || role.isInverseFunctional() )
			return false;

		ensureConsistency();

		ATermAppl c = ATermUtils.makeTermAppl( "_C_" );
		ATermAppl notC = ATermUtils.makeNot( c );
		ATermAppl test = ATermUtils.makeAnd( ATermUtils.makeSomeValues( r, ATermUtils
				.makeSomeValues( r, c ) ), ATermUtils.makeAllValues( r, notC ) );

		return !abox.isSatisfiable( test );
	}

	public boolean isSymmetricProperty(ATermAppl p) {
		return isInverse( p, p );
	}

	public boolean isFunctionalProperty(ATermAppl p) {
		Role role = getRole( p );

		if( role == null ) {
			handleUndefinedEntity( p + " is not a known property" );
			return false;
		}

		if( role.isFunctional() )
			return true;
		else if( !role.isSimple() )
			return false;

		ATermAppl min2P = role.isDatatypeRole()
			? ATermUtils.makeMin( p, 2, ATermUtils.TOP_LIT )
			: ATermUtils.makeMin( p, 2, ATermUtils.TOP );
		return !isSatisfiable( min2P );
	}

	public boolean isInverseFunctionalProperty(ATermAppl p) {
		Role role = getRole( p );

		if( role == null ) {
			handleUndefinedEntity( p + " is not a known property" );
			return false;
		}

		if( !role.isObjectRole() )
			return false;
		else if( role.isInverseFunctional() )
			return true;

		ATermAppl invP = role.getInverse().getName();
		ATermAppl max1invP = role.isDatatypeRole()
			? ATermUtils.makeMax( invP, 1, ATermUtils.TOP_LIT )
			: ATermUtils.makeMax( invP, 1, ATermUtils.TOP );
		return isSubClassOf( ATermUtils.TOP, max1invP );
	}

	public boolean isReflexiveProperty(ATermAppl p) {
		Role role = getRole( p );

		if( role == null ) {
			handleUndefinedEntity( p + " is not a known property" );
			return false;
		}

		if( !role.isObjectRole() || role.isIrreflexive() )
			return false;
		else if( role.isReflexive() )
			return true;

		ensureConsistency();

		ATermAppl c = ATermUtils.makeTermAppl( "_C_" );
		ATermAppl notC = ATermUtils.makeNot( c );
		ATermAppl test = ATermUtils.makeAnd( c, ATermUtils.makeAllValues( p, notC ) );

		return !abox.isSatisfiable( test );
	}

	public boolean isIrreflexiveProperty(ATermAppl p) {
		Role role = getRole( p );

		if( role == null ) {
			handleUndefinedEntity( p + " is not a known property" );
			return false;
		}

		if( !role.isObjectRole() || role.isReflexive() )
			return false;
		else if( role.isIrreflexive() || role.isAntisymmetric() )
			return true;

		ensureConsistency();

		ATermAppl test = ATermUtils.makeSelf( p );

		return !abox.isSatisfiable( test );
	}

	public boolean isAntisymmetricProperty(ATermAppl p) {
		Role role = getRole( p );

		if( role == null ) {
			handleUndefinedEntity( p + " is not a known property" );
			return false;
		}

		if( !role.isObjectRole() )
			return false;
		else if( role.isAntisymmetric() )
			return true;

		ensureConsistency();

		ATermAppl o = ATermUtils.makeAnonNominal( "_o_" );
		ATermAppl nom = ATermUtils.makeValue( o );
		ATermAppl test = ATermUtils.makeAnd( nom, ATermUtils.makeSomeValues( p, ATermUtils.makeAnd(
				ATermUtils.makeNot( nom ), ATermUtils.makeSomeValues( p, nom ) ) ) );

		return !abox.isSatisfiable( test );
	}

	public boolean isSubPropertyOf(ATermAppl sub, ATermAppl sup) {
		Role roleSub = rbox.getRole( sub );
		Role roleSup = rbox.getRole( sup );

		if( roleSub == null ) {
			handleUndefinedEntity( sub + " is not a known property" );
			return false;
		}

		if( roleSup == null ) {
			handleUndefinedEntity( sup + " is not a known property" );
			return false;
		}

		if( roleSub.isSubRoleOf( roleSup ) )
			return true;

		ensureConsistency();

		ATermAppl c = ATermUtils.makeTermAppl( "_C_" );
		ATermAppl notC = ATermUtils.makeNot( c );
		ATermAppl test = ATermUtils.makeAnd( ATermUtils.makeSomeValues( sub, c ), ATermUtils
				.makeAllValues( sup, notC ) );

		return !abox.isSatisfiable( test );
	}

	public boolean isEquivalentProperty(ATermAppl p1, ATermAppl p2) {
		return isSubPropertyOf( p1, p2 ) && isSubPropertyOf( p2, p1 );
	}

	public boolean isInverse(ATermAppl r1, ATermAppl r2) {
		Role role1 = getRole( r1 );
		Role role2 = getRole( r2 );

		if( role1 == null ) {
			handleUndefinedEntity( r1 + " is not a known property" );
			return false;
		}

		if( role2 == null ) {
			handleUndefinedEntity( r2 + " is not a known property" );
			return false;
		}

		// the following condition is wrong due to nominals, see OWL test
		// cases SymmetricProperty-002
		// if( !role1.hasNamedInverse() )
		// return false;

		if( !role1.isObjectRole() || !role2.isObjectRole() )
			return false;

		if( role1.getInverse().equals( role2 ) )
			return true;

		ensureConsistency();

		ATermAppl c = ATermUtils.makeTermAppl( "_C_" );
		ATermAppl notC = ATermUtils.makeNot( c );
		ATermAppl test = ATermUtils.makeAnd( c, ATermUtils.makeSomeValues( r1, ATermUtils
				.makeAllValues( r2, notC ) ) );

		return !abox.isSatisfiable( test );
	}

	public boolean hasDomain(ATermAppl p, ATermAppl c) {
		Role r = rbox.getRole( p );
		if( r == null ) {
			handleUndefinedEntity( p + " is not a property!" );
			return false;
		}

		if( !isClass( c ) ) {
			handleUndefinedEntity( c + " is not a valid class expression" );
			return false;
		}

		ATermAppl someP = ATermUtils.makeSomeValues( p, ATermUtils.getTop( r ) );
		return isSubClassOf( someP, c );
	}

	public boolean hasRange(ATermAppl p, ATermAppl c) {
		if( !isClass( c ) && !isDatatype( c ) ) {
			handleUndefinedEntity( c + " is not a valid class expression" );
			return false;
		}
		ATermAppl allValues = ATermUtils.makeAllValues( p, c );
		return isSubClassOf( ATermUtils.TOP, allValues );
	}

	/**
	 * @deprecated Use {@link #isDatatype(ATermAppl)} instead
	 */
	public boolean isDatatype(ATerm p) {
		DatatypeReasoner dtReasoner = getDatatypeReasoner();
		return dtReasoner.isDefined( p.toString() );
	}

	public boolean isDatatype(ATermAppl c) {
		return abox.getDatatypeReasoner().isDefined( c.getName() );
	}

	public boolean isSatisfiable(ATermAppl c) {
		ensureConsistency();

		if( !isClass( c ) ) {
			handleUndefinedEntity( c + " is not a known class!" );
			return false;
		}

		return abox.isSatisfiable( c );
	}

	/**
	 * Returns true if there is at least one named individual that belongs to
	 * the given class
	 * 
	 * @param c
	 * @return
	 */
	public boolean hasInstance(ATerm d) {
		ensureConsistency();

		ATermAppl c = ATermUtils.normalize( (ATermAppl) d );

		List<ATermAppl> unknowns = new ArrayList<ATermAppl>();
		Iterator i = abox.getIndIterator();
		while( i.hasNext() ) {
			ATermAppl x = ((Individual) i.next()).getName();

			Bool knownType = abox.isKnownType( x, c );
			if( knownType.isTrue() )
				return true;
			else if( knownType.isUnknown() )
				unknowns.add( x );
		}

		boolean hasInstance = !unknowns.isEmpty() && abox.isType( unknowns, c );

		return hasInstance;
	}

	public boolean isSubTypeOf(ATermAppl d1, ATermAppl d2) {
		if( !isDatatype( d1 ) ) {
			handleUndefinedEntity( d1 + " is not a known datatype" );
			return false;
		}

		if( !isDatatype( d2 ) ) {
			handleUndefinedEntity( d2 + " is not a known datatype" );
			return false;
		}

		return getDatatypeReasoner().isSubTypeOf( d1, d2 );
	}

	/**
	 * Check if class c1 is subclass of class c2.
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public boolean isSubClassOf(ATermAppl c1, ATermAppl c2) {
		ensureConsistency();

		if( !isClass( c1 ) ) {
			handleUndefinedEntity( c1 + " is not a known class" );
			return false;
		}

		if( !isClass( c2 ) ) {
			handleUndefinedEntity( c2 + " is not a known class" );
			return false;
		}

		if( c1.equals( c2 ) )
			return true;

		// normalize concepts
		c1 = ATermUtils.normalize( c1 );
		c2 = ATermUtils.normalize( c2 );

		if( isClassified() && !doExplanation() ) {
			Bool isSubNode = taxonomy.isSubNodeOf( c1, c2 );
			if( isSubNode.isKnown() )
				return isSubNode.isTrue();
		}

		return abox.isSubClassOf( c1, c2 );
	}

	/**
	 * Check if class c1 is equivalent to class c2.
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public boolean isEquivalentClass(ATermAppl c1, ATermAppl c2) {
		ensureConsistency();

		if( !isClass( c1 ) ) {
			handleUndefinedEntity( c1 + " is not a known class" );
			return false;
		}

		if( !isClass( c2 ) ) {
			handleUndefinedEntity( c2 + " is not a known class" );
			return false;
		}

		if( c1.equals( c2 ) )
			return true;

		// normalize concepts
		c1 = ATermUtils.normalize( c1 );
		c2 = ATermUtils.normalize( c2 );

		if( !doExplanation() ) {
			Bool isEquivalent = Bool.UNKNOWN;
			if( isClassified() )
				isEquivalent = taxonomy.isEquivalent( c1, c2 );

			if( isEquivalent.isKnown() )
				isEquivalent = abox.isKnownSubClassOf( c1, c2 ).and(
						abox.isKnownSubClassOf( c2, c1 ) );

			if( isEquivalent.isKnown() )
				return isEquivalent.isTrue();
		}

		ATermAppl notC2 = ATermUtils.negate( c2 );
		ATermAppl notC1 = ATermUtils.negate( c1 );
		ATermAppl c1NotC2 = ATermUtils.makeAnd( c1, notC2 );
		ATermAppl c2NotC1 = ATermUtils.makeAnd( c2, notC1 );
		ATermAppl test = ATermUtils.makeOr( c1NotC2, c2NotC1 );

		return !isSatisfiable( test );
	}

	public boolean isDisjoint(ATermAppl c1, ATermAppl c2) {
		if( isClass( c1 ) && isClass( c2 ) )
			return isDisjointClass( c1, c2 );
		else if( isProperty( c1 ) && isProperty( c2 ) )
			return isDisjointProperty( c1, c2 );
		else
			return false;
	}

	public boolean isDisjointClass(ATermAppl c1, ATermAppl c2) {
		ATermAppl notC2 = ATermUtils.makeNot( c2 );

		return isSubClassOf( c1, notC2 );
	}

	public boolean isDisjointProperty(ATermAppl r1, ATermAppl r2) {
		Role role1 = getRole( r1 );
		Role role2 = getRole( r2 );

		if( role1 == null ) {
			handleUndefinedEntity( r1 + " is not a known property" );
			return false;
		}

		if( role2 == null ) {
			handleUndefinedEntity( r2 + " is not a known property" );
			return false;
		}

		if( role1.getDisjointRoles().contains( role2 ) )
			return true;

		ensureConsistency();

		ATermAppl o = ATermUtils.makeAnonNominal( "_o_" );
		ATermAppl nom = ATermUtils.makeValue( o );
		ATermAppl test = ATermUtils.makeAnd( ATermUtils.makeSomeValues( r1, nom ), ATermUtils
				.makeSomeValues( r2, nom ) );

		return !abox.isSatisfiable( test );
	}

	public boolean isComplement(ATermAppl c1, ATermAppl c2) {
		ATermAppl notC2 = ATermUtils.makeNot( c2 );

		return isEquivalentClass( c1, notC2 );
	}

	/**
	 * Answers the isType question without doing any satisfiability check. It
	 * might return <code>Bool.TRUE</code>, <code>Bool.FALSE</code>, or
	 * <code>Bool.UNKNOWN</code>. If <code>Bool.UNKNOWN</code> is returned
	 * <code>isType</code> function needs to be called to get the answer.
	 * 
	 * @param x
	 * @param c
	 * @return
	 */
	public Bool isKnownType(ATermAppl x, ATermAppl c) {
		ensureConsistency();

		if( !isIndividual( x ) ) {
			handleUndefinedEntity( x + " is not an individual!" );
			return Bool.FALSE;
		}
		if( !isClass( c ) ) {
			handleUndefinedEntity( c + " is not a valid class expression" );
			return Bool.FALSE;
		}

		c = ATermUtils.normalize( c );

		return abox.isKnownType( x, c );
	}

	public boolean isType(ATermAppl x, ATermAppl c) {
		ensureConsistency();

		if( !isIndividual( x ) ) {
			handleUndefinedEntity( x + " is not an individual!" );
			return false;
		}
		if( !isClass( c ) ) {
			handleUndefinedEntity( c + " is not a valid class expression" );
			return false;
		}

		boolean isType = isRealized() && taxonomy.contains( c )
			? taxonomy.isType( x, c )
			: abox.isType( x, c );
		
		return isType;
	}

	public boolean isSameAs(ATermAppl t1, ATermAppl t2) {
		ensureConsistency();

		if( !isIndividual( t1 ) ) {
			handleUndefinedEntity( t1 + " is not an individual!" );
			return false;
		}
		if( !isIndividual( t2 ) ) {
			handleUndefinedEntity( t2 + " is not an individual!" );
			return false;
		}

		if( t1.equals( t2 ) )
			return true;

		Set knowns = new HashSet();
		Set unknowns = new HashSet();

		Individual ind = abox.getPseudoModel().getIndividual( t1 );
		if( ind.isMerged() && !ind.getMergeDependency( true ).isIndependent() )
			abox.getSames( (Individual) ind.getSame(), unknowns, unknowns );
		else
			abox.getSames( (Individual) ind.getSame(), knowns, unknowns );

		if( knowns.contains( t2 ) )
			return true;
		else if( !unknowns.contains( t2 ) )
			return false;
		else
			return abox.isSameAs( t1, t2 );
	}

	public boolean isDifferentFrom(ATermAppl t1, ATermAppl t2) {
		Individual ind1 = abox.getIndividual( t1 );
		Individual ind2 = abox.getIndividual( t2 );

		if( ind1 == null ) {
			handleUndefinedEntity( t1 + " is not an individual!" );
			return false;
		}

		if( ind2 == null ) {
			handleUndefinedEntity( t2 + " is not an individual!" );
			return false;
		}

		if( ind1.isDifferent( ind2 ) )
			return true;

		ATermAppl c = ATermUtils.makeNot( ATermUtils.makeValue( t2 ) );

		return isType( t1, c );
	}

	public Set<ATermAppl> getDifferents(ATermAppl name) {
		Individual ind = abox.getIndividual( name );

		if( ind == null ) {
			handleUndefinedEntity( name + " is not an individual!" );
			return Collections.emptySet();
		}

		ATermAppl c = ATermUtils.makeNot( ATermUtils.makeValue( name ) );

		return getInstances( c );
	}

	public boolean hasPropertyValue(ATermAppl s, ATermAppl p, ATermAppl o) {
		ensureConsistency();

		if( !isIndividual( s ) ) {
			handleUndefinedEntity( s + " is not an individual!" );
			return false;
		}

		if( !isProperty( p ) ) {
			handleUndefinedEntity( p + " is not a known property!" );
			return false;
		}

		if( o != null ) {
			if( isDatatypeProperty( p ) ) {
				if( !ATermUtils.isLiteral( o ) )
					return false;
			}
			else if( !isIndividual( o ) ) {
				return false;
			}
		}

		return abox.hasPropertyValue( s, p, o );
	}

	/**
	 * Answers the hasPropertyValue question without doing any satisfiability
	 * check. It might return <code>Boolean.TRUE</code>,
	 * <code>Boolean.FALSE</code>, or <code>null</code> (unknown). If the
	 * null value is returned <code>hasPropertyValue</code> function needs to
	 * be called to get the answer.
	 * 
	 * @param s
	 *            Subject
	 * @param p
	 *            Predicate
	 * @param o
	 *            Object (<code>null</code> can be used as wildcard)
	 * @return
	 */
	public Bool hasKnownPropertyValue(ATermAppl s, ATermAppl p, ATermAppl o) {
		ensureConsistency();

		return abox.hasObviousPropertyValue( s, p, o );
	}

	/**
	 * @return Returns the abox.
	 */
	public ABox getABox() {
		return abox;
	}

	/**
	 * @return Returns the rbox.
	 */
	public RBox getRBox() {
		return rbox;
	}

	/**
	 * @return Returns the tbox.
	 */
	public TBox getTBox() {
		return tbox;
	}

	/**
	 * @return Returns the DatatypeReasoner
	 */
	public DatatypeReasoner getDatatypeReasoner() {
		return abox.getDatatypeReasoner();
	}

	/**
	 * Returns the (named) superclasses of class c. Depending on the second
	 * parameter the resulting list will include either all or only the direct
	 * superclasses. A class d is a direct superclass of c iff
	 * <ol>
	 * <li> d is superclass of c </li>
	 * <li> there is no other class x such that x is superclass of c and d is
	 * superclass of x </li>
	 * </ol>
	 * The class c itself is not included in the list but all the other classes
	 * that are sameAs c are put into the list. Also note that the returned list
	 * will always have at least one element. The list will either include one
	 * other concept from the hierarchy or the TOP concept if no other class
	 * subsumes c. By definition TOP concept is superclass of every concept.
	 * <p>
	 * *** This function will first classify the whole ontology ***
	 * </p>
	 * 
	 * @param c
	 *            class whose superclasses are returned
	 * @return A set of sets, where each set in the collection represents an
	 *         equivalence class. The elements of the inner class are ATermAppl
	 *         objects.
	 */
	public Set<Set<ATermAppl>> getSuperClasses(ATermAppl c, boolean direct) {
		c = ATermUtils.normalize( c );

		if( !isClass( c ) ) {
			handleUndefinedEntity( c + " is not a class!" );
			return Collections.emptySet();
		}

		classify();

		if( !taxonomy.contains( c ) )
			builder.classify( c );

		return taxonomy.getSupers( c, direct );
	}

	/**
	 * Returns all the (named) subclasses of class c. The class c itself is not
	 * included in the list but all the other classes that are equivalent to c
	 * are put into the list. Also note that the returned list will always have
	 * at least one element, that is the BOTTOM concept. By definition BOTTOM
	 * concept is subclass of every concept. This function is equivalent to
	 * calling getSubClasses(c, true).
	 * <p>
	 * *** This function will first classify the whole ontology ***
	 * </p>
	 * 
	 * @param c
	 *            class whose subclasses are returned
	 * @return A set of sets, where each set in the collection represents an
	 *         equivalence class. The elements of the inner class are ATermAppl
	 *         objects.
	 */
	public Set<Set<ATermAppl>> getSubClasses(ATermAppl c) {
		return getSubClasses( c, false );
	}

	public Set<Set<ATermAppl>> getDisjoints(ATermAppl c) {
		if( isClass( c ) )
			return getDisjointClasses( c );
		else if( isProperty( c ) )
			return getDisjointProperties( c );
		else
			handleUndefinedEntity( c + " is not a property nor a class!" );
		return Collections.emptySet();
	}

	public Set<Set<ATermAppl>> getDisjointClasses(ATermAppl c) {
		if( !isClass( c ) ) {
			handleUndefinedEntity( c + " is not a class!" );
			return Collections.emptySet();
		}

		ATermAppl notC = ATermUtils.normalize( ATermUtils.makeNot( c ) );
		Set<Set<ATermAppl>> disjoints = getSubClasses( notC );

		if( tbox.getAllClasses().contains( notC ) )
			disjoints.add( getAllEquivalentClasses( notC ) );

		return disjoints;
	}

	public Set<Set<ATermAppl>> getDisjointProperties(ATermAppl p) {
		if( !isProperty( p ) ) {
			handleUndefinedEntity( p + " is not a property!" );
			return Collections.emptySet();
		}

		Role role = rbox.getRole( p );
		Set<Set<ATermAppl>> disjoints = new HashSet<Set<ATermAppl>>();

		for( Iterator i = role.getDisjointRoles().iterator(); i.hasNext(); ) {
			Role disjointRole = (Role) i.next();
			if( !disjointRole.isAnon() )
				disjoints.add( getAllEquivalentProperties( disjointRole.getName() ) );
		}

		return disjoints;
	}

	public Set<ATermAppl> getComplements(ATermAppl c) {
		if( !isClass( c ) ) {
			handleUndefinedEntity( c + " is not a class!" );
			return Collections.emptySet();
		}

		ATermAppl notC = ATermUtils.normalize( ATermUtils.makeNot( c ) );
		Set<ATermAppl> complements = getEquivalentClasses( notC );

		if( tbox.getAllClasses().contains( notC ) )
			complements.add( notC );

		return complements;
	}

	/**
	 * Returns the (named) classes individual belongs to. Depending on the
	 * second parameter the result will include either all types or only the
	 * direct types.
	 * 
	 * @param ind
	 *            An individual name
	 * @param direct
	 *            If true return only the direct types, otherwise return all
	 *            types
	 * @return A set of sets, where each set in the collection represents an
	 *         equivalence class. The elements of the inner class are ATermAppl
	 *         objects.
	 */
	public Set<Set<ATermAppl>> getTypes(ATermAppl ind, boolean direct) {
		if( !isIndividual( ind ) ) {
			handleUndefinedEntity( ind + " is not an individual!" );
			return Collections.emptySet();
		}

		realize();

		return taxonomy.getTypes( ind, direct );
	}

	/**
	 * Get all the (named) classes individual belongs to.
	 * <p>
	 * *** This function will first realize the whole ontology ***
	 * </p>
	 * 
	 * @param ind
	 *            An individual name
	 * @return A set of sets, where each set in the collection represents an
	 *         equivalence class. The elements of the inner class are ATermAppl
	 *         objects.
	 */
	public Set<Set<ATermAppl>> getTypes(ATermAppl ind) {
		if( !isIndividual( ind ) ) {
			handleUndefinedEntity( ind + " is not an individual!" );
			return Collections.emptySet();
		}

		realize();

		return taxonomy.getTypes( ind );
	}

	public ATermAppl getType(ATermAppl ind) {
		if( !isIndividual( ind ) ) {
			handleUndefinedEntity( ind + " is not an individual!" );
			return null;
		}

		// there is always at least one atomic class guranteed to exist (i.e.
		// owl:Thing)
		return (ATermAppl) abox.getIndividual( ind ).getTypes( Node.ATOM ).iterator().next();
	}

	public ATermAppl getType(ATermAppl ind, boolean direct) {
		if( !isIndividual( ind ) ) {
			handleUndefinedEntity( ind + " is not an individual!" );
			return null;
		}

		realize();

		Set setOfSets = taxonomy.getTypes( ind, direct );
		Set set = (Set) setOfSets.iterator().next();
		return (ATermAppl) set.iterator().next();
	}

	/**
	 * Returns all the instances of concept c. If TOP concept is used every
	 * individual in the knowledge base will be returned
	 * 
	 * @param c
	 *            class whose instances are returned
	 * @return A set of ATerm objects
	 */
	public Set<ATermAppl> getInstances(ATermAppl c) {
		if( !isClass( c ) ) {
			handleUndefinedEntity( c + " is not a class!" );
			return Collections.emptySet();
		}

		if( isRealized() && taxonomy.contains( c ) )
			return taxonomy.getInstances( c );

		return new HashSet<ATermAppl>( retrieve( c, individuals ) );
	}

	/**
	 * Returns the instances of class c. Depending on the second parameter the
	 * resulting list will include all or only the direct instances. An
	 * individual x is a direct instance of c iff x is of type c and there is no
	 * subclass d of c such that x is of type d.
	 * <p>
	 * *** This function will first realize the whole ontology ***
	 * </p>
	 * 
	 * @param c
	 *            class whose instances are returned
	 * @param direct
	 *            if true return only the direct instances, otherwise return all
	 *            the instances
	 * @return A set of ATerm objects
	 */
	public Set<ATermAppl> getInstances(ATermAppl c, boolean direct) {
		if( !isClass( c ) ) {
			handleUndefinedEntity( c + " is not a class!" );
			return Collections.emptySet();
		}

		if( !direct )
			return getInstances( c );

		if( ATermUtils.isPrimitive( c ) ) {
			realize();

			return taxonomy.getInstances( c, direct );
		}

		return Collections.emptySet();
	}

	/**
	 * Returns all the classes that are equivalent to class c, excluding c
	 * itself.
	 * <p>
	 * *** This function will first classify the whole ontology ***
	 * </p>
	 * 
	 * @param c
	 *            class whose equivalent classes are found
	 * @return A set of ATerm objects
	 */
	public Set<ATermAppl> getEquivalentClasses(ATermAppl c) {
		c = ATermUtils.normalize( c );

		if( !isClass( c ) ) {
			handleUndefinedEntity( c + " is not a class!" );
			return Collections.emptySet();
		}

		classify();

		if( !taxonomy.contains( c ) )
			builder.classify( c );

		return taxonomy.getEquivalents( c );
	}

	/**
	 * Returns all the classes that are equivalent to class c, including c
	 * itself.
	 * <p>
	 * *** This function will first classify the whole ontology ***
	 * </p>
	 * 
	 * @param c
	 *            class whose equivalent classes are found
	 * @return A set of ATerm objects
	 */
	public Set<ATermAppl> getAllEquivalentClasses(ATermAppl c) {
		c = ATermUtils.normalize( c );

		if( !isClass( c ) ) {
			handleUndefinedEntity( c + " is not a class!" );
			return Collections.emptySet();
		}

		classify();

		if( !taxonomy.contains( c ) )
			builder.classify( c );

		return taxonomy.getAllEquivalents( c );
	}

	/**
	 * Returns all the superclasses (implicitly or explicitly defined) of class
	 * c. The class c itself is not included in the list. but all the other
	 * classes that are sameAs c are put into the list. Also note that the
	 * returned list will always have at least one element, that is TOP concept.
	 * By definition TOP concept is superclass of every concept. This function
	 * is equivalent to calling getSuperClasses(c, true).
	 * <p>
	 * *** This function will first classify the whole ontology ***
	 * </p>
	 * 
	 * @param c
	 *            class whose superclasses are returned
	 * @return A set of sets, where each set in the collection represents an
	 *         equivalence class. The elements of the inner class are ATermAppl
	 *         objects.
	 */
	public Set<Set<ATermAppl>> getSuperClasses(ATermAppl c) {
		return getSuperClasses( c, false );
	}

	/**
	 * Returns the (named) subclasses of class c. Depending onthe second
	 * parameter the result will include either all subclasses or only the
	 * direct subclasses. A class d is a direct subclass of c iff
	 * <ol>
	 * <li>d is subclass of c</li>
	 * <li>there is no other class x different from c and d such that x is
	 * subclass of c and d is subclass of x</li>
	 * </ol>
	 * The class c itself is not included in the list but all the other classes
	 * that are sameAs c are put into the list. Also note that the returned list
	 * will always have at least one element. The list will either include one
	 * other concept from the hierachy or the BOTTOM concept if no other class
	 * is subsumed by c. By definition BOTTOM concept is subclass of every
	 * concept.
	 * <p>
	 * *** This function will first classify the whole ontology ***
	 * </p>
	 * 
	 * @param c
	 *            class whose subclasses are returned
	 * @param direct
	 *            If true return only the direct subclasses, otherwise return
	 *            all the subclasses
	 * @return A set of sets, where each set in the collection represents an
	 *         equivalence class. The elements of the inner class are ATermAppl
	 *         objects.
	 */
	public Set<Set<ATermAppl>> getSubClasses(ATermAppl c, boolean direct) {
		c = ATermUtils.normalize( c );

		if( !isClass( c ) ) {
			handleUndefinedEntity( c + " is not a class!" );
			return Collections.emptySet();
		}

		classify();

		if( !taxonomy.contains( c ) )
			builder.classify( c );

		return taxonomy.getSubs( c, direct );
	}

	/**
	 * Return all the super properties of p.
	 * 
	 * @param prop
	 * @return A set of sets, where each set in the collection represents a set
	 *         of equivalent properties. The elements of the inner class are
	 *         Role objects.
	 */
	public Set<Set<ATermAppl>> getSuperProperties(ATermAppl prop) {
		return getSuperProperties( prop, false );
	}

	/**
	 * Return the super properties of p. Depending on the second parameter the
	 * result will include either all super properties or only the direct super
	 * properties.
	 * 
	 * @param prop
	 * @param direct
	 *            If true return only the direct super properties, otherwise
	 *            return all the super properties
	 * @return A set of sets, where each set in the collection represents a set
	 *         of equivalent properties. The elements of the inner class are
	 *         Role objects.
	 */
	public Set<Set<ATermAppl>> getSuperProperties(ATermAppl prop, boolean direct) {
		prepare();

		return rbox.getTaxonomy().getSupers( prop, direct );
	}

	/**
	 * Return all the sub properties of p.
	 * 
	 * @param prop
	 * @return A set of sets, where each set in the collection represents a set
	 *         of equivalent properties. The elements of the inner class are
	 *         ATermAppl objects.
	 */
	public Set<Set<ATermAppl>> getSubProperties(ATermAppl prop) {
		return getSubProperties( prop, false );
	}

	/**
	 * Return the sub properties of p. Depending on the second parameter the
	 * result will include either all subproperties or only the direct
	 * subproperties.
	 * 
	 * @param prop
	 * @param direct
	 *            If true return only the direct subproperties, otherwise return
	 *            all the subproperties
	 * @return A set of sets, where each set in the collection represents a set
	 *         of equivalent properties. The elements of the inner class are
	 *         ATermAppl objects.
	 */
	public Set<Set<ATermAppl>> getSubProperties(ATermAppl prop, boolean direct) {
		prepare();

		return rbox.getTaxonomy().getSubs( prop, direct );
	}

	/**
	 * Return all the properties that are equivalent to p.
	 * 
	 * @param prop
	 * @return A set of ATermAppl objects.
	 */
	public Set<ATermAppl> getEquivalentProperties(ATermAppl prop) {
		prepare();

		Set<ATermAppl> eqs = rbox.getTaxonomy().getEquivalents( prop );

		return eqs;
	}

	public Set<ATermAppl> getAllEquivalentProperties(ATermAppl prop) {
		prepare();

		Set<ATermAppl> eqs = rbox.getTaxonomy().getAllEquivalents( prop );

		return eqs;
	}

	/**
	 * Return the named inverse property and all its equivalent properties.
	 * 
	 * @param prop
	 * @return
	 */
	public Set<ATermAppl> getInverses(ATerm name) {
		ATermAppl invR = getInverse( name );
		if( invR != null ) {
			Set<ATermAppl> inverses = getAllEquivalentProperties( invR );
			return inverses;
		}

		return Collections.emptySet();
	}

	/**
	 * Returns the inverse of given property. This could possibly be an internal
	 * property created by the reasoner rather than a named property. In case
	 * the given property has more than one inverse any one of them can be
	 * returned.
	 * 
	 * @param name
	 *            Property whose inverse being sought
	 * @return Inverse property or null if given property is not defined or it
	 *         is not an object property
	 */
	public ATermAppl getInverse(ATerm name) {
		Role prop = rbox.getRole( name );
		if( prop == null ) {
			handleUndefinedEntity( name + " is not a property!" );
			return null;
		}

		Role invProp = prop.getInverse();

		return invProp != null
			? invProp.getName()
			: null;
	}

	/**
	 * Return the domain restrictions on the property. The results of this
	 * function is not guaranteed to be complete. Use
	 * {@link #hasDomain(ATermAppl, ATermAppl)} to get complete answers.
	 * 
	 * @param prop
	 * @return
	 */
	public Set<ATermAppl> getDomains(ATermAppl name) {
		ensureConsistency();

		Set<ATermAppl> set = new HashSet<ATermAppl>();
		Role prop = rbox.getRole( name );
		if( prop == null ) {
			handleUndefinedEntity( name + " is not a property!" );
			return Collections.emptySet();
		}

		ATermAppl domain = prop.getDomain();
		if( domain != null ) {
			if( ATermUtils.isAnd( domain ) )
				set = ATermUtils.getPrimitives( (ATermList) domain.getArgument( 0 ) );
			else if( ATermUtils.isPrimitive( domain ) )
				set = Collections.singleton( domain );
		}

		return set;
	}

	/**
	 * Return the domain restrictions on the property. The results of this
	 * function is not guaranteed to be complete. Use
	 * {@link #hasRange(ATermAppl, ATermAppl)} to get complete answers.
	 * 
	 * @param prop
	 * @return
	 */
	public Set<ATermAppl> getRanges(ATerm name) {
		ensureConsistency();

		Set<ATermAppl> set = Collections.emptySet();
		Role prop = rbox.getRole( name );
		if( prop == null ) {
			handleUndefinedEntity( name + " is not a property!" );
			return set;
		}

		ATermAppl range = prop.getRange();
		if( range != null ) {
			if( ATermUtils.isAnd( range ) )
				set = ATermUtils.getPrimitives( (ATermList) range.getArgument( 0 ) );
			else if( ATermUtils.isPrimitive( range ) )
				set = Collections.singleton( range );
		}

		return set;
	}

	/**
	 * Return all the indviduals asserted to be equal to the given individual
	 * inluding the individual itself.
	 * 
	 * @param name
	 * @return
	 */
	public Set<ATermAppl> getAllSames(ATermAppl name) {
		ensureConsistency();

		Set<ATermAppl> knowns = new HashSet<ATermAppl>();
		Set<ATermAppl> unknowns = new HashSet<ATermAppl>();

		Individual ind = abox.getPseudoModel().getIndividual( name );
		if( ind == null ) {
			handleUndefinedEntity( name + " is not an individual!" );
			return Collections.emptySet();
		}

		if( ind.isMerged() && !ind.getMergeDependency( true ).isIndependent() ) {
			knowns.add( name );
			abox.getSames( (Individual) ind.getSame(), unknowns, unknowns );
			unknowns.remove( name );
		}
		else
			abox.getSames( (Individual) ind.getSame(), knowns, unknowns );

		for( Iterator i = unknowns.iterator(); i.hasNext(); ) {
			ATermAppl other = (ATermAppl) i.next();
			if( abox.isSameAs( name, other ) )
				knowns.add( other );
		}

		return knowns;
	}

	/**
	 * Return all the individuals asserted to be equal to the given individual
	 * but not the the individual itself.
	 * 
	 * @param name
	 * @return
	 */
	public Set<ATermAppl> getSames(ATermAppl name) {
		Set<ATermAppl> sames = getAllSames( name );
		sames.remove( name );

		return sames;
	}

	/**
	 * Run the given RDQL query.
	 * 
	 * @deprecated Use QueryEngine.exec methods instead
	 * @param query
	 * @return
	 */
//	public QueryResults runQuery(String queryStr) {
//		return QueryEngine.execRDQL( queryStr, this );
//	}

	/**
	 * Return all literal values for a given dataproperty that belongs to the
	 * specified datatype.
	 * 
	 * @param r
	 * @param x
	 * @param lang
	 * @return List of ATermAppl objects representing literals. These objects
	 *         are in the form literal(value, lang, datatypeURI).
	 */
	public List<ATermAppl> getDataPropertyValues(ATermAppl r, ATermAppl x, Datatype datatype) {
		ensureConsistency();

		Individual ind = abox.getIndividual( x );
		Role role = rbox.getRole( r );

		if( ind == null ) {
			handleUndefinedEntity( x + " is not an individual!" );
			return Collections.emptyList();
		}

		if( role == null || !role.isDatatypeRole() ) {
			handleUndefinedEntity( r + " is not a known data property!" );
			return Collections.emptyList();
		}

		return abox.getDataPropertyValues( x, role, datatype );
	}

	public Set<Role> getPossibleProperties(ATermAppl x) {
		ensureConsistency();

		Individual ind = abox.getIndividual( x );

		if( ind == null ) {
			handleUndefinedEntity( x + " is not an individual!" );
			return Collections.emptySet();
		}

		return abox.getPossibleProperties( x );
	}

	/**
	 * Return all literal values for a given dataproperty that has the specified
	 * language identifier.
	 * 
	 * @param r
	 * @param x
	 * @param lang
	 * @return List of ATermAppl objects.
	 */
	public List<ATermAppl> getDataPropertyValues(ATermAppl r, ATermAppl x, String lang) {
		List<ATermAppl> values = getDataPropertyValues( r, x );
		if( lang == null )
			return values;

		List<ATermAppl> result = new ArrayList<ATermAppl>();
		Iterator i = values.iterator();
		while( i.hasNext() ) {
			ATermAppl lit = (ATermAppl) i.next();
			String litLang = ((ATermAppl) lit.getArgument( 1 )).getName();

			if( litLang.equals( lang ) )
				result.add( lit );
		}

		return result;
	}

	/**
	 * Return all literal values for a given dataproperty and subject value.
	 * 
	 * @param r
	 * @param x
	 * @return List of ATermAppl objects.
	 */
	public List<ATermAppl> getDataPropertyValues(ATermAppl r, ATermAppl x) {
		return getDataPropertyValues( r, x, (Datatype) null );
	}

	/**
	 * Return all property values for a given object property and subject value.
	 * 
	 * @param r
	 * @param x
	 * @return A list of ATermAppl objects
	 */
	public List<ATermAppl> getObjectPropertyValues(ATermAppl r, ATermAppl x) {
		ensureConsistency();

		Role role = rbox.getRole( r );

		if( role == null || !role.isObjectRole() ) {
			handleUndefinedEntity( r + " is not a known object property!" );
			return Collections.emptyList();
		}

		// TODO get rid of unnecessary Set + List creation
		Set<ATermAppl> knowns = new HashSet<ATermAppl>();
		Set<ATermAppl> unknowns = new HashSet<ATermAppl>();

		abox.getObjectPropertyValues( x, role, knowns, unknowns, true );

		if( !unknowns.isEmpty() ) {
			ATermAppl valueX = ATermUtils.makeHasValue( role.getInverse().getName(), x );
			ATermAppl c = ATermUtils.normalize( valueX );

			binaryInstanceRetrieval( c, new ArrayList<ATermAppl>( unknowns ), knowns );
		}

		return new ArrayList<ATermAppl>( knowns );
	}

	/**
	 * Return all property values for a given property and subject value.
	 * 
	 * @param r
	 * @param x
	 * @return List of ATermAppl objects.
	 */
	public List<ATermAppl> getPropertyValues(ATermAppl r, ATermAppl x) {
		Role role = rbox.getRole( r );

		if( role == null ) {
			handleUndefinedEntity( r + " is not a known property!" );
			return Collections.emptyList();
		}

		if( role.isObjectRole() )
			return getObjectPropertyValues( r, x );
		else
			return getDataPropertyValues( r, x );
	}

	/**
	 * List all subjects with a given property and property value.
	 * 
	 * @param r
	 * @param x
	 *            If property is an object property an ATermAppl object that is
	 *            the URI of the individual, if the property is a data property
	 *            an ATerm object that contains the literal value (See {#link
	 *            #getIndividualsWithDataProperty(ATermAppl, ATermAppl)} for
	 *            details)
	 * @return List of ATermAppl objects.
	 */
	public List getIndividualsWithProperty(ATermAppl r, ATermAppl x) {
		Role role = rbox.getRole( r );

		if( role == null ) {
			handleUndefinedEntity( r + " is not a known property!" );
			return Collections.emptyList();
		}

		if( role.isObjectRole() )
			return getIndividualsWithObjectProperty( r, x );
		else
			return getIndividualsWithDataProperty( r, x );
	}

	/**
	 * List all subjects with the given literal value for the specified data
	 * property.
	 * 
	 * @param r
	 *            An ATerm object that contains the literal value in the form
	 *            literal(lexicalValue, langIdentifier, datatypeURI). Should be
	 *            created with ATermUtils.makeXXXLiteral() functions.
	 * @param x
	 * @return List of ATermAppl objects.
	 */
	public List<ATermAppl> getIndividualsWithDataProperty(ATermAppl r, ATermAppl litValue) {
		ensureConsistency();

		Object value = getDatatypeReasoner().getValue( litValue );

		if( value == null ) {
			handleUndefinedEntity( litValue + " is not a valid literal value!" );
			return Collections.emptyList();
		}

		List<ATermAppl> knowns = new ArrayList<ATermAppl>();
		List<ATermAppl> unknowns = new ArrayList<ATermAppl>();

		Iterator i = abox.getIndIterator();
		while( i.hasNext() ) {
			ATermAppl subj = ((Individual) i.next()).getName();

			Bool hasObviousValue = abox.hasObviousDataPropertyValue( subj, r, value );
			if( hasObviousValue.isUnknown() )
				unknowns.add( subj );
			else if( hasObviousValue.isTrue() )
				knowns.add( subj );
		}

		if( !unknowns.isEmpty() ) {
			ATermAppl c = ATermUtils.normalize( ATermUtils.makeHasValue( r, litValue ) );

			binaryInstanceRetrieval( c, unknowns, knowns );
		}

		return knowns;
	}

	/**
	 * List all subjects with the given value for the specified object property.
	 * 
	 * @param r
	 * @param o
	 *            An ATerm object that is the URI of an individual
	 * @return List of ATermAppl objects.
	 */
	public List getIndividualsWithObjectProperty(ATermAppl r, ATermAppl o) {
		ensureConsistency();

		if( !isIndividual( o ) ) {
			handleUndefinedEntity( o + " is not an individual!" );
			return Collections.emptyList();
		}

		Role role = rbox.getRole( r );

		ATermAppl invR = role.getInverse().getName();

		return getObjectPropertyValues( invR, o );
	}

	/**
	 * List all properties asserted between a subject and object.
	 */
	public List<ATermAppl> getProperties(ATermAppl s, ATermAppl o) {
		if( !isIndividual( s ) ) {
			handleUndefinedEntity( s + " is not an individual!" );
			return Collections.emptyList();
		}

		if( !isIndividual( o ) ) {
			handleUndefinedEntity( o + " is not an individual!" );
			return Collections.emptyList();
		}

		List<ATermAppl> props = new ArrayList<ATermAppl>();

		Iterator i = ATermUtils.isLiteral( o )
			? getDataProperties().iterator()
			: getObjectProperties().iterator();
		while( i.hasNext() ) {
			ATermAppl p = (ATermAppl) i.next();
			if( abox.hasPropertyValue( s, p, o ) )
				props.add( p );
		}

		return props;
	}

	public Map<ATermAppl, List<ATermAppl>> getPropertyValues(ATermAppl pred) {
		Map<ATermAppl, List<ATermAppl>> result = new HashMap<ATermAppl, List<ATermAppl>>();

		Iterator subjects = retrieveIndividualsWithProperty( pred ).iterator();
		while( subjects.hasNext() ) {
			ATermAppl subj = (ATermAppl) subjects.next();
			List<ATermAppl> objects = getPropertyValues( pred, subj );
			if( !objects.isEmpty() )
				result.put( subj, objects );
		}

		return result;
	}

	/**
	 * Return all the individuals that belong to the given class which is not
	 * necessarily a named class.
	 * 
	 * @param d
	 * @return
	 */
	public Set<ATermAppl> retrieve(ATermAppl d, Collection<ATermAppl> individuals) {
		ensureConsistency();

		ATermAppl c = ATermUtils.normalize( d );

		if( instances.containsKey( c ) )
			return instances.get( c );
		else if( isRealized() && taxonomy.contains( c ) )
			return getInstances( c );

		Timer timer = timers.startTimer( "retrieve" );

		ATermAppl notC = ATermUtils.negate( c );
		List<ATermAppl> knowns = new ArrayList<ATermAppl>();

		// this is mostly to ensure that a model for notC is cached
		if( !abox.isSatisfiable( notC ) ) {
			// if negation is unsat c itself is TOP
			knowns.addAll( getIndividuals() );
		}
		else if( abox.isSatisfiable( c ) ) {

			List<ATermAppl> unknowns;
			if( isClassified() && taxonomy.contains( c ) ) {
				unknowns = new ArrayList<ATermAppl>();
				Set subs = taxonomy.getSubs( c, false, true );
				subs.remove( ATermUtils.BOTTOM );
				for( ATermAppl x : individuals ) {
					Bool isType = abox.isKnownType( x, c, subs );
					if( isType.isTrue() )
						knowns.add( x );
					else if( isType.isUnknown() )
						unknowns.add( x );
				}
			}
			else
				unknowns = new ArrayList<ATermAppl>( individuals );

			if( !unknowns.isEmpty() && abox.isType( unknowns, c ) ) {
				if( PelletOptions.USE_BINARY_INSTANCE_RETRIEVAL )
					binaryInstanceRetrieval( c, unknowns, knowns );
				else
					linearInstanceRetrieval( c, unknowns, knowns );
			}

		}
		
		timer.stop();

		Set<ATermAppl> result = Collections.unmodifiableSet( new HashSet<ATermAppl>( knowns ) );

		if( PelletOptions.CACHE_RETRIEVAL )
			instances.put( c, result );

		return result;
	}

	// private List filterList(ATermAppl c, List candidates, Collection results)
	// {
	// List filtered = candidates;
	//	    
	// Clash clash = abox.getLastClash();
	// // if the clash is not dependant on a branch and the node is one of the
	// candidates remove it
	// if( clash.depends.isIndependent() && clash.isAtomic() &&
	// clash.args[0].equals(c) ) {
	// int index = candidates.indexOf( clash.node.getName() );
	// if( index >= 0 ) {
	// System.out.println(
	// "Filter obvious instance " + clash.node + " while retrieving " + c );
	// Collections.swap( candidates, index, 0 );
	// results.add( candidates.get( 0 ) );
	// filtered = candidates.subList( 1, candidates.size() );
	// }
	// }
	//	    
	// return filtered;
	// }

	public List<ATermAppl> retrieveIndividualsWithProperty(ATermAppl r) {
		ensureConsistency();

		List<ATermAppl> result = new ArrayList<ATermAppl>();
		Iterator i = abox.getIndIterator();
		while( i.hasNext() ) {
			ATermAppl x = ((Individual) i.next()).getName();

			if( !abox.hasObviousPropertyValue( x, r, null ).isFalse() )
				result.add( x );
		}

		return result;
	}

	public void linearInstanceRetrieval(ATermAppl c, List<ATermAppl> candidates,
			Collection<ATermAppl> results) {
		for( Iterator i = candidates.iterator(); i.hasNext(); ) {
			ATermAppl ind = (ATermAppl) i.next();
			if( abox.isType( ind, c ) )
				results.add( ind );
		}
	}

	public void binaryInstanceRetrieval(ATermAppl c, List<ATermAppl> candidates,
			Collection<ATermAppl> results) {
		if( candidates.isEmpty() )
			return;
		else {
			List<ATermAppl>[] partitions = partition( candidates );
			partitionInstanceRetrieval( c, partitions, results );
		}
	}

	private void partitionInstanceRetrieval(ATermAppl c, List<ATermAppl>[] partitions,
			Collection<ATermAppl> results) {
		if( partitions[0].size() == 1 ) {
			ATermAppl i = partitions[0].get( 0 );
			binaryInstanceRetrieval( c, partitions[1], results );

			if( abox.isType( i, c ) )
				results.add( i );
		}
		else if( !abox.isType( partitions[0], c ) ) {
			binaryInstanceRetrieval( c, partitions[1], results );
		}
		else {
			// partitions[0] = filterList(c, partitions[0], results);
			if( !abox.isType( partitions[1], c ) ) {
				binaryInstanceRetrieval( c, partitions[0], results );
			}
			else {
				// partitions[1] = filterList(c, partitions[1], results);
				binaryInstanceRetrieval( c, partitions[0], results );
				binaryInstanceRetrieval( c, partitions[1], results );
			}
		}
	}

	@SuppressWarnings("unchecked")
	private List<ATermAppl>[] partition(List<ATermAppl> candidates) {
		List<ATermAppl>[] partitions = new List[2];
		int n = candidates.size();
		if( n <= 1 ) {
			partitions[0] = candidates;
			partitions[1] = new ArrayList<ATermAppl>();
		}
		else {
			partitions[0] = candidates.subList( 0, n / 2 );
			partitions[1] = candidates.subList( n / 2, n );
		}

		return partitions;
	}

	// private List binarySubClassRetrieval(ATermAppl c, List candidates) {
	// if(candidates.isEmpty())
	// return new ArrayList();
	// else{
	// List[] partitions = partition(candidates);
	// return partitionSubClassRetrieval(c, partitions);
	// }
	// }
	//	
	// private List partitionSubClassRetrieval(ATermAppl c, List[] partitions) {
	// if(partitions[0].size() == 1) {
	// ATermAppl d = (ATermAppl) partitions[0].get(0);
	// List l = binarySubClassRetrieval(c, partitions[1]);
	//
	// if(isSubclassOf(d, c))
	// l.add(d);
	//			
	// return l;
	// }
	// else if(!abox.isSubClassOf(partitions[0], c))
	// return binarySubClassRetrieval(c, partitions[1]);
	// else if(!abox.isSubClassOf(partitions[1], c))
	// return binarySubClassRetrieval(c, partitions[0]);
	// else {
	// List l1 = binarySubClassRetrieval(c, partitions[0]);
	// List l2 = binarySubClassRetrieval(c, partitions[1]);
	//			
	// l1.addAll(l2);
	//			
	// return l1;
	// }
	// }

	/**
	 * Print the class hierarchy on the standard output.
	 */
	public void printClassTree() {
		classify();

		taxonomy.print();
	}

	public void printClassTree(OutputFormatter out) {
		classify();

		taxonomy.print( out );
	}

	public boolean doExplanation() {
		return abox.doExplanation();
	}

	/**
	 * @param doExplanation
	 *            The doExplanation to set.
	 */
	public void setDoExplanation(boolean doExplanation) {
		abox.setDoExplanation( doExplanation );
	}

	public String getExplanation() {
		return abox.getExplanation();
	}

	/**
	 * @deprecated Use setDoExplanation instead
	 */
	public void setDoDependencyAxioms(boolean doDepAxioms) {
		if( log.isDebugEnabled() )
			log.debug( "Setting DoDependencyAxioms = " + doDepAxioms );
	}

	/**
	 * @deprecated Use getExplanation instead
	 */
	public boolean getDoDependencyAxioms() {
		return false;
	}

	public Set<ATermAppl> getExplanationSet() {
		return abox.getExplanationSet();
	}

	/**
	 * @param rbox
	 *            The rbox to set.
	 */
	public void setRBox(RBox rbox) {
		this.rbox = rbox;
	}

	/**
	 * @param tbox
	 *            The tbox to set.
	 */
	public void setTBox(TBox tbox) {
		this.tbox = tbox;
	}

	CompletionStrategy chooseStrategy(ABox abox) {
		return chooseStrategy( abox, getExpressivity() );
	}

	/**
	 * Choose a completion strategy based on the expressivity of the KB. The
	 * abox given is not necessarily the ABox that belongs to this KB but can be
	 * a derivative.
	 * 
	 * @return
	 */
	CompletionStrategy chooseStrategy(ABox abox, Expressivity expressivity) {
		// if there are dl-safe rules present, use RuleStrategy which is a
		// subclass of SHOIN
		// only problem is, we're using SHOIN everytime there are rule- it is
		// faster to use SHN + Rules in some cases
		if( this.getRules() != null ) {
			return new RuleStrategy( abox );
		}
		if( PelletOptions.DEFAULT_COMPLETION_STRATEGY != null ) {
			Class[] types = new Class[] { ABox.class };
			Object[] args = new Object[] { abox };
			try {
				Constructor cons = PelletOptions.DEFAULT_COMPLETION_STRATEGY.getConstructor( types );
				return (CompletionStrategy) cons.newInstance( args );
			} catch( Exception e ) {
				e.printStackTrace();
				throw new InternalReasonerException(
						"Failed to create the default completion strategy defined in PelletOptions!" );
			}
		}
		else if( PelletOptions.USE_COMPLETION_STRATEGY ) {
			boolean emptyStrategy = (abox.size() == 1)
					&& ((Individual) abox.getIndIterator().next()).getOutEdges().isEmpty();

			boolean fullDatatypeReasoning = PelletOptions.USE_FULL_DATATYPE_REASONING
					&& (expressivity.hasCardinalityD() || expressivity.hasKeys());

			if( !fullDatatypeReasoning ) {
				if( expressivity.hasCardinalityQ() || expressivity.hasComplexSubRoles() )
					return new SHOIQStrategy( abox );
				else if( expressivity.hasNominal() ) {
					if( expressivity.hasInverse() )
						return new SHOINStrategy( abox );
					else
						return new SHONStrategy( abox );
				}
				else if( expressivity.hasInverse() )
					return new SHINStrategy( abox );
				else if( emptyStrategy && !expressivity.hasCardinalityD()
						&& !expressivity.hasKeys() )
					return new EmptySHNStrategy( abox );
				else
					return new SHNStrategy( abox );
			}
		}

		return new SHOIQStrategy( abox );
	}

	/**
	 * @deprecated Not used any more
	 */
	public String getOntology() {
		return "";
	}

	/**
	 * @deprecated Not used any more
	 */
	public void setOntology(String ontology) {
		// noop
	}

	/**
	 * Set a timeout for the main timer. Used to stop an automated test after a
	 * reasonable amount of time has passed.
	 * 
	 * @param timeout
	 */
	public void setTimeout(long timeout) {
		timers.mainTimer.setTimeout( timeout );
	}

	/**
	 * @param term
	 * @return
	 */
	Role getRole(ATerm term) {
		return rbox.getRole( term );
	}

	/**
	 * Get the classification results.
	 */
	public Taxonomy getTaxonomy() {
		classify();

		return taxonomy;
	}

	public TaxonomyBuilder getTaxonomyBuilder() {
		if( builder == null ) {
			builder = new CDOptimizedTaxonomyBuilder();
			builder.setKB( this );
		}

		return builder;
	}

	public Taxonomy getRoleTaxonomy() {
		prepare();

		return rbox.getTaxonomy();
	}

	public SizeEstimate getSizeEstimate() {
		return estimate;
	}

	public void addRule(Rule rule) {
		// DL-safe rules affects the ABox so we might redo the reasoning
		status |= ABOX_CHANGED;

		if( rules == null )
			rules = new HashSet<Rule>();

		rules.add( rule );

		if( log.isDebugEnabled() )
			log.debug( "rule " + rule );
	}

	public void setRules(Set<Rule> rules) {
		// DL-safe rules affects the ABox so we might redo the reasoning
		status |= ABOX_CHANGED;

		this.rules = rules;
	}

	public Set getRules() {
		return this.rules;
	}

	public void removeIndividual(ATermAppl c) {
		// Flag so that we can not use inc. reasoning - currently this is not
		// supported
		aboxDeletion = true;
		aboxAddition = true;

		abox.removeIndividual( c );
		individuals.remove( c );
	}

	/**
	 * Check if we can use incremental consistency checking
	 * 
	 * @return
	 */
	protected boolean canUseIncConsistency() {
		// can we do incremental consistency checking
		if( expressivity == null )
			return false;

		boolean canUseIncConsistency = (!(expressivity.hasNominal() && expressivity.hasInverse()))
				&&
				// !expressivity.hasAdvancedRoles() &&
				// !expressivity.hasCardinalityQ() &&
				!isTBoxChanged() && !isRBoxChanged() && (abox.getPseudoModel() != null)
				&& PelletOptions.USE_INCREMENTAL_CONSISTENCY &&
				// support additions only; also support deletions with or with
				// additions, however tracing must be on to support incremental
				// deletions
				((!aboxDeletion && aboxAddition) || (aboxDeletion
						&& PelletOptions.USE_INCREMENTAL_DELETION && (aboxAddition || !aboxAddition)));

		return canUseIncConsistency;
	}

	/**
	 * Method to remove all stuctures dependent on an ABox assertion from the
	 * abox. This is used for incremental reasoning under ABox deletions.
	 * 
	 * @param ATermAppl
	 *            assertion The deleted assertion
	 */
	protected void restoreDependencies() {

		// iterate over all removed assertions
		for( Iterator it = deletedAssertions.iterator(); it.hasNext(); ) {
			// get next assertion
			ATermAppl next = (ATermAppl) it.next();

			// get the dependency entry
			DependencyEntry entry = dependencyIndex.getDependencies( next );

			if( entry != null ) {
				if( DependencyIndex.log.isDebugEnabled() )
					DependencyIndex.log.debug( "Restoring dependencies for " + next );

				// restore the entry
				restoreDependency( next, entry );
			}

			// remove the entry in the index for this assertion
			dependencyIndex.removeDependencies( next );
		}

	}

	/**
	 * Get the dependency index for syntactic assertions in this kb
	 * 
	 * @return
	 */
	protected DependencyIndex getDependencyIndex() {
		return dependencyIndex;
	}

	/**
	 * Get syntactic assertions in the kb
	 * 
	 * @return
	 */
	public Set<ATermAppl> getSyntacticAssertions() {
		return syntacticAssertions;
	}

	/**
	 * Perform the actual rollback of a depenedency entry
	 * 
	 * @param assertion
	 * @param entry
	 */
	private void restoreDependency(ATermAppl assertion, DependencyEntry entry) {

		if( DependencyIndex.log.isDebugEnabled() )
			DependencyIndex.log.debug( "  Restoring Edge Dependencies:" );
		for( Iterator it = entry.getEdges().iterator(); it.hasNext(); ) {
			Edge next = (Edge) it.next();
			restoreEdge( assertion, next );
		}

		if( DependencyIndex.log.isDebugEnabled() )
			DependencyIndex.log.debug( "  Restoring Type Dependencies:" );
		for( Iterator it = entry.getTypes().iterator(); it.hasNext(); ) {
			TypeDependency next = (TypeDependency) it.next();
			restoreType( assertion, next );
		}

		if( DependencyIndex.log.isDebugEnabled() )
			DependencyIndex.log.debug( "  Restoring Merge Dependencies: " + entry.getMerges() );
		for( Iterator it = entry.getMerges().iterator(); it.hasNext(); ) {
			MergeDependency next = (MergeDependency) it.next();
			restoreMerge( assertion, next );
		}

		if( DependencyIndex.log.isDebugEnabled() )
			DependencyIndex.log.debug( "  Restoring Branch Add Dependencies: "
					+ entry.getBranchAdds() );
		for( Iterator it = entry.getBranchAdds().iterator(); it.hasNext(); ) {
			BranchAddDependency next = (BranchAddDependency) it.next();
			restoreBranchAdd( assertion, next );
		}

		if( DependencyIndex.log.isDebugEnabled() )
			DependencyIndex.log.debug( "  Restoring Branch Remove DS Dependencies: "
					+ entry.getBranchAdds() );
		for( Iterator it = entry.getCloseBranches().iterator(); it.hasNext(); ) {
			CloseBranchDependency next = (CloseBranchDependency) it.next();
			restoreCloseBranch( assertion, next );
		}

		if( DependencyIndex.log.isDebugEnabled() )
			DependencyIndex.log.debug( "  Restoring clash dependency: " + entry.getClash() );
		if( entry.getClash() != null ) {
			restoreClash( assertion, entry.getClash() );
		}

	}

	/**
	 * Restore an edge - i.e., remove it
	 * 
	 * @param assertion
	 * @param edge
	 */
	private void restoreEdge(ATermAppl assertion, Edge theEdge) {
		if( DependencyIndex.log.isDebugEnabled() )
			DependencyIndex.log.debug( "    Removing edge? " + theEdge );

		// the edge could have previously been removed so return
		if( theEdge == null )
			return;

		// get the object
		Individual subj = abox.getPseudoModel().getIndividual( theEdge.getFrom().getName() );
		Node obj = abox.getPseudoModel().getNode( theEdge.getTo().getName() );
		Role role = getRole( theEdge.getRole().getName() );

		// loop over all edges for the subject
		EdgeList edges = subj.getEdgesTo( obj, role );
		for( int i = 0; i < edges.size(); i++ ) {
			Edge edge = edges.edgeAt( i );
			if( edge.getRole().equals( role ) ) {
				// get dependency set for the edge
				DependencySet ds = edge.getDepends();

				// clean it
				ds.removeExplain( assertion );

				// remove if the dependency set is empty
				if( ds.explain.isEmpty() ) {
					// need to check if the
					subj.removeEdge( edge );

					// add to updated individuals
					abox.updatedIndividuals.add( subj );

					// TODO: Do we need to add literals?
					if( obj instanceof Individual )
						abox.updatedIndividuals.add( (Individual) obj );

					if( DependencyIndex.log.isDebugEnabled() )
						DependencyIndex.log.debug( "           Actually removed edge!" );
				}
				break;
			}
		}
	}

	/**
	 * Restore a type dependency
	 * 
	 * @param assertion
	 * @param type
	 */
	private void restoreType(ATermAppl assertion, TypeDependency type) {
		if( DependencyIndex.log.isDebugEnabled() ) {
			if( abox.getPseudoModel().getNode( type.getInd() ) instanceof Individual )
				DependencyIndex.log.debug( "    Removing type? " + type.getType() + " from "
						+ abox.getPseudoModel().getIndividual( type.getInd() ).debugString() );
			else
				DependencyIndex.log.debug( "    Removing type? " + type.getType() + " from "
						+ abox.getPseudoModel().getNode( type.getInd() ) );
		}

		// get the dependency set - Note: we must normalize the concept
		DependencySet ds = (DependencySet) abox.getPseudoModel().getNode( type.getInd() ).depends
				.get( ATermUtils.normalize( type.getType() ) );

		// return if null - this can happen as currently I have dupilicates in
		// the index
		if( ds == null || type.getType() == ATermUtils.TOP )
			return;

		// clean it
		ds.removeExplain( assertion );

		// remove if the explanation set is empty
		if( ds.explain.isEmpty() ) {
			abox.getPseudoModel().removeType( type.getInd(), type.getType() );

			// add to updated individuals
			if( abox.getPseudoModel().getNode( type.getInd() ) instanceof Individual ) {
				Individual ind = abox.getPseudoModel().getIndividual( type.getInd() );
				abox.updatedIndividuals.add( ind );

				// also need to add all edge object to updated individuals -
				// this is needed to fire allValues/domain/range rules etc.
				for( Iterator it = ind.getInEdges().iterator(); it.hasNext(); ) {
					Edge e = (Edge) it.next();
					abox.updatedIndividuals.add( e.getFrom() );
				}
				for( Iterator it = ind.getOutEdges().iterator(); it.hasNext(); ) {
					Edge e = (Edge) it.next();
					if( e.getTo() instanceof Individual )
						abox.updatedIndividuals.add( (Individual) e.getTo() );
				}
			}

			if( DependencyIndex.log.isDebugEnabled() )
				DependencyIndex.log.debug( "           Actually removed type!" );
		}
	}

	/**
	 * Restore a merge dependency
	 * 
	 * @param assertion
	 * @param merge
	 */
	private void restoreMerge(ATermAppl assertion, MergeDependency merge) {

		if( DependencyIndex.log.isDebugEnabled() )
			DependencyIndex.log.debug( "    Removing merge? " + merge.getInd() + " merged to "
					+ merge.getmergedIntoInd() );

		// get merge dependency
		DependencySet ds = abox.getPseudoModel().getNode( merge.getInd() ).mergeDepends;

		// remove the dependency
		ds.removeExplain( assertion );

		// undo merge if empty
		if( ds.explain.isEmpty() ) {
			if( DependencyIndex.log.isDebugEnabled() )
				DependencyIndex.log.debug( "           Actually removing merge!" );

			// get nodes
			Node ind = abox.getPseudoModel().getNode( merge.getInd() );
			Node mergedToInd = abox.getPseudoModel().getNode( merge.getmergedIntoInd() );

			// check that they are actually the same - else throw error
			if( !ind.isSame( mergedToInd ) )
				throw new InternalReasonerException( " Restore merge error: " + ind
						+ " not same as " + mergedToInd );

			if( !ind.isPruned() )
				throw new InternalReasonerException( " Restore merge error: " + ind + " not pruned" );

			// unprune to prune branch
			ind.unprune( ind.pruned.branch );

			// undo set same
			ind.undoSetSame();

			// add to updated
			// Note that ind.unprune may add edges, however we do not need to
			// add them to the updated individuals as
			// they will be added when the edge is removed from the node which
			// this individual was merged to
			if( ind instanceof Individual )
				abox.updatedIndividuals.add( (Individual) ind );
		}
	}

	/**
	 * Restore a branch add dependency
	 * 
	 * @param assertion
	 * @param branch
	 */
	@SuppressWarnings("unchecked")
	private void restoreBranchAdd(ATermAppl assertion, BranchAddDependency branch) {
		if( DependencyIndex.log.isDebugEnabled() )
			DependencyIndex.log.debug( "    Removing branch add? " + branch.getBranch() );

		// get merge dependency
		DependencySet ds = branch.getBranch().termDepends;

		// remove the dependency
		ds.removeExplain( assertion );

		// undo merge if empty
		if( ds.explain.isEmpty() ) {
			if( DependencyIndex.log.isDebugEnabled() )
				DependencyIndex.log.debug( "           Actually removing branch!" );

			// !!!!!!!!!!!!!!!! First update completion queue branch effects
			// !!!!!!!!!!!!!!
			// need to update the completion queue
			// currently i track all nodes that are effected during the
			// expansion rules for a given branch
			List<Node> brEffects = abox.getPseudoModel().completionQueue.branchEffects;
			Set allEffects = new HashSet<Node>();
			for( int i = branch.getBranch().branch; i < brEffects.size(); i++ ) {
				allEffects.addAll( (Set) brEffects.get( i ) );
			}

			for( Iterator it = allEffects.iterator(); it.hasNext(); ) {
				ATermAppl nextATerm = (ATermAppl) it.next();

				// get the actual node
				Node node = abox.getPseudoModel().getNode( nextATerm );

				// update type dependencies
				Set types = node.getTypes();
				for( Iterator tIt = types.iterator(); tIt.hasNext(); ) {
					// get next type
					ATermAppl type = (ATermAppl) tIt.next();

					// get ds for type
					DependencySet tDS = node.getDepends( type );

					// update branch if necessary
					if( tDS.branch > branch.getBranch().branch )
						tDS.branch--;

					for( int i = branch.getBranch().branch; i <= abox.getPseudoModel()
							.getBranches().size(); i++ ) {
						// update dependency set
						if( tDS.contains( i ) ) {
							tDS.remove( i );
							tDS.add( i - 1 );
						}
					}
				}

				// update edge depdencies
				EdgeList edges = node.getInEdges();
				for( Iterator eIt = edges.iterator(); eIt.hasNext(); ) {
					// get next type
					Edge edge = (Edge) eIt.next();

					// update branch if necessary
					if( edge.getDepends().branch > branch.getBranch().branch )
						edge.getDepends().branch--;

					for( int i = branch.getBranch().branch; i <= abox.getPseudoModel()
							.getBranches().size(); i++ ) {
						// update dependency set
						if( edge.getDepends().contains( i ) ) {
							edge.getDepends().remove( i );
							edge.getDepends().add( i - 1 );
						}
					}
				}

				// //TODO:The following code update outedges as well - after
				// testing is seems that this is un-necessary
				// if(node instanceof Individual){
				// Individual ind = (Individual)node;
				//					
				// //update edge depdencies
				// //update type dependencies
				// edges = ind.getInEdges();
				// for(Iterator eIt = edges.iterator(); eIt.hasNext();){
				// //get next type
				// Edge edge = (Edge)eIt.next();
				//						
				// //update branch if necessary
				// if(edge.getDepends().branch > branch.getBranch().branch)
				// edge.getDepends().branch--;
				//
				// for(int i = branch.getBranch().branch; i <=
				// abox.getPseudoModel().getBranches().size(); i++){
				// //update dependency set
				// if(edge.getDepends().contains(i)){
				// edge.getDepends().remove(i);
				// edge.getDepends().add(i-1);
				// }
				// }
				// }
				// }
			}

			// remove the deleted branch from branch effects
			abox.getPseudoModel().completionQueue.branchEffects.remove( branch.getBranch().branch );

			// !!!!!!!!!!!!!!!! Next update abox branches !!!!!!!!!!!!!!
			// remove the branch from branches
			List branches = abox.getPseudoModel().getBranches();

			// decrease branch id for each branch after the branch we're
			// removing
			// also need to change the dependency set for each label
			for( int i = branch.getBranch().branch; i < branches.size(); i++ ) {
				// cast for ease
				Branch br = ((Branch) branches.get( i ));

				// update the term depends in the branch
				if( br.termDepends.branch > branch.getBranch().branch )
					br.termDepends.branch--;

				for( int j = branch.getBranch().branch; j < abox.getPseudoModel().getBranches()
						.size(); j++ ) {
					if( br.termDepends.contains( j ) ) {
						br.termDepends.remove( j );
						br.termDepends.add( j - 1 );
					}
				}

				// also need to decrement the branch number
				br.branch--;
			}

			// remove the actual branch
			branches.remove( branch.getBranch() );

			// set the branch counter
			abox.getPseudoModel().setBranch( abox.getPseudoModel().getBranch() - 1 );
		}
	}

	/**
	 * Restore a disjunct, merge pairs, etc. of a branch that has been closed
	 * due to a clash whose dependency set contains an assertion that has been
	 * deleted
	 * 
	 * @param assertion
	 * @param branch
	 */
	private void restoreCloseBranch(ATermAppl assertion, CloseBranchDependency branch) {
		// only proceed if tryNext is larger than 1!
		if( branch.getTheBranch().tryNext > -1 ) {
			if( DependencyIndex.log.isDebugEnabled() )
				DependencyIndex.log.debug( "    Undoing branch remove - branch "
						+ branch.getBranch() + "  -  " + branch.getInd() + "   tryNext: "
						+ branch.getTryNext() );

			// shift try next for branch
			branch.getTheBranch().shiftTryNext( branch.getTryNext() );
		}
	}

	/**
	 * Restore a clash dependency
	 * 
	 * @param assertion
	 * @param clash
	 */
	private void restoreClash(ATermAppl assertion, ClashDependency clash) {

		if( DependencyIndex.log.isDebugEnabled() )
			DependencyIndex.log.debug( "    Restoring clash dependency clash: " + clash.getClash() );

		// remove the dependency
		clash.getClash().depends.removeExplain( assertion );

		// undo clash if empty and is independent
		if( clash.getClash().depends.explain.isEmpty() && clash.getClash().depends.isIndependent() ) {
			if( DependencyIndex.log.isDebugEnabled() )
				DependencyIndex.log.debug( "           Actually removing clash!" );

			abox.getPseudoModel().setClash( null );
		}
	}

	protected static void handleUndefinedEntity(String s) {
		if( !PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
			throw new UndefinedEntityException( s );
	}
	
	public Set<ATermAppl> getABoxAssertions( AssertionType assertionType ) {
		Set<ATermAppl> assertions = aboxAssertions.get( assertionType );
		
		if( assertions == null )
			return Collections.emptySet();
		else
			return Collections.unmodifiableSet( assertions );
	}

	/**
	 * @deprecated Use {@link #getABoxAssertions(org.mindswap.pellet.KnowledgeBase.AssertionType)} instead
	 */
	public Set<ATermAppl> getAboxMembershipAssertions() {
		return getABoxAssertions( AssertionType.TYPE );
	}
	
	/**
	 * @deprecated Use {@link #getABoxAssertions(org.mindswap.pellet.KnowledgeBase.AssertionType)} instead
	 */
	public Set<ATermAppl> getAboxObjectRoleAssertions() {
		return getABoxAssertions( AssertionType.OBJ_ROLE );
	}
	
	/**
	 * @deprecated Use {@link #getABoxAssertions(org.mindswap.pellet.KnowledgeBase.AssertionType)} instead
	 */
	public Set<ATermAppl> getAboxDataRoleAssertions() {
		return getABoxAssertions( AssertionType.DATA_ROLE );
	}

}
