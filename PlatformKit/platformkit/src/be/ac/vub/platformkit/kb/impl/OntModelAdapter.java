package be.ac.vub.platformkit.kb.jena;

import java.util.Iterator;

import junit.framework.Assert;
import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntModel;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.RDFList;

public class OntModelAdapter implements IOntModel {
	
	private class OntClassIterator implements Iterator<OntClass> {
		private Iterator<IOntClass> inner;
		
		public OntClassIterator(Iterator<IOntClass> inner) {
			Assert.assertNotNull(inner);
			this.inner = inner;
		}

		public boolean hasNext() {
			return inner.hasNext();
		}

		public OntClass next() {
			return ((OntClassAdapter) inner.next()).model;
		}

		public void remove() {
			inner.remove();
		}
		
	}

	protected OntModel model;
	
	public OntModelAdapter(OntModel model) {
		Assert.assertNotNull(model);
		this.model = model;
	}

	public String toString() {
		return model.toString();
	}

    public IOntClass getOntClass(String uri){
    	OntClass c = model.getOntClass(uri);
    	if (c != null) {
    		return new OntClassAdapter(c);
    	}
		return null;
    }
    
    public IOntClass createIntersectionClass(String uri, Iterator<IOntClass> members) {
        RDFList constraints = model.createList(new OntClassIterator(members));
        return new OntClassAdapter(model.createIntersectionClass(uri, constraints));
    }
}
