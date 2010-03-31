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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.io.PhysicalURIInputSource;
import org.semanticweb.owl.io.StreamInputSource;
import org.semanticweb.owl.io.StreamOutputTarget;
import org.semanticweb.owl.model.AddAxiom;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLClassAxiom;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLEntity;
import org.semanticweb.owl.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChange;
import org.semanticweb.owl.model.OWLOntologyChangeException;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLSubClassAxiom;
import org.semanticweb.owl.model.RemoveAxiom;

import uk.ac.manchester.cs.owl.inference.dig11.DIGReasoner;
import uk.ac.manchester.cs.owl.inference.dig11.DIGReasonerPreferences;
import be.ac.vub.platformkit.kb.AbstractOntologies;
import be.ac.vub.platformkit.kb.BaseOntologyProvider;
import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntModel;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.IOntologyProvider;
import be.ac.vub.platformkit.kb.util.OntException;
import be.ac.vub.platformkit.registry.PlatformkitRegistry;

/**
 * The OWLAPI version of the ontology repository for the PlatformKit.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class OWLAPIOntologies extends AbstractOntologies {

	protected OWLOntologyManager mgr;
	protected OWLReasoner reasoner = null;
	private OWLOntologyAdapter ontology;
	private OWLOntologyAdapter instances = null;
	private Map<OWLClass, Set<OWLClass>> superClasses = new HashMap<OWLClass, Set<OWLClass>>();
	private Map<OWLClass, Set<OWLClass>> equivClasses = new HashMap<OWLClass, Set<OWLClass>>();
	private Map<OWLClass, Set<OWLClass>> obsoleteSuperClasses = new HashMap<OWLClass, Set<OWLClass>>();

	/**
	 * Creates a new {@link OWLAPIOntologies}.
	 * @throws IOException if the local ontology mapping could not be read.
	 */
	public OWLAPIOntologies() throws IOException {
		mgr = OWLManager.createOWLOntologyManager();
		URI localInfNs = URI.create(IOntologies.LOCAL_INF_NS);
		try {
			ontology = new OWLOntologyAdapter(mgr.createOntology(localInfNs), this);
		} catch (OWLOntologyCreationException e) {
			IOException ioe = new IOException(e.getLocalizedMessage());
			ioe.initCause(e);
			throw ioe;
		}
		addLocalOntologies();
	}

	/**
	 * Adds all known local ontologies to the document manager.
	 * @param provider The ontology provider.
	 * @throws IOException if the local ontology mapping could not be read.
	 */
	public void addLocalOntologies(IOntologyProvider provider) throws IOException {
		InputStream[] streams = provider.getOntologies();
		for (int k = 0; k < streams.length; k++) {
			try {
				addLocalOntology(streams[k]);
			} catch (OWLOntologyCreationException e) {
				IOException ioe = new IOException(e.getLocalizedMessage());
				ioe.initCause(e);
				throw ioe;
			}
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
	 * @param resource The local ontology model resource.
	 * @throws OWLOntologyCreationException
	 */
	private void addLocalOntology(InputStream resource) throws OWLOntologyCreationException {
		OWLOntology ont = mgr.loadOntology(new StreamInputSource(resource));
		String ns = ont.getURI().toString();
		logger.info(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.addingLocalOnt"), 
				ns)); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#attachDIGReasoner()
	 */
	public void attachDIGReasoner() {
		if (reasoner != null) {
			if (reasoner instanceof DIGReasoner) {
				logger.warning(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.digAlreadyAttached")); //$NON-NLS-1$
				return;
			}
			detachReasoner();
		}
		logger.info(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.attachingDigAt"), 
				getReasonerUrl())); //$NON-NLS-1$
		try {
			DIGReasonerPreferences.getInstance().setReasonerURL(new URL(getReasonerUrl()));
			reasoner = new DIGReasoner(mgr);
			reasoner.loadOntologies(mgr.getImportsClosure(ontology.model));
			if (instances != null) {
				reasoner.loadOntologies(mgr.getImportsClosure(instances.model));
			}
		} catch (OWLException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (MalformedURLException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#attachPelletReasoner()
	 */
	public void attachPelletReasoner() {
		if (reasoner != null) {
			if (reasoner instanceof org.mindswap.pellet.owlapi.Reasoner) {
				logger.warning(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.pelletAlreadyAttached")); //$NON-NLS-1$
				return;
			}
			detachReasoner();
		}
		logger.info(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.attachingPellet")); //$NON-NLS-1$
		reasoner = new org.mindswap.pellet.owlapi.Reasoner(mgr);
		try {
			reasoner.loadOntologies(mgr.getImportsClosure(ontology.model));
			if (instances != null) {
				reasoner.loadOntologies(mgr.getImportsClosure(instances.model));
			}
		} catch (OWLException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#attachTransitiveReasoner()
	 */
	public void attachTransitiveReasoner() {
		//TODO there is no transitive reasoner for OWLAPI
		attachPelletReasoner();
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#buildHierarchyMap(be.ac.vub.platformkit.kb.IOntClass)
	 */
	public void buildHierarchyMap(IOntClass forClass) throws OntException {
		logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.buildingHierarchyFor"), 
				forClass)); //$NON-NLS-1$
		OWLClass owlClass = ((OWLClassAdapter)forClass).model;
		Set<OWLClass> equivs = new HashSet<OWLClass>();
		Set<OWLClass> supers = new HashSet<OWLClass>();
		if (reasoner != null) {
			try {
				Set<Set<OWLClass>> superCs = reasoner.getAncestorClasses(owlClass);
				for (Set<OWLClass> scs : superCs) {
					supers.addAll(scs);
				}
				equivs = reasoner.getEquivalentClasses(owlClass);
				supers.removeAll(equivs);
				logger.info(String.format(
						PlatformkitOWLAPIResources.getString("OWLAPIOntologies.equivToSubOf"), 
						forClass, 
						equivs, 
						supers)); //$NON-NLS-1$
				equivClasses.put(owlClass, equivs);
				superClasses.put(owlClass, supers);
			} catch (OWLReasonerException e) {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#checkConsistency()
	 */
	public void checkConsistency() throws OntException {
		logger.info(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.checkingConsistency")); //$NON-NLS-1$
		if (reasoner != null) {
			try {
				if (!reasoner.isConsistent(ontology.model)) {
					throw new OntException(String.format(
							PlatformkitOWLAPIResources.getString("OWLAPIOntologies.ontInconsistent"), 
							ontology)); //$NON-NLS-1$
				}
				if ((instances != null) ? !reasoner.isConsistent(instances.model) : false) {
					throw new OntException(String.format(
							PlatformkitOWLAPIResources.getString("OWLAPIOntologies.ontInconsistent"), 
							instances)); //$NON-NLS-1$
				}
			} catch (OWLReasonerException e) {
				throw new OntException(e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#detachReasoner()
	 */
	public void detachReasoner() {
		if (reasoner == null) {
			logger.warning(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.alreadyDetached")); //$NON-NLS-1$
			return;
		}
		logger.info(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.detaching")); //$NON-NLS-1$
		reasoner = null;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getBaseOntology()
	 */
	public IOntModel getBaseOntology() {
		return ontology;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getInstances()
	 */
	public IOntModel getInstances() {
		return instances;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getLocalNamedClasses()
	 */
	public List<IOntClass> getLocalNamedClasses() {
		List<IOntClass> namedClasses = new ArrayList<IOntClass>();
		Iterator<OWLClassAxiom> clsAxs = ontology.model.getClassAxioms().iterator();
		while (clsAxs.hasNext()) {
			OWLClassAxiom clsAx = clsAxs.next();
			for (OWLEntity ent : clsAx.getReferencedEntities()) {
				if (ent.isOWLClass()) {
					namedClasses.add(new OWLClassAdapter(ent.asOWLClass(), this));
				}
			}
		}
		return namedClasses;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getOntModel()
	 */
	public IOntModel getOntModel() {
		return ontology;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadInstances(java.lang.String)
	 */
	public void loadInstances(String url) {
		unloadInstances();
		logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.loadingInstanceOntFrom"), 
				url)); //$NON-NLS-1$
		try {
			OWLOntology ont = mgr.loadOntology(new PhysicalURIInputSource(new URI(url)));
			instances = new OWLOntologyAdapter(ont, this);
			if (reasoner != null) {
				Set<OWLOntology> load = new HashSet<OWLOntology>();
				load.addAll(mgr.getImportsClosure(instances.model));
				load.removeAll(mgr.getImportsClosure(ontology.model));
				reasoner.loadOntologies(load);
			}
		} catch (OWLException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (URISyntaxException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadInstances(java.io.InputStream)
	 */
	public void loadInstances(InputStream in) {
		unloadInstances();
		logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.loadingInstanceOntFrom"), 
				in)); //$NON-NLS-1$
		try {
			OWLOntology ont = mgr.loadOntology(new StreamInputSource(in));
			instances = new OWLOntologyAdapter(ont, this);
			if (reasoner != null) {
				Set<OWLOntology> load = new HashSet<OWLOntology>();
				load.addAll(mgr.getImportsClosure(instances.model));
				load.removeAll(mgr.getImportsClosure(ontology.model));
				reasoner.loadOntologies(load);
			}
		} catch (OWLException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadOntology(java.lang.String)
	 */
	public void loadOntology(String url) throws OntException {
		logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.loadingOntFrom"), 
				url)); //$NON-NLS-1$
		try {
			if (reasoner != null) {
				reasoner.clearOntologies();
			}
			OWLOntology ont = mgr.loadOntology(new PhysicalURIInputSource(new URI(url)));
			mergeOntology(ont);
			if (reasoner != null) {
				reasoner.loadOntologies(mgr.getImportsClosure(ontology.model));
			}
		} catch (OWLException e) {
			throw new OntException(e);
		} catch (URISyntaxException e) {
			throw new OntException(e);
		}
		notifyOntologyChanged();
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadOntology(java.io.InputStream)
	 */
	public void loadOntology(InputStream in) throws OntException {
		logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.loadingOntFrom"), 
				in)); //$NON-NLS-1$
		try {
			if (reasoner != null) {
				reasoner.clearOntologies();
			}
			OWLOntology ont = mgr.loadOntology(new StreamInputSource(in));
			mergeOntology(ont);
			if (reasoner != null) {
				reasoner.loadOntologies(mgr.getImportsClosure(ontology.model));
			}
		} catch (OWLException e) {
			throw new OntException(e);
		}
		notifyOntologyChanged();
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#pruneHierarchyMap(be.ac.vub.platformkit.kb.IOntClass)
	 */
	public void pruneHierarchyMap(IOntClass forClass) throws OntException {
		logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.pruningHierarchyFor"), 
				forClass)); //$NON-NLS-1$
		OWLClass owlClass = ((OWLClassAdapter)forClass).model;
		Set<OWLClass> obsolete = new HashSet<OWLClass>();
		Set<OWLClass> superCs = superClasses.get(owlClass);
		if (superCs == null) {
			throw new OntException(String.format(
					PlatformkitOWLAPIResources.getString("OWLAPIOntologies.superNotFound"), 
					forClass)); //$NON-NLS-1$
		}
		for (OWLClass sc : superCs) {
			Set<OWLClass> superSuperCs = superClasses.get(sc);
			if (superSuperCs != null) {
				obsolete.addAll(superSuperCs);
			}
		}
		logger.info(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.obsoleteSuperOf"), 
				obsolete, 
				forClass)); //$NON-NLS-1$
		obsoleteSuperClasses.put(owlClass, obsolete);        
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#saveOntology(java.io.OutputStream)
	 */
	public void saveOntology(OutputStream out) {
		try {
			mgr.saveOntology(ontology.model, new StreamOutputTarget(out));
		} catch (OWLException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#unloadInstances()
	 */
	public void unloadInstances() {
		if (instances != null) {
			logger.fine(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.unloadingInstanceOnt")); //$NON-NLS-1$
			if (reasoner != null) {
				Set<OWLOntology> unload = new HashSet<OWLOntology>();
				unload.addAll(mgr.getImportsClosure(instances.model));
				unload.removeAll(mgr.getImportsClosure(ontology.model));
				try {
					reasoner.unloadOntologies(unload);
				} catch (OWLReasonerException e) {
					logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
				}
			}
			mgr.removeOntology(instances.model.getURI());
			instances = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#updateHierarchy(be.ac.vub.platformkit.kb.IOntClass)
	 */
	public void updateHierarchy(IOntClass forClass) throws OntException {
		logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.updatingHierarchyFor"), 
				forClass)); //$NON-NLS-1$
		OWLClass owlClass = ((OWLClassAdapter)forClass).model;
		Set<OWLClass> equivCs = equivClasses.get(owlClass);
		if (equivCs == null) {
			throw new OntException(String.format(
					PlatformkitOWLAPIResources.getString("OWLAPIOntologies.equivNotFound"),
					forClass)); //$NON-NLS-1$
		}
		Set<OWLClass> superCs = superClasses.get(owlClass);
		if (superCs == null) {
			throw new OntException(String.format(
					PlatformkitOWLAPIResources.getString("OWLAPIOntologies.superNotFound"),
					forClass)); //$NON-NLS-1$
		}
		Set<OWLClass> obsoleteCs = obsoleteSuperClasses.get(owlClass);
		if (obsoleteCs == null) {
			throw new OntException(String.format(
					PlatformkitOWLAPIResources.getString("OWLAPIOntologies.obsoleteSuperNotFound"),
					forClass)); //$NON-NLS-1$
		}
		superCs.removeAll(obsoleteCs);
		try {
			addEquivalentClasses(owlClass, equivCs);
			addSuperClasses(owlClass, superCs);
			removeSuperClasses(owlClass, obsoleteCs);
		} catch (OWLException e) {
			throw new OntException(e);
		}
	}

	/**
	 * Merges ont with ontology.model. Based on OWLOntologyMerger.
	 * @param ont
	 * @throws OWLOntologyChangeException
	 */
	private void mergeOntology(OWLOntology ont) throws OWLOntologyChangeException {
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		for (OWLAxiom ax : ont.getAxioms()) {
			changes.add(new AddAxiom(ontology.model, ax));
		}
		mgr.applyChanges(changes);
	}

	/**
	 * Adds given equivalent classes to forClass
	 * @param forClass
	 * @param equivCs
	 * @throws OWLOntologyChangeException 
	 * @throws OWLReasonerException 
	 */
	private void addEquivalentClasses(OWLClass forClass, Collection<OWLClass> equivCs) 
	throws OWLOntologyChangeException, OWLReasonerException {
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		Set<OWLEquivalentClassesAxiom> ecAxs = ontology.model.getEquivalentClassesAxioms(forClass);
		for (OWLClass c : equivCs) {
			boolean isEquiv = false;
			for (OWLEquivalentClassesAxiom ecAx : ecAxs) {
				if (ecAx.getDescriptions().contains(c)) {
					isEquiv = true;
					break;
				}
			}
			if (!isEquiv) {
				logger.info(String.format(
						PlatformkitOWLAPIResources.getString("OWLAPIOntologies.addingAsEquivTo"),
						c, 
						forClass)); //$NON-NLS-1$
				OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(c, forClass);
				changes.add(new AddAxiom(ontology.model, axiom));
			} else {
				logger.fine(String.format(
						PlatformkitOWLAPIResources.getString("OWLAPIOntologies.alreadyHasEquiv"),
						forClass, 
						c)); //$NON-NLS-1$
			}
		}
		mgr.applyChanges(changes);
	}

	/**
	 * Adds given non-obsolete superclasses to forClass
	 * @param forClass
	 * @param superCs
	 * @throws OWLOntologyChangeException 
	 * @throws OWLReasonerException 
	 */
	private void addSuperClasses(OWLClass forClass, Collection<OWLClass> superCs) throws OWLOntologyChangeException, OWLReasonerException {
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		Set<OWLSubClassAxiom> scAxs = ontology.model.getSubClassAxiomsForLHS(forClass);
		for (OWLClass c : superCs) {
			boolean isSubclass = false;
			for (OWLSubClassAxiom scAx : scAxs) {
				if (scAx.getSuperClass().equals(c)) {
					isSubclass = true;
					break;
				}
			}
			if (!isSubclass) {
				logger.info(String.format(
						PlatformkitOWLAPIResources.getString("OWLAPIOntologies.addingAsSuperTo"),
						c, 
						forClass)); //$NON-NLS-1$
				OWLAxiom axiom = factory.getOWLSubClassAxiom(forClass, c);
				changes.add(new AddAxiom(ontology.model, axiom));
			} else {
				logger.fine(String.format(
						PlatformkitOWLAPIResources.getString("OWLAPIOntologies.alreadyHasSuper"),
						forClass,
						c)); //$NON-NLS-1$
			}
		}
		mgr.applyChanges(changes);
	}

	/**
	 * Removes given superclasses from forClass.
	 * @param forClass
	 * @param superCs
	 * @throws OWLReasonerException 
	 * @throws OWLOntologyChangeException 
	 */
	private void removeSuperClasses(OWLClass forClass, Collection<OWLClass> superCs) throws OWLReasonerException, OWLOntologyChangeException {
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		for (OWLClass c : superCs) {
			boolean isObsolete = false;
			Set<OWLSubClassAxiom> scAxs = ontology.model.getSubClassAxiomsForLHS(forClass);
			for (OWLSubClassAxiom scAx : scAxs) {
				if (scAx.getSuperClass().equals(c)) {
					logger.info(String.format(
							PlatformkitOWLAPIResources.getString("OWLAPIOntologies.removingAsSuperFrom"),
							c, 
							forClass)); //$NON-NLS-1$
					changes.add(new RemoveAxiom(ontology.model, scAx));
					isObsolete = true;
					break;
				}
			}
			if (!isObsolete) {
				logger.fine(String.format(
						PlatformkitOWLAPIResources.getString("OWLAPIOntologies.doesNotHaveSuper"),
						forClass, 
						c)); //$NON-NLS-1$
			}
		}
		mgr.applyChanges(changes);
	}

}
