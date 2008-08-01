/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import java.math.BigInteger;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.GenericIntervalList;
import org.mindswap.pellet.utils.Namespaces;
import org.mindswap.pellet.utils.NumberUtils;
import org.relaxng.datatype.DatatypeException;

import aterm.ATermAppl;

import com.sun.msv.datatype.xsd.DatatypeFactory;
import com.sun.msv.datatype.xsd.XSDatatype;
import com.sun.msv.datatype.xsd.datetime.BigDateTimeValueType;
import com.sun.msv.datatype.xsd.datetime.BigTimeDurationValueType;
import com.sun.msv.datatype.xsd.datetime.IDateTimeValueType;
import com.sun.msv.datatype.xsd.datetime.ITimeDurationValueType;

/**
 * @author kolovski
 */
public class XSDYear extends BaseXSDAtomicType implements AtomicDatatype, XSDAtomicType {    
    private static XSDatatype dt = null;
    private static IDateTimeValueType min = null;
    private static IDateTimeValueType max = null;

    static {
        try {
            dt = DatatypeFactory.getTypeByName( "gYear" );
            min = (IDateTimeValueType) dt.createValue( "-9999", null );
            max = (IDateTimeValueType) dt.createValue( "9999", null );
        }
        catch( DatatypeException e ) {
            e.printStackTrace();
        }
    }
    
    private static final ValueSpace YEAR_VALUE_SPACE = new YearValueSpace();
    
    private static class YearValueSpace extends AbstractDateTimeValueSpace implements ValueSpace {          
        public YearValueSpace() {
            super( min, max, dt );
        }

        //return the difference in Years
        public int count(Object start, Object end) {            
            BigDateTimeValueType calendarStart = ((IDateTimeValueType) start).getBigValue();          
            BigDateTimeValueType calendarEnd = ((IDateTimeValueType) end).getBigValue();      
            
            return calendarEnd.getYear().intValue() - calendarStart.getYear().intValue() + 1;           
        }       

        public Object succ( Object value, int n ) {
            BigInteger bigN = new BigInteger( String.valueOf( n ) );
            ITimeDurationValueType nYears =
                new BigTimeDurationValueType(
                    bigN, NumberUtils.INTEGER_ZERO, NumberUtils.INTEGER_ZERO,
                    NumberUtils.INTEGER_ZERO, NumberUtils.INTEGER_ZERO, NumberUtils.DECIMAL_ZERO );
            IDateTimeValueType s = ((IDateTimeValueType)value).add( nYears );
            
            return s;
        }
    }
    
    public static XSDYear instance = new XSDYear(ATermUtils.makeTermAppl(Namespaces.XSD + "gYear"));

    protected XSDYear(ATermAppl name) {
        super( name, YEAR_VALUE_SPACE );
    }

    public BaseXSDAtomicType create( GenericIntervalList intervals ) {
        XSDYear type = new XSDYear( null );
        type.values = intervals;

        return type;
    }

    public AtomicDatatype getPrimitiveType() {
        return instance;
    }   
}   