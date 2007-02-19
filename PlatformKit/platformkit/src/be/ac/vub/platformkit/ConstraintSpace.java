/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package be.ac.vub.platformkit;

import java.io.IOException;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import be.ac.vub.platformkit.kb.Ontologies;
import be.ac.vub.platformkit.util.PathResolver;

import com.hp.hpl.jena.ontology.OntModel;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Constraint Space</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link be.ac.vub.platformkit.ConstraintSpace#getOntology <em>Ontology</em>}</li>
 *   <li>{@link be.ac.vub.platformkit.ConstraintSpace#getConstraintSet <em>Constraint Set</em>}</li>
 * </ul>
 * </p>
 *
 * @see be.ac.vub.platformkit.PlatformkitPackage#getConstraintSpace()
 * @model annotation="GenModel documentation='The total space of platform constraint sets to consider.'"
 * @generated
 */
public interface ConstraintSpace extends EObject {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String copyright = "(C) 2007, Dennis Wagelaar, Vrije Universiteit Brussel";

	/**
	 * Returns the value of the '<em><b>Ontology</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Ontology</em>' attribute list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Ontology</em>' attribute list.
	 * @see be.ac.vub.platformkit.PlatformkitPackage#getConstraintSpace_Ontology()
	 * @model type="java.lang.String"
	 *        annotation="GenModel documentation='The list of relative ontology URIs.'"
	 * @generated
	 */
	EList getOntology();

	/**
	 * Returns the value of the '<em><b>Constraint Set</b></em>' containment reference list.
	 * The list contents are of type {@link be.ac.vub.platformkit.ConstraintSet}.
	 * It is bidirectional and its opposite is '{@link be.ac.vub.platformkit.ConstraintSet#getSpace <em>Space</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Constraint Set</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Constraint Set</em>' containment reference list.
	 * @see be.ac.vub.platformkit.PlatformkitPackage#getConstraintSpace_ConstraintSet()
	 * @see be.ac.vub.platformkit.ConstraintSet#getSpace
	 * @model type="be.ac.vub.platformkit.ConstraintSet" opposite="space" containment="true"
	 *        annotation="GenModel documentation='The constraint sets that are part of this constraint space.'"
	 * @generated
	 */
	EList getConstraintSet();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model kind="operation"
	 *        annotation="GenModel documentation='Returns a constraint set consisting of all the intersection classes of all the constraints. Creates the IntersectionClasses for all ConstraintSets as necessary.'"
	 * @generated
	 */
	ConstraintSet getIntersectionSet();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model type="be.ac.vub.platformkit.ConstraintSet"
	 *        annotation="GenModel documentation='Returns all or valid constraint sets in order, least-specific first. Requires a reasoner.'"
	 * @generated
	 */
	EList getLeastSpecific(boolean validate);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model type="be.ac.vub.platformkit.ConstraintSet"
	 *        annotation="GenModel documentation='Returns all or valid constraint sets in order, most-specific first. Requires a reasoner.'"
	 * @generated
	 */
	EList getMostSpecific(boolean validate);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model kind="operation" type="be.ac.vub.platformkit.ConstraintSet"
	 *        annotation="GenModel documentation='Returns valid constraint sets. Requires a reasoner.'"
	 * @generated
	 */
	EList getValid();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model kind="operation" type="be.ac.vub.platformkit.ConstraintSet"
	 *        annotation="GenModel documentation='Returns invalid constraint sets. Requires a reasoner.'"
	 * @generated
	 */
	EList getInvalid();

    /**
     * Sets the path resolver for finding referenced ontologies.
     * @param pathResolver
     */
	void setPathResolver(PathResolver pathResolver);

	/**
	 * Sets the knowledge base (default = null).
	 * @param knowledgeBase The knowledge base to use.
	 */
	void setKnowledgeBase(Ontologies knowledgeBase);

	/**
	 * @return The Ontologies knowledge base (default = null).
	 */
	Ontologies getKnowledgeBase();
	
	/**
	 * @return The OntModel from the knowledge base.
	 * @see #getKnowledgeBase()
	 */
	OntModel getOntModel();
	
    /**
     * Loads all ontologies into the knowledge base of this constraint space.
     * Requires knowledge base to be set.
     * @see #setKnowledgeBase(Ontologies)
     * @param searchPreClassified If true, searches for the pre-classified ontology.
     * @throws IOException if the input ontologies cannot be read.
     * @return True if the pre-classified ontology is found, false otherwise.
     */
	boolean init(boolean searchPreClassified)
    throws IOException;
    
} // ConstraintSpace