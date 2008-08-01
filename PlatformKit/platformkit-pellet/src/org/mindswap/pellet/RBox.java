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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.taxonomy.Taxonomy;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Pair;
import org.mindswap.pellet.utils.SetUtils;
import org.mindswap.pellet.utils.fsm.State;
import org.mindswap.pellet.utils.fsm.TransitionGraph;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;

public class RBox {
	public static Log				log				= LogFactory.getLog( RBox.class );

	private Map<ATermAppl, Role>	roles			= new HashMap<ATermAppl, Role>();
	private Set<Role>				reflexiveRoles	= new HashSet<Role>();

	private Taxonomy				taxonomy;

	/**
	 * @deprecated Simply ignore invalid statements
	 */
	boolean							consistent		= true;

	public RBox() {
	}

	/**
	 * Return the role with the given name
	 * 
	 * @param r
	 *            Name (URI) of the role
	 * @return
	 */
	public Role getRole(ATerm r) {
		return (Role) roles.get( r );
	}

	/**
	 * Return the role with the given name and throw and exception if it is not
	 * found.
	 * 
	 * @param r
	 *            Name (URI) of the role
	 * @return
	 */
	public Role getDefinedRole(ATerm r) {
		Role role = (Role) roles.get( r );

		if( role == null )
			throw new RuntimeException( r + " is not defined as a property" );

		return role;
	}

	/**
	 * @deprecated Inconsistent axioms are rejected
	 */
	public boolean isConsistent() {
		return consistent;
	}

	public Role addRole(ATermAppl r) {
		Role role = getRole( r );

		if( role == null ) {
			role = new Role( r, Role.UNTYPED );
			roles.put( r, role );
		}

		return role;
	}

	public Role addObjectRole(ATermAppl r) {
		Role role = getRole( r );
		int roleType = (role == null)
			? Role.UNTYPED
			: role.getType();

		switch ( roleType ) {
		case Role.DATATYPE:
			role = null;
			break;
		case Role.OBJECT:
			break;
		default:
			if( role == null ) {
				role = new Role( r, Role.OBJECT );
				roles.put( r, role );
			}
			else {
				role.setType( Role.OBJECT );
			}

			ATermAppl invR = ATermUtils.makeInv( r );
			Role invRole = new Role( invR, Role.OBJECT );
			roles.put( invR, invRole );

			role.setInverse( invRole );
			invRole.setInverse( role );
			break;
		}

		return role;
	}

	public Role addDatatypeRole(ATermAppl r) {
		Role role = getRole( r );

		if( role == null ) {
			role = new Role( r, Role.DATATYPE );
			roles.put( r, role );
		}
		else {
			switch ( role.getType() ) {
			case Role.DATATYPE:
				break;
			case Role.OBJECT:
				role = null;
				break;
			default:
				role.setType( Role.DATATYPE );
				break;
			}
		}

		return role;
	}

	public Role addAnnotationRole(ATermAppl r) {
		Role role = getRole( r );

		if( role == null ) {
			role = new Role( r, Role.ANNOTATION );
			roles.put( r, role );
		}
		else if( role.getType() == Role.UNTYPED ) {
			role.setType( Role.ANNOTATION );
		}
		else if( role.getType() != Role.ANNOTATION ) {
			role = null;
		}

		return role;
	}

	public Role addOntologyRole(ATermAppl r) {
		Role role = getRole( r );

		if( role == null ) {
			role = new Role( r, Role.ONTOLOGY );
			roles.put( r, role );
		}
		else if( role.getType() == Role.UNTYPED ) {
			role.setType( Role.ONTOLOGY );
		}
		else if( role.getType() != Role.ONTOLOGY ) {
			role = null;
		}

		return role;
	}

	public boolean addSubRole(ATerm sub, ATerm sup) {
		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( ATermUtils.makeSubProp( sub, sup ) )
			: DependencySet.INDEPENDENT;
		return addSubRole( sub, sup, ds );
	}

	public boolean addSubRole(ATerm sub, ATerm sup, DependencySet ds) {
		Role roleSup = getRole( sup );
		Role roleSub = getRole( sub );

		if( roleSup == null )
			return false;
		else if( sub.getType() == ATerm.LIST )
			roleSup.addSubRoleChain( (ATermList) sub, ds );
		else if( roleSub == null )
			return false;
		else {
			roleSup.addSubRole( roleSub, ds );
			roleSub.addSuperRole( roleSup, ds );
		}

		// TODO Need to figure out what to do about about role lists
		// explanationTable.add(ATermUtils.makeSub(sub, sup), ds);
		return true;
	}

