/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package be.ac.vub.platformkit.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentWithInverseEList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.InternalEList;

import be.ac.vub.platformkit.Constraint;
import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.PlatformkitFactory;
import be.ac.vub.platformkit.PlatformkitPackage;
import be.ac.vub.platformkit.kb.Ontologies;
import be.ac.vub.platformkit.util.HierarchyComparator;
import be.ac.vub.platformkit.util.TreeSorter;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.RDFList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Constraint Set</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link be.ac.vub.platformkit.impl.ConstraintSetImpl#getSpace <em>Space</em>}</li>
 *   <li>{@link be.ac.vub.platformkit.impl.ConstraintSetImpl#getName <em>Name</em>}</li>
 *   <li>{@link be.ac.vub.platformkit.impl.ConstraintSetImpl#getConstraint <em>Constraint</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ConstraintSetImpl extends EObjectImpl implements ConstraintSet {
	
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final String copyright = "(C) 2007, Dennis Wagelaar, Vrije Universiteit Brussel";

	/**
	 * Wrapper iterator that returns the OntClass objects attached to Constraint object.
	 * Requires an inner Iterator over Constraint objects.
	 * @author dennis
	 *
	 */
	private class ConstraintIterator implements Iterator {
		private Iterator inner;
		
		/**
		 * Creates a new ConstraintIterator.
		 * @param inner An Iterator over Constraint objects.
		 */
		public ConstraintIterator(Iterator inner) {
			Assert.assertNotNull(inner);
			this.inner = inner;
		}

		public boolean hasNext() {
			return inner.hasNext();
		}

		public Object next() {
			return ((Constraint) inner.next()).getOntClass();
		}

		public void remove() {
			inner.remove();
		}
		
	}
	
	private class CacheAdapter extends AdapterImpl {

		@Override
		public void notifyChanged(Notification msg) {
			super.notifyChanged(msg);
			if ((msg.getFeature().equals(PlatformkitPackage.eINSTANCE.getConstraintSet_Constraint())) ||
			    (msg.getFeature().equals(PlatformkitPackage.eINSTANCE.getConstraintSet_Name()))) {
				resetCache();
			}
		}
		
	}
	
    protected static Logger logger = Logger.getLogger(Ontologies.LOGGER);

    private EList mostSpecific = null;
    private EList leastSpecific = null;
    private Constraint intersection = null;
    private ConstraintSpace transientSpace = null;

    private static TreeSorter msfSorter = new TreeSorter( 
            new HierarchyComparator(HierarchyComparator.MOST_SPECIFIC_FIRST)); 
    private static TreeSorter lsfSorter = new TreeSorter( 
            new HierarchyComparator(HierarchyComparator.LEAST_SPECIFIC_FIRST)); 

	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * The cached value of the '{@link #getConstraint() <em>Constraint</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConstraint()
	 * @generated
	 * @ordered
	 */
	protected EList constraint = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 */
	protected ConstraintSetImpl() {
		super();
		this.eAdapters().add(new CacheAdapter());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return PlatformkitPackage.Literals.CONSTRAINT_SET;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ConstraintSpace getSpace() {
		if (eContainerFeatureID != PlatformkitPackage.CONSTRAINT_SET__SPACE) return null;
		return (ConstraintSpace)eContainer();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetSpace(ConstraintSpace newSpace, NotificationChain msgs) {
		msgs = eBasicSetContainer((InternalEObject)newSpace, PlatformkitPackage.CONSTRAINT_SET__SPACE, msgs);
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSpace(ConstraintSpace newSpace) {
		if (newSpace != eInternalContainer() || (eContainerFeatureID != PlatformkitPackage.CONSTRAINT_SET__SPACE && newSpace != null)) {
			if (EcoreUtil.isAncestor(this, newSpace))
				throw new IllegalArgumentException("Recursive containment not allowed for " + toString());
			NotificationChain msgs = null;
			if (eInternalContainer() != null)
				msgs = eBasicRemoveFromContainer(msgs);
			if (newSpace != null)
				msgs = ((InternalEObject)newSpace).eInverseAdd(this, PlatformkitPackage.CONSTRAINT_SPACE__CONSTRAINT_SET, ConstraintSpace.class, msgs);
			msgs = basicSetSpace(newSpace, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PlatformkitPackage.CONSTRAINT_SET__SPACE, newSpace, newSpace));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PlatformkitPackage.CONSTRAINT_SET__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getConstraint() {
		if (constraint == null) {
			constraint = new EObjectContainmentWithInverseEList(Constraint.class, this, PlatformkitPackage.CONSTRAINT_SET__CONSTRAINT, PlatformkitPackage.CONSTRAINT__SET);
		}
		return constraint;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 */
	public boolean isValid() {
        for (Iterator cs = getConstraint().iterator(); cs.hasNext();) {
            if (!((Constraint) cs.next()).isValid()) {
                return false;
            }
        }
        return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 */
	public EList getMostSpecific() {
        if (mostSpecific == null) {
            mostSpecific = createAllMostSpecific();
        }
        return mostSpecific;
	}

    /**
     * @return All constraints in this set in order, most-specific first.
     * Requires a reasoner.
     */
    private EList createAllMostSpecific() {
        logger.info("Calculating most-specific constraint order for " + getName());
        EList mostSpecific = new BasicEList();
        mostSpecific.addAll(getConstraint());
        msfSorter.sort(mostSpecific);
        return mostSpecific;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 */
	public EList getLeastSpecific() {
        if (leastSpecific == null) {
            leastSpecific = createAllLeastSpecific();
        }
        return leastSpecific;
	}

    /**
     * @return All constraints in this set in order, least-specific first.
     * Requires a reasoner.
     */
    private EList createAllLeastSpecific() {
        logger.info("Calculating least-specific constraint order for " + getName());
        EList leastSpecific = new BasicEList();
        leastSpecific.addAll(getConstraint());
        lsfSorter.sort(leastSpecific);
        return leastSpecific;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 */
	public Constraint getIntersection() {
        if (intersection == null) {
            intersection = createIntersection();
        }
        return intersection;
	}

    /**
     * Creates the intersection constraint of all contained constraints.
     * @return the intersection constraint.
     */
    private Constraint createIntersection() {
    	String ontClassURI = Ontologies.LOCAL_INF_NS + "#" + getName() + "Intersection";
    	OntModel ontology = getOntModel();
    	Assert.assertNotNull(ontology);
        logger.info("creating/retrieving intersection class for " + getName());
        //Jena is not thread-safe when communicating to the DIG reasoner,
        //so lock all actions that trigger DIG activity.
        synchronized (Ontologies.class) {
            //attempt to retrieve existing intersection class
            if (ontology.getOntClass(ontClassURI) == null) {
                logger.fine("creating intersection class for " + getName());
                RDFList constraints = ontology.createList(
                        new ConstraintIterator(getConstraint().iterator()));
                ontology.createIntersectionClass(ontClassURI, constraints);
            }
        }
        Constraint intersection = PlatformkitFactory.eINSTANCE.createConstraint();
        intersection.setOntClassURI(ontClassURI);
        intersection.ontModelChanged(ontology);
        return intersection;
    }

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case PlatformkitPackage.CONSTRAINT_SET__SPACE:
				if (eInternalContainer() != null)
					msgs = eBasicRemoveFromContainer(msgs);
				return basicSetSpace((ConstraintSpace)otherEnd, msgs);
			case PlatformkitPackage.CONSTRAINT_SET__CONSTRAINT:
				return ((InternalEList)getConstraint()).basicAdd(otherEnd, msgs);
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
			case PlatformkitPackage.CONSTRAINT_SET__SPACE:
				return basicSetSpace(null, msgs);
			case PlatformkitPackage.CONSTRAINT_SET__CONSTRAINT:
				return ((InternalEList)getConstraint()).basicRemove(otherEnd, msgs);
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
			case PlatformkitPackage.CONSTRAINT_SET__SPACE:
				return eInternalContainer().eInverseRemove(this, PlatformkitPackage.CONSTRAINT_SPACE__CONSTRAINT_SET, ConstraintSpace.class, msgs);
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
			case PlatformkitPackage.CONSTRAINT_SET__SPACE:
				return getSpace();
			case PlatformkitPackage.CONSTRAINT_SET__NAME:
				return getName();
			case PlatformkitPackage.CONSTRAINT_SET__CONSTRAINT:
				return getConstraint();
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
			case PlatformkitPackage.CONSTRAINT_SET__SPACE:
				setSpace((ConstraintSpace)newValue);
				return;
			case PlatformkitPackage.CONSTRAINT_SET__NAME:
				setName((String)newValue);
				return;
			case PlatformkitPackage.CONSTRAINT_SET__CONSTRAINT:
				getConstraint().clear();
				getConstraint().addAll((Collection)newValue);
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
			case PlatformkitPackage.CONSTRAINT_SET__SPACE:
				setSpace((ConstraintSpace)null);
				return;
			case PlatformkitPackage.CONSTRAINT_SET__NAME:
				setName(NAME_EDEFAULT);
				return;
			case PlatformkitPackage.CONSTRAINT_SET__CONSTRAINT:
				getConstraint().clear();
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
			case PlatformkitPackage.CONSTRAINT_SET__SPACE:
				return getSpace() != null;
			case PlatformkitPackage.CONSTRAINT_SET__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case PlatformkitPackage.CONSTRAINT_SET__CONSTRAINT:
				return constraint != null && !constraint.isEmpty();
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
		result.append(" (name: ");
		result.append(name);
		result.append(')');
		return result.toString();
	}

	public void addAllOntologyChangeListeners(Ontologies kb) {
		Assert.assertNotNull(kb);
		for (Iterator it = getConstraint().iterator(); it.hasNext();) {
			kb.addOntModelChangeListener((Constraint) it.next());
		}
	}

	public void removeAllOntologyChangeListeners(Ontologies kb) {
		Assert.assertNotNull(kb);
		for (Iterator it = getConstraint().iterator(); it.hasNext();) {
			Constraint constraint = (Constraint) it.next();
			kb.removeOntModelChangeListener(constraint);
			constraint.ontModelChanged(null);
		}
		resetCache();
	}

	public ConstraintSpace getTransientSpace() {
		return transientSpace;
	}

	public void setTransientSpace(ConstraintSpace transientSpace) {
		this.transientSpace = transientSpace;
	}
	
	public OntModel getOntModel() {
		ConstraintSpace space = getSpace();
		if (space == null) {
			space = getTransientSpace();
		}
		if (space != null) {
			return space.getOntModel();
		} else {
			return null;
		}
	}
	
    /**
     * Resets cached volatile constraints.
     */
    protected void resetCache() {
        if (intersection != null) {
            intersection = null;
            logger.fine("intersection class reset for " + getName());
        }
        if (mostSpecific != null) {
            mostSpecific = null;
            logger.fine("most-specific cache reset for " + getName());
        }
        if (leastSpecific != null) {
            leastSpecific = null;
            logger.fine("least-specific cache reset for " + getName());
        }
    }

} //ConstraintSetImpl