/*
 * Created on Oct 2, 2005
 */
package org.mindswap.pellet.utils;

/**
 * @author Evren Sirin
 *
 */
public class Bool {
    public final static Bool FALSE   = new Bool();
    public final static Bool TRUE    = new Bool();
    public final static Bool UNKNOWN = new Bool();
    
    private Bool() {
    }
    
    public static Bool create( boolean value ) {
        return value ? TRUE : FALSE;
    }
    
    public Bool not() {
        if( this == TRUE )
            return FALSE;
        
        if( this == FALSE )
            return TRUE;
        
        return UNKNOWN;       
    }

    public Bool or( Bool other ) {
        if( this == TRUE || other == TRUE )
            return TRUE;
        
        if( this == FALSE && other == FALSE )
            return FALSE;
        
        return UNKNOWN;      
    }
    
    public Bool and( Bool other ) {
        if( this == TRUE && other == TRUE )
            return TRUE;
        
        if( this == FALSE || other == FALSE )
            return FALSE;
        
        return UNKNOWN;       
    }
    
    public boolean isTrue() {
        return this == TRUE;
    }
    
    public boolean isFalse() {
        return this == FALSE;        
    }

    public boolean isUnknown() {
        return this == UNKNOWN;        
    }

    public boolean isKnown() {
        return this != UNKNOWN;        
    }
    
    public String toString() {
        return isTrue() ? "true" : isFalse() ? "false" : "unknown";
    }
}
