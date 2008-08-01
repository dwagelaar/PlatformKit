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

package org.mindswap.pellet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mindswap.pellet.CachedNode.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.datatypes.AtomicDatatype;
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.datatypes.DatatypeReasoner;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.tbox.TBox;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Bool;
import org.mindswap.pellet.utils.CandidateSet;
import org.mindswap.pellet.utils.MultiListIterator;
import org.mindswap.pellet.utils.Namespaces;
import org.mindswap.pellet.utils.Pair;
import org.mindswap.pellet.utils.SetUtils;
import org.mindswap.pellet.utils.Timer;
import org.mindswap.pellet.utils.fsm.State;
import org.mindswap.pellet.utils.fsm.Transition;
import org.mindswap.pellet.utils.fsm.TransitionGraph;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;

/**
 * @author Evren Sirin
 */
public class ABox {
	public final static Log		log					= LogFactory.getLog( ABox.class );

	/**
	 * @deprecated Use log4j.properties instead
	 */
	public static boolean		DEBUG				= false;

	// following two variables are used to generate names
	// for newly generated individuals. so during rules are
	// applied anon1, anon2, etc. will be generated. This prefix
	// will also make sure that any node whose name starts with
	// this prefix is not a root node
	protected int				anonCount			= 0;

	/**
	 * Total number of satisfiability tests performed (for statistical purposes)
	 */
	public long					satisfiabilityCount	= 0;

	/**
	 * Total number of ABox consistency checks (for statistical purposes)
	 */
	public long					consistencyCount	= 0;

	/**
	 * size of the completion tree for statistical purposes
	 */
	public int					treeDepth			= 0;

	/**
	 * datatype reasoner used for checking the satisfiability of datatypes
	 */
	protected DatatypeReasoner	dtReasoner;

	/**
	 * This is a list of nodes. Each node has a name expressed as an ATerm which
	 * is used as the key in the Hashtable. The value is the actual node object
	 */
	protected Map				nodes;

	/**
	 * This is a list of node names. This list stores the individuals in the
	 * order they are created
	 */
	protected List				nodeList;

	/**
	 * Indicates if any of the completion rules has been applied to modify ABox
	 */
	boolean						changed				= false;

	private boolean				doExplanation;

	// cached satisfiability results
	// the table maps every atomic concept A (and also its negation not(A))
	// to the root node of its completed tree. If a concept is mapped to
	// null value it means it is not satisfiable
	protected ConceptCache		cache;

	// pseudo model for this Abox. This is the ABox that results from
	// completing to the original Abox
	private ABox				pseudoModel;

	// cache of the last completion. it may be different from the pseudo
	// model, e.g. type checking for individual adds one extra assertion
	// last completion is stored for caching the root nodes that was
	// the result of
	private ABox				lastCompletion;
	private boolean				keepLastCompletion;
	private Clash				lastClash;

	HashMap						allBindings			= new HashMap();

	// complete ABox means no more tableau rules are applicable
	private boolean				isComplete			= false;

	// the last clash recorded
	private Clash				clash;

	// the current branch number
	private int					branch;
	private List				branches;

	List						toBeMerged;

	Map							disjBranchStats;

	// if we are using copy on write, this is where to copy from
	ABox						sourceABox;

	Map							typeAssertions		= new HashMap();

	// return true if init() function is called. This indicates parsing
	// is completed and ABox is ready for completion
	private boolean				initialized			= false;

	// The KB to which this ABox belongs
	private KnowledgeBase		kb;

	public boolean				rulesNotApplied;

	public boolean				ranRete				= false;
	public boolean				useRete				= false;

	public Map					stats;

	// THE FOLLOWING ARE USED FOR INCREMENTAL REASONING
	// Completion queue for application of completion rules
	protected CompletionQueue	completionQueue;

	// Individuals updated
	protected Set<Individual>	updatedIndividuals;
	
	// New individuals added - used for adding UC to new individuals
	protected Set<Individual>	newIndividuals;
	

	// flag set when incrementally updating the abox with explicit assertions
	private boolean				syntacticUpdate		= false;

	public ABox() {
		this( new KnowledgeBase() );
	}

	public ABox(KnowledgeBase kb) {
		this.kb = kb;
		nodes = new HashMap();
		nodeList = new ArrayList();
		clash = null;
		doExplanation = false;
		dtReasoner = new DatatypeReasoner();
		keepLastCompletion = false;

		clearCaches( true );

		branch = 0;
		branches = new ArrayList();
		disjBranchStats = new HashMap();

		toBeMerged = new ArrayList();
		rulesNotApplied = true;

		completionQueue = new CompletionQueue( this );
		updatedIndividuals = new HashSet<Individual>();
		newIndividuals = new HashSet<Individual>();
	}

	public ABox(ABox abox) {
		this( abox, null, true );
	}

	public ABox(ABox abox, ATermAppl extraIndividual, boolean copyIndividuals) {
		this.kb = abox.kb;
		Timer timer = kb.timers.startTimer( "cloneABox" );

		completionQueue = new CompletionQueue( this );
		this.rulesNotApplied = true;
		initialized = abox.initialized;
		changed = abox.changed;
		anonCount = abox.anonCount;
		cache = abox.cache;
		clash = abox.clash;
		dtReasoner = abox.dtReasoner;
		doExplanation = abox.doExplanation;
		disjBranchStats = abox.disjBranchStats;

		int extra = (extraIndividual == null)
			? 0
			: 1;
		int nodeCount = extra + (copyIndividuals
			? abox.nodes.size()
			: 0);

		nodes = new HashMap( nodeCount );
		nodeList = new ArrayList( nodeCount );

		// copy the queue - this must be done early so that the effects of
		// adding the extra individual do not get removed
		if( PelletOptions.USE_COMPLETION_QUEUE ) {
			this.completionQueue = abox.completionQueue.copy();
			completionQueue.setABox( this );

			if( PelletOptions.USE_INCREMENTAL_CONSISTENCY ) {
				this.updatedIndividuals = new HashSet<Individual>();
				if( abox.updatedIndividuals != null )
					this.updatedIndividuals.addAll( abox.updatedIndividuals );
				
				this.newIndividuals = new HashSet<Individual>();
				if( abox.newIndividuals != null )
					this.newIndividuals.addAll( abox.newIndividuals );

			}
		}

		if( extraIndividual != null ) {
			Individual n = new Individual( extraIndividual, this, Individual.BLOCKABLE );

			n.setConceptRoot( true );
			n.branch = DependencySet.NO_BRANCH;
			n.addType( ATermUtils.TOP, DependencySet.INDEPENDENT );
			nodes.put( extraIndividual, n );
			nodeList.add( extraIndividual );

			if( PelletOptions.COPY_ON_WRITE )
				sourceABox = abox;
		}

		if( copyIndividuals ) {
			toBeMerged = abox.toBeMerged;
			if( sourceABox == null ) {
				for( int i = 0; i < nodeCount - extra; i++ ) {
					ATerm x = (ATerm) abox.nodeList.get( i );
					Node node = abox.getNode( x );
					Node copy = node.copyTo( this );

					nodes.put( x, copy );
					nodeList.add( x );
				}

				for( Iterator i = nodes.values().iterator(); i.hasNext(); ) {
					Node node = (Node) i.next();

					node.updateNodeReferences();
				}
			}
		}
		else {
			toBeMerged = Collections.EMPTY_LIST;
			sourceABox = null;
		}

		branch = abox.branch;
		branches = new ArrayList( abox.branches.size() );
		for( int i = 0, n = abox.branches.size(); i < n; i++ ) {
			Branch branch = (Branch) abox.branches.get( i );
			Branch copy;

			if( sourceABox == null ) {
				copy = branch.copyTo( this );
				copy.nodeCount = branch.nodeCount + extra;
			}
			else {
				copy = branch;
			}
			branches.add( copy );
		}

		timer.stop();

	}

	/**
	 * Create a copy of this ABox with all the nodes and edges.
	 * 
	 * @return
	 */
	public ABox copy() {
		return new ABox( this );
	}

	public Object clone() {
		ABox temp = new ABox( this );
		if( pseudoModel != null ) {
			temp.pseudoModel = new ABox( pseudoModel );
		}
		if( lastCompletion != null ) {
			temp.lastCompletion = new ABox( lastCompletion );
		}
		return temp;
	}

	/**
	 * Create a copy of this ABox with one more additional individual. This is
	 * <b>NOT</b> equivalent to create a copy and then add the individual. The
	 * order of individuals in the ABox is important to figure out which
	 * individuals exist in the original ontology and which ones are created by
	 * the tableau algorithm. This function creates a new ABox such that the
	 * individual is supposed to exist in the original ontology. This is very
	 * important when satisfiability of a concept starts with a pesudo model
	 * rather than the initial ABox.
	 * 
	 * @param extraIndividual
	 *            Extra individual to be added to the copy ABox
	 * @return
	 */
	public ABox copy(ATermAppl extraIndividual, boolean copyIndividuals) {
		return new ABox( this, extraIndividual, copyIndividuals );
	}

