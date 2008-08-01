/*
 * Created on Aug 29, 2004
 */
package org.mindswap.pellet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.datatypes.ValueSpace;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.tbox.TBox;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Pair;
import org.mindswap.pellet.utils.SetUtils;
import org.mindswap.pellet.utils.Timer;
import org.mindswap.pellet.utils.Timers;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;

/**
 * A completion strategy specifies how the tableau rules will be applied to an ABox. Depending on
 * the expressivity of the KB, e.g. SHIN, SHON, etc., different (more efficient) strategies may be
 * used. This class is the base for all different implementations and contains strategy independent
 * functions.
 * 
 * @author Evren Sirin
 */
public abstract class CompletionStrategy {
    public final static Log log = LogFactory.getLog( ABox.class );

    /**
     * ABox being completed
     */
    protected ABox abox;
    
    /**
     * TBox associated with the abox
     */
    protected TBox tbox;

    /**
     * Blocking method specific to this completion strategy
     */
    protected Blocking blocking;

    /**
     * Timers of the associated KB
     */
    protected Timers timers;

    /**
     * Timer to be used by the complete function. KB's consistency timer depends on this one and
     * this dependency is set in the constructor. Any concrete class that extends CompletionStrategy
     * should check this timer to respect the timeouts defined in the KB.
     */
    protected Timer completionTimer;

    /**
     * Flag to indicate that a merge operation is going on
     */
    private boolean merging = false;

    /**
     * The queue of node pairs that are waiting to be merged
     */
    protected List mergeList;

    /**
     * 
     */
    public CompletionStrategy( ABox abox, Blocking blocking ) {
        this.abox = abox;
        this.tbox = abox.getTBox();
        this.blocking = blocking;
        this.timers = abox.getKB().timers;

        completionTimer = timers.createTimer( "complete" );
    }
    
    /**
     * Return individuals to which we need to apply the initialization rules
     *  
     * @return
     */
    public Iterator getInitializeIterator() {	
    	return new IndividualIterator( abox );
	}

    public void initialize() {
        mergeList = new ArrayList();

        for( Iterator i = abox.getBranches().iterator(); i.hasNext(); ) {
            Branch branch = (Branch) i.next();
            branch.setStrategy( this );
        }

        if( abox.isInitialized() ) {
        	
            boolean first = true;
            Iterator i = getInitializeIterator();
            while( i.hasNext() ) {
                Individual n = (Individual) i.next();
                
                if( n.isMerged() )
                    continue;

                if( first ) {
                    applyUniversalRestrictions( n );
                    first = false;
                }
//                else if( !n.isChanged( Node.ALL ) )
//                    continue;

                applyAllValues( n );
                if( n.isMerged() )
                    continue;
                applyNominalRule( n );
                if( n.isMerged() )
                    continue;
                applySelfRule( n );

                //CHW-added for inc. queue must see if this is bad
                EdgeList allEdges = n.getOutEdges();
                for( int e = 0; e < allEdges.size(); e++ ) {
                    Edge edge = allEdges.edgeAt( e );
                    if( edge.getTo().isPruned() )
                        continue;

                    applyPropertyRestrictions( edge );
                    if( n.isMerged() )
                        break;
                }

                
            }

            return;
        }

        if( log.isDebugEnabled() )
            log.debug( "Initialize started" );

        abox.setBranch( 0 );

        mergeList.addAll( abox.toBeMerged );

        if( !mergeList.isEmpty() )
            mergeFirst();

        Iterator i = getInitializeIterator();
        while( i.hasNext() ) {
            Individual n = (Individual) i.next();

            if( n.isMerged() )
                continue;

            n.setChanged( true );

            applyUniversalRestrictions( n );

            applyUnfoldingRule( n );

            applySelfRule( n );

            EdgeList allEdges = n.getOutEdges();
            for( int e = 0; e < allEdges.size(); e++ ) {
                Edge edge = allEdges.edgeAt( e );

                if( edge.getTo().isPruned() )
                    continue;

                applyPropertyRestrictions( edge );

                if( n.isMerged() )
                    break;
            }
        }

        if( log.isDebugEnabled() )
            log.debug( "Merging: " + mergeList );

        if( !mergeList.isEmpty() )
            mergeFirst();

        if( log.isDebugEnabled() )
            log.debug( "Initialize finished" );

        abox.setBranch( abox.getBranches().size() + 1 );
        abox.treeDepth = 1;
        abox.changed = true;
        abox.setComplete( false );
        abox.setInitialized( true );
    }
    
    /**
     * return a complete ABox by applying all the tableau rules
     * 
     */
    abstract ABox complete();

    abstract boolean supportsPseudoModelCompletion();

    Individual createFreshIndividual( boolean isNominal ) {
        Individual ind = abox.addFreshIndividual( isNominal );

        applyUniversalRestrictions( ind );

        return ind;
    }

    void applyUniversalRestrictions( Individual node ) {
        addType( node, ATermUtils.TOP, DependencySet.INDEPENDENT );

        List<Pair<ATermAppl,Set<ATermAppl>>> UC = tbox.getUC();
        if( UC != null ) {
            if( log.isDebugEnabled() )
                log.debug( "UC  : " + node + " " + UC );

            // all the concepts inside the universal concept should
            // be added to each node manually
            for( Pair<ATermAppl,Set<ATermAppl>> pair : UC ) {
            	ATermAppl c = pair.first;
            	Set<ATermAppl> explain = pair.second; 
            	
            	DependencySet ds = new DependencySet( explain );
                addType( node, c, ds);
            }
        }

        Set reflexives = abox.getKB().getRBox().getReflexiveRoles();
        for( Iterator i = reflexives.iterator(); i.hasNext(); ) {
            Role r = (Role) i.next();
            if( log.isDebugEnabled() && !node.hasRNeighbor( r, node ) )
                log.debug( "REF : " + node + " " + r );
            addEdge( node, r, node, r.getExplainReflexive() );
        }
    }

    public void addType( Node node, ATermAppl c, DependencySet ds ) {
        if( abox.isClosed() )
            return;

        node.addType( c, ds );
        
        //update dependency index for this node
        if(PelletOptions.USE_INCREMENTAL_DELETION)
        		abox.getKB().getDependencyIndex().addTypeDependency(node.getName(), c, ds);
        
        
        
        if( log.isTraceEnabled() )
        	log.trace( "ADD: " + node + " " + c + " - " + ds + " " + ds.explain);
        
        if( c.getAFun().equals( ATermUtils.ANDFUN ) ) {
            for( ATermList cs = (ATermList) c.getArgument( 0 ); !cs.isEmpty(); cs = cs.getNext() ) {
                ATermAppl conj = (ATermAppl) cs.getFirst();

                addType( node, conj, ds );

                node = node.getSame();
            }
        }
        else if( c.getAFun().equals( ATermUtils.ALLFUN ) ) {
            applyAllValues( (Individual) node, c, ds );
        }
        else if( c.getAFun().equals( ATermUtils.SELFFUN ) ) {
            ATermAppl pred = (ATermAppl) c.getArgument( 0 );
            Role role = abox.getRole( pred );
            if( log.isDebugEnabled() && !((Individual) node).hasRSuccessor( role, node ) )
                log.debug( "SELF: " + node + " " + role + " " + node.getDepends( c ) );
            addEdge( (Individual) node, role, node, ds );
        }
        // else if( c.getAFun().equals( ATermUtils.VALUE ) ) {
        // applyNominalRule( (Individual) node, c, ds);
        // }
    }

	/**
	 * This method updates the queue in the event that there is an edge added between two nodes. The individual must be added back onto the MAXLIST 
	 */
    protected void updateQueueAddEdge(Individual subj, Role pred, Node obj) {
		// for each min and max card restrictions for the subject, a new
		// queueElement must be generated and added
		List types = subj.getTypes( Node.MAX );
		int size = types.size();
		for( int j = 0; j < size; j++ ) {
			ATermAppl c = (ATermAppl) types.get( j );
			ATermAppl max = (ATermAppl) c.getArgument( 0 );
			Role r = abox.getRole( max.getArgument( 0 ) );
			if( pred.isSubRoleOf( r ) || pred == r ) {
				QueueElement newElement = new QueueElement( subj.getName(), c );
				abox.completionQueue.add( newElement, CompletionQueue.MAXLIST );
				abox.completionQueue.add( newElement, CompletionQueue.CHOOSELIST );
			}
		}

		// if the predicate has an inverse or is inversefunctional and the obj
		// is an individual, then add the object to the list.
		if( (pred.hasNamedInverse() || pred.isInverseFunctional()) && obj instanceof Individual){
			types = ((Individual) obj).getTypes( Node.MAX );
			size = types.size();
			for( int j = 0; j < size; j++ ) {
				ATermAppl c = (ATermAppl) types.get( j );
				ATermAppl max = (ATermAppl) c.getArgument( 0 );
				Role r = abox.getRole( max.getArgument( 0 ) );
				if( pred.isSubRoleOf( r.getInverse() ) || pred == r.getInverse() ) {
					QueueElement newElement = new QueueElement( obj.getName(), c );
					abox.completionQueue.add( newElement, CompletionQueue.MAXLIST );
					abox.completionQueue.add( newElement, CompletionQueue.CHOOSELIST );
				}
			}
		}
	}
    
    public void addEdge( Individual subj, Role pred, Node obj, DependencySet ds ) {
        Edge edge = subj.addEdge( pred, obj, ds );
        
        //add to the kb dependencies
        if(PelletOptions.USE_INCREMENTAL_DELETION)
			abox.getKB().getDependencyIndex().addEdgeDependency(edge, ds);

        if(PelletOptions.USE_COMPLETION_QUEUE) {
    			// update the queue as we are adding an edge - we must add
			// elements to the MAXLIST
    			updateQueueAddEdge(subj, pred, obj);
		}
        
        
        if( edge != null ) {
    		// note that we do not need to enforce the guess rule for 
    		// datatype properties because we may only have inverse 
    		// functional datatype properties which will be handled
    		// inside applyPropertyRestrictions
    		if( subj.isBlockable() && obj.isNominal() && !obj.isLiteral() ) {
				Individual o = (Individual) obj;
				int max = o.getMaxCard( pred.getInverse() );
				if( max != Integer.MAX_VALUE
					&& !o.hasDistinctRNeighborsForMin( pred.getInverse(), max, ATermUtils.TOP, true ) ) {
					int guessMin = o.getMinCard( pred.getInverse() );
					if( guessMin == 0 )
						guessMin = 1;

					if( guessMin > max )
						return;

					GuessBranch newBranch = new GuessBranch( abox, this, o, pred.getInverse(),
							guessMin, max, ds );
					addBranch( newBranch );

					// try a merge that does not trivially fail
					if( newBranch.tryNext() == false )
						return;

					if( abox.isClosed() )
						return;
				}
			}
    		
    		applyPropertyRestrictions( edge );
        }
    }

