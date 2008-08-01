package org.mindswap.pellet.taxonomy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.CachedNode;
import org.mindswap.pellet.Expressivity;
import org.mindswap.pellet.Individual;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.tbox.TBox;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Pair;
import org.mindswap.pellet.utils.SetUtils;
import org.mindswap.pellet.utils.Timer;
import org.mindswap.pellet.utils.URIUtils;
import org.mindswap.pellet.utils.progress.ProgressMonitor;
import org.mindswap.pellet.utils.progress.SilentProgressMonitor;

import aterm.ATermAppl;
import aterm.ATermList;

/**
 * <p>
 * Title: CD Optimized Taxonomy Builder
 * </p>
 * <p>
 * Description: Taxonomy Builder implementation optimized for completely defined
 * concepts
 * </p>
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 * 
 * @author Mike Smith
 */
public class CDOptimizedTaxonomyBuilder implements TaxonomyBuilder {
	protected static Log					log				= LogFactory.getLog( Taxonomy.class );

	private ProgressMonitor					monitor			= PelletOptions.USE_CLASSIFICATION_MONITOR
																	.create();

	private static final byte				PROPOGATE_UP	= 1;
	private static final byte				NO_PROPOGATE	= 0;
	private static final byte				PROPOGATE_DOWN	= -1;

	protected Collection<ATermAppl>			classes;

	private Map<ATermAppl, Set<ATermAppl>>	toldDisjoints;

	private Map<ATermAppl, ATermList>		unionClasses;

	private Set<ATermAppl>					cyclicConcepts;

	protected Taxonomy						toldTaxonomy;

	protected Taxonomy						taxonomy;
	protected KnowledgeBase					kb;

	private int								count;
	private boolean							useCD;

	private List<TaxonomyNode>				markedNodes;

	private enum ConceptFlag {
		COMPLETELY_DEFINED, PRIMITIVE, NONPRIMITIVE, NONPRIMITIVE_TA, OTHER
	}

	private Map<ATermAppl, ConceptFlag>	conceptFlags;

	public CDOptimizedTaxonomyBuilder() {

	}

	public void setKB(KnowledgeBase kb) {
		this.kb = kb;
	}

	public void setProgressMonitor(ProgressMonitor monitor) {
		if( monitor == null )
			this.monitor = new SilentProgressMonitor();
		else
			this.monitor = monitor;
	}

	/**
	 * Classify the KB.
	 */
	public Taxonomy classify() {
		classes = kb.getClasses();

		if( kb.getClasses().isEmpty() )
			return (taxonomy = new Taxonomy());

		if( log.isInfoEnabled() ) {
			kb.timers.createTimer( "classifySub" );
			log.info( "Classes: " + (classes.size() + 2) + " Individuals: "
					+ kb.getIndividuals().size() );
		}

		prepare();

		if( log.isInfoEnabled() ) {
			log.info( "Starting classification..." );
		}

		Collection<ATermAppl> phaseOne;
		Collection<ATermAppl> phaseTwo;

		if( useCD ) {
			phaseOne = new ArrayList<ATermAppl>();
			phaseTwo = new ArrayList<ATermAppl>();
			for( ATermAppl c : classes ) {
				switch ( conceptFlags.get( c ) ) {
				case COMPLETELY_DEFINED:
				case PRIMITIVE:
				case OTHER:
					phaseOne.add( c );
					break;
				default:
					phaseTwo.add( c );
					break;
				}
			}

			if( log.isInfoEnabled() )
				log.info( "Using CD classification with phaseOne: " + phaseOne.size()
						+ " phaseTwo: " + phaseTwo.size() );
		}
		else {
			phaseOne = Collections.emptyList();
			phaseTwo = classes;

			if( log.isInfoEnabled() )
				log.info( "CD classification disabled" );
		}

		monitor.setProgressTitle( "Classifying" );
		monitor.setProgressLength( classes.size() );
		monitor.taskStarted();

		for( ATermAppl c : phaseOne ) {
			classify( c, /* requireTopSearch = */false );
			monitor.incrementProgress();
			if( monitor.isCanceled() ) {
				classes = kb.getClasses();
				return null;
			}
		}
		phaseOne = null;

		for( ATermAppl c : phaseTwo ) {
			classify( c, /* requireTopSearch = */true );
			monitor.incrementProgress();
			if( monitor.isCanceled() ) {
				classes = kb.getClasses();
				return null;
			}
		}
		phaseTwo = null;

		monitor.taskFinished();

		if( log.isInfoEnabled() ) {
			log.info( "SubClass Count: " + kb.timers.getTimer( "classifySub" ).getCount() );
			log.info( "Satisfiability Count: "
					+ (kb.getABox().satisfiabilityCount - (2 * kb.getClasses().size())) );
		}

		// Reset the class list, so the sorted copy can be gc'd
		classes = kb.getClasses();

		taxonomy.assertValid();

		return taxonomy;
	}

	private void prepare() {
		reset();

		computeToldInformation();

		createDefinitionOrder();

		computeConceptFlags();
	}

