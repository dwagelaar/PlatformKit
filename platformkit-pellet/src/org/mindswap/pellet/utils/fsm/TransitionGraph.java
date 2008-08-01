package org.mindswap.pellet.utils.fsm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.Pair;

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
public class TransitionGraph {
    private State initialState; // the initial state for the TG

    private Set<State> allStates; // set of all states in the TG

    private Set<State> finalStates; // set of final states for the TG

    private Set<Object> alphabet; // set of all characters in TG

    public TransitionGraph() {
        initialState = null;
        allStates = new HashSet<State>();
        finalStates = new HashSet<State>();
        alphabet = new HashSet<Object>();
    }
    
    public TransitionGraph copy() {
    	TransitionGraph copy = new TransitionGraph();
    	
    	copy.alphabet = new HashSet<Object>( alphabet );
    	
    	Map<State,State> newStates = new HashMap<State,State>();
    	
    	for( State s1 : allStates ) {
            State n1 = newStates.get( s1 );
            if( n1 == null ) {
            	n1 = copy.newState();
            	newStates.put( s1, n1 );
            }
            
            if( finalStates.contains( s1 ) )
            	copy.finalStates.add( n1 );
            
            for (Iterator j = s1.transitions.iterator(); j.hasNext();) {
				Transition t = (Transition) j.next();
				State s2 = t.getTo();
				Object symbol = t.getName();
				
				State n2 = newStates.get( s2 );
	            if( n2 == null ) {
	            	n2 = copy.newState();
	            	newStates.put( s2, n2 );
	            }
	            
	            n1.addTransition( symbol, n2 );
			}
        }
        
        copy.initialState = newStates.get( initialState );

        return copy;
    }
    
    /**
     * Returns the number of states in this transition graph
     * 
     * @return
     */
    public int size() {
    	return allStates.size();
    }
    
    // ---------------------------------------------------
    // adds a new state to the graph

    public State newState() {
        State s = new State();
        allStates.add( s );
        return s;
    }    
    
    public Set getAlpahabet() {
    	return Collections.unmodifiableSet( alphabet );
    }

    public Set<State> getAllStates() {
        return Collections.unmodifiableSet( allStates );
    }
    
    public void setInitialState( State s ) {
        initialState = s;
    }

    public State getInitialState() {
        return initialState;
    }
    
    public void addFinalState( State s ) {
        finalStates.add( s );
    }

    public Set<State> getFinalStates() {
        return finalStates;
    }
    
    public State getFinalState() {
        int size =  finalStates.size();
        
        if( size == 0 )
            throw new RuntimeException( "There are no final states!" );
        else if ( size > 1 )
            throw new RuntimeException( "There is more than one final state!" );
        
        return finalStates.iterator().next();
    }
    
    public void addTransition( State begin, Object transition, State end ) {
        begin.addTransition( transition, end );
        if( transition != Transition.EPSILON )
        	alphabet.add( transition );
        else if( transition == null )
        	throw new NullPointerException();
    }
    
    public void addTransition( State begin, State end ) {
        begin.addTransition( end );
    }
    
    public List<Pair<State,State>> findTransitions( Object transition ) {
        List<Pair<State,State>> result = new ArrayList<Pair<State,State>>();
        
        for( State s1 : allStates ) {
            State s2 = s1.dMove( transition ); 
            
            if( s2 != null )
                result.add( new Pair<State,State>( s1, s2 ) );
        }
        
        return result;
    }
    
    public boolean isFinal( State st ) {
        return finalStates.contains( st );
    }
    
    // ---------------------------------------------------
    // test whether Set<State> is DFA final state (contains NFA final state)

    public boolean isFinal( Set<State> ss ) {
        Iterator i = ss.iterator();
        while( i.hasNext() ) {
            State st = (State) i.next();
            if( finalStates.contains( st ) )
                return true;
        }
        return false;
    }

    // ---------------------------------------------------
    // Return a TG that accepts just epsilon

    public TransitionGraph epsilon() {
        TransitionGraph tg = new TransitionGraph();
        State s = tg.newState();
        State f = tg.newState();
        s.addTransition( f );
        tg.initialState = s;
        tg.finalStates.add( f );
        return tg;
    }

    // ---------------------------------------------------
    // Return a TG that accepts just a single character

    public static TransitionGraph symbol( Object transition ) {
        TransitionGraph tg = new TransitionGraph();
        State s = tg.newState();
        State f = tg.newState();
        s.addTransition( transition, f );
        tg.initialState = s;
        tg.finalStates.add( f );
        tg.alphabet.add( transition );
        return tg;
    }

    // ---------------------------------------------------
    // given a DFA, print it out