    void applyPropertyRestrictions( Edge edge ) {
        Individual subj = edge.getFrom();
        Role pred = edge.getRole();
        Node obj = edge.getTo();
        DependencySet ds = edge.getDepends();

        applyDomainRange( subj, pred, obj, ds );
        if( subj.isPruned() || obj.isPruned() )
            return;
        applyFunctionality( subj, pred, obj );
        if( subj.isPruned() || obj.isPruned() )
            return;
        applyDisjointness( subj, pred, obj, ds );
        applyAllValues( subj, pred, obj, ds );
        if( subj.isPruned() || obj.isPruned() )
            return;
        if( pred.isObjectRole() ) {
        	Individual o = (Individual) obj;
            applyAllValues( o, pred.getInverse(), subj, ds );
            checkReflexivitySymmetry( subj, pred, o, ds );
            checkReflexivitySymmetry( o, pred.getInverse(), subj, ds );
        }
    }

    void applyDomainRange( Individual subj, Role pred, Node obj, DependencySet ds ) {
        ATermAppl domain = pred.getDomain();
        ATermAppl range = pred.getRange();

        if( domain != null ) {
            if( log.isDebugEnabled() && !subj.hasType( domain ) )
                log.debug( "DOM : " + obj + " <- " + pred + " <- " + subj + " : " + domain );
            addType( subj, domain, ds.union(pred.getExplainDomain(), abox.doExplanation()) );
        }
        if( range != null ) {
            if( log.isDebugEnabled() && !obj.hasType( range ) )
                log.debug( "RAN : " + subj + " -> " + pred + " -> " + obj + " : " + range );
            addType( obj, range, ds.union(pred.getExplainRange(), abox.doExplanation()) );
        }
    }

    void applyFunctionality( Individual subj, Role pred, Node obj ) {
        DependencySet maxCardDS = pred.isFunctional() ? pred.getExplainFunctional() : subj
            .hasMax1( pred );

        if( maxCardDS != null ) {
            applyFunctionalMaxRule( subj, pred, ATermUtils.getTop( pred ), maxCardDS );
        }

        if( pred.isDatatypeRole() && pred.isInverseFunctional() ) {
            applyFunctionalMaxRule( (Literal) obj, pred, DependencySet.INDEPENDENT );
        }
        else if( pred.isObjectRole() ) {
            Individual val = (Individual) obj;
            Role invR = pred.getInverse();

            maxCardDS = invR.isFunctional() ? invR.getExplainFunctional() : val.hasMax1( invR );

            if( maxCardDS != null )
                applyFunctionalMaxRule( val, invR, ATermUtils.TOP, maxCardDS );
        }

    }

    void applyDisjointness( Individual subj, Role pred, Node obj, DependencySet ds ) {
        // TODO what about inv edges?
        // TODO improve this check
        Set disjoints = pred.getDisjointRoles();
        if( disjoints.isEmpty() )
            return;
        EdgeList edges = subj.getEdgesTo( obj );
        for( int i = 0, n = edges.size(); i < n; i++ ) {
            Edge otherEdge = edges.edgeAt( i );

            if( disjoints.contains( otherEdge.getRole() ) ) {
                abox.setClash( Clash.unexplained( subj, ds.union( otherEdge.getDepends(), abox.doExplanation() ),
                    "Disjoint properties " + pred + " " + otherEdge.getRole() ) );
                return;
            }
        }

    }

    void checkReflexivitySymmetry( Individual subj, Role pred, Individual obj, DependencySet ds ) {        
        if( pred.isAntisymmetric() && obj.hasRSuccessor( pred, subj ) ) {
        	EdgeList edges = obj.getEdgesTo(subj, pred);
        	ds = ds.union(edges.edgeAt(0).getDepends(), abox.doExplanation());
            if( PelletOptions.USE_TRACING ) 
            	ds = ds.union(pred.getExplainAntisymmetric(), abox.doExplanation());
            abox.setClash( Clash.unexplained( subj, ds, "Antisymmetric property " + pred ) );
        }        
        else if( subj.equals( obj ) ) {
        	if( pred.isIrreflexive() ) {
                abox.setClash( Clash.unexplained( subj, ds.union(pred.getExplainIrreflexive(), abox.doExplanation()), "Irreflexive property " + pred ) );
	        }
	        else {
	            ATerm notSelfP = ATermUtils.makeNot( ATermUtils.makeSelf( pred.getName() ) );
	            if( subj.hasType( notSelfP ) )
	                abox.setClash( Clash.unexplained( subj, ds.union( subj.getDepends( notSelfP ), abox.doExplanation() ),
	                    "Local irreflexive property " + pred ) );
	        }        
        }
    }

    void applyAllValues( Individual subj, Role pred, Node obj, DependencySet ds ) {
        List allValues = subj.getTypes( Node.ALL );
        int allValuesSize = allValues.size();
        Iterator i = allValues.iterator();
        while( i.hasNext() ) {
            ATermAppl av = (ATermAppl) i.next();

            ATerm p = av.getArgument( 0 );
            ATermAppl c = (ATermAppl) av.getArgument( 1 );
            ATermList roleChain = ATermUtils.EMPTY_LIST;
            Role s = null;
            if( p.getType() == ATerm.LIST ) {
                roleChain = (ATermList) p;
                s = abox.getRole( roleChain.getFirst() );
                roleChain = roleChain.getNext();
            }
            else
                s = abox.getRole( p );

            if( pred.isSubRoleOf( s ) ) {
                DependencySet finalDS = subj.getDepends( av ).union( ds, abox.doExplanation() ).union(s.getExplainSub(pred.getName()), abox.doExplanation());
                if( roleChain.isEmpty() )
                    applyAllValues( subj, s, obj, c, finalDS );
                else {
                    ATermAppl allRC = ATermUtils.makeAllValues( roleChain, c );

                    applyAllValues( subj, allRC, finalDS );
                }
                
                if( abox.isClosed() )
                    return;
            }

            if( !s.isSimple() ) {
                DependencySet finalDS = subj.getDepends( av ).union( ds, abox.doExplanation() );
                Set subRoleChains = s.getSubRoleChains();
                for( Iterator it = subRoleChains.iterator(); it.hasNext(); ) {
                    ATermList chain = (ATermList) it.next();
                    
//                    if( !pred.getName().equals( chain.getFirst() ) )
                    Role firstRole = abox.getRole(chain.getFirst());
                    if( !pred.isSubRoleOf( firstRole ) )
                        continue;

                    ATermAppl allRC = ATermUtils.makeAllValues( chain.getNext(), c );

                    applyAllValues( subj, pred, obj, allRC, finalDS.union(
                    		firstRole.getExplainSub(pred.getName()), abox.doExplanation()).union(
                    				s.getExplainSub(chain), abox.doExplanation() ) );

                    if( subj.isMerged() || abox.isClosed() )
                        return;
                }
            }

            if( subj.isMerged() )
                return;

            obj = obj.getSame();

            // if there are self links then restart
            if( allValuesSize != allValues.size() ) {
                i = allValues.iterator();
                allValuesSize = allValues.size();
            }
        }
    }

    /**
     * Apply the unfolding rule to every concept in every node.
     */
    protected void applyUnfoldingRule( IndividualIterator i ) {
        i.reset();
        while( i.hasNext() ) {
            Individual node = (Individual) i.next();

            applyUnfoldingRule( node );

            if( abox.isClosed() )
                return;
        }
    }

    protected final void applySelfRule( Individual node ) {
        List types = node.getTypes( Node.ATOM );
        int size = types.size();
        for( int j = 0; j < size; j++ ) {
            ATermAppl c = (ATermAppl) types.get( j );

            if(!PelletOptions.MAINTAIN_COMPLETION_QUEUE && node.getDepends(c) == null)
				continue;

            
            if( ATermUtils.isSelf( c ) ) {
                ATermAppl pred = (ATermAppl) c.getArgument( 0 );
                Role role = abox.getRole( pred );
                if( log.isDebugEnabled() && !node.hasRSuccessor( role, node ) )
                    log.debug( "SELF: " + node + " " + role + " " + node.getDepends( c ) );
                addEdge( node, role, node, node.getDepends( c ) );

                if( abox.isClosed() )
                    return;
            }
        }
    }
    
    protected final void applyUnfoldingRule( QueueElement element ) {
		Individual nextIn = (Individual)abox.getNode(element.getNode());
		
		nextIn = (Individual)nextIn.getSame();
		
		if(nextIn.isPruned())
			return;

		if( blocking.isBlocked( nextIn ) ){ 
			//readd this to the queue in case the node is unblocked at a later point
			//TODO: this will impose a memory overhead, so alternative techniques should be investigated
			abox.completionQueue.add( element, CompletionQueue.ATOMLIST );
			return;
		}
		
		ATermAppl c = element.getLabel();
		applyUnfoldingRule( nextIn, c );		
		
    }

    protected final void applyUnfoldingRule( Individual node ) {
        if( !node.canApply( Node.ATOM ) || blocking.isBlocked( node ) )
            return;

        List types = node.getTypes( Node.ATOM );
        int size = types.size();
        for( int j = node.applyNext[Node.ATOM]; j < size; j++ ) {
            ATermAppl c = (ATermAppl) types.get( j );

            if(!PelletOptions.MAINTAIN_COMPLETION_QUEUE && node.getDepends(c) == null)
				continue;

            
            applyUnfoldingRule( node, c );
            
            if( abox.isClosed() )
                return; 
            
            // it is possible that unfolding added new atomic 
            // concepts that we need to further unfold
            size = types.size();  
        }
        node.applyNext[Node.ATOM] = size;
    }
    
