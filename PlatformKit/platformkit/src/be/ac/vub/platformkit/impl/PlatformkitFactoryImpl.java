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

import be.ac.vub.platformkit.*;
import be.ac.vub.platformkit.kb.util.OntException;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;

import be.ac.vub.platformkit.Constraint;
import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.PlatformkitFactory;
import be.ac.vub.platformkit.PlatformkitPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class PlatformkitFactoryImpl extends EFactoryImpl implements PlatformkitFactory {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final String copyright = "(C) 2005-2010, Dennis Wagelaar, Vrije Universiteit Brussel";

	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static PlatformkitFactory init() {
		try {
			PlatformkitFactory thePlatformkitFactory = (PlatformkitFactory)EPackage.Registry.INSTANCE.getEFactory("http://ssel.vub.ac.be/platformkit"); //$NON-NLS-1$ 
			if (thePlatformkitFactory != null) {
				return thePlatformkitFactory;
			}
		}
		catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new PlatformkitFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PlatformkitFactoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
			case PlatformkitPackage.CONSTRAINT_SPACE: return createConstraintSpace();
			case PlatformkitPackage.CONSTRAINT_SET: return createConstraintSet();
			case PlatformkitPackage.CONSTRAINT: return createConstraint();
			default:
				throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ConstraintSpace createConstraintSpace() {
		ConstraintSpaceImpl constraintSpace = new ConstraintSpaceImpl();
		return constraintSpace;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ConstraintSet createConstraintSet() {
		ConstraintSetImpl constraintSet = new ConstraintSetImpl();
		return constraintSet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Constraint createConstraint() {
		ConstraintImpl constraint = new ConstraintImpl();
		return constraint;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PlatformkitPackage getPlatformkitPackage() {
		return (PlatformkitPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static PlatformkitPackage getPackage() {
		return PlatformkitPackage.eINSTANCE;
	}

} //PlatformkitFactoryImpl
