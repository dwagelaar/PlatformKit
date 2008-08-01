//The MIT License
//
//Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to
//deal in the Software without restriction, including without limitation the
//rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
//sell copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
//FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.

package org.mindswap.pellet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Bool;
import org.mindswap.pellet.utils.SetUtils;

import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;

/*
 * Created on Aug 27, 2003
 *
 */

/**
 * @author Evren Sirin
 *
 */
public class Individual extends Node {
	private EdgeList outEdges;
	
	private ArrayList[] types = new ArrayList[TYPES];
	int[] applyNext = new int[TYPES];
	
	/**
	 * nominal level as defined in the SHOIQ algorithm (it would be more appropriate
	 * to define this in Individual but to avoid casting as much as possible it is
	 * defined here
	 */
	private int nominalLevel;
	
	List ancestors;
	
	Individual(ATermAppl name) {
		this(name, null, BLOCKABLE);
	}
	
	Individual(ATermAppl name, ABox abox, int nominalLevel) {
		super(name, abox);

		this.nominalLevel = nominalLevel;
		
		for(int i = 0; i < TYPES; i++) {
			types[i] = new ArrayList();
			applyNext[i] = 0;		
		}
		
		outEdges = new EdgeList();
	}
	
	Individual(Individual ind, ABox abox) {
		super(ind, abox);
		
		nominalLevel = ind.nominalLevel;

		for(int i = 0; i < TYPES; i++) {
			types[i] = new ArrayList(ind.types[i]);		
			applyNext[i] = ind.applyNext[i];		
		}
	
		if(abox == null) {
		    mergedTo = null;
			outEdges = new EdgeList(ind.outEdges.size());
			for(int i = 0; i < ind.outEdges.size(); i++) {
				Edge edge = ind.outEdges.edgeAt(i);
				Individual from = this;
				Individual to = new Individual(edge.getTo().getName());
				to.nominalLevel = edge.getTo().getNominalLevel();
				Edge newEdge = new Edge(edge.getRole(), from, to, edge.getDepends());
				outEdges.addEdge(newEdge);
			}
		}
		else {
			outEdges = new EdgeList(ind.outEdges.size());
		}
	}
	
	public boolean isLiteral() {
	    return false;
	}
	
	public boolean isIndividual() {
	    return true;
	}

	public boolean isNominal() {
		return nominalLevel != BLOCKABLE;
	}
	
	public boolean isBlockable() {
		return nominalLevel == BLOCKABLE;
	}
	
	public void setNominalLevel(int level) {
	    nominalLevel = level;
	    
	    if( nominalLevel != BLOCKABLE )
	        ancestors = null;
	}

	public int getNominalLevel() {
	    return nominalLevel;
	}
	
    public ATermAppl getTerm() {
        return name;
    }

	public Node copyTo(ABox abox) {
		return new Individual(this, abox);
	}	
		
	public Set getMerged() {
	    return (merged == null) ? SetUtils.EMPTY_SET : merged;
	}
	
	public List getTypes(int type) {
		return types[type];
	}
		
	public boolean isDifferent( Node node ) {
	    if( PelletOptions.USE_UNIQUE_NAME_ASSUMPTION ) {
	        if( isNamedIndividual() && node.isNamedIndividual() )
	            return !name.equals( node.name );
	    }
	    
		return differents.containsKey(node);
	}
		
	public Set getDifferents() {
		return differents.keySet();
	}

	public DependencySet getDifferenceDependency( Node node ) {
	    if( PelletOptions.USE_UNIQUE_NAME_ASSUMPTION ) {
	        if( isNamedIndividual() && node.isNamedIndividual() )
	            return DependencySet.INDEPENDENT;
	    }
	    
		return (DependencySet) differents.get(node);
	}	

	/**
	 * Collects atomic concepts such that either that concept or its negation 
	 * exist in the types list without depending on any non-deterministic branch. 
	 * First list is filled with types and second list is filled with non-types,
	 * i.e. this individual can never be an instance of any element in the 
	 * second list. 
	 * 
	 * @param types All atomic concepts found in types
	 * @param nonTypes All atomic concepts
	 */
	public void getObviousTypes( List types, List nonTypes ) {
		List atomic = getTypes( Node.ATOM );
		for(Iterator i = atomic.iterator(); i.hasNext();) {
            ATermAppl c = (ATermAppl) i.next();
            
            if( getDepends( c ).isIndependent() ) {
                if( ATermUtils.isPrimitive( c ) ) {
                    types.add( c );
                }
                else if( ATermUtils.isNegatedPrimitive( c ) )                   
                    nonTypes.add( c.getArgument( 0 ) );                
            }
        }
	}
	
