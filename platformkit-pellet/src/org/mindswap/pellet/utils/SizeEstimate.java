/*
 * Created on Mar 13, 2006
 */
package org.mindswap.pellet.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.Role;
import org.mindswap.pellet.exceptions.InternalReasonerException;

import aterm.ATermAppl;

public class SizeEstimate {
	protected static Log			log					= LogFactory.getLog( SizeEstimate.class );

	public static double			UNKNOWN_PROB		= 0.5;
	public static boolean			CHECK_CONCEPT_SAT	= false;

	private KnowledgeBase			kb;
	private Map<ATermAppl, Integer>	sizes;
	private Map<ATermAppl, Double>	avgs;

	public SizeEstimate(KnowledgeBase kb) {
		this.kb = kb;

		init();
	}

	private void init() {
		sizes = new HashMap<ATermAppl, Integer>();
		avgs = new HashMap<ATermAppl, Double>();

		sizes.put( ATermUtils.TOP, kb.getIndividuals().size() );
		sizes.put( ATermUtils.BOTTOM, 0 );
	}

	public void clearAll() {
		init();
	}

	public void computeAll() {
		compute( new HashSet<ATermAppl>( kb.getClasses() ), new HashSet<ATermAppl>( kb
				.getProperties() ) );
	}

	public boolean isComputed(ATermAppl term) {
		return sizes.containsKey( term );
	}

	public void compute(Collection<ATermAppl> concepts, Collection<ATermAppl> properties) {
		concepts.removeAll( sizes.keySet() );
		properties.removeAll( sizes.keySet() );

		if( concepts.isEmpty() && properties.isEmpty() ) {
			return;
		}

		Timer timer = kb.timers.startTimer( "sizeEstimate" );

		if( log.isInfoEnabled() )
			log.info( "Size estimation started" );

		Random randomGen = new Random();

		Map<ATermAppl,Integer> pSubj = new HashMap<ATermAppl,Integer>();
		Map<ATermAppl,Integer> pObj = new HashMap<ATermAppl,Integer>();

		for( Iterator i = concepts.iterator(); i.hasNext(); ) {
			ATermAppl c = (ATermAppl) i.next();

			if( kb.isRealized() )
				sizes.put( c, kb.getInstances( c ).size() );
			else {
				sizes.put( c, 0 );

				if( CHECK_CONCEPT_SAT ) {
					if( !kb.isSatisfiable( c ) )
						i.remove();
	
					if( !kb.isSatisfiable( ATermUtils.makeNot( c ) ) ) {
						i.remove();
						sizes.put( c, kb.getIndividuals().size() );
					}
				}
			}

			if( log.isDebugEnabled() )
				log.debug( "Initialize " + c + " = " + size( c ) );
		}

		for( Iterator i = properties.iterator(); i.hasNext(); ) {
			ATermAppl p = (ATermAppl) i.next();
			sizes.put( p, 0 );
			pSubj.put( p, 0 );
			pObj.put( p, 0 );
		}

		Set individuals = kb.getIndividuals();
		int count = 0;
		for( Iterator i = individuals.iterator(); i.hasNext(); ) {
			ATermAppl ind = (ATermAppl) i.next();

			float random = randomGen.nextFloat();
			if( random > PelletOptions.SAMPLING_RATIO )
				continue;

			count++;

			if( !kb.isRealized() ) {
				for( Iterator j = concepts.iterator(); j.hasNext(); ) {
					ATermAppl c = (ATermAppl) j.next();
	
					Set subs = kb.isClassified()
						? kb.getTaxonomy().getSubs( c, false, true )
						: SetUtils.EMPTY_SET;
					subs.remove( ATermUtils.BOTTOM );
	
					Bool isKnownType = kb.getABox().isKnownType( ind, c, subs );
					if( isKnownType.isTrue()
							|| (CHECK_CONCEPT_SAT && isKnownType.isUnknown() && (randomGen.nextFloat() < UNKNOWN_PROB)) )
						sizes.put( c, size( c ) + 1 );
				}
			}

			for( Iterator j = properties.iterator(); j.hasNext(); ) {
				ATermAppl p = (ATermAppl) j.next();
				Role role = kb.getRBox().getRole( p );

				Collection knowns = new HashSet();
				Collection unknowns = new HashSet();

				if( role.isObjectRole() )
					kb.getABox().getObjectPropertyValues( ind, role, (Set) knowns, (Set) unknowns,
							true );
				else
					knowns = kb.getABox().getObviousDataPropertyValues( ind, role, null );

				if( !knowns.isEmpty() ) {
					if( log.isTraceEnabled() )
						log.trace( "Update " + p + " by " + knowns.size() );
					sizes.put( p, size( p ) + knowns.size() );
					pSubj.put( p, pSubj.get( p ) + 1 );
				}

				if( role.isObjectRole() ) {
					role = role.getInverse();

					knowns = new HashSet();
					unknowns = new HashSet();

					kb.getABox().getObjectPropertyValues( ind, role, (Set) knowns, (Set) unknowns,
							true );

					if( !knowns.isEmpty() ) {
						pObj.put( p, pObj.get( p ) + 1 );
					}
				}
			}
		}

		if( !kb.isRealized() ) {
			for( Iterator i = concepts.iterator(); i.hasNext(); ) {
				ATermAppl c = (ATermAppl) i.next();
				int size = size( c );
				if( size == 0 )
					sizes.put( c, 1 );
				else
					sizes.put( c, (int) (size / PelletOptions.SAMPLING_RATIO) );
			}
		}

		for( Iterator i = properties.iterator(); i.hasNext(); ) {
			ATermAppl p = (ATermAppl) i.next();

			int size = size( p );
			if( size == 0 )
				sizes.put( p, 1 );
			else
				sizes.put( p, (int) (size / PelletOptions.SAMPLING_RATIO) );

			Role role = kb.getRBox().getRole( p );
			ATermAppl invP = (role.getInverse() != null)
				? role.getInverse().getName()
				: null;
			int subjCount = pSubj.get( p );
			if( subjCount == 0 )
				subjCount = 1;
			int objCount = pObj.get( p );
			if( objCount == 0 )
				objCount = 1;

			avgs.put( p, Double.valueOf( (double) size / subjCount ) );
			if( invP != null )
				avgs.put( invP, Double.valueOf( (double) size / objCount ) );
		}

		timer.stop();

		if( log.isDebugEnabled() ) {
			log.debug( "Sizes:" );
			log.debug( sizes );
			log.debug( "Averages:" );
			log.debug( avgs );
		}

		if( log.isInfoEnabled() ) {
			NumberFormat nf = new DecimalFormat( "0.00" );
			log.info( "Size estimation finished in " + nf.format( timer.getLast() / 1000.0 )
					+ " sec" );
		}
	}

	public int size(ATermAppl c) {
		if( !sizes.containsKey( c ) )
			throw new InternalReasonerException( "Size estimate for " + c + " is not found!" );
		return sizes.get( c );
	}

	public double avg(ATermAppl pred) {
		if( !avgs.containsKey( pred ) )
			throw new InternalReasonerException( "Average estimate for " + pred + " is not found!" );
		return avgs.get( pred );
	}
}