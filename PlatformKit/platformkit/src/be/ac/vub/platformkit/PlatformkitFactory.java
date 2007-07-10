/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package be.ac.vub.platformkit;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see be.ac.vub.platformkit.PlatformkitPackage
 * @generated
 */
public interface PlatformkitFactory extends EFactory {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String copyright = "(C) 2007, Dennis Wagelaar, Vrije Universiteit Brussel";

	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	PlatformkitFactory eINSTANCE = be.ac.vub.platformkit.impl.PlatformkitFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Constraint Space</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Constraint Space</em>'.
	 * @generated
	 */
	ConstraintSpace createConstraintSpace();

	/**
	 * Returns a new object of class '<em>Constraint Set</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Constraint Set</em>'.
	 * @generated
	 */
	ConstraintSet createConstraintSet();

	/**
	 * Returns a new object of class '<em>Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Constraint</em>'.
	 * @generated
	 */
	Constraint createConstraint();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	PlatformkitPackage getPlatformkitPackage();

} //PlatformkitFactory