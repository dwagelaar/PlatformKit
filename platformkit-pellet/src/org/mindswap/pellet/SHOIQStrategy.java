package org.mindswap.pellet;

import java.util.Iterator;
import java.util.List;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.Timer;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Clark & Parsia, LLC. <http://www.clarkparsia.com></p>
 *
 * @author Evren Sirin
 */
public class SHOIQStrategy extends CompletionStrategy {
    public SHOIQStrategy(ABox abox) {
        super(abox, PelletOptions.FORCE_OPTIMIZED_BLOCKING 
            ? (Blocking) new OptimizedDoubleBlocking() 
            : (Blocking) new DoubleBlocking() );
    }
        
    boolean supportsPseudoModelCompletion() {
        return true;
    }        

    protected boolean backtrack() {
        boolean branchFound = false;

        while(!branchFound) {
            completionTimer.check();
            
            int lastBranch = abox.getClash().depends.max();

            if(lastBranch <= 0)            	
            		return false;            
            else if(lastBranch > abox.getBranches().size())
                throw new InternalReasonerException(
                    "Backtrack: Trying to backtrack to branch " + lastBranch
                        + " but has only " + abox.getBranches().size()
                        + " branches");
            else if(PelletOptions.USE_INCREMENTAL_DELETION){            	
            		//get the last branch
            		Branch br = (Branch)abox.getBranches().get(lastBranch-1);
            		
            		//if this is the last disjunction, merge pair, etc. for the branch (i.e, br.tryNext == br.tryCount-1)  and there are no other branches to test (ie. abox.getClash().depends.size()==2), 
            		//then update depedency index and return false
	            	if( (br.tryNext == br.tryCount-1) && abox.getClash().depends.size()==2){
		        		abox.getKB().getDependencyIndex().addCloseBranchDependency(br, abox.getClash().depends);
		        		return false;
            		}
            }
            List branches = abox.getBranches();
            
            
            //CHW - added for incremental deletion support
            if(PelletOptions.USE_TRACING && PelletOptions.USE_INCREMENTAL_CONSISTENCY){
            		//we must clean up the KB dependecny index
            		List brList = branches.subList(lastBranch, branches.size());
            		for(Iterator it = brList.iterator(); it.hasNext();){
            			//remove from the dependency index
            			abox.getKB().getDependencyIndex().removeBranchDependencies((Branch)it.next());
            		}            
            		brList.clear();
            }else{
            		//old approach
            		branches.subList(lastBranch, branches.size()).clear();            	
            }
            
            Branch newBranch = (Branch) branches.get(lastBranch - 1);


            if( log.isDebugEnabled() )
                log.debug("JUMP: Branch " + lastBranch);
            
            if(lastBranch != newBranch.branch)
                throw new InternalReasonerException(
                    "Backtrack: Trying to backtrack to branch " + lastBranch
                        + " but got " + newBranch.branch);

            if(newBranch.tryNext < newBranch.tryCount)
                newBranch.setLastClash( abox.getClash().depends );

            newBranch.tryNext++;

            if(newBranch.tryNext < newBranch.tryCount) {
                restore(newBranch);

                branchFound = newBranch.tryNext();
            }
            else
                abox.getClash().depends.remove(lastBranch);
            if(!branchFound) {
                if( log.isDebugEnabled() )
                    log.debug( "FAIL: Branch " + lastBranch );
            }
        }

        return branchFound;
    }

