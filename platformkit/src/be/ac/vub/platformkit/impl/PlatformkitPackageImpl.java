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
package be.ac.vub.platformkit.impl;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.impl.EPackageImpl;

import be.ac.vub.platformkit.Constraint;
import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.IOntModelChangeListener;
import be.ac.vub.platformkit.PlatformkitFactory;
import be.ac.vub.platformkit.PlatformkitPackage;
import be.ac.vub.platformkit.kb.IOntModel;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class PlatformkitPackageImpl extends EPackageImpl implements PlatformkitPackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final String copyright = "(C) 2005-2010, Dennis Wagelaar, Vrije Universiteit Brussel";

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass constraintSpaceEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass constraintSetEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass constraintEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass iOntModelEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass iOntModelChangeListenerEClass = null;

	/**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
	 * package URI value.
	 * <p>Note: the correct way to create the package is via the static
	 * factory method {@link #init init()}, which also performs
	 * initialization of the package, or returns the registered package,
	 * if one already exists.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see be.ac.vub.platformkit.PlatformkitPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private PlatformkitPackageImpl() {
		super(eNS_URI, PlatformkitFactory.eINSTANCE);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
	 * 
	 * <p>This method is used to initialize {@link PlatformkitPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static PlatformkitPackage init() {
		if (isInited) return (PlatformkitPackage)EPackage.Registry.INSTANCE.getEPackage(PlatformkitPackage.eNS_URI);

		// Obtain or create and register package
		PlatformkitPackageImpl thePlatformkitPackage = (PlatformkitPackageImpl)(EPackage.Registry.INSTANCE.get(eNS_URI) instanceof PlatformkitPackageImpl ? EPackage.Registry.INSTANCE.get(eNS_URI) : new PlatformkitPackageImpl());

		isInited = true;

		// Create package meta-data objects
		thePlatformkitPackage.createPackageContents();

		// Initialize created meta-data
		thePlatformkitPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		thePlatformkitPackage.freeze();

  
		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(PlatformkitPackage.eNS_URI, thePlatformkitPackage);
		return thePlatformkitPackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getConstraintSpace() {
		return constraintSpaceEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConstraintSpace_Ontology() {
		return (EAttribute)constraintSpaceEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getConstraintSpace_ConstraintSet() {
		return (EReference)constraintSpaceEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getConstraintSet() {
		return constraintSetEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getConstraintSet_Space() {
		return (EReference)constraintSetEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConstraintSet_Name() {
		return (EAttribute)constraintSetEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getConstraintSet_Constraint() {
		return (EReference)constraintSetEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getConstraint() {
		return constraintEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getConstraint_Set() {
		return (EReference)constraintEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConstraint_OntClassURI() {
		return (EAttribute)constraintEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getIOntModel() {
		return iOntModelEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getIOntModelChangeListener() {
		return iOntModelChangeListenerEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PlatformkitFactory getPlatformkitFactory() {
		return (PlatformkitFactory)getEFactoryInstance();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isCreated = false;

	/**
	 * Creates the meta-model objects for the package.  This method is
	 * guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void createPackageContents() {
		if (isCreated) return;
		isCreated = true;

		// Create classes and their features
		constraintSpaceEClass = createEClass(CONSTRAINT_SPACE);
		createEAttribute(constraintSpaceEClass, CONSTRAINT_SPACE__ONTOLOGY);
		createEReference(constraintSpaceEClass, CONSTRAINT_SPACE__CONSTRAINT_SET);

		constraintSetEClass = createEClass(CONSTRAINT_SET);
		createEReference(constraintSetEClass, CONSTRAINT_SET__SPACE);
		createEAttribute(constraintSetEClass, CONSTRAINT_SET__NAME);
		createEReference(constraintSetEClass, CONSTRAINT_SET__CONSTRAINT);

		constraintEClass = createEClass(CONSTRAINT);
		createEReference(constraintEClass, CONSTRAINT__SET);
		createEAttribute(constraintEClass, CONSTRAINT__ONT_CLASS_URI);

		iOntModelEClass = createEClass(IONT_MODEL);

		iOntModelChangeListenerEClass = createEClass(IONT_MODEL_CHANGE_LISTENER);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isInitialized = false;

	/**
	 * Complete the initialization of the package and its meta-model.  This
	 * method is guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void initializePackageContents() {
		if (isInitialized) return;
		isInitialized = true;

		// Initialize package
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Create type parameters

		// Set bounds for type parameters

		// Add supertypes to classes
		constraintEClass.getESuperTypes().add(this.getIOntModelChangeListener());

		// Initialize classes and features; add operations and parameters
		initEClass(constraintSpaceEClass, ConstraintSpace.class, "ConstraintSpace", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS); //$NON-NLS-1$
		initEAttribute(getConstraintSpace_Ontology(), ecorePackage.getEString(), "ontology", null, 0, -1, ConstraintSpace.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED); //$NON-NLS-1$
		initEReference(getConstraintSpace_ConstraintSet(), this.getConstraintSet(), this.getConstraintSet_Space(), "constraintSet", null, 0, -1, ConstraintSpace.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED); //$NON-NLS-1$

		addEOperation(constraintSpaceEClass, this.getConstraintSet(), "getIntersectionSet", 0, 1, IS_UNIQUE, IS_ORDERED); //$NON-NLS-1$

		EOperation op = addEOperation(constraintSpaceEClass, this.getConstraintSet(), "getMostSpecific", 0, -1, IS_UNIQUE, IS_ORDERED); //$NON-NLS-1$
		addEParameter(op, ecorePackage.getEBoolean(), "validate", 0, 1, IS_UNIQUE, IS_ORDERED); //$NON-NLS-1$

		op = addEOperation(constraintSpaceEClass, this.getConstraintSet(), "getLeastSpecific", 0, -1, IS_UNIQUE, IS_ORDERED); //$NON-NLS-1$
		addEParameter(op, ecorePackage.getEBoolean(), "validate", 0, 1, IS_UNIQUE, IS_ORDERED); //$NON-NLS-1$

		addEOperation(constraintSpaceEClass, this.getConstraintSet(), "getValid", 0, -1, IS_UNIQUE, IS_ORDERED); //$NON-NLS-1$

		addEOperation(constraintSpaceEClass, this.getConstraintSet(), "getInvalid", 0, -1, IS_UNIQUE, IS_ORDERED); //$NON-NLS-1$

		initEClass(constraintSetEClass, ConstraintSet.class, "ConstraintSet", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS); //$NON-NLS-1$
		initEReference(getConstraintSet_Space(), this.getConstraintSpace(), this.getConstraintSpace_ConstraintSet(), "space", null, 1, 1, ConstraintSet.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED); //$NON-NLS-1$
		initEAttribute(getConstraintSet_Name(), ecorePackage.getEString(), "name", null, 1, 1, ConstraintSet.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED); //$NON-NLS-1$
		initEReference(getConstraintSet_Constraint(), this.getConstraint(), this.getConstraint_Set(), "constraint", null, 0, -1, ConstraintSet.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED); //$NON-NLS-1$

		addEOperation(constraintSetEClass, ecorePackage.getEBoolean(), "isValid", 0, 1, IS_UNIQUE, IS_ORDERED); //$NON-NLS-1$

		addEOperation(constraintSetEClass, this.getConstraint(), "getMostSpecific", 0, -1, IS_UNIQUE, IS_ORDERED); //$NON-NLS-1$

		addEOperation(constraintSetEClass, this.getConstraint(), "getLeastSpecific", 0, -1, IS_UNIQUE, IS_ORDERED); //$NON-NLS-1$

		addEOperation(constraintSetEClass, this.getConstraint(), "getIntersection", 0, 1, IS_UNIQUE, IS_ORDERED); //$NON-NLS-1$

		initEClass(constraintEClass, Constraint.class, "Constraint", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS); //$NON-NLS-1$
		initEReference(getConstraint_Set(), this.getConstraintSet(), this.getConstraintSet_Constraint(), "set", null, 1, 1, Constraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED); //$NON-NLS-1$
		initEAttribute(getConstraint_OntClassURI(), ecorePackage.getEString(), "ontClassURI", null, 1, 1, Constraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED); //$NON-NLS-1$

		addEOperation(constraintEClass, ecorePackage.getEBoolean(), "isValid", 0, 1, IS_UNIQUE, IS_ORDERED); //$NON-NLS-1$

		initEClass(iOntModelEClass, IOntModel.class, "IOntModel", IS_ABSTRACT, IS_INTERFACE, !IS_GENERATED_INSTANCE_CLASS); //$NON-NLS-1$

		initEClass(iOntModelChangeListenerEClass, IOntModelChangeListener.class, "IOntModelChangeListener", IS_ABSTRACT, IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS); //$NON-NLS-1$

		op = addEOperation(iOntModelChangeListenerEClass, null, "ontModelChanged", 0, 1, IS_UNIQUE, IS_ORDERED); //$NON-NLS-1$
		addEParameter(op, this.getIOntModel(), "ontModel", 0, 1, IS_UNIQUE, IS_ORDERED); //$NON-NLS-1$

		// Create resource
		createResource(eNS_URI);

		// Create annotations
		// GenModel
		createGenModelAnnotations();
	}

	/**
	 * Initializes the annotations for <b>GenModel</b>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void createGenModelAnnotations() {
		String source = "GenModel"; //$NON-NLS-1$		
		addAnnotation
		  (constraintSpaceEClass, 
		   source, 
		   new String[] {
			 "documentation", "The total space of platform constraint sets to consider." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (constraintSpaceEClass.getEOperations().get(0), 
		   source, 
		   new String[] {
			 "documentation", "Returns a constraint set consisting of all the intersection classes of all the constraints. Creates the IntersectionClasses for all ConstraintSets as necessary." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (constraintSpaceEClass.getEOperations().get(1), 
		   source, 
		   new String[] {
			 "documentation", "Returns all or valid constraint sets in order, most-specific first. Requires a reasoner." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (constraintSpaceEClass.getEOperations().get(2), 
		   source, 
		   new String[] {
			 "documentation", "Returns all or valid constraint sets in order, least-specific first. Requires a reasoner." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (constraintSpaceEClass.getEOperations().get(3), 
		   source, 
		   new String[] {
			 "documentation", "Returns valid constraint sets. Requires a reasoner." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (constraintSpaceEClass.getEOperations().get(4), 
		   source, 
		   new String[] {
			 "documentation", "Returns invalid constraint sets. Requires a reasoner." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (getConstraintSpace_Ontology(), 
		   source, 
		   new String[] {
			 "documentation", "The list of relative ontology URIs." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (getConstraintSpace_ConstraintSet(), 
		   source, 
		   new String[] {
			 "documentation", "The constraint sets that are part of this constraint space." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (constraintSetEClass, 
		   source, 
		   new String[] {
			 "documentation", "A set of platform constraints for a targeted entity." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (constraintSetEClass.getEOperations().get(0), 
		   source, 
		   new String[] {
			 "documentation", "Returns true if all the constraints hold, false otherwise." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (constraintSetEClass.getEOperations().get(1), 
		   source, 
		   new String[] {
			 "documentation", "Returns all constraints in this set in order, most-specific first. Requires a reasoner." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (constraintSetEClass.getEOperations().get(2), 
		   source, 
		   new String[] {
			 "documentation", "Returns all constraints in this set in order, least-specific first. Requires a reasoner." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (constraintSetEClass.getEOperations().get(3), 
		   source, 
		   new String[] {
			 "documentation", "Returns the intersection class constraint of all contained constraints." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (getConstraintSet_Space(), 
		   source, 
		   new String[] {
			 "documentation", "The owning constraint space." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (getConstraintSet_Name(), 
		   source, 
		   new String[] {
			 "documentation", "The constraint set identifier. Also used as deployment redirection target when this constraint set refers to a deployable entity." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (getConstraintSet_Constraint(), 
		   source, 
		   new String[] {
			 "documentation", "The constraints that are part of this constraint set." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (constraintEClass, 
		   source, 
		   new String[] {
			 "documentation", "A single platform constraint. Corresponds to an OntClass." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (constraintEClass.getEOperations().get(0), 
		   source, 
		   new String[] {
			 "documentation", "Returns true if this constraint hold, false otherwise." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (getConstraint_Set(), 
		   source, 
		   new String[] {
			 "documentation", "The owning constraint set." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (getConstraint_OntClassURI(), 
		   source, 
		   new String[] {
			 "documentation", "The URI that identifies the ontology model and the ontology class therein." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (iOntModelChangeListenerEClass, 
		   source, 
		   new String[] {
			 "documentation", "Change listener interface for {@link Ontologies#getOntModel()}" //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  (iOntModelChangeListenerEClass.getEOperations().get(0), 
		   source, 
		   new String[] {
			 "documentation", "Invoked when ontology model has changed." //$NON-NLS-1$ //$NON-NLS-2$
		   });		
		addAnnotation
		  ((iOntModelChangeListenerEClass.getEOperations().get(0)).getEParameters().get(0), 
		   source, 
		   new String[] {
			 "documentation", "the new ontology model." //$NON-NLS-1$ //$NON-NLS-2$
		   });
	}

} //PlatformkitPackageImpl
