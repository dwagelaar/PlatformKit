/*
 * @(#)$Id: BigDateTimeValueTypeTest.java,v 1.3 2003/06/09 20:50:21 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.datatype.xsd.datetime;

import java.io.StringReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.msv.datatype.xsd.Comparator;

/**
 * tests BigDateTimeValueType.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class BigDateTimeValueTypeTest extends TestCase {    
    
    public BigDateTimeValueTypeTest(String testName) {
        super(testName);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(BigDateTimeValueTypeTest.class);
    }
    
    private ISO8601Parser getParser( String s ) throws Exception
    {
        return new ISO8601Parser(new StringReader(s));
    }
    
    /** Test of getBigValue method, of class com.sun.msv.datatype.datetime.BigDateTimeValueType. */
    public void testGetBigValue()  throws Exception
    {
        BigDateTimeValueType t = (BigDateTimeValueType)getParser("2000-01").yearMonthTypeV();
        assertEquals( t, t.getBigValue() );
    }
    
    /** Test of compare method, of class com.sun.msv.datatype.datetime.BigDateTimeValueType. */
    public void testCompare() throws Exception
    {
        // from examples of the spec
        int r;
        
        r = getParser("2000-01-15T00:00:00").dateTimeTypeV().compare(
            getParser("2000-02-15T00:00:00").dateTimeTypeV() );
        assertEquals( r, Comparator.LESS );
            
        r = getParser("2000-01-15T12:00:00" ).dateTimeTypeV().compare(
            getParser("2000-01-16T12:00:00Z").dateTimeTypeV() );
        assertEquals( r, Comparator.LESS );
            
        r = getParser("2000-01-01T12:00:00" ).dateTimeTypeV().compare(
            getParser("1999-12-31T23:00:00Z").dateTimeTypeV() );
        assertEquals( r, Comparator.UNDECIDABLE );
        
        r = getParser("2000-01-16T12:00:00" ).dateTimeTypeV().compare(
            getParser("2000-01-16T12:00:00Z").dateTimeTypeV() );
        assertEquals( r, Comparator.UNDECIDABLE );
            
        r = getParser("2000-01-16T00:00:00" ).dateTimeTypeV().compare(
            getParser("2000-01-16T12:00:00Z").dateTimeTypeV() );
        assertEquals( r, Comparator.UNDECIDABLE );
    }
    
    /** Test of normalize method, of class com.sun.msv.datatype.datetime.BigDateTimeValueType. */
    public void testNormalize() throws Exception
    {
        BigDateTimeValueType v;
        
        v = (BigDateTimeValueType)getParser("2000-03-04T23:00:00-03").dateTimeTypeV().normalize();
        
        // equals method compares two by calling normalize,
        // so actually this cannot be said as a testing.
        assertEquals( v,
            getParser("2000-03-05T02:00:00Z").dateTimeTypeV() );
    }
    
    /** Test of add method, of class com.sun.msv.datatype.datetime.BigDateTimeValueType. */
    public void testAdd() throws Exception
    {
        BigDateTimeValueType v;
        
        // from examples of Appendix.E of the spec.
        
        v = getParser("2000-01-12T12:13:14Z").dateTimeTypeV().add(
                getParser("P1Y3M5DT7H10M3.3S").durationTypeV() ).getBigValue();
        assertEquals( v, getParser("2001-04-17T19:23:17.3Z").dateTimeTypeV() );
        
        v = getParser("2000-01").yearMonthTypeV().add(
                getParser("-P3M").durationTypeV() ).getBigValue();
        assertEquals( v, getParser("1999-10").yearMonthTypeV() );
        
        v = getParser("2000-01-12-05").dateTypeV().add(
                getParser("PT33H").durationTypeV() ).getBigValue();
        assertEquals( v, getParser("2000-01-13-05").dateTypeV() );
    }
    
}
