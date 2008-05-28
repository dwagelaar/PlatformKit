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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
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

/**
 * The OWLAPI version of the ontology repository for the PlatformKit.
 * Only works with Pellet reasoner, but should provide better performance.
 * @author dennis
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
     * Creates a new OWLAPIOntologies knowledgebase object.
     * @throws IOException if the local ontology mapping could not be read.
     */
    public OWLAPIOntologies() throws IOException {
    	mgr = OWLManager.createOWLOntologyManager();
    	URI localInfNs = URI.create(IOntologies.LOCAL_INF_NS);
        try {
			ontology = new OWLOntologyAdapter(mgr.createOntology(localInfNs), this);
		} catch (OWLOntologyCreationException e) {
			throw new IOException(e);
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
				throw new IOException(e);
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
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        if (registry == null) {
            logger.info("Eclipse platform extension registry not found. Local ontology registration does not work outside Eclipse.");
            return;
        }
        IExtensionPoint point = registry.getExtensionPoint(IOntologies.ONTOLOGY_EXT_POINT);
        for (IExtension extension : point.getExtensions()) {
            for (IConfigurationElement element : extension.getConfigurationElements()) {
                try {
                    IOntologyProvider provider = (IOntologyProvider)
                    		element.createExecutableExtension("provider");
                    addLocalOntologies(provider);
                } catch (CoreException e) {
                    throw new IOException(e);
                }
            }
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
        logger.info("Adding local ontology " + ns);
    }

	public void attachDIGReasoner() {
		if (reasoner != null) {
			if (reasoner instanceof DIGReasoner) {
                logger.warning("DIG reasoner already attached");
                return;
			}
			detachReasoner();
		}
        logger.info("Attaching DIG reasoner at " + getReasonerUrl());
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

	public void attachPelletReasoner() {
		if (reasoner != null) {
			if (reasoner instanceof org.mindswap.pellet.owlapi.Reasoner) {
                logger.warning("Pellet reasoner already attached");
                return;
			}
			detachReasoner();
		}
        logger.info("Attaching Pellet reasoner");
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

	public void attachTransitiveReasoner() {
		//TODO there is no transitive reasoner for OWLAPI
		attachPelletReasoner();
	}

	public void buildHierarchyMap(IOntClass forClass) throws OntException {
        logger.fine("building hierarchy for " + forClass);
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
		        logger.info(forClass + " equivalent to " + equivs.toString() + 
		                ", subclass of " + supers.toString());
		        equivClasses.put(owlClass, equivs);
		        superClasses.put(owlClass, supers);
			} catch (OWLReasonerException e) {
				logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
        }
	}

	public void checkConsistency() throws OntException {
        logger.info("Checking ontology consistency");
        if (reasoner != null) {
        	try {
				if (!reasoner.isConsistent(ontology.model)) {
					throw new OntException(ontology + " is inconsistent");
				}
				if ((instances != null) ? !reasoner.isConsistent(instances.model) : false) {
					throw new OntException(instances + " is inconsistent");
				}
			} catch (OWLReasonerException e) {
				throw new OntException(e);
			}
        }
	}

	public void detachReasoner() {
		if (reasoner == null) {
            logger.warning("Reasoner already detached");
            return;
		}
        logger.info("Detaching reasoner");
        reasoner = null;
	}

	public IOntModel getBaseOntology() {
		return ontology;
	}

	public IOntModel getInstances() {
		return instances;
	}

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

	public IOntModel getOntModel() {
		return ontology;
	}

	public void loadInstances(String url) {
        unloadInstances();
        logger.fine("Loading instance ontology " + url);
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

	public void loadInstances(InputStream in) {
        unloadInstances();
        logger.fine("Loading instance ontology from " + in);
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

	public void loadOntology(String url) {
		logger.fine("Loading ontology from " + url);
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
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		} catch (URISyntaxException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
        notifyOntologyChanged();
	}

	public void loadOntology(InputStream in) {
		logger.fine("Loading ontology from " + in);
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
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
        notifyOntologyChanged();
	}

	public void pruneHierarchyMap(IOntClass forClass) throws OntException {
        logger.fine("pruning hierarchy map for " + forClass);
        OWLClass owlClass = ((OWLClassAdapter)forClass).model;
        Set<OWLClass> obsolete = new HashSet<OWLClass>();
        Set<OWLClass> superCs = superClasses.get(owlClass);
        if (superCs == null) {
            throw new OntException(forClass + " superclasses not found in the hierarchy map");
        }
        for (OWLClass sc : superCs) {
        	Set<OWLClass> superSuperCs = superClasses.get(sc);
            if (superSuperCs != null) {
                obsolete.addAll(superSuperCs);
            }
        }
        logger.info(forClass + " obsolete subclass of " + obsolete.toString());
        obsoleteSuperClasses.put(owlClass, obsolete);        
	}

	public void saveOntology(OutputStream out) {
		try {
			mgr.saveOntology(ontology.model, new StreamOutputTarget(out));
		} catch (OWLException e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	public void unloadInstances() {
        if (instances != null) {
            logger.fine("Unloading current instance ontology");
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

	public void updateHierarchy(IOntClass forClass) throws OntException {
        logger.fine("augmenting hierarchy for " + forClass);
        OWLClass owlClass = ((OWLClassAdapter)forClass).model;
        Set<OWLClass> equivCs = equivClasses.get(owlClass);
        if (equivCs == null) {
            throw new OntException(forClass + " equivalent classes not found in the hierarchy map");
        }
        Set<OWLClass> superCs = superClasses.get(owlClass);
        if (superCs == null) {
            throw new OntException(forClass + " superclasses not found in the hierarchy map");
        }
        Set<OWLClass> obsoleteCs = obsoleteSuperClasses.get(owlClass);
        if (obsoleteCs == null) {
            throw new OntException(forClass + " obsolete superclasses not found in the hierarchy map");
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
                logger.info("*** adding " + c + " as equivalent class to " + forClass);
                OWLAxiom axiom = factory.getOWLEquivalentClassesAxiom(c, forClass);
                changes.add(new AddAxiom(ontology.model, axiom));
            } else {
                logger.fine(forClass + " already has equivalent class " + c);
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
                logger.info("*** adding " + c + " as direct superclass to " + forClass);
                OWLAxiom axiom = factory.getOWLSubClassAxiom(forClass, c);
                changes.add(new AddAxiom(ontology.model, axiom));
            } else {
                logger.fine(forClass + " already has superclass " + c);
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
                    logger.info("*** removing " + c + " as direct superclass from " + forClass);
            		changes.add(new RemoveAxiom(ontology.model, scAx));
            		isObsolete = true;
            		break;
            	}
            }
    		if (!isObsolete) {
                logger.fine(forClass + " does not have superclass " + c);
    		}
    	}
		mgr.applyChanges(changes);
    }

}
