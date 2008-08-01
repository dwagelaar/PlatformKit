// The MIT License
//
// Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
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

package org.mindswap.pellet.tbox;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.mindswap.pellet.utils.Pair;

import aterm.ATermAppl;

public interface TBox {
	/**
	 * Add a named class declaration
	 * 
	 * @return <code>true</code> if TBox changed as a result of this call
	 */
	public boolean addClass( ATermAppl term );

	/**
	 * Return all the named classes 
	 */
	public Set<ATermAppl> getClasses();
	
	/**
	 * Return all the named classes plus TOP and BOTTOM
	 */
	public Set<ATermAppl> getAllClasses();

	/**
	 * Return all the axioms defined in this TBox
	 */
	public Collection<ATermAppl> getAxioms();
	
	/**
	 * Return all the sub and equivalent class axioms that have 
	 * the given concept on the left hand side
	 */
	public Collection<ATermAppl> getAxioms( ATermAppl concept );

	/**
	 * @deprecated Not used anymore	 
	 */
	public void split();

	/**
	 * Absorb GCIs into primitive definitions
	 */
	public void absorb();
	
	/**
	 * Normalize the primitive definitions so they can be directly used
	 * in unfolding
	 */
	public void normalize();
	
	/**
	 * Turn the non-absorbed GCIs into disjunctions that will be used
	 * in the universal concept
	 */
	public void internalize();

	/**
	 * Return the universal concept
	 */
	public List<Pair<ATermAppl,Set<ATermAppl>>> getUC();

	/**
	 * Lazy unfold the given concept
	 *  
	 * @param c
	 * @return
	 */
	public List<Pair<ATermAppl,Set<ATermAppl>>> unfold( ATermAppl c );	

	/**
	 * Add a new sub or equivalent class axiom with the given explanation.
	 * The explanation is the syntactic axiom(s) used in the ontology. For
	 * example, a disjoint axiom is turned into a subclass axiom so the
	 * explanation for sub(a,not(b)) will be disjoint(a,b). Explanation
	 * should be a minimal set of axioms for the overall explanation to 
	 * come out correct.
	 * 
	 * @param axiom
	 * @param explain
	 * @return
	 */
	public boolean addAxiom(ATermAppl axiom, Set<ATermAppl> explain);
	
	/**
	 * Add a new explanation for the given axiom. If a previous explanation
	 * exists this will be stored as another explanation.
	 * 
	 * @param axiom
	 * @param explain
	 * @return
	 */
	public boolean addAxiomExplanation(ATermAppl axiom, Set<ATermAppl> explain);
	
	/**
	 * Remove the axiom from TBox and all other axioms that depend on it. An axiom
	 * depends on another axiom if it is a syntactic transformation (as in disjoint
	 * axiom is transformed into subclass) or it is obtained via absorption (as 
	 * equivalent class axioms are absorbed into subclass axioms)
	 * @param axiom
	 * @return
	 */
	public boolean removeAxiom(ATermAppl axiom);
	
	/**
	 * Return a single explanation for the given TBox axiom.
	 * 
	 * @param axiom
	 * @return
	 */
	public Set<ATermAppl> getAxiomExplanation(ATermAppl axiom);
	
	/**
	 * Return multiple explanations for the given TBox axiom.
	 * 
	 * @param axiom
	 * @return
	 */
	public Set<Set<ATermAppl>> getAxiomExplanations(ATermAppl axiom);	

}
