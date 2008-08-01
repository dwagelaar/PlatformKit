package org.mindswap.pellet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.rete.Constant;
import org.mindswap.pellet.rete.Fact;
import org.mindswap.pellet.rete.Interpreter;
import org.mindswap.pellet.rete.Rule;
import org.mindswap.pellet.rete.Term;
import org.mindswap.pellet.rete.Triple;
import org.mindswap.pellet.rete.Variable;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.QNameProvider;
import org.mindswap.pellet.utils.Timer;
import org.mindswap.pellet.utils.URIUtils;
import org.semanticweb.owl.model.OWLException;

import aterm.ATermAppl;

public class RuleStrategy extends SHOIQStrategy {

	public RuleStrategy(ABox abox) {
		super( abox );
	}

	public List getVars(Rule rule) throws OWLException {
		Set vars = new HashSet();

		//count the vars in the antecedents only (datalog safety)
		Iterator ants = rule.body.iterator();
		while( ants.hasNext() ) {
			Triple triple = (Triple) ants.next();
			List tripleVars = triple.getVars();

			vars.addAll( tripleVars );
		}

		return new ArrayList( vars );
	}

	public void applyRULERule() {

		HashMap bindings = new HashMap();

		//		OWLRule rule = null;
		// go through the rule and create the aterms    	 
		Iterator rulesIterator = this.abox.getKB().getRules().iterator();
		while( rulesIterator.hasNext() ) {

			Rule rule = (Rule) rulesIterator.next();

			try {
				List vars = getVars( rule );
				//create a binding

				// before enumerating the bindings for a particular rule, 
				// we can eliminate some of the individuals 
				// if a rule is A(x) ^ B(x,y) -> D(x) and for some ind. a, a:D,
				// then no need to generate any bindings including ind. a

				//find eligible individuals for this rule
				total = 0;
				findBinding( 0, bindings, vars, rule );

				if( log.isDebugEnabled() ) {
					log.debug( "total bindings:" + total );
					log.debug( "branches:" + abox.getBranch() );
				}
			} catch( OWLException e ) {
				e.printStackTrace();

			}

		}

	}

	int	total	= 0;

	//is it legal to bind two variables to the same individual?    
	//let's not do it for now
	private void findBinding(int current, HashMap bindings, List vars, Rule rule)
			throws OWLException {
		Individual ind = null;
		if( current < vars.size() ) {
			IndividualIterator i = abox.getIndIterator();

			while( i.hasNext() ) {

				ind = (Individual) i.next();

				if( !ind.isNamedIndividual() )
					continue; // || bindings.containsValue(ind)) continue;

				/* check whether ind trivially satisfies rule */
				/* ind is used in place of current var */

				//				
				//				if (triviallySatisfied(ind, rule, (URI) vars.get(current), bindings)) {
				////				System.out.println("TRIVIALLY!");
				//				continue;
				//				}
				bindings.put( vars.get( current ), ind );
				if( triviallySatisfiedAllBindings( bindings, rule ) ) {

					bindings.remove( vars.get( current ) );
					continue;
				}

				findBinding( current + 1, bindings, vars, rule );
				bindings.remove( vars.get( current ) );

			}

		}
		else {

			// found a binding
			total++;
			if( log.isDebugEnabled() ) {
				Iterator keys = bindings.keySet().iterator();
				while( keys.hasNext() ) {
					Object k = keys.next();
					log.debug( "key:" + k + " value:" + bindings.get( k ) + "-" );
				}
				log.debug( "total:" + total );
			}
			if( !abox.isClosed() ) {
				createDisjunctionsFromBinding( bindings, rule );
			}

		}
	}

