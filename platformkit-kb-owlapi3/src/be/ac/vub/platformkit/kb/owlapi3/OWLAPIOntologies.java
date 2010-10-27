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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;
import be.ac.vub.platformkit.kb.AbstractOntologies;
import be.ac.vub.platformkit.kb.BaseOntologyProvider;
import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntModel;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.IOntologyProvider;
import be.ac.vub.platformkit.kb.util.OntException;
import be.ac.vub.platformkit.logging.PlatformkitLogger;
import be.ac.vub.platformkit.registry.PlatformkitRegistry;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

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
	private Map<String, IOntModel> localOntologies = new HashMap<String, IOntModel>();
	private String dlReasonerId = "uk.ac.manchester.cs.factplusplus.owlapi.Reasoner"; //$NON-NLS-1$
	private boolean dlReasonerAttached;

	/**
	 * Creates a new {@link OWLAPIOntologies}.
	 * @throws IOException if the local ontology mapping could not be read.
	 */
	public OWLAPIOntologies() throws IOException {
		setMgr(OWLManager.createOWLOntologyManager());
		final OWLOntologyManager mgr = getMgr();
		final IRI localInfNs = IRI.create(IOntologies.LOCAL_INF_NS);
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
		final IOntologyProvider[] providers = PlatformkitRegistry.INSTANCE.getOntologyProviders();
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
		final OWLOntology ont = mgr.loadOntologyFromOntologyDocument(new StreamDocumentSource(resource));
		localOntologies.put(ont.getOntologyID().getOntologyIRI().toString(), new OWLOntologyAdapter(ont, this));
		PlatformkitLogger.logger.info(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.addingLocalOnt"), 
				ont.getOntologyID().getOntologyIRI())); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#attachDLReasoner()
	 */
	public void attachDLReasoner() throws OntException {
		if (dlReasonerAttached) {
			PlatformkitLogger.logger.warning(
					PlatformkitOWLAPIResources.getString("OWLAPIOntologies.dlAlreadyAttached")); //$NON-NLS-1$
			return;
		}
		final String id = getDlReasonerId();
		if ("uk.ac.manchester.cs.factplusplus.owlapi.Reasoner".equals(id)) {
			attachFactPPReasoner();
		} else if ("com.clarkparsia.pellet.owlapiv3.PelletReasoner".equals(id)) {
			attachPelletReasoner();
		} else {
			throw new OntException(String.format(
					PlatformkitOWLAPIResources.getString("OWLAPIOntologies.reasonerNotFound"), 
					id)); //$NON-NLS-1$
		}
		dlReasonerAttached = true;
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#selectDLReasoner(java.lang.String)
	 */
	public void selectDLReasoner(String id) throws OntException {
		dlReasonerId = id;
	}

	/**
	 * @return the dlReasonerId
	 */
	public String getDlReasonerId() {
		return dlReasonerId;
	}

	/**
	 * Attaches the Pellet DL reasoner.
	 * @throws OntException 
	 */
	public void attachPelletReasoner() throws OntException {
		PlatformkitLogger.logger.info(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.attachingPellet")); //$NON-NLS-1$
		final OWLOntology ont = getInstances() != null ? getInstances().getModel() : getBaseOntology().getModel();
		try {
			final OWLReasonerFactory fact = new PelletReasonerFactory();
			reasoner = fact.createReasoner(ont);
			setReasoner(reasoner);
		} catch (Exception e) {
			throw new OntException(e);
		}
	}

	/**
	 * Attaches the Fact++ DL reasoner.
	 * @throws OntException 
	 */
	public void attachFactPPReasoner() throws OntException {
		PlatformkitLogger.logger.info(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.attachingFactPP")); //$NON-NLS-1$
		final OWLOntology ont = getInstances() != null ? getInstances().getModel() : getBaseOntology().getModel();
		try {
			final OWLReasonerFactory fact = new FaCTPlusPlusReasonerFactory();
			final OWLReasoner reasoner = fact.createReasoner(ont);
			setReasoner(reasoner);
		} catch (Exception e) {
			throw new OntException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#attachTransitiveReasoner()
	 */
	public void attachTransitiveReasoner() throws OntException {
		//TODO there is no transitive reasoner for OWLAPI
		attachDLReasoner();
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
			NodeSet<OWLClass> superCs = reasoner.getSuperClasses(owlClass, false);
			for (Node<OWLClass> scs : superCs) {
				supers.addAll(scs.getEntities());
			}
			Node<OWLClass> equivs = reasoner.getEquivalentClasses(owlClass);
			supers.removeAll(equivs.getEntities());
			PlatformkitLogger.logger.info(String.format(
					PlatformkitOWLAPIResources.getString("OWLAPIOntologies.equivToSubOf"), 
					forClass, 
					equivs, 
					supers)); //$NON-NLS-1$
			getEquivClasses().put(owlClass, equivs.getEntities());
			getSuperClasses().put(owlClass, supers);
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
			if (!reasoner.isConsistent()) {
				throw new OntException(String.format(
						PlatformkitOWLAPIResources.getString("OWLAPIOntologies.ontInconsistent"), 
						getInstances() == null ? getBaseOntology() : getInstances())); //$NON-NLS-1$
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
		dlReasonerAttached = false;
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
		final Set<OWLClass> clsAxs = getBaseOntology().getModel().getClassesInSignature();
		final List<IOntClass> namedClasses = new ArrayList<IOntClass>(clsAxs.size());
		for (OWLClass cl : clsAxs) {
			namedClasses.add(new OWLClassAdapter(cl, this));
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
			final OWLOntology ont = mgr.loadOntologyFromOntologyDocument(IRI.create(url));
			setInstances(new OWLOntologyAdapter(ont, this));
			addImports(getInstances(), getBaseOntology());
			if (getReasoner() != null) {
				detachReasoner();
				attachDLReasoner();
			}
		} catch (OWLException e) {
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
			final OWLOntology ont = mgr.loadOntologyFromOntologyDocument(new StreamDocumentSource(in));
			setInstances(new OWLOntologyAdapter(ont, this));
			addImports(getInstances(), getBaseOntology());
			if (getReasoner() != null) {
				detachReasoner();
				attachDLReasoner();
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
		final OWLReasoner reasoner = getReasoner();
		final OWLOntologyManager mgr = getMgr();
		final IRI iri = IRI.create(url);
		try {
			final OWLOntology ont = mgr.loadOntologyFromOntologyDocument(iri);
			mergeOntology(ont);
			mgr.removeOntology(ont); //remove after merging
			if (reasoner != null) {
				reasoner.flush();
			}
		} catch (OWLException e) {
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
		final OWLReasoner reasoner = getReasoner();
		final OWLOntologyManager mgr = getMgr();
		try {
			final OWLOntology ont = mgr.loadOntologyFromOntologyDocument(new StreamDocumentSource(in));
			mergeOntology(ont);
			mgr.removeOntology(ont); //remove after merging
			if (reasoner != null) {
				reasoner.flush();
			}
		} catch (OWLException e) {
			throw new OntException(e);
		}
		notifyOntologyChanged();
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadBaseOntology(java.io.InputStream)
	 */
	public void loadBaseOntology(InputStream in) throws OntException {
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.loadingBaseOntFrom"), 
				in)); //$NON-NLS-1$
		final OWLReasoner reasoner = getReasoner();
		final OWLOntologyManager mgr = getMgr();
		try {
			mgr.removeOntology(getBaseOntology().getModel());
			final OWLOntology ont = mgr.loadOntologyFromOntologyDocument(new StreamDocumentSource(in));
			if (!ont.getOntologyID().getOntologyIRI().toString().equals(LOCAL_INF_NS)) {
				throw new OntException(String.format(
						PlatformkitOWLAPIResources.getString("OWLAPIOntologies.baseOntMustHaveIRI"), 
						LOCAL_INF_NS,
						ont.getOntologyID().getOntologyIRI().toString()));
			}
			setBaseOntology(new OWLOntologyAdapter(ont, this));
			if (reasoner != null) {
				detachReasoner();
				attachDLReasoner();
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
			mgr.saveOntology(getBaseOntology().getModel(), new StreamDocumentTarget(out));
		} catch (OWLException e) {
			throw new OntException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#unloadInstances()
	 */
	public void unloadInstances() throws OntException {
		final OWLOntologyAdapter instances = getInstances();
		if (instances != null) {
			final OWLReasoner reasoner = getReasoner();
			final OWLOntologyManager mgr = getMgr();
			final OWLOntology model = instances.getModel();
			PlatformkitLogger.logger.fine(PlatformkitOWLAPIResources.getString("OWLAPIOntologies.unloadingInstanceOnt")); //$NON-NLS-1$
			if (reasoner != null) {
				detachReasoner();
				attachDLReasoner();
			}
			mgr.removeOntology(model);
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
		addEquivalentClasses(owlClass, equivCs);
		addSuperClasses(owlClass, superCs);
		removeSuperClasses(owlClass, obsoleteCs);
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#createNewOntology(java.lang.String)
	 */
	public IOntModel createNewOntology(String url) throws OntException {
		try {
			final IOntModel ont = new OWLOntologyAdapter(getMgr().createOntology(IRI.create(url)), this);
			addAllImports(ont);
			return ont;
		} catch (OWLOntologyCreationException e) {
			throw new OntException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#unloadOntology(be.ac.vub.platformkit.kb.IOntModel)
	 */
	public void unloadOntology(IOntModel ont) throws OntException {
		getMgr().removeOntology(((OWLOntologyAdapter) ontology).getModel());
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getLocalOntologies()
	 */
	public Collection<IOntModel> getLocalOntologies() {
		return localOntologies.values();
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getLocalOntology(java.lang.String)
	 */
	public IOntModel getLocalOntology(String uri) {
		return localOntologies.get(uri);
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadSingleOnt(java.io.InputStream)
	 */
	public IOntModel loadSingleOnt(InputStream in) throws OntException {
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitOWLAPIResources.getString("OWLAPIOntologies.loadingOntFrom"), 
				in)); //$NON-NLS-1$
		try {
			final OWLOntology ont = mgr.loadOntologyFromOntologyDocument(new StreamDocumentSource(in));
			return new OWLOntologyAdapter(ont, this);
		} catch (OWLException e) {
			throw new OntException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.AbstractOntologies#addImports(be.ac.vub.platformkit.kb.IOntModel, java.io.InputStream)
	 */
	@Override
	protected void addImports(IOntModel ont, IOntModel importedOnt) throws OntException {
		try {
			assert ont instanceof OWLOntologyAdapter;
			assert importedOnt instanceof OWLOntologyAdapter;
			final OWLOntology owlOnt = ((OWLOntologyAdapter) ont).getModel();
			final OWLOntology owlImportedOnt = ((OWLOntologyAdapter) importedOnt).getModel();
			final OWLOntologyManager mgr = getMgr();
			final OWLDataFactory factory = mgr.getOWLDataFactory();
			final OWLImportsDeclaration importsDecl = factory.getOWLImportsDeclaration(
					owlImportedOnt.getOntologyID().getOntologyIRI());
			mgr.applyChange(new AddImport(owlOnt, importsDecl));
		} catch (OWLOntologyChangeException e) {
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
		final OWLOntologyManager mgr = getMgr();
		for (OWLImportsDeclaration impDecl : ont.getImportsDeclarations()) {
			mgr.applyChange(new AddImport(model, impDecl));
		}
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
	throws OWLOntologyChangeException {
		final OWLOntologyManager mgr = getMgr();
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		final OWLOntology model = getBaseOntology().getModel();
		Set<OWLEquivalentClassesAxiom> ecAxs = model.getEquivalentClassesAxioms(forClass);
		for (OWLClass c : equivCs) {
			boolean isEquiv = false;
			for (OWLEquivalentClassesAxiom ecAx : ecAxs) {
				if (ecAx.contains(c)) {
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
	private void addSuperClasses(OWLClass forClass, Collection<OWLClass> superCs) throws OWLOntologyChangeException {
		final OWLOntologyManager mgr = getMgr();
		final OWLDataFactory factory = mgr.getOWLDataFactory();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		Set<OWLSubClassOfAxiom> scAxs = getBaseOntology().getModel().getSubClassAxiomsForSubClass(forClass);
		for (OWLClass c : superCs) {
			boolean isSubclass = false;
			for (OWLSubClassOfAxiom scAx : scAxs) {
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
				OWLAxiom axiom = factory.getOWLSubClassOfAxiom(forClass, c);
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
	private void removeSuperClasses(OWLClass forClass, Collection<OWLClass> superCs) throws OWLOntologyChangeException {
		final OWLOntologyManager mgr = getMgr();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		for (OWLClass c : superCs) {
			boolean isObsolete = false;
			Set<OWLSubClassOfAxiom> scAxs = getBaseOntology().getModel().getSubClassAxiomsForSubClass(forClass);
			for (OWLSubClassOfAxiom scAx : scAxs) {
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