    protected void applyUnfoldingRule( Individual node, ATermAppl c ) {
    	List<Pair<ATermAppl,Set<ATermAppl>>> unfoldingList = tbox.unfold( c );

        if( unfoldingList != null ) {
            DependencySet ds = node.getDepends( c );
            
            if(!PelletOptions.MAINTAIN_COMPLETION_QUEUE && ds == null)
    			return;           	
            
            for( Pair<ATermAppl, Set<ATermAppl>> pair : unfoldingList ) {
				ATermAppl unfoldedConcept = pair.first;
				Set<ATermAppl> unfoldingDS = pair.second;            	
            	DependencySet finalDS = ds.addExplain( unfoldingDS, abox.doExplanation() );
            	
                if( log.isDebugEnabled() && !node.hasType( unfoldedConcept ) )
                    log.debug( "UNF : " + node + ", " + c + " -> " + unfoldedConcept + " - " + finalDS );

                addType( node, unfoldedConcept, finalDS );
            }
        }
    }

    protected void applyFunctionalMaxRule( Individual x, Role s, ATermAppl c, DependencySet ds ) {
        Set functionalSupers = s.getFunctionalSupers();
        if( functionalSupers.isEmpty() )
            functionalSupers = SetUtils.singleton( s );
        LOOP:
        for( Iterator it = functionalSupers.iterator(); it.hasNext(); ) {
            Role r = (Role) it.next();

            if (PelletOptions.USE_TRACING) {
            	ds = ds.union( s.getExplainSuper(r.getName()), abox.doExplanation() ).union( r.getExplainFunctional(), abox.doExplanation() );
            }
            
            EdgeList edges = x.getRNeighborEdges( r );

            // if there is not more than one edge then func max rule won't be triggered
            if( edges.size() <= 1 )
                continue;

            // find all distinct R-neighbors of x
            Set neighbors = edges.getFilteredNeighbors( x, c );

            // if there is not more than one neighbor then func max rule won't be triggered
            if( neighbors.size() <= 1 )
                continue;

            Node head = null;

            int edgeIndex = 0;
            int edgeCount = edges.size();

            // find the head and its corresponding dependency information. 
            // since head is not necessarily the first element in the 
            // neighbor list we need to first find the un-pruned node 
            for( ; edgeIndex < edgeCount; edgeIndex++ ) {
                Edge edge = edges.edgeAt( edgeIndex );
                head = edge.getNeighbor( x );

                if( head.isPruned() || !neighbors.contains( head ) )
                    continue;

                // this node is included in the merge list because the edge
                // exists and the node has the qualification in its types
                ds = ds.union( edge.getDepends(), abox.doExplanation() );
                ds = ds.union( head.getDepends( c ), abox.doExplanation() );
                break;
            }

            // now iterate through the rest of the elements in the neighbors
            // and merge them to the head node. it is possible that we will
            // switch the head at some point because of merging rules such
            // that you alway merge to a nominal of higher level
            for( edgeIndex++; edgeIndex < edgeCount; edgeIndex++ ) {
                Edge edge = edges.edgeAt( edgeIndex );
                Node next = edge.getNeighbor( x );

                if( next.isPruned() || !neighbors.contains( next ) )
                    continue;

                // it is possible that there are multiple edges to the same
                // node, e.g. property p and its super property, so check if
                // we already merged this one
                if( head.isSame( next ) )
                    continue;

                // this node is included in the merge list because the edge
                // exists and the node has the qualification in its types
                ds = ds.union( edge.getDepends(), abox.doExplanation() );
                ds = ds.union( next.getDepends( c ), abox.doExplanation() );

                if( next.isDifferent( head ) ) {
                    ds = ds.union( head.getDepends( c ), abox.doExplanation() );
                    ds = ds.union( next.getDepends( c ), abox.doExplanation() );
                    ds = ds.union( next.getDifferenceDependency( head ), abox.doExplanation() );
                    if( r.isFunctional() )
                        abox.setClash( Clash.functionalCardinality( x, ds, r.getName() ) );
                    else
                        abox.setClash( Clash.maxCardinality( x, ds, r.getName(), 1 ) );

                    break;
                }

                if( x.isNominal() && head.isBlockable() && next.isBlockable()
                    && head.hasSuccessor( x ) && next.hasSuccessor( x ) ) {
                    Individual newNominal = createFreshIndividual( true );

                    addEdge( x, r, newNominal, ds );

                    continue LOOP;
                }
                // always merge to a nominal (of lowest level) or an ancestor
                else if( (next.getNominalLevel() < head.getNominalLevel())
                    || (!head.isNominal() && next.hasSuccessor( x )) ) {
                    Node temp = head;
                    head = next;
                    next = temp;
                }

                if( log.isDebugEnabled() )
                    log.debug( "FUNC: " + x + " for prop " + r + " merge " + next + " -> " + head
                        + " " + ds );

                mergeTo( next, head, ds );

                if( abox.isClosed() )
                    return;

                if( head.isPruned() ) {
                    ds = ds.union( head.getMergeDependency( true ), abox.doExplanation() );
                    head = head.getSame();
                }
            }
        }
    }

    protected void applyFunctionalMaxRule( Literal x, Role r, DependencySet ds ) {
        // Set functionalSupers = s.getFunctionalSupers();
        // if( functionalSupers.isEmpty() )
        // functionalSupers = SetUtils.singleton( s );
        // for(Iterator it = functionalSupers.iterator(); it.hasNext(); ) {
        // Role r = (Role) it.next();

        EdgeList edges = x.getInEdges().getEdges( r );

        // if there is not more than one edge then func max rule won't be triggered
        if( edges.size() <= 1 )
            return;// continue;

        // find all distinct R-neighbors of x
        Set neighbors = edges.getNeighbors( x );

        // if there is not more than one neighbor then func max rule won't be triggered
        if( neighbors.size() <= 1 )
            return;// continue;
        
        Individual head = null;
        DependencySet headDS = null;
        // find a nominal node to use as the head
        for( int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++ ) {
            Edge edge = edges.edgeAt( edgeIndex );
            Individual ind = edge.getFrom();
            
            if( ind.isNominal()
            	&& (head == null || ind.getNominalLevel() < head.getNominalLevel()) )  {
                head = ind;
                headDS = edge.getDepends();
            }
        }
        
        // if there is no nominal in the merge list we need to create one
        if( head == null ) {
        	head = abox.addFreshIndividual( true );
        }
        else {
        	ds = ds.union( headDS, abox.doExplanation() );
        }

        for( int i = 0; i < edges.size(); i++ ) {
            Edge edge = edges.edgeAt( i );
            Individual next = edge.getFrom();

            if( next.isPruned() )
                continue;

            // it is possible that there are multiple edges to the same
            // node, e.g. property p and its super property, so check if
            // we already merged this one
            if( head.isSame( next ) )
                continue;

            ds = ds.union( edge.getDepends(), abox.doExplanation() );

            if( next.isDifferent( head ) ) {
                ds = ds.union( next.getDifferenceDependency( head ), abox.doExplanation() );
                if( r.isFunctional() )
                    abox.setClash( Clash.functionalCardinality( x, ds, r.getName() ) );
                else
                    abox.setClash( Clash.maxCardinality( x, ds, r.getName(), 1 ) );

                break;
            }

            if( log.isDebugEnabled() )
                log.debug( "FUNC: " + x + " for prop " + r + " merge " + next + " -> " + head + " "
                    + ds );

            mergeTo( next, head, ds );

            if( abox.isClosed() )
                return;

            if( head.isPruned() ) {
                ds = ds.union( head.getMergeDependency( true ), abox.doExplanation() );
                head = (Individual) head.getSame();
            }
        }
        // }
    }

	/**
	 * Apply all values restriction to a queue element
	 * 
	 * @param element QueueElement
	 */
	void applyAllValues(QueueElement element) {

		Individual x = (Individual) abox.getNode( element.getNode() );

		//check if its been merged
		if( x.isPruned() || x.isMerged() )
			x = (Individual) x.getSame();

		if( x.isPruned() )
			return;

		applyAllValues( x );
	}

    /**
     * Iterate through all the allValuesFrom restrictions on this individual and apply the
     * restriction.
     * 
     * @param x
     */
    void applyAllValues( Individual x ) {
        List allValues = x.getTypes( Node.ALL );
        x.setChanged( Node.ALL, false );
        Iterator i = allValues.iterator();
        while( i.hasNext() ) {
            ATermAppl av = (ATermAppl) i.next();
            DependencySet avDepends = x.getDepends( av );

            if(!PelletOptions.MAINTAIN_COMPLETION_QUEUE && avDepends == null)
				continue;
            
            applyAllValues( x, av, avDepends );

            if( x.isMerged() || abox.isClosed() )
                return;

            // if there are self links through transitive properties restart
            if( x.isChanged( Node.ALL ) ) {
                i = allValues.iterator();
                x.setChanged( Node.ALL, false );
            }
        }
    }

    /**
     * Apply the allValues rule for the given type with the given dependency. The concept is in the
     * form all(r,C) and this function adds C to all r-neighbors of x
     * 
     * @param x
     * @param av
     * @param ds
     */
    void applyAllValues( Individual x, ATermAppl av, DependencySet ds ) {
        // Timer timer = kb.timers.startTimer("applyAllValues");

        if( av.getArity() == 0 )
            throw new InternalReasonerException();
        ATerm p = av.getArgument( 0 );
        ATermAppl c = (ATermAppl) av.getArgument( 1 );
        ATermList roleChain = ATermUtils.EMPTY_LIST;
        Role s = null;
        if( p.getType() == ATerm.LIST ) {
            roleChain = (ATermList) p;
            s = abox.getRole( roleChain.getFirst() );
            roleChain = roleChain.getNext();
        }
        else
            s = abox.getRole( p );

        EdgeList edges = x.getRNeighborEdges( s );
        for( int e = 0; e < edges.size(); e++ ) {
            Edge edgeToY = edges.edgeAt( e );
            Node y = edgeToY.getNeighbor( x );
            DependencySet finalDS = ds.union( edgeToY.getDepends(), abox.doExplanation() );
            
            if( roleChain.isEmpty() )
                applyAllValues( x, s, y, c, finalDS );
            else {
                ATermAppl allRC = ATermUtils.makeAllValues( roleChain, c );

                applyAllValues( (Individual) y, allRC, finalDS );
            }

            if( x.isMerged() || abox.isClosed() )
                return;
        }

        if( !s.isSimple() ) {
            Set subRoleChains = s.getSubRoleChains();
            for( Iterator it = subRoleChains.iterator(); it.hasNext(); ) {
                ATermList chain = (ATermList) it.next();
                DependencySet subChainDS = s.getExplainSub(chain);
                Role r = abox.getRole( chain.getFirst() );
                
                edges = x.getRNeighborEdges( r );
                if( !edges.isEmpty() ) {
                    ATermAppl allRC = ATermUtils.makeAllValues( chain.getNext(), c );

                    for( int e = 0; e < edges.size(); e++ ) {
                        Edge edgeToY = edges.edgeAt( e );
                        Node y = edgeToY.getNeighbor( x );
                        DependencySet finalDS = ds.union( edgeToY.getDepends(), abox.doExplanation() ).union( subChainDS, abox.doExplanation() );
                        
                        applyAllValues( x, r, y, allRC, finalDS );

                        if( x.isMerged() || abox.isClosed() )
                            return;
                    }
                }
            }
        }

        // timer.stop();
    }

