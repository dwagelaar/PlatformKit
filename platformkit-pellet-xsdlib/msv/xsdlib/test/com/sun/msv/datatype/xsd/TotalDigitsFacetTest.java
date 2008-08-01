/*
 * @(#)$Id: TotalDigitsFacetTest.java,v 1.3 2003/06/09 20:50:19 kk122374 Exp $
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
 * tests TotalDigitsFacet.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class TotalDigitsFacetTest extends TestCase
{
    public TotalDigitsFacetTest( String name ) { super(name); }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(TotalDigitsFacetTest.class);
    }
    
    public void testCountScale()
    {
        assertEquals( 1, TotalDigitsFacet.countPrecision("5") );
        assertEquals( 1, TotalDigitsFacet.countPrecision("500") );
        assertEquals( 1, TotalDigitsFacet.countPrecision("500000") );
        assertEquals( 1, TotalDigitsFacet.countPrecision("-500000.000000") );
        assertEquals( 2, TotalDigitsFacet.countPrecision("0.05") );
        assertEquals( 2, TotalDigitsFacet.countPrecision(".05") );
        assertEquals( 3, TotalDigitsFacet.countPrecision("-0.952") );
        assertEquals( 3, TotalDigitsFacet.countPrecision("-9.52") );
        assertEquals( 3, TotalDigitsFacet.countPrecision("-952") );
        assertEquals( 3, TotalDigitsFacet.countPrecision("-9520000") );
    }
}