	private boolean triviallySatisfiedAllBindings(HashMap bindings, Rule rule) throws OWLException {

		Triple head = (Triple) rule.head.iterator().next();
		Term pred = head.getPred();
		if( pred.equals( Constant.TYPE ) ) {
			// assumption: named class in the head
			// convert class name to an ATerm
			ATermAppl c = term( head.getObj().toString() );

			// get the pellet individual object from the rule head atom
			Individual ind = getIndividual( head.getSubj(), bindings );

			// if the individual has the type already, disjunction is trivially
			// satisfied
			if( ind != null && ind.hasType( c ) )
				return true;

		}
		else if( pred.equals( Constant.SAME_AS ) ) {
			List inds = getIndividuals( head, bindings );

			// there should be exactly two inds in the list
			if( inds.size() == 2 ) {
				ATermAppl sam = ATermUtils.makeValue( ((Individual) inds.get( 1 )).getTerm() );

				if( ((Individual) inds.get( 0 )).hasType( sam ) ) {
					return true;
				}
			}
		}
		else if( pred.equals( Constant.DIFF_FROM ) ) {

			List inds = getIndividuals( head, bindings );

			// TODO: we should check for isDifferent
			// there should be exactly two inds in the list
			if( inds.size() == 2 ) {
				ATermAppl dif = ATermUtils.makeNot( ATermUtils.makeValue( ((Individual) inds
						.get( 1 )).getTerm() ) );

				if( ((Individual) inds.get( 0 )).hasType( dif ) ) {
					return true;
				}
			}
		}
		else {
			List inds = getIndividuals( head, bindings );

			// there should be exactly two inds in the list
			if( inds.size() == 2 ) {
				ATermAppl p = term( head.getPred().toString() );

				ATermAppl notO = ATermUtils.negate( ATermUtils.makeValue( ((Individual) inds
						.get( 1 )).getTerm() ) );
				ATermAppl notAllPnotO = ATermUtils.negate( ATermUtils.makeAllValues( p, notO ) );

				if( ((Individual) inds.get( 0 )).hasType( notAllPnotO ) ) {
					return true;
				}
			}
		}

		Iterator ants = rule.body.iterator();
		while( ants.hasNext() ) {
			Triple atom = (Triple) ants.next();
			pred = atom.getPred();
			if( pred.equals( Constant.TYPE ) ) {
				Individual ind = getIndividual( atom.getSubj(), bindings );
				ATermAppl c = term( atom.getObj().toString() );
				ATermAppl notC = ATermUtils.negate( c );

				if( ind != null && ind.hasType( notC ) )
					return true;

			}
			else if( pred.equals( Constant.SAME_AS ) ) {
				List inds = getIndividuals( atom, bindings );

				//there should be exactly two inds in the list 
				if( inds.size() == 2 ) {

					ATermAppl dif = ATermUtils.makeNot( ATermUtils.makeValue( ((Individual) inds
							.get( 1 )).getTerm() ) );

					if( ((Individual) inds.get( 0 )).hasType( dif ) ) {
						return true;
					}
				}
			}
			else if( pred.equals( Constant.DIFF_FROM ) ) {
				List inds = getIndividuals( atom, bindings );

				//there should be exactly two inds in the list 
				if( inds.size() == 2 ) {

					ATermAppl sam = ATermUtils.makeValue( ((Individual) inds.get( 1 )).getTerm() );

					if( ((Individual) inds.get( 0 )).hasType( sam ) ) {
						return true;
					}
				}
			}
			else {
				//return a list of inds, only two entries: [subject, object]
				List inds = getIndividuals( atom, bindings );

				//there should be exactly two inds in the list 
				if( inds.size() == 2 ) {
					ATermAppl p = term( pred.toString() );
					//you want to add the value forall(p, not({o})) to s

					ATermAppl notO = ATermUtils.negate( ATermUtils.makeValue( ((Individual) inds
							.get( 1 )).getTerm() ) );
					ATermAppl allPNotO = ATermUtils.makeAllValues( p, notO );

					if( ((Individual) inds.get( 0 )).hasType( allPNotO ) ) {
						return true;
					}
				}
			}

		}
		return false;
	}

	private Individual getIndividual(Term term, Map bindings) {
		Individual ind = (term instanceof Variable) 
			? (Individual) bindings.get( term ) 
			: abox.getIndividual( term( ((Constant) term).getValue() ) );

		return ind;
	}

