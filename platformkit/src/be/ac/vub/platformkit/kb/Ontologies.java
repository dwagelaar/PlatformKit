/*
 * $Id: Ontologies.java 5627 2006-09-23 16:13:52Z dwagelaa $
 * Created on 23-aug-2005
 * (C) 2005, Dennis Wagelaar, SSEL, VUB
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2 as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  (See the file "COPYING" that is included with this source distribution.)
 */
package be.ac.vub.platformkit.kb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.mindswap.pellet.jena.PelletReasoner;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import be.ac.vub.platformkit.IOntModelChangeListener;
import be.ac.vub.platformkit.kb.util.NamedClassFilter;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntologyException;
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
public class Ontologies {
    public static final String LOGGER = "be.ac.vub.platformkit";
    public static final String ONTOLOGY_EXT_POINT = "be.ac.vub.platformkit.ontology";
    public static final String LOCAL_INF_NS = "http://local/platformkit/inferred.owl";

    protected static Logger logger = Logger.getLogger(LOGGER);
    private OntModel baseOntology;
    private OntModel ontology;
    private OntModel instances = null;
    private String reasonerUrl = "http://localhost:8081";
    private Set prefixURIs = new HashSet();
    private Map superClasses = new HashMap();
    private Map equivClasses = new HashMap();
    private Map obsoleteSuperClasses = new HashMap();
    private static NamedClassFilter ncFilter = new NamedClassFilter();
    private List ontologyChangeListeners = new ArrayList();

    /**
     * Creates a new Ontologies knowledgebase object.
     * @throws IOException if the local ontology mapping could not be read.
     */
    public Ontologies() throws IOException {
        ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
        ontology.setNsPrefix("", LOCAL_INF_NS);
        addLocalOntologies(ontology.getDocumentManager());
        baseOntology = ontology;
        prefixURIs.add(LOCAL_INF_NS + '#');
    }

    /**
     * Adds listener for changes to {@link #getOntModel()}.
     * @param listener
     */
    public void addOntModelChangeListener(IOntModelChangeListener listener) {
    	ontologyChangeListeners.add(listener);
    }
    
    /**
     * Removes listener for changes to {@link #getOntModel()}.
     * @param listener
     */
    public void removeOntModelChangeListener(IOntModelChangeListener listener) {
    	ontologyChangeListeners.remove(listener);
    }
    
    /**
     * Notifies all ontology change listeners of the new ontology model.
     * @see #addOntModelChangeListener(IOntModelChangeListener)
     */
    protected void notifyOntologyChanged() {
    	OntModel ontology = getOntModel();
    	for (Iterator it = ontologyChangeListeners.iterator(); it.hasNext();) {
    		((IOntModelChangeListener) it.next()).ontModelChanged(ontology);
    	}
    }
    