	private void reset() {
		count = 0;

		kb.prepare();
		Expressivity expr = kb.getExpressivity();
		TBox tbox = kb.getTBox();
		useCD = PelletOptions.USE_CD_CLASSIFICATION && tbox.getUC() == null
				&& tbox.unfold( ATermUtils.TOP ) == null && !expr.hasInverse()
				&& !expr.hasNominal() && !expr.hasComplexSubRoles();

		classes = new ArrayList<ATermAppl>( kb.getClasses() );

		toldDisjoints = new HashMap<ATermAppl, Set<ATermAppl>>();
		unionClasses = new HashMap<ATermAppl, ATermList>();
		markedNodes = new ArrayList<TaxonomyNode>( classes.size() );

		taxonomy = new Taxonomy();
		toldTaxonomy = new Taxonomy( classes );

		cyclicConcepts = new HashSet<ATermAppl>();

		conceptFlags = new HashMap<ATermAppl, ConceptFlag>();
	}

	private void computeToldInformation() {
		// compute told subsumers for each concept
		TBox tbox = kb.getTBox();
		Collection<ATermAppl> axioms = tbox.getAxioms();
		for( ATermAppl axiom : axioms ) {
			ATermAppl c1 = (ATermAppl) axiom.getArgument( 0 );
			ATermAppl c2 = (ATermAppl) axiom.getArgument( 1 );

			boolean equivalent = axiom.getName().equals( ATermUtils.SAME );
			Set<ATermAppl> explanation = tbox.getAxiomExplanation( axiom );

			boolean reverseArgs = !ATermUtils.isPrimitive( c1 ) && ATermUtils.isPrimitive( c2 );
			if( equivalent && reverseArgs )
				addToldRelation( c2, c1, equivalent, explanation );
			else
				addToldRelation( c1, c2, equivalent, explanation );
		}

		// for( ATermAppl c : classes ) {
		// ATermAppl desc = (ATermAppl) kb.getTBox().getUnfoldingMap().get( c );
		//
		// if( desc == null )
		// continue;
		//			
		// addToldRelation( c, desc, false );
		// }

		// additional step for union classes. for example, if we have
		// C = or(A, B)
		// and both A and B subclass of D then we can conclude C is also
		// subclass of D
		for( ATermAppl c : unionClasses.keySet() ) {
			ATermList disj = unionClasses.get( c );

			List lca = toldTaxonomy.computeLCA( disj );

			for( Iterator j = lca.iterator(); j.hasNext(); ) {
				ATermAppl d = (ATermAppl) j.next();

				if( log.isDebugEnabled() )
					log.debug( "Union subsumption " + getName( c ) + " " + getName( d ) );

				addToldSubsumer( c, d );
			}
		}

		// we don't need this any more so set it null and let GC claim it
		unionClasses = null;

		toldTaxonomy.assertValid();

	}

	private void createDefinitionOrder() {
		Timer t = kb.timers.startTimer( "createDefinitionOrder" );

		Taxonomy definitionOrderTaxonomy = new Taxonomy( classes );
		for( ATermAppl c : classes ) {
			List<Pair<ATermAppl, Set<ATermAppl>>> desc = kb.getTBox().unfold( c );

			if( desc == null )
				continue;

			for( Pair<ATermAppl, Set<ATermAppl>> pair : desc ) {
				Set<ATermAppl> usedByC = ATermUtils.findPrimitives( pair.first, true, true );
				for( ATermAppl d : usedByC ) {
					if( c.equals( d ) )
						continue;

					TaxonomyNode cNode = definitionOrderTaxonomy.getNode( c );
					TaxonomyNode dNode = definitionOrderTaxonomy.getNode( d );
					if( cNode == null )
						throw new InternalReasonerException( c + " is not in the definition order" );
					else if( cNode.equals( toldTaxonomy.getTop() ) )
						definitionOrderTaxonomy.merge( cNode, dNode );
					else {
						dNode.addSub( cNode );
						definitionOrderTaxonomy.removeCycles( cNode );
					}
				}
			}
		}

		definitionOrderTaxonomy.assertValid();

		classes = definitionOrderTaxonomy.topologocialSort( true );

		cyclicConcepts = new HashSet<ATermAppl>();
		for( TaxonomyNode node : definitionOrderTaxonomy.getNodes() ) {
			Set<ATermAppl> names = node.getEquivalents();
			if( names.size() > 1 )
				cyclicConcepts.addAll( names );
		}

		if( log.isDebugEnabled() )
			log.debug( "Cyclic concepts (" + cyclicConcepts.size() + "): " + cyclicConcepts );

		if( log.isDebugEnabled() )
			log.debug( "Sorted: " + classes );

		t.stop();
	}

