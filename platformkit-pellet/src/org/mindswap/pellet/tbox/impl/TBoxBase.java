package org.mindswap.pellet.tbox.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;

import aterm.ATerm;
import aterm.ATermAppl;

public class TBoxBase {
	public static Log log = LogFactory.getLog( TBoxBase.class );
	
	protected KnowledgeBase kb;

	protected Map<ATermAppl,TermDefinition> termhash = new HashMap<ATermAppl,TermDefinition>();
	
	public TBoxBase(KnowledgeBase kb) {
		this.kb = kb;
	}
	
	public boolean addDef(ATermAppl def) {
		ATermAppl name = (ATermAppl) def.getArgument(0);
		if (termhash.containsKey(name)) {
			getTD(name).addDef(def);
		} else {
			TermDefinition td = new TermDefinition();
			td.addDef(def);
			termhash.put(name, td);
		}
		
		return true;
	}
	
	public boolean removeDef(ATermAppl axiom) {
		boolean removed = false;
		
		ATermAppl name = (ATermAppl) axiom.getArgument( 0 );
		TermDefinition td = getTD( name );
		if( td != null ) 
			removed = td.removeDef( axiom );		
		
		return removed;
	}
	
	public boolean contains(ATerm name) {
		return termhash.containsKey(name);
	}
	
	public TermDefinition getTD(ATerm name) {
		return termhash.get(name);

	}	
	
	public boolean isEmpty() {
		return (termhash.size() == 0);
	}
	
	/**
	 * Returns the number of term definitions stored in this TBox.
	 * 
	 * @return
	 */
	public int size() {
		return termhash.size();
	}	
	
}
