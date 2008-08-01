/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.GenericIntervalList;
import org.mindswap.pellet.utils.Namespaces;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 */
public class XSDInteger extends XSDDecimal implements AtomicDatatype, XSDAtomicType {
    public static XSDInteger instance = new XSDInteger( ATermUtils.makeTermAppl( Namespaces.XSD + "integer" ) );

    protected XSDInteger( ATermAppl name ) {
        super( name, false );
    }

    public BaseXSDAtomicType create( GenericIntervalList intervals ) {
        XSDInteger type = new XSDInteger( null );
        type.values = intervals;

        return type;
    }
}