    public String toString() {
        StringBuffer buf = new StringBuffer();

        Iterator i;

        buf.append( "[Transition Graph\n" );

        // print all states and edges
        i = allStates.iterator();
        while( i.hasNext() ) {
            State st = (State) i.next();
            buf.append( st ).append( "\n" );
        }

        // print start state
        buf.append( "initial state: " );
        buf.append( initialState.getName() );
        buf.append( "\n" );

        // print final state(s)
        buf.append( "final states: { " );
        i = finalStates.iterator();
        while( i.hasNext() ) {
            State st = (State) i.next();
            buf.append( st.getName() );
            if( i.hasNext() )
                buf.append( ", " );
        }
        buf.append( "}\n" );

        // print alphabet
        buf.append( "alphabet: { " );
        i = alphabet.iterator();
        while( i.hasNext() ) {
            Object transition = i.next();
            buf.append( transition );
            if( i.hasNext() )
                buf.append( ", " );
        }
        buf.append( " }\n" );
        buf.append( "]\n" );
        
        return buf.toString();
    }

    // ---------------------------------------------------
    // renumber states of TG in preorder, beginning at start state

    public TransitionGraph renumber() {
        for( State st : allStates ) {
            st.marked = false;
            st.partition_num = 0;
        }
        
        LinkedList<State> workList = new LinkedList<State>();

        int val = 0;
        workList.addFirst( initialState );

        while( workList.size() > 0 ) {
            State s = workList.removeFirst();
            s.name = val++;
            s.marked = true;

            Iterator i = s.transitions.iterator();
            while( i.hasNext() ) {
                Transition e = (Transition) i.next();
                if( !e.getTo().marked )
                    workList.addLast( e.getTo() );
            }
        }

        return this;
    }

    // ---------------------------------------------------
    // given a DFA and a string, trace the execution of
    // the DFA on the string and decide accept/reject

    public boolean accepts( List str ) {
        State s = initialState;
        for( Iterator i = str.iterator(); i.hasNext(); ) {
            Object ch = i.next();
            s = s.dMove( ch );
            if( s == null ) {
                return false;
            }
        }

        return finalStates.contains( s );
    }

    // -------------------------------------------------------------//
    // -------------------------------------------------------------//
    // --------------Make changes past this point-------------------//
    // -------------------------------------------------------------//
    // -------------------------------------------------------------//

    // ---------------------------------------------------
    // modify TG so that it accepts strings accepted by
    // either current TG or new TG

    public TransitionGraph choice( TransitionGraph t ) {
        Iterator i;
        State s = newState(); // new start state
        State f = newState(); // new final state

        // combine all states and final states
        allStates.addAll( t.allStates );
        finalStates.addAll( t.finalStates );

        // add an epsilon edge from new start state to
        // current TG's and parameter TG's start state
        s.addTransition( initialState );
        s.addTransition( t.initialState );
        initialState = s;

        // from all final states add an epsilon edge to new final state
        i = finalStates.iterator();
        while( i.hasNext() ) {
            State st = (State) i.next();
            st.addTransition( f );
        }
        // make f the only final state
        finalStates.clear();
        finalStates.add( f );

        // combine the alphabets
        alphabet.addAll( t.alphabet );

        return this;
    }

    // ---------------------------------------------------
    // modify TG so that it accepts strings composed
    // of strings accepted by current TG followed strings
    // accepted by new TG

    public TransitionGraph concat( TransitionGraph t ) {
        Iterator i;
        State s = newState(); // new start state
        State f = newState(); // new final state

        // combine all states
        allStates.addAll( t.allStates );

        // add an epsilon edge from new start state to current
        // TG's start state and make it the start state
        s.addTransition( initialState );
        initialState = s;

        // from final states of current TG add an
        // epsilon edge to start state of parameter TG
        i = finalStates.iterator();
        while( i.hasNext() ) {
            State st = (State) i.next();
            st.addTransition( t.initialState );
        }

        // from final states of parameter TG add an
        // epsilon edge to new final state
        i = t.finalStates.iterator();
        while( i.hasNext() ) {
            State st = (State) i.next();
            st.addTransition( f );
        }

        // make f the only final state
        finalStates.clear();
        finalStates.add( f );

        // combine alphabets
        alphabet.addAll( t.alphabet );

        return this;
    }

    // ---------------------------------------------------
    // Return a TG that accepts any sequence of 0 or more
    // strings accepted by TG

