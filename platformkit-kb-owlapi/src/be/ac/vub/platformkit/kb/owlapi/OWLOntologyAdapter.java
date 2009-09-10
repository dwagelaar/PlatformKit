package be.ac.vub.platformkit.kb.owlapi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.semanticweb.owl.model.AddAxiom;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLObjectIntersectionOf;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChangeException;
import org.semanticweb.owl.model.OWLOntologyManager;

import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntModel;
import be.ac.vub.platformkit.kb.IOntologies;

public class OWLOntologyAdapter implements IOntModel {
	
	protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);
	protected OWLOntology model;
	protected OWLAPIOntologies ontologies;
	
	public OWLOntologyAdapter(OWLOntology model, OWLAPIOntologies ontologies) {
		Assert.assertNotNull(model);
		Assert.assertNotNull(ontologies);
		this.model = model;
		this.ontologies = ontologies;
	}

	public String toString() {
		return model.toString();
	}

	public IOntClass createIntersectionClass(String uri,
			Iterator<IOntClass> members) {
		final OWLOntologyManager mgr = ontologies.mgr;
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		try {
			URI classURI = new URI(uri);
			OWLClass intsecClass = factory.getOWLClass(classURI);
			Set<OWLClass> operands = new HashSet<OWLClass>();
			while (members.hasNext()) {
				OWLClassAdapter c = (OWLClassAdapter) members.next();
				operands.add(c.model);
			}
			OWLObjectIntersectionOf intersection = factory.getOWLObjectIntersectionOf(operands);
			OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(intsecClass, intersection);
			AddAxiom addAxiom = new AddAxiom(model, axiom);
			mgr.applyChange(addAxiom);
			return new OWLClassAdapter(intsecClass, ontologies);
		} catch (URISyntaxException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (OWLOntologyChangeException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		return null;
	}

	public IOntClass getOntClass(String uri) {
		final OWLOntologyManager mgr = ontologies.mgr;
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		try {
			URI classURI = new URI(uri);
			if (model.containsClassReference(classURI)) {
				OWLClass owlClass = factory.getOWLClass(classURI);
				return new OWLClassAdapter(owlClass, ontologies);
			}
		} catch (URISyntaxException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		return null;
	}

}