	private void computeConceptFlags() {
		if( !useCD )
			return;

		/*
		 * Use RBox domain axioms to mark some concepts as complex
		 */
		for( ATermAppl p : (Set<ATermAppl>) kb.getRBox().getRoleNames() ) {
			for( ATermAppl c : (Set<ATermAppl>) kb.getDomains( p ) ) {
				conceptFlags.put( c, ConceptFlag.OTHER );
			}
		}

		/*
		 * Iterate over the post-absorption unfolded class descriptions to set
		 * concept flags The iteration needs to be over classes to include
		 * orphans
		 */
		TBox tbox = kb.getTBox();
		for( ATermAppl c : classes ) {

			List<Pair<ATermAppl, Set<ATermAppl>>> desc = kb.getTBox().unfold( c );

			if( tbox.unfold( ATermUtils.makeNot( c ) ) != null || cyclicConcepts.contains( c ) ) {
				conceptFlags.put( c, ConceptFlag.NONPRIMITIVE );
				for( Pair<ATermAppl, Set<ATermAppl>> pair : desc ) {
					for( ATermAppl d : (Set<ATermAppl>) ATermUtils.findPrimitives( pair.first ) ) {
						ConceptFlag current = conceptFlags.get( d );
						if( current == null || current == ConceptFlag.COMPLETELY_DEFINED )
							conceptFlags.put( d, ConceptFlag.PRIMITIVE );
					}
				}
				continue;
			}

			boolean flagged = false;
			for( ATermAppl sup : (Set<ATermAppl>) toldTaxonomy.getSupers( c, true, true ) ) {
				ConceptFlag supFlag = conceptFlags.get( sup );
				if( (supFlag == ConceptFlag.NONPRIMITIVE)
						|| (supFlag == ConceptFlag.NONPRIMITIVE_TA) ) {
					conceptFlags.put( c, ConceptFlag.NONPRIMITIVE_TA );
					flagged = true;
					break;
				}
			}
			if( flagged )
				continue;

			/*
			 * The concept may have appeared in the definition of a
			 * non-primitive or, it may already have an 'OTHER' flag.
			 */
			if( conceptFlags.get( c ) != null )
				continue;

			conceptFlags.put( c, isCDDesc( desc )
				? ConceptFlag.COMPLETELY_DEFINED
				: ConceptFlag.PRIMITIVE );
		}

		if( log.isInfoEnabled() ) {

			int cd = 0;
			int p = 0;
			int np = 0;
			int npta = 0;
			int other = 0;

			for( ATermAppl c : classes ) {
				switch ( conceptFlags.get( c ) ) {
				case COMPLETELY_DEFINED:
					cd++;
					break;
				case PRIMITIVE:
					p++;
					break;
				case NONPRIMITIVE:
					np++;
					break;
				case NONPRIMITIVE_TA:
					npta++;
					break;
				case OTHER:
					other++;
					break;
				default:
					log.warn( c.getName() + " has no classification flag" );
					break;
				}
			}

			log.info( "CD,P,NP,NPTA,O: " + cd + "," + p + "," + np + "," + npta + "," + other );
		}
	}

	private void clearMarks() {
		for( TaxonomyNode n : markedNodes )
			n.mark = null;

		markedNodes.clear();
	}

	private boolean isCDDesc(List<Pair<ATermAppl, Set<ATermAppl>>> desc) {
		if( desc != null ) {
			for( Pair<ATermAppl, Set<ATermAppl>> pair : desc ) {
				if( !isCDDesc( pair.first ) )
					return false;
			}
		}

		return true;
	}

	private boolean isCDDesc(ATermAppl desc) {
		if( desc == null )
			return true;

		if( ATermUtils.isPrimitive( desc ) )
			return true;

		if( ATermUtils.isAllValues( desc ) )
			return true;

		if( ATermUtils.isAnd( desc ) ) {
			boolean allCDConj = true;
			ATermList conj = (ATermList) desc.getArgument( 0 );
			for( ATermList subConj = conj; allCDConj && !subConj.isEmpty(); subConj = subConj
					.getNext() ) {
				ATermAppl ci = (ATermAppl) subConj.getFirst();
				allCDConj = isCDDesc( ci );
			}
			return allCDConj;
		}

		if( ATermUtils.isNot( desc ) ) {
			ATermAppl negd = (ATermAppl) desc.getArgument( 0 );

			if( ATermUtils.isPrimitive( negd ) )
				return true;

		}

		return false;
	}

