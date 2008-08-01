/*
 * @(#)$Id: DataTypeImplTest.java,v 1.3 2003/06/09 20:50:19 kk122374 Exp $
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
import util.Checker;
import util.ResourceChecker;

/**
 * tests DataTypeImpl.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class DataTypeImplTest extends TestCase
{
    public DataTypeImplTest( String name ) { super(name); }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(DataTypeImplTest.class);
    }
    
    /** tests the existence of all messages */
    public void testMessages() throws Exception {
        ResourceChecker.check(
            XSDatatypeImpl.class,
            "",
            new Checker(){
                public void check( String propertyName ) {
                    // if the specified property doesn't exist, this will throw an error
                    System.out.println(
                        XSDatatypeImpl.localize(propertyName,new Object[]{"@@@","@@@","@@@","@@@","@@@"}));
                }
            });
    }
}
