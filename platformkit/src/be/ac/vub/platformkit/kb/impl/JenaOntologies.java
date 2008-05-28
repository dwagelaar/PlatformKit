/*
 * $Id: Ontologies.java 5627 2006-09-23 16:13:52Z dwagelaa $
 * Created on 23-aug-2005
 * (C) 2005, Dennis Wagelaar, SSEL, VUB
 */
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.mindswap.pellet.jena.PelletReasoner;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import be.ac.vub.platformkit.kb.AbstractOntologies;
import be.ac.vub.platformkit.kb.BaseOntologyProvider;
import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntModel;
import be.ac.vub.platformkit.kb.IOntologyProvider;
import be.ac.vub.platformkit.kb.util.OntException;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.dig.DIGReasoner;
import com.hp.hpl.jena.reasoner.dig.DIGReasonerFactory;
import com.hp.hpl.jena.reasoner.rulesys.OWLMicroReasoner;
import com.hp.hpl.jena.reasoner.transitiveReasoner.TransitiveReasoner;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;

/**
 * The ontology repository for the PlatformKit.
 * @author dennis
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
     * Creates a new Ontologies knowledgebase object.
     * @throws IOException if the local ontology mapping could not be read.
     */
    public JenaOntologies() throws IOException {
        OntModel ont = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
        ont.setNsPrefix("", LOCAL_INF_NS);
        ontology = new OntModelAdapter(ont);
        baseOntology = ontology;
        prefixURIs.add(LOCAL_INF_NS + '#');
        addLocalOntologies();
    }

    /* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#attachDIGReasoner()
	 */
    public void attachDIGReasoner() {
        if (ontology.model.getSpecification().getReasoner() != null) {
        	if (ontology.model.getSpecification().getReasoner() instanceof DIGReasoner) {
                logger.warning("DIG reasoner already attached");
                return;
        	}
        	detachReasoner();
        }
        logger.info("Attaching DIG reasoner at " + getReasonerUrl());
        Model cModel = ModelFactory.createDefaultModel();
        Resource conf = cModel.createResource();
        conf.addProperty(ReasonerVocabulary.EXT_REASONER_URL, cModel
                .createResource(getReasonerUrl()));
        DIGReasonerFactory drf = (DIGReasonerFactory) ReasonerRegistry
                .theRegistry().getFactory(DIGReasonerFactory.URI);
        DIGReasoner r = (DIGReasoner) drf.create(conf);
        OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_DL_MEM);
        spec.setReasoner(r);
        //Jena is not thread-safe when communicating to the DIG reasoner,
        //so lock all actions that trigger DIG activity.
        synchronized (JenaOntologies.class) {
            OntModel ont = ModelFactory.createOntologyModel(spec, baseOntology.model);
            ont.setNsPrefix("", LOCAL_INF_NS);
            ontology = new OntModelAdapter(ont);
        }
        notifyOntologyChanged();
    }
    
    /* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#attachPelletReasoner()
	 */
    public void attachPelletReasoner() {
        if (ontology.model.getSpecification().getReasoner() != null) {
        	if (ontology.model.getSpecification().getReasoner() instanceof PelletReasoner) {
                logger.warning("Pellet reasoner already attached");
                return;
        	}
        	detachReasoner();
        }
        logger.info("Attaching Pellet reasoner");
        //Jena is not thread-safe when communicating to the reasoner,
        //so lock all actions that trigger reasoner activity.
        synchronized (JenaOntologies.class) {
            OntModel ont = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, baseOntology.model);
            ont.setNsPrefix("", LOCAL_INF_NS);
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
                logger.warning("OWL micro reasoner already attached");
                return;
        	}
        	detachReasoner();
        }
        logger.info("Attaching OWL micro reasoner");
        Reasoner r = ReasonerRegistry.getOWLMicroReasoner();
        OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_DL_MEM);
        spec.setReasoner(r);
        //Jena is not thread-safe when communicating to the reasoner,
        //so lock all actions that trigger reasoner activity.
        synchronized (JenaOntologies.class) {
            OntModel ont = ModelFactory.createOntologyModel(spec, baseOntology.model);
            ont.setNsPrefix("", LOCAL_INF_NS);
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
                logger.warning("Transitive reasoner already attached");
                return;
        	}
        	detachReasoner();
        }
        logger.info("Attaching transitive reasoner");
        Reasoner r = ReasonerRegistry.getTransitiveReasoner();
        OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_DL_MEM);
        spec.setReasoner(r);
        //Jena is not thread-safe when communicating to the reasoner,
        //so lock all actions that trigger reasoner activity.
        synchronized (JenaOntologies.class) {
            OntModel ont = ModelFactory.createOntologyModel(spec, baseOntology.model);
            ont.setNsPrefix("", LOCAL_INF_NS);
            ontology = new OntModelAdapter(ont);
        }
        notifyOntologyChanged();
    }
    
    /* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#detachReasoner()
	 */
    public void detachReasoner() {
        if (ontology.model.getSpecification().getReasoner() == null) {
            logger.warning("Reasoner already detached");
            return;
        }
        logger.info("Detaching reasoner");
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
        logger.fine("Loading ontology from " + url);
        OntModel ont = ontology.model;
        ont.read(url);
        registerPrefix(ont);
        notifyOntologyChanged();
    }
    
    /* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadOntology(java.io.InputStream)
	 */
    public void loadOntology(InputStream in) {
        logger.fine("Loading ontology from " + in);
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
        logger.info("Loaded ontology with namespace " + prefix + ":" + prefixURI);
        prefixURIs.add(prefixURI);
    }
    
    /**
     * Unregisters the prefix URI after unloading a model.
     */
    private void unregisterPrefix(OntModel ont) {
        String prefixURI = ont.getNsPrefixURI("");
        String prefix = ont.getNsURIPrefix(prefixURI);
        logger.info("Unloaded ontology with namespace " + prefix + ":" + prefixURI);
        prefixURIs.remove(prefixURI);
    }
    
    /* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#loadInstances(java.lang.String)
	 */
    public void loadInstances(String url) {
        unloadInstances();
        logger.fine("Loading instance ontology " + url);
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
        logger.fine("Loading instance ontology from " + in);
        OntModel inst = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
        inst.read(in, "");
        instances = new OntModelAdapter(inst);
        ontology.model.add(inst);
        registerPrefix(inst);
    }
    
    /* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#unloadInstances()
	 */
    public void unloadInstances() {
        if (instances != null) {
            logger.fine("Unloading current instance ontology");
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
        writer.setProperty("showXmlDeclaration", "true");
        writer.setProperty("relativeURIs", "same-document");
        writer.setProperty("xmlbase", namespace);
    }

    /* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#checkConsistency()
	 */
    public void checkConsistency() throws OntException {
        logger.info("Checking ontology consistency");
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
                throw new OntException("Class " + c + " is unsatisfiable");
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
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        if (registry == null) {
            logger.info("Eclipse platform extension registry not found. Local ontology registration does not work outside Eclipse.");
            return;
        }
        IExtensionPoint point = registry.getExtensionPoint(ONTOLOGY_EXT_POINT);
        IExtension[] extensions = point.getExtensions();
        for (int i = 0 ; i < extensions.length ; i++) {
            IConfigurationElement[] elements = extensions[i].getConfigurationElements();
            for (int j = 0 ; j < elements.length ; j++) {
                try {
                    IOntologyProvider provider = (IOntologyProvider)
                    		elements[j].createExecutableExtension("provider");
                    addLocalOntologies(provider);
                } catch (CoreException e) {
                    throw new IOException(e.getLocalizedMessage());
                }
            }
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
        logger.info("Adding local ontology " + ns);
        dm.addModel(ns, model);
        /*
         * If the prefix of this ontology is added, then #getLocalNamedClasses()
         * will also return the classes from this ontology. This has as a consequence
         * that taxonomy classification will extend into the standard vocabulary
         * ontologies as well. This currently creates too much overhead and is not
         * necessary for proper functioning of PlatformKit.
         */
//        registerPrefix(model);
    }

    /* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#getLocalNamedClasses()
	 */
    public List<IOntClass> getLocalNamedClasses() {
        List<IOntClass> namedClasses = new ArrayList<IOntClass>();
        synchronized (JenaOntologies.class) {
            ExtendedIterator ncs = ontology.model.listNamedClasses().filterKeep(ncFilter);
            while (ncs.hasNext()) {
                OntClass c = (OntClass) ncs.next();
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
        logger.fine("augmenting hierarchy for " + forClass);
        OntClass ontClass = ((OntClassAdapter)forClass).model;
        Set<OntClass> equivCs = equivClasses.get(ontClass);
        if (equivCs == null) {
            throw new OntException(forClass + " equivalent classes not found in the hierarchy map");
        }
        addEquivalentClasses(ontClass, equivCs);
        Set<OntClass> superCs = superClasses.get(ontClass);
        if (superCs == null) {
            throw new OntException(forClass + " superclasses not found in the hierarchy map");
        }
        Set<OntClass> obsoleteCs = obsoleteSuperClasses.get(ontClass);
        if (obsoleteCs == null) {
            throw new OntException(forClass + " obsolete superclasses not found in the hierarchy map");
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
        logger.fine("pruning hierarchy map for " + forClass);
        OntClass ontClass = ((OntClassAdapter)forClass).model;
        Set<OntClass> obsolete = new HashSet<OntClass>();
        Set<OntClass> superCs = superClasses.get(ontClass);
        if (superCs == null) {
            throw new OntException(forClass + " superclasses not found in the hierarchy map");
        }
        for (Iterator<OntClass> cs = superCs.iterator(); cs.hasNext();) {
            OntClass superC = cs.next();
            Set<OntClass> superSuperCs = superClasses.get(superC);
            if (superSuperCs != null) {
                obsolete.addAll(superSuperCs);
            }
        }
        logger.info(forClass + " obsolete subclass of " + obsolete.toString());
        obsoleteSuperClasses.put(ontClass, obsolete);        
    }
    
    /* (non-Javadoc)
	 * @see be.ac.vub.platformkit.kb.IOntologies#buildHierarchyMap(com.hp.hpl.jena.ontology.OntClass)
	 */
    public void buildHierarchyMap(IOntClass forClass)
    throws OntException {
        logger.fine("building hierarchy for " + forClass);
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
        logger.info(forClass + " equivalent to " + equivs.toString() + 
                ", subclass of " + supers.toString());
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
                    logger.info("*** adding " + c + " as equivalent class to " + forClass);
                    forClass.addEquivalentClass(c);
                } else {
                    logger.fine(forClass + " already has equivalent class " + c);
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
                    logger.info("*** adding " + c + " as direct superclass to " + forClass);
                    forClass.addSuperClass(c);
                } else {
                    logger.fine(forClass + " already has superclass " + c);
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
                    logger.info("*** removing " + c + " as direct superclass from " + forClass);
                    forClass.removeSuperClass(c);
                } else {
                    logger.fine(forClass + " does not have superclass " + c);
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
            throw new NotFoundException(c + " not found in base ontology");
        }
        return baseClass;
    }
}