	public boolean canApply(int type) {
		return applyNext[type] < types[type].size();
	}
	
	public void addType(ATermAppl c, DependencySet ds) {
	    if( isPruned() )
	        throw new InternalReasonerException( "Adding type to a pruned node " + this + " " + c );
	    else if( isMerged() )
	        return;
	    
		if( depends.containsKey( c ) ) {
			//Seems this should be commented out
			//ds = ds.copy();
			//ds.branch = abox.getBranch();
			
//			handleReaddType(c, ds);
//			ds = ds.union( getDepends(c) );
//		    depends.put(c, ds);
//			if (c.getAFun().equals(ATermUtils.ANDFUN)) {
//				for(ATermList cs = (ATermList) c.getArgument(0); !cs.isEmpty(); cs = cs.getNext()) {
//					ATermAppl conj = (ATermAppl) cs.getFirst();
//					
//					addType(conj, ds);
//				}			
//			}		    
			return;		
		}
        
//        if( ABox.log.isDebugEnabled() ) 
//            ABox.log.debug( "TYPE: " + this + " " + c );
        
		ds = ds.copy();
		ds.branch = abox.getBranch();
		
		// if we are checking entailment using a pseduo model, abox.branch 
		// is set to -1. however, since applyAllValues is done automatically
		// and the edge used in applyAllValues may depend on a branch we want
		// this type to be deleted when that edge goes away, i.e. we backtrack
		// to a position before the max dependency of this type
		int max = ds.max();
		if(ds.branch == -1 && max != 0)
		    ds.branch = max + 1;
		
		depends.put(c, ds);

		abox.changed = true;


		//create new queue element
		QueueElement qElement = new QueueElement(this.getName(), c);
		
		ATermAppl notC = ATermUtils.negate(c);
		DependencySet clashDepends = (DependencySet) depends.get(notC);
		if(clashDepends != null) {
			if(abox.doExplanation()) {
			    ATermAppl positive = (ATermUtils.isNot(notC) ? c : notC);
			    abox.setClash(Clash.atomic(this, clashDepends.union(ds, abox.doExplanation()), positive));
			}
			else
			    abox.setClash(Clash.atomic(this, clashDepends.union(ds, abox.doExplanation())));
		}
		
		if (ATermUtils.isPrimitive(c)) {
			setChanged(ATOM, true);
			types[ATOM].add(c);

			if(PelletOptions.USE_COMPLETION_QUEUE){
				//update completion queue
				abox.completionQueue.add(qElement, CompletionQueue.ATOMLIST);
			}
		}
		else {
			if (c.getAFun().equals(ATermUtils.ANDFUN)){
				for(ATermList cs = (ATermList) c.getArgument(0); !cs.isEmpty(); cs = cs.getNext()) {
					ATermAppl conj = (ATermAppl) cs.getFirst();
					
					addType(conj, ds);
				}			
			}
			else if (c.getAFun().equals(ATermUtils.ALLFUN)) {
				setChanged(ALL, true);			
				types[ALL].add(c);			

				if(PelletOptions.USE_COMPLETION_QUEUE){
					//update completion queue
					abox.completionQueue.add(qElement, CompletionQueue.ALLLIST);
				}
			}
			else if (c.getAFun().equals(ATermUtils.MINFUN)) {
				if(checkMinClash(c, ds))
					return;
				else if(!isRedundantMin(c)) {
					types[MIN].add(c);
					setChanged(MIN, true);
					
					if(PelletOptions.USE_COMPLETION_QUEUE){		
						//update completion queue
						abox.completionQueue.add(qElement, CompletionQueue.MINLIST);
					}
				}				
			}
			else if(c.getAFun().equals(ATermUtils.NOTFUN)) {
				ATermAppl x = (ATermAppl) c.getArgument(0);
				if(ATermUtils.isAnd(x)) {
					setChanged(OR, true);
					types[OR].add(c);
					
					if(PelletOptions.USE_COMPLETION_QUEUE){
						//update completion queue
						abox.completionQueue.add(qElement, CompletionQueue.ORLIST);
					}
				}
				else if(ATermUtils.isAllValues(x)) {
					setChanged(SOME, true);
					types[SOME].add(c);
					
					if(PelletOptions.USE_COMPLETION_QUEUE){
						//update completion queue					
						abox.completionQueue.add(qElement, CompletionQueue.SOMELIST);
					}
				}
				else if(ATermUtils.isMin(x)) {
					if(checkMaxClash(c, ds))
						return;
					else if(!isRedundantMax(x)) {
						types[MAX].add(c);
						setChanged(MAX, true);
						
						if(PelletOptions.USE_COMPLETION_QUEUE){
							//update completion queue						
							abox.completionQueue.add(qElement, CompletionQueue.MAXLIST);
							abox.completionQueue.add(qElement, CompletionQueue.CHOOSELIST);
							abox.completionQueue.add(qElement, CompletionQueue.GUESSLIST);
						}
					}
				}
				else if(ATermUtils.isNominal(x)) {
					setChanged(ATOM, true);
					types[ATOM].add(c);
						
					if(PelletOptions.USE_COMPLETION_QUEUE){
						//update completion queue					
						abox.completionQueue.add(qElement, CompletionQueue.ATOMLIST);
					}
				}
				else if (ATermUtils.isSelf(x)) {
	            	ATermAppl p = (ATermAppl) x.getArgument( 0 );
	            	Role role = abox.getRole( p );
	            	// during loading role would be null
	            	if( role != null ) {
	            		EdgeList selfEdges = outEdges.getEdges(role).getEdgesTo( this );
	            		if( !selfEdges.isEmpty() ) {	            			
	            			abox.setClash(Clash.unexplained( this, selfEdges.getDepends( abox.doExplanation() )));
	            		}
	            	}
	            }
				else if(x.getArity() == 0) {
					setChanged(ATOM, true);
					types[ATOM].add(c);
					
					if(PelletOptions.USE_COMPLETION_QUEUE){
						//update completion queue					
						abox.completionQueue.add(qElement, CompletionQueue.ATOMLIST);
					}
				}
				else
				    throw new InternalReasonerException( "Invalid type " +  c + " for individual " + name);
			}
			else if (c.getAFun().equals(ATermUtils.VALUEFUN)) {
				setChanged(NOM, true);
				types[NOM].add(c);
			
				if(PelletOptions.USE_COMPLETION_QUEUE){
					//update completion queue				
					abox.completionQueue.add(qElement, CompletionQueue.NOMLIST);
				}
			}		
            else if (ATermUtils.isSelf(c)) {
            	setChanged( ATOM, true );
                types[ATOM].add(c);
            }
			else {
				System.err.println("Warning: Adding invalid class constructor - " + c);				
				depends.put(ATermUtils.BOTTOM, ds);
			}				
		}		
	}
	
