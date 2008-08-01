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

package org.mindswap.pellet.taxonomy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 *
 */
public class TaxonomyNode {
    private ATermAppl name;
    
    private boolean hidden;
    
    protected Boolean mark;
    
    private Set<ATermAppl> equivalents;
    private List<TaxonomyNode> supers;
    private List<TaxonomyNode> subs;
    
    private Map<TaxonomyNode, Set<Set<ATermAppl>>>	superExplanations;
    
    private Set<ATermAppl> instances;
    
    public TaxonomyNode( ATermAppl name, boolean hidden ) {        
        this.name = name;
        this.hidden = hidden;
                
        equivalents = Collections.singleton( name );

        supers = new ArrayList<TaxonomyNode>( 2 );
        subs = new ArrayList<TaxonomyNode>();
    }
    
    public boolean isTop() {
    	return supers.isEmpty();
    }
    
    public boolean isBottom() {
    	return subs.isEmpty();
    }
    
    public boolean isLeaf() {
    	return subs.size() == 1 && subs.get( 0 ).isBottom();
    }
    
    public static List removeRepeatedElements(List l){
    	ArrayList result = new ArrayList();
    	int n = l.size();
    	for(int i = 0; i < n ; i++){
    		TaxonomyNode aux1 = (TaxonomyNode)l.get( i );
    		ATermAppl name1 = aux1.getName();
    		int n2 = result.size();
    		boolean b=false;
    		if(result.isEmpty())
    			result.add(aux1);
    		else{
    			for(int j = 0; j < n2 ; j++){
    				TaxonomyNode aux2 = (TaxonomyNode)result.get( j );
    				ATermAppl name2 = aux2.getName();
    				if(name2.equals(name1))
    					b= true;
    			}
    			if(b==false)
    				result.add(aux1);
    		}
    	}
    	return result;
    }
    
    public static List removeFromNodeList(List l, ATermAppl t){
    	ArrayList result = new ArrayList();
    	int n = l.size();
    	for(int i = 0; i < n ; i++){
    		TaxonomyNode aux1 = (TaxonomyNode)l.get( i );
    		if(!(aux1.getName().equals(t))){
    			result.add(aux1);
    		}
    	}
    	return result;
    }
    
    public boolean compareTo(TaxonomyNode node){
    	if(node.getName().equals(ATermUtils.TOP) || node.getName().equals(ATermUtils.BOTTOM) )
    		return true;
    	if(this.getName().equals(ATermUtils.TOP) || this.getName().equals(ATermUtils.BOTTOM) )
    		return true;
    	
    	List supers1 = (List)this.getSupers();
    	List supers2 = (List)node.getSupers();
    	supers1 = removeFromNodeList(supers1,ATermUtils.TOP);
    	supers2 = removeFromNodeList(supers2,ATermUtils.TOP);
    	supers1 = removeRepeatedElements(supers1);
    	supers2 = removeRepeatedElements(supers2);
    	
    		
    	
    	List subs1 = (List)this.getSubs();
    	List subs2 = (List)node.getSubs();
    	
    	subs1 = removeFromNodeList(subs1,ATermUtils.BOTTOM);
    	subs2 = removeFromNodeList(subs2,ATermUtils.BOTTOM);
    	subs1 = removeRepeatedElements(subs1);
    	subs2 = removeRepeatedElements(subs2);
    	
    	
    	
    	if(!this.equivalents.equals(node.equivalents)){
    		System.out.println("The class: " + this.getName() + " has different equivalent classes");
    		return false;
    	}
    	if(!compareLists(supers1,supers2)){
    		System.out.println("The class: " + this.getName() + " has different super-classes");
    		return false;
       	}
    	if(!compareLists(subs1,subs2)){
    		System.out.println("The class: " + this.getName() + " has different sub-classes");
    		return false;
    	}
    	return true;
    }
    
    public boolean compareLists(List l1, List l2){
    	int size1 = l1.size();
    	int size2 = l2.size();
    	Set toSet1 = new HashSet();
    	Set toSet2 = new HashSet();
    	if(size1!=size2)
    		return false;
    	else{
    		int n = l1.size();
    	    for(int i = 0; i < n ; i++){
    	       TaxonomyNode aux1 = (TaxonomyNode)l1.get( i );
    	       ATermAppl name1= aux1.getName();
    	       toSet1.add(name1);
    	    }
    	    int m = l2.size();
    	    for(int j = 0; j < m ; j++){
    	       TaxonomyNode aux2 = (TaxonomyNode)l2.get( j );
    	       ATermAppl name2= aux2.getName();
    	       toSet2.add(name2);
    	    }
    	    if(!toSet1.containsAll(toSet2))
    	    	return false;
    	    
    	}
    		
    	return true;
    }
    
    public boolean isHidden() {
        return hidden;
    }
    
    public boolean contains( ATermAppl c ) {
        return equivalents.contains( c );
    }
    
    public void addEquivalent(ATermAppl c) {
        if( equivalents.size() == 1 )
            equivalents = new HashSet<ATermAppl>( equivalents );
        
        equivalents.add( c );
    }
    
    public void addSub( TaxonomyNode other ) {
        if( this.equals( other ) || subs.contains( other ) )
            return;
        
        subs.add( other );
        if( !hidden )
            other.supers.add( this );
    }

