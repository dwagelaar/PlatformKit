// The MIT License
//
// Copyright (c) 2007 Christian Halaschek-Wiener
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import aterm.ATermAppl;


/**
 * 
 * This is the index structure for maintaining the dependencies between structures in an ABox and the syntactic asseertions which caused them to be created. This is used
 * for incremental deletions.
 * 
 * @author Christian Halaschek-Wiener
 */
public class DependencyIndex {
	public final static Log log = LogFactory.getLog( DependencyIndex.class );
	
	
	/**
	 * Map from assertions (ATermAppl) to Dependency entries
	 */
	private Map<ATermAppl, DependencyEntry> dependencies;
	
	
	/**
	 * Branch depedency index
	 */
	private Map<Branch, Set<BranchDependency>> branchIndex;
	
	
	/**
	 * Clash depedency - used for cleanup
	 */
	private Set<ClashDependency> clashIndex;
	
	
	/**
	 * KB object
	 */
	private KnowledgeBase kb;
	
	
	/**
	 * Default consutructor
	 *
	 */
	public DependencyIndex(KnowledgeBase kb){
		dependencies = new HashMap<ATermAppl, DependencyEntry>();
		branchIndex = new HashMap<Branch, Set<BranchDependency>>();
		clashIndex = new HashSet<ClashDependency>();
		this.kb = kb; 
	}
	
	
	/**
	 * Copy constructor
	 *
	 */
	public DependencyIndex(KnowledgeBase kb, DependencyIndex oldIndex){
		this.kb = kb;
		
		dependencies = new HashMap<ATermAppl, DependencyEntry>();
		
		//iterate over old dependencies and copy
		for(Iterator it = oldIndex.getDependencies().keySet().iterator(); it.hasNext();){
			//get assertion
			ATermAppl next = (ATermAppl)it.next();
			
			//duplication entry
			DependencyEntry entry = oldIndex.getDependencies(next).copy();
			
			//add
			dependencies.put(next, entry);
		}
		
	}
	
	
	/**
	 * 
	 * @param assertion
	 * @return
	 */
	protected DependencyEntry getDependencies(ATermAppl assertion){
		return dependencies.get(assertion);
	}
	
	
	
	/**
	 * 
	 * @return
	 */
	protected Map<ATermAppl, DependencyEntry> getDependencies(){
		return dependencies;
	}
	
	
	/**
	 * Add a new type dependency
	 * @param ind
	 * @param type
	 * @param ds
	 */
	protected void addTypeDependency(ATermAppl ind, ATermAppl type, DependencySet ds){
//		if(log.isDebugEnabled())
//			log.debug("DependencyIndex- Calling add type dependency");
		
		//loop over ds
		for(Iterator it = ds.explain.iterator(); it.hasNext(); ){
			ATermAppl nextAtom = (ATermAppl)it.next();
			
			//check if this assertion exists
			if(kb.getSyntacticAssertions().contains(nextAtom)){
				//if this entry does not exist then create it
				if(!dependencies.containsKey(nextAtom))
					dependencies.put(nextAtom, new DependencyEntry());
					
//					if(log.isDebugEnabled())
//						log.debug("DependencyIndex- Adding type dependency: Axiom [" +nextAtom + "]   ,  Ind [" + ind + "]   ,  Type["  + type + "]");
				
				//add the dependency
				dependencies.get(nextAtom).addTypeDependency(ind, type);
			}
		}
	}
	
	
	
	
	/**
	 * Add a new merge dependency
	 * @param ind
	 * @param type
	 * @param ds
	 */
	protected void addMergeDependency(ATermAppl ind, ATermAppl mergedTo, DependencySet ds){
//		if(log.isDebugEnabled())
//			log.debug("DependencyIndex- Calling add merge dependency");

		//loop over ds
		for(Iterator it = ds.explain.iterator(); it.hasNext(); ){
			//get explain atom
			ATermAppl nextAtom = (ATermAppl)it.next();
			
			//check if this assertion exists
			if(kb.getSyntacticAssertions().contains(nextAtom)){
				//if this entry does not exist then create it
				if(!dependencies.containsKey(nextAtom))
					dependencies.put(nextAtom, new DependencyEntry());
					
//					if(log.isDebugEnabled())
//						log.debug("DependencyIndex- Adding merge dependency: Axiom [" +nextAtom + "]   ,  Ind [" + ind + "]   ,  mergedToInd["  + mergedTo + "]");
				
				//add the dependency
				dependencies.get(nextAtom).addMergeDependency(ind, mergedTo);
			}			
		}
	}
	
	
	
	/**
	 * Add a new edge dependency
	 * @param edge
	 * @param ds
	 */
	protected void addEdgeDependency(Edge edge, DependencySet ds){
//		if(log.isDebugEnabled())
//			log.debug("DependencyIndex- Calling add edge dependency");

		//loop over ds
		for(Iterator it = ds.explain.iterator(); it.hasNext(); ){
			//get explain atom
			ATermAppl nextAtom = (ATermAppl)it.next();
			
			//check if this assertion exists
			if(kb.getSyntacticAssertions().contains(nextAtom)){
				//if this entry does not exist then create it
				if(!dependencies.containsKey(nextAtom))
					dependencies.put(nextAtom, new DependencyEntry());
					
//					if(log.isDebugEnabled())
//						log.debug("  DependencyIndex- Adding edge dependency: Axiom [" +nextAtom + "]   ,  Edge [" + edge + "]");

				//add the dependency
				dependencies.get(nextAtom).addEdgeDependency(edge);
			}			
		}
	}
	
	
	
