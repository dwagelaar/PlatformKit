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

import java.util.Iterator;
import java.util.List;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;



class MaxBranch extends Branch {
	List mergePairs;
	Role r;
	int n;
	ATermAppl qualification;
	DependencySet[] prevDS;

	MaxBranch(ABox abox, CompletionStrategy strategy, Individual x, Role r, int n, ATermAppl qualification, List mergePairs, DependencySet ds) {
		super(abox, strategy, x, ds, mergePairs.size());
		
		this.r = r;
		this.n = n;
		this.mergePairs = mergePairs;
		this.qualification = qualification;
        this.prevDS = new DependencySet[mergePairs.size()];
	}		
		
	protected Branch copyTo(ABox abox) {
	    Individual x = abox.getIndividual(ind.getName());
	    MaxBranch b = new MaxBranch(abox, null, x, r, n, qualification, mergePairs, termDepends);
	    b.anonCount = anonCount;
	    b.nodeCount = nodeCount;
	    b.branch = branch;
	    b.nodeName = ind.getName();
	    b.strategy = strategy;
        b.tryNext = tryNext;
        b.prevDS = new DependencySet[prevDS.length];
        System.arraycopy(prevDS, 0, b.prevDS, 0, tryNext );
        
	    return b;
	}
	
	protected void tryBranch() {		
		abox.incrementBranch();
		
		//we must re-add this individual to the max queue. This is because we may still need to keep 
		//applying the max rule for additional merges		
		//recreate the label for the individuals
		ATermAppl maxCon = ATermUtils.makeMax(this.r.getName(), this.n, this.qualification);
		//normalize the label
		maxCon = ATermUtils.normalize(maxCon);
		//create the queue element
		QueueElement qElement = new QueueElement(this.ind.getName(), maxCon);
		//add to the queue
		abox.completionQueue.add(qElement, abox.completionQueue.MAXLIST);
		abox.completionQueue.add(qElement, abox.completionQueue.CHOOSELIST);
		
		
		DependencySet ds = termDepends;			
		for(; tryNext < tryCount; tryNext++) {		
			if(PelletOptions.USE_SEMANTIC_BRANCHING) {
				for(int m = 0; m < tryNext; m++) {
					NodeMerge nm = (NodeMerge) mergePairs.get(m);			
					Node y = abox.getNode(nm.y);
					Node z = abox.getNode(nm.z);
					y.setDifferent( z, prevDS[m]);
					//strategy.addType( y, ATermUtils.makeNot( ATermUtils.makeValue( z.getName() ) ), prevDS[m] );
				}
			}
			
			NodeMerge nm = (NodeMerge) mergePairs.get(tryNext);			
			Node y = abox.getNode(nm.y);
			Node z = abox.getNode(nm.z);
						
			if( log.isDebugEnabled() ) 
                log.debug( 
				    "MAX : (" + (tryNext+1) + "/" + mergePairs.size() + 
				    ") at branch (" + branch + ") to  " + ind + 
				    " for prop " + r + " qualification " + qualification + 
				    " merge " + y + " -> " + z + " " + ds);						
			
			ds = ds.union(new DependencySet(branch), abox.doExplanation());
			
			// max cardinality merge also depends on all the edges 
			// between the individual that has the cardinality and 
			// nodes that are going to be merged 
			EdgeList rNeighbors = ind.getRNeighborEdges(r);
			boolean yEdge = false, zEdge = false;
			for( Iterator i = rNeighbors.iterator(); i.hasNext(); ) {
				Edge edge = (Edge) i.next();
				Node neighbor = edge.getNeighbor( ind );
				
				if( neighbor.equals( y ) ) {
					ds = ds.union( edge.getDepends(), abox.doExplanation() );
					yEdge = true;
				}
				else if( neighbor.equals( z ) ) {
					ds = ds.union( edge.getDepends(), abox.doExplanation() );
					zEdge = true;
				}
			}
			
			// if there is no edge coming into the node that is
			// going to be merged then it is not possible that
			// they are affected by the cardinality restriction
			// just die instead of possibly unsound results
			if(!yEdge || !zEdge)
			    throw new InternalReasonerException( 
			        "An error occurred related to the max cardinality restriction about "  + r);
			
			// if the neighbor nodes did not have the qualification
			// in their type list they would have not been affected
			// by the cardinality restriction. so this merges depends
			// on their types
			ds = ds.union( y.getDepends( qualification ), abox.doExplanation() );
			ds = ds.union( z.getDepends( qualification ), abox.doExplanation() );
			
            // if there were other merges based on the exact same cardinality
			// restriction then this merge depends on them, too (we wouldn't
			// have to merge these two nodes if the previous merge did not
			// eliminate some other possibilities)
	        for( int b = abox.getBranches().size() -2; b >=0; b-- ) {
	        	Object branch = abox.getBranches().get( b );
	        	if( branch instanceof MaxBranch ) {
	        		MaxBranch prevBranch = (MaxBranch) branch;
	        		if( prevBranch.ind.equals( ind )
	        			&& prevBranch.r.equals( r )
	        			&& prevBranch.qualification.equals( qualification ) ) {
	        			ds.add( prevBranch.branch );
	        		}
	        		else {
	        			break;
	        		}
	        	}
	        	else
	        		break;
	        }
			
			strategy.mergeTo(y, z, ds);

//			abox.validate();
			
			boolean earlyClash = abox.isClosed();
			if(earlyClash) {
				if( log.isDebugEnabled() ) 
                    log.debug("CLASH: Branch " + branch + " " + abox.getClash() + "!");

				DependencySet clashDepends = abox.getClash().depends;
				
				if(clashDepends.contains(branch)) {
					// we need a global restore here because the merge operation modified three
					// different nodes and possibly other global variables
					strategy.restore(this);
					
					// global restore sets the branch number to previous value so we need to
					// increment it again
					abox.incrementBranch();
									
                    setLastClash( clashDepends );
				}
				else
					return;
			} 
			else 
				return;	
		}
		
        ds = getCombinedClash();
        
        //CHW - removed for rollback through deletions
        if(!PelletOptions.USE_INCREMENTAL_DELETION)
        		ds.remove( branch );
        
		if(abox.doExplanation())
		    abox.setClash(Clash.maxCardinality(ind, ds, r.getName(), n));
		else
		    abox.setClash(Clash.maxCardinality(ind, ds));
	
		return;
	}
	
    public void setLastClash( DependencySet ds ) {
        super.setLastClash( ds );
        if(tryNext>=0)
        		prevDS[tryNext] = ds;
    }
	
	public String toString() {
		if(tryNext < mergePairs.size())
			return "Branch " + branch + " max rule on " + ind + " merged  " + mergePairs.get(tryNext);
		
		return "Branch " + branch + " max rule on " + ind + " exhausted merge possibilities";
	}
	
	
	
	/**
	 * Added for to re-open closed branches.
	 * This is needed for incremental reasoning through deletions
	 * 
	 * @param index The shift index
	 */
	protected void shiftTryNext(int openIndex){
		//save vals
		DependencySet preDS = prevDS[openIndex];

		//re-open the merge pair
		NodeMerge nm = (NodeMerge)mergePairs.remove(openIndex);
		mergePairs.add(nm);		

		//shift the previous ds
		for(int i = openIndex; i < mergePairs.size(); i++){
			prevDS[i] = prevDS[i+1];
		}

		//move open label to end
		prevDS[mergePairs.size()-1] = null;
		
		//decrement trynext
		tryNext--;		
	}
	
}