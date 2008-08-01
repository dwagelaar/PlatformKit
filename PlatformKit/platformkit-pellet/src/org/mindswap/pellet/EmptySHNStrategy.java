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

/*
 * Created on Aug 29, 2004
 */
package org.mindswap.pellet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Bool;
import org.mindswap.pellet.utils.Timer;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;

/**
 * Completion strategy for a SHN KB that does not have individuals in the ABox.
 * When ABox is empty completion always starts with a single root individual
 * that represents the concept whose satisfiability is being searched. Since
 * there are no inverse roles in SHN completion starts with root node and moves
 * towards the leaves. Once a node's satisfiability has been established there
 * is no need to reevaluate that value.
 * 
 * @author Evren Sirin
 */
public class EmptySHNStrategy extends SHOIQStrategy {       
    private LinkedList mayNeedExpanding;

    private Individual root;
//    private boolean useCaching;
    
    private Map cachedNodes;

    public static final int NONE = 0x00;
    public static final int HIT  = 0x01;
    public static final int MISS = 0x02;
    public static final int FAIL = 0x04;
    public static final int ADD  = 0x08;
    public static final int ALL  = 0x0F;

    public static int SHOW_CACHE_INFO = NONE;

    public EmptySHNStrategy(ABox abox) {
        super(abox);
        this.blocking = new SubsetBlocking();
    }
    
    boolean supportsPseudoModelCompletion() {
        return false;
    }
    
    public void initialize() {     
//        useCaching = !abox.getKB().getExpressivity().hasDomain;
        mergeList = new ArrayList();
        
        cachedNodes = new HashMap();
		
        root = (Individual) abox.getNodes().iterator().next();
		root.setChanged(true);		
		applyUniversalRestrictions( root );
		
		abox.setBranch( 1 );
		abox.treeDepth = 1;
		abox.changed = true;
		abox.setComplete( false );		
        abox.setInitialized( true );
    }


    ABox complete() {
        if( log.isDebugEnabled() ) 
            log.debug("************  EmptySHNStrategy  ************");
        
        if(abox.getNodes().isEmpty()) {
            abox.setComplete(true);
            return abox;
        }
        else if(abox.getNodes().size() > 1)
            throw new RuntimeException(
                "EmptySHNStrategy can only be used with an ABox that has a single individual.");

        initialize();
        
        mayNeedExpanding = new LinkedList();
        mayNeedExpanding.add( root );

        while( !abox.isComplete() && !abox.isClosed() ) {
            Individual x = getNextIndividual();

            if( x == null ) {
                abox.setComplete( true );
                break;
            }
            
            if( log.isDebugEnabled() ) {
                log.debug("Starting with node " + x);
                abox.printTree();
                
                abox.validate();
            }
            
            expand( x );
            
            if(abox.isClosed()) {
                if( log.isDebugEnabled() ) 
                    log.debug("Clash at Branch (" + abox.getBranch() + ") " + abox.getClash());

                if( backtrack() )
                    abox.setClash( null );
                else
                    abox.setComplete( true );
            }
        }
        
        if( log.isDebugEnabled() ) abox.printTree();
        
        if( PelletOptions.USE_ADVANCED_CACHING ) {
            // if completion tree is clash free cache all sat concepts
	        if( !abox.isClosed() ) {
	            for(Iterator i = abox.getIndIterator(); i.hasNext();) {
	                Individual ind = (Individual) i.next();
	                ATermAppl c = (ATermAppl) cachedNodes.get( ind );
	                if( c != null ) {
					    if( abox.cache.putSat( c, true ) ) {
					        if((EmptySHNStrategy.SHOW_CACHE_INFO & EmptySHNStrategy.ADD) != 0) 
					            System.out.println( "+++ Cache sat concept " + c );
					        if(ATermUtils.isAnd( c )) {
					            ATermList list = (ATermList) c.getArgument(0);
					            while(  !list.isEmpty() ) {
					                ATermAppl d = (ATermAppl) list.getFirst();
								    if( abox.cache.putSat( d, true ) ) 
								        if((EmptySHNStrategy.SHOW_CACHE_INFO & EmptySHNStrategy.ADD) != 0) 
								            System.out.println( "+++ Cache sat concept " + d );
								    list = list.getNext();
					            }					            
					        }					        
					    }
	                }
	            }
	        }
        }
        
        return abox;
    }
    
    private Individual getNextIndividual() {
        Node next = null;
        while( !mayNeedExpanding.isEmpty() ) {
            next = (Node) mayNeedExpanding.get( 0 );
            if( next instanceof Literal ) {
                next = null;
                mayNeedExpanding.remove( 0 );
            }
            else
                break;
        }
        
        return (Individual) next;
    }
    
