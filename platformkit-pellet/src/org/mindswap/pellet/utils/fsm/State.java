/*
 * Created on Oct 20, 2006
 */
package org.mindswap.pellet.utils.fsm;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.mindswap.pellet.Role;

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
public class State {
    static int next_unused_name = 0;

    int name; // number of state

    Set<Transition> transitions; // set of outgoing edges from state

    // for epsilonClosure(), renumber()
    // flag whether state already processed
    boolean marked; 

    // for minimize()
    // number of partition
    int partition_num; 

    State rep; // representative state for partition

    // ---------------------------------------------------
    // constructor

    public State() {
        name = next_unused_name++;
        transitions = new HashSet<Transition>();
    }

    // ---------------------------------------------------
    // add an edge to from current state to s on transition

    public void addTransition( Object transition, State s ) {
        if( transition == null || s == null )
            throw new NullPointerException();
        
        if( !(transition instanceof Role) )
            throw new ClassCastException();

        Transition t = new Transition( transition, s );
        transitions.add( t );
    }

    // ---------------------------------------------------
    // add an edge to from current state to s on epsilon transition

    public void addTransition( State s ) {
        if( s == null )
            throw new NullPointerException();
        
        Transition t = new Transition( s );
        transitions.add( t );
    }
    
    public Set<Transition> getTransitions() {
        return transitions;
    }

    // ---------------------------------------------------
    // move from state on "c"

    public State dMove( Object c ) {
        Iterator i = transitions.iterator();
        while( i.hasNext() ) {
            Transition t = (Transition) i.next();
            if( t.hasName( c ) )
                return t.getTo();
        }
        return null;
    }

    public String getName() {
        return String.valueOf( name );
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        
        buf.append( name ).append( ": " );
        Iterator i = transitions.iterator();
        while( i.hasNext() ) {
            Transition t = (Transition) i.next();
            buf.append( t );
            if( i.hasNext() )
                buf.append( ", " );
        }
        return buf.toString();
    }
}