    void applyAllValues( Individual subj, Role pred, Node obj, ATermAppl c, DependencySet ds ) {
        if( !obj.hasType( c ) ) {
            if( log.isDebugEnabled() ) {
                log.debug( "ALL : " + subj + " -> " + pred + " -> " + obj + " : " + c + " - " + ds );
            }

            addType( obj, c, ds );
        }
    }

    /**
     * apply some values rule to the ABox
     * 
     */
    protected void applySomeValuesRule( IndividualIterator i ) {
        i.reset();
        while( i.hasNext() ) {
            Individual x = (Individual) i.next();

            applySomeValuesRule( x );

            if( abox.isClosed() || x.isMerged() )
                return;
        }
    }

	/**
	 * apply some values rule to queue element
	 *  
	 */
	protected void applySomeValuesRule(QueueElement element) {
		Individual nextIn = (Individual) abox.getNode( element.getNode() );
		nextIn = (Individual) nextIn.getSame();
		
		if( blocking.isBlocked( nextIn ) ){
			//readd this to the queue in case the node is unblocked at a later point
			//TODO: this will impose a memory overhead, so alternative techniques should be investigated
			abox.completionQueue.add( element, CompletionQueue.SOMELIST );
			return;
		}
		
		if( nextIn.isPruned() )
			return;

		applySomeValuesRule( nextIn, element.getLabel() );
	}

    /**
     * apply some values rule to the individual
     */
    protected void applySomeValuesRule( Individual x ) {
        if( !x.canApply( Individual.SOME ) || blocking.isBlocked( x ) )
            return;

        List types = x.getTypes( Node.SOME );
        int size = types.size();
        for( int j = x.applyNext[Node.SOME]; j < size; j++ ) {
            ATermAppl sv = (ATermAppl) types.get( j );

            applySomeValuesRule( x, sv );
            
            if( abox.isClosed() || x.isPruned() )
                return;
        }
        x.applyNext[Individual.SOME] = size;
    }
    
    protected void applySomeValuesRule( Individual x, ATermAppl sv ) {
        // someValuesFrom is now in the form not(all(p. not(c)))
        ATermAppl a = (ATermAppl) sv.getArgument( 0 );
        ATermAppl s = (ATermAppl) a.getArgument( 0 );
        ATermAppl c = (ATermAppl) a.getArgument( 1 );

        
        if(!PelletOptions.MAINTAIN_COMPLETION_QUEUE && x.getDepends(sv) == null)
			return;
        
        Role role = abox.getRole( s );

        c = ATermUtils.negate( c );

        // Is there a r-neighbor that satisfies the someValuesFrom restriction
        boolean neighborFound = false;
        // Safety condition as defined in the SHOIQ algorithm.
        // An R-neighbor y of a node x is safe if
        // (i) x is blockable or if
        // (ii) x is a nominal node and y is not blocked.
        boolean neighborSafe = x.isBlockable();
        // y is going to be the node we create, and edge its connection to the
        // current node
        Node y = null;
        Edge edge = null;

        // edges contains all the edges going into of coming out from the node
        // And labeled with the role R
        EdgeList edges = x.getRNeighborEdges( role );
        // We examine all those edges one by one and check if the neighbor has
        // type C, in which case we set neighborFound to true
        for( Iterator i = edges.iterator(); i.hasNext(); ) {
            edge = (Edge) i.next();

            y = edge.getNeighbor( x );

            if( y.hasType( c ) ) {
                neighborSafe |= y.isLiteral() || !blocking.isBlocked( (Individual) y );
                if( neighborSafe ) {
                    neighborFound = true;
                    break;
                }
            }
        }

        // If we have found a R-neighbor with type C, continue, do nothing
        if( neighborFound )
            return;

        // If not, we have to create it
        DependencySet ds = x.getDepends( sv ).copy();

        // If the role is a datatype property...
        if( role.isDatatypeRole() ) {
            if( log.isDebugEnabled() )
                log.debug( "SOME: " + x + " -> " + s + " -> " + y + " : " + c + " - " + ds );

            Literal literal = (Literal) y;
            if( ATermUtils.isNominal( c ) && !PelletOptions.USE_PSEUDO_NOMINALS ) {
                abox.copyOnWrite();
                
                literal = abox.addLiteral( (ATermAppl) c.getArgument( 0 ) );
            }
            else {
                if( !role.isFunctional() || literal == null )
                    literal = abox.addLiteral();
                literal.addType( c, ds );
            }
            addEdge( x, role, literal, ds );
        }
        // If it is an object property
        else {
            if( ATermUtils.isNominal( c ) && !PelletOptions.USE_PSEUDO_NOMINALS ) {
                abox.copyOnWrite();

                ATermAppl value = (ATermAppl) c.getArgument( 0 );
                y = abox.getIndividual( value );

                if( log.isDebugEnabled() )
                    log.debug( "VAL : " + x + " -> " + s + " -> " + y + " - " + ds );

                if( y == null ) {
                    if( ATermUtils.isAnonNominal( value ) ) {
                        y = abox.addIndividual( value );
                    }
                    else if( ATermUtils.isLiteral( value ) )
                        throw new InternalReasonerException( "Object Property " + role
                            + " is used with a hasValue restriction "
                            + "where the value is a literal: " + ATermUtils.toString( value ) );
                    else
                        throw new InternalReasonerException( "Nominal " + c
                            + " is not found in the KB!" );
                }

                if( y.isMerged() ) {
                    ds = ds.union( y.getMergeDependency( true ), abox.doExplanation() );

                    y = (Individual) y.getSame();
                }

                addEdge( x, role, y, ds );
            }
            else {
                boolean useExistingNode = false;
                boolean useExistingRole = false;
                DependencySet maxCardDS = role.isFunctional() ? DependencySet.INDEPENDENT : x
                    .hasMax1( role );
                if( maxCardDS != null ) {
                    ds = ds.union( maxCardDS, abox.doExplanation() );

                    // if there is an r-neighbor and we can have at most one r then
                    // we should reuse that node and edge. there is no way that neighbor
                    // is not safe (a node is unsafe only if it is blockable and has
                    // a nominal successor which is not possible if there is a cardinality
                    // restriction on the property)
                    if( edge != null )
                        useExistingRole = useExistingNode = true;
                    else {
                        // this is the tricky part. we need some merges to happen
                        // under following conditions:
                        // 1) if r is functional and there is a p-neighbor where
                        // p is superproperty of r then we need to reuse that
                        // p neighbor for the some values restriction (no
                        // need to check subproperties because functionality of r
                        // precents having two or more successors for subproperties)
                        // 2) if r is not functional, i.e. max(r, 1) is in the types,
                        // then having a p neighbor (where p is subproperty of r)
                        // means we need to reuse that p-neighbor
                        // In either case if there are more than one such value we also
                        // need to merge them together
                        Set fs = role.isFunctional() ? role.getFunctionalSupers() : role
                            .getSubRoles();
                        
                        for( Iterator it = fs.iterator(); it.hasNext(); ) {
                            Role f = (Role) it.next();
                            edges = x.getRNeighborEdges( f );
                            if( !edges.isEmpty() ) {
                                if( useExistingNode ) {
                                	DependencySet fds = DependencySet.INDEPENDENT;
                                	if (PelletOptions.USE_TRACING) {
                                		if (role.isFunctional()) {
                                			fds = role.getExplainSuper(f.getName());
                                		} else {
                                			fds = role.getExplainSub(f.getName());
                                		}
                                	}
                                    Edge otherEdge = edges.edgeAt( 0 );
                                    Node otherNode = otherEdge.getNeighbor( x );
                                    DependencySet d = ds.union( edge.getDepends(), abox.doExplanation() ).union(
                                        otherEdge.getDepends(), abox.doExplanation() ).union(fds, abox.doExplanation());
                                    mergeTo( y, otherNode, d );
                                }
                                else {
                                    useExistingNode = true;
                                    edge = edges.edgeAt( 0 );
                                    y = edge.getNeighbor( x );
                                }
                            }
                        }
                        if( y != null )
                            y = y.getSame();
                    }
                }

                if( useExistingNode ) {
                    ds = ds.union( edge.getDepends(), abox.doExplanation() );
                }
                else {
                    y = createFreshIndividual( false );
                    y.depth = x.depth + 1;

                    if( x.depth >= abox.treeDepth )
                        abox.treeDepth = x.depth + 1;
                }

                if( log.isDebugEnabled() )
                    log.debug( "SOME: " + x + " -> " + s + " -> " + y + " : " + c
                        + (useExistingNode ? "" : " (*)") + " - " + ds );

                addType( y, c, ds );

                if( !useExistingRole )
                    addEdge( x, role, y, ds );
            }
        }   
    }

    /**
     * apply disjunction rule to the ABox
     */
    protected void applyDisjunctionRule( IndividualIterator i ) {
        i.reset();
        while( i.hasNext() ) {
            Individual node = (Individual) i.next();

            applyDisjunctionRule( node );

            if( abox.isClosed() || node.isMerged() )
                return;
        }
    }