	public boolean checkMinClash(ATermAppl minCard, DependencySet minDepends) {
		Role minR = abox.getRole(minCard.getArgument(0));
		if( minR == null )
			return false;
		int min = ((ATermInt) minCard.getArgument(1)).getInt();
        ATermAppl minC = (ATermAppl) minCard.getArgument(2);
		
		if(minR.isFunctional() && min > 1) {
			String exp = null;
			if(abox.doExplanation())
				exp = minCard + " on FunctionalProperty";

			abox.setClash(new Clash(this, Clash.MIN_MAX, minDepends, exp));
			
			return true;
		}

		for(Iterator i = types[MAX].iterator(); i.hasNext(); ) {
			ATermAppl mc = (ATermAppl) i.next();

			// max(r, n) is in normalized form not(min(p, n + 1))
			ATermAppl maxCard = (ATermAppl) mc.getArgument(0);								
			Role maxR = abox.getRole(maxCard.getArgument(0));
			if( maxR == null )
				return false;
			int max = ((ATermInt) maxCard.getArgument(1)).getInt() - 1;
            ATermAppl maxC = (ATermAppl) maxCard.getArgument(2);
            
			if(max < min && minC.equals( maxC ) && minR.isSubRoleOf(maxR)) {
				DependencySet maxDepends = getDepends(mc);
				DependencySet subDepends = maxR.getExplainSub(minR.getName());
				DependencySet ds = minDepends.union(maxDepends, abox.doExplanation()).union(subDepends, abox.doExplanation());
				
				String exp = null;
				if(abox.doExplanation())
					exp = minCard + " max(" + maxR + ", " + max + ")";
					
				abox.setClash(new Clash(this, Clash.MIN_MAX, ds, exp));

				return true;
			}
		}		
		
		return false;
	}

