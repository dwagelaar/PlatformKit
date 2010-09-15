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

import org.mindswap.pellet.jena.PelletReasoner;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import be.ac.vub.platformkit.kb.AbstractOntologies;
import be.ac.vub.platformkit.kb.BaseOntologyProvider;
import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntModel;
import be.ac.vub.platformkit.kb.IOntologyProvider;
import be.ac.vub.platformkit.kb.util.OntException;
import be.ac.vub.platformkit.logging.PlatformkitLogger;
import be.ac.vub.platformkit.registry.PlatformkitRegistry;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
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
	private Map<String, IOntModel> localOntologies = new HashMap<String, IOntModel>();

	/**
	 * Creates a new {@link JenaOntologies}.
	 * @throws IOException if the local ontology mapping could not be read.
	 */
	public JenaOntologies() throws IOException {
		OntModel ont = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
		ont.setNsPrefix("", LOCAL_INF_NS); //$NON-NLS-1$
		setOntology(new OntModelAdapter(ont));
		setBaseOntology(getOntology());
		getPrefixURIs().add(LOCAL_INF_NS + '#');
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
	public void attachPelletReasoner() throws OntException {
		final Reasoner reasoner = getOntology().getModel().getSpecification().getReasoner();
		if (reasoner != null) {
			if (reasoner instanceof PelletReasoner) {
				PlatformkitLogger.logger.warning(PlatformkitJenaResources.getString("JenaOntologies.pelletAlreadyAttached")); //$NON-NLS-1$
				return;
			}
			detachReasoner();
		}
		PlatformkitLogger.logger.info(PlatformkitJenaResources.getString("JenaOntologies.attachingPellet")); //$NON-NLS-1$
		//Jena is not thread-safe when communicating to the reasoner,
		//so lock all actions that trigger reasoner activity.
		synchronized (JenaOntologies.class) {
			OntModel ont = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, getBaseOntology().getModel());
			ont.setNsPrefix("", LOCAL_INF_NS); //$NON-NLS-1$
			setOntology(new OntModelAdapter(ont));
		}
		notifyOntologyChanged();
	}

	/**
	 * Attaches the OWL micro reasoner to the knowledgebase.
	 * @throws OntException 
	 * @see ReasonerRegistry#getOWLMicroReasoner()
	 */
	public void attachOWLReasoner() throws OntException {
		final Reasoner reasoner = getOntology().getModel().getSpecification().getReasoner();
		if (reasoner != null) {
			if (reasoner instanceof OWLMicroReasoner) {
				PlatformkitLogger.logger.warning(PlatformkitJenaResources.getString("JenaOntologies.owlMicroAlreadyAttached")); //$NON-NLS-1$
				return;
			}
			detachReasoner();
		}
		PlatformkitLogger.logger.info(PlatformkitJenaResources.getString("JenaOntologies.attachingOwlMicro")); //$NON-NLS-1$
		final Reasoner r = ReasonerRegistry.getOWLMicroReasoner();
		final OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_DL_MEM);
		spec.setReasoner(r);
		//Jena is not thread-safe when communicating to the reasoner,
		//so lock all actions that trigger reasoner activity.
		synchronized (JenaOntologies.class) {
			OntModel ont = ModelFactory.createOntologyModel(spec, getBaseOntology().getModel());
			ont.setNsPrefix("", LOCAL_INF_NS); //$NON-NLS-1$
			setOntology(new OntModelAdapter(ont));
		}
		notifyOntologyChanged();
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#attachTransitiveReasoner()
	 * @see ReasonerRegistry#getTransitiveReasoner()
	 */
	public void attachTransitiveReasoner() throws OntException {
		final Reasoner reasoner = getOntology().getModel().getSpecification().getReasoner();
		if (reasoner != null) {
			if (reasoner instanceof TransitiveReasoner) {
				PlatformkitLogger.logger.warning(PlatformkitJenaResources.getString("JenaOntologies.transAlreadyAttached")); //$NON-NLS-1$
				return;
			}
			detachReasoner();
		}
		PlatformkitLogger.logger.info(PlatformkitJenaResources.getString("JenaOntologies.attachingTrans")); //$NON-NLS-1$
		final Reasoner r = ReasonerRegistry.getTransitiveReasoner();
		final OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_DL_MEM);
		spec.setReasoner(r);
		//Jena is not thread-safe when communicating to the reasoner,
		//so lock all actions that trigger reasoner activity.
		synchronized (JenaOntologies.class) {
			OntModel ont = ModelFactory.createOntologyModel(spec, getBaseOntology().getModel());
			ont.setNsPrefix("", LOCAL_INF_NS); //$NON-NLS-1$
			setOntology(new OntModelAdapter(ont));
		}
		notifyOntologyChanged();
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#detachReasoner()
	 */
	public void detachReasoner() throws OntException {
		final Reasoner reasoner = getOntology().getModel().getSpecification().getReasoner();
		if (reasoner == null) {
			PlatformkitLogger.logger.warning(PlatformkitJenaResources.getString("JenaOntologies.alreadyDetached")); //$NON-NLS-1$
			return;
		}
		PlatformkitLogger.logger.info(PlatformkitJenaResources.getString("JenaOntologies.detaching")); //$NON-NLS-1$
		setOntology(getBaseOntology());
		notifyOntologyChanged();
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getBaseOntology()
	 */
	public OntModelAdapter getBaseOntology() {
		return baseOntology;
	}

	/**
	 * @param baseOntology the baseOntology to set
	 */
	protected void setBaseOntology(OntModelAdapter baseOntology) {
		this.baseOntology = baseOntology;
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getOntModel()
	 */
	public IOntModel getOntModel() {
		return getOntology();
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getInstances()
	 */
	public OntModelAdapter getInstances() {
		return instances;
	}

	/**
	 * @param instances the instances to set
	 */
	protected void setInstances(OntModelAdapter instances) {
		this.instances = instances;
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadOntology(java.lang.String)
	 */
	public void loadOntology(String url) throws OntException {
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.loadingOntFrom"), 
				url)); //$NON-NLS-1$
		final OntModel ont = getOntology().getModel();
		ont.read(url);
		registerPrefix(ont);
		notifyOntologyChanged();
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadOntology(java.io.InputStream)
	 */
	public void loadOntology(InputStream in) throws OntException {
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.loadingOntFrom"), 
				in)); //$NON-NLS-1$
		final OntModel ont = getOntology().getModel();
		ont.read(in, "");
		registerPrefix(ont);
		notifyOntologyChanged();
	}

	/**
	 * Registers the new prefix URI after reading a model.
	 */
	private void registerPrefix(OntModel ont) {
		final String prefixURI = ont.getNsPrefixURI("");
		final String prefix = ont.getNsURIPrefix(prefixURI);
		PlatformkitLogger.logger.info(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.loadedOntWithNS"), 
				prefix, 
				prefixURI)); //$NON-NLS-1$
		getPrefixURIs().add(prefixURI);
	}

	/**
	 * Unregisters the prefix URI after unloading a model.
	 */
	private void unregisterPrefix(OntModel ont) {
		String prefixURI = ont.getNsPrefixURI("");
		String prefix = ont.getNsURIPrefix(prefixURI);
		PlatformkitLogger.logger.info(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.unloadedOntWithNS"), 
				prefix, 
				prefixURI)); //$NON-NLS-1$
		getPrefixURIs().remove(prefixURI);
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadInstances(java.lang.String)
	 */
	public void loadInstances(String url) {
		unloadInstances();
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.loadingInstanceOntFrom"), 
				url)); //$NON-NLS-1$
		final OntModel inst = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
		inst.read(url);
		setInstances(new OntModelAdapter(inst));
		getOntology().getModel().add(inst);
		registerPrefix(inst);
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadInstances(java.io.InputStream)
	 */
	public void loadInstances(InputStream in) {
		unloadInstances();
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.loadingInstanceOntFrom"), 
				in)); //$NON-NLS-1$
		final OntModel inst = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
		inst.read(in, ""); //$NON-NLS-1$
		setInstances(new OntModelAdapter(inst));
		getOntology().getModel().add(inst);
		registerPrefix(inst);
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#unloadInstances()
	 */
	public void unloadInstances() {
		final OntModelAdapter instances = getInstances();
		if (instances != null) {
			PlatformkitLogger.logger.fine(PlatformkitJenaResources.getString("JenaOntologies.unloadingInstanceOnt")); //$NON-NLS-1$
			final OntModel model = instances.getModel();
			unregisterPrefix(model);
			getOntology().getModel().remove(model);
			setInstances(null);
		}
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#saveOntology(java.io.OutputStream)
	 */
	public void saveOntology(OutputStream out) {
		final OntModel model = getOntology().getModel();
		final RDFWriter writer = model.getWriter(FileUtils.langXML);
		prepareWriter(writer, LOCAL_INF_NS);
		writer.write(model, out, LOCAL_INF_NS);
	}

	/**
	 * Configures the given RDFWriter.
	 * @param writer
	 * @param namespace XML namespace.
	 */
	protected static void prepareWriter(RDFWriter writer, String namespace) {
		writer.setProperty("showXmlDeclaration", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.setProperty("relativeURIs", "same-document"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.setProperty("xmlbase", namespace); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#checkConsistency()
	 */
	public void checkConsistency() throws OntException {
		PlatformkitLogger.logger.info(PlatformkitJenaResources.getString("JenaOntologies.checkingConsistency")); //$NON-NLS-1$
		StmtIterator i;
		//Jena is not thread-safe when communicating to the DIG reasoner,
		//so lock all actions that trigger DIG activity.
		synchronized (JenaOntologies.class) {
			i = getOntology().getModel().listStatements(null, OWL.equivalentClass,
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
		final OntDocumentManager dm = getOntology().getModel().getDocumentManager();
		final InputStream[] streams = provider.getOntologies();
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
		final IOntologyProvider[] providers = PlatformkitRegistry.INSTANCE.getOntologyProviders();
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
		final OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
		model.read(resource, null, null);
		String ns = model.getNsPrefixURI("");
		ns = ns.substring(0, ns.length() - 1);
		PlatformkitLogger.logger.info(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.addingLocalOnt"), 
				ns)); //$NON-NLS-1$
		dm.addModel(ns, model);
		localOntologies.put(ns, new OntModelAdapter(model));
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
		final List<IOntClass> namedClasses = new ArrayList<IOntClass>();
		synchronized (JenaOntologies.class) {
			final ExtendedIterator<OntClass> ncs = getOntology().getModel().listNamedClasses().filterKeep(ncFilter);
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
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.updatingHierarchyFor"), 
				forClass)); //$NON-NLS-1$
		final OntClass ontClass = ((OntClassAdapter)forClass).getModel();
		final Set<OntClass> equivCs = getEquivClasses().get(ontClass);
		if (equivCs == null) {
			throw new OntException(String.format(
					PlatformkitJenaResources.getString("JenaOntologies.equivNotFound"), 
					forClass)); //$NON-NLS-1$
		}
		addEquivalentClasses(ontClass, equivCs);
		final Set<OntClass> superCs = getSuperClasses().get(ontClass);
		if (superCs == null) {
			throw new OntException(String.format(
					PlatformkitJenaResources.getString("JenaOntologies.superNotFound"), 
					forClass)); //$NON-NLS-1$
		}
		final Set<OntClass> obsoleteCs = getObsoleteSuperClasses().get(ontClass);
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
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.pruningHierarchyFor"), 
				forClass)); //$NON-NLS-1$
		final OntClass ontClass = ((OntClassAdapter)forClass).getModel();
		final Set<OntClass> obsolete = new HashSet<OntClass>();
		final Set<OntClass> superCs = getSuperClasses().get(ontClass);
		if (superCs == null) {
			throw new OntException(String.format(
					PlatformkitJenaResources.getString("JenaOntologies.superNotFound"), 
					forClass)); //$NON-NLS-1$
		}
		for (Iterator<OntClass> cs = superCs.iterator(); cs.hasNext();) {
			OntClass superC = cs.next();
			Set<OntClass> superSuperCs = getSuperClasses().get(superC);
			if (superSuperCs != null) {
				obsolete.addAll(superSuperCs);
			}
		}
		PlatformkitLogger.logger.info(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.obsoleteSuperOf"), 
				obsolete, 
				forClass)); //$NON-NLS-1$
		getObsoleteSuperClasses().put(ontClass, obsolete);        
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#buildHierarchyMap(com.hp.hpl.jena.ontology.OntClass)
	 */
	public void buildHierarchyMap(IOntClass forClass)
	throws OntException {
		PlatformkitLogger.logger.fine(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.buildingHierarchyFor"), 
				forClass)); //$NON-NLS-1$
		final OntClass ontClass = ((OntClassAdapter)forClass).getModel();
		final OntClass baseClass = getBaseClass(ontClass);
		final Set<OntClass> equivs = new HashSet<OntClass>();
		final Set<OntClass> supers = new HashSet<OntClass>();
		synchronized (JenaOntologies.class) {
			final Iterator<OntClass> superCs = ontClass.listSuperClasses().filterKeep(ncFilter);
			while (superCs.hasNext()) {
				OntClass superC = superCs.next();
				try {
					OntClass baseSuperC = getBaseClass(superC);
					supers.add(baseSuperC);
				} catch (NotFoundException nfe) {
					PlatformkitLogger.logger.warning(nfe.getMessage());
				}
			}
			final Iterator<OntClass> equivCs = ontClass.listEquivalentClasses().filterKeep(ncFilter);
			while (equivCs.hasNext()) {
				OntClass equivC = equivCs.next();
				if (!equivC.equals(ontClass)) {
					try {
						OntClass baseEquivC = getBaseClass(equivC);
						equivs.add(baseEquivC);
					} catch (NotFoundException nfe) {
						PlatformkitLogger.logger.warning(nfe.getMessage());
					}
				}
			}
		}
		supers.removeAll(equivs);
		PlatformkitLogger.logger.info(String.format(
				PlatformkitJenaResources.getString("JenaOntologies.equivToSubOf"), 
				forClass, 
				equivs, 
				supers)); //$NON-NLS-1$
		getEquivClasses().put(baseClass, equivs);
		getSuperClasses().put(baseClass, supers);
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#createNewOntology(java.lang.String)
	 */
	public IOntModel createNewOntology(String url) throws OntException {
		final OntModel ont = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
		ont.setNsPrefix("", url + "#"); //$NON-NLS-1$
		final IOntModel iont = new OntModelAdapter(ont);
		addAllImports(iont);
		return iont;
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
				PlatformkitJenaResources.getString("JenaOntologies.loadingOntFrom"), 
				in)); //$NON-NLS-1$
		final OntModel ont = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		ont.read(in, "");
		return new OntModelAdapter(ont);
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.AbstractOntologies#addImports(be.ac.vub.platformkit.kb.IOntModel, be.ac.vub.platformkit.kb.IOntModel)
	 */
	@Override
	protected void addImports(IOntModel ont, IOntModel importedOnt) throws OntException {
		assert ont instanceof OntModelAdapter;
		assert importedOnt instanceof OntModelAdapter;
		final OntModel jenaOnt = ((OntModelAdapter) ont).getModel();
		final OntModel jenaImportedOnt = ((OntModelAdapter) importedOnt).getModel();
		final Ontology jenaOntObject = jenaOnt.createOntology(ont.getNsURI());
		final Ontology jenaImportOntObject = jenaImportedOnt.createOntology(importedOnt.getNsURI());
		final Statement importStatement = ResourceFactory.createStatement(jenaOntObject, jenaOnt.getProfile().IMPORTS(), jenaImportOntObject);
		jenaOnt.add(importStatement);
	}

	/**
	 * @param c
	 * @return True if c has a prefix URI of one of the directly loaded models.
	 */
	private boolean hasLocalPrefix(OntClass c) {
		for (Iterator<String> pfxs = getPrefixURIs().iterator(); pfxs.hasNext();) {
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
		for (OntClass c : equivCs) {
			synchronized (JenaOntologies.class) {
				if (!forClass.hasEquivalentClass(c)) {
					PlatformkitLogger.logger.info(String.format(
							PlatformkitJenaResources.getString("JenaOntologies.addingAsEquivTo"), 
							c, 
							forClass)); //$NON-NLS-1$
					forClass.addEquivalentClass(c);
				} else {
					PlatformkitLogger.logger.fine(String.format(
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
		for (OntClass c : superCs) {
			synchronized (JenaOntologies.class) {
				if (!forClass.hasSuperClass(c)) {
					PlatformkitLogger.logger.info(String.format(
							PlatformkitJenaResources.getString("JenaOntologies.addingAsSuperTo"), 
							c, 
							forClass)); //$NON-NLS-1$
					forClass.addSuperClass(c);
				} else {
					PlatformkitLogger.logger.fine(String.format(
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
		for (OntClass c : superCs) {
			synchronized (JenaOntologies.class) {
				if (forClass.hasSuperClass(c)) {
					PlatformkitLogger.logger.info(String.format(
							PlatformkitJenaResources.getString("JenaOntologies.addingAsSuperTo="), 
							c, 
							forClass)); //$NON-NLS-1$
					forClass.removeSuperClass(c);
				} else {
					PlatformkitLogger.logger.fine(String.format(
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
		final OntModel baseModel = getBaseOntology().getModel();
		assert baseModel != null;
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

	/**
	 * @param ontology the ontology to set
	 */
	protected void setOntology(OntModelAdapter ontology) {
		this.ontology = ontology;
	}

	/**
	 * @return the ontology
	 */
	protected OntModelAdapter getOntology() {
		return ontology;
	}

	/**
	 * @return the prefixURIs
	 */
	protected Set<String> getPrefixURIs() {
		return prefixURIs;
	}

	/**
	 * @return the superClasses
	 */
	protected Map<OntClass, Set<OntClass>> getSuperClasses() {
		return superClasses;
	}

	/**
	 * @return the equivClasses
	 */
	protected Map<OntClass, Set<OntClass>> getEquivClasses() {
		return equivClasses;
	}

	/**
	 * @return the obsoleteSuperClasses
	 */
	protected Map<OntClass, Set<OntClass>> getObsoleteSuperClasses() {
		return obsoleteSuperClasses;
	}
}
