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
 * Created on May 4, 2004
 */
package org.mindswap.pellet;


import java.util.HashSet;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;


class DisjunctionBranch extends Branch {
	ATermAppl disjunction;
	ATermAppl[] disj;
    DependencySet[] prevDS;
	int[] order;
	
	DisjunctionBranch(ABox abox, CompletionStrategy completion, Node x, ATermAppl disjunction, DependencySet ds, ATermAppl[] disj) {
		super(abox, completion, x, ds, disj.length);
		
		this.disjunction = disjunction;
		this.disj = disj;
        this.prevDS = new DependencySet[disj.length];
		this.order = new int[disj.length];
        for(int i = 0; i < disj.length; i++)
            order[i] = i;
	}
    
    protected String getDebugMsg() {
        return "DISJ: Branch (" + branch + ") try (" + (tryNext + 1) + "/" + tryCount
            + ") " + ind.getName() + " " +  disj[tryNext] + " " + disjunction ;
    }
	
	protected Branch copyTo(ABox abox) {
	    Individual x = abox.getIndividual(ind.getName());
	    DisjunctionBranch b = new DisjunctionBranch(abox, null, x, disjunction, termDepends, disj);
	    b.anonCount = anonCount;
	    b.nodeCount = nodeCount;
	    b.branch = branch;
	    b.nodeName = ind.getName();
	    b.strategy = strategy;
        b.tryNext = tryNext;

        b.prevDS = new DependencySet[disj.length];
        System.arraycopy(prevDS, 0, b.prevDS, 0, tryNext );
        b.order = new int[disj.length];
        System.arraycopy(prevDS, 0, b.prevDS, 0, disj.length );

	    return b;
	}
	
	/**
	 * This function finds preferred disjuncts using different heuristics.
	 * 
	 * 1) A common kind of axiom that exist in a lot of  ontologies is 
	 * in the form A = and(B, some(p, C)) which is absorbed into an
	 * axiom like sub(B, or(A, all(p, not(C))). For these disjunctions,
	 * we always prefer picking all(p, C) because it causes an immediate 
	 * clash for the instances of A so there is no overhead. For 
	 * non-instances of A, this builds better pseudo models     
     * 
	 * @return
	 */
	private int preferredDisjunct() {
        if( disj.length != 2 ) 
            return -1;
        
        if( ATermUtils.isPrimitive( disj[0] ) && 
            ATermUtils.isAllValues( disj[1] ) &&
            ATermUtils.isNot( (ATermAppl) disj[1].getArgument( 1 ) ) )
            return 1;
            	                
        if( ATermUtils.isPrimitive( disj[1] ) && 
            ATermUtils.isAllValues( disj[0] ) &&
            ATermUtils.isNot( (ATermAppl) disj[0].getArgument( 1 ) ) )
            return 0;
        
        return -1;
	}
	
    public void setLastClash( DependencySet ds ) {
    		super.setLastClash( ds );
    		if(tryNext>=0)
    			prevDS[tryNext] = ds;
    }
    