	public boolean checkMaxClash(ATermAppl normalizedMax, DependencySet maxDepends) {
        ATermAppl maxCard = (ATermAppl) normalizedMax.getArgument(0);
		Role maxR = abox.getRole(maxCard.getArgument(0));
		if( maxR == null )
			return false;
		int max = ((ATermInt) maxCard.getArgument(1)).getInt() - 1;
        ATermAppl maxC = (ATermAppl) maxCard.getArgument(2);

		for(Iterator i = types[MIN].iterator(); i.hasNext(); ) {
			ATermAppl minCard = (ATermAppl) i.next();											
			Role minR = abox.getRole(minCard.getArgument(0));	
			if( minR == null )
				return false;
			int min = ((ATermInt) minCard.getArgument(1)).getInt();
            ATermAppl minC = (ATermAppl) minCard.getArgument(2);

			if(max < min && minC.equals( maxC ) && minR.isSubRoleOf(maxR)) {
				DependencySet minDepends = getDepends(minCard);
				DependencySet subDepends = maxR.getExplainSub(minR.getName());
				DependencySet ds = minDepends.union(maxDepends, abox.doExplanation()).union(subDepends, abox.doExplanation());
				String exp = null;
				if(abox.doExplanation())
					exp = minCard + " max(" + maxR + ", " + max + ")";
					
				abox.setClash(new Clash(this, Clash.MIN_MAX, ds, exp));

				return true;
			}
		}		
		
		return false;
	}

	public boolean isRedundantMin(ATermAppl minCard) {
		Role minR = abox.getRole(minCard.getArgument(0));
		
        if( minR == null )
            return false;

		int min = ((ATermInt) minCard.getArgument(1)).getInt();
        ATermAppl minQ = (ATermAppl) minCard.getArgument( 2 );
        
		for(Iterator i = types[MIN].iterator(); i.hasNext(); ) {
			ATermAppl prevMinCard  = (ATermAppl) i.next();
			Role prevMinR = abox.getRole(prevMinCard.getArgument(0));
			
			 if( prevMinR == null )
		         continue;
			 
			int prevMin = ((ATermInt) prevMinCard.getArgument(1)).getInt() - 1;
            ATermAppl prevMinQ = (ATermAppl) prevMinCard.getArgument( 2 );
            
			if( min <= prevMin 
                && prevMinR.isSubRoleOf( minR ) 
                && ( minQ.equals( prevMinQ ) 
                    || ATermUtils.isTop( minQ ) ) )
				return true;
		}

		return false;
	}

	public boolean isRedundantMax(ATermAppl maxCard) {
		Role maxR = abox.getRole(maxCard.getArgument(0));
        if( maxR == null )
            return false;
        
		int max = ((ATermInt) maxCard.getArgument(1)).getInt() - 1;

		if(max == 1 && maxR != null && maxR.isFunctional())
			return true;
        
        ATermAppl maxQ = (ATermAppl) maxCard.getArgument( 2 );
		
		for(Iterator i = types[MAX].iterator(); i.hasNext(); ) {
			ATermAppl mc = (ATermAppl) i.next();

			// max(r, n) is in normalized form not(min(p, n + 1))
			ATermAppl prevMaxCard = (ATermAppl) mc.getArgument(0);								
			Role prevMaxR = abox.getRole(prevMaxCard.getArgument(0));
						
			 if( prevMaxR == null )
		         continue;
			 
			int prevMax = ((ATermInt) prevMaxCard.getArgument(1)).getInt() - 1;
            ATermAppl prevMaxQ = (ATermAppl) prevMaxCard.getArgument( 2 );
            
			if( max >= prevMax
                && maxR.isSubRoleOf( prevMaxR ) 
                && ( maxQ.equals( prevMaxQ ) 
                    || ATermUtils.isTop( prevMaxQ ) ) )
				return true;
		}		
		
		return false;
	}

