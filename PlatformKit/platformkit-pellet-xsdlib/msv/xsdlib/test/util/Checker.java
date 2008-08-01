/*
 * @(#)$Id: Checker.java,v 1.2 2003/06/09 20:50:21 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package util;

/**
 * used with ResourceChecker.
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public interface Checker
{
    void check( String propertyName ) throws Exception;
}