    /**
     * apply disjunction rule to the individual
     * 
     */
    protected void applyDisjunctionRule( Individual node ) {
        node.setChanged( Node.OR, false );

        if( !node.canApply( Node.OR ) || blocking.isIndirectlyBlocked( node ) )
            return;

        List types = node.getTypes( Node.OR );

        int size = types.size();
        ATermAppl[] disjunctions = new ATermAppl[size - node.applyNext[Node.OR]];
        types.subList( node.applyNext[Node.OR], size ).toArray( disjunctions );
        if( PelletOptions.USE_DISJUNCTION_SORTING != PelletOptions.NO_SORTING )
            DisjunctionSorting.sort( node, disjunctions );

        LOOP: for( int j = 0, n = disjunctions.length; j < n; j++ ) {
            ATermAppl disjunction = disjunctions[j];

            if(!PelletOptions.MAINTAIN_COMPLETION_QUEUE && node.getDepends(disjunction) == null)
				continue;
            
            // disjunction is now in the form not(and([not(d1), not(d2), ...]))
            ATermAppl a = (ATermAppl) disjunction.getArgument( 0 );
            ATermList disjuncts = (ATermList) a.getArgument( 0 );
            ATermAppl[] disj = new ATermAppl[disjuncts.getLength()];

            for( int index = 0; !disjuncts.isEmpty(); disjuncts = disjuncts.getNext(), index++ ) {
                disj[index] = ATermUtils.negate( (ATermAppl) disjuncts.getFirst() );
                if( node.hasType( disj[index] ) )
                    continue LOOP;
            }

            DisjunctionBranch newBranch = new DisjunctionBranch( abox, this, node, disjunction,
                node.getDepends( disjunction ), disj );
            addBranch( newBranch );

            newBranch.tryNext();

            if( abox.isClosed() || node.isMerged() )
                return;
        }
        node.applyNext[Node.OR] = size;
    }

	protected void applyDisjunctionRule(QueueElement element) {

		Individual nextIn = (Individual) abox.getNode( element.getNode() );
		nextIn = (Individual) nextIn.getSame();
		if( nextIn.isPruned() )
			return;

		if( blocking.isIndirectlyBlocked( nextIn ) ){
			//readd this to the queue in case the node is unblocked at a later point
			//TODO: this will impose a memory overhead, so alternative techniques should be investigated
			abox.completionQueue.add( element, CompletionQueue.ORLIST );
			return;
		}

		ATermAppl disjunction = element.getLabel();

		
        if(!PelletOptions.MAINTAIN_COMPLETION_QUEUE && nextIn.getDepends(disjunction) == null)
			return;

		// disjunction is now in the form not(and([not(d1), not(d2), ...]))
		ATermAppl a = (ATermAppl) disjunction.getArgument( 0 );
		ATermList disjuncts = (ATermList) a.getArgument( 0 );
		ATermAppl[] disj = new ATermAppl[disjuncts.getLength()];

		for( int index = 0; !disjuncts.isEmpty(); disjuncts = disjuncts.getNext(), index++ ) {
			disj[index] = ATermUtils.negate( (ATermAppl) disjuncts.getFirst() );
			if( nextIn.hasType( disj[index] ) )
				return;
		}

		DisjunctionBranch newBranch = new DisjunctionBranch( abox, this, nextIn, disjunction,
				nextIn.getDepends( disjunction ), disj );
		addBranch( newBranch );

		newBranch.tryNext();
	}

    /**
     * 
     * applyMaxRule
     * 
     * @param x
     * @param r
     * @param k
     * @param ds
     * 
     * @return true if more merges are required for this maxCardinality
     */
    protected boolean applyMaxRule( Individual x, Role r, ATermAppl c, int k, DependencySet ds ) {

        EdgeList edges = x.getRNeighborEdges( r );
        // find all distinct R-neighbors of x
        Set neighbors = edges.getFilteredNeighbors( x, c );

        int n = neighbors.size();

        // if( log.isDebugEnabled() )
        // log.debug( "Neighbors: " + n + " maxCardinality: " + k);

        // if restriction was maxCardinality 0 then having any R-neighbor
        // violates the restriction. no merge can fix this. compute the
        // dependency and return
        if( k == 0 && n > 0 ) {
            for( int e = 0; e < edges.size(); e++ ) {
                Edge edge = edges.edgeAt( e );
                Node neighbor = edge.getNeighbor( x );
                DependencySet typeDS = neighbor.getDepends( c );
                if( typeDS != null ) {
                	DependencySet subDS = r.getExplainSub(edge.getRole().getName());
                	if (subDS != null) {
                		ds = ds.union(subDS, abox.doExplanation());
                	}
	                ds = ds.union( edge.getDepends(), abox.doExplanation() );
	                ds = ds.union( typeDS, abox.doExplanation() );
	                
                }
            }

            abox.setClash( Clash.maxCardinality( x, ds, r.getName(), 0 ) );
            return false;
        }

        // if there are less than n neighbors than max rule won't be triggered
        // return false because no more merge required for this role
        if( n <= k )
            return false;        
        
        // create the pairs to be merged
        List mergePairs = new ArrayList();
        DependencySet differenceDS = findMergeNodes( neighbors, x, mergePairs );
        ds = ds.union( differenceDS, abox.doExplanation() );

        // if no pairs were found, i.e. all were defined to be different from
        // each other, then it means this max cardinality restriction is
        // violated. dependency of this clash is on all the neighbors plus the
        // dependency of the restriction type
        if( mergePairs.size() == 0 ) {
            DependencySet dsEdges = x.hasDistinctRNeighborsForMax( r, k + 1, c );
            if( dsEdges == null ) {
            	if( log.isDebugEnabled() )
                	log.debug( "Cannot determine the exact clash dependency for " + x );
                abox.setClash( Clash.maxCardinality( x, ds ) );
                return false;
            }
            else {
                if( log.isDebugEnabled() )
                    log.debug( "Early clash detection for max rule worked " + x + " has more than "
                        + k + " " + r + " edges " + ds.union( dsEdges, abox.doExplanation() ) + " "
                        + x.getRNeighborEdges( r ).getNeighbors( x ) );

                if( abox.doExplanation() )
                    abox.setClash( Clash.maxCardinality( x, ds.union( dsEdges, abox.doExplanation() ), r.getName(), k ) );
                else
                    abox.setClash( Clash.maxCardinality( x, ds.union( dsEdges, abox.doExplanation() ) ) );

                return false;
            }
        }

        // add the list of possible pairs to be merged in the branch list
        MaxBranch newBranch = new MaxBranch( abox, this, x, r, k, c, mergePairs, ds );
        addBranch( newBranch );

        // try a merge that does not trivially fail
        if( newBranch.tryNext() == false )
            return false;

        if( log.isDebugEnabled() )
            log.debug( "hasMore: " + (n > k + 1) );

        // if there were exactly k + 1 neighbors the previous step would
        // eliminate one node and only n neighbors would be left. This means
        // restriction is satisfied. If there were more than k + 1 neighbors
        // merging one pair would not be enough and more merges are required,
        // thus false is returned
        return n > k + 1;
    }

    protected void applyChooseRule( IndividualIterator i ) {
        i.reset();

        while( i.hasNext() ) {
            Individual x = (Individual) i.next();

            applyChooseRule( x );

            if( abox.isClosed() )
                return;
        }
    }
    
    
    /**
	 * apply choose rule to a queue element
	 * 
	 */
	protected void applyChooseRule(QueueElement element) {
		Individual nextIn = (Individual) abox.getNode( element.getNode() );
		nextIn = (Individual) nextIn.getSame();
		if( nextIn.isPruned() )
			return;

		applyChooseRule( nextIn, element.getLabel() );
	}

    /**
     * Apply max rule to the individual.
     */
    protected void applyChooseRule( Individual x ) {
        if( !x.canApply( Individual.MAX ) || blocking.isIndirectlyBlocked( x ) )
            return;

        List maxCardinality = x.getTypes( Node.MAX );
        Iterator j = maxCardinality.iterator();

        while( j.hasNext() ) {
        	ATermAppl maxCard = (ATermAppl) j.next();
        	applyChooseRule( x, maxCard );
        }
    }
    
    protected void applyChooseRule( Individual x, ATermAppl maxCard ) {
        // max(r, n, c) is in normalized form not(min(p, n + 1, c))       
        ATermAppl max = (ATermAppl) maxCard.getArgument( 0 );
        Role r = abox.getRole( max.getArgument( 0 ) );
        ATermAppl c = (ATermAppl) max.getArgument( 2 );

        if( ATermUtils.isTop( c ) )
            return;
        
        if(!PelletOptions.MAINTAIN_COMPLETION_QUEUE && x.getDepends(maxCard) == null)
    			return;

        EdgeList edges = x.getRNeighborEdges( r );
        for( Iterator i = edges.iterator(); i.hasNext(); ) {
            Edge edge = (Edge) i.next();
            Node neighbor = edge.getNeighbor( x );

            if( !neighbor.hasType( c ) && !neighbor.hasType( ATermUtils.negate( c ) ) ) {
                ChooseBranch newBranch = new ChooseBranch( abox, this, neighbor, c, x
                    .getDepends( maxCard ) );
                addBranch( newBranch );

                newBranch.tryNext();

                if( abox.isClosed() )
                    return;
            }
        }    	
    }

    protected void applyGuessingRule( IndividualIterator i ) {
        i.reset();
        LOOP: while( i.hasNext() ) {
            Individual x = (Individual) i.next();

            if( x.isBlockable() )
                continue;

            List types = x.getTypes( Node.MAX );
            int size = types.size();
            for( int j = 0; j < size; j++ ) {
                ATermAppl mc = (ATermAppl) types.get( j );

                applyGuessingRule( x, mc );
                
                if( abox.isClosed() )
                    return;

                if( x.isPruned() )
                    break LOOP;
            }
        }
    }
    
    /**
     * Guessing rule for a queue element
     */
    protected void applyGuessingRule(QueueElement element){
    	// get the individuals
		Individual x = (Individual) abox.getNode( element.getNode() );
		x = (Individual)x.getSame();
		
		if( x.isBlockable() ){
			//readd this to the queue in case the node is unblocked at a later point
			//TODO: this will impose a memory overhead, so alternative techniques should be investigated
			abox.completionQueue.add( element, CompletionQueue.GUESSLIST );
			return;
		}
		
		//if its been pruned, return
		if( x.isPruned() )
			return;

		applyGuessingRule(x, element.getLabel());
		
    }
    
    

