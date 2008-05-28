/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package be.ac.vub.platformkit.impl;

import java.util.logging.Logger;

import junit.framework.Assert;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;

import be.ac.vub.platformkit.Constraint;
import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.PlatformkitPackage;
import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntModel;
import be.ac.vub.platformkit.kb.IOntologies;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Constraint</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link be.ac.vub.platformkit.impl.ConstraintImpl#getSet <em>Set</em>}</li>
 *   <li>{@link be.ac.vub.platformkit.impl.ConstraintImpl#getOntClassURI <em>Ont Class URI</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ConstraintImpl extends EObjectImpl implements Constraint {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final String copyright = "(C) 2007-2008, Dennis Wagelaar, Vrije Universiteit Brussel";
	
	private class CacheAdapter extends AdapterImpl {

		public void notifyChanged(Notification msg) {
			super.notifyChanged(msg);
			if (msg.getFeature().equals(PlatformkitPackage.eINSTANCE.getConstraint_OntClassURI())) {
				setOntClass(null);
			}
		}
		
	}

    protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);
	private IOntClass ontClass = null;
	
	/**
	 * The default value of the '{@link #getOntClassURI() <em>Ont Class URI</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOntClassURI()
	 * @generated
	 * @ordered
	 */
	protected static final String ONT_CLASS_URI_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getOntClassURI() <em>Ont Class URI</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOntClassURI()
	 * @generated
	 * @ordered
	 */
	protected String ontClassURI = ONT_CLASS_URI_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 */
	protected ConstraintImpl() {
		super();
		this.eAdapters().add(new CacheAdapter());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return PlatformkitPackage.Literals.CONSTRAINT;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ConstraintSet getSet() {
		if (eContainerFeatureID != PlatformkitPackage.CONSTRAINT__SET) return null;
		return (ConstraintSet)eContainer();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetSet(ConstraintSet newSet, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newSet, PlatformkitPackage.CONSTRAINT__SET, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSet(ConstraintSet newSet) {
		if (newSet != eInternalContainer() || (eContainerFeatureID != PlatformkitPackage.CONSTRAINT__SET && newSet != null)) {
			if (EcoreUtil.isAncestor(this, newSet))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newSet != null)
				msgs = ((InternalEObject)newSet).eInverseAdd(this, PlatformkitPackage.CONSTRAINT_SET__CONSTRAINT, ConstraintSet.class, msgs);
			msgs = basicSetSet(newSet, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PlatformkitPackage.CONSTRAINT__SET, newSet, newSet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getOntClassURI() {
		return ontClassURI;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setOntClassURI(String newOntClassURI) {
		String oldOntClassURI = ontClassURI;
		ontClassURI = newOntClassURI;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PlatformkitPackage.CONSTRAINT__ONT_CLASS_URI, oldOntClassURI, ontClassURI));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 */
	public boolean isValid() {
		Assert.assertNotNull(ontClass);
        boolean valid;
        //Jena is not thread-safe when communicating to the DIG reasoner,
        //so lock all actions that trigger DIG activity.
        synchronized (IOntologies.class) {
            valid = ontClass.hasInstances();
        }
        if (valid) {
            logger.fine(ontClass + " is valid");
            return true;
        } else {
            logger.info(ontClass + " is invalid");
            return false;
        }
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case PlatformkitPackage.CONSTRAINT__SET:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetSet((ConstraintSet)otherEnd, msgs);
		}
		return super.eInverseAdd(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case PlatformkitPackage.CONSTRAINT__SET:
				return basicSetSet(null, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eBasicRemoveFromContainerFeature(NotificationChain msgs) {
		switch (eContainerFeatureID) {
			case PlatformkitPackage.CONSTRAINT__SET:
				return eInternalContainer().eInverseRemove(this, PlatformkitPackage.CONSTRAINT_SET__CONSTRAINT, ConstraintSet.class, msgs);
		}
		return super.eBasicRemoveFromContainerFeature(msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case PlatformkitPackage.CONSTRAINT__SET:
				return getSet();
			case PlatformkitPackage.CONSTRAINT__ONT_CLASS_URI:
				return getOntClassURI();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case PlatformkitPackage.CONSTRAINT__SET:
				setSet((ConstraintSet)newValue);
				return;
			case PlatformkitPackage.CONSTRAINT__ONT_CLASS_URI:
				setOntClassURI((String)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case PlatformkitPackage.CONSTRAINT__SET:
				setSet((ConstraintSet)null);
				return;
			case PlatformkitPackage.CONSTRAINT__ONT_CLASS_URI:
				setOntClassURI(ONT_CLASS_URI_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case PlatformkitPackage.CONSTRAINT__SET:
				return getSet() != null;
			case PlatformkitPackage.CONSTRAINT__ONT_CLASS_URI:
				return ONT_CLASS_URI_EDEFAULT == null ? ontClassURI != null : !ONT_CLASS_URI_EDEFAULT.equals(ontClassURI);
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (ontClassURI: ");
		result.append(ontClassURI);
		result.append(')');
		return result.toString();
	}

	public void ontModelChanged(IOntModel ontModel) {
		if (ontClassURI != null) {
			if (ontModel == null) {
				setOntClass(null);
			} else {
				setOntClass(findOntClass(ontModel));
			}
            logger.info("OntModel changed; refreshed " + this);
		}
	}
	
	protected IOntClass findOntClass(IOntModel ontModel) {
		IOntClass ontClass = null;
		if (ontClassURI != null) {
			if (ontModel != null) {
	            //Jena is not thread-safe when communicating to the DIG reasoner,
	            //so lock all actions that trigger DIG activity.
	            synchronized (IOntologies.class) {
	            	ontClass = ontModel.getOntClass(ontClassURI);
	            }
			}
		}
		return ontClass;
	}

	public IOntClass getOntClass() {
		if (ontClass == null) {
			ontClass = findOntClass(getOntModel());
            logger.info("Refreshed " + this);
		}
		return ontClass;
	}

	public void setOntClass(IOntClass ontClass) {
		this.ontClass = ontClass;
	}
	
	public IOntModel getOntModel() {
		ConstraintSet set = getSet();
		if (set != null) {
			return set.getOntModel();
		} else {
			return null;
		}
	}

} //ConstraintImpl