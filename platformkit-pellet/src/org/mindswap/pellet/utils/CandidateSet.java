/*
 * Created on Oct 25, 2005
 */
package org.mindswap.pellet.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Evren Sirin
 *
 */
public class CandidateSet {
    private Set knowns, unknowns;

    public CandidateSet() {
        this.knowns = new HashSet();
        this.unknowns = new HashSet();        
    }
    
    public CandidateSet( Set knowns ) {
        this.knowns = new HashSet( knowns );
        this.unknowns = new HashSet();        
    }
    
    public CandidateSet( Set knowns, Set unknowns ) {
        this.knowns = new HashSet( knowns );
        this.unknowns = new HashSet( unknowns );
    }
    
    public Set getKnowns() {
        return knowns;
    }
    
    public Set getUnknowns() {
        return unknowns;
    }
    
    public void add( Object obj, Bool isKnown ) {
        if( isKnown.isTrue() ) {
            knowns.add( obj );
        }
        else if( isKnown.isUnknown() ) {
            unknowns.add( obj );
        }            
    }
    
    public void update( Object obj, Bool isCandidate ) {
        if( isCandidate.isTrue() ) {
            // do nothing
        }
        else if( isCandidate.isFalse() ) {
            remove( obj );
        }
        else {
            if( knowns.contains( obj ) ) {
                knowns.remove( obj );
                unknowns.add( obj );
            }                
        }            
    }
    
    public boolean remove( Object obj ) {
        return knowns.remove( obj ) || unknowns.remove( obj );
    }
    
    public boolean contains( Object obj ) {
        return knowns.contains( obj ) || unknowns.contains( obj );
    }

    public int size() {
        return knowns.size() + unknowns.size();
    }

    public Iterator iterator() {
        return new MultiIterator( knowns.iterator(), unknowns.iterator() );
    }
    
    public String toString() {
        return "Knowns: " + knowns.size() + " Unknowns: " + unknowns.size();
    }
}