    private void applyGuessingRule(Individual x, ATermAppl mc) {

        // max(r, n) is in normalized form not(min(p, n + 1))
        ATermAppl max = (ATermAppl) mc.getArgument( 0 );

        Role r = abox.getRole( max.getArgument( 0 ) );
        int n = ((ATermInt) max.getArgument( 1 )).getInt() - 1;

        // obviously if r is a datatype role then there can be no r-predecessor
        // and we cannot apply the rule
        if( r.isDatatypeRole() )
            return;

        // FIXME instead of doing the following check set a flag when the edge is added
        // check that x has to have at least one r neighbor y
        // which is blockable and has successor x
        // (so y is an inv(r) predecessor of x)
        boolean apply = false;
        EdgeList edges = x.getRPredecessorEdges( r.getInverse() );
        for( int e = 0; e < edges.size(); e++ ) {
            Edge edge = edges.edgeAt( e );
            Individual pred = edge.getFrom();
            if( pred.isBlockable() ) {
                apply = true;
                break;
            }
        }
        if( !apply )
            return;

        if( x.getMaxCard( r ) < n )
            return;

        if( x.hasDistinctRNeighborsForMin( r, n, ATermUtils.TOP, true ) )
            return;

        // if( n == 1 ) {
        // throw new InternalReasonerException(
        // "Functional rule should have been applied " +
        // x + " " + x.isNominal() + " " + edges);
        // }

        int guessMin = x.getMinCard( r );
        if( guessMin == 0 )
            guessMin = 1;

        // TODO not clear what the correct ds is so be pessimistic and include everything
        DependencySet ds = x.getDepends( mc );
        edges = x.getRNeighborEdges( r );
        for( int e = 0; e < edges.size(); e++ ) {
            Edge edge = edges.edgeAt( e );
            ds = ds.union( edge.getDepends(), abox.doExplanation() );
        }

        GuessBranch newBranch = new GuessBranch( abox, this, x, r, guessMin, n, ds );
        addBranch( newBranch );

        newBranch.tryNext();
	}

	/**
     * Apply max rule to all the individuals in the ABox.
     * 
     */
    protected void applyMaxRule( IndividualIterator i ) {
        i.reset();
        while( i.hasNext() ) {
            Individual x = (Individual) i.next();

            applyMaxRule( x );

            if( abox.isClosed() )
                return;
        }
    }

    /**
     * Apply max rule to the individual.
     */
    protected void applyMaxRule( Individual x ) {
        if( !x.canApply( Individual.MAX ) || blocking.isIndirectlyBlocked( x ) )
            return;

        List maxCardinality = x.getTypes( Node.MAX );
        Iterator j = maxCardinality.iterator();
        while( j.hasNext() ) {
            ATermAppl mc = (ATermAppl) j.next();

            applyMaxRule( x, mc );
            
            if( abox.isClosed() )
                return;

            if( x.isMerged() ) 
                return;
        }

        x.setChanged( Node.MAX, false );
    }
    
    protected void applyMaxRule( Individual x, ATermAppl mc ) {
 
        // max(r, n) is in normalized form not(min(p, n + 1))
        ATermAppl max = (ATermAppl) mc.getArgument( 0 );

        Role r = abox.getRole( max.getArgument( 0 ) );
        int n = ((ATermInt) max.getArgument( 1 )).getInt() - 1;
        ATermAppl c = (ATermAppl) max.getArgument( 2 );

        DependencySet ds = x.getDepends( mc );

        if(!PelletOptions.MAINTAIN_COMPLETION_QUEUE && ds == null)
        		return;
        		
        
        if( n == 1 ) {
            applyFunctionalMaxRule( x, r, c, ds );
            if( abox.isClosed() )
                return;
        }
        else {
            boolean hasMore = true;
            
            while( hasMore ) {
            		hasMore = applyMaxRule( x, r, c, n, ds );

                if( abox.isClosed() )
                    return;

                if( x.isMerged() ) {
                    return;
                }

                if( hasMore ) {
                    // subsequent merges depend on the previous merge
                    ds = ds.union( new DependencySet( abox.getBranches().size() ), abox.doExplanation() );
                }
            }
        }
    
    }
    
    /**
	 * apply max rule to a queue element
	 * 
	 */
	protected void applyMaxRule(QueueElement element) {
		Individual nextIn = (Individual) abox.getNode( element.getNode() );
		nextIn = (Individual) nextIn.getSame();
		
		if( blocking.isIndirectlyBlocked( nextIn ) ){
			//readd this to the queue in case the node is unblocked at a later point
			//TODO: this will impose a memory overhead, so alternative techniques should be investigated
			abox.completionQueue.add( element, CompletionQueue.MAXLIST );
			return;
		}
		
		if( nextIn.isPruned() )
			return;

		applyMaxRule( nextIn, element.getLabel() );
	}

    /**
     * Apply min rule to all the individuals
     */
    protected void applyMinRule( IndividualIterator i ) {
        i.reset();
        while( i.hasNext() ) {
            Individual x = (Individual) i.next();

            applyMinRule( x );

            if( abox.isClosed() )
                return;
        }
    }

    /**
     * Apply min rule to the individual
     */
    protected void applyMinRule( Individual x ) {
        if( !x.canApply( Node.MIN ) || blocking.isBlocked( x ) )
            return;

        // We get all the minCard restrictions in the node and store
        // them in the list ''types''
        List types = x.getTypes( Node.MIN );
        int size = types.size();
        for( int j = x.applyNext[Node.MIN]; j < size; j++ ) {
            // mc stores the current type (the current minCard restriction)
            ATermAppl mc = (ATermAppl) types.get( j );

            applyMinRule( x, mc );

            if( abox.isClosed() )
                return;
        }
        x.applyNext[Node.MIN] = size;
    }
    
    protected void applyMinRule( Individual x, ATermAppl mc ) {
        // We retrieve the role associated to the current
        // min restriction
        Role r = abox.getRole( mc.getArgument( 0 ) );
        int n = ((ATermInt) mc.getArgument( 1 )).getInt();
        ATermAppl c = (ATermAppl) mc.getArgument( 2 );

        // FIXME make sure all neighbors are safe
        if( x.hasDistinctRNeighborsForMin( r, n, c ) )
            return;

        DependencySet ds = x.getDepends( mc );
        
        if(!PelletOptions.MAINTAIN_COMPLETION_QUEUE && ds == null)
			return;


        if( log.isDebugEnabled() )
            log.debug( "MIN : " + x + " -> " + r + " -> anon"
                + (n == 1 ? "" : (abox.anonCount + 1) + " - anon") + (abox.anonCount + n) + " "
                + c + " " + ds );

        Node[] y = new Node[n];
        for( int c1 = 0; c1 < n; c1++ ) {
            if( r.isDatatypeRole() )
                y[c1] = abox.addLiteral();
            else {
                y[c1] = createFreshIndividual( false );
                y[c1].depth = x.depth + 1;

                if( x.depth >= abox.treeDepth )
                    abox.treeDepth = x.depth + 1;
            }
            Node succ = y[c1];
            DependencySet finalDS = ds;

            addEdge( x, r, succ, ds );
            if( succ.isPruned() ) {
                succ = succ.getMergedTo();
                finalDS = finalDS.union( succ.getMergeDependency( true ), abox.doExplanation() );
            }

            addType( succ, c, finalDS );
            for( int c2 = 0; c2 < c1; c2++ )
                succ.setDifferent( y[c2], ds );
        }
    }

	protected void applyMinRule(QueueElement element) {
		Individual x = (Individual) abox.getNode( element.getNode() );
		x = (Individual) x.getSame();
		
		if( blocking.isBlocked( x ) ) {
			//readd this to the queue in case the node is unblocked at a later point
			//TODO: this will impose a memory overhead, so alternative techniques should be investigated
			abox.completionQueue.add( element, CompletionQueue.MINLIST );
			return;
		}
		
		if( x.isPruned() )
			return;

		ATermAppl mc = element.getLabel();
		
		applyMinRule( x, mc );
	}


	protected void applyLiteralRule(QueueElement element) {
		Node node = (Node) abox.getNode( element.getNode() );
		if( !(node instanceof Literal) )
			return;

		//get the actual literal
		Literal lit = (Literal) node;
		lit = (Literal) lit.getSame();
		if( lit.isPruned() )
			return;

		if( lit.getValue() != null )
			return;

		LiteralValueBranch newBranch = new LiteralValueBranch( abox, this, lit, lit.getDatatype() );
		addBranch( newBranch );

		newBranch.tryNext();

		if( abox.isClosed() )
			return;

	}


    protected void applyLiteralRule() {
        Iterator i = new LiteralIterator( abox );
        while( i.hasNext() ) {
            Literal lit = (Literal) i.next();

            if( lit.getValue() != null )
                continue;

            LiteralValueBranch newBranch = new LiteralValueBranch( abox, this, lit, lit
                .getDatatype() );
            addBranch( newBranch );

            newBranch.tryNext();

            if( abox.isClosed() )
                return;
        }
    }

    protected void applyNominalRule( IndividualIterator i ) {
        // boolean ruleApplied = true;
        // while(ruleApplied) {
        // ruleApplied = false;

        i.reset();
        while( i.hasNext() ) {
            Individual y = (Individual) i.next();

            if( !y.canApply( Individual.NOM ) || blocking.isBlocked( y ) )
                continue;

            applyNominalRule( y );

            y.setChanged( Node.NOM, false );

            if( abox.isClosed() )
                return;

            if( y.isMerged() ) {
                // ruleApplied = true;
                applyNominalRule( (Individual) y.getSame() );
                // break;
            }
        }
        // }
    }

    void applyNominalRule( Individual y ) {
        if( PelletOptions.USE_PSEUDO_NOMINALS )
            return;

        List types = y.getTypes( Node.NOM );
        int size = types.size();
        for( int j = 0; j < size; j++ ) {
            ATermAppl nc = (ATermAppl) types.get( j );
            DependencySet ds = y.getDepends( nc );

            if(!PelletOptions.MAINTAIN_COMPLETION_QUEUE && ds == null)
				continue;
            
            applyNominalRule( y, nc, ds );

            if( abox.isClosed() || y.isMerged() )
                return;
        }
    }
    
    void applyNominalRule(QueueElement element) {
		if( PelletOptions.USE_PSEUDO_NOMINALS )
			return;

		//get individual from the queue element - need to chase it down on the fly, 
		//and return if its pruned
		Individual y = (Individual) abox.getNode( element.getNode() );
		y = (Individual) y.getSame();
		
		if( blocking.isBlocked(y) ){
			//readd this to the queue in case the node is unblocked at a later point
			//TODO: this will impose a memory overhead, so alternative techniques should be investigated
			abox.completionQueue.add( element, CompletionQueue.NOMLIST );
			return;
		}
		
		if( y.isPruned() )
			return;

		//get label
		ATermAppl nc = element.getLabel();

		//get dependency set
		DependencySet ds = y.getDepends( nc );

        if(!PelletOptions.MAINTAIN_COMPLETION_QUEUE && ds == null)
			return;
		
		//apply the nom rule
		applyNominalRule( y, nc, ds );

	}
    
    

