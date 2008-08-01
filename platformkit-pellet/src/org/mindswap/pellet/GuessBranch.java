package org.mindswap.pellet;

import java.util.List;

import org.mindswap.pellet.utils.ATermUtils;


/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Clark & Parsia, LLC. <http://www.clarkparsia.com></p>
 *
 * @author Evren sirin
 */
class GuessBranch extends Branch {
	List mergePairs;
	Role r;
	
	int minGuess;

	GuessBranch(ABox abox, CompletionStrategy strategy, Individual x, Role r, int minGuess, int maxGuess, DependencySet ds) {
		super(abox, strategy, x, ds, maxGuess - minGuess + 1);
		
		this.r = r;
		this.minGuess = minGuess;
	}		
		
	protected Branch copyTo(ABox abox) {
	    Individual x = abox.getIndividual(ind.getName());
	    Branch b = new GuessBranch(abox, null, x, r, minGuess, minGuess + tryCount - 1, termDepends);
	    b.anonCount = anonCount;
	    b.nodeCount = nodeCount;
	    b.branch = branch;
	    b.nodeName = ind.getName();
	    b.strategy = strategy;
        b.tryNext = tryNext;

	    return b;
	}
	
	protected void tryBranch() {		
		abox.incrementBranch();
		
		DependencySet ds = termDepends;			
		for(; tryNext < tryCount; tryNext++) {		    
		     // start with max possibility and decrement at each try  
		    int n = minGuess + tryCount - tryNext - 1;
			
			if( log.isDebugEnabled() ) 
                log.debug( 
				    "GUES: (" + (tryNext+1) + "/" + tryCount + 
				    ") at branch (" + branch + ") to  " + ind + 
                    " -> " + r + " -> anon" + (n == 1 ? "" : 
                    (abox.anonCount + 1) + " - anon") +
                    (abox.anonCount + n) + " " + ds);						

			ds = ds.union( new DependencySet( branch ), abox.doExplanation() );
			
			// add the max cardinality for guess
			strategy.addType( ind, ATermUtils.makeNormalizedMax(r.getName(), n, ATermUtils.TOP), ds);
			
			// create n distinct nominal successors
            Individual[] y = new Individual[n];
            for(int c1 = 0; c1 < n; c1++) {
                y[c1] = strategy.createFreshIndividual( true );                

                strategy.addEdge( ind, r, y[c1], ds );
                for(int c2 = 0; c2 < c1; c2++)
                    y[c1].setDifferent( y[c2], ds );
            }

            // add the min cardinality restriction just to make early clash detection easier
			strategy.addType( ind, ATermUtils.makeMin(r.getName(), n, ATermUtils.TOP), ds);

			
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
        
		abox.setClash(Clash.unexplained(ind, ds));
	
		return;
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
	 * Currently this method does nothing as we cannot support incremental reasoning when
	 * both nominals and inverses are used - this is the only case when the guess rule is needed.
	 *
	 * @param index The shift index
	 */
	protected void shiftTryNext(int openIndex){
		//decrement trynext
		//tryNext--;		
	}
	
}