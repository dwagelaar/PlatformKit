/*
 * @(#)$Id: UnionTypeTest.java,v 1.7 2003/06/09 20:50:19 kk122374 Exp $
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

import org.relaxng.datatype.DatatypeException;

import com.sun.msv.datatype.xsd.conformance.DummyContextProvider;

/**
 * tests UnionType.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class UnionTypeTest extends TestCase
{
    public UnionTypeTest( String name ) { super(name); }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(UnionTypeTest.class);
    }

    private UnionType createUnion( String newName,
        XSDatatype type1, XSDatatype type2, XSDatatype type3 )
            throws DatatypeException
    {
        return (UnionType)DatatypeFactory.deriveByUnion(
            "", newName, new XSDatatypeImpl[]{
                (XSDatatypeImpl)type1,
                (XSDatatypeImpl)type2,
                (XSDatatypeImpl)type3});
    }
    
    private UnionType createUnion( String newName,
        String type1, String type2, String type3 )
            throws DatatypeException
    {
        return createUnion( newName,
            DatatypeFactory.getTypeByName(type1),
            DatatypeFactory.getTypeByName(type2),
            DatatypeFactory.getTypeByName(type3) );
    }
    
    /** test get method */
    public void testIsAtomType() throws DatatypeException
    {
        // union is not an atom
        assertEquals( XSDatatype.VARIETY_UNION,
            createUnion( "test", "string", "integer", "QName" ).getVariety() );
    }
    
    /** test verify method */
    public void testVerify() throws DatatypeException
    {
        // this test is naive, and we need further systematic testing.
        // but better something than nothing.
        XSDatatype u = createUnion(null,"integer","QName","gYearMonth");
        
        assertTrue( u.isValid("1520",DummyContextProvider.theInstance) );
        assertTrue( u.isValid("foo:role",DummyContextProvider.theInstance) );
        assertTrue( u.isValid("2000-05",DummyContextProvider.theInstance) );
    }
    
    /** test convertToObject method */
    public void testConvertToObject() throws DatatypeException
    {
        XSDatatype tf = DatatypeFactory.getTypeByName("float");
        XSDatatype td = DatatypeFactory.getTypeByName("date");
        XSDatatype th = DatatypeFactory.getTypeByName("hexBinary");
        
        XSDatatype tu = createUnion("myTest", tf, td, th );
        
        assertEquals(
            tu.createValue("2.000",DummyContextProvider.theInstance),
            tf.createValue("2.000",DummyContextProvider.theInstance) );
        assertEquals(
            tu.createValue("2001-02-20",DummyContextProvider.theInstance),
            td.createValue("2001-02-20",DummyContextProvider.theInstance) );
        assertEquals(
            tu.createValue("1f5280",DummyContextProvider.theInstance),
            th.createValue("1F5280",DummyContextProvider.theInstance) );
    }
}