	/**
	 * @deprecated Use addEquivalentRole instead
	 */
	public boolean addSameRole(ATerm s, ATerm r) {
		return addEquivalentRole( s, r );
	}

	public boolean addEquivalentRole(ATerm s, ATerm r) {
		DependencySet ds = PelletOptions.USE_TRACING
			? new DependencySet( ATermUtils.makeEqProp( r, s ) )
			: DependencySet.INDEPENDENT;
		return addEquivalentRole( r, s, ds );
	}

	public boolean addEquivalentRole(ATerm s, ATerm r, DependencySet ds) {
		Role roleS = getRole( s );
		Role roleR = getRole( r );

		if( roleS == null || roleR == null )
			return false;

		roleR.addSubRole( roleS, ds );
		roleR.addSuperRole( roleS, ds );
		roleS.addSubRole( roleR, ds );
		roleS.addSuperRole( roleR, ds );

		if( roleR.getInverse() != null ) {
			roleR.getInverse().addSubRole( roleS.getInverse(), ds );
			roleR.getInverse().addSuperRole( roleS.getInverse(), ds );
			roleS.getInverse().addSubRole( roleR.getInverse(), ds );
			roleS.getInverse().addSuperRole( roleR.getInverse(), ds );
		}

		return true;
	}

	public boolean addDisjointRole(ATerm s, ATerm r) {
		Role roleS = getRole( s );
		Role roleR = getRole( r );

		if( roleS == null || roleR == null )
			return false;

		roleR.addDisjointRole( roleS );
		roleS.addDisjointRole( roleR );

		return true;
	}

	public boolean addInverseRole(ATerm s, ATerm r, DependencySet ds) {
		Role roleS = getRole( s );
		Role roleR = getRole( r );

		if( roleS == null || roleR == null || !roleS.isObjectRole() || !roleR.isObjectRole() )
			return false;
		else {
			addEquivalentRole( roleS.getInverse().getName(), r, ds );
		}

		return true;
	}

	/**
	 * check if the term is declared as a role
	 */
	public boolean isRole(ATerm r) {
		return roles.containsKey( r );
	}