	public void copyOnWrite() {
		if( sourceABox == null )
			return;

		Timer t = kb.timers.startTimer( "copyOnWrite" );

		List currentNodeList = new ArrayList( nodeList );
		int currentSize = currentNodeList.size();
		int nodeCount = sourceABox.nodes.size();

		nodeList = new ArrayList( nodeCount + 1 );
		nodeList.add( currentNodeList.get( 0 ) );

		for( int i = 0; i < nodeCount; i++ ) {
			ATerm x = (ATerm) sourceABox.nodeList.get( i );
			Node node = sourceABox.getNode( x );
			Node copyNode = node.copyTo( this );
			nodes.put( x, copyNode );
			nodeList.add( x );
		}

		if( currentSize > 1 )
			nodeList.addAll( currentNodeList.subList( 1, currentSize ) );

		for( Iterator i = nodes.values().iterator(); i.hasNext(); ) {
			Node node = (Node) i.next();

			if( sourceABox.nodes.containsKey( node.getName() ) )
				node.updateNodeReferences();
		}

		for( int i = 0, n = branches.size(); i < n; i++ ) {
			Branch branch = (Branch) branches.get( i );
			Branch copy = branch.copyTo( this );
			branches.set( i, copy );

			if( i >= sourceABox.getBranches().size() )
				copy.nodeCount += nodeCount;
			else
				copy.nodeCount += 1;
		}

		t.stop();

		sourceABox = null;
	}

	/**
	 * Clear the pseudo model created for the ABox and concept satisfiability.
	 * 
	 * @param clearSatCache
	 *            If true clear concept satisfiability cache, if false only
	 *            clear pseudo model.
	 */
	public void clearCaches(boolean clearSatCache) {
		pseudoModel = null;
		lastCompletion = null;

		if( clearSatCache ) {
			cache = new ConceptCacheLRU();
		}
	}

	public Bool getCachedSat(ATermAppl c) {
		return cache.getSat( c );
	}

	/**
	 * @deprecated Use {@link #getCache()}
	 */
	public ConceptCache getAllCached() {
		return cache;
	}

	public ConceptCache getCache() {
		return cache;
	}

	public CachedNode getCached(ATermAppl c) {
		return cache.get( c );
	}

	private void cache(Individual rootNode, ATermAppl c, boolean isConsistent) {

		if( !isConsistent ) {
			if( log.isDebugEnabled() ) {
				log.debug( c + " is not satisfiable" );
				log.debug( ATermUtils.negate( c ) + " is TOP" );
			}

			cache.putSat( c, false );
		}
		else {
			// if a merge occurred due to one or more non-deterministic
			// branches then what we are caching depends on this set
			DependencySet ds = rootNode.getMergeDependency( true );
			// if it is merged, get the representative node
			rootNode = (Individual) rootNode.getSame();

			// collect all transitive property values
			if( kb.getExpressivity().hasNominal() )
				rootNode.getABox().collectComplexPropertyValues( rootNode );

			// create a copy of the individual (copying the edges
			// but not the neighbors)
			rootNode = (Individual) rootNode.copy();

			if( log.isDebugEnabled() )
				log.debug( "Cache " + rootNode.debugString() );

			cache.put( c, CachedNode.createNode( rootNode, ds ) );
		}
	}

	Bool mergable(Individual root1, Individual root2, boolean independent) {
		Individual roots[] = new Individual[] { root1, root2 };

		// if a concept c has a cached node rootX == topNode then it means
		// not(c) has a cached node rootY == bottomNode. Only nodes whose
		// negation is unsatisfiable has topNode in their cache

		if( roots[0] == BOTTOM_IND || roots[1] == BOTTOM_IND ) {
			if( log.isDebugEnabled() )
				log.debug( "(1) true " );
			return Bool.FALSE;
		}
		else if( roots[0] == TOP_IND && roots[1] != BOTTOM_IND ) {
			if( log.isDebugEnabled() )
				log.debug( "(2) false " );
			return Bool.TRUE;
		}
		else if( roots[1] == TOP_IND && roots[0] != BOTTOM_IND ) {
			if( log.isDebugEnabled() )
				log.debug( "(3) false " );
			return Bool.TRUE;
		}
		else if( roots[0] == DUMMY_IND || roots[1] == DUMMY_IND )
			return Bool.UNKNOWN;

		Bool result = Bool.TRUE;

		// first test if there is an atomic clash between the types of two roots
		// we pick the root with lower number of types an iterate through all
		// the
		// types in its label
		int root = roots[0].getTypes().size() < roots[1].getTypes().size()
			? 0
			: 1;
		int otherRoot = 1 - root;
		for( Iterator i = roots[root].getTypes().iterator(); i.hasNext(); ) {
			ATermAppl c = (ATermAppl) i.next();
			ATermAppl notC = ATermUtils.negate( c );

			if( roots[otherRoot].hasType( notC ) ) {
				DependencySet ds1 = roots[root].getDepends( c );
				DependencySet ds2 = roots[otherRoot].getDepends( notC );
				boolean allIndependent = independent && ds1.isIndependent() && ds2.isIndependent();
				if( allIndependent )
					return Bool.FALSE;
				else {
					if( log.isDebugEnabled() )
						log.debug( roots[root] + " has " + c + " " + roots[otherRoot]
								+ " has negation " + ds1.max() + " " + ds2.max() );
					result = Bool.UNKNOWN;
				}
			}
		}

		// if there is a suspicion there is no way to fix it later so return now
		if( result.isUnknown() )
			return result;

		for( root = 0; root < 2; root++ ) {
			otherRoot = 1 - root;

			for( Iterator i = roots[root].getTypes( Node.ALL ).iterator(); i.hasNext(); ) {
				ATermAppl av = (ATermAppl) i.next();
				ATerm r = av.getArgument( 0 );
				if( r.getType() == ATerm.LIST )
					r = ((ATermList) r).getFirst();
				Role role = getRole( r );

				if( !role.hasComplexSubRole() ) {
					if( roots[otherRoot].hasRNeighbor( role ) ) {
						if( log.isDebugEnabled() )
							log.debug( roots[root] + " has " + av + " " + roots[otherRoot]
									+ " has " + role + " neighbor" );

						return Bool.UNKNOWN;
					}
				}
				else {
					TransitionGraph tg = role.getFSM();
					for( Iterator it = tg.getInitialState().getTransitions().iterator(); it
							.hasNext(); ) {
						Transition t = (Transition) it.next();
						if( roots[otherRoot].hasRNeighbor( (Role) t.getName() ) ) {
							if( log.isDebugEnabled() )
								log.debug( roots[root] + " has " + av + " " + roots[otherRoot]
										+ " has " + t.getName() + " neighbor" );

							return Bool.UNKNOWN;
						}
					}
				}
			}

			for( Iterator i = roots[root].getTypes( Node.MAX ).iterator(); i.hasNext(); ) {
				ATermAppl mc = (ATermAppl) i.next();
				ATermAppl maxCard = (ATermAppl) mc.getArgument( 0 );

				Role maxR = getRole( maxCard.getArgument( 0 ) );
				int max = ((ATermInt) maxCard.getArgument( 1 )).getInt() - 1;

				int n1 = roots[root].getRNeighborEdges( maxR ).getFilteredNeighbors( roots[root],
						ATermUtils.getTop( maxR ) ).size();
				int n2 = roots[otherRoot].getRNeighborEdges( maxR ).getFilteredNeighbors(
						roots[otherRoot], ATermUtils.getTop( maxR ) ).size();

				if( n1 + n2 > max ) {
					if( log.isDebugEnabled() )
						log.debug( roots[root] + " has " + mc + " " + roots[otherRoot]
								+ " has R-neighbor" );
					return Bool.UNKNOWN;
				}
			}
		}

		if( kb.getExpressivity().hasFunctionality() ) {
			// Timer t = kb.timers.startTimer( "func" );

			root = (roots[0].getOutEdges().size() + roots[0].getInEdges().size()) < (roots[1]
					.getOutEdges().size() + roots[1].getInEdges().size())
				? 0
				: 1;
			otherRoot = 1 - root;

			Set checked = new HashSet();
			for( Iterator i = roots[root].getOutEdges().iterator(); i.hasNext(); ) {
				Edge edge = (Edge) i.next();
				Role role = edge.getRole();

				if( !role.isFunctional() )
					continue;

				Set functionalSupers = role.getFunctionalSupers();
				for( Iterator j = functionalSupers.iterator(); j.hasNext(); ) {
					Role supRole = (Role) j.next();

					if( checked.contains( supRole ) )
						continue;

					checked.add( supRole );

					if( roots[otherRoot].hasRNeighbor( supRole ) ) {
						if( log.isDebugEnabled() )
							log.debug( root1 + " and " + root2 + " has " + supRole );
						return Bool.UNKNOWN;
					}
				}
			}

			for( Iterator i = roots[root].getInEdges().iterator(); i.hasNext(); ) {
				Edge edge = (Edge) i.next();
				Role role = edge.getRole().getInverse();

				if( role == null || !role.isFunctional() )
					continue;

				Set functionalSupers = role.getFunctionalSupers();
				for( Iterator j = functionalSupers.iterator(); j.hasNext(); ) {
					Role supRole = (Role) j.next();

					if( checked.contains( supRole ) )
						continue;

					checked.add( supRole );

					if( roots[otherRoot].hasRNeighbor( supRole ) ) {
						if( log.isDebugEnabled() )
							log.debug( root1 + " and " + root2 + " has " + supRole );
						return Bool.UNKNOWN;
					}
				}
			}

			// t.stop();
		}

		if( kb.getExpressivity().hasNominal() ) {
			boolean nom1 = root1.isNamedIndividual();
			for( Iterator i = root1.getTypes( Node.NOM ).iterator(); !nom1 && i.hasNext(); ) {
				ATermAppl nom = (ATermAppl) i.next();
				ATermAppl name = (ATermAppl) nom.getArgument( 0 );

				nom1 = !ATermUtils.isAnon( name );
			}

			boolean nom2 = root2.isNamedIndividual();
			for( Iterator i = root2.getTypes( Node.NOM ).iterator(); !nom2 && i.hasNext(); ) {
				ATermAppl nom = (ATermAppl) i.next();
				ATermAppl name = (ATermAppl) nom.getArgument( 0 );

				nom2 = !ATermUtils.isAnon( name );
			}

			// FIXME it should be enough to check if named individuals are
			// different or not
			if( nom1 && nom2 )
				return Bool.UNKNOWN;
		}

		// if there is no obvious clash then c1 & not(c2) is satisfiable
		// therefore
		// c1 is NOT a subclass of c2.
		return Bool.TRUE;
	}

