/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package be.ac.vub.platformkit.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;
import org.eclipse.emf.ecore.util.EObjectContainmentWithInverseEList;
import org.eclipse.emf.ecore.util.InternalEList;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.PlatformkitFactory;
import be.ac.vub.platformkit.PlatformkitPackage;
import be.ac.vub.platformkit.kb.Ontologies;
import be.ac.vub.platformkit.util.DefaultPathResolver;
import be.ac.vub.platformkit.util.PathResolver;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.shared.NotFoundException;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Constraint Space</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link be.ac.vub.platformkit.impl.ConstraintSpaceImpl#getOntology <em>Ontology</em>}</li>
 *   <li>{@link be.ac.vub.platformkit.impl.ConstraintSpaceImpl#getConstraintSet <em>Constraint Set</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ConstraintSpaceImpl extends EObjectImpl implements ConstraintSpace {
	
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final String copyright = "(C) 2007, Dennis Wagelaar, Vrije Universiteit Brussel";

	private class CacheAdapter extends AdapterImpl {

		@Override
		public void notifyChanged(Notification msg) {
			super.notifyChanged(msg);
			if (msg.getFeature().equals(PlatformkitPackage.eINSTANCE.getConstraintSpace_ConstraintSet())) {
				resetCache();
			} else if (msg.getFeature().equals(PlatformkitPackage.eINSTANCE.getConstraintSpace_Ontology())) {
				setKnowledgeBase(null);
			}
		}
		
	}
	
    protected static Logger logger = Logger.getLogger(Ontologies.LOGGER);
    
    private Ontologies knowledgeBase = null;
    private PathResolver pathResolver = new DefaultPathResolver();
    private ConstraintSet intersectionSet = null;

	/**
	 * The cached value of the '{@link #getOntology() <em>Ontology</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOntology()
	 * @generated
	 * @ordered
	 */
	protected EList ontology = null;

	/**
	 * The cached value of the '{@link #getConstraintSet() <em>Constraint Set</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConstraintSet()
	 * @generated
	 * @ordered
	 */
	protected EList constraintSet = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 */
	protected ConstraintSpaceImpl() {
		super();
		this.eAdapters().add(new CacheAdapter());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return PlatformkitPackage.Literals.CONSTRAINT_SPACE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getOntology() {
		if (ontology == null) {
			ontology = new EDataTypeUniqueEList(String.class, this, PlatformkitPackage.CONSTRAINT_SPACE__ONTOLOGY);
		}
		return ontology;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getConstraintSet() {
		if (constraintSet == null) {
			constraintSet = new EObjectContainmentWithInverseEList(ConstraintSet.class, this, PlatformkitPackage.CONSTRAINT_SPACE__CONSTRAINT_SET, PlatformkitPackage.CONSTRAINT_SET__SPACE);
		}
		return constraintSet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 */
	public ConstraintSet getIntersectionSet() {
        if (intersectionSet == null) {
            intersectionSet = createIntersectionSet();
            intersectionSet.setTransientSpace(this);
        }
        return intersectionSet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 */
	public EList getLeastSpecific(boolean validate) {
        logger.info("Calculating least-specific constraint sets");
        EList optimalClasses = getIntersectionSet().getLeastSpecific();
        EList optimalLists = new BasicEList();
        Map im = createIntersectionMap(validate);
        for (Iterator ocs = optimalClasses.iterator(); ocs.hasNext();) {
            ConstraintSet set = (ConstraintSet) im.get(ocs.next());
            if (set != null) {
                optimalLists.add(set);
            }
        }
        return optimalLists;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 */
	public EList getMostSpecific(boolean validate) {
        logger.info("Calculating most-specific constraint sets");
        EList optimalClasses = getIntersectionSet().getMostSpecific();
        EList optimalLists = new BasicEList();
        Map im = createIntersectionMap(validate);
        for (Iterator ocs = optimalClasses.iterator(); ocs.hasNext();) {
            ConstraintSet set = (ConstraintSet) im.get(ocs.next());
            if (set != null) {
                optimalLists.add(set);
            }
        }
        return optimalLists;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 */
	public EList getValid() {
        logger.info("Calculating valid constraint sets");
        EList optimalClasses = getIntersectionSet().getConstraint();
        EList optimalLists = new BasicEList();
        Map im = createIntersectionMap(true);
        for (Iterator ocs = optimalClasses.iterator(); ocs.hasNext();) {
            ConstraintSet set = (ConstraintSet) im.get(ocs.next());
            if (set != null) {
                optimalLists.add(set);
            }
        }
        return optimalLists;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 */
	public EList getInvalid() {
        logger.info("Calculating invalid constraint sets");
        EList optimalClasses = getIntersectionSet().getConstraint();
        EList optimalLists = new BasicEList();
        Map im = createIntersectionMap(false);
        for (Iterator ocs = optimalClasses.iterator(); ocs.hasNext();) {
            ConstraintSet set = (ConstraintSet) im.get(ocs.next());
            if (!set.isValid()) {
                optimalLists.add(set);
            }
        }
        return optimalLists;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case PlatformkitPackage.CONSTRAINT_SPACE__CONSTRAINT_SET:
				return ((InternalEList)getConstraintSet()).basicAdd(otherEnd, msgs);
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
			case PlatformkitPackage.CONSTRAINT_SPACE__CONSTRAINT_SET:
				return ((InternalEList)getConstraintSet()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case PlatformkitPackage.CONSTRAINT_SPACE__ONTOLOGY:
				return getOntology();
			case PlatformkitPackage.CONSTRAINT_SPACE__CONSTRAINT_SET:
				return getConstraintSet();
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
			case PlatformkitPackage.CONSTRAINT_SPACE__ONTOLOGY:
				getOntology().clear();
				getOntology().addAll((Collection)newValue);
				return;
			case PlatformkitPackage.CONSTRAINT_SPACE__CONSTRAINT_SET:
				getConstraintSet().clear();
				getConstraintSet().addAll((Collection)newValue);
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
			case PlatformkitPackage.CONSTRAINT_SPACE__ONTOLOGY:
				getOntology().clear();
				return;
			case PlatformkitPackage.CONSTRAINT_SPACE__CONSTRAINT_SET:
				getConstraintSet().clear();
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
			case PlatformkitPackage.CONSTRAINT_SPACE__ONTOLOGY:
				return ontology != null && !ontology.isEmpty();
			case PlatformkitPackage.CONSTRAINT_SPACE__CONSTRAINT_SET:
				return constraintSet != null && !constraintSet.isEmpty();
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
		result.append(" (ontology: ");
		result.append(ontology);
		result.append(')');
		return result.toString();
	}

	public Ontologies getKnowledgeBase() {
		return knowledgeBase;
	}

	public void setKnowledgeBase(Ontologies knowledgeBase) {
		if (this.knowledgeBase != null) {
			removeAllOntologyChangeListeners(this.knowledgeBase);
		}
		this.knowledgeBase = knowledgeBase;
		if (knowledgeBase != null) {
			addAllOntologyChangeListeners(knowledgeBase);
		}
	}
	
	public OntModel getOntModel() {
		if (knowledgeBase != null) {
			return knowledgeBase.getOntModel();
		} else {
			return null;
		}
	}

	/**
	 * Adds all constraints as listeners to kb.
	 * @param kb the knowledge base (may not be null).
	 */
	private void addAllOntologyChangeListeners(Ontologies kb) {
		Assert.assertNotNull(kb);
		for (Iterator it = getConstraintSet().iterator(); it.hasNext();) {
			((ConstraintSet) it.next()).addAllOntologyChangeListeners(kb);
		}
	}

	/**
	 * Removes all constraints as listeners from kb and resets cache.
	 * @param kb the knowledge base (may not be null).
	 */
	private void removeAllOntologyChangeListeners(Ontologies kb) {
		Assert.assertNotNull(kb);
		for (Iterator it = getConstraintSet().iterator(); it.hasNext();) {
			((ConstraintSet) it.next()).removeAllOntologyChangeListeners(kb);
		}
		resetCache();
	}

	public void setPathResolver(PathResolver pathResolver) {
		Assert.assertNotNull(pathResolver);
		this.pathResolver = pathResolver;
	}
	
    public void init(InputStream preClassifiedOntology)
    throws IOException, NotFoundException {
    	Assert.assertNotNull(getKnowledgeBase());
        if (preClassifiedOntology != null) {
            getKnowledgeBase().loadOntology(preClassifiedOntology);
        } else {
            EList onts = getOntology();
            for (int i = 0; i < onts.size(); i++) {
                InputStream in = pathResolver.getContents((String) onts.get(i));
                getKnowledgeBase().loadOntology(in);
            }
        }
    }

    /**
     * @return A Map of intersection constraints mapped to their constraint sets.
     * @param validate If true, validate each constraint set before including.
     */
    private Map createIntersectionMap(boolean validate) {
        Map im = new HashMap();
        for (Iterator css = getConstraintSet().iterator(); css.hasNext();) {
            ConstraintSet cs = (ConstraintSet) css.next();
            if (!validate || cs.isValid()) {
                im.put(cs.getIntersection(), cs);
            } else {
                logger.info(cs.getName() + " is invalid - removed");
            }
        }
        return im;
    }
    
    /**
     * Creates a constraint set consisting of all the intersection
     * constraints of all the constraints.
     * @return the intersection constraint set.
     */
    private ConstraintSet createIntersectionSet() {
    	ConstraintSet ics = PlatformkitFactory.eINSTANCE.createConstraintSet();
        ics.setName("_intersectionSet_");
        for (Iterator it = getConstraintSet().iterator(); it.hasNext();) {
            ConstraintSet cs = (ConstraintSet) it.next();
            ics.getConstraint().add(cs.getIntersection());
        }
        return ics;
    }
    
    /**
     * Resets cached intersection set.
     */
    protected void resetCache() {
        if (intersectionSet != null) {
            intersectionSet = null;
            logger.fine("intersection set reset for constraint space");
        }
    }
	
} //ConstraintSpaceImpl