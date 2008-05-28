/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package be.ac.vub.platformkit.util;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;
import org.eclipse.emf.ecore.EObject;

import be.ac.vub.platformkit.Constraint;
import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.IOntModelChangeListener;
import be.ac.vub.platformkit.PlatformkitPackage;
import be.ac.vub.platformkit.kb.IOntModel;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see be.ac.vub.platformkit.PlatformkitPackage
 * @generated
 */
public class PlatformkitAdapterFactory extends AdapterFactoryImpl {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final String copyright = "(C) 2007-2008, Dennis Wagelaar, Vrije Universiteit Brussel";

	/**
	 * The cached model package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static PlatformkitPackage modelPackage;

	/**
	 * Creates an instance of the adapter factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PlatformkitAdapterFactory() {
		if (modelPackage == null) {
			modelPackage = PlatformkitPackage.eINSTANCE;
		}
	}

	/**
	 * Returns whether this factory is applicable for the type of the object.
	 * <!-- begin-user-doc -->
	 * This implementation returns <code>true</code> if the object is either the model's package or is an instance object of the model.
	 * <!-- end-user-doc -->
	 * @return whether this factory is applicable for the type of the object.
	 * @generated
	 */
	@Override
	public boolean isFactoryForType(Object object) {
		if (object == modelPackage) {
			return true;
		}
		if (object instanceof EObject) {
			return ((EObject)object).eClass().getEPackage() == modelPackage;
		}
		return false;
	}

	/**
	 * The switch the delegates to the <code>createXXX</code> methods.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected PlatformkitSwitch<Adapter> modelSwitch =
		new PlatformkitSwitch<Adapter>() {
			@Override
			public Adapter caseConstraintSpace(ConstraintSpace object) {
				return createConstraintSpaceAdapter();
			}
			@Override
			public Adapter caseConstraintSet(ConstraintSet object) {
				return createConstraintSetAdapter();
			}
			@Override
			public Adapter caseConstraint(Constraint object) {
				return createConstraintAdapter();
			}
			@Override
			public Adapter caseIOntModel(IOntModel object) {
				return createIOntModelAdapter();
			}
			@Override
			public Adapter caseIOntModelChangeListener(IOntModelChangeListener object) {
				return createIOntModelChangeListenerAdapter();
			}
			@Override
			public Adapter defaultCase(EObject object) {
				return createEObjectAdapter();
			}
		};

	/**
	 * Creates an adapter for the <code>target</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param target the object to adapt.
	 * @return the adapter for the <code>target</code>.
	 * @generated
	 */
	@Override
	public Adapter createAdapter(Notifier target) {
		return modelSwitch.doSwitch((EObject)target);
	}


	/**
	 * Creates a new adapter for an object of class '{@link be.ac.vub.platformkit.ConstraintSpace <em>Constraint Space</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see be.ac.vub.platformkit.ConstraintSpace
	 * @generated
	 */
	public Adapter createConstraintSpaceAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link be.ac.vub.platformkit.ConstraintSet <em>Constraint Set</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see be.ac.vub.platformkit.ConstraintSet
	 * @generated
	 */
	public Adapter createConstraintSetAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link be.ac.vub.platformkit.Constraint <em>Constraint</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see be.ac.vub.platformkit.Constraint
	 * @generated
	 */
	public Adapter createConstraintAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link be.ac.vub.platformkit.kb.IOntModel <em>IOnt Model</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see be.ac.vub.platformkit.kb.IOntModel
	 * @generated
	 */
	public Adapter createIOntModelAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link be.ac.vub.platformkit.IOntModelChangeListener <em>IOnt Model Change Listener</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see be.ac.vub.platformkit.IOntModelChangeListener
	 * @generated
	 */
	public Adapter createIOntModelChangeListenerAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for the default case.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @generated
	 */
	public Adapter createEObjectAdapter() {
		return null;
	}

} //PlatformkitAdapterFactory