    public TransitionGraph closure() {
        Iterator i;
        State s = newState(); // new start state
        State f = newState(); // new final state

        // from final states of current TG add an epsilon
        // edge to old start state and new final state
        i = finalStates.iterator();
        while( i.hasNext() ) {
            State st = (State) i.next();
            st.addTransition( initialState );
            st.addTransition( f );
        }
        // make f the only final state
        finalStates.clear();
        finalStates.add( f );

        // add an epsilon edge from new start state to
        // old start state and to new final state
        s.addTransition( initialState );
        s.addTransition( f );
        initialState = s;

        return this;
    }
    
    public TransitionGraph insert( TransitionGraph t, State i, State f ) {
        Iterator it;

        // combine all states
        allStates.addAll( t.allStates );

        // combine the alphabets
        alphabet.addAll( t.alphabet );

        // add an epsilon edge from the given initial state to
        // current TG's initial state
        i.addTransition( t.getInitialState() );        

        // from all final states add an epsilon edge to new final state
        it = t.getFinalStates().iterator();
        while( it.hasNext() ) {
            State st = (State) it.next();
            st.addTransition( f );
        }
        
        return this;
    }

    // ---------------------------------------------------
    // compute a NFA move from a set of states
    // to states that are reachable by one edge labeled c

    public Set<State> move( Set<State> SS, Object c ) {
        Set<State> result = new HashSet<State>();

        // for all the states in the set SS
        Iterator i = SS.iterator();
        while( i.hasNext() ) {
            State st = (State) i.next();
            Iterator ii = st.transitions.iterator();

            // for all the edges from state st
            while( ii.hasNext() ) {
                Transition e = (Transition) ii.next();

                // add the 'to' state if transition matches
                if( e.hasName( c ) )
                    result.add( e.getTo() );
            }
        }

        return result;
    }

    // ---------------------------------------------------
    // USER DEFINED FUNCTION
    // compute from a set of states, the states that are
    // reachable by any number of edges labeled epsilon
    // from only one state

    public Set<State> epsilonClosure( State s, Set<State> result ) {
        Iterator i = s.transitions.iterator();

        // s is in the epsilon closure of itself
        result.add( s );

        // for each edge from s
        while( i.hasNext() ) {
            Transition e = (Transition) i.next();

            // if this is an epsilon transition and the result
            // does not contain 'to' state then add the epsilon
            // closure of 'to' state to the result set
            if( e.hasName( Transition.EPSILON ) && !result.contains( e.getTo() ) )
                result = epsilonClosure( e.getTo(), result );
        }

        return result;
    }

    // ---------------------------------------------------
    // compute from a set of states, the states that are
    // reachable by any number of edges labeled epsilon

    public Set<State> epsilonClosure( Set<State> SS ) {
        Set<State> result = new HashSet<State>();
        Iterator i = SS.iterator();

        // for each state in SS add their epsilon closure to the result
        while( i.hasNext() ) {
            State st = (State) i.next();
            result = epsilonClosure( st, result );
        }

        return result;
    }
    
    public boolean isDeterministic() {
    	if( !allStates.contains( initialState ) )
    		throw new InternalReasonerException();
    	
    	for (Iterator i = allStates.iterator(); i.hasNext();) {
			State s = (State) i.next();
			Set<Object> seenSymbols = new HashSet<Object>();
			for (Iterator j = s.transitions.iterator(); j.hasNext();) {
				Transition t = (Transition) j.next();
				Object symbol = t.getName();
				
				if( symbol == Transition.EPSILON || seenSymbols.contains( symbol ) )
					return false;
				
				seenSymbols.add( symbol );
			}
		}
    	
    	return true;
    }

    // ---------------------------------------------------
    // convert NFA into equivalent DFA