    void applyNominalRule( Individual y, ATermAppl nc, DependencySet ds ) {
        abox.copyOnWrite();

        ATermAppl nominal = (ATermAppl) nc.getArgument( 0 );
        // first find the individual for the given nominal
        Individual z = abox.getIndividual( nominal );
        if( z == null ) {
            if( ATermUtils.isAnonNominal( nominal ) ) {
                z = abox.addIndividual( nominal );
            }
            else
                throw new InternalReasonerException( "Nominal " + nominal + " not found in KB!" );
        }

        // Get the value of mergedTo because of the following possibility:
        // Suppose there are three individuals like this
        // [x,{}],[y,{value(x)}],[z,{value(y)}]
        // After we merge x to y, the individual x is now represented by
        // the node y. It is too hard to update all the references of
        // value(x) so here we find the actual representative node
        // by calling getSame()
        if( z.isMerged() ) {
            ds = ds.union( z.getMergeDependency( true ), abox.doExplanation() );

            z = (Individual) z.getSame();

			if( abox.getBranch() > 0 && PelletOptions.USE_COMPLETION_QUEUE )
				abox.completionQueue.addEffected( abox.getBranch(), z.getName() );
        }

        if( y.isSame( z ) )
            return;

        if( y.isDifferent( z ) ) {
            ds = ds.union( y.getDifferenceDependency( z ), abox.doExplanation() );
            if( abox.doExplanation() )
                abox.setClash( Clash.nominal( y, ds, z.getName() ) );
            else
                abox.setClash( Clash.nominal( y, ds ) );
            return;
        }

        if( log.isDebugEnabled() )
            log.debug( "NOM:  " + y + " -> " + z );

        mergeTo( y, z, ds );
    }

    private void mergeLater( Node y, Node z, DependencySet ds ) {
        mergeList.add( new NodeMerge( y, z, ds ) );
    }

    /**
     * Merge the first node pair in the queue.
     */
    protected void mergeFirst() {
        NodeMerge merge = (NodeMerge) mergeList.remove( 0 );

        Node y = abox.getNode( merge.y );
        Node z = abox.getNode( merge.z );
        DependencySet ds = merge.ds;
        
        if( y.isMerged() ) {
        	ds = ds.union( y.getMergeDependency( true ), abox.doExplanation() );
        	y = y.getSame();
        }
        
        if( z.isMerged() ) {
        	ds = ds.union( z.getMergeDependency( true ), abox.doExplanation() );
        	z = z.getSame();
        }

        if( y.isPruned() || z.isPruned() )
            return;

        mergeTo( y, z, ds );
    }

    /**
     * Merge node y into z. Node y and all its descendants will be pruned from the completion graph.
     * 
     * @param y
     *            Node being pruned
     * @param z
     *            Node that is being merged into
     * @param ds
     *            Dependency of this merge operation
     */
    public void mergeTo( Node y, Node z, DependencySet ds ) {

		//add to effected list of queue
		if( abox.getBranch() >= 0 && PelletOptions.USE_COMPLETION_QUEUE ) {
			abox.completionQueue.addEffected( abox.getBranch(), y.getName() );
			abox.completionQueue.addEffected( abox.getBranch(), z.getName() );
		}
		
		//add to merge dependency to dependency index
        if(PelletOptions.USE_INCREMENTAL_DELETION)
        		abox.getKB().getDependencyIndex().addMergeDependency(y.getName(), z.getName(), ds);
		
        if( y.isSame( z ) )
            return;
        else if( y.isDifferent( z ) ) {
            abox.setClash( Clash.nominal( y, y.getDifferenceDependency( z ).union( ds, abox.doExplanation() ) ) );
            return;
        }

        if( !y.isSame( z ) ) {
	        abox.changed = true;
	
	        if( merging ) {
	            mergeLater( y, z, ds );
	            return;
	        }
	        else
	            merging = true;
	
	        if( log.isDebugEnabled() )
	            log.debug( "MERG: " + y + " -> " + z + " " + ds );
	
	        ds = ds.copy();
	        ds.branch = abox.getBranch();
	
	        if( y instanceof Literal && z instanceof Literal )
	            mergeLiterals( (Literal) y, (Literal) z, ds );
	        else if( y instanceof Individual && z instanceof Individual )
	            mergeIndividuals( (Individual) y, (Individual) z, ds );
	        else
	            throw new InternalReasonerException( "Invalid merge operation!" );
        }
    	
        merging = false;
        
        if( !mergeList.isEmpty() ) {
            if( abox.isClosed() )
                return;

            mergeFirst();
        }
    }

    /**
     * Merge individual y into x. Individual y and all its descendants will be pruned from the
     * completion graph.
     * 
     * @param y
     *            Individual being pruned
     * @param x
     *            Individual that is being merged into
     * @param ds
     *            Dependency of this merge operation
     */
    protected void mergeIndividuals( Individual y, Individual x, DependencySet ds ) {
    		y.setSame( x, ds );

    		// if both x and y are blockable x still remains blockable (nominal level
        // is still set to BLOCKABLE), if one or both are nominals then x becomes
        // a nominal with the minimum level
        x.setNominalLevel( Math.min( x.getNominalLevel(), y.getNominalLevel() ) );

        // copy the types
        Map types = y.getDepends();
        for( Iterator yTypes = types.entrySet().iterator(); yTypes.hasNext(); ) {
            Map.Entry entry = (Map.Entry) yTypes.next();
            ATermAppl yType = (ATermAppl) entry.getKey();
            DependencySet finalDS = ds.union( (DependencySet) entry.getValue(), abox.doExplanation() );
            addType( x, yType, finalDS );
        }

        // for all edges (z, r, y) add an edge (z, r, x)
        EdgeList inEdges = y.getInEdges();
        for( int e = 0; e < inEdges.size(); e++ ) {
            Edge edge = inEdges.edgeAt( e );

            Individual z = edge.getFrom();
            Role r = edge.getRole();
            DependencySet finalDS = ds.union( edge.getDepends(), abox.doExplanation() );

            // if y has a self edge then x should have the same self edge
            if( y.equals( z ) ) {
                addEdge( x, r, x, finalDS );
            }
            // if z is already a successor of x add the reverse edge
            else if( x.hasSuccessor( z ) ) {
                // FIXME what if there were no inverses in this expressitivity
                addEdge( x, r.getInverse(), z, finalDS );
            }
            else {
                addEdge( z, r, x, finalDS );
            }

            // only remove the edge from z and keep a copy in y for a
            // possible restore operation in the future
            z.removeEdge( edge );

			//add to effected list of queue
			if( abox.getBranch() >= 0 && PelletOptions.USE_COMPLETION_QUEUE ) {
				abox.completionQueue.addEffected( abox.getBranch(), z.getName() );
			}
        }

        // for all z such that y != z set x != z
        x.inheritDifferents( y, ds );

        // we want to prune y early due to an implementation issue about literals
        // if y has an outgoing edge to a literal with concrete value
        y.prune( ds );

        // for all edges (y, r, z) where z is a nominal add an edge (x, r, z)
        EdgeList outEdges = y.getOutEdges();
        for( int e = 0; e < outEdges.size(); e++ ) {
            Edge edge = outEdges.edgeAt( e );
            Node z = edge.getTo();

            if( z.isNominal() && !y.equals( z ) ) {
                Role r = edge.getRole();
                DependencySet finalDS = ds.union( edge.getDepends(), abox.doExplanation() );

                addEdge( x, r, z, finalDS );

				//add to effected list of queue
				if( abox.getBranch() >= 0 && PelletOptions.USE_COMPLETION_QUEUE ) {
					abox.completionQueue.addEffected( abox.getBranch(), z.getName() );
				}
                // do not remove edge here because prune will take care of that
            }
        }
    }

    /**
     * Merge literal y into x. Literal y will be pruned from* the completion graph.
     * 
     * @param y
     *            Literal being pruned
     * @param x
     *            Literal that is being merged into
     * @param ds
     *            Dependency of this merge operation
     */
    protected void mergeLiterals( Literal y, Literal x, DependencySet ds ) {
        y.setSame( x, ds );

        x.addAllTypes( y.getDepends(), ds );

        // for all edges (z, r, y) add an edge (z, r, x)
        EdgeList inEdges = y.getInEdges();
        for( int e = 0; e < inEdges.size(); e++ ) {
            Edge edge = inEdges.edgeAt( e );

            Individual z = edge.getFrom();
            Role r = edge.getRole();
            DependencySet finalDS = ds.union( edge.getDepends(), abox.doExplanation() );

            addEdge( z, r, x, finalDS );

            // only remove the edge from z and keep a copy in y for a
            // possible restore operation in the future
            z.removeEdge( edge );

			//add to effected list of queue
			if( abox.getBranch() >= 0 && PelletOptions.USE_COMPLETION_QUEUE ) {
				abox.completionQueue.addEffected( abox.getBranch(), z.getName() );
        	}
		}

        // Edge edge = y.getInEdge();
        // Individual z = edge.getFrom();
        //
        // // only remove the edge from z and keep a copy in y for a
        // // possible restore operation in the future
        // z.removeEdge( edge );
        //
        // // no need to add the edge (z, r, x) because only way these two literals
        // // can merge is if there is an edge (z, r, x)
        // if( !z.hasEdge( edge.getRole(), x ) )
        // throw new InternalReasonerException( "Cannot find expected edge " + edge);

        x.inheritDifferents( y, ds );

        y.prune( ds );
    }