	public Bool isKnownSubClassOf(ATermAppl c1, ATermAppl c2) {
		CachedNode cached = getCached( c1 );
		if( cached != null && !doExplanation ) {
			Bool type = isType( cached.node, c2, cached.depends.isIndependent()/*
																				 * ,
																				 * SetUtils.EMPTY_SET
																				 */);

			if( type.isKnown() )
				return type;
		}

		return Bool.UNKNOWN;
	}

	public boolean isSubClassOf(ATermAppl c1, ATermAppl c2) {
		Bool isKnownSubClass = isKnownSubClassOf( c1, c2 );
		if( isKnownSubClass.isKnown() )
			return isKnownSubClass.isTrue();

		if( log.isDebugEnabled() ) {
			long count = kb.timers.getTimer( "subClassSat" ) == null
				? 0
				: kb.timers.getTimer( "subClassSat" ).getCount();
			log.debug( count + ") Checking subclass [" + c1 + " " + c2 + "]" );
		}

		ATermAppl notC2 = ATermUtils.negate( c2 );
		ATermAppl c = ATermUtils.makeAnd( c1, notC2 );
		Timer t = kb.timers.startTimer( "subClassSat" );
		boolean sub = !isSatisfiable( c, false );
		t.stop();

		if( log.isDebugEnabled() )
			log.debug( " Result: " + sub + " (" + t.getLast() + "ms)" );

		return sub;
	}

	public boolean isSatisfiable(ATermAppl c) {
		boolean cacheModel = PelletOptions.USE_CACHING
				&& (ATermUtils.isPrimitiveOrNegated( c ) || PelletOptions.USE_ADVANCED_CACHING);
		return isSatisfiable( c, cacheModel );
	}

	public boolean isSatisfiable(ATermAppl c, boolean cacheModel) {
		c = ATermUtils.normalize( c );

		// if normalization revealed an obvious unsatisfiability, return
		// immediately
		if( c.equals( ATermUtils.BOTTOM ) )
			return false;

		if( log.isDebugEnabled() )
			log.debug( "Satisfiability for " + c );

		if( cacheModel ) {
			CachedNode cached = getCached( c );
			if( cached != null ) {
				boolean satisfiable = !cached.isBottom();
				boolean needToCacheModel = cacheModel && !cached.isComplete();
				if( log.isDebugEnabled() )
					log.debug( "Cached sat for " + c + " is " + satisfiable );
				// if explanation is enabled we should actually build the
				// tableau again to generate the clash. we don't cache the
				// explanation up front because generating explanation is costly
				// and we only want to do it when explicitly asked note that
				// when the concepts is satisfiable there is no explanation to
				// be generated so we return the result immediately
				if( !needToCacheModel && (satisfiable || !doExplanation) )
					return satisfiable;
			}
		}

		satisfiabilityCount++;

		Timer t = kb.timers.startTimer( "satisfiability" );
		boolean isSat = isConsistent( SetUtils.EMPTY_SET, c, cacheModel );
		t.stop();

		if( !isSat && doExplanation && PelletOptions.USE_TRACING ) {
			ATermAppl tempAxiom = ATermUtils.makeTypeAtom( ATermUtils.CONCEPT_SAT_IND, c );
			Set explanationSet = getExplanationSet();
			boolean removed = explanationSet.remove( tempAxiom );
			if( !removed )
				log.warn( "Explanation set is missing an axiom.\n\tAxiom: " + tempAxiom
						+ "\n\tExplantionSet: " + explanationSet );
		}

		return isSat;
	}

	public CandidateSet getObviousInstances(ATermAppl c) {
		return getObviousInstances( c, kb.getIndividuals() );
	}

	public CandidateSet getObviousInstances(ATermAppl c, Collection individuals) {
		c = ATermUtils.normalize( c );
		Set subs = kb.isClassified()
			? kb.taxonomy.getSubs( c, false, true )
			: SetUtils.EMPTY_SET;
		subs.remove( ATermUtils.BOTTOM );

		CandidateSet cs = new CandidateSet();
		Iterator i = individuals.iterator();
		while( i.hasNext() ) {
			ATermAppl x = (ATermAppl) i.next();

			Bool isType = isKnownType( x, c, subs );
			cs.add( x, isType );
		}

		return cs;
	}

	public void getObviousTypes(ATermAppl x, List types, List nonTypes) {
		Individual pNode = pseudoModel.getIndividual( x );
		if( !pNode.getMergeDependency( true ).isIndependent() )
			pNode = getIndividual( x );
		else
			pNode = (Individual) pNode.getSame();

		pNode.getObviousTypes( types, nonTypes );
	}

	public CandidateSet getObviousSubjects(ATermAppl p, ATermAppl o) {
		CandidateSet candidates = new CandidateSet( kb.getIndividuals() );
		getObviousSubjects( p, o, candidates );

		return candidates;
	}

	public void getSubjects(ATermAppl p, ATermAppl o, CandidateSet candidates) {
		Iterator i = candidates.iterator();
		while( i.hasNext() ) {
			ATermAppl s = (ATermAppl) i.next();

			Bool hasObviousValue = hasObviousPropertyValue( s, p, o );
			candidates.update( s, hasObviousValue );
		}
	}

	public void getObviousSubjects(ATermAppl p, ATermAppl o, CandidateSet candidates) {
		Iterator i = candidates.iterator();
		while( i.hasNext() ) {
			ATermAppl s = (ATermAppl) i.next();

			Bool hasObviousValue = hasObviousPropertyValue( s, p, o );
			if( hasObviousValue.isFalse() )
				i.remove();
			else
				candidates.update( s, hasObviousValue );
		}
	}

	public void getObviousObjects(ATermAppl p, CandidateSet candidates) {
		p = getRole( p ).getInverse().getName();
		Iterator i = candidates.iterator();
		while( i.hasNext() ) {
			ATermAppl s = (ATermAppl) i.next();

			Bool hasObviousValue = hasObviousObjectPropertyValue( s, p, null );
			candidates.update( s, hasObviousValue );
		}
	}

	public Bool isKnownType(ATermAppl x, ATermAppl c) {
		return isKnownType( x, c, SetUtils.EMPTY_SET );
	}

	public Bool isKnownType(ATermAppl x, ATermAppl c, Collection subs) {
		Individual pNode = pseudoModel.getIndividual( x );

		boolean isIndependent = true;
		if( pNode.isMerged() ) {
			isIndependent = pNode.getMergeDependency( true ).isIndependent();
			pNode = (Individual) pNode.getSame();
		}

		Bool isType = isKnownType( pNode, c, subs );

		if( isIndependent )
			return isType;
		else if( isType.isTrue() )
			return Bool.UNKNOWN;
		else
			return isType;
	}

	public Bool isKnownType(Individual pNode, ATermAppl concept, /*
																	 * boolean
																	 * isIndependent,
																	 */
	Collection subs) {
		// Timer t = kb.timers.startTimer( "isKnownType" );
		Bool isType = isType( pNode, concept, true );
		if( isType.isUnknown() ) {
			Set concepts = ATermUtils.isAnd( concept )
				? ATermUtils.listToSet( (ATermList) concept.getArgument( 0 ) )
				: SetUtils.singleton( concept );
			for( Iterator it = concepts.iterator(); it.hasNext(); ) {
				ATermAppl c = (ATermAppl) it.next();
				if( pNode.getABox() != null
						&& (pNode.hasObviousType( c ) || pNode.hasObviousType( subs )) ) {
					isType = Bool.TRUE;
				}
				else {
					isType = Bool.UNKNOWN;

					Collection<ATermAppl> axioms = kb.getTBox().getAxioms( c );
					LOOP: for( Iterator j = axioms.iterator(); j.hasNext(); ) {
						ATermAppl axiom = (ATermAppl) j.next();
						ATermAppl term = (ATermAppl) axiom.getArgument( 1 );

						boolean equivalent = axiom.getName().equals( ATermUtils.SAME );
						if( equivalent ) {
							Iterator i = ATermUtils.isAnd( term )
								? new MultiListIterator( (ATermList) term.getArgument( 0 ) )
								: Collections.singleton( term ).iterator();
							Bool knownType = Bool.TRUE;
							while( i.hasNext() && knownType.isTrue() ) {
								term = (ATermAppl) i.next();
								knownType = isKnownType( pNode, term, /* isIndependent, */
								SetUtils.EMPTY_SET );
							}
							if( knownType.isTrue() ) {
								isType = Bool.TRUE;
								break LOOP;
							}
						}
					}
					if( isType.isUnknown() )
						return Bool.UNKNOWN;
				}
			}

		}
		// t.stop();

		return isType;
	}

