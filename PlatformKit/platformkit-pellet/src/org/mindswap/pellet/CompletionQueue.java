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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import aterm.ATermAppl;


/**
 * A queue for individuals that need to have completion rules applied
 * 
 * @author Christian Halaschek-Wiener
 */
public class CompletionQueue{
	
	/**
	 * Indexes for the various queues
	 */
	static protected int GUESSLIST = 0;
	static protected int NOMLIST = 1;
	static protected int MAXLIST = 2;
	static protected int DATATYPELIST = 3;
	static protected int ATOMLIST = 4;
	static protected int ORLIST = 5;
	static protected int SOMELIST = 6;
	static protected int MINLIST = 7;
	static protected int LITERALLIST = 8;
	static protected int ALLLIST = 9;
	static protected int CHOOSELIST = 10;
	static protected int SIZE = 11;
	
	
	/**
	 * This queue is the global queue which will be used for explicit types. During an incremental update, this is the queue which new elements will be pushed onto
	 */
	protected List[] gQueue;

	/**
	 * The queue - array - each entry is an arraylist for a particular rule type 
	 */
	protected List[] queue;
	
	/**
	 * List of current index pointer for each queue
	 */
	protected int[] current;
	
	/**
	 * List of current index pointer for each global queue
	 */
	protected int[] gCurrent;
	
	/**
	 * List of current index pointer for the stopping point at each queue 
	 */
	protected int[] cutOff;

	/**
	 * List of branch pointers for each queue 
	 */
	protected List branches;
	
	/**
	 * List of individuals effected in each branch - used for optimized restore 
	 */
	protected List branchEffects;
	
	/**
	 * Pointer to the abox 
	 */
	protected ABox abox;
	

	/**
	 * Constructor - create queue
	 * 
	 * @param abox
	 */
	protected CompletionQueue(ABox abox) {
		this.abox = abox;
		
		//Init 
		queue = new ArrayList[SIZE];
		gQueue = new ArrayList[SIZE];
		current = new int[SIZE];
		gCurrent = new int[SIZE];
		cutOff = new int[SIZE];
		branches = new ArrayList();
		branchEffects = new ArrayList();
		
		//each branch index will consist of the current pointer and branch pointer
		Object[] initbranches = new Object[SIZE];
		

		//init all queue structures
		for(int i = 0; i < SIZE; i++){
			//default entries for branch 0
			int[] initial = new int[4];
			//current pointer
			initial[0] = 0;
			//branch pointer;
			initial[1] = 0;
			//end pointer
			initial[2] = 0;
			//gCurrent pointer
			initial[3] = 0;

			queue[i] = new ArrayList();
			gQueue[i] = new ArrayList();
			current[i] = 0;
			gCurrent[i] = 0;
			cutOff[i] = 0;
			initbranches[i] = initial;
		}
		
		//add the initial branch pointers which point to the first element
		branches.add(0,initbranches);
	}		

	/**
	 * Find the next individual in a given queue 
	 * @param type
	 */
	protected void findNext(int type) {
		//var if we have found a next element
		boolean found = false;
		
		//first search the global queue - this is done due to incremental additions as well as additions to the abox in ABox.isConsistent()
		for(; gCurrent[type] < gQueue[type].size() && (gCurrent[type] + current[type]) < cutOff[type]; gCurrent[type]++) {
		    //Get next node from queue
			QueueElement next = null;
			
			//get element from global queue
			next = (QueueElement)gQueue[type].get( gCurrent[type] );
			
			Node node = (Node) abox.getNodeMap().get( next.getNode() ) ;
			
			if(node == null)
				continue;
			
			//run down actual node on the fly
			node = node.getSame();
			
			//TODO: This could most likely be removed and only done once when an element is popped off the queue
			if(type == LITERALLIST && node instanceof Literal && !node.isPruned()){
				found = true;
				break;
			}else if(node instanceof Individual && !node.isPruned()){
				found = true;
				break;
			}
			
		}
		
		//return if found an element on the global queue
		if(found)
			return;

		//similarly we must check the regular queue for any elements
		for(; current[type] < queue[type].size() && (gCurrent[type] + current[type]) < cutOff[type]; current[type]++) {
		    //Get next node from queue
			QueueElement next = null;
			
			//get element off of the normal queue
			next = (QueueElement)queue[type].get( current[type] );
			
			Node node = (Node) abox.getNodeMap().get( next.getNode() ) ;
			
			if(node == null)
				continue;
			
			//run down actual node on the fly
			node = node.getSame();
			
			//TODO: This could most likely be removed and only done once when an element is popped off the queue
			if(type == LITERALLIST && node instanceof Literal && !node.isPruned()){
					break;
			}else if(node instanceof Individual && !node.isPruned()){
					break;
			}
		}
	}
	

	
	/**
	 * Test if there is another element on the queue to process
	 * 
	 * @param type
	 * @return
	 */
	public boolean hasNext(int type) {
		//find next
		findNext(type);
		
		//check one exits
		if( ( (current[type] < queue[type].size()) || (gCurrent[type] < gQueue[type].size()))  && (gCurrent[type] + current[type]) < cutOff[type])
			return true;
		else
			return false;
	}

	
	