    DependencySet findMergeNodes( Set neighbors, Individual node, List pairs ) {
        Timer t = timers.startTimer( "findMergeNodes" );
        DependencySet ds = DependencySet.INDEPENDENT;

        List nodes = new ArrayList( neighbors );
        for( int i = 0; i < nodes.size(); i++ ) {
            Node y = (Node) nodes.get( i );
            for( int j = i + 1; j < nodes.size(); j++ ) {
                Node x = (Node) nodes.get( j );

                if( y.isDifferent( x ) ) {
                	ds = ds.union( y.getDifferenceDependency( x ), abox.doExplanation() );
                    continue;
                }

                // 1. if x is a nominal node (of lower level), then Merge(y, x)
                if( x.getNominalLevel() < y.getNominalLevel() )
                    pairs.add( new NodeMerge( y, x ) );
                // 2. if y is a nominal node or an ancestor of x, then Merge(x, y)
                else if( y.isNominal() )
                    pairs.add( new NodeMerge( x, y ) );
                // 3. if y is an ancestor of x, then Merge(x, y)
                // Note: y is an ancestor of x iff the max cardinality
                // on node merges the "node"'s parent y with "node"'s
                // child x
                else if( y.hasSuccessor( node ) )
                    pairs.add( new NodeMerge( x, y ) );
                // 4. else Merge(y, x)
                else
                    pairs.add( new NodeMerge( y, x ) );

                // if(y.isLeaf() && z.isLeaf())
                // pairs.add(new NodeMerge(y, z));
                // else if(y.isRoot() && z.isRoot()) {
                // if(z.isBnode())
                // pairs.add(new NodeMerge(z, y));
                // else
                // pairs.add(new NodeMerge(y, z));
                // }
                // else if(!y.isRoot() && /*!y.isLeaf() &&*/ !z.hasAncestor((Individual) y))
                // pairs.add(new NodeMerge(y, z));
                // else if(!z.isRoot() && /*!z.isLeaf() &&*/ !y.hasAncestor((Individual) z))
                // pairs.add(new NodeMerge(z, y));
                // else
                // log.warn("Cannot determine how to merge nodes " + y + " " + z);
            }
        }
        t.stop();
        return ds;
    }

    public void restore( Branch br ) {
        // Timers timers = abox.getKB().timers;
        // Timer timer = timers.startTimer("restore");
		//		System.out.println("!!!-- Entering restore to branch( " + br.getClass() + ") " + br.branch);
        abox.setBranch( br.branch );
        abox.setClash( null );
        abox.anonCount = br.anonCount;
        abox.rulesNotApplied = true;
        mergeList.clear();

        List nodeList = abox.getNodeNames();
        Map nodes = abox.getNodeMap();

        if( log.isDebugEnabled() ) {
            log.debug( "RESTORE: Branch " + br.branch );
            if( br.nodeCount < nodeList.size() )
                log.debug( "Remove nodes " + nodeList.subList( br.nodeCount, nodeList.size() ) );
        }

		if( PelletOptions.USE_COMPLETION_QUEUE ) {
			Set effected = abox.completionQueue.removeEffects( br.branch );

			//First remove all nodes from the node map that were added after this branch
			for( int i = br.nodeCount; i < nodeList.size(); i++ ) {
				ATermAppl next = (ATermAppl) nodeList.get( i );
				nodes.remove( next );
				effected.remove( next );
			}

			//Next restore remaining nodes that were effected during the branch after the one we are restoring too.
			//Currenlty, I'm tracking the effected nodes through the application of the completion rules.
			Iterator it = effected.iterator();
			while( it.hasNext() ) {
				ATerm x = (ATerm) it.next();
				Node node = abox.getNode( x );
				node.restore( br.branch );
			}
		}
		else {

			//Below is the old code	
        for( int i = 0; i < nodeList.size(); i++ ) {
            ATerm x = (ATerm) nodeList.get( i );

            Node node = abox.getNode( x );
			if( i >= br.nodeCount ) {
                nodes.remove( x );
			}
            // if(node.branch > br.branch) {
            // nodes.remove(x);
            // int lastIndex = nodeList.size() - 1;
            // nodeList.set(i, nodeList.get(lastIndex));
            // nodeList.remove(lastIndex);
            // i--;
            // }
			else {
                node.restore( br.branch );
        	}
			}
		}

		//clear the nodelist as well
        nodeList.subList( br.nodeCount, nodeList.size() ).clear();

		if( PelletOptions.USE_COMPLETION_QUEUE ) {
			//reset the queues
			abox.completionQueue.restore( br.branch );

			//This is new code that only fires all values for inds that are on the queue
			abox.completionQueue.init( CompletionQueue.ALLLIST );
			while( abox.completionQueue.hasNext( CompletionQueue.ALLLIST ) ) {
				QueueElement next = (QueueElement) abox.completionQueue
						.getNext( CompletionQueue.ALLLIST );
				applyAllValues( next );
				if( abox.isClosed() )
					break;
			}
		}
		else {
			//Below is the old code for firing the all values rule
	        for( Iterator i = abox.getIndIterator(); i.hasNext(); ) {
	            Individual ind = (Individual) i.next();
	            applyAllValues( ind );
	        }
        }

        if( log.isDebugEnabled() )
            abox.printTree();

        if( !abox.isClosed() )
            abox.validate();

//        System.out.println("Comp q");
//        abox.completionQueue.print();
        
        // timer.stop();
    }

    void addBranch( Branch newBranch ) {
        abox.getBranches().add( newBranch );

        if( newBranch.branch != abox.getBranches().size() )
            throw new RuntimeException( "Invalid branch created!" );

        completionTimer.check();
        
        //CHW - added for incremental deletion support
        if(this.supportsPseudoModelCompletion() && PelletOptions.USE_INCREMENTAL_DELETION){
        		abox.getKB().getDependencyIndex().addBranchAddDependency(newBranch);
        }
    }

    void printBlocked() {
        int blockedCount = 0;
        String blockedNodes = "";
        Iterator n = abox.getIndIterator();
        while( n.hasNext() ) {
            Individual node = (Individual) n.next();
            ATermAppl x = node.getName();

            if( blocking.isBlocked( node ) ) {
                blockedCount++;
                blockedNodes += x + " ";
            }
        }

        log.debug( "Blocked nodes " + blockedCount + " [" + blockedNodes + "]" );
    }

    void checkDatatypeCount( IndividualIterator it ) {
        timers.startTimer( "clashDatatype" );

        it.reset();
        while( it.hasNext() ) {
            Individual x = (Individual) it.next();

            checkDatatypeCount( x );

            if( abox.isClosed() )
                return;
        }
        timers.stopTimer( "clashDatatype" );
    }
    
    void checkDatatypeCount( QueueElement element ) {
        timers.startTimer( "clashDatatype" );
        
        Individual x = (Individual)abox.getNode(element.getNode());
        x = (Individual)x.getSame();
		if(x.isPruned() || ( !x.isChanged( Node.ALL ) && !x.isChanged( Node.MIN ) ))
			return;
		
		checkDatatypeCount( x );
    }

    void checkDatatypeCount( Individual x ) {
        if( !x.isChanged( Node.ALL ) && !x.isChanged( Node.MIN ) )
            return;

        // for DatatypeProperties we have to compute the maximum number of
        // successors it can have on the fly. This is because as we go on
        // with completion there can be more concepts in the form all(dp, X)
        // added to node label. so first for each datatype property collect
        // all the different allValuesFrom axioms together
        Map allValues = new HashMap();
        Map depends = new HashMap();
        for( Iterator i = x.getTypes( Node.ALL ).iterator(); i.hasNext(); ) {
            ATermAppl av = (ATermAppl) i.next();
            ATermAppl r = (ATermAppl) av.getArgument( 0 );
            ATermAppl c = (ATermAppl) av.getArgument( 1 );

            Role role = abox.getRole( r );
            if( !role.isDatatypeRole() )
                continue;

            DependencySet ds = (DependencySet) depends.get( r );
            
            if(!PelletOptions.MAINTAIN_COMPLETION_QUEUE && x.getDepends(av) == null)
        			continue;


            List ranges = (List) allValues.get( r );
            if( ranges == null ) {
                ranges = new ArrayList();
                ds = DependencySet.EMPTY;
            }

            if( ATermUtils.isAnd( c ) ) {
                ATermList types = (ATermList) c.getArgument( 0 );
                for( ; types.isEmpty(); types = types.getNext() )
                    ranges.add( types.getFirst() );
            }
            else
                ranges.add( c );

            ds = ds.union( x.getDepends( av ), abox.doExplanation() );

            allValues.put( r, ranges );
            depends.put( r, ds );
        }

        for( Iterator i = x.getTypes( Node.MIN ).iterator(); i.hasNext(); ) {
            // mc stores the current type (the current minCard restriction)
            ATermAppl mc = (ATermAppl) i.next();
            ATermAppl r = (ATermAppl) mc.getArgument( 0 );
            Role role = abox.getRole( r );
            ATermAppl range = role.getRange();

            if( !role.isDatatypeRole() || range == null )
                continue;
            
            if(!PelletOptions.MAINTAIN_COMPLETION_QUEUE && x.getDepends(mc) == null)
    				continue;


            List ranges = (List) allValues.get( r );
            if( ranges == null ) {
                ranges = new ArrayList();
                allValues.put( r, ranges );
                depends.put( r, DependencySet.INDEPENDENT );
            }

            ranges.add( range );
        }

        for( Iterator i = allValues.keySet().iterator(); i.hasNext(); ) {
            ATermAppl r = (ATermAppl) i.next();
            Role role = abox.getRole( r );
            List ranges = (List) allValues.get( r );

            ATermAppl[] dt = new ATermAppl[ranges.size()];
            ranges.toArray( dt );

            timers.startTimer( "getMaxCount" );
            int n = abox.getDatatypeReasoner().intersection( dt ).size();
            timers.stopTimer( "getMaxCount" );

            if( n == ValueSpace.INFINITE || n == Integer.MAX_VALUE )
                continue;

            boolean clash = x.checkMaxClash( ATermUtils
                .makeNormalizedMax( r, n, ATermUtils.TOP_LIT ), DependencySet.INDEPENDENT );
            if( clash )
                return;

            DependencySet dsEdges = x.hasDistinctRNeighborsForMax( role, n + 1, ATermUtils.TOP_LIT );

            if( dsEdges != null ) {
                DependencySet ds = (DependencySet) depends.get( r );
                ds = ds.union( dsEdges, abox.doExplanation() );

                if( log.isDebugEnabled() )
                    log.debug( "CLASH: literal restriction " + x + " has " + ranges
                        + " and more neighbors -> " + ds );

                abox.setClash( Clash.unexplained( x, ds ) );

                return;
            }
        }
    }

    public String toString() {
        String name = getClass().getName();
        int lastIndex = name.lastIndexOf( '.' );
        return name.substring( lastIndex + 1 );
    }
}