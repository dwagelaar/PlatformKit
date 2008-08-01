package org.mindswap.pellet.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: Clark & Parsia, LLC. <http://www.clarkparsia.com></p>
 *
 * @author Evren Sirin
 */
public class MultiValueMap<K,V> extends HashMap<K,Set<V>> implements Map<K,Set<V>> {
    private static final long serialVersionUID = 2660982967886888197L;

    public MultiValueMap() {    
    }

    public Set<V> put( K key, V value ) {
        Set<V> set = new HashSet<V>();
        set.add( value );
        
        return super.put( key, set );
    }

    public Set<V> put( K key, Set<V> values ) {
        return super.put( key, values );
    }

    public boolean add( K key, V value ) {
        Set<V> values = get( key );
        if( values == null ) {
            values = new HashSet<V>();
            super.put( key, values );
        }
        
        return values.add( value );
    }
    
    public boolean remove( K key, V value ) {
    	boolean removed = false;
    	
        Set<V> values = get( key );
        if( values != null ) { 
            removed = values.remove( value );
            
            if( values.isEmpty() )
            	super.remove( key );
        }
        
        return removed;
    }
    
    public boolean add( K key, Set<V> newValues ) {
        Set<V> values = get( key );
        if( values == null ) {
            values = new HashSet<V>();
            super.put( key, values );
        }
        
        return values.addAll( newValues );
    }

}