	private Bool isType(Individual pNode, ATermAppl c, boolean isIndependent) {
		ATermAppl notC = ATermUtils.negate( c );
		CachedNode cached = getCached( notC );
		if( cached != null && cached.isComplete() ) {
			Timer t = kb.timers.startTimer( "mergable" );
			isIndependent &= cached.depends.isIndependent();
			Bool mergable = mergable( pNode, cached.node, isIndependent );
			t.stop();
			if( mergable.isKnown() )
				return mergable.not();
		}

		if( PelletOptions.CHECK_NOMINAL_EDGES /*
												 * &&
												 * kb.getExpressivity().hasNominal()
												 */) {
			cached = getCached( c );
			if( cached != null && cached.depends.isIndependent() ) {
				Timer t = kb.timers.startTimer( "checkNominalEdges" );
				Individual cNode = cached.node;
				for( Iterator i = cNode.getOutEdges().iterator(); i.hasNext(); ) {
					Edge edge = (Edge) i.next();
					Role role = edge.getRole();

					if( edge.getDepends().isIndependent() ) {
						boolean found = false;
						Node val = edge.getTo();

						if( !role.isObjectRole() ) {
							found = pNode.hasRSuccessor( role );
						}
						else if( !val.isRootNominal() ) {
							if( !role.hasComplexSubRole() )
								found = pNode.hasRNeighbor( role );
							else {
								TransitionGraph tg = role.getFSM();
								Iterator it = tg.getInitialState().getTransitions().iterator();
								while( !found && it.hasNext() ) {
									Transition tr = (Transition) it.next();
									found = pNode.hasRNeighbor( (Role) tr.getName() );
								}
							}
						}
						else {
							Set neighbors = null;

							if( role.isSimple() || pNode.isConceptRoot() )
								neighbors = pNode.getRNeighborNames( role );
							else {
								neighbors = new HashSet();
								getObjectPropertyValues( pNode.getName(), role, neighbors,
										neighbors, false );
							}

							found = neighbors.contains( val.getName() );
						}

						if( !found ) {
							t.stop();
							return Bool.FALSE;
						}
					}
				}

				for( Iterator i = cNode.getInEdges().iterator(); i.hasNext(); ) {
					Edge edge = (Edge) i.next();
					Role role = edge.getRole().getInverse();
					Node val = edge.getFrom();

					if( edge.getDepends().isIndependent() ) {
						boolean found = false;

						if( !val.isRootNominal() ) {
							if( role.isSimple() )
								found = pNode.hasRNeighbor( role );
							else {
								Iterator it = role.getFSM().getInitialState().getTransitions()
										.iterator();
								while( !found && it.hasNext() ) {
									Transition tr = (Transition) it.next();
									found = pNode.hasRNeighbor( (Role) tr.getName() );
								}
							}
						}
						else {
							Set neighbors = null;

							if( role.isSimple() || pNode.isConceptRoot() )
								neighbors = pNode.getRNeighborNames( role );
							else {
								neighbors = new HashSet();
								getObjectPropertyValues( pNode.getName(), role, neighbors,
										neighbors, false );
							}

							found = neighbors.contains( val.getName() );
						}

						if( !found ) {
							t.stop();
							// System.out.println( "NOMINAL EDGE " + pNode + " "
							// + c + " " + edge );
							return Bool.FALSE;
						}
					}
				}
				t.stop();
			}
		}

		return Bool.UNKNOWN;
	}

	public boolean isSameAs(ATermAppl ind1, ATermAppl ind2) {
		ATermAppl c = ATermUtils.makeValue( ind2 );

		return isType( ind1, c );
	}

	/**
	 * Returns true if individual x belongs to type c. This is a logical
	 * consequence of the KB if in all possible models x belongs to C. This is
	 * checked by trying to construct a model where x belongs to not(c).
	 * 
	 * @param x
	 * @param c
	 * @return
	 */
	public boolean isType(ATermAppl x, ATermAppl c) {
		c = ATermUtils.normalize( c );

		Set subs = kb.isClassified() && kb.taxonomy.contains( c )
			? kb.taxonomy.getSubs( c, false, true )
			: SetUtils.EMPTY_SET;
		subs.remove( ATermUtils.BOTTOM );

		Bool type = isKnownType( x, c, subs );
		if( type.isKnown() )
			return type.isTrue();

		// List list = (List) kb.instances.get( c );
		// if( list != null )
		// return list.contains( x );

		if( log.isDebugEnabled() )
			log.debug( "Checking type " + c + " for individual " + x );

		ATermAppl notC = ATermUtils.negate( c );

		Timer t = kb.timers.startTimer( "isType" );
		boolean isType = !isConsistent( SetUtils.singleton( x ), notC, false );
		t.stop();

		if( log.isDebugEnabled() )
			log.debug( "Type " + isType + " " + c + " for individual " + x );

		return isType;
	}

	/**
	 * Returns true if any of the individuals in the given list belongs to type
	 * c.
	 * 
	 * @param c
	 * @param inds
	 * @return
	 */
	public boolean isType(List inds, ATermAppl c) {
		c = ATermUtils.normalize( c );

		if( log.isDebugEnabled() )
			log.debug( "Checking type " + c + " for individuals " + inds );

		ATermAppl notC = ATermUtils.negate( c );

		boolean isType = !isConsistent( inds, notC, false );

		if( log.isDebugEnabled() )
			log.debug( "Type " + isType + " " + c + " for individuals " + inds );

		return isType;
	}

	public Bool hasObviousPropertyValue(ATermAppl s, ATermAppl p, ATermAppl o) {
		Role prop = getRole( p );

		if( prop.isDatatypeRole() ) {
			Object value = (o == null)
				? null
				: dtReasoner.getValue( o );
			return hasObviousDataPropertyValue( s, p, value );
		}
		else
			return hasObviousObjectPropertyValue( s, p, o );
	}

	public Bool hasObviousDataPropertyValue(ATermAppl s, ATermAppl p, Object value) {
		Individual subj = pseudoModel.getIndividual( s );
		Role prop = getRole( p );

		// if onlyPositive is set then the answer returned is sound but not
		// complete so we cannot return negative answers
		boolean onlyPositive = false;

		if( !subj.getMergeDependency( true ).isIndependent() ) {
			onlyPositive = true;
			subj = getIndividual( s );
		}
		else
			subj = (Individual) subj.getSame();

		Bool hasValue = subj.hasDataPropertyValue( prop, value );
		if( onlyPositive && hasValue.isFalse() )
			return Bool.UNKNOWN;

		return hasValue;
	}

	public Bool hasObviousObjectPropertyValue(ATermAppl s, ATermAppl p, ATermAppl o) {
		Role prop = getRole( p );

		Set knowns = new HashSet();
		Set unknowns = new HashSet();

		getObjectPropertyValues( s, prop, knowns, unknowns, true );

		if( o == null ) {
			if( !knowns.isEmpty() )
				return Bool.TRUE;
			else if( !unknowns.isEmpty() )
				return Bool.UNKNOWN;
			else
				return Bool.FALSE;
		}
		else {
			if( knowns.contains( o ) )
				return Bool.TRUE;
			else if( unknowns.contains( o ) )
				return Bool.UNKNOWN;
			else
				return Bool.FALSE;
		}
	}

	public boolean hasPropertyValue(ATermAppl s, ATermAppl p, ATermAppl o) {
		Timer t = kb.timers.startTimer( "hasPropertyValue" );
		Bool hasObviousValue = hasObviousPropertyValue( s, p, o );
		if( hasObviousValue.isKnown() )
			return hasObviousValue.isTrue();

		ATermAppl c = null;
		if( o == null ) {
			if( kb.isDatatypeProperty( p ) )
				c = ATermUtils.makeMin( p, 1, ATermUtils.TOP_LIT );
			else
				c = ATermUtils.makeMin( p, 1, ATermUtils.TOP );
		}
		else {
			c = ATermUtils.makeHasValue( p, o );
		}

		boolean isType = isType( s, c );

		t.stop();

		return isType;
	}

	public Set<Role> getPossibleProperties(ATermAppl s) {
		Individual subj = (Individual) pseudoModel.getIndividual( s ).getSame();

		Set<Role> set = new HashSet<Role>();
		EdgeList edges = subj.getOutEdges();
		for( int i = 0; i < edges.size(); i++ ) {
			Edge edge = edges.edgeAt( i );
			Role role = edge.getRole();

			set.addAll( role.getSubRoles() );
			set.addAll( role.getSuperRoles() );
		}

		edges = subj.getInEdges();
		for( int i = 0; i < edges.size(); i++ ) {
			Edge edge = edges.edgeAt( i );
			Role role = edge.getRole();

			role = role.getInverse();
			set.addAll( role.getSubRoles() );
			set.addAll( role.getSuperRoles() );
		}

		for( Iterator i = set.iterator(); i.hasNext(); ) {
			Role role = (Role) i.next();
			if( role.isAnon() )
				i.remove();
		}

		return set;
	}

	public List<ATermAppl> getDataPropertyValues(ATermAppl s, Role role, Datatype datatype) {
		return getDataPropertyValues( s, role, datatype, false );
	}

	public List<ATermAppl> getDataPropertyValues(ATermAppl s, Role role, Datatype datatype,
			boolean onlyObvious) {
		Individual subj = pseudoModel.getIndividual( s );

		List<ATermAppl> values = new ArrayList<ATermAppl>();

		boolean isIndependent = true;
		if( subj.isMerged() ) {
			isIndependent = subj.getMergeDependency( true ).isIndependent();
			subj = (Individual) subj.getSame();
		}

		EdgeList edges = subj.getRSuccessorEdges( role );
		for( int i = 0; i < edges.size(); i++ ) {
			Edge edge = edges.edgeAt( i );
			DependencySet ds = edge.getDepends();
			Literal literal = (Literal) edge.getTo();
			ATermAppl literalValue = literal.getTerm();
			if( literalValue != null ) {
				if( datatype != null && !datatype.contains( literal.getValue() ) )
					continue;

				if( isIndependent && ds.isIndependent() )
					values.add( literalValue );
				else if( !onlyObvious ) {
					ATermAppl hasValue = ATermUtils.makeHasValue( role.getName(), literalValue );
					if( isType( s, hasValue ) )
						values.add( literalValue );
				}
			}
			else {
				// TODO maybe we can get the value is
				// literal.getDatatype().size() == 1
			}
		}

		return values;
	}

