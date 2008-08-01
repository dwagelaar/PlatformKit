/*
 * @(#)$Id: FractionDigitsFacetTest.java,v 1.3 2003/06/09 20:50:19 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.datatype.xsd;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * tests FractionDigitsFacet.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class FractionDigitsFacetTest extends TestCase
{
    public FractionDigitsFacetTest( String name ) { super(name); }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(FractionDigitsFacetTest.class);
    }
    
    public void testCountScale()
    {
        assertEquals( 0, FractionDigitsFacet.countScale("5.000000000000") );
        assertEquals( 0, FractionDigitsFacet.countScale("-95") );
        assertEquals( 1, FractionDigitsFacet.countScale("5.9") );
        assertEquals( 1, FractionDigitsFacet.countScale("99925.900") );
        assertEquals( 5, FractionDigitsFacet.countScale("6.0000400") );
        assertEquals( 5, FractionDigitsFacet.countScale("6.0030400") );
    }
}
