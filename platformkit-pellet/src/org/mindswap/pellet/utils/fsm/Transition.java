/*
 * Created on Oct 20, 2006
 */
package org.mindswap.pellet.utils.fsm;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Clark & Parsia, LLC. <http://www.clarkparsia.com></p>
 *
 * @author Evren Sirin
 */
public class Transition {
    public static Object EPSILON = null;
    
    private Object name;

    private State to;

    /**
    /* add edge with epsilon edge
     */
    public Transition( State t ) {
        name = EPSILON;
        to = t;
    }

    /**
     * add edge for name from current state to state t on c
     */      
    public Transition( Object name, State to ) {
        this.name = name;
        this.to = to;
    }
    
    public State getTo() {
        return to;
    }

    public void setTo( State to ) {
        this.to = to;
    }
    
    public Object getName() {
        return name;
    }
    
    public boolean hasName( Object c ) {
        return (name == EPSILON) ? c == EPSILON : (c == EPSILON) ? false : name.equals( c );
    }
        
    public String toString() {
        return (name == EPSILON ? "epsilon" : name.toString() ) + " -> " + to.getName();
    }
}
