/*
 * @(#)$Id: ChoiceTestPattern.java,v 1.4 2003/06/09 20:50:20 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.datatype.xsd.conformance;

import org.relaxng.datatype.DatatypeException;

import com.sun.msv.datatype.xsd.TypeIncubator;

/**
 * choose one from child pattern
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
class ChoiceTestPattern implements TestPattern
{
    private final TestPattern[] children;
    private int idx=0;

    public ChoiceTestPattern( TestPattern[] children )
    {
        this.children = children;
        reset();
    }

    /** returns the number of test cases to be generated */
    public long totalCases()
    {
        int result=0;
        for( int i=0; i<children.length; i++ )
            // every pattern comes with empty. So we have to remove it.
            result += children[i].totalCases();
        return result;
    }

    /** restart generating test cases */
    public void reset()
    {
        for( int i=0; i<children.length; i++ )
            children[i].reset();
        idx=0;
    }

    /** get the current test case */
    public String get( TypeIncubator ti ) throws DatatypeException
    {
        return children[idx].get(ti);
    }

    /** generate next test case */
    public void next()
    {
        int prev = idx;

        children[idx].next();

        do
        {
            idx = (idx+1)%children.length;
        }while(!children[idx].hasMore() && idx!=prev);

        if(!children[idx].hasMore())
        {// all patterns of children have enumerated
            idx=-1;    // finish iterating test cases.
        }
    }

    public boolean hasMore() { return idx!=-1; }
}