	private void addToldRelation(ATermAppl c, ATermAppl d, boolean equivalent,
			Set<ATermAppl> explanation) {

		if( !equivalent && ((c == ATermUtils.BOTTOM) || (d == ATermUtils.TOP)) )
			return;

		if( !ATermUtils.isPrimitive( c ) ) {
			if( c.getAFun().equals( ATermUtils.ORFUN ) ) {
				ATermList list = (ATermList) c.getArgument( 0 );
				for( ATermList disj = list; !disj.isEmpty(); disj = disj.getNext() ) {
					ATermAppl e = (ATermAppl) disj.getFirst();
					addToldRelation( e, d, false, explanation );
				}
			}
			else if( c.getAFun().equals( ATermUtils.NOTFUN ) ) {
				if( ATermUtils.isPrimitive( d ) ) {
					ATermAppl negation = (ATermAppl) c.getArgument( 0 );

					addToldDisjoint( d, negation );
					addToldDisjoint( negation, d );
				}
			}
		}
		else if( ATermUtils.isPrimitive( d ) ) {
			if( d.getName().startsWith( PelletOptions.BNODE ) )
				return;

			if( !equivalent ) {
				if( log.isDebugEnabled() )
					log.debug( "Preclassify (1) " + getName( c ) + " " + getName( d ) );

				addToldSubsumer( c, d, explanation );
			}
			else {
				if( log.isDebugEnabled() )
					log.debug( "Preclassify (2) " + getName( c ) + " " + getName( d ) );

				addToldEquivalent( c, d );
			}
		}
		else if( d.getAFun().equals( ATermUtils.ANDFUN ) ) {
			for( ATermList conj = (ATermList) d.getArgument( 0 ); !conj.isEmpty(); conj = conj
					.getNext() ) {
				ATermAppl e = (ATermAppl) conj.getFirst();
				addToldRelation( c, e, false, explanation );
			}
		}
		else if( d.getAFun().equals( ATermUtils.ORFUN ) ) {
			boolean allPrimitive = true;

			ATermList list = (ATermList) d.getArgument( 0 );
			for( ATermList disj = list; !disj.isEmpty(); disj = disj.getNext() ) {
				ATermAppl e = (ATermAppl) disj.getFirst();
				if( ATermUtils.isPrimitive( e ) ) {
					if( equivalent ) {
						if( log.isDebugEnabled() )
							log.debug( "Preclassify (3) " + getName( c ) + " " + getName( e ) );

						addToldSubsumer( e, c );
					}
				}
				else
					allPrimitive = false;
			}

			if( allPrimitive )
				unionClasses.put( c, list );
		}
		else if( d.equals( ATermUtils.BOTTOM ) ) {
			if( log.isDebugEnabled() )
				log.debug( "Preclassify (4) " + getName( c ) + " BOTTOM" );
			addToldEquivalent( c, ATermUtils.BOTTOM );
		}
		else if( d.getAFun().equals( ATermUtils.NOTFUN ) ) {
			// handle case sub(a, not(b)) which implies sub[a][b] is false
			ATermAppl negation = (ATermAppl) d.getArgument( 0 );
			if( ATermUtils.isPrimitive( negation ) ) {
				if( log.isDebugEnabled() )
					log.debug( "Preclassify (5) " + getName( c ) + " not " + getName( negation ) );

				addToldDisjoint( c, negation );
				addToldDisjoint( negation, c );
			}
		}
	}

	private void addToldEquivalent(ATermAppl c, ATermAppl d) {
		if( c.equals( d ) )
			return;

		TaxonomyNode cNode = toldTaxonomy.getNode( c );
		TaxonomyNode dNode = toldTaxonomy.getNode( d );

		toldTaxonomy.merge( cNode, dNode );
	}

	private void addToldSubsumer(ATermAppl c, ATermAppl d) {
		addToldSubsumer( c, d, null );
	}

	private void addToldSubsumer(ATermAppl c, ATermAppl d, Set<ATermAppl> explanation) {
		TaxonomyNode cNode = toldTaxonomy.getNode( c );
		TaxonomyNode dNode = toldTaxonomy.getNode( d );

		if( cNode == null )
			throw new InternalReasonerException( c + " is not in the definition order" );

		if( dNode == null )
			throw new InternalReasonerException( d + " is not in the definition order" );

		if( cNode.equals( dNode ) )
			return;

		if( cNode.equals( toldTaxonomy.getTop() ) )
			toldTaxonomy.merge( cNode, dNode );
		else {
			dNode.addSub( cNode );
			toldTaxonomy.removeCycles( cNode );
			if( explanation != null )
				cNode.addSuperExplanation( dNode, explanation );
		}
	}

	private void addToldDisjoint(ATermAppl c, ATermAppl d) {
		Set<ATermAppl> disjoints = toldDisjoints.get( c );
		if( disjoints == null ) {
			disjoints = new HashSet<ATermAppl>();
			toldDisjoints.put( c, disjoints );
		}
		disjoints.add( d );
	}

	private void markToldSubsumers(ATermAppl c) {
		TaxonomyNode node = taxonomy.getNode( c );
		if( node != null ) {
			boolean newMark = mark( node, Boolean.TRUE, PROPOGATE_UP );
			if( !newMark )
				return;
		}
		else if( log.isInfoEnabled() && markedNodes.size() > 2 )
			log.warn( "Told subsumer " + c + " is not classified yet" );

		if( toldTaxonomy.contains( c ) ) {
			// TODO just getting direct supers and letting recursion handle rest
			// might be more efficient
			Set supers = toldTaxonomy.getSupers( c, true, true );
			for( Iterator i = supers.iterator(); i.hasNext(); ) {
				ATermAppl sup = (ATermAppl) i.next();
				markToldSubsumers( sup );
			}
		}
	}

	private void markToldSubsumeds(ATermAppl c, Boolean b) {
		TaxonomyNode node = taxonomy.getNode( c );
		if( node != null ) {
			boolean newMark = mark( node, b, PROPOGATE_DOWN );
			if( !newMark )
				return;
		}

		if( toldTaxonomy.contains( c ) ) {
			Set subs = toldTaxonomy.getSubs( c, true, true );
			for( Iterator i = subs.iterator(); i.hasNext(); ) {
				ATermAppl sub = (ATermAppl) i.next();
				markToldSubsumeds( sub, b );
			}
		}
	}