	public List getObviousDataPropertyValues(ATermAppl s, Role prop, Datatype datatype) {
		return getDataPropertyValues( s, prop, datatype, true );
	}

	public void getObjectPropertyValues(ATermAppl s, Role role, Set knowns, Set unknowns,
			boolean getSames) {
		Individual subj = (pseudoModel != null)
			? pseudoModel.getIndividual( s )
			: getIndividual( s );

		boolean isIndependent = true;
		if( subj.isMerged() ) {
			isIndependent = subj.getMergeDependency( true ).isIndependent();
			subj = (Individual) subj.getSame();
		}

		if( role.isSimple() )
			getSimpleObjectPropertyValues( subj, role, knowns, unknowns, getSames );
		else if( !role.hasComplexSubRole() )
			getTransitivePropertyValues( subj, role, knowns, unknowns, getSames, new HashSet(),
					true );
		else {
			TransitionGraph tg = role.getFSM();
			getComplexObjectPropertyValues( subj, tg.getInitialState(), tg, knowns, unknowns,
					getSames, new HashSet(), true );
		}

		if( !isIndependent ) {
			unknowns.addAll( knowns );
			knowns.clear();
		}
	}

	void getSimpleObjectPropertyValues(Individual subj, Role role, Set knowns, Set unknowns,
			boolean getSames) {
		EdgeList edges = subj.getRNeighborEdges( role );
		for( int i = 0; i < edges.size(); i++ ) {
			Edge edge = edges.edgeAt( i );
			DependencySet ds = edge.getDepends();
			Individual value = (Individual) edge.getNeighbor( subj );

			if( value.isRootNominal() ) {
				if( ds.isIndependent() ) {
					if( getSames )
						getSames( value, knowns, unknowns );
					else
						knowns.add( value.getName() );
				}
				else {
					if( getSames )
						getSames( value, unknowns, unknowns );
					else
						unknowns.add( value.getName() );
				}
			}
		}
	}

	void getTransitivePropertyValues(Individual subj, Role prop, Set knowns, Set unknowns,
			boolean getSames, Set visited, boolean isIndependent) {
		if( visited.contains( subj.getName() ) )
			return;
		else
			visited.add( subj.getName() );

		EdgeList edges = subj.getRNeighborEdges( prop );
		for( int i = 0; i < edges.size(); i++ ) {
			Edge edge = edges.edgeAt( i );
			DependencySet ds = edge.getDepends();
			Individual value = (Individual) edge.getNeighbor( subj );
			Role edgeRole = edge.getFrom().equals( subj )
				? edge.getRole()
				: edge.getRole().getInverse();
			if( value.isRootNominal() ) {
				if( isIndependent && ds.isIndependent() ) {
					if( getSames )
						getSames( value, knowns, unknowns );
					else
						knowns.add( value.getName() );
				}
				else {
					if( getSames )
						getSames( value, unknowns, unknowns );
					else
						unknowns.add( value.getName() );
				}
			}

			if( !prop.isSimple() ) {
				// all the following roles might cause this property to
				// propagate
				Set transRoles = SetUtils.intersection( edgeRole.getSuperRoles(), prop
						.getTransitiveSubRoles() );
				for( Iterator j = transRoles.iterator(); j.hasNext(); ) {
					Role transRole = (Role) j.next();
					getTransitivePropertyValues( value, transRole, knowns, unknowns, getSames,
							visited, isIndependent && ds.isIndependent() );
				}
			}
		}
	}

	void getComplexObjectPropertyValues(Individual subj, State st, TransitionGraph tg, Set knowns,
			Set unknowns, boolean getSames, Set visited, boolean isIndependent) {
		Pair key = new Pair( subj, st );
		if( visited.contains( key ) )
			return;
		else
			visited.add( key );

		if( tg.isFinal( st ) && subj.isRootNominal() ) {
			log.debug( "add " + subj );
			if( isIndependent ) {
				if( getSames )
					getSames( subj, knowns, unknowns );
				else
					knowns.add( subj.getName() );
			}
			else {
				if( getSames )
					getSames( subj, unknowns, unknowns );
				else
					unknowns.add( subj.getName() );
			}
		}

		log.debug( subj );

		for( Iterator it = st.getTransitions().iterator(); it.hasNext(); ) {
			Transition t = (Transition) it.next();
			Role r = (Role) t.getName();
			EdgeList edges = subj.getRNeighborEdges( r );
			for( int i = 0; i < edges.size(); i++ ) {
				Edge edge = edges.edgeAt( i );
				DependencySet ds = edge.getDepends();
				Individual value = (Individual) edge.getNeighbor( subj );

				getComplexObjectPropertyValues( value, t.getTo(), tg, knowns, unknowns, getSames,
						visited, isIndependent && ds.isIndependent() );
			}
		}
	}

	void collectComplexPropertyValues(Individual subj) {
		Set collected = new HashSet();
		Set roles = subj.getOutEdges().getRoles();
		for( Iterator i = roles.iterator(); i.hasNext(); ) {
			Role role = (Role) i.next();
			// only collect non-simple, i.e. complex, roles
			// TODO we might not need to collect all non-simple roles
			// collecting only the base ones, i.e. minimal w.r.t. role
			// ordering, would be enough
			if( role.isSimple() || collected.contains( role ) )
				continue;

			collectComplexPropertyValues( subj, role );
		}
		roles = subj.getInEdges().getRoles();
		for( Iterator i = roles.iterator(); i.hasNext(); ) {
			Role role = (Role) i.next();
			role = role.getInverse();
			if( role.isSimple() || collected.contains( role ) )
				continue;

			collectComplexPropertyValues( subj, role );
		}
	}

	void collectComplexPropertyValues(Individual subj, Role role) {
		Set knowns = new HashSet();
		Set unknowns = new HashSet();

		getObjectPropertyValues( subj.getName(), role, knowns, unknowns, false );

		for( Iterator j = knowns.iterator(); j.hasNext(); ) {
			ATermAppl val = (ATermAppl) j.next();
			Individual ind = getIndividual( val );
			subj.addEdge( role, ind, DependencySet.INDEPENDENT );
		}
		for( Iterator j = unknowns.iterator(); j.hasNext(); ) {
			ATermAppl val = (ATermAppl) j.next();
			Individual ind = getIndividual( val );
			subj.addEdge( role, ind, DependencySet.EMPTY );
		}
	}

	public void getSames(Individual ind, Set knowns, Set unknowns) {
		knowns.add( ind.getName() );

		boolean thisMerged = ind.isMerged() && !ind.getMergeDependency( true ).isIndependent();

		Iterator i = ind.getMerged().iterator();
		while( i.hasNext() ) {
			Individual other = (Individual) i.next();

			if( !other.isRootNominal() )
				continue;

			boolean otherMerged = other.isMerged()
					&& !other.getMergeDependency( true ).isIndependent();
			if( thisMerged || otherMerged ) {
				unknowns.add( other.getName() );
				getSames( other, unknowns, unknowns );
			}
			else {
				knowns.add( other.getName() );
				getSames( other, knowns, unknowns );
			}
		}
	}

	/**
	 * Return true if this ABox is consistent. Consistent ABox means after
	 * applying all the tableau completion rules at least one branch with no
	 * clashes was found
	 * 
	 * @return
	 */
	public boolean isConsistent() {
		boolean isConsistent = false;

		// if we can use the incremental consistency checking approach, use it
		if( kb.canUseIncConsistency() ) {
			isConsistent = isIncConsistent();
		}
		else {
			// if ABox is empty then we need to actually check the
			// satisfiability
			// of the TOP class to make sure that universe is not empty
			ATermAppl c = isEmpty()
				? ATermUtils.TOP
				: null;

			isConsistent = isConsistent( SetUtils.EMPTY_SET, c, false );

			if( isConsistent ) {
				// put the BOTTOM concept into the cache which will
				// also put TOP in there
				cache.putSat( ATermUtils.BOTTOM, false );
			}
		}

		// reset the updated indviduals
		updatedIndividuals = new HashSet();
		newIndividuals = new HashSet();

		return isConsistent;
	}

