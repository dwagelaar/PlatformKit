/*******************************************************************************
 * Copyright (c) 2005-2010 Dennis Wagelaar, Vrije Universiteit Brussel.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dennis Wagelaar, Vrije Universiteit Brussel
 *******************************************************************************/
package be.ac.vub.platformkit.kb.owlapi;

import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.io.StreamOutputTarget;
import org.semanticweb.owl.model.AddAxiom;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataRangeFacetRestriction;
import org.semanticweb.owl.model.OWLDataRangeRestriction;
import org.semanticweb.owl.model.OWLDataSomeRestriction;
import org.semanticweb.owl.model.OWLDataType;
import org.semanticweb.owl.model.OWLDataValueRestriction;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owl.model.OWLObjectIntersectionOf;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLObjectSomeRestriction;
import org.semanticweb.owl.model.OWLObjectUnionOf;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChange;
import org.semanticweb.owl.model.OWLOntologyChangeException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLOntologyStorageException;
import org.semanticweb.owl.model.OWLProperty;
import org.semanticweb.owl.model.OWLPropertyExpression;
import org.semanticweb.owl.model.OWLRestriction;
import org.semanticweb.owl.model.RemoveAxiom;
import org.semanticweb.owl.model.UnknownOWLOntologyException;
import org.semanticweb.owl.vocab.OWLRestrictedDataRangeFacetVocabulary;

import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntModel;
import be.ac.vub.platformkit.kb.util.OntException;