	public DependencySet hasMax1( Role r ) {
		for(Iterator i = types[MAX].iterator(); i.hasNext(); ) {
            // max(r, n, c) is in normalized form not(min(p, n + 1))
			ATermAppl maxCard = (ATermAppl) ((ATermAppl) i.next()).getArgument(0);
			Role maxR = abox.getRole(maxCard.getArgument(0));
			int max = ((ATermInt) maxCard.getArgument(1)).getInt() - 1;
			ATermAppl maxQ = (ATermAppl) maxCard.getArgument( 2 );
            
			// FIXME returned dependency set might be wrong 
			// if there are two types max(r,1) and max(p,1) where r subproperty of p
			// then the dependency set what we return might be wrong
			if( max == 1 && r.isSubRoleOf( maxR ) && ATermUtils.isTop( maxQ ) )
				return getDepends( maxCard );
		}		
		
		return null;
	}	
	
	public int getMaxCard( Role r ) {
	    int min = Integer.MAX_VALUE;
		for(Iterator i = types[MAX].iterator(); i.hasNext(); ) {
			ATermAppl mc = (ATermAppl) i.next();

			// max(r, n) is in normalized form not(min(p, n + 1))
			ATermAppl maxCard = (ATermAppl) mc.getArgument(0);								
			Role maxR = abox.getRole( maxCard.getArgument(0) );
			int max = ((ATermInt) maxCard.getArgument(1)).getInt() - 1;

			if( r.isSubRoleOf( maxR ) && max < min )
				min = max;
		}		
		
		if( r.isFunctional() && min > 1 )
		    min = 1;
		
		return min;
	}
	
	public int getMinCard( Role r ) {
	    int max = 0;
		for(Iterator i = types[MIN].iterator(); i.hasNext(); ) {
			ATermAppl minCard = (ATermAppl) i.next();								
			Role minR = abox.getRole( minCard.getArgument(0) );			
			int min = ((ATermInt) minCard.getArgument(1)).getInt();

			if( minR.isSubRoleOf( r ) && max < min )
				max = min;
		}		
		
		return max;
	}
	
	public void removeType(ATermAppl c) {
		depends.remove(c);
		setChanged(true);

		if (ATermUtils.isPrimitive(c) || ATermUtils.isSelf(c)) {
			types[ATOM].remove(c);
		}
		else {
			if(c.getAFun().equals(ATermUtils.ANDFUN)) {
//			    types[AND].remove(c);
			}
			else if (c.getAFun().equals(ATermUtils.ALLFUN)) {
				types[ALL].remove(c);
			}
			else if (c.getAFun().equals(ATermUtils.MINFUN)) {
				types[MIN].remove(c);
			}
			else if (c.getAFun().equals(ATermUtils.NOTFUN)) {
				ATermAppl x = (ATermAppl) c.getArgument(0);
				if(ATermUtils.isAnd(x)) {
					types[OR].remove(c);
				}
				else if(ATermUtils.isAllValues(x)) {
					types[SOME].remove(c);
				}
				else if(ATermUtils.isMin(x)) {
					types[MAX].remove(c);
				}
				else if(ATermUtils.isNominal(x)) {
					types[ATOM].remove(c);
				}
				else if(x.getArity() == 0) {
					types[ATOM].remove(c);
				}
				else
				    throw new InternalReasonerException( "Invalid type " +  c + " for individual " + name);				
			}
			else if(c.getAFun().equals(ATermUtils.VALUEFUN))
				types[NOM].remove(c);
			else
				throw new RuntimeException("Invalid concept " + c);
		}
	}

	final public boolean isLeaf() {
		return !isRoot() && outEdges.isEmpty();
	}
	
	final public Set getSuccessors() {
		return outEdges.getSuccessors();
	}

	final public Set getSortedSuccessors() {	    
		return outEdges.sort().getSuccessors();
	}					

	final public Set getRSuccessors(Role r, ATermAppl c) {
        Set result = new HashSet();
        
        EdgeList edges = outEdges.getEdges(r);
        for(int i = 0, n = edges.size(); i < n; i++) {
            Edge edge = edges.edgeAt(i); 
            Node other = edge.getNeighbor( this );
            if( other.hasType( c) )
                result.add( other );
        }
        
        return result;
	}
    
    final public Set getRSuccessors(Role r) {
        return outEdges.getEdges(r).getNeighbors(this);
    }

	final public EdgeList getRSuccessorEdges(Role r) {
		return outEdges.getEdges(r);
	}

	final public EdgeList getRPredecessorEdges(Role r) {
		return inEdges.getEdges(r);
	}