	/**
	 * Check the consistency of this ABox possibly after adding some type
	 * assertions. If <code>c</code> is null then nothing is added to ABox
	 * (pure consistency test) and the individuals should be an empty
	 * collection. If <code>c</code> is not null but <code>individuals</code>
	 * is empty, this is a satisfiability check for concept <code>c</code> so
	 * a new individual will be added with type <code>c</code>. If
	 * individuals is not empty, this means we will add type <code>c</code> to
	 * each of the individuals in the collection and check the consistency.
	 * <p>
	 * The consistency checks will be done either on a copy of the ABox or its
	 * pseudo model depending on the situation. In either case this ABox will
	 * not be modified at all. After the consistency check lastCompletion points
	 * to the modified ABox.
	 * 
	 * @param individuals
	 * @param c
	 * @return
	 */
	private boolean isConsistent(Collection individuals, ATermAppl c, boolean cacheModel) {
		Timer t = kb.timers.startTimer( "isConsistent" );

		if( log.isInfoEnabled() ) {
			if( c == null )
				log.info( "ABox consistency for " + individuals.size() + " individuals" );
			else
				log.info( "Consistency " + c + " for " + individuals.size() + " individuals "
						+ individuals );
		}

		Expressivity expr = kb.getExpressivity().compute( c );

		// if c is null we are checking the consistency of this ABox as
		// it is and we will not add anything extra
		boolean buildPseudoModel = (c == null);

		// if individuals is empty and we are not building the pseudo
		// model then this is concept satisfiability
		boolean conceptSatisfiability = !buildPseudoModel && individuals.isEmpty();

		// Check if there are any nominals in the KB or nominal
		// reasoning is disabled
		boolean hasNominal = expr.hasNominal() && !PelletOptions.USE_PSEUDO_NOMINALS;

		// Use empty model only if this is concept satisfiability for a KB
		// where there are no nominals (for Econn never use empty ABox)
		boolean canUseEmptyModel = conceptSatisfiability && !hasNominal;

		// Use pseudo model only if we are not building the pseudo model, pseudo
		// model
		// option is enabled and the strategy used to build the pseduo model
		// actually
		// supports pseudo model completion
		boolean canUsePseudoModel = !buildPseudoModel && pseudoModel != null
				&& PelletOptions.USE_PSEUDO_MODEL
				&& kb.chooseStrategy( this ).supportsPseudoModelCompletion();

		ATermAppl x = null;
		if( conceptSatisfiability ) {
			x = ATermUtils.CONCEPT_SAT_IND;
			individuals = SetUtils.singleton( x );
		}

		ABox abox = canUseEmptyModel
			? copy( x, false )
			: canUsePseudoModel
				? pseudoModel.copy( x, true )
				: copy( x, true );

		// Due to abox deletions, it could be the case that this is an abox
		// consistency check, the kb was previously inconsistent,
		// and there was a removal which invalidated the clash. If the clash is
		// independent, then we need to retract the clash
		if( (abox.getClash() != null) && buildPseudoModel && getKB().aboxDeletion ) {
			if( abox.getClash().depends.isIndependent() )
				abox.setClash( null );
		}

		for( Iterator i = individuals.iterator(); i.hasNext(); ) {
			ATermAppl ind = (ATermAppl) i.next();

			abox.setSyntacticUpdate( true );
			abox.addType( ind, c );
			abox.setSyntacticUpdate( false );
		}

		if( log.isDebugEnabled() )
			log.debug( "Consistency check starts" );

		ABox completion = null;

		if( abox.isEmpty() ) {
			completion = abox;
		}
		else {
			CompletionStrategy strategy = kb.chooseStrategy( abox, expr );

			if( log.isDebugEnabled() )
				log.debug( "Strategy: " + strategy.getClass().getName() );

			completion = strategy.complete();
		}

		boolean consistent = !completion.isClosed();

		if( buildPseudoModel )
			pseudoModel = completion;

		if( x != null && c != null && cacheModel ) {
			cache( completion.getIndividual( x ), c, consistent );
		}

		if( log.isInfoEnabled() )
			log.info( "Consistent: " + consistent + " Tree depth: " + completion.treeDepth
					+ " Tree size: " + completion.getNodes().size() + " Time: "
					+ t.getElapsed() );

		if( !consistent ) {
			lastClash = completion.getClash();
			if( log.isDebugEnabled() )
				log.debug( completion.getClash().detailedString() );
		}

		consistencyCount++;

		if( keepLastCompletion )
			lastCompletion = completion;
		else
			lastCompletion = null;

		t.stop();

		return consistent;
	}

	/**
	 * Check the consistency of this ABox using the incremental consistency
	 * checking approach
	 */
	private boolean isIncConsistent() {
		Timer incT = kb.timers.startTimer( "isIncConsistent" );
		Timer t = kb.timers.startTimer( "isConsistent" );

		// set debug output
		// ((Log4JLogger)ABox.log).getLogger().setLevel(Level.DEBUG);
		// ((Log4JLogger)DependencyIndex.log).getLogger().setLevel(Level.DEBUG);

		// throw away old information to let gc do its work
		lastCompletion = null;

		// as this is an inc. consistency check, simply use the pseudomodel
		ABox abox = pseudoModel;

		// if this is a deletion then remove dependent structures, update
		// completion queue and update branches
		if( kb.aboxDeletion )
			kb.restoreDependencies();

		// set updated individuals for incremental completion
		abox.updatedIndividuals = this.updatedIndividuals;
		abox.newIndividuals = this.newIndividuals;

		// CHW - It appears that there are cases that kb.status may be false
		// prior to an incremental deletion. I think this may pop up if the KB
		// is inconsistent
		// prior to the deletion (which certainly should be supported). In order
		// for Strategy.initialize to only perform the limited initialization
		// (ie., when the kb is initialized)
		// we explicitly set the status here.
		// TODO: this should be looked into further
		kb.status = KnowledgeBase.UNCHANGED;
		abox.setInitialized( true );

		if( log.isDebugEnabled() )
			log.debug( "Consistency check starts" );

		if( abox.isEmpty() ) {
			lastCompletion = abox;
		}
		else {

			Timer timer = kb.timers.startTimer( "loadStrat" );
			// currently there is only one incremental consistency strategy
			CompletionStrategy incStrategy = new SHOIQIncStrategy( abox );
			timer.stop();

			if( log.isDebugEnabled() )
				log.debug( "Strategy: " + incStrategy.getClass().getName() );

			// set abox to not being complete
			abox.setComplete( false );
			lastCompletion = incStrategy.complete();
		}

		boolean consistent = !lastCompletion.isClosed();

		pseudoModel = lastCompletion;

		if( log.isInfoEnabled() )
			log.info( "Consistent: " + consistent + " Tree depth: " + lastCompletion.treeDepth
					+ " Tree size: " + lastCompletion.getNodes().size() );

		if( !consistent ) {
			lastClash = lastCompletion.getClash();
			if( log.isDebugEnabled() )
				log.debug( lastCompletion.getClash().detailedString() );
		}

		consistencyCount++;

		if( !keepLastCompletion )
			lastCompletion = null;

		t.stop();
		incT.stop();

		// ((Log4JLogger)ABox.log).getLogger().setLevel(Level.OFF);
		// ((Log4JLogger)DependencyIndex.log).getLogger().setLevel(Level.OFF);

		return consistent;
	}

	public EdgeList getInEdges(ATerm x) {
		return getNode( x ).getInEdges();
	}

	public EdgeList getOutEdges(ATerm x) {
		Node node = getNode( x );
		if( node instanceof Literal )
			return new EdgeList();
		return ((Individual) node).getOutEdges();
	}

	public Individual getIndividual(ATerm x) {
		return (Individual) nodes.get( x );
	}

	public Literal getLiteral(ATerm x) {
		return (Literal) nodes.get( x );
	}

	public Node getNode(ATerm x) {
		return (Node) nodes.get( x );
	}

	public void addType(ATermAppl x, ATermAppl c) {
		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( Collections.singleton( ATermUtils.makeTypeAtom( x, c ) ) )
			: DependencySet.INDEPENDENT;

		addType( x, c, ds );
	}

	public void addType(ATermAppl x, ATermAppl c, DependencySet ds) {
		c = ATermUtils.normalize( c );

		// when a type is being added to a pseudo model, i.e.
		// an ABox that has already been completed, the branch
		// of the dependency set will automatically be set to
		// the current branch. We need to set it to initial
		// branch number to make sure that this type assertion
		// is being added to the initial model
		int remember = branch;
		branch = DependencySet.NO_BRANCH;

		Node node = getNode( x );

		if( node.isMerged() ) {
			ds = node.getMergeDependency( true );
			node = node.getSame();
			if( !ds.isIndependent() ) {
				typeAssertions.put( x, c );
			}
		}

		node.addType( c, ds );

		branch = remember;
	}

	public void removeType(ATermAppl x, ATermAppl c) {
		c = ATermUtils.normalize( c );

		Node node = getNode( x );
		node.removeType( c );
	}

	/**
	 * Add a new literal to the ABox. This function is used only when the
	 * literal value does not have a known value, e.g. applyMinRule would create
	 * such a literal.
	 * 
	 * @return
	 */
	public Literal addLiteral() {
		return createLiteral( ATermUtils.makeLiteral( createUniqueName( false ) ) );
	}

	/**
	 * Add a new literal to the ABox. Literal will be assigned a fresh unique
	 * name.
	 * 
	 * @param dataValue
	 *            A literal ATerm which should be constructed with one of
	 *            ATermUtils.makeXXXLiteral functions
	 * @return Literal object that has been created
	 */
	public Literal addLiteral(ATermAppl dataValue) {
		return addLiteral( dataValue, DependencySet.INDEPENDENT );
	}

	public Literal addLiteral(ATermAppl dataValue, DependencySet ds) {
		if( dataValue == null || !ATermUtils.isLiteral( dataValue ) )
			throw new InternalReasonerException( "Invalid value to create a literal. Value: "
					+ dataValue );

		return createLiteral( dataValue, ds );
	}

	/**
	 * Helper function to add a literal.
	 * 
	 * @param value
	 *            The java object that represents the value of this literal
	 * @return
	 */
	private Literal createLiteral(ATermAppl dataValue) {
		return createLiteral( dataValue, DependencySet.INDEPENDENT );
	}

