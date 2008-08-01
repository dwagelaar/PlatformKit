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
 * A type dependency.
 * 
 * @author Christian Halaschek-Wiener
 *
 */
public class TypeDependency implements Dependency{

	/**
	 * The type
	 */
	private ATermAppl type;
	
	/**
	 * The individual
	 */
	private ATermAppl ind;
	
	
	/**
	 * Constructor
	 * 
	 * @param ind
	 * @param type
	 */
	public TypeDependency(ATermAppl ind, ATermAppl type){
		this.type = type;
		this.ind = ind;
	}


	/**
	 * Get the individual
	 * 
	 * @return
	 */
	public ATermAppl getInd() {
		return ind;
	}


	/**
	 * Get the type
	 * 
	 * @return
	 */
	public ATermAppl getType() {
		return type;
	}
	
	
	
	/**
	 * ToString method
	 */
	public String toString(){
		return "Type [" + ind + "]  - [" + type + "]";
	}
	
	
	
	/**
	 * Equals method
	 */
	public boolean equals(Object other){
		if(other instanceof TypeDependency){
			return this.ind.equals(((TypeDependency)other).ind) && this.type.equals(((TypeDependency)other).type);	
		}else
			return false;
	}
	
	
	/**
	 * Hashcode method
	 */
	public int hashCode(){ 
		return this.ind.hashCode() + this.type.hashCode(); 
	}

}
