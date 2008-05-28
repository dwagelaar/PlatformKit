/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package be.ac.vub.platformkit.impl;

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
	public static final String copyright = "(C) 2007-2008, Dennis Wagelaar, Vrije Universiteit Brussel";

	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static PlatformkitFactory init() {
		try {
			PlatformkitFactory thePlatformkitFactory = (PlatformkitFactory)EPackage.Registry.INSTANCE.getEFactory("http://ssel.vub.ac.be/platformkit"); 
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
				throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
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