	public void prepare() {
		boolean hasComplexSubRoles = false;

		// first pass - compute sub roles
		for( Role role : roles.values() ) {
			if( role.getType() == Role.OBJECT || role.getType() == Role.DATATYPE ) {
				Map subExplain = new HashMap();
				Set subRoles = new HashSet();
				Set subRoleChains = new HashSet();
				hasComplexSubRoles |= role.hasComplexSubRole();
				computeSubRoles( role, subRoles, subRoleChains, subExplain,
						DependencySet.INDEPENDENT );
				role.setSubRolesAndChains( subRoles, subRoleChains, subExplain );
			}
		}

		// second pass - set super roles and propagate domain & range
		for( Role role : roles.values() ) {

			Role invR = role.getInverse();
			if( invR != null ) {
				DependencySet ds = PelletOptions.USE_TRACING
					? role.getExplainInverse()
					: DependencySet.INDEPENDENT;

				// domain of inv role is the range of this role
				Set domains = invR.getDomains();
				if( domains != null ) {
					if( PelletOptions.USE_TRACING ) {
						for( Iterator diter = domains.iterator(); diter.hasNext(); ) {
							ATermAppl domain = (ATermAppl) diter.next();
							if( role.getExplainRange( domain ) == null ) {
								role.addRange( domain, ds.union( invR.getExplainDomain( domain ),
										true ) );
							}
						}
					}
					else {
						role.addRanges( domains );
					}
				}
				Set ranges = invR.getRanges();
				if( ranges != null ) {
					if( PelletOptions.USE_TRACING ) {
						for( Iterator riter = ranges.iterator(); riter.hasNext(); ) {
							ATermAppl range = (ATermAppl) riter.next();
							if( role.getExplainDomain( range ) == null ) {
								role.addDomain( range, ds.union( invR.getExplainRange( range ),
										true ) );
							}
						}
					}
					else {
						role.addDomains( ranges );
					}
				}
				if( invR.isTransitive() && !role.isTransitive() )
					role.setTransitive( true, invR.getExplainTransitive().union( ds, true ) );
				else if( role.isTransitive() && !invR.isTransitive() )
					invR.setTransitive( true, role.getExplainTransitive().union( ds, true ) );
				if( invR.isFunctional() && !role.isInverseFunctional() )
					role.setInverseFunctional( true, invR.getExplainFunctional().union( ds, true ) );
				if( role.isFunctional() && !invR.isInverseFunctional() )
					invR.setInverseFunctional( true, role.getExplainFunctional().union( ds, true ) );
				if( invR.isInverseFunctional() && !role.isFunctional() )
					role.setFunctional( true, invR.getExplainInverseFunctional().union( ds, true ) );
				if( invR.isAntisymmetric() && !role.isAntisymmetric() )
					role.setAntisymmetric( true, invR.getExplainAntisymmetric().union( ds, true ) );
				if( role.isAntisymmetric() && !invR.isAntisymmetric() )
					invR.setAntisymmetric( true, role.getExplainAntisymmetric().union( ds, true ) );
				if( invR.isReflexive() && !role.isReflexive() )
					role.setReflexive( true, invR.getExplainReflexive().union( ds, true ) );
				if( role.isReflexive() && !invR.isReflexive() )
					invR.setReflexive( true, role.getExplainReflexive().union( ds, true ) );
				if( invR.hasComplexSubRole() )
					role.setHasComplexSubRole( true );
				if( role.hasComplexSubRole() )
					invR.setHasComplexSubRole( true );
			}

			Set domains = role.getDomains();
			Set ranges = role.getRanges();
			Iterator subs = role.getSubRoles().iterator();
			while( subs.hasNext() ) {
				Role s = (Role) subs.next();
				s.addSuperRole( role, role.getExplainSub( s.getName() ) );

				if( role.isForceSimple() )
					s.setForceSimple( true );
				if( !s.isSimple() )
					role.setSimple( false );
				if( s.hasComplexSubRole() )
					role.setHasComplexSubRole( true );
				if( domains != null ) {
					if( PelletOptions.USE_TRACING ) {
						for( Iterator diter = new HashSet( domains ).iterator(); diter.hasNext(); ) {
							ATermAppl domain = (ATermAppl) diter.next();
							if( s.getExplainRange( domain ) == null ) {
								s.addDomain( domain, role.getExplainSub( s.getName() ).union(
										role.getExplainDomain( domain ), true ) );
							}
						}
					}
					else {
						s.addDomains( domains );
					}
				}
				if( ranges != null ) {
					if( PelletOptions.USE_TRACING ) {
						for( Iterator riter = new HashSet( ranges ).iterator(); riter.hasNext(); ) {
							ATermAppl range = (ATermAppl) riter.next();
							if( s.getExplainRange( range ) == null ) {
								s.addRange( range, role.getExplainSub( s.getName() ).union(
										role.getExplainRange( range ), true ) );
							}
						}
					}
					else {
						s.addRanges( ranges );
					}
				}
			}
		}

		// TODO propagate disjoint roles through sub/super roles

		// third pass - set transitivity and functionality
		for( Role r : roles.values() ) {

			r.normalize();
			
			if( r.isForceSimple() ) {
				if( !r.isSimple() )
					ignoreTransitivity( r );
			}
			else {
				boolean isTransitive = r.isTransitive();
				DependencySet transitiveDS = r.getExplainTransitive();
				Iterator subs = r.getSubRoles().iterator();
				while( subs.hasNext() ) {
					Role s = (Role) subs.next();
					if( s.isTransitive() ) {
						if( r.isSubRoleOf( s ) && (r != s) ) {
							isTransitive = true;
							transitiveDS = r.getExplainSub( s.getName() ).union(
									s.getExplainTransitive(), true );
						}
						r.addTransitiveSubRole( s );
					}
				}
				if( isTransitive != r.isTransitive() )
					r.setTransitive( isTransitive, transitiveDS );
	
				if( hasComplexSubRoles && !r.isSimple() )
					buildDFA( r );
			}

			Iterator supers = r.getSuperRoles().iterator();
			while( supers.hasNext() ) {
				Role s = (Role) supers.next();
				if( s.isFunctional() ) {
					DependencySet ds = PelletOptions.USE_TRACING
						? r.getExplainSuper( s.getName() ).union( s.getExplainFunctional(), true )
						: DependencySet.INDEPENDENT;
					r.setFunctional( true, ds );
					r.addFunctionalSuper( s );
				}
				if( s.isIrreflexive() && !r.isIrreflexive() ) {
					DependencySet ds = PelletOptions.USE_TRACING
						? r.getExplainSuper( s.getName() ).union( s.getExplainIrreflexive(), true )
						: DependencySet.INDEPENDENT;
					r.setIrreflexive( true, ds );
				}
			}

			if( r.isReflexive() && !r.isAnon() )
				reflexiveRoles.add( r );

			if( log.isDebugEnabled() )
				log.debug( r.debugString() );
		}

		// we will compute the taxonomy when we need it
		taxonomy = null;
	}
	