	/**
	 * Reset the queue to be the current nodes in the abox; Also reset the type index to 0
	 * @param branch
	 */
	public void restore(int branch) {
		//clear the extra branches
		if(branch+1 < branches.size())
			branches.subList(branch+1, branches.size()).clear();
		
		Object[] theBranch = (Object[])branches.get(branch);
		int branchP;
		
		//reset queues - currently do not reset guess list
		for(int i = 0; i < SIZE; i++){
			int[] index = (int[])theBranch[i];
			
			//set the current pointer
			current[i] = index[0];			
			//get the branch pointer - use it to clear the queues
			branchP = index[1];
			//get the end type pointer
			cutOff[i] = index[2];
			//get the global queue pointer
			gCurrent[i] = index[3];
			
			//do not clear gQueue as these are explicit assertions
			//CHW - this is old 
//			if(branchP < gQueue[i].size())
//				queue[i].subList( 0, queue[i].size()).clear();
//			else
//				queue[i].subList( (branchP - gQueue[i].size()), queue[i].size()).clear();

			//new approach
			queue[i].subList( Math.min(queue[i].size(), cutOff[i]), queue[i].size()).clear();		
		}		
	}
	
	
	
	/**
	 * Get the next element of a queue of a given type 
	 * @param type
	 * @return
	 */
	public Object getNext(int type) {
		//get the next index
		findNext(type);
		
		QueueElement elem = null;

		//get element from correct queue
		if(gCurrent[type] < gQueue[type].size())
			elem = (QueueElement)gQueue[type].get( gCurrent[type]++ );
		else{
			elem = (QueueElement)queue[type].get( current[type]++ );		
		}
		return elem;
	}
	
	

	/**
	 * Add an element to the queue 
	 * @param x
	 * @param type
	 */
	public void add(QueueElement x, int type){		
		if(abox.isSyntacticUpdate()){
			gQueue[type].add(x);
		}
		else
			queue[type].add(x);
	}
	
	
	/**
	 * Add a list to the queue 
	 * @param xs
	 * @param type
	 */
	public void addAll(List xs, int type){
		if(abox.isSyntacticUpdate())
			gQueue[type].addAll(xs);
		else
			queue[type].addAll(xs);
	}
	
	
	/**
	 * Reset the cutoff for a given type index
	 * @param type
	 */
	public void init(int type){
		cutOff[type] = gQueue[type].size() + queue[type].size();
	}
	
	
	/**
	 * Set branch pointers to current pointer. This is done whenever abox.incrementBranch is called 
	 * @param branch
	 */
	public void incrementBranch(int branch){
		Object[] theBranch;
		
		//if branch exists get it, else create new object
		if(branch < branches.size())
			theBranch = (Object[])branches.get(branch);
		else
			theBranch = new Object[SIZE];
			
		//set all branch pointers to the be the current pointer of each queue
		for(int i = 0; i < SIZE; i++){
			int[] entry = new int[4];
			
			entry[0] = current[i];
			entry[1] = gQueue[i].size() + queue[i].size();

			//old approach
			//entry[2] = cutOff[i];
			//new appraoch
			entry[2] = queue[i].size()+1;
			entry[3] = gCurrent[i];
			
			theBranch[i] = entry;
		}
		
		//add branch pointers back
		if( branch < branches.size()){
			branches.set(branch,theBranch);
		}
		else{
			branches.add(branch, theBranch);
		}
		
		
		
		//add new set to branch effects
//		if(branch > branchEffects.size())
//			branchEffects.add(new HashSet());
	}
	

