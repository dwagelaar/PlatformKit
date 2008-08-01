package org.mindswap.pellet.taxonomy;


import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.utils.progress.ProgressMonitor;

import aterm.ATermAppl;

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
public interface TaxonomyBuilder {
	public void setKB( KnowledgeBase kb );
	
	public void setProgressMonitor(ProgressMonitor monitor);
	
	/**
	 * Classify the KB.
	 */
	public Taxonomy classify();

	public void classify(ATermAppl c);
	
	/**
	 * Realize the KB by finding the instances of each class.
	 */
	public Taxonomy realize();
}
