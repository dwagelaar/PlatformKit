package be.ac.vub.platformkit.kb.owlapi;

import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.model.OWLClass;

import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntologies;

public class OWLClassAdapter implements IOntClass {
	
	protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);
	protected OWLClass model;
	protected OWLAPIOntologies ontologies;

	public OWLClassAdapter(OWLClass model, OWLAPIOntologies ontologies) {
		Assert.assertNotNull(model);
		Assert.assertNotNull(ontologies);
		this.model = model;
		this.ontologies = ontologies;
	}

	public String toString() {
		return model.toString();
	}

	public boolean hasEquivalentClass(IOntClass c) throws ClassCastException {
		final OWLReasoner reasoner = ontologies.reasoner;
		if (reasoner != null) {
			try {
				return reasoner.isEquivalentClass(((OWLClassAdapter)c).model, model);
			} catch (OWLReasonerException e) {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
		return false;
	}

	public boolean hasSubClass(IOntClass c) throws ClassCastException {
		final OWLReasoner reasoner = ontologies.reasoner;
		if (reasoner != null) {
			try {
				return reasoner.isSubClassOf(((OWLClassAdapter)c).model, model);
			} catch (OWLReasonerException e) {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
		return false;
	}

	public boolean hasSuperClass(IOntClass c) throws ClassCastException {
		final OWLReasoner reasoner = ontologies.reasoner;
		if (reasoner != null) {
			try {
				return reasoner.isSubClassOf(model, ((OWLClassAdapter)c).model);
			} catch (OWLReasonerException e) {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
		return false;
	}

	public boolean hasInstances() {
		final OWLReasoner reasoner = ontologies.reasoner;
		if (reasoner != null) {
			try {
				return !reasoner.getIndividuals(model, false).isEmpty();
			} catch (OWLReasonerException e) {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
		return false;
	}

}
