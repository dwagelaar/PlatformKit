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

import aterm.ATermAppl;

/**
 * Dependency structure for when a branch is added.
 * 
 * @author Christian Halaschek-Wiener
 *
 */
public class BranchAddDependency extends BranchDependency{

	/**
	 * The actual branch
	 */
	private Branch branch;
	
	
	
	/**
	 * Constructor
	 * @param index
	 * @param branch
	 */
	public BranchAddDependency(ATermAppl assertion, int index, Branch branch){
		super(assertion);
		this.branch = branch;
	}



	/**
	 * Get branch
	 * @return
	 */
	public Branch getBranch() {
		return branch;
	}
	
	
	
	/**
	 * ToString method
	 */
	public String toString(){
		return "Branch  - [" + branch + "]";
	}
	
	
	
	/**
	 * Equals method
	 */
	public boolean equals(Object other){
		if(other instanceof BranchAddDependency){
			return (this.branch.branch == ((BranchAddDependency)other).branch.branch) && this.assertion.equals(((BranchAddDependency)other).assertion) ;	
		}else
			return false;
	}
	
	
	/**
	 * Hashcode method
	 */
	public int hashCode(){ 
		return this.branch.branch + this.assertion.hashCode(); 
	}
	
}
