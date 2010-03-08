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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.mindswap.pellet.jena.PelletReasoner;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import be.ac.vub.platformkit.kb.AbstractOntologies;
import be.ac.vub.platformkit.kb.BaseOntologyProvider;
import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntModel;
import be.ac.vub.platformkit.kb.IOntologyProvider;
import be.ac.vub.platformkit.kb.util.OntException;
import be.ac.vub.platformkit.registry.PlatformkitRegistry;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.rulesys.OWLMicroReasoner;
import com.hp.hpl.jena.reasoner.transitiveReasoner.TransitiveReasoner;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * The Jena ontology repository for the PlatformKit.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class JenaOntologies extends AbstractOntologies {

	private OntModelAdapter baseOntology;
	private OntModelAdapter ontology;
	private OntModelAdapter instances = null;
	private Set<String> prefixURIs = new HashSet<String>();
	private Map<OntClass, Set<OntClass>> superClasses = new HashMap<OntClass, Set<OntClass>>();
	private Map<OntClass, Set<OntClass>> equivClasses = new HashMap<OntClass, Set<OntClass>>();
	private Map<OntClass, Set<OntClass>> obsoleteSuperClasses = new HashMap<OntClass, Set<OntClass>>();
	private static NamedClassFilter ncFilter = new NamedClassFilter();

	/**
	 * Creates a new {@link JenaOntologies}.
	 * @throws IOException if the local ontology mapping could not be read.
	 */
	public JenaOntologies() throws IOException {
		OntModel ont = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
		ont.setNsPrefix("", LOCAL_INF_NS); //$NON-NLS-1$
		ontology = new OntModelAdapter(ont);
		baseOntology = ontology;
		prefixURIs.add(LOCAL_INF_NS + '#');
		addLocalOntologies();
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#attachDIGReasoner()
	 */
	public void attachDIGReasoner() {
		throw new UnsupportedOperationException(
				PlatformkitJenaResources.getString("JenaOntologies.digNoLongerAvailable")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#attachPelletReasoner()
	 */
	public void attachPelletReasoner() {
		if (ontology.model.getSpecification().getReasoner() != null) {
			if (ontology.model.getSpecification().getReasoner() instanceof PelletReasoner) {
				logger.warning(PlatformkitJenaResources.getString("JenaOntologies.pelletAlreadyAttached")); //$NON-NLS-1$
				return;
			}
			detachReasoner();
		}
		logger.info(PlatformkitJenaResources.getString("JenaOntologies.attachingPellet")); //$NON-NLS-1$
		//Jena is not thread-safe when communicating to the reasoner,
		//so lock all actions that trigger reasoner activity.
		synchronized (JenaOntologies.class) {
			OntModel ont = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, baseOntology.model);
			ont.setNsPrefix("", LOCAL_INF_NS); //$NON-NLS-1$
			ontology = new OntModelAdapter(ont);
		}
		notifyOntologyChanged();
	}

	/**
	 * Attaches the OWL micro reasoner to the knowledgebase.
	 * @see ReasonerRegistry#getOWLMicroReasoner()
	 */
	public void attachOWLReasoner() {
		if (ontology.model.getSpecification().getReasoner() != null) {
			if (ontology.model.getSpecification().getReasoner() instanceof OWLMicroReasoner) {
				logger.warning(PlatformkitJenaResources.getString("JenaOntologies.owlMicroAlreadyAttached")); //$NON-NLS-1$
				return;
			}
			detachReasoner();
		}
		logger.info(PlatformkitJenaResources.getString("JenaOntologies.attachingOwlMicro")); //$NON-NLS-1$
		Reasoner r = ReasonerRegistry.getOWLMicroReasoner();
		OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_DL_MEM);
		spec.setReasoner(r);
		//Jena is not thread-safe when communicating to the reasoner,
		//so lock all actions that trigger reasoner activity.
		synchronized (JenaOntologies.class) {
			OntModel ont = ModelFactory.createOntologyModel(spec, baseOntology.model);
			ont.setNsPrefix("", LOCAL_INF_NS); //$NON-NLS-1$
			ontology = new OntModelAdapter(ont);
		}
		notifyOntologyChanged();
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#attachTransitiveReasoner()
	 * @see ReasonerRegistry#getTransitiveReasoner()
	 */
	public void attachTransitiveReasoner() {
		if (ontology.model.getSpecification().getReasoner() != null) {
			if (ontology.model.getSpecification().getReasoner() instanceof TransitiveReasoner) {
				logger.warning(PlatformkitJenaResources.getString("JenaOntologies.transAlreadyAttached")); //$NON-NLS-1$
				return;
			}
			detachReasoner();
		}
		logger.info(PlatformkitJenaResources.getString("JenaOntologies.attachingTrans")); //$NON-NLS-1$
		Reasoner r = ReasonerRegistry.getTransitiveReasoner();
		OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_DL_MEM);
		spec.setReasoner(r);
		//Jena is not thread-safe when communicating to the reasoner,
		//so lock all actions that trigger reasoner activity.
		synchronized (JenaOntologies.class) {
			OntModel ont = ModelFactory.createOntologyModel(spec, baseOntology.model);
			ont.setNsPrefix("", LOCAL_INF_NS); //$NON-NLS-1$
			ontology = new OntModelAdapter(ont);
		}
		notifyOntologyChanged();
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#detachReasoner()
	 */
	public void detachReasoner() {
		if (ontology.model.getSpecification().getReasoner() == null) {
			logger.warning(PlatformkitJenaResources.getString("JenaOntologies.alreadyDetached")); //$NON-NLS-1$
			return;
		}
		logger.info(PlatformkitJenaResources.getString("JenaOntologies.detaching")); //$NON-NLS-1$
		ontology = baseOntology;
		notifyOntologyChanged();
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getBaseOntology()
	 */
	public IOntModel getBaseOntology() {
		return baseOntology;
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getOntModel()
	 */
	public IOntModel getOntModel() {
		return ontology;
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getInstances()
	 */
	public IOntModel getInstances() {
		return instances;
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadOntology(java.lang.String)
	 */
	public void loadOntology(String url) {
		logger.fine(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.loadingOntFrom"), 
				url)); //$NON-NLS-1$
		OntModel ont = ontology.model;
		ont.read(url);
		registerPrefix(ont);
		notifyOntologyChanged();
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadOntology(java.io.InputStream)
	 */
	public void loadOntology(InputStream in) {
		logger.fine(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.loadingOntFrom"), 
				in)); //$NON-NLS-1$
		OntModel ont = ontology.model;
		ont.read(in, "");
		registerPrefix(ont);
		notifyOntologyChanged();
	}

	/**
	 * Registers the new prefix URI after reading a model.
	 */
	private void registerPrefix(OntModel ont) {
		String prefixURI = ont.getNsPrefixURI("");
		String prefix = ont.getNsURIPrefix(prefixURI);
		logger.info(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.loadedOntWithNS"), 
				prefix, 
				prefixURI)); //$NON-NLS-1$
		prefixURIs.add(prefixURI);
	}

	/**
	 * Unregisters the prefix URI after unloading a model.
	 */
	private void unregisterPrefix(OntModel ont) {
		String prefixURI = ont.getNsPrefixURI("");
		String prefix = ont.getNsURIPrefix(prefixURI);
		logger.info(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.unloadedOntWithNS"), 
				prefix, 
				prefixURI)); //$NON-NLS-1$
		prefixURIs.remove(prefixURI);
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadInstances(java.lang.String)
	 */
	public void loadInstances(String url) {
		unloadInstances();
		logger.fine(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.loadingInstanceOntFrom"), 
				url)); //$NON-NLS-1$
		OntModel inst = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
		inst.read(url);
		instances = new OntModelAdapter(inst);
		ontology.model.add(inst);
		registerPrefix(inst);
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadInstances(java.io.InputStream)
	 */
	public void loadInstances(InputStream in) {
		unloadInstances();
		logger.fine(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.loadingInstanceOntFrom"), 
				in)); //$NON-NLS-1$
		OntModel inst = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
		inst.read(in, ""); //$NON-NLS-1$
		instances = new OntModelAdapter(inst);
		ontology.model.add(inst);
		registerPrefix(inst);
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#unloadInstances()
	 */
	public void unloadInstances() {
		if (instances != null) {
			logger.fine(PlatformkitJenaResources.getString("JenaOntologies.unloadingInstanceOnt")); //$NON-NLS-1$
			unregisterPrefix(instances.model);
			ontology.model.remove(instances.model);
			instances = null;
		}
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#saveOntology(java.io.OutputStream)
	 */
	public void saveOntology(OutputStream out) {
		RDFWriter writer = ontology.model.getWriter(FileUtils.langXML);
		prepareWriter(writer, LOCAL_INF_NS);
		writer.write(ontology.model, out, LOCAL_INF_NS);
	}

	/**
	 * Configures the given RDFWriter.
	 * @param writer
	 * @param namespace XML namespace.
	 */
	private static void prepareWriter(RDFWriter writer, String namespace) {
		writer.setProperty("showXmlDeclaration", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.setProperty("relativeURIs", "same-document"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.setProperty("xmlbase", namespace); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#checkConsistency()
	 */
	public void checkConsistency() throws OntException {
		logger.info(PlatformkitJenaResources.getString("JenaOntologies.checkingConsistency")); //$NON-NLS-1$
		StmtIterator i;
		//Jena is not thread-safe when communicating to the DIG reasoner,
		//so lock all actions that trigger DIG activity.
		synchronized (JenaOntologies.class) {
			i = ontology.model.listStatements(null, OWL.equivalentClass,
					OWL.Nothing);
		}
		while (i.hasNext()) {
			Resource c = i.nextStatement().getSubject();
			if (!c.getURI().equals(OWL.Nothing.getURI())) {
				throw new OntException(String.format(
						PlatformkitJenaResources.getString("JenaOntologies.classUnsatisfiable"), 
						c)); //$NON-NLS-1$
			}
		}
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#addLocalOntologies(be.ac.vub.platformkit.kb.IOntologyProvider)
	 */
	public void addLocalOntologies(IOntologyProvider provider) throws IOException {
		OntDocumentManager dm = ontology.model.getDocumentManager();
		InputStream[] streams = provider.getOntologies();
		for (int k = 0; k < streams.length; k++) {
			addLocalOntology(dm, streams[k]);
		}
	}

	/**
	 * Adds all known local ontologies to the document manager.
	 * @param dm The document manager.
	 * @throws IOException if the local ontology mapping could not be read.
	 */
	private void addLocalOntologies() throws IOException {
		addLocalOntologies(BaseOntologyProvider.INSTANCE);
		IOntologyProvider[] providers = PlatformkitRegistry.INSTANCE.getOntologyProviders();
		for (IOntologyProvider provider : providers) {
			addLocalOntologies(provider);
		}
	}

	/**
	 * Adds a local ontology to the document manager.
	 * @param dm The document manager.
	 * @param resource The local ontology model resource.
	 */
	private void addLocalOntology(OntDocumentManager dm, InputStream resource) {
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
		model.read(resource, null, null);
		String ns = model.getNsPrefixURI("");
		ns = ns.substring(0, ns.length() - 1);
		logger.info(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.addingLocalOnt"), 
				ns)); //$NON-NLS-1$
		dm.addModel(ns, model);
		/*
		 * If the prefix of this ontology is added, then #getLocalNamedClasses()
		 * will also return the classes from this ontology. This has as a consequence
		 * that taxonomy classification will extend into the standard vocabulary
		 * ontologies as well. This currently creates too much overhead and is not
		 * necessary for proper functioning of PlatformKit.
		 */
		//registerPrefix(model);
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getLocalNamedClasses()
	 */
	public List<IOntClass> getLocalNamedClasses() {
		List<IOntClass> namedClasses = new ArrayList<IOntClass>();
		synchronized (JenaOntologies.class) {
			ExtendedIterator<OntClass> ncs = ontology.model.listNamedClasses().filterKeep(ncFilter);
			while (ncs.hasNext()) {
				OntClass c = ncs.next();
				if (hasLocalPrefix(c)) {
					namedClasses.add(new OntClassAdapter(c));
				}
			}
		}
		return namedClasses;
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#updateHierarchy(com.hp.hpl.jena.ontology.OntClass)
	 */
	public void updateHierarchy(IOntClass forClass)
	throws OntException {
		logger.fine(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.updatingHierarchyFor"), 
				forClass)); //$NON-NLS-1$
		OntClass ontClass = ((OntClassAdapter)forClass).model;
		Set<OntClass> equivCs = equivClasses.get(ontClass);
		if (equivCs == null) {
			throw new OntException(String.format(
					PlatformkitJenaResources.getString("JenaOntologies.equivNotFound"), 
					forClass)); //$NON-NLS-1$
		}
		addEquivalentClasses(ontClass, equivCs);
		Set<OntClass> superCs = superClasses.get(ontClass);
		if (superCs == null) {
			throw new OntException(String.format(
					PlatformkitJenaResources.getString("JenaOntologies.superNotFound"), 
					forClass)); //$NON-NLS-1$
		}
		Set<OntClass> obsoleteCs = obsoleteSuperClasses.get(ontClass);
		if (obsoleteCs == null) {
			throw new OntException(String.format(
					PlatformkitJenaResources.getString("JenaOntologies.obsoleteSuperNotFound"), 
					forClass)); //$NON-NLS-1$
		}
		superCs.removeAll(obsoleteCs);
		addSuperClasses(ontClass, superCs);
		removeSuperClasses(ontClass, obsoleteCs);
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#pruneHierarchyMap(com.hp.hpl.jena.ontology.OntClass)
	 */
	public void pruneHierarchyMap(IOntClass forClass)
	throws OntException {
		logger.fine(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.pruningHierarchyFor"), 
				forClass)); //$NON-NLS-1$
		OntClass ontClass = ((OntClassAdapter)forClass).model;
		Set<OntClass> obsolete = new HashSet<OntClass>();
		Set<OntClass> superCs = superClasses.get(ontClass);
		if (superCs == null) {
			throw new OntException(String.format(
					PlatformkitJenaResources.getString("JenaOntologies.superNotFound"), 
					forClass)); //$NON-NLS-1$
		}
		for (Iterator<OntClass> cs = superCs.iterator(); cs.hasNext();) {
			OntClass superC = cs.next();
			Set<OntClass> superSuperCs = superClasses.get(superC);
			if (superSuperCs != null) {
				obsolete.addAll(superSuperCs);
			}
		}
		logger.info(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.obsoleteSuperOf"), 
				obsolete, 
				forClass)); //$NON-NLS-1$
		obsoleteSuperClasses.put(ontClass, obsolete);        
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#buildHierarchyMap(com.hp.hpl.jena.ontology.OntClass)
	 */
	public void buildHierarchyMap(IOntClass forClass)
	throws OntException {
		logger.fine(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.buildingHierarchyFor"), 
				forClass)); //$NON-NLS-1$
		OntClass ontClass = ((OntClassAdapter)forClass).model;
		OntClass baseClass = getBaseClass(ontClass);
		Set<OntClass> equivs = new HashSet<OntClass>();
		Set<OntClass> supers = new HashSet<OntClass>();
		synchronized (JenaOntologies.class) {
			Iterator<OntClass> superCs = ontClass.listSuperClasses().filterKeep(ncFilter);
			while (superCs.hasNext()) {
				OntClass superC = superCs.next();
				try {
					OntClass baseSuperC = getBaseClass(superC);
					supers.add(baseSuperC);
				} catch (NotFoundException nfe) {
					logger.warning(nfe.getMessage());
				}
			}
			Iterator<OntClass> equivCs = ontClass.listEquivalentClasses().filterKeep(ncFilter);
			while (equivCs.hasNext()) {
				OntClass equivC = equivCs.next();
				if (!equivC.equals(forClass)) {
					try {
						OntClass baseEquivC = getBaseClass(equivC);
						equivs.add(baseEquivC);
					} catch (NotFoundException nfe) {
						logger.warning(nfe.getMessage());
					}
				}
			}
		}
		supers.removeAll(equivs);
		logger.info(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.equivToSubOf"), 
				forClass, 
				equivs, 
				supers)); //$NON-NLS-1$
		equivClasses.put(baseClass, equivs);
		superClasses.put(baseClass, supers);
	}

	/**
	 * @param c
	 * @return True if c has a prefix URI of one of the directly loaded models.
	 */
	private boolean hasLocalPrefix(OntClass c) {
		for (Iterator<String> pfxs = prefixURIs.iterator(); pfxs.hasNext();) {
			if (c.getURI().startsWith(pfxs.next())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds given equivalent classes to forClass
	 * @param forClass
	 * @param equivCs
	 */
	private void addEquivalentClasses(OntClass forClass, Collection<OntClass> equivCs) {
		for (Iterator<OntClass> cs = equivCs.iterator(); cs.hasNext();) {
			OntClass c = cs.next();
			synchronized (JenaOntologies.class) {
				if (!forClass.hasEquivalentClass(c)) {
					logger.info(String.format(
							PlatformkitJenaResources.getString("JenaOntologies.addingAsEquivTo"), 
							c, 
							forClass)); //$NON-NLS-1$
					forClass.addEquivalentClass(c);
				} else {
					logger.fine(String.format(
							PlatformkitJenaResources.getString("JenaOntologies.alreadyHasEquiv"), 
							forClass, 
							c)); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Adds given non-obsolete superclasses to forClass
	 * @param forClass
	 * @param superCs
	 */
	private void addSuperClasses(OntClass forClass, Collection<OntClass> superCs) {
		for (Iterator<OntClass> cs = superCs.iterator(); cs.hasNext();) {
			OntClass c = cs.next();
			synchronized (JenaOntologies.class) {
				if (!forClass.hasSuperClass(c)) {
					logger.info(String.format(
							PlatformkitJenaResources.getString("JenaOntologies.addingAsSuperTo"), 
							c, 
							forClass)); //$NON-NLS-1$
					forClass.addSuperClass(c);
				} else {
					logger.fine(String.format(
							PlatformkitJenaResources.getString("JenaOntologies.alreadyHasSuper"), 
							forClass, 
							c)); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Removes given superclasses from forClass.
	 * @param forClass
	 * @param superCs
	 */
	private void removeSuperClasses(OntClass forClass, Collection<OntClass> superCs) {
		for (Iterator<OntClass> scs = superCs.iterator(); scs.hasNext();) {
			OntClass c = scs.next();
			synchronized (JenaOntologies.class) {
				if (forClass.hasSuperClass(c)) {
					logger.info(String.format(
							PlatformkitJenaResources.getString("JenaOntologies.addingAsSuperTo="), 
							c, 
							forClass)); //$NON-NLS-1$
					forClass.removeSuperClass(c);
				} else {
					logger.fine(String.format(
							PlatformkitJenaResources.getString("JenaOntologies.doesNotHaveSuper"), 
							forClass, 
							c)); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * @param c
	 * @return the base model class for the given c.
	 * @throws NotFoundException
	 */
	private OntClass getBaseClass(OntClass c)
	throws NotFoundException {
		OntModel baseModel = baseOntology.model;
		Assert.assertNotNull(baseModel);
		OntClass baseClass;
		synchronized (JenaOntologies.class) {
			baseClass = baseModel.getOntClass(c.getURI());
		}
		if (baseClass == null) {
			throw new NotFoundException(String.format(
					PlatformkitJenaResources.getString("JenaOntologies.notFoundInBaseOnt"), 
					c)); //$NON-NLS-1$
		}
		return baseClass;
	}
}