    public void addSubs( Collection others ) {
        subs.addAll( others );
        if( !hidden ) {
	        for(Iterator i = others.iterator(); i.hasNext();) {
	            TaxonomyNode other = (TaxonomyNode) i.next();
	            other.supers.add( this );
	        }        
        }
    }
      
    public void addSupers( Collection others ) {
        supers.addAll( others );
        if( !hidden ) {
	        for(Iterator i = others.iterator(); i.hasNext();) {
	            TaxonomyNode other = (TaxonomyNode) i.next();
	            other.subs.add( this );
	        }        
        }
    }
    
    public void removeSub(TaxonomyNode other) {
        subs.remove( other );
        other.supers.remove( this );
        if (other.superExplanations != null)
			other.superExplanations.remove(this);
    }
    
	public void disconnect() {
        for(Iterator j = subs.iterator(); j.hasNext();) {
            TaxonomyNode sub = (TaxonomyNode) j.next();
            j.remove();
            sub.supers.remove( this );
        }
        
        for(Iterator j = supers.iterator(); j.hasNext();) {
            TaxonomyNode sup = (TaxonomyNode) j.next();
            j.remove();
            sup.subs.remove( this );
        } 
	}
    
    public void addInstance( ATermAppl ind ) {
        if( instances == null )
            instances = new HashSet();
        instances.add( ind );
    }
    
    /**
     * Add an explanation for a subsumption relationship
     * 
     * @param sup Superclass (this [= sup)
     * @param exp Set of explanation erms
     */
    public void addSuperExplanation(TaxonomyNode sup, Set<ATermAppl> exp) {
		Set<Set<ATermAppl>> exps;

		if( superExplanations == null ) {
			superExplanations = new HashMap<TaxonomyNode, Set<Set<ATermAppl>>>();
			exps = null;
		}
		else
			exps = superExplanations.get( sup );

		if( exps == null )
			exps = new HashSet<Set<ATermAppl>>();

		exps.add( exp );

		superExplanations.put( sup, exps );
	}
	
    
    public void setInstances( Set instances ) {
        this.instances = instances;
    }
    
    public ATermAppl getName() {
        return name;
    }
    
    public Set<ATermAppl> getEquivalents() {
        return equivalents;
    }
    
    public Set<ATermAppl> getInstances() {
    	if( instances == null )
			return Collections.emptySet();
    	
        return Collections.unmodifiableSet( instances );
    }
    
    /**
	 * Get the set of explanations associated with a subsumption relationship
	 * 
	 * @param sup
	 *            Superclass (this [= sup)
	 * @return Set of explanations or null if none are present
	 */
    public Set<Set<ATermAppl>> getSuperExplanations(TaxonomyNode sup) {
		return (superExplanations == null) ? null : superExplanations.get( sup );
	}
    
    public List<TaxonomyNode> getSubs() {
        return subs;
    }
    
    public List<TaxonomyNode> getSupers() {
        return supers;
    }
    
    public void setSubs( List<TaxonomyNode> subs ) {
        this.subs = subs;
    }
    
    public void setSupers( List<TaxonomyNode> supers ) {
        this.supers = supers;
    }
    
    public void removeMultiplePaths() {
        if( !hidden ) {
			for(Iterator i1 = supers.iterator();  i1.hasNext(); ) {
				TaxonomyNode sup = (TaxonomyNode) i1.next();
				 
				for(Iterator i2 = subs.iterator(); i2.hasNext(); ) {
				    TaxonomyNode sub = (TaxonomyNode) i2.next();
				    
					sup.removeSub( sub );
				}
			}
        }
    }
    
    public void print() {
        print( "" );
    }
    
    public void print( String indent ) {
        if( subs.isEmpty() ) return;
        
        System.out.print( indent );
        System.out.println( equivalents + "(" + hashCode() + ")");
        
        indent += "  ";
        for(Iterator j = subs.iterator(); j.hasNext();) {
            TaxonomyNode sub = (TaxonomyNode) j.next();
            sub.print( indent );
        }
    }

    public String toString() {
        return name.getName();// + " = " + equivalents;
    }

    protected TaxonomyNode copy(Map conversion) {
        TaxonomyNode newnode = new TaxonomyNode(name, hidden);
        return copy(newnode, conversion);
    }
    protected TaxonomyNode copy(TaxonomyNode newnode, Map conversion) {
        if(equivalents != null) {
            newnode.equivalents = new HashSet(equivalents);
        } else {
            newnode.equivalents = null;
        }
        if(instances != null) {
            newnode.instances = new HashSet(instances);
        } else {
            newnode.instances = null;
        }
        newnode.supers = copySet(supers, conversion);
        newnode.subs = copySet(subs, conversion);
        return newnode;
    }
    private List copySet(List set, Map conversion) {
        if(set == null) {
            return null;
        }
        Iterator i = set.iterator();
        List newset = new LinkedList();
        while(i.hasNext()) {
            TaxonomyNode node = (TaxonomyNode)i.next();
            if(!conversion.containsKey(node)) {
                conversion.put(node, new TaxonomyNode(node.name, node.hidden));
            }
            newset.add(conversion.get(node));
        }
        return newset;
    }

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
}