    public TransitionGraph determinize() {
        // Define a map for the new states in DFA. The key for the
        // elements in map is the set of NFA states and the value
        // is the new state in DFA
        HashMap<Set<State>,State> dStates = new HashMap<Set<State>,State>();

        // start state of DFA is epsilon closure of start state in NFA
        State s = new State();
        Set<State> ss = epsilonClosure( initialState, new HashSet<State>() );
        
        initialState = s;

        // unmarked states in dStates will be processed
        s.marked = false;
        dStates.put( ss, s );
        initialState = s;

        // if there are unprocessed states continue
        boolean hasUnmarked = true;
        while( hasUnmarked ) {
            State u = null;
            Set<State> U = null;

            hasUnmarked = false;

            //find an unmarked state in mappings in dStates
            for( Map.Entry<Set<State>,State> entry : dStates.entrySet() ) {				
                s = entry.getValue();
                ss = entry.getKey();
                hasUnmarked = !s.marked;
                
                if( hasUnmarked )
                	break;
            }

            if( hasUnmarked ) {
                Iterator ai = alphabet.iterator();

                // for each symbol in alphabet
                while( ai.hasNext() ) {
                    Object a = ai.next();

                    // find epsilon closure of move with a
                    U = epsilonClosure( move( ss, a ) );
                    // if result is empty continue
                    if( U.size() == 0 )
                        continue;
                    // check if this set of NFA states are
                    // already in dStates
                    u = dStates.get( U );

                    // if the result is equal to NFA states
                    // associated with the processed state
                    // then add an edge from s to itself
                    // else create a new state and add edge
                    if( u == null ) {
                        u = new State();
                        u.marked = false;
                        dStates.put( U, u );
                    }
                    else if( u.equals( s ) )
                        u = s;
                    s.addTransition( a, u );
                }
                // update s in dStates (since key is unchanged only
                // the changed value i.e state s is updated in dStates)
                s.marked = true;
                dStates.put( ss, s );
            }
        }
        // a set of final states for DFA
        Set<State> acceptingStates = new HashSet<State>();
        // clear all states
        allStates.clear();

        for( Map.Entry<Set<State>,State> entry : dStates.entrySet() ) {
            // find DFA state and corresponding set of NFA states
            s = entry.getValue();
            ss = entry.getKey();
            // add DFA state to state set
            allStates.add( s );
            // if any of NFA states are final update accepting states
            if( isFinal( ss ) )
                acceptingStates.add( s );
        }
        // accepting states becomes final states
        finalStates.clear();
        finalStates = acceptingStates;

        return this;
    }

    public void setPartition( Set<State> stateSet, int num ) {
    	for( State st : stateSet ) {
            st.partition_num = num;
        }
    }
    
    // ---------------------------------------------------
    // given a DFA, produce an equivalent minimized DFA

    
	public TransitionGraph minimize() {
		@SuppressWarnings("unchecked")
        // partitions are set of states, where max # of sets = # of states
        Set<State>[] partitions = new Set[allStates.size()];
        int numPartitions = 1;

        // first partition is the set of final states
        partitions[0] = new HashSet<State>();
        partitions[0].addAll( finalStates );
        setPartition( partitions[0], 0 );

        // check if there are any states that are not final
        if( partitions[0].size() < allStates.size() ) {
            // second partition is set of non-accepting states
            partitions[1] = new HashSet<State>();
            partitions[1].addAll( allStates );
            partitions[1].removeAll( finalStates );
            setPartition( partitions[1], 1 );
            numPartitions++;
        }

        for( int p = 0; p < numPartitions; p++ ) {
            Iterator i = partitions[p].iterator();

            // store the first element of the set
            State s = (State) i.next();
            boolean partitionCreated = false;

            // for all the states in a partition
            while( i.hasNext() ) {
                State t = (State) i.next();
                Iterator ai = alphabet.iterator();

                // for all the symbols in an alphabet
                while( ai.hasNext() ) {
                    Object a = ai.next();

                    // find move(a) for the first and current state					
                    int sn = (s.dMove( a ) == null) ? -1 : s.dMove( a ).partition_num;
                    int tn = (t.dMove( a ) == null) ? -1 : t.dMove( a ).partition_num;
                    // if they go to different partitions
                    if( sn != tn ) {
                        // if a new partition was not created in this iteration
                        // create a new partition
                        if( !partitionCreated )
                            partitions[numPartitions++] = new HashSet<State>();
                        partitionCreated = true;
                        // remove current state from this partition						
                        i.remove();
                        // add it to the new partition
                        partitions[numPartitions - 1].add( t );
                        break;
                    }
                }
            }
            if( partitionCreated ) {
                // set the partition_num for all the states put into new partition
                setPartition( partitions[numPartitions - 1], numPartitions - 1 );
                // start checking from the first partition
                p = 0;
            }
        }
        // store the partition_num of the start state
        int startPartition = initialState.partition_num;

        // for each partition the first state is marked as the representative 
        // of that partition and rest is removed from states
        for( int p = 0; p < numPartitions; p++ ) {
            Iterator i = partitions[p].iterator();
            State s = (State) i.next();
            s.rep = s;
            if( p == startPartition )
                initialState = s;
            while( i.hasNext() ) {
                State t = (State) i.next();
                allStates.remove( t );
                finalStates.remove( t );
                // set rep so that we can later update 
                // edges to this state
                t.rep = s;
            }
        }

        // correct any edges that are going to states that are removed, 
        // by updating the target state to be the rep of partition which
        // dead state belonged to
        Iterator i = allStates.iterator();
        while( i.hasNext() ) {
            State t = (State) i.next();
            Iterator e = t.transitions.iterator();
            while( e.hasNext() ) {
                Transition edge = (Transition) e.next();
                edge.setTo( edge.getTo().rep );
            }
        }

        return this;
    }
}