	/**
	 * Add a new branch dependency
	 * @param ind
	 * @param type
	 * @param ds
	 */
	protected void addBranchAddDependency(Branch branch){
		//loop over ds
		for(Iterator it = branch.termDepends.explain.iterator(); it.hasNext(); ){
			ATermAppl nextAtom = (ATermAppl)it.next();
			
			//check if this assertion exists
			if(kb.getSyntacticAssertions().contains(nextAtom)){
				//if this entry does not exist then create it
				if(!dependencies.containsKey(nextAtom))
					dependencies.put(nextAtom, new DependencyEntry());
					
				if(log.isDebugEnabled())
					log.debug("DependencyIndex- Adding branch add dependency for assertion: " + nextAtom + " -  Branch id [" +branch.branch + "]   ,  Branch [" + branch + "]");
				
				
				//add the dependency
				BranchDependency newDep = dependencies.get(nextAtom).addBranchAddDependency(nextAtom, branch.branch, branch);
				
				//add depedency to index so that backjumping can be supported (ie, we need a fast way to remove the branch dependencies
				if(!branchIndex.containsKey(branch)){
					Set<BranchDependency> newS = new HashSet<BranchDependency>();
					newS.add(newDep);
					branchIndex.put(branch, newS);
				}else{
					branchIndex.get(branch).add(newDep);
				}
			}			
		}
	}
	
	
	
	/**
	 * Add a new branch ds removal dependency
	 * @param ind
	 * @param type
	 * @param ds
	 */
	protected void addCloseBranchDependency(Branch branch, DependencySet ds){
		//loop over ds
		for(Iterator it = ds.explain.iterator(); it.hasNext(); ){
			ATermAppl nextAtom = (ATermAppl)it.next();
			
			//check if this assertion exists
			if(kb.getSyntacticAssertions().contains(nextAtom)){
				//if this entry does not exist then create it
				if(!dependencies.containsKey(nextAtom))
					dependencies.put(nextAtom, new DependencyEntry());
				
				ATermAppl label = null;
				if(branch instanceof DisjunctionBranch)
					label = ((DisjunctionBranch)branch).disj[((DisjunctionBranch)branch).tryNext]; 
				
				if(log.isDebugEnabled())
					log.debug("DependencyIndex- Adding branch remove ds dependency for assertion: " + nextAtom + " -  Branch id [" +branch.branch + "]   ,  Branch [" + branch + "]   on label [" + label + "]  ,    tryNext [" + branch.tryNext +"]");

				//add the dependency
				BranchDependency newDep = dependencies.get(nextAtom).addCloseBranchDependency(nextAtom, branch);
				
				//add depedency to index so that backjumping can be supported (ie, we need a fast way to remove the branch dependencies
				if(!branchIndex.containsKey(branch)){
					Set<BranchDependency> newS = new HashSet<BranchDependency>();
					newS.add(newDep);
					branchIndex.put(branch, newS);
				}else{
					branchIndex.get(branch).add(newDep);
				}
			}
		}
	}
	
	
	/**
	 * Remove the dependencies for a given assertion
	 * @param assertion
	 */
	protected void removeDependencies(ATermAppl assertion){
		dependencies.remove(assertion);
	}
	
	
	/**
	 * Remove branch dependencies - this is needed due to backjumping!
	 * @param b
	 */
	protected void removeBranchDependencies(Branch b){
		Set<BranchDependency>deps = branchIndex.get(b);
		
		//TODO: why is this null? is this because of duplicate entries in the index set?
		//This seems to creep up in WebOntTest-I5.8-Manifest004 and 5 among others...
		if(deps == null)
			return;
		
		//loop over depencies and remove them
		for(Iterator it = deps.iterator(); it.hasNext();){
			BranchDependency next = (BranchDependency)it.next();
			if(log.isDebugEnabled())
				log.debug("DependencyIndex: RESTORE causing remove of branch index for assertion: " + next.getAssertion() + " branch dep.: " +next);
			if(next instanceof BranchAddDependency){
				//remove the dependency
				((DependencyEntry)dependencies.get(next.getAssertion())).getBranchAdds().remove(next);
			}else{
				//remove the dependency
				//((DependencyEntry)dependencies.get(next.getAssertion())).getBranchRemoveDSs().remove(next);
			}
			
		}		
	}
	
	
	
	
	
	
	/**
	 * Set clash dependencies
	 */
	protected void setClashDependencies(Clash clash){

		//first remove old entry using clashindex
		for(Iterator it = clashIndex.iterator(); it.hasNext();){
			ClashDependency next = (ClashDependency)it.next();			
			//remove the dependency
			if(((DependencyEntry)dependencies.get(next.getAssertion())) != null)
				((DependencyEntry)dependencies.get(next.getAssertion())).setClash(null);			
		}
		
		//clear the old index
		clashIndex.clear();
		
		if(clash==null)
			return;
		
		//loop over ds
		for(Iterator it = clash.depends.explain.iterator(); it.hasNext(); ){
			//get explain atom
			ATermAppl nextAtom = (ATermAppl)it.next();
			
			//check if this assertion exists
			if(kb.getSyntacticAssertions().contains(nextAtom)){
				//if this entry does not exist then create it
				if(!dependencies.containsKey(nextAtom))
					dependencies.put(nextAtom, new DependencyEntry());
					
				if(log.isDebugEnabled())
					log.debug("  DependencyIndex- Adding clash dependency: Axiom [" +nextAtom + "]   ,  Clash [" + clash + "]");

				ClashDependency newDep = new ClashDependency(nextAtom, clash);

				//set the dependency
				dependencies.get(nextAtom).setClash(newDep);
				
				//update index
				clashIndex.add(newDep);
			}		
		}
	}
}
