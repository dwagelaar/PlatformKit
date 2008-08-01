package org.mindswap.pellet.rete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mindswap.pellet.ABox;
import org.mindswap.pellet.Edge;
import org.mindswap.pellet.EdgeList;
import org.mindswap.pellet.Individual;
import org.mindswap.pellet.Literal;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;


public class Compiler {
	
	AlphaStore alphaNodeStore;
	BetaStore betaNodeStore;
	AlphaIndex alphaIndex;
	// do we need a  betaindex?
	public Compiler() {
		alphaNodeStore = new AlphaStore();
		betaNodeStore = new BetaStore();
		alphaIndex = new AlphaIndex();
		
	}
	
	public Compiler(Set facts) {
		
	}
	
	public Compiler compile(Collection rules) {
		
		//iterate over rules
		Iterator it = rules.iterator();
		while (it.hasNext()) {
			
			Rule rule = (Rule) it.next();
			AlphaStore alphaNodesOfRule = new AlphaStore();
			Iterator patternIt = rule.body.iterator();
			
			//iterate over the left hand side (rule body)
			//turn each triple into an alpha node, and add to AlphaStore
			while (patternIt.hasNext()) {
				Triple anodePattern = (Triple) patternIt.next();
				AlphaNode anode = makeAlphaNode(anodePattern);
				alphaNodesOfRule.addNode(anode);				
			}
			
			//sort them (body triples)
			alphaNodesOfRule.sort();
			alphaNodeStore.sort();
			
			
			
			int l = alphaNodesOfRule.nodes.size();
			
			//no body/assendants
			if (l==0) {
				System.err.println("Malformed Input");
			} 
			//1 assendant
			else if (l==1) {
				BetaNode beta1 = makeBetaNode((Node)alphaNodesOfRule.nodes.get(0), (Node)alphaNodesOfRule.nodes.get(0), false);
				AlphaNode a = (AlphaNode) alphaNodesOfRule.nodes.get(0);
				a.betaNodes = new ArrayList();
				a.betaNodes.add(beta1);
				beta1.rule = new RuleNode(rule);
				beta1.rule.betaNode = beta1;
				
				
			} 
			////2 body/assendants
			else  if (l==2){
				BetaNode beta1 = makeBetaNode((Node)alphaNodesOfRule.nodes.get(0), (Node)alphaNodesOfRule.nodes.get(1), false);
				AlphaNode a = (AlphaNode) alphaNodesOfRule.nodes.get(0);
				a.betaNodes = new ArrayList();
				a.betaNodes.add(beta1);
				
				AlphaNode b = (AlphaNode) alphaNodesOfRule.nodes.get(1);
				b.betaNodes = new ArrayList();
				b.betaNodes.add(beta1);
				
				beta1.rule = new RuleNode(rule);
				beta1.rule.betaNode = beta1;
				betaNodeStore.addNode(beta1);				
			} 
			//more than 2 assendants
			else {
				
				BetaNode beta1 = makeBetaNode((Node)alphaNodesOfRule.nodes.get(0), (Node)alphaNodesOfRule.nodes.get(1), true);
//				System.out.println("adding betanode:");
//				System.out.println(beta1);
				AlphaNode a = (AlphaNode) alphaNodesOfRule.nodes.get(0);
				a.betaNodes = new ArrayList();
				a.betaNodes.add(beta1);
				
				AlphaNode b = (AlphaNode) alphaNodesOfRule.nodes.get(1);
				b.betaNodes = new ArrayList();
				b.betaNodes.add(beta1);
				
				betaNodeStore.addNode(beta1);
				makeBetaNetwork(rule, beta1, alphaNodesOfRule.nodes.subList(2, alphaNodesOfRule.nodes.size()));
				
			}
		}
		return this;
		
		
		//result of method is that the AlphaStore has all the assendants of the bodies of the rules in it
	}
	
	
	public void makeBetaNetwork(Rule rule, BetaNode betaNode, List alphaNodeList) {
		if (alphaNodeList.size()==0) {
			betaNode.rule = new RuleNode(rule);
			betaNode.rule.betaNode = betaNode;
			
		} else {
			AlphaNode alpha = (AlphaNode) alphaNodeList.get(0);
			BetaNode betaChild = makeBetaNode(betaNode, alpha, true);
			

			
			betaChild.parents = new ArrayList();
			betaChild.parents.add(betaNode);
			
			betaNode.children = new ArrayList();
			betaNode.children.add(betaChild);
			ArrayList sharedJoinVars = (ArrayList) Utils.getSharedVars(betaNode, alpha);
			Collections.sort(sharedJoinVars);
			betaNode.svars = sharedJoinVars;
			
			List tmp = Utils.append(sharedJoinVars, betaNode.vars);
			
			
			betaNode.vars = Utils.removeDups(tmp);
			alpha.betaNodes = new ArrayList();
			alpha.betaNodes.add(betaChild);
			
			betaNodeStore.addNode(betaNode);
			betaNodeStore.addNode(betaChild);
			makeBetaNetwork(rule, betaChild, alphaNodeList.subList(1, alphaNodeList.size()));
			
		}
	}		
	
