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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import aterm.ATermAppl;


/**
 * Structure for containing all dependencies for a given assertion. This is the object stored
 * in the dependency index 
 * 
 * @author Christian Halaschek-Wiener
 *
 */
public class DependencyEntry {

	/**
	 * The set of node lables which are dependent
	 */
	private Set<Dependency> types;

	
	/**
	 * The set of merges which are dependent
	 */
	private Set<Dependency> merges;
	
	/**
	 * The set of edge which are dependent
	 */
	private Set<Edge> edges;


	/**
	 * The set of branches which are dependent
	 */
	private Set<Dependency> branchAdds;
	
	
	/**
	 * The set of branch remove ds' which are dependent
	 */
	private Set<Dependency> branchCloses;
	
	
	/**
	 * Clash dependency
	 */
	private ClashDependency clash;

	
	/**
	 * Default constructor
	 *
	 */
	public DependencyEntry(){
		types = new HashSet<Dependency>();
		edges = new HashSet<Edge>();
		merges = new HashSet<Dependency>();
		branchAdds = new HashSet<Dependency>();
		branchCloses = new HashSet<Dependency>();
		clash = null;
	}
	
	
	
	/**
	 * 
	 * @return
	 */
	public DependencyEntry copy(){
		DependencyEntry newEntry = new DependencyEntry();
		
		
		//TODO:may need to perform a deep copy here
		newEntry.types = new HashSet<Dependency>(this.types);

		//TODO:may need to perform a deep copy here
		newEntry.merges = new HashSet<Dependency>(this.merges);
		
		
		//copy edge depenedencies
		for(Iterator it = edges.iterator(); it.hasNext();){
			Edge next = (Edge)it.next();
			
			//create new edge
			Edge newEdge = new Edge(next.getRole(), next.getFrom(), next.getTo(), next.getDepends().copy()); 
			
			//add to edge list
			newEntry.edges.add(newEdge);
		}
		
		//TODO:may need to perform a deep copy here
		newEntry.branchAdds = new HashSet<Dependency>(this.branchAdds);

		//TODO:may need to perform a deep copy here
		newEntry.branchCloses = new HashSet<Dependency>(this.branchCloses);
		
		
		//TODO:may need to perform a deep copy here
		newEntry.clash = this.clash;

		
		return newEntry;
	}
	
	
	/**
	 * Add a type dependency
	 * 
	 * @param ind
	 * @param type
	 */
	protected void addTypeDependency(ATermAppl ind, ATermAppl type){
		types.add(new TypeDependency(ind, type));
	}
	
	/**
	 * Add a edge dependency
	 * 
	 * @param edge
	 */
	protected void addEdgeDependency(Edge edge){
		edges.add(edge);
	}
	
	
	/**
	 * Add a edge dependency
	 * 
	 * @param ind
	 * @param mergedTo
	 */
	protected void addMergeDependency(ATermAppl ind, ATermAppl mergedTo){
		merges.add(new MergeDependency(ind, mergedTo));
	}
	
	/**
	 * Add a branch add dependency
	 * 
	 * @param branchId
	 * @param branch
	 */
	protected BranchDependency addBranchAddDependency(ATermAppl assertion, int branchId, Branch branch){
		BranchDependency b = new BranchAddDependency(assertion, branchId, branch); 
		
		branchAdds.add( b );
		return b;
	}
	
	
	/**
	 * Add a branch remove ds dependency
	 * 
	 * @param branchId
	 * @param branch
	 */
	protected BranchDependency addCloseBranchDependency(ATermAppl assertion, Branch theBranch){
		BranchDependency b = new CloseBranchDependency(assertion, theBranch.tryNext, theBranch); 
		
		if(branchCloses.contains(b))
			branchCloses.remove(b);
		
		branchCloses.add( b );
		return b;
	}

	
	
	/**
	 * Helper method to print all dependencies
	 * TODO: this print is not complete
	 */
	public void print(){
		System.out.println("  Edge Dependencies:");
		for(Iterator it = edges.iterator(); it.hasNext();){
			System.out.println("    " + ((Edge)it.next()).toString());
		}
		
		System.out.println("  Type Dependencies:");
		for(Iterator it = types.iterator(); it.hasNext();){
			System.out.println("    " + ((TypeDependency)it.next()).toString());
		}
		
		
		System.out.println("  Merge Dependencies:");
		for(Iterator it = merges.iterator(); it.hasNext();){
			System.out.println("    " + ((MergeDependency)it.next()).toString());
		}
	}


	/**
	 * Get edges
	 * @return
	 */
	public Set<Edge> getEdges() {
		return edges;
	}


	/**
	 * Get merges
	 * @return
	 */
	public Set<Dependency> getMerges() {
		return merges;
	}


	/**
	 * Get types
	 * @return
	 */
	public Set<Dependency> getTypes() {
		return types;
	}


	/**
	 * Get branches
	 * @return
	 */
	public Set<Dependency> getBranchAdds() {
		return branchAdds;
	}



	/**
	 * Get the close branches for this entry
	 * 
	 * @return
	 */
	public Set<Dependency> getCloseBranches() {
		return branchCloses;
	}


	/**
	 * Get clash dependency
	 * @return
	 */
	protected ClashDependency getClash() {
		return clash;
	}



	/**
	 * Set clash dependency
	 * @param clash
	 */
	protected void setClash(ClashDependency clash) {
		this.clash = clash;
	}

	
	
	
}