    final public Set getRNeighborNames(Role r) {
        return getRNeighborEdges(r).getNeighborNames(this);
    }
    
	final public Set getRNeighbors(Role r) {
		return getRNeighborEdges(r).getNeighbors(this);
	}
	
	public EdgeList getRNeighborEdges(Role r) {
		EdgeList neighbors = outEdges.getEdges(r);
		
		if( r == null )
			throw new NullPointerException();
		
		Role invR = r.getInverse();
		// inverse of datatype properties is not defined
		if(invR != null) 
			neighbors.addEdgeList(inEdges.getEdges(invR));	
		
		return neighbors;
	}	
		
	public EdgeList getEdgesTo(Node x) {
		return outEdges.getEdgesTo(x);
	}

	public EdgeList getEdgesTo(Node x, Role r) {
		return outEdges.getEdgesTo(x).getEdges(r);
	}
	

	/**
	 * Checks if this individual has at least n distinct r-neighbors that has 
     * a specific type. 
	 * 
	 * @param r Role we use to find neighbors
	 * @param n Number of neighbors 
     * @param c The type that all neighbors should belong to 
	 * @return The union of dependencies for the edges leading to neighbors and 
     * the dependency of the type assertion for each neighbor 
	 */
	DependencySet hasDistinctRNeighborsForMax( Role r, int n, ATermAppl c ) {
//	    Timer t = abox.getKB().timers.startTimer("hasDistinctRNeighbors1"); 
	    
        boolean hasNeighbors = false; 
        
	    // get all the edges to x with a role (or subrole of) r
		EdgeList edges = getRNeighborEdges( r );

		if(edges.size() >= n) {
			List allDisjointSets = new ArrayList();			
			
		outerloop:	
			for(int i = 0; i < edges.size(); i++ ) {
				Node y = edges.edgeAt(i).getNeighbor(this);
                
                if( !y.hasType( c ) )
                    continue;                    
				
				boolean added = false;
				for(int j = 0; j < allDisjointSets.size(); j++) {
					List disjointSet = (List) allDisjointSets.get(j);
					int k = 0;
					for(; k < disjointSet.size(); k++) {
						Node z = (Node) disjointSet.get(k);
						if(!y.isDifferent(z)) 
							break;
					}
					if(k == disjointSet.size()) {
						added = true;
						disjointSet.add(y);
						if(disjointSet.size() >= n) {
                            hasNeighbors = true;
							break outerloop;
						}
					}
				}
				if(!added) {
					List singletonSet = new ArrayList();
					singletonSet.add(y);
					allDisjointSets.add( singletonSet );
					if( n == 1 ) {
                        hasNeighbors = true;
						break outerloop;
					}						
				}
			}			
		}
//		t.stop();

        if( !hasNeighbors )
            return null;
        
        // we are being overly cautious here by getting the union of all
        // the edges to all r-neighbors 
        DependencySet ds = DependencySet.EMPTY;
        for( Iterator i = edges.iterator(); i.hasNext(); ) {
            Edge edge = (Edge) i.next();
            ds = ds.union( edge.getDepends(), abox.doExplanation() );
            Node node = edge.getNeighbor( this );
            DependencySet typeDS = node.getDepends( c );
            if( typeDS != null )
                ds = ds.union( typeDS, abox.doExplanation() );
        }        
        
		return ds;
	}

	boolean hasDistinctRNeighborsForMin( Role r, int n, ATermAppl c ) {
	    return hasDistinctRNeighborsForMin( r, n, c, false );
	}
	