	public AlphaNode makeAlphaNode(Triple pattern) {
		AlphaNode a = new AlphaNode(pattern);
		alphaIndex.add(a);
		alphaNodeStore.addNode(a);
		return a;	        
	}
	
	public BetaNode makeBetaNode(Node node1, Node node2, boolean futureJoins) {
		List sharedVars = Utils.getSharedVars(node1, node2);
		Collections.sort(sharedVars);
		if (node1 instanceof AlphaNode) {
			node1.svars = sharedVars;
			
		}
		node2.svars = sharedVars;
		
		BetaNode b = new BetaNode(node1, node2);
		b.svars = sharedVars;
		return b;
		
	}
	
	public Set compileFacts(ABox abox) {
		Set result = new HashSet();
		//compile facts
		
		//get all the individuals
		Iterator i = abox.getIndIterator();
		while (i.hasNext()) {
			Individual ind = (Individual) i.next();			
			
			//only named individuals
			if (!ind.isNamedIndividual())
				continue;
			List atomic = ind.getTypes( org.mindswap.pellet.Node.ATOM );
			
			//loop through each type for the individual and and the 
			//individual and type as a fact to the results
			//if the type is independant & primative
			for(Iterator it = atomic.iterator(); it.hasNext();) {
				ATermAppl c = (ATermAppl) it.next();
				
				if( ind.getDepends( c ).isIndependent() ) {
					if( ATermUtils.isPrimitive( c ) ) {
						result.add(createFact(ind, c));
					}					
				}
			}
			
			//get the out edges - ie the properties going out from this 
			//individual (data properties have none)
			//add the out individuals aswell
			EdgeList edges = ind.getOutEdges();
			for (Iterator it = edges.iterator(); it.hasNext();) {
				Edge edge = (Edge) it.next();
				
				Individual from = edge.getFrom();
				// 
				
				if (edge.getTo() instanceof Individual) {
					
					Individual to= (Individual) edge.getTo();
					
					//only named, not annoymous
					if (!from.isNamedIndividual() || !to.isNamedIndividual() )
						continue;
					
					result.add(createFact(from, to, edge));
				}
				else if(edge.getTo() instanceof Literal) //added by luke
				{
					Literal to= (Literal) edge.getTo();
					
					//only named, not annoymous
					if (!from.isNamedIndividual() || !to.isNamedIndividual() )
						continue;
					
					result.add(createDataValueFact(from, to, edge));
					//System.out.println("get to: " + to + " edge " + edge );
					
				}
			}

		}
		return result;
	}
	
	private Fact createFact(Individual from, Individual to, Edge edge) {
		Constant subj = null, pred = null, obj = null;
		
		subj = new Constant(from.getNameStr());		
		pred = new Constant(edge.getRole().getName().toString()); 
		obj = new Constant(to.getNameStr());
		
		return new Fact(subj, pred, obj);
	}


	private Fact createDataValueFact(Individual from, Literal to, Edge edge) {
		Constant subj = null, pred = null, obj = null;
		
		subj = new Constant(from.getNameStr());		
		pred = new Constant(edge.getRole().getName().toString()); 
		obj = new Constant(to.toString());
		
		return new Fact(subj, pred, obj);
	}

	private Fact createFact(Individual ind, ATermAppl c) {
		Constant subj = null, obj = null;
		// System.out.println("str:" + ind);
		subj = new Constant(ind.getNameStr());		
		Constant predType = Constant.TYPE;
		obj = new Constant(c.getName());
		
		return new Fact(subj, predType, obj);
	}
	
	
}
