package be.ac.vub.platformkit.kb.jena;

import junit.framework.Assert;
import be.ac.vub.platformkit.kb.IOntClass;

import com.hp.hpl.jena.ontology.OntClass;

public class OntClassAdapter implements IOntClass {

	protected OntClass model;
	
	public OntClassAdapter(OntClass model) {
		Assert.assertNotNull(model);
		this.model = model;
	}
	
	public String toString() {
		return model.toString();
	}
	
	public boolean hasEquivalentClass(IOntClass c) throws ClassCastException {
		return model.hasEquivalentClass(((OntClassAdapter)c).model);
	}
	
    public boolean hasSubClass(IOntClass c) throws ClassCastException {
    	return model.hasSubClass(((OntClassAdapter)c).model);
    }
    
    public boolean hasSuperClass(IOntClass c) throws ClassCastException {
    	return model.hasSuperClass(((OntClassAdapter)c).model);
    }
	
    public void addEquivalentClass(IOntClass c) throws ClassCastException {
    	model.addEquivalentClass(((OntClassAdapter)c).model);
    }
    
    public boolean hasInstances() {
    	return model.listInstances().hasNext();
    }

}