	/**
	 * Copy the queue
	 * 
	 * @return
	 */
	public CompletionQueue copy(){
		CompletionQueue copy = new CompletionQueue(this.abox);
		
		//copy all the queues
		for(int i = 0; i < SIZE; i++){
			copy.queue[i] = new ArrayList(this.queue[i]);
			copy.gQueue[i] = new ArrayList(this.gQueue[i]);
			copy.current[i] = this.current[i];
			copy.cutOff[i] = this.cutOff[i];
			copy.gCurrent[i] = this.gCurrent[i];
		}
		
		//copy branch information
		for(int i = 0; i < branches.size(); i++){
			Object[] branchArray = (Object[])branches.get(i);
			Object[] newBranchArray = new Object[branchArray.length];
			
			int[] oldEntry;
			
			for(int j = 0; j < branchArray.length; j++){
				oldEntry = (int[])branchArray[j];

				int[] newEntry = new int[4];
				newEntry[0] = oldEntry[0];
				newEntry[1] = oldEntry[1];
				newEntry[2] = oldEntry[2];
				newEntry[3] = oldEntry[3];
				
				newBranchArray[j] = newEntry;
			}
			
			if(i<copy.branches.size())
				copy.branches.set(i,newBranchArray);
			else
				copy.branches.add(newBranchArray);
			
		}
		
		
		//copy branch effects
		for(int i = 0; i < branchEffects.size(); i++){
			HashSet cp = new HashSet();
			cp.addAll((Set)branchEffects.get(i));
			copy.branchEffects.add(cp);		
		}
		
		return copy;
	}
	
	
	/**
	 * Set the abox for the queue
	 * @param ab
	 */
	public void setABox(ABox ab){
    		this.abox = ab;
    }
	
	
	
	/**
	 * Print method for a given queue type
	 * 
	 * @param type
	 */
	public void print(int type){
		System.out.println("Queue for type: " + type);
	
		System.out.println("   Global Curr Pointer " + gCurrent[type] + "\n Global Queue:");		
		for(int i = 0; i < gQueue[type].size(); i++){
			System.out.println("     " + ((QueueElement)gQueue[type].get(i)).getNode() + "  " + ((QueueElement)gQueue[type].get(i)).getLabel());
		}			
		System.out.println("   Queue Curr Pointer " + current[type] + "\n Queue:");		
		for(int i = 0; i < queue[type].size(); i++){
			System.out.println("     " +  ((QueueElement)queue[type].get(i)).getNode() + "  " + ((QueueElement)queue[type].get(i)).getLabel());
		}		
	}
	
	
	
	/**
	 * Print method for entire queue
	 *
	 */
	public void print(){
		for(int i = 0;i < this.SIZE; i++){
			print(i);
		}
		
		printBranchInfo();
	}

	
	/**
	 * Print branch information
	 *
	 */
	public void printBranchInfo(){
		System.out.println("Branch pointers: ") ;
		for(int i = 1; i < branches.size(); i++){
			Object[] theBranch = (Object[])branches.get(i);
				

			System.out.println("Branch: " + i);
			//set all branch pointers to the be the current pointer of each queue
			for(int j = 0; j < SIZE; j++){
				int[] entry = (int[])theBranch[j];
				System.out.println("  Queue - " + j);
				System.out.println("    Current pointer: " + entry[0]);
				System.out.println("    Total size (gQ + queue i): " + entry[1]);
				System.out.println("    Cutoff: " + entry[2]);
				System.out.println("    gCurrent: " + entry[3]);
			}
		}
	}
	

	
	/**
	 * Used to track individuals affect during each branch - needed for backjumping. 
	 * 
	 * @param branch
	 * @param node
	 */
	protected void addEffected(int branch, ATermAppl node){
		if(branch <= 0)
			return;
		
		Set inds;			 
		//add branch pointers back
		if( branch < branchEffects.size() ){
			inds = (HashSet) branchEffects.get(branch);
			inds.add(node);
			branchEffects.set(branch, inds);
		}
		else{
//			for( int i = 0; i < branch; i++ )
			for( int i = branchEffects.size(); i < branch; i++ )
				branchEffects.add( new HashSet() );
			inds = new HashSet();
			inds.add(node);
			branchEffects.add(branch,inds);			
		}
	}

	
	
	/**
	 * Remove effected individuals for a given branch
	 * 
	 * @param branch
	 * @return
	 */
	protected Set removeEffects(int branch){
		Set effected = new HashSet();
		for(int i = (branch+1); i < branchEffects.size(); i++){
			Set next = (Set)branchEffects.get(i);
			effected.addAll(next);
			branchEffects.set(i, new HashSet());
		}
		return effected;
	}	
}