    /**
     * Attaches the DIG reasoner to the knowledgebase.
     */
    public void attachDIGReasoner() {
        if (ontology.getSpecification().getReasoner() != null) {
        	if (ontology.getSpecification().getReasoner() instanceof DIGReasoner) {
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
        synchronized (Ontologies.class) {
            ontology = ModelFactory.createOntologyModel(spec, getBaseOntology());
            ontology.setNsPrefix("", LOCAL_INF_NS);
        }
        notifyOntologyChanged();
    }
    
    /**
     * Attaches the Pellet OWL reasoner to the knowledgebase.
     */
    public void attachPelletReasoner() {
        if (ontology.getSpecification().getReasoner() != null) {
        	if (ontology.getSpecification().getReasoner() instanceof PelletReasoner) {
                logger.warning("Pellet reasoner already attached");
                return;
        	}
        	detachReasoner();
        }
        logger.info("Attaching Pellet reasoner");
        //Jena is not thread-safe when communicating to the reasoner,
        //so lock all actions that trigger reasoner activity.
        synchronized (Ontologies.class) {
            ontology = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, getBaseOntology());
            ontology.setNsPrefix("", LOCAL_INF_NS);
        }
        notifyOntologyChanged();
    }
    
    /**
     * Attaches the OWL micro reasoner to the knowledgebase.
     * @see ReasonerRegistry#getOWLMicroReasoner()
     */
    public void attachOWLReasoner() {
        if (ontology.getSpecification().getReasoner() != null) {
        	if (ontology.getSpecification().getReasoner() instanceof OWLMicroReasoner) {
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
        synchronized (Ontologies.class) {
            ontology = ModelFactory.createOntologyModel(spec, getBaseOntology());
            ontology.setNsPrefix("", LOCAL_INF_NS);
        }
        notifyOntologyChanged();
    }
    
    /**
     * Attaches the transitive reasoner to the knowledgebase.
     * @see ReasonerRegistry#getTransitiveReasoner()
     */
    public void attachTransitiveReasoner() {
        if (ontology.getSpecification().getReasoner() != null) {
        	if (ontology.getSpecification().getReasoner() instanceof TransitiveReasoner) {
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
        synchronized (Ontologies.class) {
            ontology = ModelFactory.createOntologyModel(spec, getBaseOntology());
            ontology.setNsPrefix("", LOCAL_INF_NS);
        }
        notifyOntologyChanged();
    }
    
    /**
     * Detaches the current reasoner from the knowledgebase.
     */
    public void detachReasoner() {
        logger.info("Detaching reasoner");
        if (ontology.getSpecification().getReasoner() == null) {
            logger.warning("Reasoner already detached");
            return;
        }
        ontology = getBaseOntology();
        notifyOntologyChanged();
    }
    
    /**
     * @return The base ontology object (excluding reasoner results).
     */
    public OntModel getBaseOntology() {
        return baseOntology;
    }
    
    /**
     * @return The inner ontology object.
     */
    public OntModel getOntModel() {
        return ontology;
    }
    
    /**
     * @return The inner instances ontology object.
     */
    public OntModel getInstances() {
        return instances;
    }
    
    /**
     * Loads an ontology into the repository.
     * @param url the ontology url or namespace.
     */
    public void loadOntology(String url) {
        logger.fine("Loading ontology from " + url);
        OntModel ont = getOntModel();
        ont.read(url);
        registerPrefix(ont);
        notifyOntologyChanged();
    }
    
    /**
     * Loads an ontology into the repository.
     * @param in the inputstream containing the ontology.
     */
    public void loadOntology(InputStream in) {
        logger.fine("Loading ontology from " + in);
        OntModel ont = getOntModel();
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
    
    /**
     * Loads an instance ontology into the repository, removing the previous
     * instance ontology.
     * @param url the ontology url or namespace.
     */
    public void loadInstances(String url) {
        logger.fine("Loading instance ontology " + url);
        if (instances != null) {
            unregisterPrefix(instances);
            getOntModel().remove(instances);
        }
        instances = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
        instances.read(url);
        getOntModel().add(instances);
        registerPrefix(instances);
    }
    
    /**
     * Loads an instance ontology into the repository, removing the previous
     * instance ontology.
     * @param in the inputstream containing the ontology.
     */
    public void loadInstances(InputStream in) {
        logger.fine("Loading instance ontology from " + in);
        if (instances != null) {
            unregisterPrefix(instances);
            getOntModel().remove(instances);
        }
        instances = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
        instances.read(in, "");
        getOntModel().add(instances);
        registerPrefix(instances);
    }
    
    /**
     * Writes the ontology to the given output stream.
     * @param out
     * @throws UnsupportedEncodingException
     */
    public void saveOntology(OutputStream out)
    throws UnsupportedEncodingException {
        PrintStream ps = new PrintStream(out);
        RDFWriter writer = getOntModel().getWriter(FileUtils.langXMLAbbrev);
        prepareWriter(writer, LOCAL_INF_NS);
        String encoding = "UTF-8";
        writer.write(getOntModel(), new OutputStreamWriter(ps, encoding), LOCAL_INF_NS);
    }
    
    /**
     * Configures the given RDFWriter.
     * @param writer
     * @param namespace XML namespace.
     */
    private static void prepareWriter(RDFWriter writer, String namespace) {
        writer.setProperty("showXmlDeclaration", "" + true);
        writer.setProperty("relativeURIs", "same-document");
        writer.setProperty("xmlbase", namespace);
        writer.setProperty("blockRules", "propertyAttr");
    }

    /**
     * Checks if all classes are satisfiable.
     * @throws OntologyException if not all classes are satifiable.
     */
    public void checkConsistency() throws OntologyException {
        logger.info("Checking ontology consistency");
        StmtIterator i;
        //Jena is not thread-safe when communicating to the DIG reasoner,
        //so lock all actions that trigger DIG activity.
        synchronized (Ontologies.class) {
            i = getOntModel().listStatements(null, OWL.equivalentClass,
                    OWL.Nothing);
        }
        while (i.hasNext()) {
            Resource c = i.nextStatement().getSubject();
            if (!c.getURI().equals(OWL.Nothing.getURI())) {
                throw new OntologyException("Class " + c + " is unsatisfiable");
            }
        }
    }
    
    /**
     * Adds all known local ontologies to the document manager.
     * @param dm The document manager.
     * @throws IOException if the local ontology mapping could not be read.
     */
    private void addLocalOntologies(OntDocumentManager dm) throws IOException {
    	addLocalOntologies(dm, BaseOntologyProvider.INSTANCE);
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
					InputStream[] streams = provider.getOntologies();
					for (int k = 0; k < streams.length; k++) {
						addLocalOntology(dm, streams[k]);
					}
				} catch (CoreException e) {
					throw new IOException(e.getLocalizedMessage());
				}
			}
		 }
    }
    
    /**
     * Adds all known local ontologies to the document manager.
     * @param dm The document manager.
     * @param provider The ontology provider.
     * @throws IOException if the local ontology mapping could not be read.
     */
    private void addLocalOntologies(OntDocumentManager dm, IOntologyProvider provider) throws IOException {
		InputStream[] streams = provider.getOntologies();
		for (int k = 0; k < streams.length; k++) {
			addLocalOntology(dm, streams[k]);
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
        //registerPrefix(model);
        String ns = model.getNsPrefixURI("");
        ns = ns.substring(0, ns.length() - 1);
        logger.info("Adding local ontology " + ns);
        dm.addModel(ns, model);
    }

    /**
     * @param reasonerUrl The reasonerUrl to set.
     */
    public void setReasonerUrl(String reasonerUrl) {
        this.reasonerUrl = reasonerUrl;
    }

    /**
     * @return Returns the reasonerUrl.
     */
    public String getReasonerUrl() {
        return reasonerUrl;
    }
    
    /**
     * @return The named ontology classes with a local prefix URI
     * @see #hasLocalPrefix(OntClass)
     */
    public List getLocalNamedClasses() {
        List namedClasses = new ArrayList();
        synchronized (Ontologies.class) {
            ExtendedIterator ncs = getOntModel().listNamedClasses().filterKeep(ncFilter);
            while (ncs.hasNext()) {
                OntClass c = (OntClass) ncs.next();
                if (hasLocalPrefix(c)) {
                    namedClasses.add(c);
                }
            }
        }
        return namedClasses;
    }
    
    /**
     * Updates the asserted class hierarchy with the inferred hierarchy for the local named classes.
     * Requires @see #buildHierarchyMap() to be invoked first.
     */
    public void updateHierarchy() {
        List forClasses = getLocalNamedClasses();
        for (int i = 0; i < forClasses.size(); i++) {
            try {
                pruneHierarchyMap((OntClass) forClasses.get(i));
            } catch (NotFoundException nfe) {
                logger.warning(nfe.getMessage());
            }
        }
        for (int i = 0; i < forClasses.size(); i++) {
            try {
                updateHierarchy((OntClass) forClasses.get(i));
            } catch (NotFoundException nfe) {
                logger.warning(nfe.getMessage());
            }
        }
    }
    
    /**
     * Builds an internal map of the superclasses and equivalent classes of all local named classes in the base ontology.
     * Requires DIG reasoner.
     * Required for invocation of @see #augmentHierarchy(OntClass) and @see #pruneHierarchy(OntClass)
     */
    public void buildHierarchyMap() {
        List forClasses = getLocalNamedClasses();
        for (int i = 0; i < forClasses.size(); i++) {
            try {
                buildHierarchyMap((OntClass) forClasses.get(i));
            } catch (NotFoundException nfe) {
                logger.warning(nfe.getMessage());
            }
        }
    }
    
    /**
     * Updates the asserted class hierarchy with the inferred hierarchy for the given class.
     * Requires pre-built and pruned hierarchy map by @see #buildHierarchyMap(OntClass) and
     * @see #pruneHierarchyMap(OntClass).
     * @param forClass
     * @throws NotFoundException if the class cannot be found in the base ontology.
     */
    public void updateHierarchy(OntClass forClass)
    throws NotFoundException {
        logger.fine("augmenting hierarchy for " + forClass);
        addEquivalentClasses(forClass, (Set) equivClasses.get(forClass));
        Set superCs = (Set) superClasses.get(forClass);
        if (superCs == null) {
            throw new NotFoundException(forClass + " superclasses not found in the hierarchy map");
        }
        Set obsoleteCs = (Set) obsoleteSuperClasses.get(forClass);
        if (obsoleteCs == null) {
            throw new NotFoundException(forClass + " obsolete superclasses not found in the hierarchy map");
        }
        superCs.removeAll(obsoleteCs);
        addSuperClasses(forClass, superCs);
        removeSuperClasses(forClass, obsoleteCs);
    }
    
    /**
     * Prunes the class hierarchy map for the given class.
     * Requires pre-built hierarchy map by @see #buildHierarchyMap(OntClass).
     * @param forClass
     * @throws NotFoundException if the class cannot be found in the hierarchy map.
     */
    public void pruneHierarchyMap(OntClass forClass)
    throws NotFoundException {
        logger.fine("pruning hierarchy map for " + forClass);
        Set obsolete = new HashSet();
        Set superCs = (Set) superClasses.get(forClass);
        if (superCs == null) {
            throw new NotFoundException(forClass + " superclasses not found in the hierarchy map");
        }
        for (Iterator cs = superCs.iterator(); cs.hasNext();) {
            OntClass superC = (OntClass) cs.next();
            Set superSuperCs = (Set) superClasses.get(superC);
            if (superSuperCs != null) {
                obsolete.addAll(superSuperCs);
            }
        }
        logger.info(forClass + " obsolete subclass of " + obsolete.toString());
        obsoleteSuperClasses.put(forClass, obsolete);        
    }
    
    /**
     * Builds an internal map of the superclasses and equivalent classes of forClass in the base ontology.
     * Required for invocation of @see #augmentHierarchy(OntClass) and @see #pruneHierarchy(OntClass)
     * @param forClass
     * @throws NotFoundException if the class cannot be found in the base ontology.
     */
    public void buildHierarchyMap(OntClass forClass)
    throws NotFoundException {
        logger.fine("building hierarchy for " + forClass);
        OntClass baseClass = getBaseClass(forClass);
        Set equivs = new HashSet();
        Set supers = new HashSet();
        synchronized (Ontologies.class) {
            Iterator superCs = forClass.listSuperClasses().filterKeep(ncFilter);
            while (superCs.hasNext()) {
                OntClass superC = (OntClass) superCs.next();
                try {
                    OntClass baseSuperC = getBaseClass(superC);
                    supers.add(baseSuperC);
                } catch (NotFoundException nfe) {
                    logger.warning(nfe.getMessage());
                }
            }
            Iterator equivCs = forClass.listEquivalentClasses().filterKeep(ncFilter);
            while (equivCs.hasNext()) {
                OntClass equivC = (OntClass) equivCs.next();
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
        for (Iterator pfxs = prefixURIs.iterator(); pfxs.hasNext();) {
            if (c.getURI().startsWith((String) pfxs.next())) {
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
    private void addEquivalentClasses(OntClass forClass, Collection equivCs) {
        for (Iterator cs = equivCs.iterator(); cs.hasNext();) {
            OntClass c = (OntClass) cs.next();
            synchronized (Ontologies.class) {
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
    private void addSuperClasses(OntClass forClass, Collection superCs) {
        for (Iterator cs = superCs.iterator(); cs.hasNext();) {
            OntClass c = (OntClass) cs.next();
            synchronized (Ontologies.class) {
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
    private void removeSuperClasses(OntClass forClass, Collection superCs) {
        for (Iterator scs = superCs.iterator(); scs.hasNext();) {
            OntClass c = (OntClass) scs.next();
            synchronized (Ontologies.class) {
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
        OntModel baseModel = getBaseOntology();
        Assert.assertNotNull(baseModel);
        OntClass baseClass;
        synchronized (Ontologies.class) {
            baseClass = baseModel.getOntClass(c.getURI());
        }
        if (baseClass == null) {
            throw new NotFoundException(c + " not found in base ontology");
        }
        return baseClass;
    }
}
