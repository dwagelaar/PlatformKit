package org.mindswap.pellet.utils;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Clark & Parsia, LLC. <http://www.clarkparsia.com></p>
 *
 * @author Evren sirin
 */
public class Pair<F,S> {
    public F first;
    public S second;

    public Pair( F first, S second ) {
        if( first == null || second == null )
            throw new NullPointerException();
        
        this.first = first;
        this.second = second;        
    }

    public int hashCode() {
        return first.hashCode() + second.hashCode();        
    }
    
    public boolean equals( Object o ) {
        if( o == this )
            return true;

        if( !(o instanceof Pair) )
            return false;

        Pair p = (Pair) o;

        return first.equals( p.first ) && second.equals( p.second );
    }
    
    public String toString() {
        return "[" + first + ", " + second + "]";
    }
}
