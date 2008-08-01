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
 * A clash dependency.
 * 
 * @author Christian Halaschek-Wiener
 *
 */
public class ClashDependency implements Dependency{

	/**
	 * The assertion
	 */
	private ATermAppl assertion;
	
	
	/**
	 * The clash
	 */
	private Clash clash;
	
	/**
	 * Constructor
	 * @param assertion
	 * @param clash
	 */
	public ClashDependency(ATermAppl assertion, Clash clash){
		this.assertion = assertion;
		this.clash = clash;
	}


	
	
	/**
	 * ToString method
	 */
	public String toString(){
		return "Clash [" + assertion + "]  - [" + clash + "]";
	}
	
	
	
	/**
	 * Equals method
	 */
	public boolean equals(Object other){
		if(other instanceof ClashDependency){
			return this.assertion.equals(((ClashDependency)other).assertion) && this.clash.node.equals(((ClashDependency)other).clash.node) && this.clash.type == ((ClashDependency)other).clash.type && this.clash.depends.equals(((ClashDependency)other).clash.depends);	
		}else
			return false;
	}
	
	
	/**
	 * Hashcode method
	 * TODO: this may not be sufficient
	 */
	public int hashCode(){ 
		return this.clash.type+ this.clash.depends.hashCode() + this.clash.node.hashCode() + this.assertion.hashCode(); 
	}




	/**
	 * Get the assertion
	 *  
	 * @return
	 */
	protected ATermAppl getAssertion() {
		return assertion;
	}




	/**
	 * Get the clash
	 * 
	 * @return
	 */
	protected Clash getClash() {
		return clash;
	}

}
