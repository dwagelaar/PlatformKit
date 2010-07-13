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
package be.ac.vub.platformkit;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import be.ac.vub.platformkit.kb.IOntModel;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.util.OntException;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Constraint Set</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link be.ac.vub.platformkit.ConstraintSet#getSpace <em>Space</em>}</li>
 *   <li>{@link be.ac.vub.platformkit.ConstraintSet#getName <em>Name</em>}</li>
 *   <li>{@link be.ac.vub.platformkit.ConstraintSet#getConstraint <em>Constraint</em>}</li>
 * </ul>
 * </p>
 *
 * @see be.ac.vub.platformkit.PlatformkitPackage#getConstraintSet()
 * @model annotation="GenModel documentation='A set of platform constraints for a targeted entity.'"
 * @generated
 */
public interface ConstraintSet extends EObject {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String copyright = "(C) 2005-2010, Dennis Wagelaar, Vrije Universiteit Brussel";

	/**
	 * Returns the value of the '<em><b>Space</b></em>' container reference.
	 * It is bidirectional and its opposite is '{@link be.ac.vub.platformkit.ConstraintSpace#getConstraintSet <em>Constraint Set</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Space</em>' container reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Space</em>' container reference.
	 * @see #setSpace(ConstraintSpace)
	 * @see be.ac.vub.platformkit.PlatformkitPackage#getConstraintSet_Space()
	 * @see be.ac.vub.platformkit.ConstraintSpace#getConstraintSet
	 * @model opposite="constraintSet" required="true" transient="false"
	 *        annotation="GenModel documentation='The owning constraint space.'"
	 * @generated
	 */
	ConstraintSpace getSpace();

	/**
	 * Sets the value of the '{@link be.ac.vub.platformkit.ConstraintSet#getSpace <em>Space</em>}' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Space</em>' container reference.
	 * @see #getSpace()
	 * @generated
	 */
	void setSpace(ConstraintSpace value);

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Name</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see be.ac.vub.platformkit.PlatformkitPackage#getConstraintSet_Name()
	 * @model id="true" required="true"
	 *        annotation="GenModel documentation='The constraint set identifier. Also used as deployment redirection target when this constraint set refers to a deployable entity.'"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link be.ac.vub.platformkit.ConstraintSet#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Constraint</b></em>' containment reference list.
	 * The list contents are of type {@link be.ac.vub.platformkit.Constraint}.
	 * It is bidirectional and its opposite is '{@link be.ac.vub.platformkit.Constraint#getSet <em>Set</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Constraint</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Constraint</em>' containment reference list.
	 * @see be.ac.vub.platformkit.PlatformkitPackage#getConstraintSet_Constraint()
	 * @see be.ac.vub.platformkit.Constraint#getSet
	 * @model opposite="set" containment="true"
	 *        annotation="GenModel documentation='The constraints that are part of this constraint set.'"
	 * @generated
	 */
	EList<Constraint> getConstraint();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model kind="operation" exceptions="be.ac.vub.platformkit.OntException"
	 *        annotation="GenModel documentation='Returns true if all the constraints hold, false otherwise.'"
	 * @generated
	 */
	boolean isValid() throws OntException;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model kind="operation"
	 *        annotation="GenModel documentation='Returns all constraints in this set in order, most-specific first. Requires a reasoner.'"
	 * @generated
	 */
	EList<Constraint> getMostSpecific();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model kind="operation"
	 *        annotation="GenModel documentation='Returns all constraints in this set in order, least-specific first. Requires a reasoner.'"
	 * @generated
	 */
	EList<Constraint> getLeastSpecific();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model kind="operation" exceptions="be.ac.vub.platformkit.OntException"
	 *        annotation="GenModel documentation='Returns the intersection class constraint of all contained constraints.'"
	 * @generated
	 */
	Constraint getIntersection() throws OntException;

	/**
	 * Adds all constraints as listeners to kb.
	 * @param kb the knowledge base (may not be null).
	 */
	void addAllOntologyChangeListeners(IOntologies kb);
	
	/**
	 * Removes all constraints as listeners from kb.
	 * @param kb the knowledge base (may not be null).
	 */
	void removeAllOntologyChangeListeners(IOntologies kb);
	
	/**
	 * Sets the transient constraint space, for if this constraint set is not meant to
	 * be a persistent part of a constraint space.
	 * @param transientSpace
	 */
	void setTransientSpace(ConstraintSpace transientSpace);

	/**
	 * @return The OntModel from the constraint space, if any, null otherwise.
	 * @throws OntException 
	 * @see #getSpace()
	 * @see #setTransientSpace(ConstraintSpace)
	 */
	IOntModel getOntModel() throws OntException;

} // ConstraintSet