    private void expand(Individual x) {
        if( blocking.isBlocked(x) ) {
            mayNeedExpanding.remove( 0 );
            return;
        }
        
        if( /*useCaching &&*/ PelletOptions.USE_ADVANCED_CACHING ) {
            Timer t = abox.getKB().timers.startTimer( "cache" );
	        Bool cachedSat = cachedSat( x );
	        t.stop();
	        if( cachedSat.isKnown() ) {
	            if( cachedSat.isTrue() )
	                mayNeedExpanding.remove( 0 );
	            else {
	                // set the clash information to be the union of all types
	                DependencySet ds = DependencySet.EMPTY;
	                for(Iterator i = x.getTypes().iterator(); i.hasNext();) {
                        ATermAppl c = (ATermAppl) i.next();
                        ds = ds.union( x.getDepends( c ), abox.doExplanation() );
                    }
	                abox.setClash( Clash.atomic( x, ds ) );
	            }
	            return;
	        }
        }
        
        do {	        
	        applyUnfoldingRule(x);
	        if(abox.isClosed()) return;
		    
	        applyDisjunctionRule(x);
	        if(abox.isClosed()) return;
	        
	        if( x.canApply( Node.ATOM ) || x.canApply( Node.OR ) )
                continue;
	        
	        // TODO: do we want to check blocking here again?

		    applySomeValuesRule(x);
		    if(abox.isClosed()) return;
		
		    applyMinRule(x);
		    if(abox.isClosed()) return;
		                            
            // we don't have any inverse properties but we could have 
            // domain restrictions which means we might have to re-apply
            // unfolding and disjunction rules
            if( x.canApply( Node.ATOM ) || x.canApply( Node.OR ) )
                continue;     

            applyChooseRule(x);
            if(abox.isClosed()) return;
            
	        applyMaxRule(x);
	        if(abox.isClosed()) return;		    
            
//            applyLiteralRule(x);
//            if(abox.isClosed()) return;         
        }    
	    while( x.canApply(Node.ATOM) || x.canApply(Node.OR) || x.canApply(Node.SOME) || x.canApply(Node.MIN) );
        
        mayNeedExpanding.remove( 0 );
        
        int insert = ( PelletOptions.SEARCH_TYPE == PelletOptions.DEPTH_FIRST )
        	? 0
        	: mayNeedExpanding.size();
        
        mayNeedExpanding.addAll( insert, x.getSortedSuccessors() );                        
    }
    
    private ATermAppl createConcept( Individual x ) {
        Set types = new HashSet( x.getTypes() );
        types.remove( ATermUtils.TOP );
        types.remove( ATermUtils.makeValue( x.getName() ) );
        
        if( types.isEmpty() ) {
        	return ATermUtils.TOP;
        }
        
        int count = 0;
        ATerm[] terms = new ATerm[ types.size() ];        
        for(Iterator i = types.iterator(); i.hasNext(); ) {
            ATermAppl c = (ATermAppl) i.next();
            if( !ATermUtils.isAnd( c ) )
                terms[ count++ ] = c ;
        }
        
        return ATermUtils.makeAnd( ATermUtils.toSet( terms, count ) );   
    }
    
//  public static int earlyCache = 0;
//  public static int doubleCache = 0;
//  public static int multipleCache = 0;
    
    private Bool cachedSat(Individual x) {
        if( x.equals( root ) )
            return Bool.UNKNOWN;
        
        ATermAppl c = createConcept( x );
        
        if( cachedNodes.containsValue( c ) ) {
//            earlyCache++;
            if((EmptySHNStrategy.SHOW_CACHE_INFO & EmptySHNStrategy.HIT) != 0)
                System.out.println("already searching for " + c);
            return Bool.TRUE;
        }
        
        Bool sat = abox.getCachedSat( c );
        
        if( sat.isUnknown() && ATermUtils.isAnd( c ) ) {
	        ATermList concepts = (ATermList) c.getArgument( 0 );
	        if( concepts.getLength() == 2 ) {
	            ATermAppl c1 = (ATermAppl) concepts.getFirst();
	            ATermAppl c2 = (ATermAppl) concepts.getLast();
	            CachedNode cached1 = abox.getCached( c1 );
	            CachedNode cached2 = abox.getCached( c2 );
	            if( cached1 != null && cached1.isComplete() && cached2 != null && cached2.isComplete() ) {
	                sat = abox.mergable( cached1.node, cached2.node,
	                    cached1.depends.isIndependent() && cached2.depends.isIndependent());
	                if( sat.isKnown() ) {
//	                    doubleCache++;
	                	abox.cache.putSat( c, sat.isTrue() );	                        
	                }
	            }
	        }
//	        else {
//	            boolean allCached = true;
//	            for( ATermList list = concepts; !list.isEmpty(); list = list.getNext() ) {
//	                ATermAppl t = (ATermAppl) list.getFirst();
//		            CachedNode cached = abox.getCached( t );
//		            if( cached == null || !cached.isComplete() ) {
//	                    allCached = false;
//	                    break;
//	                }	                    
//	            }
//	            if(allCached) {
////	                multipleCache++;
//	                System.out.println( "Cache possibility " + concepts);
//	            }
//	        }
	    }
        
        if( sat.isUnknown() ) {
            if((EmptySHNStrategy.SHOW_CACHE_INFO & EmptySHNStrategy.MISS) != 0) 
                System.out.println( "??? Cache miss for " + c );
            cachedNodes.put( x, c );            
        }
        else
            if((EmptySHNStrategy.SHOW_CACHE_INFO & EmptySHNStrategy.HIT) != 0) 
                System.out.println( "*** Cache hit for " + c + " sat = " + sat);            
        
        return sat;
    }