	private void markToldDisjoints(Collection<ATermAppl> inputc, boolean topSearch) {

		Set<ATermAppl> cset = new HashSet<ATermAppl>();
		cset.addAll( inputc );

		for( ATermAppl c : inputc ) {
			if( taxonomy.contains( c ) )
				cset.addAll( taxonomy.getSupers( c, false, true ) );

			if( toldTaxonomy.contains( c ) )
				cset.addAll( toldTaxonomy.getSupers( c, false, true ) );
		}

		Set<ATermAppl> disjoints = new HashSet<ATermAppl>();
		for( ATermAppl a : cset ) {
			Set<ATermAppl> disj = toldDisjoints.get( a );
			if( disj != null )
				disjoints.addAll( disj );
		}

		if( topSearch ) {
			for( ATermAppl d : disjoints ) {
				TaxonomyNode node = taxonomy.getNode( d );
				if( node != null )
					mark( node, Boolean.FALSE, NO_PROPOGATE );
			}
		}
		else {
			for( ATermAppl d : disjoints ) {
				markToldSubsumeds( d, Boolean.FALSE );
			}
		}
	}

	private TaxonomyNode checkSatisfiability(ATermAppl c) {
		if( log.isDebugEnabled() )
			log.debug( "Satisfiable " );

		Timer t = kb.timers.startTimer( "classifySat" );
		boolean isSatisfiable = kb.getABox().isSatisfiable( c, true );
		t.stop();

		if( log.isDebugEnabled() )
			log.debug( (isSatisfiable
				? "true"
				: "*****FALSE*****") + " (" + t.getLast() + "ms)" );

		if( !isSatisfiable )
			taxonomy.addEquivalentNode( c, taxonomy.getBottom() );

		if( PelletOptions.USE_CACHING ) {
			if( log.isDebugEnabled() )
				log.debug( "...negation " );
			
			t.start();
			ATermAppl notC = ATermUtils.makeNot( c );
			isSatisfiable = kb.getABox().isSatisfiable( notC, true );
			t.stop();

			if( !isSatisfiable )
				taxonomy.addEquivalentNode( c, taxonomy.getTop() );

			if( log.isDebugEnabled() )
				log.debug( isSatisfiable + " (" + t.getLast() + "ms)" );
		}

		return taxonomy.getNode( c );
	}

	/**
	 * Add a new concept to the already classified taxonomy
	 */
	public void classify(ATermAppl c) {
		classify( c, /* requireTopSearch = */true );
	}

	private TaxonomyNode classify(ATermAppl c, boolean requireTopSearch) {

		if( log.isDebugEnabled() )
			log.debug( "Classify (" + (++count) + ") " + getName( c ) + "..." );

		boolean skipTopSearch;
		boolean skipBottomSearch;

		TaxonomyNode node = taxonomy.getNode( c );
		if( node != null )
			return node;

		node = checkSatisfiability( c );
		if( node != null )
			return node;

		clearMarks();

		List<TaxonomyNode> supers;
		List<TaxonomyNode> subs;

		ConceptFlag flag = conceptFlags.get( c );

		// FIXME: There may be a better thing to do here...
		if( flag == null )
			flag = ConceptFlag.OTHER;

		skipTopSearch = !requireTopSearch && useCD && (flag == ConceptFlag.COMPLETELY_DEFINED);

		if( skipTopSearch ) {

			supers = getCDSupers( c );
			skipBottomSearch = true;

		}
		else {

			supers = doTopSearch( c );
			skipBottomSearch = useCD
					&& ((flag == ConceptFlag.PRIMITIVE) && (flag == ConceptFlag.COMPLETELY_DEFINED));
		}

		if( skipBottomSearch )
			subs = Collections.singletonList( taxonomy.getBottom() );
		else {
			if( supers.size() == 1 ) {
				TaxonomyNode supNode = supers.iterator().next();

				/*
				 * if i has only one super class j and j is a subclass of i then
				 * it means i = j. There is no need to classify i since we
				 * already know everything about j
				 */
				ATermAppl sup = supNode.getName();
				if( subsumes( c, sup ) ) {
					if( log.isDebugEnabled() )
						log.debug( getName( c ) + " = " + getName( sup ) );

					taxonomy.addEquivalentNode( c, supNode );
					return supNode;
				}
			}

			subs = doBottomSearch( c, supers );
		}

		node = taxonomy.addNode( c );
		node.addSupers( supers );
		node.addSubs( subs );
		node.removeMultiplePaths();

		/*
		 * For told relations maintain explanations.
		 */
		TaxonomyNode toldNode = toldTaxonomy.getNode( c );
		if( toldNode != null ) {
			// Add the told equivalents to the taxonomy
			TaxonomyNode defOrder = toldTaxonomy.getNode( c );
			for( ATermAppl eq : (Set<ATermAppl>) defOrder.getEquivalents() ) {
				taxonomy.addEquivalentNode( eq, node );
			}

			for( TaxonomyNode n : supers ) {
				TaxonomyNode supTold = toldTaxonomy.getNode( n.getName() );
				Set<Set<ATermAppl>> exps = toldNode.getSuperExplanations( supTold );
				if( exps != null )
					for( Set<ATermAppl> exp : exps )
						node.addSuperExplanation( n, exp );
			}
		}

		if( log.isDebugEnabled() )
			log.debug( "Subsumption Count: " + kb.getABox().satisfiabilityCount );

		return node;
	}