    ABox complete() {
        Timer t;
        
        completionTimer.start();

        // FIXME the expressivity of the completion graph might be different
        // than th original ABox
        Expressivity expressivity = abox.getKB().getExpressivity();        
        boolean fullDatatypeReasoning =
            PelletOptions.USE_FULL_DATATYPE_REASONING &&
            (expressivity.hasCardinalityD() || expressivity.hasKeys());
        
        initialize();
        
        while(!abox.isComplete()) {
            while(abox.changed && !abox.isClosed()) {                
                completionTimer.check();

                abox.changed = false;
               
                if( log.isDebugEnabled() ) {
                    log.debug("Branch: " + abox.getBranch() +
                        ", Depth: " + abox.treeDepth + ", Size: " + abox.getNodes().size() + 
                        ", Mem: " + (Runtime.getRuntime().freeMemory()/1000) + "kb");
                    abox.validate();
                    printBlocked();
                    abox.printTree();
                }

                IndividualIterator i = abox.getIndIterator();

                if( !PelletOptions.USE_PSEUDO_NOMINALS ) {
                    t = timers.startTimer( "rule-nominal");
                    if(PelletOptions.USE_COMPLETION_QUEUE){
	                    //init the end pointer for the queue
	                    abox.completionQueue.init(CompletionQueue.NOMLIST);
		    	            while(abox.completionQueue.hasNext(CompletionQueue.NOMLIST)){	               
		    	            		applyNominalRule((QueueElement)abox.completionQueue.getNext(CompletionQueue.NOMLIST));
			            		if(abox.isClosed()) break;  
		    	            }
                    }
                    else
                    	applyNominalRule(i);
                    t.stop();
	                if(abox.isClosed()) break;
                }

                t = timers.startTimer("rule-guess");     
                if(PelletOptions.USE_COMPLETION_QUEUE){
                    //init the end pointer for the queue                
	                abox.completionQueue.init(CompletionQueue.GUESSLIST);
		            while(abox.completionQueue.hasNext(CompletionQueue.GUESSLIST)){	                                   		
		            	applyGuessingRule((QueueElement)abox.completionQueue.getNext(CompletionQueue.GUESSLIST));
	            		if(abox.isClosed()) break;	  
		            } 
                }
                else
                	applyGuessingRule(i);
                t.stop();
                if(abox.isClosed()) break;
                
                t = timers.startTimer("rule-choose");
                if(PelletOptions.USE_COMPLETION_QUEUE){
	                abox.completionQueue.init(CompletionQueue.CHOOSELIST);
		            while(abox.completionQueue.hasNext(CompletionQueue.CHOOSELIST)){	               	                                     		
		            		applyChooseRule((QueueElement)abox.completionQueue.getNext(CompletionQueue.CHOOSELIST));
		            		if(abox.isClosed()) break;
		            }
                }
                else
                	applyChooseRule(i);
                t.stop();
                if(abox.isClosed()) break;
                
                t = timers.startTimer("rule-max");
                if(PelletOptions.USE_COMPLETION_QUEUE){
	                abox.completionQueue.init(CompletionQueue.MAXLIST);
		            while(abox.completionQueue.hasNext(CompletionQueue.MAXLIST)){	               	                                     		
		            		applyMaxRule((QueueElement)abox.completionQueue.getNext(CompletionQueue.MAXLIST));
		            		if(abox.isClosed()) break;
		            }
                }
                else
                	applyMaxRule(i);
                t.stop();
                if(abox.isClosed()) break;
                                
                if( fullDatatypeReasoning ) {
                    t = timers.startTimer("check-dt-count");
                    if(PelletOptions.USE_COMPLETION_QUEUE){
	                    abox.completionQueue.init(CompletionQueue.DATATYPELIST);
		    	            while(abox.completionQueue.hasNext(CompletionQueue.DATATYPELIST)){	               	                 
		    	            		checkDatatypeCount((QueueElement)abox.completionQueue.getNext(CompletionQueue.DATATYPELIST));
		    	            		abox.completionQueue.init(CompletionQueue.DATATYPELIST);
		    			    		if(abox.isClosed()) break;
		    	            }
                    }
                    else
                    	checkDatatypeCount(i);
                    t.stop();
                    if(abox.isClosed()) break;
    
                    t = timers.startTimer("rule-lit");
                    if(PelletOptions.USE_COMPLETION_QUEUE){
	                    abox.completionQueue.init(CompletionQueue.LITERALLIST);
		    	            while(abox.completionQueue.hasNext(CompletionQueue.LITERALLIST)){	               	                 
		    			    		applyLiteralRule((QueueElement)abox.completionQueue.getNext(CompletionQueue.LITERALLIST));
		    	            		if(abox.isClosed()) break;
		    	            }
                    }
                    else
                    	applyLiteralRule();
                    t.stop();
                    if(abox.isClosed()) break;
                }
                
                t = timers.startTimer("rule-unfold");
                if(PelletOptions.USE_COMPLETION_QUEUE){
	                abox.completionQueue.init(CompletionQueue.ATOMLIST);
		            while(abox.completionQueue.hasNext(CompletionQueue.ATOMLIST)){	
		            		applyUnfoldingRule((QueueElement)abox.completionQueue.getNext(CompletionQueue.ATOMLIST));
		            		if(abox.isClosed()) break;	  
		            }
                }
                else
                	applyUnfoldingRule(i);
                t.stop();
                if(abox.isClosed()) break;

                t = timers.startTimer("rule-disj");
                if(PelletOptions.USE_COMPLETION_QUEUE){
	                abox.completionQueue.init(CompletionQueue.ORLIST);
		            while(abox.completionQueue.hasNext(CompletionQueue.ORLIST)){	               
		            		applyDisjunctionRule((QueueElement)abox.completionQueue.getNext(CompletionQueue.ORLIST));	            	
		            		if(abox.isClosed()) break;	              		
		            }
                }
                else
                	applyDisjunctionRule(i);
                t.stop();
                if(abox.isClosed()) break;
                
                t = timers.startTimer("rule-some"); 
                if(PelletOptions.USE_COMPLETION_QUEUE){
                	//necessary to reset the end pointer for the queue	                
	                abox.completionQueue.init(CompletionQueue.SOMELIST);
		            while(abox.completionQueue.hasNext(CompletionQueue.SOMELIST)){	                                  		
		            		applySomeValuesRule((QueueElement)abox.completionQueue.getNext(CompletionQueue.SOMELIST));
		            		if(abox.isClosed()) break;	              		
		            }      
                }
                else
                	applySomeValuesRule(i);
                t.stop();
                if(abox.isClosed()) break;

                t = timers.startTimer("rule-min");  
                if(PelletOptions.USE_COMPLETION_QUEUE){
	                abox.completionQueue.init(CompletionQueue.MINLIST);
		            while(abox.completionQueue.hasNext(CompletionQueue.MINLIST)){	                            		
		            		applyMinRule((QueueElement)abox.completionQueue.getNext(CompletionQueue.MINLIST));
		            		if(abox.isClosed()) break;	              		
		            }
                }
                else
                	applyMinRule(i);
                t.stop();
                if(abox.isClosed()) break;
                                
//                t = timers.startTimer("rule-max");
//                applyMaxRule(i);
//                t.stop();
//                if(abox.isClosed()) break;
//                
//                t = timers.startTimer("rule-lit");
//                applyLiteralRule();
//                t.stop();
//                if(abox.isClosed()) break;
            }
            
//            if( !abox.isClosed() ) {
//            	abox.printTree();
//            	int branchCount = abox.getBranches().size();
//            	if( branchCount > 0 ) {
//	            	Branch lastBranch = (Branch) abox.getBranches().get( branchCount - 1 );
//	            	DependencySet ds = new DependencySet( lastBranch.branch );
//	            	Clash clash = Clash.atomic( lastBranch.node, ds );
//	            	abox.setClash( clash );
//            	}
//            }
            	
            if( abox.isClosed() ) {
                if( log.isDebugEnabled() )
                    log.debug( "Clash at Branch (" + abox.getBranch() + ") " + abox.getClash() );

                if(backtrack())
                    abox.setClash( null );
                else
                    abox.setComplete( true );
            }
            else {
            	if (PelletOptions.SATURATE_TABLEAU) {
            		Branch unexploredBranch = null;
                	for (int i=abox.getBranches().size()-1; i>=0; i--) {
                		unexploredBranch = (Branch) abox.getBranches().get(i);
                        unexploredBranch.tryNext++;
                		if (unexploredBranch.tryNext < unexploredBranch.tryCount) {
                			restore(unexploredBranch);
                            System.out.println("restoring branch "+ unexploredBranch.branch + " tryNext = "+unexploredBranch.tryNext + " tryCount = "+unexploredBranch.tryCount);
                			unexploredBranch.tryNext();
                			break;
                		}
                		else { 
                            System.out.println("removing branch "+ unexploredBranch.branch);
                            abox.getBranches().remove(i);
                            unexploredBranch = null;
                        }
                	}
                	if(unexploredBranch == null) {
                		abox.setComplete( true );	
                	}
            	}
                else 
                	abox.setComplete( true );
            }
        }
        
        completionTimer.stop();

        return abox;
    }
//
//	public void restore(Branch br) {
////	    Timers timers = abox.getKB().timers;
////		Timer timer = timers.startTimer("restore");
//		
//		abox.setBranch(br.branch);
//		abox.setClash(null);
//		abox.anonCount = br.anonCount;
//		
//		mergeList.clear();
//		
//		List nodeList = abox.getNodeNames();
//		Map nodes = abox.getNodeMap();
//		
//		if(ABox.DEBUG) System.out.println("RESTORE: Branch " + br.branch);
//		if(ABox.DEBUG && br.nodeCount < nodeList.size())
//		    System.out.println("Remove nodes " + nodeList.subList(br.nodeCount, nodeList.size()));
//		for(int i = 0; i < nodeList.size(); i++) {
//			ATerm x = (ATerm) nodeList.get(i);
//			
//			Node node = abox.getNode(x);
//			if(i >= br.nodeCount) 
//				nodes.remove(x);
////			if(node.branch > br.branch) {
////				if(ABox.DEBUG) System.out.println("Remove node " + x);	
////				nodes.remove(x);
////				int lastIndex = nodeList.size() - 1;
////				nodeList.set(i, nodeList.get(lastIndex));
////				nodeList.remove(lastIndex);
////				i--;
////			}
//			else
//				node.restore(br.branch);
//		}		
//		nodeList.subList(br.nodeCount, nodeList.size()).clear();
//
//		for(Iterator i = abox.getIndIterator(); i.hasNext(); ) {
//			Individual ind = (Individual) i.next();
////			applyConjunctions(ind);			
//			applyAllValues(ind);
////			applyNominalRule(ind);
//		}
//		
//		if(ABox.DEBUG) abox.printTree();
//		
//		if(!abox.isClosed()) abox.validate();
//			
////		timer.stop();
//	}

}
