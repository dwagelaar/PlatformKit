package org.mindswap.pellet.tbox;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.tbox.impl.TBoxExpImpl;

/**
 * 
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: Clark & Parsia, LLC. <http://www.clarkparsia.com></p>
 *
 * @author Evren Sirin
 */
public class TBoxFactory {
	public static TBox createTBox( KnowledgeBase kb ) {
		return new TBoxExpImpl( kb );
	}
}