	private List<TaxonomyNode> doBottomSearch(ATermAppl c, List<TaxonomyNode> supers) {

		Set<TaxonomyNode> searchFrom = new HashSet<TaxonomyNode>();
		for( TaxonomyNode sup : supers )
			collectLeafs( sup, searchFrom );

		if( searchFrom.isEmpty() )
			return Collections.singletonList( taxonomy.getBottom() );

		clearMarks();

		mark( taxonomy.getTop(), Boolean.FALSE, NO_PROPOGATE );
		taxonomy.getBottom().mark = Boolean.TRUE;
		markToldSubsumeds( c, Boolean.TRUE );
		for( TaxonomyNode sup : supers )
			mark( sup, Boolean.FALSE, NO_PROPOGATE );

		log.debug( "Bottom search..." );

		List<TaxonomyNode> subs = new ArrayList<TaxonomyNode>();
		Set<TaxonomyNode> visited = new HashSet<TaxonomyNode>();
		for( TaxonomyNode n : searchFrom )
			if( subsumed( n, c ) )
				search( /* topSearch = */false, c, n, visited, subs );

		if( subs.isEmpty() )
			return Collections.singletonList( taxonomy.getBottom() );

		return subs;
	}

	private void collectLeafs(TaxonomyNode node, Collection<TaxonomyNode> leafs) {
		for( TaxonomyNode sub : node.getSubs() ) {
			if( sub.isLeaf() )
				leafs.add( sub );
			else
				collectLeafs( sub, leafs );
		}
	}

	private List<TaxonomyNode> doTopSearch(ATermAppl c) {

		List<TaxonomyNode> supers = new ArrayList<TaxonomyNode>();

		mark( taxonomy.getTop(), Boolean.TRUE, NO_PROPOGATE );
		taxonomy.getBottom().mark = Boolean.FALSE;
		markToldSubsumers( c );
		markToldDisjoints( Collections.singleton( c ), true );

		log.debug( "Top search..." );

		search( true, c, taxonomy.getTop(), new HashSet<TaxonomyNode>(), supers );

		return supers;
	}

	private List<TaxonomyNode> getCDSupers(ATermAppl c) {

		/*
		 * Find all of told subsumers already classified and not redundant
		 */
		List<TaxonomyNode> supers = new ArrayList<TaxonomyNode>();

		// FIXME: Handle or rule out the null case
		TaxonomyNode defOrder = toldTaxonomy.getNode( c );
		List<TaxonomyNode> cDefs = defOrder.getSupers();

		int nTS = cDefs.size();
		if( nTS > 1 ) {
			if( nTS == 2 ) {
				for( TaxonomyNode def : cDefs ) {
					if( def == toldTaxonomy.getTop() )
						continue;
					TaxonomyNode parent = taxonomy.getNode( def.getName() );
					if( parent == null )
						throw new RuntimeException( "CD classification of " + getName( c )
								+ " failed, told subsumer " + getName( def.getName() )
								+ " not classified" );
					supers.add( parent );
					break;
				}
			}
			else {
				for( TaxonomyNode def : cDefs ) {
					if( def == toldTaxonomy.getTop() )
						continue;
					TaxonomyNode candidate = taxonomy.getNode( def.getName() );
					if( candidate == null )
						throw new RuntimeException( "CD classification of " + getName( c )
								+ " failed, told subsumer " + getName( def.getName() )
								+ " not classified" );

					for( TaxonomyNode ancestor : (List<TaxonomyNode>) candidate.getSupers() ) {
						mark( ancestor, Boolean.TRUE, PROPOGATE_UP );
					}
				}
				for( TaxonomyNode def : cDefs ) {
					if( def == toldTaxonomy.getTop() )
						continue;
					TaxonomyNode candidate = taxonomy.getNode( def.getName() );
					if( candidate.mark == null ) {
						supers.add( candidate );
						if( log.isDebugEnabled() )
							log.debug( "...completely defined by " + candidate.getName().getName() );
					}

				}
			}
		}

		if( supers.isEmpty() )
			supers.add( taxonomy.getTop() );

		return supers;
	}

	private Collection<TaxonomyNode> search(boolean topSearch, ATermAppl c, TaxonomyNode x,
			Set<TaxonomyNode> visited, List<TaxonomyNode> result) {
		List<TaxonomyNode> posSucc = new ArrayList<TaxonomyNode>();
		visited.add( x );

		List<TaxonomyNode> list = topSearch
			? x.getSubs()
			: x.getSupers();

		for( TaxonomyNode next : list ) {

			if( topSearch ) {
				if( subsumes( next, c ) )
					posSucc.add( next );
			}
			else {
				if( subsumed( next, c ) )
					posSucc.add( next );
			}
		}

		if( posSucc.isEmpty() ) {
			result.add( x );
		}
		else {
			for( TaxonomyNode y : posSucc ) {
				if( !visited.contains( y ) )
					search( topSearch, c, y, visited, result );
			}
		}

		return result;
	}