	private Literal createLiteral(ATermAppl dataValue, DependencySet ds) {
		String lexicalValue = ((ATermAppl) dataValue.getArgument( 0 )).getName();
		String lang = ((ATermAppl) dataValue.getArgument( 1 )).getName();
		String datatypeURI = ((ATermAppl) dataValue.getArgument( 2 )).getName();

		ATermAppl name = null;
		if( !datatypeURI.equals( "" ) ) {
			Datatype dt = kb.getDatatypeReasoner().getDatatype( datatypeURI );
			if( dt instanceof AtomicDatatype )
				datatypeURI = ((AtomicDatatype) dt).getPrimitiveType().getURI();
			name = ATermUtils.makeTypedLiteral( lexicalValue, datatypeURI );
		}
		else
			name = ATermUtils.makePlainLiteral( lexicalValue, lang );

		Node node = getNode( name );
		if( node != null ) {
			if( node instanceof Literal ) {

				if( ((Literal) node).getValue() == null && PelletOptions.USE_COMPLETION_QUEUE ) {
					// added for completion queue
					QueueElement newElement = new QueueElement( node.getName(), null );
					this.completionQueue.add( newElement, CompletionQueue.LITERALLIST );
				}

				if( getBranch() >= 0 && PelletOptions.USE_COMPLETION_QUEUE )
					completionQueue.addEffected( getBranch(), node.getName() );

				return (Literal) node;
			}
			else
				throw new InternalReasonerException(
						"Same term refers to both a literal and an individual: " + name );
		}

		Literal lit = new Literal( name, dataValue, this, ds );
		lit.addType( ATermUtils.makeTermAppl( Namespaces.RDFS + "Literal" ),
				DependencySet.INDEPENDENT );

		nodes.put( name, lit );
		nodeList.add( name );

		if( lit.getValue() == null && PelletOptions.USE_COMPLETION_QUEUE ) {
			// added for completion queue
			QueueElement newElement = new QueueElement( lit.getName(), null );
			this.completionQueue.add( newElement, CompletionQueue.LITERALLIST );
		}

		if( getBranch() >= 0 && PelletOptions.USE_COMPLETION_QUEUE )
			completionQueue.addEffected( getBranch(), lit.getName() );

		return lit;
	}

	public Individual addIndividual(ATermAppl x) {
		Individual ind = addIndividual( x, Individual.NOMINAL );

		if( !PelletOptions.USE_PSEUDO_NOMINALS ) {
			// add value(x) for nominal node but do not apply UC yet
			// because it might not be complete. it will be added
			// by CompletionStrategy.initialize()
			ATermAppl valueX = ATermUtils.makeValue( x );
			ind.addType( valueX, DependencySet.INDEPENDENT );
		}

		// update affected inds for this branch
		if( getBranch() >= 0 && PelletOptions.USE_COMPLETION_QUEUE )
			completionQueue.addEffected( getBranch(), ind.getName() );

		return ind;
	}

	Individual addFreshIndividual(boolean isNominal) {
		ATermAppl name = createUniqueName( isNominal );
		Individual ind = addIndividual( name, Individual.BLOCKABLE );

		if( isNominal )
			ind.setNominalLevel( 1 );

		return ind;
	}

	public void removeIndividual(ATermAppl c) {
		if( nodes.containsKey( c ) ) {
			nodes.remove( c );
			nodeList.remove( c );
		}
	}

	private Individual addIndividual(ATermAppl x, int nominalLevel) {
		if( nodes.containsKey( x ) )
			throw new InternalReasonerException( "adding a node twice " + x );

		changed = true;

		// if can use inc strategy, then set pseudomodel.changed
		if( kb.canUseIncConsistency() && this.getPseudoModel() != null ) {
			this.getPseudoModel().changed = true;
		}

		Individual n = new Individual( x, this, nominalLevel );
		n.branch = branch;
		n.addType( ATermUtils.TOP, DependencySet.INDEPENDENT );

		nodes.put( x, n );
		nodeList.add( x );

		if( getBranch() > 0 && PelletOptions.USE_COMPLETION_QUEUE )
			completionQueue.addEffected( getBranch(), n.getName() );

		return n;
	}

	public void addSame(ATermAppl x, ATermAppl y) {
		Individual ind1 = getIndividual( x );
		Individual ind2 = getIndividual( y );

		// ind1.setSame(ind2, new DependencySet(explanationTable.getCurrent()));

		// ind1.setSame(ind2, DependencySet.INDEPENDENT);
		ATermAppl sameAxiom = ATermUtils.makeSame( x, y );

		// update syntactic assertions - currently i do not add this to the
		// dependency index
		// now, as it will be added during the actual merge when the completion
		// is performed
		if( PelletOptions.USE_INCREMENTAL_DELETION )
			kb.getSyntacticAssertions().add( sameAxiom );

		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( sameAxiom )
			: DependencySet.INDEPENDENT;
		toBeMerged.add( new NodeMerge( ind1, ind2, ds ) );
	}

	public void addDifferent(ATermAppl x, ATermAppl y) {
		Individual ind1 = getIndividual( x );
		Individual ind2 = getIndividual( y );

		ATermAppl diffAxiom = ATermUtils.makeDifferent( x, y );

		// update syntactic assertions - currently i do not add this to the
		// dependency index
		// now, as it will simply be used during the completion strategy
		if( PelletOptions.USE_INCREMENTAL_DELETION )
			kb.getSyntacticAssertions().add( diffAxiom );

		// ind1.setDifferent(ind2, new
		// DependencySet(explanationTable.getCurrent()));
		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( diffAxiom )
			: DependencySet.INDEPENDENT;
		ind1.setDifferent( ind2, ds );
	}

	public void addAllDifferent(ATermList list) {
		ATermAppl allDifferent = ATermUtils.makeAllDifferent( list );
		ATermList outer = list;
		while( !outer.isEmpty() ) {
			ATermList inner = outer.getNext();
			while( !inner.isEmpty() ) {
				Individual ind1 = getIndividual( outer.getFirst() );
				Individual ind2 = getIndividual( inner.getFirst() );

				// update syntactic assertions - currently i do not add this to
				// the dependency index
				// now, as it will be added during the actual merge when the
				// completion is performed
				if( PelletOptions.USE_INCREMENTAL_DELETION )
					kb.getSyntacticAssertions().add( allDifferent );

				DependencySet ds = PelletOptions.USE_TRACING
					? new DependencySet( allDifferent )
					: DependencySet.INDEPENDENT;
				ind1.setDifferent( ind2, ds );

				inner = inner.getNext();
			}
			outer = outer.getNext();
		}
	}

	public boolean isNode(ATerm x) {
		return getNode( x ) != null;
	}

	final private ATermAppl createUniqueName(boolean isNominal) {
		String str = PelletOptions.ANON + (++anonCount);

		ATermAppl name = isNominal
			? ATermUtils.makeAnonNominal( str )
			: ATermUtils.makeTermAppl( str );

		return name;
	}

	final public Collection getNodes() {
		return nodes.values();
	}

	final Map getNodeMap() {
		return nodes;
	}

	final public List getNodeNames() {
		return nodeList;
	}

	public String toString() {
		return "[size: " + nodes.size() + " freeMemory: "
				+ (Runtime.getRuntime().freeMemory() / 1000000.0) + "mb]";
	}

	/**
	 * @return Returns the datatype reasoner.
	 */
	public DatatypeReasoner getDatatypeReasoner() {
		return dtReasoner;
	}

	/**
	 * @return Returns the isComplete.
	 */
	public boolean isComplete() {
		return isComplete;
	}

	/**
	 * @param isComplete
	 *            The isComplete to set.
	 */
	void setComplete(boolean isComplete) {
		this.isComplete = isComplete;
	}

	/**
	 * Returns true if Abox has a clash.
	 * 
	 * @return
	 */
	public boolean isClosed() {
		return !PelletOptions.SATURATE_TABLEAU && clash != null;
	}

	public Clash getClash() {
		return clash;
	}

	public void setClash(Clash clash) {
		if( clash != null ) {
			if( log.isDebugEnabled() ) {
				log.debug( "CLSH: " + clash );
				if( clash.depends.max() > branch && branch != -1 )
					log.error( "Invalid clash dependency " + clash + " > " + branch );
			}
			if( this.clash != null ) {
				if( log.isDebugEnabled() )
					log.debug( "Clash was already set \nExisting: " + this.clash + "\nNew     : "
							+ clash );

				if( this.clash.depends.max() < clash.depends.max() )
					return;
				// else if(this.clash.isAtomic() &&
				// ATermUtils.isPrimitive((ATermAppl)this.clash.args[0]))
				// return;
			}
		}

		this.clash = clash;
		// CHW - added for incremental deletions
		if( PelletOptions.USE_INCREMENTAL_DELETION )
			kb.getDependencyIndex().setClashDependencies( this.clash );

	}

	/**
	 * @return Returns the kb.
	 */
	public KnowledgeBase getKB() {
		return kb;
	}

	/**
	 * Convenience function to get the named role.
	 */
	public Role getRole(ATerm r) {
		return kb.getRole( r );
	}

	/**
	 * Return the RBox
	 */
	public RBox getRBox() {
		return kb.getRBox();
	}

	/**
	 * Return the TBox
	 */
	public TBox getTBox() {
		return kb.getTBox();
	}

	/**
	 * Return the current branch number. Branches are created when a
	 * non-deterministic rule, e.g. disjunction or max rule, is being applied.
	 * 
	 * @return Returns the branch.
	 */
	int getBranch() {
		return branch;
	}

	/**
	 * Set the branch number (should only be called when backjumping is in
	 * progress)
	 * 
	 * @param branch
	 */
	void setBranch(int branch) {
		this.branch = branch;
	}

	/**
	 * Increment the branch number (should only be called when a
	 * non-deterministic rule, e.g. disjunction or max rule, is being applied)
	 * 
	 * @param branch
	 */
	void incrementBranch() {

		if( PelletOptions.USE_COMPLETION_QUEUE ) {
			completionQueue.incrementBranch( this.branch );
		}

		this.branch++;
	}

