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
package be.ac.vub.platformkit.kb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import be.ac.vub.platformkit.IOntModelChangeListener;
import be.ac.vub.platformkit.kb.util.OntException;

/**
 * Shared functionality for ontology providers
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public abstract class AbstractOntologies implements IOntologies {

	protected static Logger logger = Logger.getLogger(LOGGER);
	protected List<IOntModelChangeListener> ontologyChangeListeners = new ArrayList<IOntModelChangeListener>();
	private String reasonerUrl = "http://localhost:8081"; //$NON-NLS-1$

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
	 * @throws OntException 
	 * @see #addOntModelChangeListener(IOntModelChangeListener)
	 */
	protected void notifyOntologyChanged() throws OntException {
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