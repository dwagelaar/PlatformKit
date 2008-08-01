package org.mindswap.pellet.tbox.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.DependencySet;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;

public class ExplanationTable {
	private Map hierarchy;
	private Map conjUCMap;
	
	public ExplanationTable() {
        hierarchy = new HashMap();
        conjUCMap = new HashMap();
	}
	
	public ExplanationTable(ExplanationTable table) {
		hierarchy = new HashMap(table.hierarchy);
		conjUCMap = new HashMap(table.conjUCMap);
	}
	
	public void add(ATermAppl axiom) {
		DependencySet ds = PelletOptions.USE_TRACING 
        ? new DependencySet( axiom) 
        : DependencySet.INDEPENDENT; 
        add(axiom, ds);
    }
	
	public void add(ATermAppl axiom, DependencySet ds) {
		if (axiom.getName().equals(ATermUtils.SAME)) {
    		ATermAppl a = (ATermAppl) axiom.getArgument(0);
    		ATermAppl b = (ATermAppl) axiom.getArgument(1);
    		hierarchy.put(ATermUtils.makeSame(ATermUtils.normalize(a), ATermUtils.normalize(b)), ds);
    		addSub(ATermUtils.makeSub(a, b), ds);
    		addSub(ATermUtils.makeSub(b, a), ds);
    	} else if (axiom.getName().equals(ATermUtils.SUB)) {
    		addSub(axiom, ds);
    	} else {
    		throw new RuntimeException("'add' takes only class axioms (sub and same)");
    	}
    }
	
    private void addSub(ATermAppl axiom, DependencySet ds) {
        ATermAppl a1 = (ATermAppl) axiom.getArgument(0);
        ATermAppl a2 = (ATermAppl) axiom.getArgument(1);
        
        a1 = ATermUtils.normalize(a1);
        a2 = ATermUtils.normalize(a2);
        axiom = ATermUtils.makeSub(a1, a2);
        if(ATermUtils.isAnd(a2)) {
			for(ATermList cs = (ATermList) a2.getArgument(0); !cs.isEmpty(); cs = cs.getNext()) {
				ATermAppl conj = (ATermAppl) cs.getFirst();
				ATermAppl newAxiom = ATermUtils.makeSub(a1, conj);				    
				add(newAxiom, ds);
			}
        }
        
        
        DependencySet originalDS = (DependencySet) hierarchy.get(axiom);
        if (originalDS != null) {
        	ds = ds.union(originalDS, true);
        }
        hierarchy.put(axiom, ds);
    }
    
    public DependencySet get(ATermAppl atermAxiom) {
    	ATermAppl a1 = (ATermAppl) atermAxiom.getArgument(0);
        ATermAppl a2 = (ATermAppl) atermAxiom.getArgument(1);
        
        if (atermAxiom.getName().equals(ATermUtils.SAME)) {
        	atermAxiom = ATermUtils.makeSame(ATermUtils.normalize(a1), ATermUtils.normalize(a2));
        }
        else if (atermAxiom.getName().equals(ATermUtils.SUB)) {
        	atermAxiom = ATermUtils.makeSub(ATermUtils.normalize(a1), ATermUtils.normalize(a2));
        }
    	DependencySet ds = (DependencySet) hierarchy.get(atermAxiom);
    	
    	// check if negated version of axiom exists
    	if (ds==null) {
	    	a1 = ATermUtils.normalize(ATermUtils.negate(a1));
	        a2 = ATermUtils.normalize(ATermUtils.negate(a2));
	        ATermAppl notAxiom = null;
	    	if (atermAxiom.getName().equals(ATermUtils.SAME)) {
	    		notAxiom = ATermUtils.makeSame(a1, a2);
	    	}
	    	else {
	    		notAxiom = ATermUtils.makeSub(a1, a2);
	    	}
	    	ds = (DependencySet) hierarchy.get(notAxiom);
    	}
    	
    	return ds;
    }
    
    public void addConjunctUCAxioms(ATerm conj, Set axioms) {
    	conjUCMap.put(conj, axioms);
    }
    
    public Set getConjunctUCAxioms(ATerm conj) {
    	if (conjUCMap.containsKey(conj)) return (Set) conjUCMap.get(conj);
    	else return new HashSet();
    }
}
