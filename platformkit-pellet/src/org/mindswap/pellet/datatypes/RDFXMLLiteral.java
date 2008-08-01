/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;


/**
 * @author Evren Sirin
 */
public class RDFXMLLiteral extends BaseAtomicDatatype implements AtomicDatatype {
	public static final RDFXMLLiteral instance = new RDFXMLLiteral();

	class XMLValue {
		String value;
		
		XMLValue(String value) {
			this.value = value;
		}
		
		public int hashCode() {
			return value.hashCode();
		}
		
		public boolean equals(Object obj) {
			if(obj instanceof XMLValue) {
				XMLValue otherVal = (XMLValue) obj;
				String stringVal = otherVal.value;
				return value.equals(stringVal);
			}
			return false;
		}
		
		public String toString() {
		    return value;
		}
	}

	RDFXMLLiteral() {
		super(ATermUtils.makeTermAppl(Namespaces.RDF + "XMLLiteral"));
	}


	/* (non-Javadoc)
	 * @see org.mindswap.pellet.datatypes.AtomicDatatype#getPrimitiveType()
	 */
	public AtomicDatatype getPrimitiveType() {
		return instance;
	}

	/* (non-Javadoc)
	 * @see org.mindswap.pellet.datatypes.Datatype#getValue(java.lang.String)
	 */
	public Object getValue(String value, String datatypeURI) {
	    if(datatypeURI.equals(name.getName()))
	        return new XMLValue(value);
	    
	    return null;
	}
	
	/* (non-Javadoc)
	 * @see org.mindswap.pellet.datatypes.Datatype#contains(java.lang.Object)
	 */
	public boolean contains(Object value) {
		return (value instanceof XMLValue) && super.contains(value);
	}
    
    public String toString() {
        return "rdf:XMLLiteral";
    }
}
