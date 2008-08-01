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
 * A dependency for a node merge
 * 
 * @author Christian Halaschek-Wiener
 *
 */
public class MergeDependency implements Dependency{

	/**
	 * The individual that ind is merged to
	 */
	private ATermAppl mergedIntoInd;
	
	/**
	 * The individual that is merged into mergedIntoInd
	 */
	private ATermAppl ind;
	
	
	/**
	 * Constructor
	 * @param ind
	 * @param mergedIntoInd
	 */
	public MergeDependency(ATermAppl ind, ATermAppl mergedIntoInd){
		this.mergedIntoInd = mergedIntoInd;
		this.ind = ind;
	}


	/**
	 * Get the individual that is merged into the other 
	 * 
	 * @return
	 */
	public ATermAppl getInd() {
		return ind;
	}


	/**
	 * Get the individual that has ind merged into it
	 * 
	 * @return
	 */
	public ATermAppl getmergedIntoInd() {
		return mergedIntoInd;
	}
	
	
	/**
	 * ToString method
	 */
	public String toString(){
		return "Merge [" + ind + "]  into [" + mergedIntoInd + "]";
	}
	

	/**
	 * Equals method
	 */
	public boolean equals(Object other){
		if(other instanceof MergeDependency){
			return this.ind.equals(((MergeDependency)other).ind) && this.mergedIntoInd.equals(((MergeDependency)other).mergedIntoInd);	
		}else
			return false;
	}
	
	
	/**
	 * Hashcode method
	 * TODO: this may not be sufficient
	 */
	public int hashCode(){ 
		return this.ind.hashCode() + this.mergedIntoInd.hashCode(); 
	}
	

}