	private void addIndividual(Term term, Map bindings, List inds) {
		Individual ind = getIndividual( term, bindings );

		if( ind != null )
			inds.add( ind );
	}

	private List getIndividuals(Triple triple, Map bindings) {
		List inds = new ArrayList();

		addIndividual( triple.getSubj(), bindings, inds );

		if( !triple.getPred().equals( Constant.TYPE ) )
			addIndividual( triple.getObj(), bindings, inds );

		return inds;
	}

	private void createDisjunctionsFromBinding(Map bindings, Rule rule) throws OWLException {
		ATermAppl disjunction = null;
		ATermAppl[] disj = new ATermAppl[rule.body.size() + 1];
		ATermAppl[] inds = new ATermAppl[rule.body.size() + 1];
		int index = 0;

		//head first
		Triple head = (Triple) rule.head.iterator().next();
		Term pred = head.getPred();
		if( pred.equals( Constant.TYPE ) ) {
			ATermAppl c = term( head.getObj().toString() );

			Individual ind = getIndividual( head.getSubj(), bindings );

			disj[index] = c;
			inds[index] = ind.getName();
			index++;

			if( disjunction == null )
				disjunction = c;
			else
				disjunction = ATermUtils.makeOr( disjunction, c );

		}
		else if( pred.equals( Constant.SAME_AS ) ) {
			List eqInds = getIndividuals( head, bindings );
			Individual s1 = (Individual) eqInds.get( 0 );
			Individual s2 = (Individual) eqInds.get( 1 );

			ATermAppl sam = ATermUtils.makeValue( s2.getTerm() );
			disj[index] = sam;
			inds[index] = s1.getName();
			index++;
			if( disjunction == null )
				disjunction = sam;
			else
				disjunction = ATermUtils.makeOr( disjunction, sam );
		}
		else if( pred.equals( Constant.DIFF_FROM ) ) {
			List ineqInds = getIndividuals( head, bindings );
			Individual s1 = (Individual) ineqInds.get( 0 );
			Individual s2 = (Individual) ineqInds.get( 1 );

			ATermAppl dif = ATermUtils.makeNot( ATermUtils.makeValue( s2.getTerm() ) );
			disj[index] = dif;
			inds[index] = s1.getName();
			index++;
			if( disjunction == null )
				disjunction = dif;
			else
				disjunction = ATermUtils.makeOr( disjunction, dif );
		}
		else {
			List propertyInds = getIndividuals( head, bindings );
			Individual s = (Individual) propertyInds.get( 0 );
			Individual o = (Individual) propertyInds.get( 1 );

			ATermAppl p = term( pred.toString() );

			ATermAppl notO = ATermUtils.negate( ATermUtils.makeValue( o.getTerm() ) );
			ATermAppl notAllPnotO = ATermUtils.negate( ATermUtils.makeAllValues( p, notO ) );

			disj[index] = notAllPnotO;
			inds[index] = s.getName();
			index++;
			if( disjunction == null )
				disjunction = notAllPnotO;
			else
				disjunction = ATermUtils.makeOr( disjunction, notAllPnotO );
		}

		Iterator ants = rule.body.iterator();
		while( ants.hasNext() ) {

			Triple atom = (Triple) ants.next();
			pred = atom.getPred();
			if( pred.equals( Constant.TYPE ) ) {
				ATermAppl c = term( atom.getObj().toString() );
				ATermAppl notC = ATermUtils.negate( c );

				Individual ind = getIndividual( atom.getSubj(), bindings );

				disj[index] = notC;
				inds[index++] = ind.getName();

				if( disjunction == null )
					disjunction = notC;
				else
					disjunction = ATermUtils.makeOr( disjunction, notC );

			}
			else if( pred.equals( Constant.SAME_AS ) ) {

				List eqInds = getIndividuals( atom, bindings );
				Individual s1 = (Individual) eqInds.get( 0 );
				Individual s2 = (Individual) eqInds.get( 1 );

				ATermAppl dif = ATermUtils.makeNot( ATermUtils.makeValue( s2.getTerm() ) );
				disj[index] = dif;
				inds[index] = s1.getName();
				index++;
				if( disjunction == null )
					disjunction = dif;
				else
					disjunction = ATermUtils.makeOr( disjunction, dif );
			}
			else if( pred.equals( Constant.DIFF_FROM ) ) {
				List ineqInds = getIndividuals( atom, bindings );
				Individual s1 = (Individual) ineqInds.get( 0 );
				Individual s2 = (Individual) ineqInds.get( 1 );

				ATermAppl sam = ATermUtils.makeValue( s2.getTerm() );
				disj[index] = sam;
				inds[index] = s1.getName();
				index++;
				if( disjunction == null )
					disjunction = sam;
				else
					disjunction = ATermUtils.makeOr( disjunction, sam );
			}
			else {
				List propertyInds = getIndividuals( atom, bindings );
				Individual s = (Individual) propertyInds.get( 0 );
				Individual o = (Individual) propertyInds.get( 1 );

				ATermAppl p = term( pred.toString() );

				ATermAppl notO = ATermUtils.negate( ATermUtils.makeValue( o.getTerm() ) );
				ATermAppl allPNotO = ATermUtils.makeAllValues( p, notO );

				//				if (s.hasType(allPNotO)) {
				//				triviallySatisfied = true;
				//				} else { 			
				disj[index] = allPNotO;
				inds[index] = s.getName();
				index++;
				if( disjunction == null )
					disjunction = allPNotO;
				else
					disjunction = ATermUtils.makeOr( disjunction, allPNotO );
				//				}				
			}

		}
		
		disjunction = ATermUtils.makeOr( ATermUtils.makeList( disj ) );

		if( !abox.isClosed() ) {
			//create a ruleBranch with a list of inds and corresponding disjuncts				
			RuleBranch r = new RuleBranch( abox, this, abox.getIndividual( inds[0] ), inds, disjunction,					
					new DependencySet( abox.getBranch() ), disj );
			addBranch( r );
			r.tryBranch();
		}
	}

