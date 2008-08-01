// The MIT License
//
// Copyright (c) 2006 Christian Halaschek-Wiener
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

import java.util.Iterator;


/**
 * The completion strategy for incremental consistency checking.
 * 
 * @author Christian Halaschek-Wiener
 */
public class SHOIQIncStrategy extends SHOIQStrategy {
	
	/**
	 * Constructor
	 * 
	 * @param abox
	 */
	public SHOIQIncStrategy(ABox abox) {
		super(abox);
    }
 
    /**
     * Return individuals to which we need to apply the initialization rules
     *  
     * @return
     */
    public Iterator getInitializeIterator() {	
    	return abox.updatedIndividuals.iterator();
	}	
    
    
    /**
     * Return individuals that were newly added
     *  
     * @return
     */
    public Iterator getNewIterator() {	
    	return abox.newIndividuals.iterator();
	}	
    
    
    /**
     * Current the incremental approach is only applicable to KBs which do not have
     * both inverses and nominals. Therefore we do not need the guessing rule.
     */
    protected void applyGuessingRule( IndividualIterator i )  {
        i.reset();                
    }
	

    
    /**
     * There are additional rule that must be fired in the event of incremental additions and deletions in order to guarentee completeness. These are done here.
     */
    public void initialize() {
        //call super - note that the initialization for new individuals will 
        //not be reached in this call; therefore it is performed below
    		super.initialize();
    		
    		//we will also need to add stuff to the queue in the event of a deletion
    		if(abox.getKB().aboxDeletion){
                Iterator i = getInitializeIterator();
                while( i.hasNext() ) {
                    Individual n = (Individual) i.next();

                    if(n.isMerged())
                    		n = (Individual)n.getSame();
                    
                    //apply unfolding rule
                    n.applyNext[Node.ATOM] = 0;
                    applyUnfoldingRule(n);
                    
                    applyAllValues( n );
                    if( n.isMerged() )
                        continue;
                    applyNominalRule( n );
                    if( n.isMerged() )
                        continue;
                    applySelfRule( n );
                    
                    //CHW-added for inc. queue must see if this is bad
                    EdgeList allEdges = n.getOutEdges();
                    for( int e = 0; e < allEdges.size(); e++ ) {
                        Edge edge = allEdges.edgeAt( e );
                        if( edge.getTo().isPruned() )
                            continue;

                        applyPropertyRestrictions( edge );
                        if( n.isMerged() )
                            break;
                    }
                    
                    
                    //There are cases under deletions when a label can be removed from a node which should actually exist; this is demonstated by IncConsistencyTests.testMerge3()
                    //In this case a merge fires before the disjunction is applied on one of the nodes being merged; when the merge is undone by a removal, the disjunction for the
                    //original node must be applied! This is resolved by the following check for all rules which introduce branches; note that this approach can clearly be optimized.
                    //Another possible approach would be to utilize sets of sets in dependency sets, however this is quite complicated.
                    n.applyNext[Node.OR] = 0;
                    applyDisjunctionRule(n);

                    n.applyNext[Node.MAX] = 0;
                    applyMaxRule(n);

                    n.applyNext[Node.MAX] = 0;
                    applyChooseRule(n);
                    
                    //may need to do the same for literals?
                }//end initialize
    		}
    		
    		
    		//if this is an incremental addition we may need to merge nodes and handle
    		//newly added individuals
    		if(abox.getKB().aboxAddition){

    			//merge nodes - note branch must be temporarily set to 0 to ensure that asssertion
    			//will not be restored during backtracking
    			int branch = abox.getBranch();
    			abox.setBranch(0);
    			mergeList.addAll( abox.toBeMerged );	
	        if( !mergeList.isEmpty() )
	            mergeFirst();	        
	        	        
    			//Apply necessary intialization to any new individual added 
	        //Currently, this is a replication of the code from CompletionStrategy.initialize()
	        Iterator newIt = getNewIterator();
	        while( newIt.hasNext() ) {
	            Individual n = (Individual) newIt.next();

	            if( n.isMerged() )
	                continue;

	            n.setChanged( true );

	            applyUniversalRestrictions( n );

	            applyUnfoldingRule( n );

	            applySelfRule( n );

	            EdgeList allEdges = n.getOutEdges();
	            for( int e = 0; e < allEdges.size(); e++ ) {
	                Edge edge = allEdges.edgeAt( e );

	                if( edge.getTo().isPruned() )
	                    continue;

	                applyPropertyRestrictions( edge );

	                if( n.isMerged() )
	                    break;
	            }
	        }
	        
	        if( !mergeList.isEmpty() )
	            mergeFirst();
	        
			abox.setBranch(branch);
    		}

    		
    }
	
}
