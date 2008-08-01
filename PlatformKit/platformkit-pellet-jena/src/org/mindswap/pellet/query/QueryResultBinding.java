/*
 * Created on Jan 8, 2005
 */
package org.mindswap.pellet.query;

import java.util.Set;

import aterm.ATermAppl;

/**
 * Results of query execution. A mapping from variable names to values, which are ATermAppl
 * objects that represent the URI for individuals and the value of literal in the form
 * literal(value, lang, datatypeURI) for data values 
 * 
 * @author Evren Sirin
 *
 */
public interface QueryResultBinding extends Cloneable {    
    public boolean hasValue(ATermAppl var);

    public ATermAppl getValue(ATermAppl var);
    
    public void setValue(ATermAppl var, ATermAppl value);
    
    public void setValues( QueryResultBinding binding );
        
    public Set getVars();   
    
    public Object clone();
}