	private void ignoreTransitivity(Role role) {
		Role namedRole = role.isAnon() ? role.getInverse() : role;
		
		String msg = "Unsupported axiom: Ignoring transitivity and/or complex subproperty axioms for "
				+ namedRole;

		if( !PelletOptions.IGNORE_UNSUPPORTED_AXIOMS )
			throw new UnsupportedFeatureException( msg );

		log.warn( msg );

		role.removeSubRoleChains();
		role.getInverse().removeSubRoleChains();
		role.setSimple( true );
	}

	private void computeImmediateSubRoles(Role r, Set subs, Map dependencies) {

		Role invR = r.getInverse();
		if( invR != null && invR != r ) {

			DependencySet ds = PelletOptions.USE_TRACING
				? r.getExplainInverse()
				: DependencySet.INDEPENDENT;

			Iterator i = invR.getSubRoles().iterator();
			while( i.hasNext() ) {
				Role invSubR = (Role) i.next();
				Role subR = invSubR.getInverse();
				if( subR == null ) {
					System.err.println( "Property " + invSubR
							+ " was supposed to be an ObjectProperty but it is not!" );
				}
				else if( subR != r ) {
					// System.out.println("expsub:
					// "+invR.getExplainSub(invSubR.getName()));
					// System.out.println("expinv:
					// "+invSubR.getExplainInverse());
					DependencySet subDS = PelletOptions.USE_TRACING
						? ds.union( invR.getExplainSub( invSubR.getName() ).union(
								invSubR.getExplainInverse(), true ), true )
						: DependencySet.INDEPENDENT;
					subs.add( subR );
					dependencies.put( subR.getName(), subDS );
				}
			}
			i = invR.getSubRoleChains().iterator();
			while( i.hasNext() ) {
				ATermList roleChain = (ATermList) i.next();
				DependencySet subDS = PelletOptions.USE_TRACING
					? ds.union( invR.getExplainSub( roleChain ), true )
					: DependencySet.INDEPENDENT;

				ATermList subChain = inverse( roleChain );
				subs.add( subChain );
				dependencies.put( subChain, subDS );
			}
		}

		for( Iterator i = r.getSubRoles().iterator(); i.hasNext(); ) {
			Role sub = (Role) i.next();

			DependencySet subDS = PelletOptions.USE_TRACING
				? r.getExplainSub( sub.getName() )
				: DependencySet.INDEPENDENT;

			subs.add( sub );
			dependencies.put( sub.getName(), subDS );
		}

		for( Iterator i = r.getSubRoleChains().iterator(); i.hasNext(); ) {
			ATermList subChain = (ATermList) i.next();

			DependencySet subDS = PelletOptions.USE_TRACING
				? r.getExplainSub( subChain )
				: DependencySet.INDEPENDENT;

			subs.add( subChain );
			dependencies.put( subChain, subDS );
		}

	}

	private void computeSubRoles(Role r, Set subRoles, Set subRoleChains, Map dependencies,
			DependencySet ds) {
		// check for loops
		if( subRoles.contains( r ) )
			return;

		// reflexive
		subRoles.add( r );
		dependencies.put( r.getName(), ds );

		// transitive closure
		Set immSubs = new HashSet();
		Map immDeps = new HashMap();
		computeImmediateSubRoles( r, immSubs, immDeps );
		Iterator i = immSubs.iterator();
		while( i.hasNext() ) {
			Object sub = i.next();
			if( sub instanceof Role ) {
				ATermAppl name = ((Role) sub).getName();
				DependencySet subDS = PelletOptions.USE_TRACING
					? ds.union( (DependencySet) immDeps.get( name ), true )
					: DependencySet.INDEPENDENT;

				computeSubRoles( (Role) sub, subRoles, subRoleChains, dependencies, subDS );

			}
			else {
				// ATermList roles = (ATermList) sub;
				DependencySet subDS = PelletOptions.USE_TRACING
					? ds.union( (DependencySet) immDeps.get( sub ), true )
					: DependencySet.INDEPENDENT;

				subRoleChains.add( sub );
				dependencies.put( sub, subDS );
			}
		}
	}

