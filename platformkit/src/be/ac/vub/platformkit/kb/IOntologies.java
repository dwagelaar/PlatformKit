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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import be.ac.vub.platformkit.IOntModelChangeListener;
import be.ac.vub.platformkit.kb.util.OntException;

/**
 * Interface for ontology repositories 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public interface IOntologies {

	public static final String LOGGER = "be.ac.vub.platformkit"; //$NON-NLS-1$
	public static final String ONTOLOGY_EXT_POINT = "be.ac.vub.platformkit.ontology"; //$NON-NLS-1$
	public static final String LOCAL_INF_NS = "http://local/platformkit/inferred.owl"; //$NON-NLS-1$

	/**
	 * Adds listener for changes to {@link #getOntModel()}.
	 * @param listener
	 */
	public abstract void addOntModelChangeListener(
			IOntModelChangeListener listener);

	/**
	 * Removes listener for changes to {@link #getOntModel()}.
	 * @param listener
	 */
	public abstract void removeOntModelChangeListener(
			IOntModelChangeListener listener);

	/**
	 * Attaches the DIG reasoner to the knowledgebase.
	 */
	public abstract void attachDIGReasoner();

	/**
	 * Attaches the Pellet OWL reasoner to the knowledgebase.
	 * @throws OntException 
	 */
	public abstract void attachPelletReasoner() throws OntException;

	/**
	 * Attaches the transitive reasoner to the knowledgebase.
	 * @throws OntException 
	 */
	public abstract void attachTransitiveReasoner() throws OntException;

	/**
	 * Detaches the current reasoner from the knowledgebase.
	 * @throws OntException 
	 */
	public abstract void detachReasoner() throws OntException;

	/**
	 * @return The base ontology object (excluding reasoner results).
	 */
	public abstract IOntModel getBaseOntology();

	/**
	 * @return The inner ontology object.
	 */
	public abstract IOntModel getOntModel();

	/**
	 * @return The inner instances ontology object.
	 */
	public abstract IOntModel getInstances();

	/**
	 * Loads an ontology into the repository.
	 * @param url the ontology url or namespace.
	 * @throws OntException 
	 */
	public abstract void loadOntology(String url) throws OntException;

	/**
	 * Loads an ontology into the repository.
	 * @param in the inputstream containing the ontology.
	 * @throws OntException 
	 */
	public abstract void loadOntology(InputStream in) throws OntException;

	/**
	 * Loads an instance ontology into the repository, removing the previous
	 * instance ontology.
	 * @param url the ontology url or namespace.
	 */
	public abstract void loadInstances(String url);

	/**
	 * Loads an instance ontology into the repository, removing the previous
	 * instance ontology.
	 * @param in the inputstream containing the ontology.
	 */
	public abstract void loadInstances(InputStream in);

	/**
	 * Removes current instance ontology, if any.
	 */
	public abstract void unloadInstances();

	/**
	 * Writes the ontology to the given output stream.
	 * @param out
	 */
	public abstract void saveOntology(OutputStream out);

	/**
	 * Checks if all classes are satisfiable.
	 * @throws OntException if not all classes are satifiable.
	 */
	public abstract void checkConsistency() throws OntException;

	/**
	 * Adds all known local ontologies to the document manager.
	 * @param provider The ontology provider.
	 * @throws IOException if the local ontology mapping could not be read.
	 */
	public abstract void addLocalOntologies(IOntologyProvider provider)
	throws IOException;

	/**
	 * @param reasonerUrl The reasonerUrl to set.
	 */
	public abstract void setReasonerUrl(String reasonerUrl);

	/**
	 * @return Returns the reasonerUrl.
	 */
	public abstract String getReasonerUrl();

	/**
	 * @return The named ontology classes with a local prefix URI
	 */
	public abstract List<IOntClass> getLocalNamedClasses();

	/**
	 * Updates the asserted class hierarchy with the inferred hierarchy for the local named classes.
	 * Requires {@link #buildHierarchyMap()} to be invoked first.
	 */
	public abstract void updateHierarchy();

	/**
	 * Builds an internal map of the superclasses and equivalent classes of all local named classes in the base ontology.
	 * Requires DIG reasoner.
	 * Required for invocation of {@link #updateHierarchy()} and {@link #pruneHierarchyMap(IOntClass)}.
	 */
	public abstract void buildHierarchyMap();

	/**
	 * Updates the asserted class hierarchy with the inferred hierarchy for the given class.
	 * Requires pre-built and pruned hierarchy map by {@link #buildHierarchyMap(IOntClass)} and
	 * {@link #pruneHierarchyMap(IOntClass)}.
	 * @param forClass
	 * @throws OntException if the class cannot be found in the base ontology.
	 */
	public abstract void updateHierarchy(IOntClass forClass)
	throws OntException;

	/**
	 * Prunes the class hierarchy map for the given class.
	 * Requires pre-built hierarchy map by @see #buildHierarchyMap(OntClass).
	 * @param forClass
	 * @throws OntException if the class cannot be found in the hierarchy map.
	 */
	public abstract void pruneHierarchyMap(IOntClass forClass)
	throws OntException;

	/**
	 * Builds an internal map of the superclasses and equivalent classes of forClass in the base ontology.
	 * Required for invocation of {@link #updateHierarchy(IOntClass)} and {@link #pruneHierarchyMap(IOntClass)}
	 * @param forClass
	 * @throws OntException if the class cannot be found in the base ontology.
	 */
	public abstract void buildHierarchyMap(IOntClass forClass)
	throws OntException;

}