/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package be.ac.vub.platformkit;

import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntModel;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Constraint</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link be.ac.vub.platformkit.Constraint#getSet <em>Set</em>}</li>
 *   <li>{@link be.ac.vub.platformkit.Constraint#getOntClassURI <em>Ont Class URI</em>}</li>
 * </ul>
 * </p>
 *
 * @see be.ac.vub.platformkit.PlatformkitPackage#getConstraint()
 * @model annotation="GenModel documentation='A single platform constraint. Corresponds to an OntClass.'"
 * @generated
 */
public interface Constraint extends IOntModelChangeListener {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String copyright = "(C) 2007-2008, Dennis Wagelaar, Vrije Universiteit Brussel";

	/**
	 * Returns the value of the '<em><b>Set</b></em>' container reference.
	 * It is bidirectional and its opposite is '{@link be.ac.vub.platformkit.ConstraintSet#getConstraint <em>Constraint</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Set</em>' container reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Set</em>' container reference.
	 * @see #setSet(ConstraintSet)
	 * @see be.ac.vub.platformkit.PlatformkitPackage#getConstraint_Set()
	 * @see be.ac.vub.platformkit.ConstraintSet#getConstraint
	 * @model opposite="constraint" required="true" transient="false"
	 *        annotation="GenModel documentation='The owning constraint set.'"
	 * @generated
	 */
	ConstraintSet getSet();

	/**
	 * Sets the value of the '{@link be.ac.vub.platformkit.Constraint#getSet <em>Set</em>}' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Set</em>' container reference.
	 * @see #getSet()
	 * @generated
	 */
	void setSet(ConstraintSet value);

	/**
	 * Returns the value of the '<em><b>Ont Class URI</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Ont Class URI</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Ont Class URI</em>' attribute.
	 * @see #setOntClassURI(String)
	 * @see be.ac.vub.platformkit.PlatformkitPackage#getConstraint_OntClassURI()
	 * @model required="true"
	 *        annotation="GenModel documentation='The URI that identifies the ontology model and the ontology class therein.'"
	 * @generated
	 */
	String getOntClassURI();

	/**
	 * Sets the value of the '{@link be.ac.vub.platformkit.Constraint#getOntClassURI <em>Ont Class URI</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Ont Class URI</em>' attribute.
	 * @see #getOntClassURI()
	 * @generated
	 */
	void setOntClassURI(String value);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model kind="operation"
	 *        annotation="GenModel documentation='Returns true if this constraint hold, false otherwise.'"
	 * @generated
	 */
	boolean isValid();
	
    /**
     * @return The inner ontology class, if it can be determined. Null otherwise.
     */
    IOntClass getOntClass();

    /**
     * @return The OntModel from the constraint set, if any, null otherwise.
     * @see #getSet()
     */
    IOntModel getOntModel();

} // Constraint