	private boolean subCheckWithCache(TaxonomyNode node, ATermAppl c, boolean topDown) {

		Boolean cached = node.mark;
		if( cached != null )
			return cached.booleanValue();

		/*
		 * Search ancestors for marks to propogate
		 */
		List<TaxonomyNode> others = topDown
			? node.getSupers()
			: node.getSubs();

		if( others.size() > 1 ) {
			Map<TaxonomyNode, TaxonomyNode> visited = new LinkedHashMap<TaxonomyNode, TaxonomyNode>();
			visited.put( node, null );

			Map<TaxonomyNode, TaxonomyNode> toBeVisited = new LinkedHashMap<TaxonomyNode, TaxonomyNode>();
			for( TaxonomyNode n : others ) {
				toBeVisited.put( n, node );
			}

			while( !toBeVisited.isEmpty() ) {
				TaxonomyNode relative = toBeVisited.keySet().iterator().next();
				TaxonomyNode reachedFrom = toBeVisited.get( relative );

				Boolean ancestorMark = relative.mark;
				if( ancestorMark == Boolean.FALSE ) {
					for( TaxonomyNode n = reachedFrom; n != null; n = visited.get( n ) ) {
						mark( n, Boolean.FALSE, NO_PROPOGATE );
					}
					return false;
				}

				if( ancestorMark == null ) {
					List<TaxonomyNode> moreRelatives = topDown
						? relative.getSupers()
						: relative.getSubs();
					for( TaxonomyNode n : moreRelatives ) {
						if( !visited.keySet().contains( n ) && !toBeVisited.keySet().contains( n ) ) {
							toBeVisited.put( n, relative );
						}
					}
				}
				toBeVisited.remove( relative );
				visited.put( relative, reachedFrom );
			}
		}

		// check subsumption
		boolean calcdMark = topDown
			? subsumes( node.getName(), c )
			: subsumes( c, node.getName() );
		// mark the node appropriately
		mark( node, Boolean.valueOf( calcdMark ), NO_PROPOGATE );

		return calcdMark;
	}

	private boolean subsumes(TaxonomyNode node, ATermAppl c) {
		return subCheckWithCache( node, c, true );
	}

	private boolean subsumed(TaxonomyNode node, ATermAppl c) {
		return subCheckWithCache( node, c, false );
	}

	private boolean mark(TaxonomyNode node, Boolean value, byte propogate) {
		if( node.getEquivalents().contains( ATermUtils.BOTTOM ) )
			return true;

		if( node.mark != null ) {
			if( node.mark != value )
				throw new RuntimeException( "Inconsistent classification result " + node.getName()
						+ " " + node.mark + " " + value );
			else
				return false;
		}
		node.mark = value;
		markedNodes.add( node );

		if( propogate != NO_PROPOGATE ) {
			List<TaxonomyNode> others = (propogate == PROPOGATE_UP)
				? node.getSupers()
				: node.getSubs();
			for( TaxonomyNode n : others )
				mark( n, value, propogate );
		}

		return true;
	}

	private boolean subsumes(ATermAppl sup, ATermAppl sub) {
		long time = 0, count = 0;
		if( log.isDebugEnabled() ) {
			time = System.currentTimeMillis();
			count = kb.getABox().satisfiabilityCount;
			log
					.debug( "Subsumption testing for [" + getName( sub ) + "," + getName( sup )
							+ "]..." );
		}

		Timer t = kb.timers.startTimer( "classifySub" );
		boolean result = kb.getABox().isSubClassOf( sub, sup );
		t.stop();

		if( log.isDebugEnabled() ) {
			String sign = (kb.getABox().satisfiabilityCount > count)
				? "+"
				: "-";
			time = System.currentTimeMillis() - time;
			log.debug( " done (" + (result
				? "+"
				: "-") + ") (" + sign + time + "ms)" );
		}

		return result;
	}

	private void mark(Set<ATermAppl> set, Map<ATermAppl, Boolean> marked, Boolean value) {
		for( Iterator i = set.iterator(); i.hasNext(); ) {
			ATermAppl c = (ATermAppl) i.next();

			marked.put( c, value );
		}
	}

	/**
	 * Realize the KB by finding the instances of each class.
	 */
	public Taxonomy realize() {
		monitor.setProgressTitle( "Realizing" );

		return PelletOptions.REALIZE_INDIVIDUAL_AT_A_TIME
			? realizeByIndividuals()
			: realizeByConcepts();
	}

