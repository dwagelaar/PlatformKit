package org.mindswap.pellet.output;

import org.mindswap.pellet.taxonomy.Taxonomy;

/**
 * <p>
 * Title: Taxonomy Printer Interface
 * </p>
 * 
 * <p>
 * Description: Interface implemented by classes capable of printing taxonomies.
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
 * @author Mike Smith
 */
public interface TaxonomyPrinter {

	public void print(Taxonomy taxonomy);

	public void print(Taxonomy taxonomy, OutputFormatter out);
}
