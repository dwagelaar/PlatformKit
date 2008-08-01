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

package org.mindswap.pellet.query.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryPattern;
import org.mindswap.pellet.query.QueryResultBinding;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

public class LiteralIterator implements Iterator {
	QueryResultBinding binding;
	Set litVars;
	ArrayList [] lb;
	int[] indices;
	boolean more = true;

	public LiteralIterator( Query q, QueryResultBinding binding)  {
	    KnowledgeBase kb = q.getKB(); 
		this.binding = binding;
		this.litVars = q.getDistLitVars();
			
		lb = new ArrayList[litVars.size()];
		indices = new int[ litVars.size() ];
		int index = 0;
		for(Iterator i = litVars.iterator(); i.hasNext();) {
            ATermAppl litVar = (ATermAppl) i.next();
			Datatype dtype = q.getDatatype( litVar );
			QueryPattern pattern =  (QueryPattern) q.findPatterns( null, null, litVar ).get( 0 );
			ATermAppl name = pattern.getSubject();
			
			if( ATermUtils.isVar( name ))
			    name = binding.getValue( name );			
			
			lb[ index++ ] = new ArrayList();
				
			List act = kb.getDataPropertyValues( pattern.getPredicate(), name, dtype );		
			if ( act.size() > 0 ) {
				for ( Iterator a = act.iterator(); a.hasNext(); ) {
					ATermAppl lit = (ATermAppl) a.next();
					lb[ index ].add( lit );
				}
			} else {
				more = false;
			}
		}
	}

	public void remove() {
	}
    
	public boolean hasNext() {
		return more;
	}

	public Object next() {
		if ( !more )
			return null;
		
		QueryResultBinding next = (QueryResultBinding) binding.clone();
		
		int index = 0;
		for(Iterator i = litVars.iterator(); i.hasNext(); index++) {
            ATermAppl o1 = (ATermAppl) i.next();
			ATermAppl o2 = (ATermAppl) lb[index].get( indices[index] );
			next.setValue( o1, o2 );
		}	
		
		more = incIndex( 0 );
		
		return next;
	}
		
	private boolean incIndex( int index ) {
		
		if ( indices[index] + 1 < lb[index].size() ) {
			indices[index]++;
		} else {
			if ( index == indices.length - 1 ) {
				return false;
			} else {
				indices[index] = 0;
				return incIndex( index + 1 );
			}
		}
		
		return true;
	}
}
