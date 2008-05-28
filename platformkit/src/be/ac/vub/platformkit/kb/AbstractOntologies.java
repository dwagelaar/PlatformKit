package be.ac.vub.platformkit.kb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import be.ac.vub.platformkit.IOntModelChangeListener;
import be.ac.vub.platformkit.kb.util.OntException;

public abstract class AbstractOntologies implements IOntologies {

	protected static Logger logger = Logger.getLogger(LOGGER);
	protected List<IOntModelChangeListener> ontologyChangeListeners = new ArrayList<IOntModelChangeListener>();
	private String reasonerUrl = "http://localhost:8081";

	public AbstractOntologies() {
		super();
	}

	public void addOntModelChangeListener(IOntModelChangeListener listener) {
		ontologyChangeListeners.add(listener);
	}

	public void removeOntModelChangeListener(IOntModelChangeListener listener) {
		ontologyChangeListeners.remove(listener);
	}

    /**
     * Notifies all ontology change listeners of the new ontology model.
     * @see #addOntModelChangeListener(IOntModelChangeListener)
     */
    protected void notifyOntologyChanged() {
    	IOntModel ontology = getOntModel();
    	for (Iterator<IOntModelChangeListener> it = ontologyChangeListeners.iterator(); it.hasNext();) {
    		it.next().ontModelChanged(ontology);
    	}
    }

	public void buildHierarchyMap() {
	    List<IOntClass> forClasses = getLocalNamedClasses();
	    for (int i = 0; i < forClasses.size(); i++) {
	        try {
	            buildHierarchyMap(forClasses.get(i));
	        } catch (OntException nfe) {
	            logger.warning(nfe.getMessage());
	        }
	    }
	}

	public void updateHierarchy() {
	    List<IOntClass> forClasses = getLocalNamedClasses();
	    for (int i = 0; i < forClasses.size(); i++) {
	        try {
	            pruneHierarchyMap(forClasses.get(i));
	        } catch (OntException nfe) {
	            logger.warning(nfe.getMessage());
	        }
	    }
	    for (int i = 0; i < forClasses.size(); i++) {
	        try {
	            updateHierarchy(forClasses.get(i));
	        } catch (OntException nfe) {
	            logger.warning(nfe.getMessage());
	        }
	    }
	}

	public void setReasonerUrl(String reasonerUrl) {
	    this.reasonerUrl = reasonerUrl;
	}

	public String getReasonerUrl() {
	    return reasonerUrl;
	}

}