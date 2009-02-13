/*
 * @(#)$Id: TimeType.java,v 1.16 2003/06/09 20:49:24 kk122374 Exp $
 *
 * Copyright 2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */
package com.sun.msv.datatype.xsd;

import java.util.Calendar;

import com.sun.msv.datatype.SerializationContext;
import com.sun.msv.datatype.xsd.datetime.BigDateTimeValueType;
import com.sun.msv.datatype.xsd.datetime.IDateTimeValueType;
import com.sun.msv.datatype.xsd.datetime.ISO8601Parser;

/**
 * "time" type.
 * 
 * type of the value object is {@link IDateTimeValueType}.
 * See http://www.w3.org/TR/xmlschema-2/#time for the spec
 * 
 * @author <a href="mailto:kohsuke.kawaguchi@eng.sun.com">Kohsuke KAWAGUCHI</a>
 */
public class TimeType extends DateTimeBaseType {
    
    public static final TimeType theInstance = new TimeType();
    private TimeType() { super("time"); }

    protected void runParserL( ISO8601Parser p ) throws Exception {
        p.timeTypeL();
    }

    protected IDateTimeValueType runParserV( ISO8601Parser p ) throws Exception {
        return p.timeTypeV();
    }
    
    public String convertToLexicalValue( Object value, SerializationContext context ) {
        if(!(value instanceof IDateTimeValueType))
            throw new IllegalArgumentException();
        
        BigDateTimeValueType bv = ((IDateTimeValueType)value).getBigValue();
        return    formatTwoDigits(bv.getHour())+":"+
                formatTwoDigits(bv.getMinute())+":"+
                formatSeconds(bv.getSecond())+
                formatTimeZone(bv.getTimeZone());
    }

    
    public String serializeJavaObject( Object value, SerializationContext context ) {
        if(!(value instanceof Calendar))    throw new IllegalArgumentException();
        Calendar cal = (Calendar)value;
        
        
        StringBuffer result = new StringBuffer();

        result.append(formatTwoDigits(cal.get(Calendar.HOUR_OF_DAY)));
        result.append(':');
        result.append(formatTwoDigits(cal.get(Calendar.MINUTE)));
        result.append(':');
        result.append(formatSeconds(cal));
        
        result.append(formatTimeZone(cal));
        
        return result.toString();
    }

    // serialization support
    private static final long serialVersionUID = 1;    
}