/**
 * {@link IOntModel} adapter for {@link OWLOntology}.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class OWLOntologyAdapter implements IOntModel {

	/**
	 * @param desc
	 * @return the local name of desc or the empty string if not available
	 */
	public static final String getLocalNameOf(OWLDescription desc) {
		if (!desc.isAnonymous()) {
			return desc.asOWLClass().getURI().getFragment();
		}
		return "";
	}

	private OWLOntology model;
	private OWLAPIOntologies ontologies;

	/**
	 * Creates a new {@link OWLOntologyAdapter}.
	 * @param model
	 * @param ontologies
	 */
	public OWLOntologyAdapter(OWLOntology model, OWLAPIOntologies ontologies) {
		assert model != null;
		assert ontologies != null;
		this.setModel(model);
		this.setOntologies(ontologies);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getModel().toString();
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#createIntersectionClass(java.lang.String, java.util.Iterator)
	 */
	public IOntClass createIntersectionClass(String uri,
			Iterator<IOntClass> members) throws OntException {
		final OWLOntologyManager mgr = getOntologies().getMgr();
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		try {
			final OWLClass intsecClass = factory.getOWLClass(new URI(uri));
			final Set<OWLClass> operands = toOWLClassSet(members);
			final OWLObjectIntersectionOf intersection = factory.getOWLObjectIntersectionOf(operands);
			final OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(intsecClass, intersection);
			final AddAxiom addAxiom = new AddAxiom(getModel(), axiom);
			mgr.applyChange(addAxiom);
			return new OWLClassAdapter(intsecClass, getOntologies());
		} catch (URISyntaxException e) {
			throw new OntException(e);
		} catch (OWLOntologyChangeException e) {
			throw new OntException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#getOntClass(java.lang.String)
	 */
	public IOntClass getOntClass(final String uri) throws OntException {
		final OWLAPIOntologies ontologies = getOntologies();
		final OWLOntologyManager mgr = ontologies.getMgr();
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		try {
			final URI classURI = new URI(uri);
			if (getModel().containsClassReference(classURI)) {
				final OWLClass owlClass = factory.getOWLClass(classURI);
				return new OWLClassAdapter(owlClass, ontologies);
			}
		} catch (URISyntaxException e) {
			throw new OntException(e);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#save(java.io.OutputStream)
	 */
	public void save(OutputStream out) throws OntException {
		final OWLAPIOntologies ontologies = getOntologies();
		final OWLOntologyManager mgr = ontologies.getMgr();
		try {
			mgr.saveOntology(getModel(), new StreamOutputTarget(out));
		} catch (UnknownOWLOntologyException e) {
			throw new OntException(e);
		} catch (OWLOntologyStorageException e) {
			throw new OntException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#getNsURI()
	 */
	public String getNsURI() {
		return getModel().getURI().toString();
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#createSomeRestriction(java.lang.String, be.ac.vub.platformkit.kb.IOntClass, java.lang.String, java.util.Iterator)
	 */
	public IOntClass createSomeRestriction(String uri, IOntClass superClass, 
			String propertyURI, Iterator<IOntClass> range) throws OntException {
		final OWLAPIOntologies ontologies = getOntologies();
		final OWLOntologyManager mgr = ontologies.getMgr();
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		try {
			final List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
			final URI restrClassURI = new URI(uri);
			final OWLClass restrClass = factory.getOWLClass(restrClassURI);
			if (propertyURI != null && range != null) {
				final URI propURI = new URI(propertyURI);
				final Set<OWLDescription> existingRanges = getExistingPropertyRestrictionRanges(restrClass, propURI);
				final OWLObjectProperty property = factory.getOWLObjectProperty(propURI);
				final Set<OWLDescription> restrictionSet = new HashSet<OWLDescription>();
				//create property restrictions on given ranges
				while (range.hasNext()) {
					OWLDescription rangeClass = ((OWLClassAdapter) range.next()).getModel();
					if (!mergeClassIntoRange(rangeClass, existingRanges)) {
						//append current range
						OWLDescription restriction = factory.getOWLObjectSomeRestriction(
								property, rangeClass);
						restrictionSet.add(restriction);
					}
				}
				//include remaining existing ranges
				for (OWLDescription rangeClass : existingRanges) {
					OWLDescription restriction = factory.getOWLObjectSomeRestriction(
							property, rangeClass);
					restrictionSet.add(restriction);
				}
				if (!restrictionSet.isEmpty()) {
					//remove existing equivalence class axioms
					for (OWLEquivalentClassesAxiom ecAxiom : getModel().getEquivalentClassesAxioms(restrClass)) {
						changes.add(new RemoveAxiom(getModel(), ecAxiom));
						restrictionSet.addAll(getOtherRestrictions(ecAxiom, propURI));
						restrictionSet.remove(restrClass);
					}
//					final OWLEquivalentClassesAxiom existingEcAxiom = getExistingEquivalentClassesAxiom(restrClass, propURI);
//					if (existingEcAxiom != null) {
//						changes.add(new RemoveAxiom(getModel(), existingEcAxiom));
//						restrictionSet.addAll(getOtherRestrictions(existingEcAxiom, propURI));
//						restrictionSet.remove(restrClass);
//					}
					//add new equivalence class axiom
					final OWLDescription restrIntersection = factory.getOWLObjectIntersectionOf(restrictionSet);
					final OWLAxiom ecAxiom = factory.getOWLEquivalentClassesAxiom(restrClass, restrIntersection);
					changes.add(new AddAxiom(getModel(), ecAxiom));
				}
			}
			if (superClass != null) {
				assert superClass instanceof OWLClassAdapter;
				final OWLAxiom scAxiom = factory.getOWLSubClassAxiom(restrClass, ((OWLClassAdapter) superClass).getModel());
				changes.add(new AddAxiom(getModel(), scAxiom));
			}
			mgr.applyChanges(changes);
			return new OWLClassAdapter(restrClass, getOntologies());
		} catch (URISyntaxException e) {
			throw new OntException(e);
		} catch (OWLOntologyChangeException e) {
			throw new OntException(e);
		} catch (OWLReasonerException e) {
			throw new OntException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#createMinInclusiveRestriction(java.lang.String, be.ac.vub.platformkit.kb.IOntClass, java.lang.String, java.lang.String, java.lang.String)
	 */
	public IOntClass createMinInclusiveRestriction(String uri, IOntClass superClass, 
			String propertyURI, String datatypeURI, String value) throws OntException {
		final OWLAPIOntologies ontologies = getOntologies();
		final OWLOntologyManager mgr = ontologies.getMgr();
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		try {
			final List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
			final URI restrClassURI = new URI(uri);
			final OWLClass restrClass = factory.getOWLClass(restrClassURI);
			if (propertyURI != null && datatypeURI != null) {
				final URI propURI = new URI(propertyURI);
				final OWLDataProperty property = factory.getOWLDataProperty(propURI);
				final Set<OWLDescription> restrictionSet = new HashSet<OWLDescription>();
	
				//remove existing equivalence class axiom
				for (OWLEquivalentClassesAxiom ecAxiom : getModel().getEquivalentClassesAxioms(restrClass)) {
					changes.add(new RemoveAxiom(getModel(), ecAxiom));
					restrictionSet.addAll(getOtherRestrictions(ecAxiom, propURI));
					restrictionSet.remove(restrClass);
				}
				//add new equivalence class axiom
				final URI dtURI = new URI(datatypeURI);
				final OWLDataType dataType = factory.getOWLDataType(dtURI);
				final OWLDataRangeFacetRestriction facetRestr = factory.getOWLDataRangeFacetRestriction(
						OWLRestrictedDataRangeFacetVocabulary.MIN_INCLUSIVE, 
						factory.getOWLTypedConstant(value, dataType));
				final OWLDataRangeRestriction rangeRestr = factory.getOWLDataRangeRestriction(dataType, facetRestr);
				final OWLDataSomeRestriction restr = factory.getOWLDataSomeRestriction(property, rangeRestr);
				if (!restrictionSet.isEmpty()) {
					restrictionSet.add(restr);
					final OWLDescription restrIntersection = factory.getOWLObjectIntersectionOf(restrictionSet);
					final OWLAxiom ecAxiom = factory.getOWLEquivalentClassesAxiom(restrClass, restrIntersection);
					changes.add(new AddAxiom(getModel(), ecAxiom));
				} else {
					final OWLAxiom ecAxiom = factory.getOWLEquivalentClassesAxiom(restrClass, restr);
					changes.add(new AddAxiom(getModel(), ecAxiom));
				}
	
			}
			if (superClass != null) {
				assert superClass instanceof OWLClassAdapter;
				final OWLAxiom scAxiom = factory.getOWLSubClassAxiom(restrClass, ((OWLClassAdapter) superClass).getModel());
				changes.add(new AddAxiom(getModel(), scAxiom));
			}
			mgr.applyChanges(changes);
			return new OWLClassAdapter(restrClass, getOntologies());
		} catch (URISyntaxException e) {
			throw new OntException(e);
		} catch (OWLOntologyChangeException e) {
			throw new OntException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#createHasValueRestriction(java.lang.String, be.ac.vub.platformkit.kb.IOntClass, java.lang.String, java.lang.String, java.lang.String)
	 */
	public IOntClass createHasValueRestriction(String uri, IOntClass superClass, 
			String propertyURI, String datatypeURI, String value) throws OntException {
		final OWLAPIOntologies ontologies = getOntologies();
		final OWLOntologyManager mgr = ontologies.getMgr();
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		try {
			final List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
			final URI restrClassURI = new URI(uri);
			final OWLClass restrClass = factory.getOWLClass(restrClassURI);
			if (propertyURI != null && datatypeURI != null) {
				final URI propURI = new URI(propertyURI);
				final OWLDataProperty property = factory.getOWLDataProperty(propURI);
				final Set<OWLDescription> restrictionSet = new HashSet<OWLDescription>();
	
				//remove existing equivalence class axiom
				for (OWLEquivalentClassesAxiom ecAxiom : getModel().getEquivalentClassesAxioms(restrClass)) {
					changes.add(new RemoveAxiom(getModel(), ecAxiom));
					restrictionSet.addAll(getOtherRestrictions(ecAxiom, propURI));
					restrictionSet.remove(restrClass);
				}
				//add new equivalence class axiom
				final URI dtURI = new URI(datatypeURI);
				final OWLDataType dataType = factory.getOWLDataType(dtURI);
				final OWLDataValueRestriction restr = factory.getOWLDataValueRestriction(
						property, factory.getOWLTypedConstant(value, dataType));
				if (!restrictionSet.isEmpty()) {
					restrictionSet.add(restr);
					final OWLDescription restrIntersection = factory.getOWLObjectIntersectionOf(restrictionSet);
					final OWLAxiom ecAxiom = factory.getOWLEquivalentClassesAxiom(restrClass, restrIntersection);
					changes.add(new AddAxiom(getModel(), ecAxiom));
				} else {
					final OWLAxiom ecAxiom = factory.getOWLEquivalentClassesAxiom(restrClass, restr);
					changes.add(new AddAxiom(getModel(), ecAxiom));
				}
	
			}
			if (superClass != null) {
				assert superClass instanceof OWLClassAdapter;
				final OWLAxiom scAxiom = factory.getOWLSubClassAxiom(restrClass, ((OWLClassAdapter) superClass).getModel());
				changes.add(new AddAxiom(getModel(), scAxiom));
			}
			mgr.applyChanges(changes);
			return new OWLClassAdapter(restrClass, getOntologies());
		} catch (URISyntaxException e) {
			throw new OntException(e);
		} catch (OWLOntologyChangeException e) {
			throw new OntException(e);
		}
	}

	/**
	 * @param uri
	 * @param propertyURI
	 * @return any existing property restriction range classes on the property with propertyURI for the ontology class with the given uri
	 */
	protected Set<OWLDescription> getExistingPropertyRestrictionRanges(
			final OWLClass restrClass, final URI propertyURI) {
		assert restrClass != null;
		return getExistingPropertyRestrictionRangesFrom(
				restrClass.getEquivalentClasses(getModel()), propertyURI);
	}

	/**
	 * @param classes
	 * @param propertyURI
	 * @return any existing property restriction range classes within classes on the property with propURI
	 */
	protected Set<OWLDescription> getExistingPropertyRestrictionRangesFrom(
			final Set<OWLDescription> classes, final URI propertyURI) {
		final Set<OWLDescription> rangeSet = new HashSet<OWLDescription>();
		for (OWLDescription c : classes) {
			if (c instanceof OWLObjectIntersectionOf) {
				OWLObjectIntersectionOf inters = (OWLObjectIntersectionOf) c;
				rangeSet.addAll(getExistingPropertyRestrictionRangesFrom(inters.getOperands(), propertyURI));
			} else if (c instanceof OWLObjectSomeRestriction) {
				OWLObjectSomeRestriction restr = (OWLObjectSomeRestriction) c;
				if (restr.getProperty().getNamedProperty().getURI().equals(propertyURI)) {
					rangeSet.add(((OWLObjectSomeRestriction) c).getFiller());
				}
			}
		}
		return rangeSet;
	}

	/**
	 * @param ecAxiom
	 * @param propertyURI
	 * @return the restrictions in the equivalence class axiom that are not property restrictions on the property with propertyURI
	 */
	protected Set<OWLDescription> getOtherRestrictions(final OWLEquivalentClassesAxiom ecAxiom, final URI propertyURI) {
		assert ecAxiom != null;
		return getOtherRestrictionsFrom(ecAxiom.getDescriptions(), propertyURI);
	}

	/**
	 * @param classes
	 * @param propertyURI
	 * @return the restrictions in classes that are not property restrictions on the property with propertyURI
	 */
	protected Set<OWLDescription> getOtherRestrictionsFrom(
			final Set<OWLDescription> classes, final URI propertyURI) {
		final Set<OWLDescription> otherRestrictions = new HashSet<OWLDescription>();
		for (OWLDescription desc : classes) {
			if (desc instanceof OWLObjectIntersectionOf) {
				otherRestrictions.addAll(
						getOtherRestrictionsFrom(((OWLObjectIntersectionOf) desc).getOperands(), propertyURI));
			} else if (!isPropertyRestrictionOn(desc, propertyURI)) {
				otherRestrictions.add(desc);
			}
		}
		return otherRestrictions;
	}

	/**
	 * @param desc
	 * @param propertyURI
	 * @return <code>true</code> iff desc is, or contains, a property restriction on the property with propertyURI
	 */
	protected boolean isPropertyRestrictionOn(OWLDescription desc, URI propertyURI) {
		if (desc instanceof OWLObjectIntersectionOf) {
			final OWLObjectIntersectionOf inters = (OWLObjectIntersectionOf) desc;
			for (OWLDescription sub : inters.getOperands()) {
				if (isPropertyRestrictionOn(sub, propertyURI)) {
					return true;
				}
			}
		} else if (desc instanceof OWLRestriction<?>) {
			final OWLPropertyExpression<?,?> propExp = ((OWLRestriction<?>) desc).getProperty();
			if (!propExp.isAnonymous()) {
				final Set<OWLProperty<?,?>> props = new HashSet<OWLProperty<?,?>>();
				props.addAll(propExp.getDataPropertiesInSignature());
				props.addAll(propExp.getObjectPropertiesInSignature());
				assert props.size() == 1;
				if (propertyURI.equals(props.iterator().next().getURI())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Given a range of OWL class descriptions, merge owlClass into the range according to
	 * the following rules:
	 * <ol>
	 * <li>owlClass replaces any entry in range that is a subclass of owlClass;</li>
	 * <li>owlClass is discarded if any entry in range is a superclass of owlClass;</li>
	 * <li>for any sibling entries in range that have the same local name as owlClass, the entry is replaced by the union of the entry and owlClass;</li>
	 * <li>for any entries in range that represent class unions, owlClass is merged with the union according to the previous rules.</li>
	 * </ol>
	 * Requires (transitive) reasoner.
	 * @param owlClass the OWL class description to merge
	 * @param range the range of OWL class descriptions into which owlClass should be merged
	 * @return <code>false</code> iff none of the above rules apply, and owlClass should just be added to the range 
	 * @throws OWLReasonerException 
	 */
	protected boolean mergeClassIntoRange(final OWLDescription owlClass, 
			final Set<OWLDescription> range) throws OWLReasonerException {

		final OWLAPIOntologies ontologies = getOntologies();
		final OWLOntologyManager mgr = ontologies.getMgr();
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		final String owlClassName = getLocalNameOf(owlClass);
		final OWLReasoner reasoner = getOntologies().getReasoner();
		assert reasoner != null;

		boolean merged = false;
		Set<OWLDescription> newEntries = new HashSet<OWLDescription>();

		//find least specific superclass in range
		for (final Iterator<OWLDescription> oc = range.iterator(); oc.hasNext();) {
			OWLDescription otherClass = oc.next();
			if (reasoner.isSubClassOf(owlClass, otherClass)) {
				//discard owlClass
				merged = true;
			} else if (reasoner.isSubClassOf(otherClass, owlClass)) {
				//replace range entry by owlClass
				oc.remove();
				newEntries.add(owlClass);
				merged = true;
			} else if (owlClassName.equals(getLocalNameOf(otherClass))) {
				//replace range entry by union of range entry and owlClass
				oc.remove();
				newEntries.add(factory.getOWLObjectUnionOf(owlClass, otherClass));
				merged = true;
			} else if (otherClass instanceof OWLObjectUnionOf) {
				//merge into class union
				Set<OWLDescription> ops = new HashSet<OWLDescription>(
						((OWLObjectUnionOf) otherClass).getOperands());
				if (mergeClassIntoRange(owlClass, ops)) {
					//replace by new union
					oc.remove();
					newEntries.add(factory.getOWLObjectUnionOf(ops));
					merged = true;
				}
			}
		}

		range.addAll(newEntries);
		return merged;
	}

	/**
	 * @param classes
	 * @return a set of {@link OWLClass}es contained in classes
	 */
	protected Set<OWLClass> toOWLClassSet(final Iterator<IOntClass> classes) {
		final Set<OWLClass> result = new HashSet<OWLClass>();
		while (classes.hasNext()) {
			OWLClassAdapter c = (OWLClassAdapter) classes.next();
			result.add(c.getModel());
		}
		return result;
	}

	/**
	 * @param ontologies the ontologies to set
	 */
	protected void setOntologies(OWLAPIOntologies ontologies) {
		this.ontologies = ontologies;
	}

	/**
	 * @return the ontologies
	 */
	protected OWLAPIOntologies getOntologies() {
		return ontologies;
	}

	/**
	 * @param model the model to set
	 */
	protected void setModel(OWLOntology model) {
		this.model = model;
	}

	/**
	 * @return the model
	 */
	protected OWLOntology getModel() {
		return model;
	}

}