	/**
	 * Check if the ABox is ready to be completed.
	 * 
	 * @return Returns the initialized.
	 */
	public boolean isInitialized() {
		return initialized && !kb.isChanged();
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	/**
	 * Checks if the explanation is turned on.
	 * 
	 * @return Returns the doExplanation.
	 */
	final public boolean doExplanation() {
		return doExplanation || log.isDebugEnabled();
	}

	/**
	 * Enable/disable explanation generation
	 * 
	 * @param doExplanation
	 *            The doExplanation to set.
	 */
	public void setDoExplanation(boolean doExplanation) {
		this.doExplanation = doExplanation;
	}

	public String getExplanation() {
		// Clash lastClash = (lastCompletion != null) ?
		// lastCompletion.getClash() : null;
		if( lastClash == null )
			return "No inconsistency was found! There is no explanation generated.";
		else
			return lastClash.detailedString();
	}

	public Set<ATermAppl> getExplanationSet() {
		if( lastClash == null )
			throw new RuntimeException( "No explanation was generated!" );
		
		return lastClash.depends.explain;
	}

	/**
	 * Returns the branches.
	 */
	public List getBranches() {
		return branches;
	}

	/**
	 * Return individuals to which we need to apply the tableau rules
	 * 
	 * @return
	 */
	public IndividualIterator getIndIterator() {
		return new IndividualIterator( this );
	}

	/**
	 * Validate all the edges in the ABox nodes. Used to find bugs in the copy
	 * and detach/attach functions.
	 */
	public void validate() {
		if( !PelletOptions.VALIDATE_ABOX )
			return;
		System.out.print( "VALIDATING..." );
		Iterator n = getIndIterator();
		while( n.hasNext() ) {
			Individual node = (Individual) n.next();
			if( node.isPruned() )
				continue;
			validate( node );
		}
	}

	void validate(Individual node) {
		List[] negatedTypes = {
				node.getTypes( Node.ATOM ), node.getTypes( Node.SOME ), node.getTypes( Node.OR ),
				node.getTypes( Node.MAX ) };

		for( int j = 0; j < negatedTypes.length; j++ ) {
			for( int i = 0, n = negatedTypes[j].size(); i < n; i++ ) {
				ATermAppl a = (ATermAppl) negatedTypes[j].get( i );
				if( a.getArity() == 0 )
					continue;
				ATermAppl notA = (ATermAppl) a.getArgument( 0 );

				if( node.hasType( notA ) ) {
					if( !node.hasType( a ) )
						throw new InternalReasonerException( "Invalid type found: " + node + " "
								+ j + " " + a + " " + node.debugString() + " " + node.depends );
					throw new InternalReasonerException( "Clash found: " + node + " " + a + " "
							+ node.debugString() + " " + node.depends );
				}
			}
		}

		if( !node.isRoot() ) {
			if( node.getPredecessors().size() != 1 )
				throw new InternalReasonerException( "Invalid blockable node: " + node + " "
						+ node.getInEdges() );

		}
		else if( node.isNominal() ) {
			ATermAppl nominal = ATermUtils.makeValue( node.getName() );
			if( !ATermUtils.isAnonNominal( node.getName() ) && !node.hasType( nominal ) )
				throw new InternalReasonerException( "Invalid nominal node: " + node + " "
						+ node.getTypes() );
		}

		for( Iterator i = node.getDepends().keySet().iterator(); i.hasNext(); ) {
			ATermAppl c = (ATermAppl) i.next();
			DependencySet ds = node.getDepends( c );
			if( ds.max() > branch || (!PelletOptions.USE_SMART_RESTORE && ds.branch > branch) )
				throw new InternalReasonerException( "Invalid ds found: " + node + " " + c + " "
						+ ds + " " + branch );
			// if( c.getAFun().equals( ATermUtils.VALUEFUN ) ) {
			// if( !PelletOptions.USE_PSEUDO_NOMINALS ) {
			// Individual z = getIndividual(c.getArgument(0));
			// if(z == null)
			// throw new InternalReasonerException("Nominal to non-existing
			// node: " + node + " " + c + " " + ds + " " + branch);
			// }
			// }
		}
		for( Iterator i = node.getDifferents().iterator(); i.hasNext(); ) {
			Node ind = (Node) i.next();
			DependencySet ds = node.getDifferenceDependency( ind );
			if( ds.max() > branch || ds.branch > branch )
				throw new InternalReasonerException( "Invalid ds: " + node + " != " + ind + " "
						+ ds );
			if( ind.getDifferenceDependency( node ) == null )
				throw new InternalReasonerException( "Invalid difference: " + node + " != " + ind
						+ " " + ds );
		}
		EdgeList edges = node.getOutEdges();
		for( int e = 0; e < edges.size(); e++ ) {
			Edge edge = edges.edgeAt( e );
			Node succ = edge.getTo();
			if( nodes.get( succ.getName() ) != succ )
				throw new InternalReasonerException( "Invalid edge to a non-existing node: " + edge
						+ " " + nodes.get( succ.getName() ) + "("
						+ nodes.get( succ.getName() ).hashCode() + ")" + succ + "("
						+ succ.hashCode() + ")" );
			if( !succ.getInEdges().hasEdge( edge ) )
				throw new InternalReasonerException( "Invalid edge: " + edge );
			if( succ.isMerged() )
				throw new InternalReasonerException( "Invalid edge to a removed node: " + edge
						+ " " + succ.isMerged() );
			DependencySet ds = edge.getDepends();
			if( ds.max() > branch || ds.branch > branch )
				throw new InternalReasonerException( "Invalid ds: " + edge + " " + ds );
			EdgeList allEdges = node.getEdgesTo( succ );
			if( allEdges.getRoles().size() != allEdges.size() )
				throw new InternalReasonerException( "Duplicate edges: " + allEdges );
		}
		edges = node.getInEdges();
		for( int e = 0; e < edges.size(); e++ ) {
			Edge edge = edges.edgeAt( e );
			DependencySet ds = edge.getDepends();
			if( ds.max() > branch || ds.branch > branch )
				throw new InternalReasonerException( "Invalid ds: " + edge + " " + ds );
		}
	}

	/**
	 * Print the ABox as a completion tree (child nodes are indented).
	 */
	public void printTree() {
		if( !PelletOptions.PRINT_ABOX )
			return;
		System.err.println( "PRINTING..." );
		Iterator n = nodes.values().iterator();
		while( n.hasNext() ) {
			Node node = (Node) n.next();
			if( !node.isRoot() || node instanceof Literal )
				continue;
			printNode( (Individual) node, new HashSet(), "   " );
		}
	}

	/**
	 * Print the node in the completion tree.
	 * 
	 * @param node
	 * @param printed
	 * @param indent
	 */
	private void printNode(Individual node, Set printed, String indent) {
		boolean printOnlyName = (node.isNominal() && !printed.isEmpty());

		if( printed.contains( node ) ) {
			System.err.println( " " + node.getNameStr() );
			return;
		}
		else
			printed.add( node );

		if( node.isMerged() ) {
			System.err.println( node.getNameStr() + " -> " + node.getSame().getNameStr() + " "
					+ node.getMergeDependency( true ) );
			return;
		}
		else if( node.isPruned() )
			throw new InternalReasonerException( "Pruned node: " + node );

		// System.err.println(node.getNameStr());
		System.err.println( node.debugString() + " " + node.getDifferents() );

		if( printOnlyName )
			return;

		indent += "  ";
		Iterator i = node.getSuccessors().iterator();
		while( i.hasNext() ) {
			Node succ = (Node) i.next();
			EdgeList edges = node.getEdgesTo( succ );

			System.err.print( indent + "[" );
			for( int e = 0; e < edges.size(); e++ ) {
				if( e > 0 )
					System.err.print( ", " );
				System.err.print( edges.edgeAt( e ).getRole() );
			}
			System.err.print( "] " );
			if( succ instanceof Individual )
				printNode( (Individual) succ, printed, indent );
			else
				System.err.println( " (Literal) " + succ.getName() + " " + succ.getTypes() );
		}
	}

	public Clash getLastClash() {
		return lastClash;
	}

	public ABox getLastCompletion() {
		return lastCompletion;
	}

	public boolean isKeepLastCompletion() {
		return keepLastCompletion;
	}

	public void setKeepLastCompletion(boolean keepLastCompletion) {
		this.keepLastCompletion = keepLastCompletion;
	}

	public ABox getPseudoModel() {
		return pseudoModel;
	}

	/**
	 * Return the number of nodes in the ABox. This number includes both the
	 * individuals and the literals.
	 * 
	 * @return
	 */
	public int size() {
		return nodes.size();
	}

	/**
	 * Returns true if there are no individuals in the ABox.
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return nodes.isEmpty();
	}

	public void setLastCompletion(ABox comp) {
		lastCompletion = comp;
	}

	/**
	 * Set whether changes to the update should be treated as syntactic updates,
	 * i.e., if the changes are made on explicit source axioms. This is used for
	 * the completion queue for incremental consistency checking purposes.
	 * 
	 * @param boolean
	 *            val The value
	 */
	protected void setSyntacticUpdate(boolean val) {
		syntacticUpdate = val;
	}

	/**
	 * Set whether changes to the update should be treated as syntactic updates,
	 * i.e., if the changes are made on explicit source axioms. This is used for
	 * the completion queue for incremental consistency checking purposes.
	 * 
	 * @param boolean
	 *            val The value
	 */
	protected boolean isSyntacticUpdate() {
		return syntacticUpdate;
	}

	public CompletionQueue getCompletionQueue() {
		return completionQueue;
	}
}
