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

import aterm.ATermAppl;


/**
 * Structured stored on the completion queue
 * 
 * @author Christian Halaschek-Wiener
 *
 */
public class QueueElement {

	/**
	 * Label for this element 
	 */
	private ATermAppl label;
	
	/**
	 * Node for this element 
	 */
	private ATermAppl node;
	
	
	/**
	 * Constructor 
	 * @param ATermAppl The node
	 * @param ATermAppl The label
	 */
	public QueueElement(ATermAppl n, ATermAppl l){
		node = n;
		
		//This will be set to null only if its called from ABox.createLiteral or Node.setChanged
		//In these cases, the element will be added to the LITERALLIST or DATATYPELIST respectively
		//In both cases it does not matter.
		label = l;
	}
	

	/**
	 * To string 
	 */
	public String toString(){
		return node.getName() + "[" + label + "]";
	}
	
	/**
	 * Set label
	 * 
	 * @param ATermAppl The label
	 */
	public void setLabel(ATermAppl l){
		label = l;
	}

	/**
	 * Set the node
	 * 
	 * @param ATermAppl The node
	 */
	public void setNode(ATermAppl n){
		node = n;
	}	
	
	/**
	 * Get the label
	 * 
	 * @return ATermAppl The label
	 */
	public ATermAppl getLabel(){
		return label;
	}

	/**
	 * Get the node
	 * 
	 * @return ATermAppl The node
	 */
	public ATermAppl getNode(){
		return node;
	}
}
