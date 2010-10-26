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
package be.ac.vub.platformkit.kb.owlapi3;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.vocab.OWLFacet;

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
	public static final String getLocalNameOf(OWLClassExpression desc) {
		if (!desc.isAnonymous()) {
			return desc.asOWLClass().getIRI().getFragment();
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
			final OWLClass intsecClass = factory.getOWLClass(IRI.create(uri));
			final Set<OWLClass> operands = toOWLClassSet(members);
			final OWLObjectIntersectionOf intersection = factory.getOWLObjectIntersectionOf(operands);
			final OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(intsecClass, intersection);
			final AddAxiom addAxiom = new AddAxiom(getModel(), axiom);
			mgr.applyChange(addAxiom);
			return new OWLClassAdapter(intsecClass, getOntologies());
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
		final IRI classURI = IRI.create(uri);
		if (getModel().containsClassInSignature(classURI)) {
			final OWLClass owlClass = factory.getOWLClass(classURI);
			return new OWLClassAdapter(owlClass, ontologies);
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
			mgr.saveOntology(getModel(), new StreamDocumentTarget(out));
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
		return getModel().getOntologyID().getOntologyIRI().toString();
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
			final IRI restrClassURI = IRI.create(uri);
			final OWLClass restrClass = factory.getOWLClass(restrClassURI);
			if (propertyURI != null && range != null) {
				final IRI propURI = IRI.create(propertyURI);
				final Set<OWLClassExpression> existingRanges = getExistingPropertyRestrictionRanges(
						restrClass, propURI);
				final OWLObjectProperty property = factory.getOWLObjectProperty(propURI);
				final Set<OWLClassExpression> restrictionSet = new HashSet<OWLClassExpression>();
				//create property restrictions on given ranges
				while (range.hasNext()) {
					OWLClassExpression rangeClass = ((OWLClassAdapter) range.next()).getModel();
					if (!mergeClassIntoRange(rangeClass, existingRanges, false)) {
						//append current range
						OWLClassExpression restriction = factory.getOWLObjectSomeValuesFrom(
								property, rangeClass);
						restrictionSet.add(restriction);
					}
				}
				//include remaining existing ranges
				for (OWLClassExpression rangeClass : existingRanges) {
					OWLClassExpression restriction = factory.getOWLObjectSomeValuesFrom(
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
					//add new equivalence class axiom
					final OWLClassExpression restrIntersection = factory.getOWLObjectIntersectionOf(restrictionSet);
					final OWLAxiom ecAxiom = factory.getOWLEquivalentClassesAxiom(restrClass, restrIntersection);
					changes.add(new AddAxiom(getModel(), ecAxiom));
				}
			}
			if (superClass != null) {
				assert superClass instanceof OWLClassAdapter;
				final OWLAxiom scAxiom = factory.getOWLSubClassOfAxiom(restrClass, ((OWLClassAdapter) superClass).getModel());
				changes.add(new AddAxiom(getModel(), scAxiom));
			}
			mgr.applyChanges(changes);
			return new OWLClassAdapter(restrClass, getOntologies());
		} catch (OWLOntologyChangeException e) {
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
			final IRI restrClassURI = IRI.create(uri);
			final OWLClass restrClass = factory.getOWLClass(restrClassURI);
			if (propertyURI != null && datatypeURI != null && value != null) {
				final IRI propURI = IRI.create(propertyURI);
				final OWLDataProperty property = factory.getOWLDataProperty(propURI);
				final Set<OWLClassExpression> restrictionSet = new HashSet<OWLClassExpression>();
	
				//remove existing equivalence class axiom
				for (OWLEquivalentClassesAxiom ecAxiom : getModel().getEquivalentClassesAxioms(restrClass)) {
					changes.add(new RemoveAxiom(getModel(), ecAxiom));
					restrictionSet.addAll(getOtherRestrictions(ecAxiom, propURI));
					restrictionSet.remove(restrClass);
				}
				//add new equivalence class axiom
				final IRI dtURI = IRI.create(datatypeURI);
				final OWLDatatype dataType = factory.getOWLDatatype(dtURI);
				final OWLFacetRestriction facetRestr = factory.getOWLFacetRestriction(
						OWLFacet.MIN_INCLUSIVE, 
						factory.getOWLLiteral(value, dataType));
				final OWLDatatypeRestriction rangeRestr = factory.getOWLDatatypeRestriction(dataType, facetRestr);
				final OWLDataSomeValuesFrom restr = factory.getOWLDataSomeValuesFrom(property, rangeRestr);
				if (!restrictionSet.isEmpty()) {
					restrictionSet.add(restr);
					final OWLClassExpression restrIntersection = factory.getOWLObjectIntersectionOf(restrictionSet);
					final OWLAxiom ecAxiom = factory.getOWLEquivalentClassesAxiom(restrClass, restrIntersection);
					changes.add(new AddAxiom(getModel(), ecAxiom));
				} else {
					final OWLAxiom ecAxiom = factory.getOWLEquivalentClassesAxiom(restrClass, restr);
					changes.add(new AddAxiom(getModel(), ecAxiom));
				}
	
			}
			if (superClass != null) {
				assert superClass instanceof OWLClassAdapter;
				final OWLAxiom scAxiom = factory.getOWLSubClassOfAxiom(restrClass, ((OWLClassAdapter) superClass).getModel());
				changes.add(new AddAxiom(getModel(), scAxiom));
			}
			mgr.applyChanges(changes);
			return new OWLClassAdapter(restrClass, getOntologies());
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
			final IRI restrClassURI = IRI.create(uri);
			final OWLClass restrClass = factory.getOWLClass(restrClassURI);
			if (propertyURI != null && datatypeURI != null && value != null) {
				final IRI propURI = IRI.create(propertyURI);
				final OWLDataProperty property = factory.getOWLDataProperty(propURI);
				final Set<OWLClassExpression> restrictionSet = new HashSet<OWLClassExpression>();
	
				//remove existing equivalence class axiom
				for (OWLEquivalentClassesAxiom ecAxiom : getModel().getEquivalentClassesAxioms(restrClass)) {
					changes.add(new RemoveAxiom(getModel(), ecAxiom));
					restrictionSet.addAll(getOtherRestrictions(ecAxiom, propURI));
					restrictionSet.remove(restrClass);
				}
				//add new equivalence class axiom
				final IRI dtURI = IRI.create(datatypeURI);
				final OWLDatatype dataType = factory.getOWLDatatype(dtURI);
				final OWLDataHasValue restr = factory.getOWLDataHasValue(
						property, factory.getOWLLiteral(value, dataType));
				if (!restrictionSet.isEmpty()) {
					restrictionSet.add(restr);
					final OWLClassExpression restrIntersection = factory.getOWLObjectIntersectionOf(restrictionSet);
					final OWLAxiom ecAxiom = factory.getOWLEquivalentClassesAxiom(restrClass, restrIntersection);
					changes.add(new AddAxiom(getModel(), ecAxiom));
				} else {
					final OWLAxiom ecAxiom = factory.getOWLEquivalentClassesAxiom(restrClass, restr);
					changes.add(new AddAxiom(getModel(), ecAxiom));
				}
	
			}
			if (superClass != null) {
				assert superClass instanceof OWLClassAdapter;
				final OWLAxiom scAxiom = factory.getOWLSubClassOfAxiom(restrClass, ((OWLClassAdapter) superClass).getModel());
				changes.add(new AddAxiom(getModel(), scAxiom));
			}
			mgr.applyChanges(changes);
			return new OWLClassAdapter(restrClass, getOntologies());
		} catch (OWLOntologyChangeException e) {
			throw new OntException(e);
		}
	}

	/**
	 * @param restrClass
	 * @param propertyURI
	 * @return any existing property restriction range classes on the property with propertyURI for the ontology class with the given uri
	 */
	protected Set<OWLClassExpression> getExistingPropertyRestrictionRanges(
			final OWLClass restrClass, final IRI propertyURI) {
		assert restrClass != null;
		return getExistingPropertyRestrictionRangesFrom(
				restrClass.getEquivalentClasses(getModel()), propertyURI);
	}

	/**
	 * @param classes
	 * @param propertyURI
	 * @return any existing property restriction range classes within classes on the property with propURI
	 */
	protected Set<OWLClassExpression> getExistingPropertyRestrictionRangesFrom(
			final Set<OWLClassExpression> classes, final IRI propertyURI) {
		final Set<OWLClassExpression> rangeSet = new HashSet<OWLClassExpression>();
		for (OWLClassExpression c : classes) {
			if (c instanceof OWLObjectIntersectionOf) {
				OWLObjectIntersectionOf inters = (OWLObjectIntersectionOf) c;
				rangeSet.addAll(getExistingPropertyRestrictionRangesFrom(
						inters.getOperands(), propertyURI));
			} else if (c instanceof OWLObjectUnionOf) {
				OWLObjectUnionOf union = (OWLObjectUnionOf) c;
				rangeSet.addAll(getExistingPropertyRestrictionRangesFrom(
						union.getOperands(), propertyURI));
			} else if (c instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom restr = (OWLObjectSomeValuesFrom) c;
				if (restr.getProperty().getNamedProperty().getIRI().equals(propertyURI)) {
					rangeSet.add(restr.getFiller());
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
	protected Set<OWLClassExpression> getOtherRestrictions(final OWLEquivalentClassesAxiom ecAxiom, 
			final IRI propertyURI) {
		assert ecAxiom != null;
		return getOtherRestrictionsFrom(ecAxiom.getClassExpressions(), propertyURI);
	}

	/**
	 * @param classes
	 * @param propertyURI
	 * @return the restrictions in classes that are not property restrictions on the property with propertyURI
	 */
	protected Set<OWLClassExpression> getOtherRestrictionsFrom(
			final Set<OWLClassExpression> classes, final IRI propertyURI) {
		final Set<OWLClassExpression> otherRestrictions = new HashSet<OWLClassExpression>();
		for (OWLClassExpression desc : classes) {
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
	protected boolean isPropertyRestrictionOn(OWLClassExpression desc, IRI propertyURI) {
		if (desc instanceof OWLObjectIntersectionOf) {
			final OWLObjectIntersectionOf inters = (OWLObjectIntersectionOf) desc;
			for (OWLClassExpression sub : inters.getOperands()) {
				if (isPropertyRestrictionOn(sub, propertyURI)) {
					return true;
				}
			}
		} else if (desc instanceof OWLRestriction<?,?,?>) {
			final OWLPropertyExpression<?,?> propExp = ((OWLRestriction<?,?,?>) desc).getProperty();
			if (!propExp.isAnonymous()) {
				final Set<OWLProperty<?,?>> props = new HashSet<OWLProperty<?,?>>();
				props.addAll(propExp.getDataPropertiesInSignature());
				props.addAll(propExp.getObjectPropertiesInSignature());
				assert props.size() == 1;
				if (propertyURI.equals(props.iterator().next().getIRI())) {
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
	 * @param rangeIsUnion if <code>true</code>, range is considered to be joined in a union instead of an intersection
	 * @return <code>false</code> iff none of the above rules apply, and owlClass should just be added to the range 
	 */
	protected boolean mergeClassIntoRange(final OWLClassExpression owlClass, 
			final Set<OWLClassExpression> range, boolean rangeIsUnion) {

		final OWLAPIOntologies ontologies = getOntologies();
		final OWLOntologyManager mgr = ontologies.getMgr();
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		final String owlClassName = getLocalNameOf(owlClass);
		final OWLReasoner reasoner = getOntologies().getReasoner();
		assert reasoner != null;

		boolean merged = false;
		final Set<OWLClassExpression> newEntries = new HashSet<OWLClassExpression>();
		final Set<OWLClassExpression> unionEntries = new HashSet<OWLClassExpression>();

		//find least specific superclass in range
		for (final Iterator<OWLClassExpression> oc = range.iterator(); oc.hasNext();) {
			OWLClassExpression otherClass = oc.next();
			NodeSet<OWLClass> subcs = reasoner.getSubClasses(owlClass, false);
			NodeSet<OWLClass> supercs = reasoner.getSuperClasses(owlClass, false);
			if (supercs.getFlattened().contains(otherClass)) {
				//discard owlClass
				merged = true;
			} else if (subcs.getFlattened().contains(otherClass)) {
				//replace range entry by owlClass
				oc.remove();
				newEntries.add(owlClass);
				merged = true;
			} else if (otherClass instanceof OWLObjectUnionOf) {
				//merge into class union
				Set<OWLClassExpression> ops = new HashSet<OWLClassExpression>(
						((OWLObjectUnionOf) otherClass).getOperands());
				if (mergeClassIntoRange(owlClass, ops, true)) {
					//replace by new union if ops.size() > 1
					oc.remove();
					if (ops.size() > 1) {
						newEntries.add(factory.getOWLObjectUnionOf(ops));
					} else {
						newEntries.addAll(ops);
					}
					merged = true;
				}
			} else if (owlClassName.equals(getLocalNameOf(otherClass))) {
				//replace range entry by union of range entry and owlClass
				oc.remove();
				unionEntries.add(otherClass);
				unionEntries.add(owlClass);
				merged = true;
			}
		}

		//process union entries, depending on whether range already represents a union or not
		if (!unionEntries.isEmpty()) {
			if (rangeIsUnion) {
				newEntries.addAll(unionEntries);
			} else {
				newEntries.add(factory.getOWLObjectUnionOf(unionEntries));
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
