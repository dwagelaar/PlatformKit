//The MIT License
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

/*
 * Created on May 8, 2004
 */
package org.mindswap.pellet.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Utility functions for {#link java.util.Set Set}s.
 * 
 * @author Evren Sirin
 */
public class SetUtils {
    public final static Set EMPTY_SET = Collections.EMPTY_SET;
    
    /**
     * Adds the given object to the set but saves memory space
     * by allocating only the required amount for small sets. The 
     * idea is to use the specialized empty set and singleton set
     * implementations (which are immutable) for the sets of size 
     * 0 and 1. If the set is empty a new singleton set is created,
     * if set has one element we create a new set with two elements,
     * otherwise we simply add the element to the given set.This 
     * technique is most useful if the expected set size is 0 or 1. 
     *   
     * @param o 
     * @param set
     * @return
     */
    public static Set add( Object o, Set set ) {
        int size = set.size();
        if( size == 0 )
            set = singleton( o );
        else if( size == 1 ) {
            Object existing = set.iterator().next();
            if( !existing.equals( o ) )
                set = binary( existing, o );
        }
        else    
            set.add( o );
        
        return set;
    }

    public static Set remove( Object o, Set set ) {
        int size = set.size();
        if( size == 0 ) {
            // do nothing
        }            
        else if( size == 1  ) {
            if( set.contains( o ) )
                set = EMPTY_SET;
        }
        else    
            set.remove( o );
        
        return set;
    }
    
    public final static Set singleton(Object o) {
        return Collections.singleton( o );
    }

    public final static Set binary( Object o1, Object o2 ) {
        Set set = new HashSet();
        set.add( o1 );
        set.add( o2 );
        
        return set;
    }
    
	/**
	 * Returns the union of all the sets given in a collection. 
	 * 
	 * @param coll A Collection of sets
	 */
	public static <T> Set<T> union(Collection<? extends Collection<T>> coll) {
		Set<T> set = new HashSet<T>();
		
		for( Collection<T> innerColl : coll ) 
			set.addAll( innerColl );
		
		return set;
	}
	
	/**
	 * Returns the union of two collections 
	 * 
	 * @param coll A Collection of sets
	 */
	public static <T> Set<T> union( Collection<T> c1, Collection<T> c2 ) {
		Set<T> set = new HashSet<T>( c1 );
		set.addAll( c2 );
		
		return set;
	}	

	/**
	 * Returns the intersection of two collections 
	 * 
	 * @param coll A Collection of sets
	 */
	public static Set intersection( Collection c1, Collection c2 ) {
		Set set = new HashSet( c1 );
		set.retainAll(c2);
		
		return set;
	}	
	
	/**
	 * Checks if two collections have any elements in common 
	 */
	public static boolean intersects( Collection c1, Collection c2 ) {
        for(Iterator i = c1.iterator(); i.hasNext();) {
            if( c2.contains( i.next() ) ) 
                return true;
        }

        return false;
    }		

	/**
	 * Checks if one set is subset of another one
	 * 
	 * @param sub
	 * @param sup
	 * @return
	 */
	public static boolean subset(Set sub, Set sup) {
		return sub.size() <= sup.size() && sup.containsAll( sub );
	}
	
	/**
	 * Checks if one set is equal of another one
	 * 
	 * @param sub
	 * @param sup
	 * @return
	 */
	public static boolean equals(Set s1, Set s2) {
		return s1.size() == s2.size() && s1.containsAll( s2 );
	}
	
	/**
	 * Returns the difference of two sets. All the elements of second set is
	 * removed from the first set  
	 * 
	 * @param coll A Collection of sets
	 */
	public static Set difference(Collection c1, Collection c2) {
		Set set = new HashSet();
		set.addAll(c1);
		set.removeAll(c2);
		
		return set;
	}		
	
	/**
	 * 
	 * Creates a list containing all the elements in the array
	 * 
	 * @param elements
	 * @return
	 */
	public static Set create(Object[] elems) {
		Set set = new HashSet( elems.length );
		for(int i = 0; i < elems.length; i++)
			set.add( elems[i] );
		
		return set;
	}
	
	
	/**
	 * 
	 * Creates a set containing all the elements in the collection
	 * 
	 * @param elements
	 * @return
	 */
	public static Set create( Collection coll ) {
		return new HashSet( coll );
	}	
}
