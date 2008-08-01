/*
 * Created on Oct 29, 2006
 */
package org.mindswap.pellet.datatypes;

import com.sun.msv.datatype.xsd.XSDatatype;
import com.sun.msv.datatype.xsd.datetime.IDateTimeValueType;

public abstract class AbstractDateTimeValueSpace extends AbstractValueSpace implements ValueSpace {
    private XSDatatype dt;
    
    public AbstractDateTimeValueSpace( IDateTimeValueType minInf, IDateTimeValueType maxInf, XSDatatype dt ) {
        super( minInf, null, maxInf, false );
        
        this.dt = dt;
    }

    public int compare( Object a, Object b ) {
        return ((IDateTimeValueType) a).compare((IDateTimeValueType) b);
    }

    public boolean isValid( Object value ) {
        return (value instanceof IDateTimeValueType);
    }

    public Object getValue( String value ) {
        return (IDateTimeValueType) dt.createValue( value, null );
    }        

    public String getLexicalForm( Object value ) {
        return dt.convertToLexicalValue( value, null );
    }

}