	protected void tryBranch() {			
		abox.incrementBranch();
		
		int[] stats = null;
		if( PelletOptions.USE_DISJUNCT_SORTING ) {
		    stats = (int[]) abox.disjBranchStats.get(disjunction);    
		    if(stats == null) {
		        int preference = preferredDisjunct();
		        stats = new int[disj.length];
		        for(int i = 0; i < disj.length; i++) {
		            stats[i] = (i != preference) ? 0 : Integer.MIN_VALUE;
		        }
		        abox.disjBranchStats.put(disjunction, stats); 
		    }
		    if(tryNext > 0) {
		        stats[order[tryNext-1]]++;
			}
			if(stats != null) {
			    int minIndex = tryNext;
			    int minValue = stats[tryNext];
		        for(int i = tryNext + 1; i < stats.length; i++) {
		            boolean tryEarlier = ( stats[i] < minValue );		                
		            
		            if( tryEarlier ) {
		                minIndex = i;
		                minValue = stats[i];
		            }
		        }
		        if(minIndex != tryNext) {
		            ATermAppl selDisj = disj[minIndex];
		            disj[minIndex] = disj[tryNext];
		            disj[tryNext] = selDisj;
		            order[minIndex] = tryNext;
		            order[tryNext] = minIndex;	            
		        }
			}
		}

		for(; tryNext < tryCount; tryNext++) {
			ATermAppl d = disj[tryNext];

			if(PelletOptions.USE_SEMANTIC_BRANCHING) {
				for(int m = 0; m < tryNext; m++)
					strategy.addType(ind, ATermUtils.negate( disj[m] ), prevDS[m]);
			}

			DependencySet ds = null;
			if(tryNext == tryCount - 1 && !PelletOptions.SATURATE_TABLEAU) {
				ds = termDepends;
				for(int m = 0; m < tryNext; m++)
					ds = ds.union(prevDS[m], abox.doExplanation());

				//CHW - added for incremental reasoning and rollback through deletions
				if(PelletOptions.USE_INCREMENTAL_DELETION)
					ds.explain = termDepends.explain;
				else
					ds.remove(branch);
			}
			else {
				//CHW - Changed for tracing purposes
				if(PelletOptions.USE_INCREMENTAL_DELETION)
					ds = termDepends.union(new DependencySet(branch), abox.doExplanation());
				else{
					ds = new DependencySet(branch);
					//added for tracing
					ds.explain = new HashSet();
					ds.explain.addAll(termDepends.explain);						
				}
            }
            
			if( log.isDebugEnabled() ) 
                log.debug( getDebugMsg() );		
			
			ATermAppl notD = ATermUtils.negate(d);
			DependencySet clashDepends = PelletOptions.SATURATE_TABLEAU ? null : ind.getDepends(notD);
			if(clashDepends == null) {
			    strategy.addType(ind, d, ds);
				// we may still find a clash if concept is allValuesFrom
				// and there are some conflicting edges
				if(abox.isClosed()) 
					clashDepends = abox.getClash().depends;
			}
			else {
			    clashDepends = clashDepends.union(ds, abox.doExplanation());
			}
			
			// if there is a clash
			if(clashDepends != null) {				
				if( log.isDebugEnabled() ) {
					Clash clash = abox.isClosed() ? abox.getClash() : Clash.atomic(ind, clashDepends, d);
                    log.debug("CLASH: Branch " + branch + " " + clash + "!" + " " + clashDepends.explain);
				}
				
				if( PelletOptions.USE_DISJUNCT_SORTING ) {
				    if(stats == null) {
				        stats = new int[disj.length];
				        for(int i = 0; i < disj.length; i++)
				            stats[i] = 0;
				        abox.disjBranchStats.put(disjunction, stats); 
				    }
					stats[order[tryNext]]++;
				}
				
				// do not restore if we do not have any more branches to try. after
				// backtrack the correct branch will restore it anyway. more
				// importantly restore clears the clash info causing exceptions
				if(tryNext < tryCount - 1 && clashDepends.contains(branch)) {
				    // do not restore if we find the problem without adding the concepts 
				    if(abox.isClosed()) {
						// we need a global restore here because one of the disjuncts could be an 
					    // all(r,C) that changed the r-neighbors
				        strategy.restore(this);
						
						// global restore sets the branch number to previous value so we need to
						// increment it again
						abox.incrementBranch();
				    }
					
                    setLastClash( clashDepends.copy() );
				}
				else {
				    // set the clash only if we are returning from the function
					if(abox.doExplanation()) {
					    ATermAppl positive = (ATermUtils.isNot(notD) ? d : notD);
					    abox.setClash(Clash.atomic(ind, clashDepends.union(ds, abox.doExplanation()), positive));
					}
					else
					    abox.setClash(Clash.atomic(ind, clashDepends.union(ds, abox.doExplanation())));

					//CHW - added for inc reasoning
					if(PelletOptions.USE_INCREMENTAL_DELETION)
						abox.getKB().getDependencyIndex().addCloseBranchDependency(this, abox.getClash().depends);

			        return;
				}
			} 
			else 
				return;
		}
		
		// this code is not unreachable. if there are no branches left restore does not call this 
		// function, and the loop immediately returns when there are no branches left in this
		// disjunction. If this exception is thrown it shows a bug in the code.
		throw new InternalReasonerException("This exception should not be thrown!");
	}
	
	
	/**
	 * Added for to re-open closed branches.
	 * This is needed for incremental reasoning through deletions
	 * 
	 * @param index The shift index
	 */
	protected void shiftTryNext(int openIndex){
		//save vals
		int ord = order[openIndex];
		ATermAppl dis = disj[openIndex];
		DependencySet preDS = prevDS[openIndex];

		//TODO: also need to handle semantic branching	
		if(PelletOptions.USE_SEMANTIC_BRANCHING){
//			if(this.ind.getDepends(ATermUtils.makeNot(dis)) != null){
//				//check if the depedency is the same as preDS - if so, then we know that we added it
//			}
		}
		
		//need to shift both prevDS and next and order
		//disjfirst
		for(int i = openIndex; i < disj.length-1; i++){
			disj[i] = disj[i+1];
			prevDS[i] = prevDS[i+1];
			order[i] = order[i];
		}

		//move open label to end
		disj[disj.length-1] = dis;
		prevDS[disj.length-1] = null;
		order[disj.length-1] = disj.length-1;
		
		//decrement trynext
		tryNext--;		
	}
	
	
	/**
	 * 
	 */
	public void printLong(){
		for(int i = 0; i < disj.length; i++){
			System.out.println("Disj[" + i + "] " + disj[i]);
			System.out.println("prevDS[" + i + "] " + prevDS[i]);
			System.out.println("order[" + i + "] " + order[i]);
		}

		//decrement trynext
		System.out.println("trynext: " + tryNext);
	}

}