	private Taxonomy realizeByIndividuals() {
		monitor.setProgressLength( kb.getIndividuals().size() );
		monitor.taskStarted();

		Iterator i = kb.getABox().getIndIterator();
		for( int count = 0; i.hasNext(); count++ ) {
			Individual x = (Individual) i.next();

			monitor.incrementProgress();
			if( monitor.isCanceled() )
				return null;

			if( log.isDebugEnabled() )
				log.debug( count + ") Realizing " + getName( x.getName() ) + " " );

			Map marked = new HashMap();

			List obviousTypes = new ArrayList();
			List obviousNonTypes = new ArrayList();

			kb.getABox().getObviousTypes( x.getName(), obviousTypes, obviousNonTypes );

			for( Iterator j = obviousTypes.iterator(); j.hasNext(); ) {
				ATermAppl c = (ATermAppl) j.next();

				// since nominals can be returned by getObviousTypes
				// we need the following check
				if( !taxonomy.contains( c ) )
					continue;

				mark( taxonomy.getAllEquivalents( c ), marked, Boolean.TRUE );
				mark( taxonomy.getSupers( c, true, true ), marked, Boolean.TRUE );

				// FIXME: markToldDisjoints operates on a map key'd with
				// TaxonomyNodes, not ATermAppls
				// markToldDisjoints( c, false );
			}

			for( Iterator j = obviousNonTypes.iterator(); j.hasNext(); ) {
				ATermAppl c = (ATermAppl) j.next();

				mark( taxonomy.getAllEquivalents( c ), marked, Boolean.FALSE );
				mark( taxonomy.getSubs( c, true, true ), marked, Boolean.FALSE );
			}

			realizeByIndividual( x.getName(), ATermUtils.TOP, marked );
		}

		monitor.taskFinished();

		return taxonomy;
	}

	private boolean realizeByIndividual(ATermAppl n, ATermAppl c, Map marked) {
		boolean realized = false;

		if( c.equals( ATermUtils.BOTTOM ) )
			return false;

		boolean isType;
		if( marked.containsKey( c ) ) {
			isType = ((Boolean) marked.get( c )).booleanValue();
		}
		else {
			long time = 0, count = 0;
			if( log.isDebugEnabled() ) {
				time = System.currentTimeMillis();
				count = kb.getABox().consistencyCount;
				log.debug( "Type checking for [" + getName( n ) + ", " + getName( c ) + "]..." );
			}

			Timer t = kb.timers.startTimer( "classifyType" );
			isType = kb.isType( n, c );
			t.stop();
			marked.put( c, isType
				? Boolean.TRUE
				: Boolean.FALSE );

			if( log.isDebugEnabled() ) {
				String sign = (kb.getABox().consistencyCount > count)
					? "+"
					: "-";
				time = System.currentTimeMillis() - time;
				log.debug( "done (" + (isType
					? "+"
					: "-") + ") (" + sign + time + "ms)" );
			}
		}

		if( isType ) {
			TaxonomyNode node = taxonomy.getNode( c );

			Iterator subs = node.getSubs().iterator();
			while( subs.hasNext() ) {
				TaxonomyNode sub = (TaxonomyNode) subs.next();
				ATermAppl d = sub.getName();

				realized = realizeByIndividual( n, d, marked ) || realized;
			}

			// this concept is the most specific concept x belongs to
			// so add it here and return true
			if( !realized ) {
				taxonomy.getNode( c ).addInstance( n );
				realized = true;
			}
		}

		return realized;
	}

	private Taxonomy realizeByConcepts() {
		monitor.setProgressLength( classes.size() + 2 );
		monitor.taskStarted();

		realizeByConcept( ATermUtils.TOP, kb.getIndividuals() );

		if( monitor.isCanceled() )
			return null;

		monitor.taskFinished();

		return taxonomy;
	}

	private Set realizeByConcept(ATermAppl c, Collection individuals) {
		if( c.equals( ATermUtils.BOTTOM ) )
			return SetUtils.EMPTY_SET;

		monitor.incrementProgress();
		if( monitor.isCanceled() )
			return null;

		if( log.isDebugEnabled() )
			log.debug( "Realizing concept " + c );

		Set instances = new HashSet( kb.retrieve( c, individuals ) );
		Set mostSpecificInstances = new HashSet( instances );

		if( !instances.isEmpty() ) {
			TaxonomyNode node = taxonomy.getNode( c );

			Iterator subs = node.getSubs().iterator();
			while( subs.hasNext() ) {
				TaxonomyNode sub = (TaxonomyNode) subs.next();
				ATermAppl d = sub.getName();

				Set subInstances = realizeByConcept( d, instances );

				mostSpecificInstances.removeAll( subInstances );
			}

			if( !mostSpecificInstances.isEmpty() )
				node.setInstances( mostSpecificInstances );
		}

		return instances;
	}

	public void printStats() {
		int numClasses = classes.size();
		System.out.println( "Num of Classes: " + numClasses + " Pairs: "
				+ (numClasses * numClasses) + " Subsumption Count: "
				+ kb.getABox().satisfiabilityCount );
	}

	private String getName(ATermAppl c) {
		if( c.equals( ATermUtils.TOP ) )
			return "owl:Thing";
		else if( c.equals( ATermUtils.BOTTOM ) )
			return "owl:Nothing";
		else if( ATermUtils.isPrimitive( c ) )
			return URIUtils.getLocalName( c.getName() );
		else
			return c.toString();
	}

}