	/**
	 * Returns true if this individual has at least n distinct r-neighbors. If
	 * only nominal neighbors are wanted then blockable ones will simply be 
	 * ignored (note that this should only happen if r is an object property)
	 * 
	 * @param r
	 * @param n
	 * @param onlyNominals
	 * @return
	 */
	boolean hasDistinctRNeighborsForMin( Role r, int n, ATermAppl c, boolean onlyNominals ) {
	    // get all the edges to x with a role (or subrole of) r
		EdgeList edges = getRNeighborEdges(r);
	    
		if( n == 1 && !onlyNominals && c.equals( ATermUtils.TOP ) ) 
		    return !edges.isEmpty();		
		
	    if( edges.size() < n ) 
		    return false;

		List allDisjointSets = new ArrayList();					
		for(int i = 0; i < edges.size(); i++ ) {
			Node y = edges.edgeAt(i).getNeighbor(this);
            
            if( !y.hasType( c ) )
                continue;
            
			if( onlyNominals ) {
			    if( y.isBlockable() )
			        continue;
			    else if( n == 1 )
			        return true;
			}
			    
			
			boolean added = false;
			for(int j = 0; j < allDisjointSets.size(); j++) {
			    boolean addToThis = true;
				List disjointSet = (List) allDisjointSets.get(j);
				for(int k = 0; k < disjointSet.size(); k++) {
					Node z = (Node) disjointSet.get(k);
					if(!y.isDifferent(z)) {
					    addToThis = false;
					    break;
					}					
				}
				if(addToThis) {
				    added = true;
					disjointSet.add(y);
					if(disjointSet.size() >= n)
						return true;
				}
			}
			if(!added) {
				List singletonSet = new ArrayList();
				singletonSet.add(y);
				allDisjointSets.add(singletonSet);					
			}
		}			
		
		return false;
	}

	final public boolean hasRNeighbor(Role r) {
		if( outEdges.hasEdge( r ) )
		    return true;
		
		Role invR = r.getInverse();
		if(invR != null && inEdges.hasEdge(invR) ) 
		    return true;
		
		return false;
	}
	
	public boolean hasRSuccessor( Role r ) {	
		return outEdges.hasEdge( r );
	}

	public boolean hasSuccessor( Node x ) {
		return outEdges.hasEdgeTo( x );
	}
	
	final boolean hasRSuccessor( Role r, Node x ) {
		return outEdges.hasEdge( this, r, x );
	}	
	
	/**
	 * Check the property assertions to see if it is possible for this individual to
	 * have the value for the given datatype property. This function is meaningful
	 * only called for individuals in a completed ABox (a pseudo model for the KB).
	 * In a completed ABox, individual will have some literal successors that may
	 * or may not have a known value. The individual has the data property value
	 * only if it has a literal successor that has the exact given value and the
	 * edge between the individual and the literal does not depend on any non-
	 * deterministic branch. If the literal value is there but the edge depends
	 * on a branch then we cannot exactly say if the literal value is there or
	 * not. If there is no literal successor with the given value then we can
	 * for sure say that individual does not have the data property value
	 * (because it does not have the value in at least one model)  
	 * 
	 * 
	 * @param r
	 * @param value
	 * @return Bool.TRUE if the individual definetely has the property value,
	 * Bool.FALSE if the individual definetely does NOT have the property value
	 * and Bool.UNKNOWN if it cannot be determined for sure, i.e. consistency check is 
	 * required 
	 */
	public Bool hasDataPropertyValue( Role r, Object value ) {
	    Bool hasValue = Bool.FALSE;
	    
		EdgeList edges = outEdges.getEdges( r );
		for(int i = 0; i < edges.size(); i++) {
		    Edge edge = edges.edgeAt( i );
		    DependencySet ds = edge.getDepends();
		    Literal literal = (Literal) edge.getTo();
		    Object literalValue = literal.getValue();
		    if( value != null && literalValue == null ) {
		        Datatype datatype = literal.getDatatype();
		        if( datatype.size() == 1 && datatype.contains( value ) ) 
		            hasValue = Bool.UNKNOWN;
		    }
		    else if( value == null || literalValue.equals( value ) ) {
		        if( ds.isIndependent() )
		            return Bool.TRUE;
		        else
		            hasValue = Bool.UNKNOWN;
		    }
		}
		
		return hasValue;
	}
	
	boolean hasRNeighbor(Role r, Node x) {
		if(hasRSuccessor(r, x))
			return true;
		
		if(x instanceof Individual)
			return ((Individual) x).hasRSuccessor(r.getInverse(), this);
			
		return false;
	}

	protected void addInEdge(Edge edge) {        
		setChanged(ALL, true);
		setChanged(MAX, true);
        
        inEdges.addEdge( edge );
	}	

	protected void addOutEdge(Edge edge) {
		setChanged(ALL, true);
		setChanged(MAX, true);

		outEdges.addEdge(edge);						
	}
	
