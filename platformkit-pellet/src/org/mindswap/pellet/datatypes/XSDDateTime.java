/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;


import java.math.BigDecimal;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.GenericIntervalList;
import org.mindswap.pellet.utils.Namespaces;
import org.mindswap.pellet.utils.NumberUtils;
import org.relaxng.datatype.DatatypeException;

import aterm.ATermAppl;

import com.sun.msv.datatype.xsd.DatatypeFactory;
import com.sun.msv.datatype.xsd.XSDatatype;
import com.sun.msv.datatype.xsd.datetime.BigTimeDurationValueType;
import com.sun.msv.datatype.xsd.datetime.IDateTimeValueType;
import com.sun.msv.datatype.xsd.datetime.ITimeDurationValueType;


/**
 * @author Evren Sirin
 */
public class XSDDateTime extends BaseXSDAtomicType implements AtomicDatatype, XSDAtomicType {	
    private static XSDatatype dt = null;
    private static IDateTimeValueType min = null;
    private static IDateTimeValueType max = null;

    static {
        try {
            dt = DatatypeFactory.getTypeByName( "dateTime" );
            min = (IDateTimeValueType) dt.createValue( "-9999-01-01T12:00:00", null );
            max = (IDateTimeValueType) dt.createValue( "9999-12-31T12:00:00", null );
        }
        catch( DatatypeException e ) {
            e.printStackTrace();
        }
    }

    private static final ValueSpace DATE_TIME_VALUE_SPACE = new DateTimeValueSpace();

    private static class DateTimeValueSpace extends AbstractDateTimeValueSpace implements ValueSpace {
        public DateTimeValueSpace() {
            super( min, max, dt );
        }

        //return the difference in Times
        public int count( Object start, Object end ) {
            long calendarStart = ((IDateTimeValueType) start).toCalendar().getTimeInMillis();
            long calendarEnd = ((IDateTimeValueType) end).toCalendar().getTimeInMillis();
            long diff = calendarStart - calendarEnd;

            if( diff > Integer.MAX_VALUE )
                return INFINITE;

            return (int) diff;
        }

        public Object succ( Object value, int n ) {
            BigDecimal bigN = new BigDecimal( String.valueOf( n ) );
            ITimeDurationValueType nSeconds =
                new BigTimeDurationValueType(
                    NumberUtils.INTEGER_ZERO, NumberUtils.INTEGER_ZERO, NumberUtils.INTEGER_ZERO,
                    NumberUtils.INTEGER_ZERO, NumberUtils.INTEGER_ZERO, bigN );
            IDateTimeValueType s = ((IDateTimeValueType)value).add( nSeconds );
            
            return s;
        }
    }

    public static XSDDateTime instance = new XSDDateTime( ATermUtils.makeTermAppl( Namespaces.XSD + "dateTime" ) );

    protected XSDDateTime( ATermAppl name ) {
        super( name, DATE_TIME_VALUE_SPACE );
    }

    public BaseXSDAtomicType create( GenericIntervalList intervals ) {
        XSDDateTime type = new XSDDateTime( null );
        type.values = intervals;

        return type;
    }

    public AtomicDatatype getPrimitiveType() {
        return instance;
    }
}
    