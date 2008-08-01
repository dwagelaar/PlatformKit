package org.mindswap.pellet.output;

import java.util.HashSet;
import java.util.Set;

import org.mindswap.pellet.taxonomy.Taxonomy;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

/**
 * <p>
 * Title: RDF/XML Taxonomy Printer
 * </p>
 * 
 * <p>
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * 
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 * 
 * @author Evren Sirin
 */
public class RDFXMLTaxonomyPrinter implements TaxonomyPrinter {
	final static String		OWL_EQUIVALENT_CLASS	= "owl:equivalentClass";
	final static String		RDFS_SUB_CLASS_OF		= "rdfs:subClassOf";
	final static String		RDF_TYPE				= "rdf:type";

	private Taxonomy		taxonomy;

	private OutputFormatter	out;
	
	private Set<ATermAppl> visited;

	public RDFXMLTaxonomyPrinter() {
	}

	public void print(Taxonomy taxonomy) {
		print( taxonomy, new OutputFormatter() );
	}

	public void print(Taxonomy taxonomy, OutputFormatter out) {
		this.taxonomy = taxonomy;
		this.out = out;

		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println();
		out.println("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" ");
		out.println("         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" " );
		out.println("         xmlns:owl=\"http://www.w3.org/2002/07/owl#\"> ");
		out.println();
		
		printTree();
		
		out.println();
		out.println("</rdf:RDF>");
		out.flush();
	}

	private void printTree() {
		visited = new HashSet<ATermAppl>();
		visited.add( ATermUtils.BOTTOM );

		printTree( ATermUtils.TOP );
		
		printTree( ATermUtils.BOTTOM );
		
		for( ATermAppl c : taxonomy.getClasses() ) 
			printTree( c );		
	}

	private void printTree( ATermAppl c ) {
		if( visited.contains( c ) )
			return;
		
		Set<ATermAppl> eqClasses = taxonomy.getEquivalents( c );
		
		visited.add( c );
		visited.addAll( eqClasses );

		printConceptDefinition( c, false );
		for( ATermAppl eq : eqClasses ) 
			printTriple( OWL_EQUIVALENT_CLASS, eq );	
		
		if( !c.equals( ATermUtils.BOTTOM ) ) {
			Set<Set<ATermAppl>> subClasses = taxonomy.getSupers( c, true );
			for( Set<ATermAppl> equivalenceSet : subClasses ) {
				ATermAppl subClass = equivalenceSet.iterator().next();
				
				printTriple( RDFS_SUB_CLASS_OF, subClass );
			}		
		}
		
		out.println( "</owl:Class>" );		

		for( ATermAppl eqClass : eqClasses ) {
			out.println();
			printConceptDefinition( eqClass, true );
		}			
		
		out.println();		
		
		Set<ATermAppl> instances = taxonomy.getInstances(c, true);
		for( ATermAppl instance : instances ) {
			if( ATermUtils.isBnode( instance ) )
				return;
			
			out.print( "<rdf:Description rdf:about=\"" );
			out.print( instance.getName() );
			out.println( "\">" );
			printTriple( RDF_TYPE, c );			
			out.println( "</rdf:Description>" );
		}
	}


	private void printTriple(String predicate, ATermAppl c2) {
		out.print( "   <" + predicate );
		out.print( " rdf:resource=\"" );
		printConcept( c2 );
		out.println( "\"/> " );
	}
	
	private void printConceptDefinition(ATermAppl c, boolean close) {
		out.print( "<owl:Class rdf:about=\"" );
		printConcept( c );
		if( close )
			out.println( "\"/> " );
		else
			out.println( "\"> " );
	}

	private void printConcept(ATermAppl c) {
		String uri = null;
		if( c.equals( ATermUtils.TOP ) )
			uri = "http://www.w3.org/2002/07/owl#Thing";
		else if( c.equals( ATermUtils.BOTTOM ) )
			uri = "http://www.w3.org/2002/07/owl#Nothing";
		else
			uri = c.getName();

		out.print( uri );
	}
	
	private void printIndividual(ATermAppl ind) {
		if( ATermUtils.isBnode( ind ) )
			return;
		
		out.print( "<rdf:Description rdf:about=\"" );
		out.print( ind.getName() );
		out.println( "\"> " );
		
		Set<Set<ATermAppl>> directTypes = taxonomy.getTypes( ind, true );
		for( Set<ATermAppl> equivalenceSet : directTypes ) {
			ATermAppl directType = equivalenceSet.iterator().next();
			
			printTriple( RDF_TYPE, directType );
		}
		
		out.println( "</rdf:Description>" );
	}
}