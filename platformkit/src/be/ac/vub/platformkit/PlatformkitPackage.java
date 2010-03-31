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

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see be.ac.vub.platformkit.PlatformkitFactory
 * @model kind="package"
 * @generated
 */
public interface PlatformkitPackage extends EPackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String copyright = "(C) 2005-2010, Dennis Wagelaar, Vrije Universiteit Brussel";

	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "platformkit";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "http://ssel.vub.ac.be/platformkit";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "platformkit";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	PlatformkitPackage eINSTANCE = be.ac.vub.platformkit.impl.PlatformkitPackageImpl.init();

	/**
	 * The meta object id for the '{@link be.ac.vub.platformkit.impl.ConstraintSpaceImpl <em>Constraint Space</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see be.ac.vub.platformkit.impl.ConstraintSpaceImpl
	 * @see be.ac.vub.platformkit.impl.PlatformkitPackageImpl#getConstraintSpace()
	 * @generated
	 */
	int CONSTRAINT_SPACE = 0;

	/**
	 * The feature id for the '<em><b>Ontology</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_SPACE__ONTOLOGY = 0;

	/**
	 * The feature id for the '<em><b>Constraint Set</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_SPACE__CONSTRAINT_SET = 1;

	/**
	 * The number of structural features of the '<em>Constraint Space</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_SPACE_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link be.ac.vub.platformkit.impl.ConstraintSetImpl <em>Constraint Set</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see be.ac.vub.platformkit.impl.ConstraintSetImpl
	 * @see be.ac.vub.platformkit.impl.PlatformkitPackageImpl#getConstraintSet()
	 * @generated
	 */
	int CONSTRAINT_SET = 1;

	/**
	 * The feature id for the '<em><b>Space</b></em>' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_SET__SPACE = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_SET__NAME = 1;

	/**
	 * The feature id for the '<em><b>Constraint</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_SET__CONSTRAINT = 2;

	/**
	 * The number of structural features of the '<em>Constraint Set</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_SET_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link be.ac.vub.platformkit.impl.ConstraintImpl <em>Constraint</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see be.ac.vub.platformkit.impl.ConstraintImpl
	 * @see be.ac.vub.platformkit.impl.PlatformkitPackageImpl#getConstraint()
	 * @generated
	 */
	int CONSTRAINT = 2;

	/**
	 * The meta object id for the '{@link be.ac.vub.platformkit.IOntModelChangeListener <em>IOnt Model Change Listener</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see be.ac.vub.platformkit.IOntModelChangeListener
	 * @see be.ac.vub.platformkit.impl.PlatformkitPackageImpl#getIOntModelChangeListener()
	 * @generated
	 */
	int IONT_MODEL_CHANGE_LISTENER = 4;

	/**
	 * The number of structural features of the '<em>IOnt Model Change Listener</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int IONT_MODEL_CHANGE_LISTENER_FEATURE_COUNT = 0;


	/**
	 * The feature id for the '<em><b>Set</b></em>' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT__SET = IONT_MODEL_CHANGE_LISTENER_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Ont Class URI</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT__ONT_CLASS_URI = IONT_MODEL_CHANGE_LISTENER_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_FEATURE_COUNT = IONT_MODEL_CHANGE_LISTENER_FEATURE_COUNT + 2;


	/**
	 * The meta object id for the '{@link be.ac.vub.platformkit.kb.IOntModel <em>IOnt Model</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see be.ac.vub.platformkit.kb.IOntModel
	 * @see be.ac.vub.platformkit.impl.PlatformkitPackageImpl#getIOntModel()
	 * @generated
	 */
	int IONT_MODEL = 3;

	/**
	 * The number of structural features of the '<em>IOnt Model</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int IONT_MODEL_FEATURE_COUNT = 0;

	/**
	 * The meta object id for the '{@link be.ac.vub.platformkit.kb.util.OntException <em>Ont Exception</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see be.ac.vub.platformkit.kb.util.OntException
	 * @see be.ac.vub.platformkit.impl.PlatformkitPackageImpl#getOntException()
	 * @generated
	 */
	int ONT_EXCEPTION = 5;

	/**
	 * The number of structural features of the '<em>Ont Exception</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ONT_EXCEPTION_FEATURE_COUNT = 0;

	/**
	 * Returns the meta object for class '{@link be.ac.vub.platformkit.ConstraintSpace <em>Constraint Space</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Constraint Space</em>'.
	 * @see be.ac.vub.platformkit.ConstraintSpace
	 * @generated
	 */
	EClass getConstraintSpace();

	/**
	 * Returns the meta object for the attribute list '{@link be.ac.vub.platformkit.ConstraintSpace#getOntology <em>Ontology</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Ontology</em>'.
	 * @see be.ac.vub.platformkit.ConstraintSpace#getOntology()
	 * @see #getConstraintSpace()
	 * @generated
	 */
	EAttribute getConstraintSpace_Ontology();

	/**
	 * Returns the meta object for the containment reference list '{@link be.ac.vub.platformkit.ConstraintSpace#getConstraintSet <em>Constraint Set</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Constraint Set</em>'.
	 * @see be.ac.vub.platformkit.ConstraintSpace#getConstraintSet()
	 * @see #getConstraintSpace()
	 * @generated
	 */
	EReference getConstraintSpace_ConstraintSet();

	/**
	 * Returns the meta object for class '{@link be.ac.vub.platformkit.ConstraintSet <em>Constraint Set</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Constraint Set</em>'.
	 * @see be.ac.vub.platformkit.ConstraintSet
	 * @generated
	 */
	EClass getConstraintSet();

	/**
	 * Returns the meta object for the container reference '{@link be.ac.vub.platformkit.ConstraintSet#getSpace <em>Space</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the container reference '<em>Space</em>'.
	 * @see be.ac.vub.platformkit.ConstraintSet#getSpace()
	 * @see #getConstraintSet()
	 * @generated
	 */
	EReference getConstraintSet_Space();

	/**
	 * Returns the meta object for the attribute '{@link be.ac.vub.platformkit.ConstraintSet#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see be.ac.vub.platformkit.ConstraintSet#getName()
	 * @see #getConstraintSet()
	 * @generated
	 */
	EAttribute getConstraintSet_Name();

	/**
	 * Returns the meta object for the containment reference list '{@link be.ac.vub.platformkit.ConstraintSet#getConstraint <em>Constraint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Constraint</em>'.
	 * @see be.ac.vub.platformkit.ConstraintSet#getConstraint()
	 * @see #getConstraintSet()
	 * @generated
	 */
	EReference getConstraintSet_Constraint();

	/**
	 * Returns the meta object for class '{@link be.ac.vub.platformkit.Constraint <em>Constraint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Constraint</em>'.
	 * @see be.ac.vub.platformkit.Constraint
	 * @generated
	 */
	EClass getConstraint();

	/**
	 * Returns the meta object for the container reference '{@link be.ac.vub.platformkit.Constraint#getSet <em>Set</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the container reference '<em>Set</em>'.
	 * @see be.ac.vub.platformkit.Constraint#getSet()
	 * @see #getConstraint()
	 * @generated
	 */
	EReference getConstraint_Set();

	/**
	 * Returns the meta object for the attribute '{@link be.ac.vub.platformkit.Constraint#getOntClassURI <em>Ont Class URI</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ont Class URI</em>'.
	 * @see be.ac.vub.platformkit.Constraint#getOntClassURI()
	 * @see #getConstraint()
	 * @generated
	 */
	EAttribute getConstraint_OntClassURI();

	/**
	 * Returns the meta object for class '{@link be.ac.vub.platformkit.kb.IOntModel <em>IOnt Model</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>IOnt Model</em>'.
	 * @see be.ac.vub.platformkit.kb.IOntModel
	 * @model instanceClass="be.ac.vub.platformkit.kb.IOntModel"
	 * @generated
	 */
	EClass getIOntModel();

	/**
	 * Returns the meta object for class '{@link be.ac.vub.platformkit.IOntModelChangeListener <em>IOnt Model Change Listener</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>IOnt Model Change Listener</em>'.
	 * @see be.ac.vub.platformkit.IOntModelChangeListener
	 * @generated
	 */
	EClass getIOntModelChangeListener();

	/**
	 * Returns the meta object for class '{@link be.ac.vub.platformkit.kb.util.OntException <em>Ont Exception</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Ont Exception</em>'.
	 * @see be.ac.vub.platformkit.kb.util.OntException
	 * @model instanceClass="be.ac.vub.platformkit.kb.util.OntException"
	 * @generated
	 */
	EClass getOntException();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	PlatformkitFactory getPlatformkitFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals  {
		/**
		 * The meta object literal for the '{@link be.ac.vub.platformkit.impl.ConstraintSpaceImpl <em>Constraint Space</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see be.ac.vub.platformkit.impl.ConstraintSpaceImpl
		 * @see be.ac.vub.platformkit.impl.PlatformkitPackageImpl#getConstraintSpace()
		 * @generated
		 */
		EClass CONSTRAINT_SPACE = eINSTANCE.getConstraintSpace();

		/**
		 * The meta object literal for the '<em><b>Ontology</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute CONSTRAINT_SPACE__ONTOLOGY = eINSTANCE.getConstraintSpace_Ontology();

		/**
		 * The meta object literal for the '<em><b>Constraint Set</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CONSTRAINT_SPACE__CONSTRAINT_SET = eINSTANCE.getConstraintSpace_ConstraintSet();

		/**
		 * The meta object literal for the '{@link be.ac.vub.platformkit.impl.ConstraintSetImpl <em>Constraint Set</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see be.ac.vub.platformkit.impl.ConstraintSetImpl
		 * @see be.ac.vub.platformkit.impl.PlatformkitPackageImpl#getConstraintSet()
		 * @generated
		 */
		EClass CONSTRAINT_SET = eINSTANCE.getConstraintSet();

		/**
		 * The meta object literal for the '<em><b>Space</b></em>' container reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CONSTRAINT_SET__SPACE = eINSTANCE.getConstraintSet_Space();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute CONSTRAINT_SET__NAME = eINSTANCE.getConstraintSet_Name();

		/**
		 * The meta object literal for the '<em><b>Constraint</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CONSTRAINT_SET__CONSTRAINT = eINSTANCE.getConstraintSet_Constraint();

		/**
		 * The meta object literal for the '{@link be.ac.vub.platformkit.impl.ConstraintImpl <em>Constraint</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see be.ac.vub.platformkit.impl.ConstraintImpl
		 * @see be.ac.vub.platformkit.impl.PlatformkitPackageImpl#getConstraint()
		 * @generated
		 */
		EClass CONSTRAINT = eINSTANCE.getConstraint();

		/**
		 * The meta object literal for the '<em><b>Set</b></em>' container reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference CONSTRAINT__SET = eINSTANCE.getConstraint_Set();

		/**
		 * The meta object literal for the '<em><b>Ont Class URI</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute CONSTRAINT__ONT_CLASS_URI = eINSTANCE.getConstraint_OntClassURI();

		/**
		 * The meta object literal for the '{@link be.ac.vub.platformkit.kb.IOntModel <em>IOnt Model</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see be.ac.vub.platformkit.kb.IOntModel
		 * @see be.ac.vub.platformkit.impl.PlatformkitPackageImpl#getIOntModel()
		 * @generated
		 */
		EClass IONT_MODEL = eINSTANCE.getIOntModel();

		/**
		 * The meta object literal for the '{@link be.ac.vub.platformkit.IOntModelChangeListener <em>IOnt Model Change Listener</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see be.ac.vub.platformkit.IOntModelChangeListener
		 * @see be.ac.vub.platformkit.impl.PlatformkitPackageImpl#getIOntModelChangeListener()
		 * @generated
		 */
		EClass IONT_MODEL_CHANGE_LISTENER = eINSTANCE.getIOntModelChangeListener();

		/**
		 * The meta object literal for the '{@link be.ac.vub.platformkit.kb.util.OntException <em>Ont Exception</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see be.ac.vub.platformkit.kb.util.OntException
		 * @see be.ac.vub.platformkit.impl.PlatformkitPackageImpl#getOntException()
		 * @generated
		 */
		EClass ONT_EXCEPTION = eINSTANCE.getOntException();

	}

} //PlatformkitPackage
