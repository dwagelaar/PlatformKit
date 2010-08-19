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
import be.ac.vub.platformkit.logging.PlatformkitLogger;
import be.ac.vub.platformkit.registry.PlatformkitRegistry;

/**
 * The OWLAPI version of the ontology repository for the PlatformKit.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class OWLAPIOntologies extends AbstractOntologies {

	private OWLOntologyManager mgr;
	private OWLReasoner reasoner = null;
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
		setMgr(OWLManager.createOWLOntologyManager());
		final OWLOntologyManager mgr = getMgr();
		final URI localInfNs = URI.create(IOntologies.LOCAL_INF_NS);
		try {
			setBaseOntology(new OWLOntologyAdapter(mgr.createOntology(localInfNs), this));
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
		final OWLOntologyManager mgr = getMgr();
		final OWLOntology ont = mgr.loadOntology(new StreamInputSource(resource));
		PlatformkitLogger.logger.info(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.addingLocalOnt"), 
				ont.getURI())); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#attachDIGReasoner()
	 */
	public void attachDIGReasoner() {
		OWLReasoner reasoner = getReasoner();
		if (reasoner != null) {
			if (reasoner instanceof DIGReasoner) {
				PlatformkitLogger.logger.warning(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.digAlreadyAttached")); //$NON-NLS-1$
				return;
			}
			detachReasoner();
		}
		PlatformkitLogger.logger.info(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.attachingDigAt"), 
				getReasonerUrl())); //$NON-NLS-1$
		try {
			DIGReasonerPreferences.getInstance().setReasonerURL(new URL(getReasonerUrl()));
			final OWLOntologyManager mgr = getMgr();
			reasoner = new DIGReasoner(mgr);
			setReasoner(reasoner);
			reasoner.loadOntologies(mgr.getImportsClosure(getBaseOntology().getModel()));
			if (getInstances() != null) {
				reasoner.loadOntologies(mgr.getImportsClosure(getInstances().getModel()));
			}
		} catch (OWLException e) {
			PlatformkitLogger.logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (MalformedURLException e) {
			PlatformkitLogger.logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#attachPelletReasoner()
	 */
	public void attachPelletReasoner() {
		OWLReasoner reasoner = getReasoner();
		if (reasoner != null) {
			if (reasoner instanceof org.mindswap.pellet.owlapi.Reasoner) {
				PlatformkitLogger.logger.warning(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.pelletAlreadyAttached")); //$NON-NLS-1$
				return;
			}
			detachReasoner();
		}
		PlatformkitLogger.logger.info(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.attachingPellet")); //$NON-NLS-1$
		final OWLOntologyManager mgr = getMgr();
		reasoner = new org.mindswap.pellet.owlapi.Reasoner(mgr);
		setReasoner(reasoner);
		try {
			reasoner.loadOntologies(mgr.getImportsClosure(getBaseOntology().getModel()));
			if (getInstances() != null) {
				reasoner.loadOntologies(mgr.getImportsClosure(getInstances().getModel()));
			}
		} catch (OWLException e) {
			PlatformkitLogger.logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
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
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.buildingHierarchyFor"), 
				forClass)); //$NON-NLS-1$
		final OWLClass owlClass = ((OWLClassAdapter)forClass).getModel();
		final Set<OWLClass> supers = new HashSet<OWLClass>();
		final OWLReasoner reasoner = getReasoner();
		if (reasoner != null) {
			try {
				Set<Set<OWLClass>> superCs = reasoner.getAncestorClasses(owlClass);
				for (Set<OWLClass> scs : superCs) {
					supers.addAll(scs);
				}
				Set<OWLClass> equivs = reasoner.getEquivalentClasses(owlClass);
				supers.removeAll(equivs);
				PlatformkitLogger.logger.info(String.format(
						PlatformkitOWLAPIResources.getString("OWLAPIOntologies.equivToSubOf"), 
						forClass, 
						equivs, 
						supers)); //$NON-NLS-1$
				getEquivClasses().put(owlClass, equivs);
				getSuperClasses().put(owlClass, supers);
			} catch (OWLReasonerException e) {
				PlatformkitLogger.logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#checkConsistency()
	 */
	public void checkConsistency() throws OntException {
		PlatformkitLogger.logger.info(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.checkingConsistency")); //$NON-NLS-1$
		final OWLReasoner reasoner = getReasoner();
		if (reasoner != null) {
			try {
				if (!reasoner.isConsistent(getBaseOntology().getModel())) {
					throw new OntException(String.format(
							PlatformkitOWLAPIResources.getString("OWLAPIOntologies.ontInconsistent"), 
							getBaseOntology())); //$NON-NLS-1$
				}
				if ((getInstances() != null) ? !reasoner.isConsistent(getInstances().getModel()) : false) {
					throw new OntException(String.format(
							PlatformkitOWLAPIResources.getString("OWLAPIOntologies.ontInconsistent"), 
							getInstances())); //$NON-NLS-1$
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
		final OWLReasoner reasoner = getReasoner();
		if (reasoner == null) {
			PlatformkitLogger.logger.warning(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.alreadyDetached")); //$NON-NLS-1$
			return;
		}
		PlatformkitLogger.logger.info(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.detaching")); //$NON-NLS-1$
		setReasoner(null);
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getBaseOntology()
	 */
	public OWLOntologyAdapter getBaseOntology() {
		return ontology;
	}

	/**
	 * @param ontology the ontology to set
	 */
	protected void setBaseOntology(OWLOntologyAdapter ontology) {
		this.ontology = ontology;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getInstances()
	 */
	public OWLOntologyAdapter getInstances() {
		return instances;
	}

	/**
	 * @param instances the instances to set
	 */
	protected void setInstances(OWLOntologyAdapter instances) {
		this.instances = instances;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getLocalNamedClasses()
	 */
	public List<IOntClass> getLocalNamedClasses() {
		List<IOntClass> namedClasses = new ArrayList<IOntClass>();
		Iterator<OWLClassAxiom> clsAxs = getBaseOntology().getModel().getClassAxioms().iterator();
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
		return getBaseOntology();
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadInstances(java.lang.String)
	 */
	public void loadInstances(String url) throws OntException {
		unloadInstances();
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.loadingInstanceOntFrom"), 
				url)); //$NON-NLS-1$
		try {
			final OWLOntologyManager mgr = getMgr();
			OWLOntology ont = mgr.loadOntology(new PhysicalURIInputSource(new URI(url)));
			setInstances(new OWLOntologyAdapter(ont, this));
			final OWLReasoner reasoner = getReasoner();
			if (reasoner != null) {
				Set<OWLOntology> load = new HashSet<OWLOntology>();
				load.addAll(mgr.getImportsClosure(getInstances().getModel()));
				load.removeAll(mgr.getImportsClosure(getBaseOntology().getModel()));
				reasoner.loadOntologies(load);
			}
		} catch (OWLException e) {
			throw new OntException(e);
		} catch (URISyntaxException e) {
			throw new OntException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadInstances(java.io.InputStream)
	 */
	public void loadInstances(InputStream in) throws OntException {
		unloadInstances();
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.loadingInstanceOntFrom"), 
				in)); //$NON-NLS-1$
		try {
			final OWLOntologyManager mgr = getMgr();
			OWLOntology ont = mgr.loadOntology(new StreamInputSource(in));
			setInstances(new OWLOntologyAdapter(ont, this));
			final OWLReasoner reasoner = getReasoner();
			if (reasoner != null) {
				Set<OWLOntology> load = new HashSet<OWLOntology>();
				load.addAll(mgr.getImportsClosure(getInstances().getModel()));
				load.removeAll(mgr.getImportsClosure(getBaseOntology().getModel()));
				reasoner.loadOntologies(load);
			}
		} catch (OWLException e) {
			throw new OntException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadOntology(java.lang.String)
	 */
	public void loadOntology(String url) throws OntException {
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.loadingOntFrom"), 
				url)); //$NON-NLS-1$
		try {
			final OWLReasoner reasoner = getReasoner();
			final OWLOntologyManager mgr = getMgr();
			if (reasoner != null) {
				reasoner.clearOntologies();
			}
			OWLOntology ont = mgr.loadOntology(new PhysicalURIInputSource(new URI(url)));
			mergeOntology(ont);
			if (reasoner != null) {
				reasoner.loadOntologies(mgr.getImportsClosure(getBaseOntology().getModel()));
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
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.loadingOntFrom"), 
				in)); //$NON-NLS-1$
		try {
			final OWLReasoner reasoner = getReasoner();
			final OWLOntologyManager mgr = getMgr();
			if (reasoner != null) {
				reasoner.clearOntologies();
			}
			OWLOntology ont = mgr.loadOntology(new StreamInputSource(in));
			mergeOntology(ont);
			if (reasoner != null) {
				reasoner.loadOntologies(mgr.getImportsClosure(getBaseOntology().getModel()));
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
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.pruningHierarchyFor"), 
				forClass)); //$NON-NLS-1$
		OWLClass owlClass = ((OWLClassAdapter)forClass).getModel();
		Set<OWLClass> obsolete = new HashSet<OWLClass>();
		Set<OWLClass> superCs = getSuperClasses().get(owlClass);
		if (superCs == null) {
			throw new OntException(String.format(
					PlatformkitOWLAPIResources.getString("OWLAPIOntologies.superNotFound"), 
					forClass)); //$NON-NLS-1$
		}
		for (OWLClass sc : superCs) {
			Set<OWLClass> superSuperCs = getSuperClasses().get(sc);
			if (superSuperCs != null) {
				obsolete.addAll(superSuperCs);
			}
		}
		PlatformkitLogger.logger.info(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.obsoleteSuperOf"), 
				obsolete, 
				forClass)); //$NON-NLS-1$
		getObsoleteSuperClasses().put(owlClass, obsolete);        
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#saveOntology(java.io.OutputStream)
	 */
	public void saveOntology(OutputStream out) throws OntException {
		try {
			final OWLOntologyManager mgr = getMgr();
			mgr.saveOntology(getBaseOntology().getModel(), new StreamOutputTarget(out));
		} catch (OWLException e) {
			throw new OntException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#unloadInstances()
	 */
	public void unloadInstances() {
		final OWLOntologyAdapter instances = getInstances();
		if (instances != null) {
			final OWLReasoner reasoner = getReasoner();
			final OWLOntologyManager mgr = getMgr();
			final OWLOntology model = instances.getModel();
			PlatformkitLogger.logger.fine(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.unloadingInstanceOnt")); //$NON-NLS-1$
			if (reasoner != null) {
				Set<OWLOntology> unload = new HashSet<OWLOntology>();
				unload.addAll(mgr.getImportsClosure(model));
				unload.removeAll(mgr.getImportsClosure(getBaseOntology().getModel()));
				try {
					reasoner.unloadOntologies(unload);
				} catch (OWLReasonerException e) {
					PlatformkitLogger.logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
				}
			}
			mgr.removeOntology(model.getURI());
			setInstances(null);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#updateHierarchy(be.ac.vub.platformkit.kb.IOntClass)
	 */
	public void updateHierarchy(IOntClass forClass) throws OntException {
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.updatingHierarchyFor"), 
				forClass)); //$NON-NLS-1$
		OWLClass owlClass = ((OWLClassAdapter)forClass).getModel();
		Set<OWLClass> equivCs = getEquivClasses().get(owlClass);
		if (equivCs == null) {
			throw new OntException(String.format(
					PlatformkitOWLAPIResources.getString("OWLAPIOntologies.equivNotFound"),
					forClass)); //$NON-NLS-1$
		}
		Set<OWLClass> superCs = getSuperClasses().get(owlClass);
		if (superCs == null) {
			throw new OntException(String.format(
					PlatformkitOWLAPIResources.getString("OWLAPIOntologies.superNotFound"),
					forClass)); //$NON-NLS-1$
		}
		Set<OWLClass> obsoleteCs = getObsoleteSuperClasses().get(owlClass);
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
		final OWLOntology model = getBaseOntology().getModel();
		final List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		for (OWLAxiom ax : ont.getAxioms()) {
			changes.add(new AddAxiom(model, ax));
		}
		getMgr().applyChanges(changes);
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
		final OWLOntologyManager mgr = getMgr();
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		final OWLOntology model = getBaseOntology().getModel();
		Set<OWLEquivalentClassesAxiom> ecAxs = model.getEquivalentClassesAxioms(forClass);
		for (OWLClass c : equivCs) {
			boolean isEquiv = false;
			for (OWLEquivalentClassesAxiom ecAx : ecAxs) {
				if (ecAx.getDescriptions().contains(c)) {
					isEquiv = true;
					break;
				}
			}
			if (!isEquiv) {
				PlatformkitLogger.logger.info(String.format(
						PlatformkitOWLAPIResources.getString("OWLAPIOntologies.addingAsEquivTo"),
						c, 
						forClass)); //$NON-NLS-1$
				OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(c, forClass);
				changes.add(new AddAxiom(model, axiom));
			} else {
				PlatformkitLogger.logger.fine(String.format(
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
		final OWLOntologyManager mgr = getMgr();
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		Set<OWLSubClassAxiom> scAxs = getBaseOntology().getModel().getSubClassAxiomsForLHS(forClass);
		for (OWLClass c : superCs) {
			boolean isSubclass = false;
			for (OWLSubClassAxiom scAx : scAxs) {
				if (scAx.getSuperClass().equals(c)) {
					isSubclass = true;
					break;
				}
			}
			if (!isSubclass) {
				PlatformkitLogger.logger.info(String.format(
						PlatformkitOWLAPIResources.getString("OWLAPIOntologies.addingAsSuperTo"),
						c, 
						forClass)); //$NON-NLS-1$
				OWLAxiom axiom = factory.getOWLSubClassAxiom(forClass, c);
				changes.add(new AddAxiom(getBaseOntology().getModel(), axiom));
			} else {
				PlatformkitLogger.logger.fine(String.format(
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
		final OWLOntologyManager mgr = getMgr();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		for (OWLClass c : superCs) {
			boolean isObsolete = false;
			Set<OWLSubClassAxiom> scAxs = getBaseOntology().getModel().getSubClassAxiomsForLHS(forClass);
			for (OWLSubClassAxiom scAx : scAxs) {
				if (scAx.getSuperClass().equals(c)) {
					PlatformkitLogger.logger.info(String.format(
							PlatformkitOWLAPIResources.getString("OWLAPIOntologies.removingAsSuperFrom"),
							c, 
							forClass)); //$NON-NLS-1$
					changes.add(new RemoveAxiom(getBaseOntology().getModel(), scAx));
					isObsolete = true;
					break;
				}
			}
			if (!isObsolete) {
				PlatformkitLogger.logger.fine(String.format(
						PlatformkitOWLAPIResources.getString("OWLAPIOntologies.doesNotHaveSuper"),
						forClass, 
						c)); //$NON-NLS-1$
			}
		}
		mgr.applyChanges(changes);
	}

	/**
	 * @return the reasoner
	 */
	protected OWLReasoner getReasoner() {
		return reasoner;
	}

	/**
	 * @param reasoner the reasoner to set
	 */
	protected void setReasoner(OWLReasoner reasoner) {
		this.reasoner = reasoner;
	}

	/**
	 * @return the mgr
	 */
	protected OWLOntologyManager getMgr() {
		return mgr;
	}

	/**
	 * @param mgr the mgr to set
	 */
	protected void setMgr(OWLOntologyManager mgr) {
		this.mgr = mgr;
	}

	/**
	 * @return the superClasses
	 */
	protected Map<OWLClass, Set<OWLClass>> getSuperClasses() {
		return superClasses;
	}

	/**
	 * @return the equivClasses
	 */
	protected Map<OWLClass, Set<OWLClass>> getEquivClasses() {
		return equivClasses;
	}

	/**
	 * @return the obsoleteSuperClasses
	 */
	protected Map<OWLClass, Set<OWLClass>> getObsoleteSuperClasses() {
		return obsoleteSuperClasses;
	}

}
