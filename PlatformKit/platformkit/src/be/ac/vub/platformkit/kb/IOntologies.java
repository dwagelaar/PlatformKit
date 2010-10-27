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
import java.util.Collection;
import java.util.List;

import be.ac.vub.platformkit.IOntModelChangeListener;
import be.ac.vub.platformkit.kb.util.OntException;

/**
 * Interface for ontology repositories 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public interface IOntologies {

	public static final String ONTOLOGY_EXT_POINT = "be.ac.vub.platformkit.ontology"; //$NON-NLS-1$
	public static final String KB_EXT_POINT = "be.ac.vub.platformkit.knowledgebase"; //$NON-NLS-1$
	public static final String BASE_NS = "http://soft.vub.ac.be/platformkit/2010/1/"; //$NON-NLS-1$
	public static final String DEPS_BASE_NS = BASE_NS + "dependencies/"; //$NON-NLS-1$
	public static final String LOCAL_INF_NS = BASE_NS + "inferred.owl"; //$NON-NLS-1$

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
	 * Selects the DL reasoner with the given ID.
	 * @param id
	 * @throws OntException if the reasoner does not exist
	 */
	public abstract void selectDLReasoner(String id) throws OntException;

	/**
	 * Attaches the DL reasoner to the knowledgebase.
	 */
	public abstract void attachDLReasoner() throws OntException;

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
	 * @throws OntException 
	 */
	public abstract IOntModel getBaseOntology() throws OntException;

	/**
	 * @return The inner ontology object.
	 * @throws OntException 
	 */
	public abstract IOntModel getOntModel() throws OntException;

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
	 * Replaces the base ontology by the ontology loaded from in.
	 * @param in
	 */
	public abstract void loadBaseOntology(InputStream in) throws OntException;

	/**
	 * Loads an instance ontology into the repository, removing the previous
	 * instance ontology.
	 * @param url the ontology url or namespace.
	 * @throws OntException 
	 */
	public abstract void loadInstances(String url) throws OntException;

	/**
	 * Loads an instance ontology into the repository, removing the previous
	 * instance ontology.
	 * @param in the inputstream containing the ontology.
	 * @throws OntException 
	 */
	public abstract void loadInstances(InputStream in) throws OntException;

	/**
	 * Removes current instance ontology, if any.
	 * @throws OntException 
	 */
	public abstract void unloadInstances() throws OntException;

	/**
	 * Writes the ontology to the given output stream.
	 * @param out
	 * @throws OntException 
	 */
	public abstract void saveOntology(OutputStream out) throws OntException;

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
	 * @throws OntException 
	 */
	public abstract List<IOntClass> getLocalNamedClasses() throws OntException;

	/**
	 * Updates the asserted class hierarchy with the inferred hierarchy for the local named classes.
	 * Requires {@link #buildHierarchyMap()} to be invoked first.
	 * @throws OntException 
	 */
	public abstract void updateHierarchy() throws OntException;

	/**
	 * Builds an internal map of the superclasses and equivalent classes of all local named classes in the base ontology.
	 * Requires DIG reasoner.
	 * Required for invocation of {@link #updateHierarchy()} and {@link #pruneHierarchyMap(IOntClass)}.
	 * @throws OntException 
	 */
	public abstract void buildHierarchyMap() throws OntException;

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

	/**
	 * Creates a new default Platformkit ontology with the given url.
	 * @param url
	 * @return the new ontology
	 * @throws OntException
	 */
	public abstract IOntModel createNewOntology(String url)
	throws OntException;

	/**
	 * Removes ont from the set of loaded ontologies. Does not remove
	 * any logical axioms that have been merged into the knowledge base.
	 * @param ont
	 * @throws OntException
	 */
	public void unloadOntology(IOntModel ont) throws OntException;

	/**
	 * @return all local ontologies
	 */
	public Collection<IOntModel> getLocalOntologies();

	/**
	 * Retrieves the local ontology registered under uri.
	 * @param uri
	 * @return the local ontology, or <code>null</code>.
	 * @throws OntException
	 */
	public IOntModel getLocalOntology(String uri);

	/**
	 * Loads a single ontology without adding it to the central knowledgebase.
	 * @param in
	 * @return the loaded ontology
	 * @throws OntException
	 */
	public IOntModel loadSingleOnt(InputStream in) throws OntException;

}