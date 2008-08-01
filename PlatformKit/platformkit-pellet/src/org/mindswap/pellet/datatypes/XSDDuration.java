/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;
import org.relaxng.datatype.DatatypeException;

import com.sun.msv.datatype.xsd.DatatypeFactory;
import com.sun.msv.datatype.xsd.XSDatatype;


/**
 * @author Evren Sirin
 */
public class XSDDuration extends BaseAtomicDatatype implements AtomicDatatype {
    private static XSDatatype dt = null;

    static {
        try {
            dt = DatatypeFactory.getTypeByName( "duration" );
        }
        catch( DatatypeException e ) {
            e.printStackTrace();
        }
    }
    
	public static XSDDuration instance = new XSDDuration();

	XSDDuration() {
		super(ATermUtils.makeTermAppl(Namespaces.XSD + "duration"));		
	}

	public AtomicDatatype getPrimitiveType() {
		return instance;
	}

	public boolean contains( Object value ) {
		return (value instanceof Object) && super.contains(value);
	}

    public Object getValue(String value, String datatypeURI) {
		return dt.createValue( value, null );
    }
	
}
