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
package be.ac.vub.platformkit.kb.jena;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntModel;
import be.ac.vub.platformkit.kb.util.OntException;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.BooleanClassDescription;
import com.hp.hpl.jena.ontology.ComplementClass;
import com.hp.hpl.jena.ontology.HasValueRestriction;
import com.hp.hpl.jena.ontology.IntersectionClass;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.SomeValuesFromRestriction;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * {@link IOntModel} adapter for {@link OntModel}. 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class OntModelAdapter implements IOntModel {

	/**
	 * Removes any equivalent class axioms from ontClass, and deletes any contained
	 * anonymous resources.
	 * @param ontClass
	 * @param excluding the set of resources to exclude from deletion
	 */
	public static final void removeEquivalentClassesFrom(OntClass ontClass, Set<Resource> excluding) {
		final Set<OntClass> ecSet = new HashSet<OntClass>();
		for (Iterator<OntClass> ecs = ontClass.listEquivalentClasses(); ecs.hasNext();) {
			OntClass ec = ecs.next();
			if (!ontClass.equals(ec)) {
				ecSet.add(ec);
			}
		}
		for (OntClass ec : ecSet) {
			ontClass.removeEquivalentClass(ec);
			if (ec.isAnon() && !excluding.contains(ec)) {
				removeNode(ec, excluding);
			}
		}
	}

	/**
	 * Recursively removes node from its ontology.
	 * Removes only anonymous sub-nodes.
	 * @param node
	 * @param excluding the set of resources to exclude from deletion
	 */
	public static final void removeNode(RDFNode node, Set<Resource> excluding) {
		BooleanClassDescription bcd = null;
		SomeValuesFromRestriction sr = null;
		AllValuesFromRestriction ar = null;
		HasValueRestriction hr = null;
		OntClass c =  null;
		Resource r = null;
		RDFNode rop = null;
		if (node.canAs(IntersectionClass.class)) {
			r = c = bcd = node.as(IntersectionClass.class);
		} else if (node.canAs(UnionClass.class)) {
			r = c = bcd = node.as(UnionClass.class);
		} else if (node.canAs(ComplementClass.class)) {
			r = c = bcd = node.as(ComplementClass.class);
		} else if (node.canAs(SomeValuesFromRestriction.class)) {
			r = c = sr = node.as(SomeValuesFromRestriction.class);
			rop = sr.getSomeValuesFrom();
		} else if (node.canAs(AllValuesFromRestriction.class)) {
			r = c = ar = node.as(AllValuesFromRestriction.class);
			rop = ar.getAllValuesFrom();
		} else if (node.canAs(HasValueRestriction.class)) {
			r = c = hr = node.as(HasValueRestriction.class);
			rop = hr.getHasValue();
		} else if (node.canAs(OntClass.class)) {
			r = c = node.as(OntClass.class);
		} else if (node.canAs(Resource.class)) {
			r = node.as(Resource.class);
		}
		if (bcd != null) {
			final Set<RDFNode> ops = new HashSet<RDFNode>();
			for (Iterator<RDFNode> o = bcd.getOperands().iterator(); o.hasNext();) {
				ops.add(o.next());
			}
			for (RDFNode op : ops) {
				bcd.removeOperand((Resource) op);
				if (op.isAnon() && !excluding.contains(op)) {
					removeNode(op, excluding);
				}
			}
		}
		if (rop != null && rop.isAnon()) {
			removeNode(rop, excluding);
		}
		if (r != null) {
			for (StmtIterator stmts = r.listProperties(); stmts.hasNext();) {
				Statement stmt = stmts.next();
				RDFNode ob = stmt.getObject();
				if (ob != null && ob.isAnon() && !excluding.contains(ob)) {
					removeNode(ob, excluding);
				}
			}
			r.removeProperties();
		}
		if (c != null) {
			c.remove();
		}
	}

	/**
	 * Unwraps the {@link OntClass}es from the {@link IOntClass}es.
	 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
	 */
	private static class OntClassIterator implements Iterator<OntClass> {
		private Iterator<IOntClass> inner;

		/**
		 * Creates a new {@link OntClassIterator}.
		 * @param inner
		 */
		public OntClassIterator(Iterator<IOntClass> inner) {
			assert inner != null;
			this.inner = inner;
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return inner.hasNext();
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public OntClass next() {
			return ((OntClassAdapter) inner.next()).getModel();
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			inner.remove();
		}

	}

	private OntModel model;

	/**
	 * Creates a new {@link OntModelAdapter}.
	 * @param model
	 */
	public OntModelAdapter(OntModel model) {
		assert model != null;
		this.setModel(model);
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
	 * @see be.ac.vub.platformkit.kb.IOntModel#getOntClass(java.lang.String)
	 */
	public IOntClass getOntClass(String uri){
		OntClass c = getModel().getOntClass(uri);
		if (c != null) {
			return new OntClassAdapter(c);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#createIntersectionClass(java.lang.String, java.util.Iterator)
	 */
	public IOntClass createIntersectionClass(String uri, Iterator<IOntClass> members) {
		final RDFList constraints = getModel().createList(new OntClassIterator(members));
		return new OntClassAdapter(getModel().createIntersectionClass(uri, constraints));
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#save(java.io.OutputStream)
	 */
	public void save(OutputStream out) throws OntException {
		final OntModel model = getModel();
		final String ns = getNsURI();
		final RDFWriter writer = model.getWriter(FileUtils.langXMLAbbrev);
		JenaOntologies.prepareWriter(writer, ns);
		writer.write(model.getBaseModel(), out, ns);
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#getNsURI()
	 */
	public String getNsURI() {
		final String ns = model.getNsPrefixURI("");
		return ns.substring(0, ns.length() - 1);
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#createSomeRestriction(java.lang.String, be.ac.vub.platformkit.kb.IOntClass, java.lang.String, java.util.Iterator)
	 */
	public IOntClass createSomeRestriction(String uri, IOntClass superClass, 
			String propertyURI,	Iterator<IOntClass> range) throws OntException {
		final OntModel model = getModel();
		final OntClass restrClass = model.createClass(uri);
		if (propertyURI != null && range != null) {
			final Set<Resource> existingRanges = getExistingPropertyRestrictionRanges(restrClass, propertyURI);
			final Property property = model.getProperty(propertyURI);
			final Set<Resource> restrictionSet = new HashSet<Resource>();
			//create property restrictions on given ranges
			while (range.hasNext()) {
				OntClass rangeClass = ((OntClassAdapter) range.next()).getModel();
				if (!mergeClassIntoRange(rangeClass, existingRanges, false)) {
					//append current range
					SomeValuesFromRestriction restr = model.createSomeValuesFromRestriction(
							null, property, rangeClass);
					restrictionSet.add(restr);
				}
			}
			//include remaining existing ranges
			for (Resource rangeClass : existingRanges) {
				Resource restriction = model.createSomeValuesFromRestriction(
						null, property, rangeClass);
				restrictionSet.add(restriction);
			}
			if (!restrictionSet.isEmpty()) {
				//remove existing equivalence classes
				restrictionSet.addAll(getOtherRestrictionsFrom(
						restrClass.listEquivalentClasses(), propertyURI));
				restrictionSet.remove(restrClass);
				removeEquivalentClassesFrom(restrClass, restrictionSet);
				//add new equivalence class
				final RDFList restrMembers = model.createList(restrictionSet.iterator());
				final IntersectionClass restrIntersection = model.createIntersectionClass(null, restrMembers);
				restrClass.addEquivalentClass(restrIntersection);
			}
		}
		if (superClass != null) {
			assert superClass instanceof OntClassAdapter;
			restrClass.addSuperClass(((OntClassAdapter) superClass).getModel());
		}
		return new OntClassAdapter(restrClass);
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#createMinInclusiveRestriction(java.lang.String, be.ac.vub.platformkit.kb.IOntClass, java.lang.String, java.lang.String, java.lang.String)
	 */
	public IOntClass createMinInclusiveRestriction(String uri,
			IOntClass superClass, String propertyURI, String datatypeURI,
			String value) throws OntException {
		final OntModel model = getModel();
		final OntClass restrClass = model.createClass(uri);
		if (propertyURI != null && datatypeURI != null && value != null) {
			final Property property = model.getProperty(propertyURI);
			final Set<Resource> restrictionSet = new HashSet<Resource>();

			//remove existing equivalence classes
			restrictionSet.addAll(getOtherRestrictionsFrom(
					restrClass.listEquivalentClasses(), propertyURI));
			restrictionSet.remove(restrClass);
			removeEquivalentClassesFrom(restrClass, restrictionSet);
			//add new equivalence class
			final Resource dataTypeRes = model.createResource(datatypeURI);
			final RDFDatatype dataType = TypeMapper.getInstance().getSafeTypeByName(datatypeURI);
			final Resource rangeRestr = model.createResource(RDFS.Datatype);
			final Property onDatatype = model.createProperty(OWL.NS, "onDatatype");
			rangeRestr.addProperty(onDatatype, dataTypeRes);
			final Resource rangeDesc = model.createResource();
			final Property minInclusive = model.createProperty(XSD.getURI(), "minInclusive");
			rangeDesc.addProperty(minInclusive, value, dataType);
			final Property withRestrictions = model.createProperty(OWL.NS, "withRestrictions");
			rangeRestr.addProperty(withRestrictions, model.createList(new RDFNode[]{ rangeDesc }));
			final SomeValuesFromRestriction restr = model.createSomeValuesFromRestriction(null, property, rangeRestr);
			if (!restrictionSet.isEmpty()) {
				restrictionSet.add(restr);
				final RDFList restrMembers = model.createList(restrictionSet.iterator());
				final IntersectionClass restrIntersection = model.createIntersectionClass(null, restrMembers);
				restrClass.addEquivalentClass(restrIntersection);
			} else {
				restrClass.addEquivalentClass(restr);
			}

		}
		if (superClass != null) {
			assert superClass instanceof OntClassAdapter;
			restrClass.addSuperClass(((OntClassAdapter) superClass).getModel());
		}
		return new OntClassAdapter(restrClass);
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntModel#createHasValueRestriction(java.lang.String, be.ac.vub.platformkit.kb.IOntClass, java.lang.String, java.lang.String, java.lang.String)
	 */
	public IOntClass createHasValueRestriction(String uri,
			IOntClass superClass, String propertyURI, String datatypeURI,
			String value) throws OntException {
		final OntModel model = getModel();
		final OntClass restrClass = model.createClass(uri);
		if (propertyURI != null && datatypeURI != null && value != null) {
			final Property property = model.getProperty(propertyURI);
			final Set<Resource> restrictionSet = new HashSet<Resource>();

			//remove existing equivalence classes
			restrictionSet.addAll(getOtherRestrictionsFrom(
					restrClass.listEquivalentClasses(), propertyURI));
			restrictionSet.remove(restrClass);
			removeEquivalentClassesFrom(restrClass, restrictionSet);
			//add new equivalence class
			final Literal valueLiteral = model.createTypedLiteral(value, datatypeURI);
			final HasValueRestriction restr = model.createHasValueRestriction(null, property, valueLiteral);
			if (!restrictionSet.isEmpty()) {
				restrictionSet.add(restr);
				final RDFList restrMembers = model.createList(restrictionSet.iterator());
				final IntersectionClass restrIntersection = model.createIntersectionClass(null, restrMembers);
				restrClass.addEquivalentClass(restrIntersection);
			} else {
				restrClass.addEquivalentClass(restr);
			}

		}
		if (superClass != null) {
			assert superClass instanceof OntClassAdapter;
			restrClass.addSuperClass(((OntClassAdapter) superClass).getModel());
		}
		return new OntClassAdapter(restrClass);
	}

	/**
	 * @param restrClass
	 * @param propertyURI
	 * @return any existing property restriction range classes on the property with propertyURI for the ontology class with the given uri
	 */
	protected Set<Resource> getExistingPropertyRestrictionRanges(
			final OntClass restrClass, final String propertyURI) {
		assert restrClass != null;
		return getExistingPropertyRestrictionRangesFrom(
				restrClass.listEquivalentClasses(), propertyURI);
	}

	/**
	 * @param classes
	 * @param propertyURI
	 * @return any existing property restriction range classes within classes on the property with propURI
	 */
	protected Set<Resource> getExistingPropertyRestrictionRangesFrom(
			final Iterator<? extends Resource> classes, final String propertyURI) {
		final Set<Resource> rangeSet = new HashSet<Resource>();
		while (classes.hasNext()) {
			Resource c = classes.next();
			if (c.canAs(IntersectionClass.class)) {
				IntersectionClass inters = c.as(IntersectionClass.class);
				rangeSet.addAll(getExistingPropertyRestrictionRangesFrom(inters.listOperands(), propertyURI));
			} else if (c.canAs(UnionClass.class)) {
				UnionClass union = c.as(UnionClass.class);
				rangeSet.addAll(getExistingPropertyRestrictionRangesFrom(union.listOperands(), propertyURI));
			} else if (c.canAs(SomeValuesFromRestriction.class)) {
				SomeValuesFromRestriction restr = c.as(SomeValuesFromRestriction.class);
				if (restr.getOnProperty().getURI().equals(propertyURI)) {
					rangeSet.add(restr.getSomeValuesFrom());
				}
			}
		}
		return rangeSet;
	}

	/**
	 * @param classes
	 * @param propertyURI
	 * @return the restrictions in classes that are not property restrictions on the property with propertyURI
	 */
	protected Set<Resource> getOtherRestrictionsFrom(
			final Iterator<? extends RDFNode> classes, final String propertyURI) {
		final Set<Resource> otherRestrictions = new HashSet<Resource>();
		while (classes.hasNext()) {
			RDFNode desc = classes.next();
			if (desc.canAs(IntersectionClass.class)) {
				IntersectionClass ic = desc.as(IntersectionClass.class);
				otherRestrictions.addAll(
						getOtherRestrictionsFrom(ic.getOperands().iterator(), propertyURI));
			} else if (!isPropertyRestrictionOn(desc, propertyURI)) {
				otherRestrictions.add(desc.as(Resource.class));
			}
		}
		return otherRestrictions;
	}

	/**
	 * @param desc
	 * @param propertyURI
	 * @return <code>true</code> iff desc is, or contains, a property restriction on the property with propertyURI
	 */
	protected boolean isPropertyRestrictionOn(RDFNode desc, String propertyURI) {
		if (desc.canAs(IntersectionClass.class)) {
			final IntersectionClass inters = desc.as(IntersectionClass.class);
			for (Iterator<RDFNode> o = inters.getOperands().iterator(); o.hasNext();) {
				RDFNode sub = o.next();
				if (isPropertyRestrictionOn(sub, propertyURI)) {
					return true;
				}
			}
		} else if (desc.canAs(Restriction.class)) {
			final OntProperty propExp = desc.as(Restriction.class).getOnProperty();
			if (!propExp.isAnon()) {
				if (propertyURI.equals(propExp.getURI())) {
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
	protected boolean mergeClassIntoRange(final OntClass owlClass, 
			final Set<Resource> range, boolean rangeIsUnion) {

		final OntModel model = getModel();
		final String owlClassName = owlClass.getLocalName();

		boolean merged = false;
		final Set<Resource> newEntries = new HashSet<Resource>();
		final Set<Resource> unionEntries = new HashSet<Resource>();

		//find least specific superclass in range
		for (final Iterator<Resource> oc = range.iterator(); oc.hasNext();) {
			Resource otherClass = oc.next();
			if (owlClass.getURI().equals(otherClass.getURI()) || owlClass.hasSuperClass(otherClass)) {
				//discard owlClass
				merged = true;
			} else if (owlClass.hasSubClass(otherClass)) {
				//replace range entry by owlClass
				oc.remove();
				if (otherClass.isAnon()) {
					removeNode(otherClass, range);
				}
				newEntries.add(owlClass);
				merged = true;
			} else if (otherClass.canAs(UnionClass.class)) {
				//merge into class union
				UnionClass unionClass = otherClass.as(UnionClass.class);
				Set<Resource> ops = new HashSet<Resource>();
				for (Iterator<? extends Resource> o = unionClass.listOperands(); o.hasNext();) {
					ops.add(o.next());
				}
				if (mergeClassIntoRange(owlClass, ops, true)) {
					//replace by new union if ops.size() > 1
					oc.remove();
					removeNode(unionClass, range);
					if (ops.size() > 1) {
						newEntries.add(model.createUnionClass(
								null, model.createList(ops.iterator())));
					} else {
						newEntries.addAll(ops);
					}
					merged = true;
				}
			} else if (owlClassName.equals(otherClass.getLocalName())) {
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
				newEntries.add(model.createUnionClass(
						null, model.createList(unionEntries.iterator())));
			}
		}

		range.addAll(newEntries);
		return merged;
	}

	/**
	 * @param model the model to set
	 */
	protected void setModel(OntModel model) {
		this.model = model;
	}

	/**
	 * @return the model
	 */
	protected OntModel getModel() {
		return model;
	}

}