	private TransitionGraph buildDFA(Role s) {
		TransitionGraph tg = s.getFSM();
		if( tg != null )
			return tg;

		if( log.isDebugEnabled() )
			log.debug( "Building NFA for " + s );

		tg = buildNFA( s );

		if( log.isDebugEnabled() )
			log.debug( "Determinize " + s + ": " + tg.size() );

		tg.determinize();

		// ATermUtils.assertTrue( tg.isDeterministic() );

		if( log.isDebugEnabled() )
			log.debug( "Minimize NFA for " + s + ": " + tg.size() );

		tg.minimize();

		// ATermUtils.assertTrue( tg.isDeterministic() );

		tg.renumber();

		// ATermUtils.assertTrue( tg.isDeterministic() );

		s.setFSM( tg );

		Set eqRoles = SetUtils.intersection( s.getSubRoles(), s.getSuperRoles() );
		eqRoles.remove( s );
		for( Iterator i = eqRoles.iterator(); i.hasNext(); ) {
			Role eqRole = (Role) i.next();
			eqRole.setFSM( tg );
		}

		if( log.isDebugEnabled() )
			log.debug( "NFA for " + tg );

		return tg;
	}

	private TransitionGraph buildNFA(Role s) {
		TransitionGraph tg = new TransitionGraph();

		State i = tg.newState();
		State f = tg.newState();

		tg.setInitialState( i );
		tg.addFinalState( f );

		tg.addTransition( i, s, f );

		Set subRoles = SetUtils.difference( s.getSubRoles(), s.getSuperRoles() );

		for( Iterator it = subRoles.iterator(); it.hasNext(); ) {
			Role sub = (Role) it.next();
			tg.addTransition( i, sub, f );
		}

		for( Iterator it = s.getSubRoleChains().iterator(); it.hasNext(); ) {
			ATermList subChain = (ATermList) it.next();
			addTransition( tg, s, subChain );
		}

		Set alphabet = new HashSet( tg.getAlpahabet() );
		alphabet.remove( s );
		for( Iterator it = alphabet.iterator(); it.hasNext(); ) {
			Role r = (Role) it.next();

			addTransitions( tg, r );
		}

		return tg;
	}

	private void addTransition(TransitionGraph tg, Role s, ATermList chain) {
		State first = tg.getInitialState(), last = tg.getFinalState();

		if( ATermUtils.isTransitiveChain( chain, s.getName() ) ) {
			tg.addTransition( last, first );
			return;
		}
		else if( chain.getFirst().equals( s.getName() ) ) {
			chain = chain.getNext();
			first = tg.getFinalState();
		}
		else if( chain.getLast().equals( s.getName() ) ) {
			chain = chain.remove( s.getName() );
			last = tg.getInitialState();
		}

		State next = tg.newState();
		tg.addTransition( first, next );
		first = next;

		for( ; !chain.isEmpty(); chain = chain.getNext() ) {
			next = tg.newState();
			tg.addTransition( first, getRole( chain.getFirst() ), next );
			first = next;
		}

		tg.addTransition( first, last );
	}

	private void addTransitions(TransitionGraph tg, Role r) {
		List pairs = tg.findTransitions( r );
		for( Iterator i = pairs.iterator(); i.hasNext(); ) {
			TransitionGraph newGraph = buildDFA( r ).copy();

			Pair pair = (Pair) i.next();

			tg.insert( newGraph, (State) pair.first, (State) pair.second );
		}
	}

	/**
	 * Returns a string representation of the RBox where for each role subroles,
	 * superroles, and isTransitive information is given
	 */
	public String toString() {
		return "[RBox " + roles.values() + "]";
	}

	/**
	 * for each role in the list finds an inverse role and returns the new list.
	 */
	public ATermList inverse(ATermList roles) {
		ATermList invList = ATermUtils.EMPTY_LIST;

		for( ATermList list = roles; !list.isEmpty(); list = list.getNext() ) {
			ATermAppl r = (ATermAppl) list.getFirst();
			Role role = getRole( r );
			Role invR = role.getInverse();
			if( invR == null ) {
				System.err.println( "Property " + r
						+ " was supposed to be an ObjectProperty but it is not!" );
			}
			else
				invList = invList.insert( invR.getName() );
		}

		return invList;
	}

	/**
	 * @return Returns the roles.
	 */
	public Set getRoleNames() {
		return roles.keySet();
	}

	public Set<Role> getReflexiveRoles() {
		return reflexiveRoles;
	}

	/**
	 * getRoles
	 * 
	 * @return
	 */
	public Collection<Role> getRoles() {
		return roles.values();
	}

	public Taxonomy getTaxonomy() {
		if( taxonomy == null ) {
			RoleTaxonomyBuilder builder = new RoleTaxonomyBuilder( this );
			taxonomy = builder.classify();
		}
		return taxonomy;
	}

}
