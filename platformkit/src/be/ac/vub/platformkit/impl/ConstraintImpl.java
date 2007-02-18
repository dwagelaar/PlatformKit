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
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;

import be.ac.vub.platformkit.Constraint;
import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.PlatformkitPackage;
import be.ac.vub.platformkit.kb.Ontologies;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;

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
	public static final String copyright = "(C) 2007, Dennis Wagelaar, Vrije Universiteit Brussel";

    protected static Logger logger = Logger.getLogger(Ontologies.LOGGER);
	private OntClass ontClass = null;
	
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
	 * @generated
	 */
	protected ConstraintImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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
        synchronized (Ontologies.class) {
            valid = ontClass.listInstances().hasNext();
        }
        if (valid) {
            logger.fine(ontClass + " is valid");
            return true;
        } else {
            logger.fine(ontClass + " is invalid");
            return false;
        }
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (ontClassURI: ");
		result.append(ontClassURI);
		result.append(", ontClass: ");
		result.append(ontClass);
		result.append(')');
		return result.toString();
	}

	public void ontModelChanged(OntModel ontModel) {
		if (ontClassURI != null) {
			if (ontModel == null) {
				setOntClass(null);
			} else {
	            //Jena is not thread-safe when communicating to the DIG reasoner,
	            //so lock all actions that trigger DIG activity.
	            synchronized (Ontologies.class) {
	            	setOntClass(ontModel.getOntClass(ontClassURI));
	            }
			}
            logger.info("OntModel changed; refreshed " + this);
		}
	}

	public OntClass getOntClass() {
		return ontClass;
	}

	public void setOntClass(OntClass ontClass) {
		this.ontClass = ontClass;
	}

} //ConstraintImpl