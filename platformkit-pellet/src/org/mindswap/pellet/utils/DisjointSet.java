/*
 * Created on Apr 20, 2006
 */
package org.mindswap.pellet.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * DisjointSet data structure. Uses path compression and union by rank. 
 * 
 * @author Evren Sirin
 *
 */
public class DisjointSet {
    private class Node {
        Object object;
        Node parent = this;          
        int rank = 0;
        
        Node( Object o ) { object = o; }
    }
    
    private Map elements;

    public DisjointSet(){
        elements = new HashMap();
    }
    
    public void add( Object o ) { 
        if( elements.containsKey( o ) )
            return;
        
        elements.put( o, new Node( o ) );
    }
    
    public boolean isSame( Object x, Object y ) {
        return find( x ).equals( find( y ) );
    }
    
    public Object find( Object o ) {      
        return findRoot( o ).object;
    }
    
    private Node findRoot( Object o ) {      
        Node node = (Node) elements.get( o );
        while( node.parent.parent != node.parent ) {
            node.parent = node.parent.parent;
            node = node.parent;
        }
        
        return node.parent;
    }
    
    public Object union( Object x, Object y ) {      
        Node rootX = findRoot( x );
        Node rootY = findRoot( y );
        
        if( rootX.rank > rootY.rank ) {
            Node node = rootX;
            rootX = rootY;
            rootY = node;
        } 
        else if( rootX.rank == rootY.rank )
            ++rootY.rank;
        
        rootX.parent = rootY;
     
        return rootY;
    }
    
    public Collection getEquivalanceSets() {
        Map equivalanceSets = new HashMap();
        for( Iterator i = elements.keySet().iterator(); i.hasNext(); ) {
            Object x = i.next();
            Object representative = find( x );
            
            Set equivalanceSet = (Set) equivalanceSets.get( representative );
            if( equivalanceSet == null ) {
                equivalanceSet = new HashSet();
                equivalanceSets.put( representative, equivalanceSet );
            }
            equivalanceSet.add( x );
        }
        
        return equivalanceSets.values(); 
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        
        buffer.append( "{" );
        for( Iterator i = elements.values().iterator(); i.hasNext(); ) {
            Node node = (Node) i.next();
            buffer.append( node.object );
            buffer.append( " -> " );
            buffer.append( node.parent.object );
            if( i.hasNext() )
                buffer.append( ", " );
        }
        buffer.append( "}" );
        
        return buffer.toString();
    }

}
