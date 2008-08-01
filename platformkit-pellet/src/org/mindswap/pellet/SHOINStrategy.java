/*
 * Created on Jul 23, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mindswap.pellet;


/**
 * @author Evren Sirin
 */
public class SHOINStrategy extends SHOIQStrategy {
	public SHOINStrategy(ABox abox) {
		super( abox );
	}

	protected void applyChooseRule(IndividualIterator i) {
		i.reset();
	}

//	void applyAllValues(Individual x, ATermAppl av, DependencySet ds) {
//		Role s = abox.getRole( av.getArgument( 0 ) );
//		ATermAppl c = (ATermAppl) av.getArgument( 1 );
//
//		EdgeList edges = x.getRNeighborEdges( s );
//		for( int e = 0; e < edges.size(); e++ ) {
//			Edge edgeToY = edges.edgeAt( e );
//			Node y = edgeToY.getNeighbor( x );
//			DependencySet finalDS = ds.union( edgeToY.getDepends() );
//			
//			applyAllValues( x, s, y, c, finalDS );
//
//			if( x.isMerged() )
//				return;
//		}
//
//		if( !s.isSimple() ) {
//			Set transitiveSubRoles = s.getTransitiveSubRoles();
//			for( Iterator it = transitiveSubRoles.iterator(); it.hasNext(); ) {
//				Role r = (Role) it.next();
//				ATermAppl allRC = ATermUtils.makeAllValues( r.getName(), c );
//
//				edges = x.getRNeighborEdges( r );
//				for( int e = 0; e < edges.size(); e++ ) {
//					Edge edgeToY = edges.edgeAt( e );
//					Node y = edgeToY.getNeighbor( x );
//					DependencySet finalDS = ds.union( edgeToY.getDepends() );
//					
//					applyAllValues( x, r, y, allRC, finalDS );
//
//					if( x.isMerged() )
//						return;
//				}
//			}
//		}
//	}
//
//	void applyAllValues( Individual subj, Role pred, Node obj, DependencySet ds) {
//		List allValues = subj.getTypes( Node.ALL );
//		Iterator i = allValues.iterator();
//		while( i.hasNext() ) {
//			ATermAppl av = (ATermAppl) i.next();
//
//			Role s = abox.getRole( av.getArgument( 0 ) );
//			if( pred.isSubRoleOf( s ) ) {
//				ATermAppl c = (ATermAppl) av.getArgument( 1 );
//				DependencySet finalDS = ds.union(  subj.getDepends( av ) );
//
//				applyAllValues( subj, s, obj, c, finalDS );
//
//				if( s.isTransitive() ) {
//					ATermAppl allRC = ATermUtils.makeAllValues( s.getName(), c );
//					finalDS = ds.union( subj.getDepends( av ) );
//					
//					applyAllValues( subj, s, obj, allRC, ds );
//				}
//			}
//
//			// if there are self links through transitive properties restart
//			if( subj.isChanged( Node.ALL ) ) {
//				i = allValues.iterator();
//				subj.setChanged( Node.ALL, false );
//			}
//		}
//
//		if( pred.isObjectRole() ) {
//			Individual y = (Individual) obj;
//			pred = pred.getInverse();
//			allValues = y.getTypes( Node.ALL );
//			i = allValues.iterator();
//			while( i.hasNext() ) {
//				// if there are self links through transitive properties restart
//				if( subj.isChanged( Node.ALL ) ) {
//					i = allValues.iterator();
//					subj.setChanged( Node.ALL, false );
//				}
//
//				ATermAppl av = (ATermAppl) i.next();
//
//				Role s = abox.getRole( av.getArgument( 0 ) );
//				if( pred.isSubRoleOf( s ) ) {
//					ATermAppl c = (ATermAppl) av.getArgument( 1 );
//					DependencySet finalDS = ds.union( y.getDepends( av ) );
//					
//					applyAllValues( y, s, subj, c, finalDS );
//
//					if( pred.isTransitive() ) {
//						ATermAppl allRC = ATermUtils.makeAllValues( pred.getName(), c );
//
//						applyAllValues( y, pred, subj, allRC, ds );
//					}
//				}
//			}
//		}
//	}
}