	public Edge addEdge( Role r, Node x, DependencySet ds ) {		
	    
		//add these nodes to the effected list in the queue	
		if(abox.getBranch() >0 && PelletOptions.USE_COMPLETION_QUEUE){ 
    			abox.completionQueue.addEffected(abox.getBranch(), this.getName());
    			abox.completionQueue.addEffected(abox.getBranch(), x.getName());
		}
		
		if( hasRSuccessor( r, x ) ) {
		    // TODO we might miss some of explanation axioms
	    	if( log.isDebugEnabled() )
	    		log.debug( "EDGE: " + this + " -> " + r + " -> " + x + ": " + ds + " " + getRNeighborEdges( r ).getEdgesContaining( x ) );
		    return null;
		}		
	    		
	    if( isPruned() )
	        throw new InternalReasonerException( "Adding edge to a pruned node " + this + " " + r + " " + x );
	    else if( isMerged() )
	        return null;

		abox.changed = true;
		setChanged(ALL, true);
		setChanged(MAX, true);
		
		ds = ds.copy();
		ds.branch = abox.getBranch();
		
	    Edge edge = new Edge(r, this, x, ds);
	    
		outEdges.addEdge(edge);
		x.addInEdge(edge);
		
		return edge;
	}
	

	final public EdgeList getOutEdges() {
		return outEdges;
	}		   	
	
	/**
	 * Returns (only blockable) ancestors. Ancestors of a nominal node is empty. 
	 * 
	 * @return
	 */
	public List getAncestors() {
	    if( ancestors == null ) {
	        if( isNominal() )
	            ancestors = Collections.EMPTY_LIST;
	        else {
			    ancestors = new ArrayList();
			    for( Node node = getParent(); node != null; node = node.getParent() ) 
			        ancestors.add( node );
	        }
	    }
	    
	    return ancestors;	
	} 	
	
	public boolean restore( int branch ) {
		boolean restored = super.restore( branch );
		
		if( !restored ) {
		    return false;
		}		

		for(int i = 0; i < TYPES; i++)
			applyNext[i] = 0;		
        
		for(Iterator i = outEdges.iterator(); i.hasNext(); ) {
			Edge e = (Edge) i.next();
			Node n = e.getTo();
			DependencySet d = e.getDepends();
				
			if( d.branch > branch ) {				
				if( log.isDebugEnabled() ) 
                    log.debug("RESTORE: " + name + " remove edge " + e + " " + d.max() + " " + branch + " " +  n.branch);
				i.remove();		
				
//				System.out.println("  Removing outedge " + this + " --- " + e.getRole() + "--- " + n);
			}			
		}		
		
		return true;
	}
	
	final public boolean removeEdge(Edge edge) {
		boolean removed = outEdges.removeEdge(edge);
		
		if( !removed )
            throw new InternalReasonerException(
                "Trying to remove a non-existing edge " + edge);
		
		return true;
	}
	
	/**
	 * Prune the given node by removing all links going to nominal nodes and recurse
	 * through all successors. No need to remove incoming edges because either the node
	 * is the first one being pruned so the merge function already handled it or
	 * this is a successor node and its successor is also being pruned
	 * 
	 * @param succ
	 * @param ds
	 */
	public void prune( DependencySet ds ) {    
		//add to effected list of queue
		if(abox.getBranch() >=0 && PelletOptions.USE_COMPLETION_QUEUE)    		
			abox.completionQueue.addEffected(abox.getBranch(), this.getName());

		pruned = ds;

		for(int i = 0; i < outEdges.size(); i++) {
            Edge edge = outEdges.edgeAt( i );
            Node succ = edge.getTo();
            
            if( succ.equals( this ) )
                continue;
            else if( succ.isNominal() )
                succ.removeInEdge( edge );
            else
                succ.prune( ds );                
        }
	}

	public void unprune( int branch ) {
	    super.unprune( branch );

        for(int i = 0; i < outEdges.size(); i++) {
            Edge edge = outEdges.edgeAt( i );
            DependencySet d = edge.getDepends();

            if( d.branch <= branch ) {
                Node succ = edge.getTo();
                Role role = edge.getRole();

                if( !succ.inEdges.hasEdge( this, role, succ ) ) {
                    succ.addInEdge( edge );
                }
                
            }
        }
    }

	public String debugString() {
		return name.getName() +
//        " (" + getNominalLevel() + ")" +
        " = " + 
		types[ATOM] + 
		types[ALL] +
		types[SOME] +
		types[OR] +
		types[MIN] +
		types[MAX] +
		types[NOM] +
		"; **" + outEdges + "**" +
		 "; **" + inEdges + "**" + 
		 " --> " + depends + 
		 "";
	}
}
