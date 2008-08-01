/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import java.net.URI;
import java.net.URISyntaxException;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;
import org.relaxng.datatype.DatatypeException;

import com.sun.msv.datatype.xsd.DatatypeFactory;
import com.sun.msv.datatype.xsd.XSDatatype;

import aterm.ATermAppl;



/**
 * @author Evren Sirin
 */
public class XSDAnyURI extends BaseAtomicDatatype implements AtomicDatatype {
    private static XSDatatype dt = null;

    static {
        try {
            dt = DatatypeFactory.getTypeByName( "anyURI" );
        }
        catch( DatatypeException e ) {
            e.printStackTrace();
        }
    }
    
	public static XSDAnyURI instance = new XSDAnyURI();

	XSDAnyURI() {
		super(ATermUtils.makeTermAppl(Namespaces.XSD + "anyURI"));
	}

	public AtomicDatatype getPrimitiveType() {
		return instance;
	}

	public Object getValue(String value, String datatypeURI) {
		try {
			return new URI(value.trim());
		} catch (URISyntaxException e) {
		    if(datatypeURI.equals(instance.name.getName())) {
		        DatatypeReasoner.log.warn( "Invalid xsd:anyURI value: '" + value + "'" );
                DatatypeReasoner.log.warn( e );
		    }
			
			return null;
		}		
	}
	
	public boolean contains(Object value) {
		return (value instanceof URI) && super.contains(value);
	}

    public boolean isFinite() {
        return true;
    }

    public ATermAppl getValue( int n ) {
//        System.out.println(instance.getURI() + " " + this);
        return ATermUtils.makeTypedLiteral( "http://test" + n, name.getName());
    }	
}