	public ATermAppl term(String s) {
		if( PelletOptions.USE_LOCAL_NAME )
			s = URIUtils.getLocalName( s );
		else if( PelletOptions.USE_QNAME )
			s = qnames.shortForm( s );

		return ATermUtils.makeTermAppl( s );
	}

	public static QNameProvider	qnames	= new QNameProvider();

	ABox complete() {
		Timer t;

		completionTimer.start();

		Expressivity expressivity = abox.getKB().getExpressivity();
		boolean fullDatatypeReasoning = PelletOptions.USE_FULL_DATATYPE_REASONING
				&& (expressivity.hasCardinalityD() || expressivity.hasKeys());

		initialize();

		//run the RETE once when the rules are not applied 
		if( !abox.ranRete && abox.rulesNotApplied ) {
			// initialize and run the rete 
			Interpreter interp = new Interpreter();

			try {
				Collection rules = abox.getKB().getRules();
				interp.rete.compile( rules );
			} catch( Exception e ) {
				System.err.println( "Exception while compiling rules!" );
				System.err.println( e );
			}

			Set facts = interp.rete.compileFacts( abox );

			interp.addFacts( facts, true );
			interp.run();

			if( log.isDebugEnabled() )
				log.debug( interp.inferredFacts.size() + " inferred fact(s)" );

			//need to add the inferred facts back to the tableau 
			Iterator it = interp.inferredFacts.iterator();
			DependencySet ds = DependencySet.INDEPENDENT;
			while( it.hasNext() ) {
				Fact f = (Fact) it.next();

				if( f.getPred().equals( Constant.TYPE ) ) {
					// add a type assertion for the individual
					//TODO: base the rete on aterms, too  - avoid this conversion bs
					Individual ind = abox.getIndividual( ATermUtils.makeTermAppl( f.getSubj()
							.toString() ) );
					ATermAppl type = ATermUtils.makeTermAppl( f.getObj().toString() );
					ind.addType( type, ds );
				}
				else {
					// add code for inferring roles, too
					// TODO:test this
					Individual from = abox.getIndividual( ATermUtils.makeTermAppl( f.getSubj()
							.toString() ) );
					Individual to = abox.getIndividual( ATermUtils.makeTermAppl( f.getObj()
							.toString() ) );

					Role r = abox.getRole( ATermUtils.makeTermAppl( f.getPred().toString() ) );
					addEdge( from, r, to, ds );
				}

			}
			abox.ranRete = true;
		}

		while( !abox.isComplete() ) {
			while( abox.changed && !abox.isClosed() ) {
				completionTimer.check();

				abox.changed = false;

				if( log.isDebugEnabled() ) {
					log.debug( "Branch: " + abox.getBranch() + ", Depth: " + abox.treeDepth
							+ ", Size: " + abox.getNodes().size() + ", Mem: "
							+ (Runtime.getRuntime().freeMemory() / 1000) + "kb" );
					abox.validate();
					//					printBlocked();
					abox.printTree();
				}

				IndividualIterator i = abox.getIndIterator();

				if( !PelletOptions.USE_PSEUDO_NOMINALS ) {
					t = timers.startTimer( "rule-nominal" );
					applyNominalRule( i );
					t.stop();
					if( abox.isClosed() )
						break;
				}

				t = timers.startTimer( "rule-guess" );
				applyGuessingRule( i );
				t.stop();
				if( abox.isClosed() )
					break;

				t = timers.startTimer( "rule-max" );
				applyMaxRule( i );
				t.stop();
				if( abox.isClosed() )
					break;

				if( fullDatatypeReasoning ) {
					t = timers.startTimer( "check-dt-count" );
					checkDatatypeCount( i );
					t.stop();
					if( abox.isClosed() )
						break;

					t = timers.startTimer( "rule-lit" );
					applyLiteralRule();
					t.stop();
					if( abox.isClosed() )
						break;
				}

				t = timers.startTimer( "rule-unfold" );
				applyUnfoldingRule( i );
				t.stop();
				if( abox.isClosed() )
					break;

				t = timers.startTimer( "rule-disj" );
				applyDisjunctionRule( i );
				t.stop();
				if( abox.isClosed() )
					break;

				t = timers.startTimer( "rule-some" );
				applySomeValuesRule( i );
				t.stop();
				if( abox.isClosed() )
					break;

				t = timers.startTimer( "rule-min" );
				applyMinRule( i );
				t.stop();
				if( abox.isClosed() )
					break;

				if( log.isDebugEnabled() ) {
					log.debug( "Applying RULE rule at branch:" + abox.getBranch() );
				}
				if( abox.rulesNotApplied ) {
					abox.rulesNotApplied = false;
					applyRULERule();

				}
				if( abox.isClosed() )
					break;

			}

			if( abox.isClosed() ) {
				if( log.isDebugEnabled() )
					log.debug( "Clash at Branch (" + abox.getBranch() + ") " + abox.getClash() );

				if( backtrack() )
					abox.setClash( null );
				else
					abox.setComplete( true );
			}
			else {
				if( PelletOptions.SATURATE_TABLEAU ) {
					Branch unexploredBranch = null;
					for( int i = abox.getBranches().size() - 1; i >= 0; i-- ) {
						unexploredBranch = (Branch) abox.getBranches().get( i );
						unexploredBranch.tryNext++;
						if( unexploredBranch.tryNext < unexploredBranch.tryCount ) {
							restore( unexploredBranch );
							System.out.println( "restoring branch " + unexploredBranch.branch
									+ " tryNext = " + unexploredBranch.tryNext + " tryCount = "
									+ unexploredBranch.tryCount );
							unexploredBranch.tryNext();
							break;
						}
						else {
							System.out.println( "removing branch " + unexploredBranch.branch );
							abox.getBranches().remove( i );
							unexploredBranch = null;
						}
					}
					if( unexploredBranch == null ) {
						abox.setComplete( true );
					}
				}
				else
					abox.setComplete( true );
			}
		}

		completionTimer.stop();

		return abox;
	}
}