	public void restore(Branch br) {
//		Timer timer = timers.startTimer("restore");
	    	    
	    Node clashNode = abox.getClash().node;
	    List clashPath = clashNode.getPath();
	    clashPath.add( clashNode.getName() );
		
		abox.setBranch(br.branch);
		abox.setClash(null);
		abox.anonCount = br.anonCount;
		
		mergeList.clear();
		
		List nodeList = abox.getNodeNames();
		Map nodes = abox.getNodeMap();
		
		if( log.isDebugEnabled() ) {
            log.debug("RESTORE: Branch " + br.branch);
            if( br.nodeCount < nodeList.size())
                log.debug("Remove nodes " + nodeList.subList(br.nodeCount, nodeList.size()));
        }
		for(int i = 0; i < nodeList.size(); i++) {
			ATerm x = (ATerm) nodeList.get(i);
			
			Node node = abox.getNode(x);
			if(i >= br.nodeCount) {
				nodes.remove(x);
				ATermAppl c = (ATermAppl) cachedNodes.remove( node );
				if(c != null && PelletOptions.USE_ADVANCED_CACHING) {
				    if(clashPath.contains(x)) {
					    if((EmptySHNStrategy.SHOW_CACHE_INFO & EmptySHNStrategy.ADD) != 0) 
					        System.out.println( "+++ Cache unsat concept " + c );
				        abox.cache.putSat( c, false );
				    }
				    else
					    if((EmptySHNStrategy.SHOW_CACHE_INFO & EmptySHNStrategy.ADD) != 0) 
					        System.out.println( "--- Do not cache concept " + c + " " + x + " " + clashNode + " " + clashPath);				        
				}
			}
			else {
				node.restore(br.branch);
				
				// FIXME should we look at the clash path or clash node
				if( node.equals( clashNode ) )
				    cachedNodes.remove( node );
			}
		}		
		nodeList.subList(br.nodeCount, nodeList.size()).clear();

		for(Iterator i = abox.getIndIterator(); i.hasNext(); ) {
			Individual ind = (Individual) i.next();
			applyAllValues(ind);
		}		
		
		if( log.isDebugEnabled() ) abox.printTree();
			
//		timer.stop();
	}
	
    protected boolean backtrack() {
        boolean branchFound = false;

        while(!branchFound) {
            int lastBranch = abox.getClash().depends.max();

            if(lastBranch <= 0) return false;

            List branches = abox.getBranches();
            Branch newBranch = null;
            if( lastBranch <= branches.size() ) {
                branches.subList(lastBranch, branches.size()).clear();
                newBranch = (Branch) branches.get(lastBranch - 1);
    
                if( log.isDebugEnabled() ) 
                    log.debug("JUMP: " + lastBranch);
                if(newBranch == null || lastBranch != newBranch.branch)
                    throw new RuntimeException(
                        "Internal error in reasoner: Trying to backtrack branch "
                            + lastBranch + " but got " + newBranch);
    
                if(newBranch.tryNext < newBranch.tryCount)
                    newBranch.setLastClash( abox.getClash().depends );
    
                newBranch.tryNext++;
                
                if(newBranch.tryNext < newBranch.tryCount) {
                    restore(newBranch);
    
                    branchFound = newBranch.tryNext();
                }                    
            }

            if(!branchFound) {
                abox.getClash().depends.remove(lastBranch);
                if( log.isDebugEnabled() ) 
                    log.debug("FAIL: " + lastBranch);
            }
            else {
                mayNeedExpanding = new LinkedList((List) newBranch.get("mnx"));
                if( log.isDebugEnabled() ) 
                    log.debug("MNX : " + mayNeedExpanding);
            }

        }
        
        abox.validate();

        return branchFound;
    }

    void addBranch(Branch newBranch) {
        super.addBranch( newBranch );

        newBranch.put("mnx", new ArrayList(mayNeedExpanding));
    }
}