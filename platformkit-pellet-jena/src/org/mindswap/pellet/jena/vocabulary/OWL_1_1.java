/*
 * Created on Oct 15, 2006
 */
package org.mindswap.pellet.jena.vocabulary;

import org.mindswap.pellet.utils.Namespaces;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;

public class OWL_1_1 extends OWL {    
	final public static String NS = Namespaces.OWL_1_1;
	
    final public static Resource ReflexiveProperty = ResourceFactory.createResource( NS + "ReflexiveProperty" );
    
    final public static Resource IrreflexiveProperty = ResourceFactory.createResource( NS + "IrreflexiveProperty" );
    
    final public static Resource AntisymmetricProperty = ResourceFactory.createResource( NS + "AntisymmetricProperty" );
    
    final public static Resource SelfRestriction = ResourceFactory.createResource( NS + "SelfRestriction" );
    
    final public static Resource NegativeObjectPropertyAssertion = ResourceFactory.createResource( NS + "NegativeObjectPropertyAssertion" );
    
    final public static Resource NegativeDataPropertyAssertion = ResourceFactory.createResource( NS + "NegativeDataPropertyAssertion" );
    
    final public static Property disjointUnionOf = ResourceFactory.createProperty( NS + "disjointUnionOf" );
    
    final public static Property disjointObjectProperties = ResourceFactory.createProperty( NS + "disjointObjectProperties" );
    
    final public static Property disjointDataProperties = ResourceFactory.createProperty( NS + "disjointDataProperties" );
    
    final public static Property onClass = ResourceFactory.createProperty( NS + "onClass" );
        
    final public static Property onDataRange = ResourceFactory.createProperty( NS + "onDataRange" );
    
    final public static Property dataComplementOf = ResourceFactory.createProperty( NS + "dataComplementOf" );
    
    final public static Property length = ResourceFactory.createProperty( NS + "length" );
    
    final public static Property maxLength = ResourceFactory.createProperty( NS + "maxLength" );
    
    final public static Property minLength = ResourceFactory.createProperty( NS + "minLength" );
    
    final public static Property enumeration = ResourceFactory.createProperty( NS + "enumeration" );
    
    final public static Property totalDigits = ResourceFactory.createProperty( NS + "totalDigits" );
    
    final public static Property fractionDigits = ResourceFactory.createProperty( NS + "fractionDigits" );
    
    final public static Property minInclusive = ResourceFactory.createProperty( NS + "minInclusive" );
    
    final public static Property minExclusive = ResourceFactory.createProperty( NS + "minExclusive" );
    
    final public static Property maxInclusive = ResourceFactory.createProperty( NS + "maxInclusive" );
    
    final public static Property maxExclusive = ResourceFactory.createProperty( NS + "maxExclusive" );
    
    final public static Property pattern = ResourceFactory.createProperty( NS + "pattern" );
    
    final public static Property whiteSpace = ResourceFactory.createProperty( NS + "whiteSpace" );
}
