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
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Internal;
import org.eclipse.emf.ecore.resource.impl.URIConverterImpl;
import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;
import org.eclipse.emf.ecore.util.EObjectContainmentWithInverseEList;
import org.eclipse.emf.ecore.util.InternalEList;

import be.ac.vub.platformkit.Constraint;
import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.PlatformkitFactory;
import be.ac.vub.platformkit.PlatformkitPackage;
import be.ac.vub.platformkit.kb.IOntModel;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.util.EMFURIPathResolver;
import be.ac.vub.platformkit.util.PathResolver;

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
	public static final String copyright = "(C) 2007-2008, Dennis Wagelaar, Vrije Universiteit Brussel";

	private class CacheAdapter extends AdapterImpl {

		public void notifyChanged(Notification msg) {
			super.notifyChanged(msg);
			if (PlatformkitPackage.eINSTANCE.getConstraintSpace_ConstraintSet().equals(msg.getFeature())) {
				resetCache();
			} else if (PlatformkitPackage.eINSTANCE.getConstraintSpace_Ontology().equals(msg.getFeature())) {
				setKnowledgeBase(null);
			}
		}
		
	}
	
    protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);
    private static URIConverterImpl converter = new URIConverterImpl();
    
    private IOntologies knowledgeBase = null;
    private PathResolver pathResolver = null;
    private ConstraintSet intersectionSet = null;

	/**
	 * The cached value of the '{@link #getOntology() <em>Ontology</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOntology()
	 * @generated
	 * @ordered
	 */
	protected EList<String> ontology;

	/**
	 * The cached value of the '{@link #getConstraintSet() <em>Constraint Set</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConstraintSet()
	 * @generated
	 * @ordered
	 */
	protected EList<ConstraintSet> constraintSet;

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
	@Override
	protected EClass eStaticClass() {
		return PlatformkitPackage.Literals.CONSTRAINT_SPACE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<String> getOntology() {
		if (ontology == null) {
			ontology = new EDataTypeUniqueEList<String>(String.class, this, PlatformkitPackage.CONSTRAINT_SPACE__ONTOLOGY);
		}
		return ontology;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<ConstraintSet> getConstraintSet() {
		if (constraintSet == null) {
			constraintSet = new EObjectContainmentWithInverseEList<ConstraintSet>(ConstraintSet.class, this, PlatformkitPackage.CONSTRAINT_SPACE__CONSTRAINT_SET, PlatformkitPackage.CONSTRAINT_SET__SPACE);
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
	public EList<ConstraintSet> getLeastSpecific(boolean validate) {
        logger.info("Calculating least-specific constraint sets");
        EList<Constraint> optimalClasses = getIntersectionSet().getLeastSpecific();
        EList<ConstraintSet> optimalLists = new BasicEList<ConstraintSet>();
        Map<Constraint, ConstraintSet> im = createIntersectionMap(validate);
        for (Iterator<Constraint> ocs = optimalClasses.iterator(); ocs.hasNext();) {
            ConstraintSet set = im.get(ocs.next());
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
	public EList<ConstraintSet> getMostSpecific(boolean validate) {
        logger.info("Calculating most-specific constraint sets");
        EList<Constraint> optimalClasses = getIntersectionSet().getMostSpecific();
        EList<ConstraintSet> optimalLists = new BasicEList<ConstraintSet>();
        Map<Constraint, ConstraintSet> im = createIntersectionMap(validate);
        for (Iterator<Constraint> ocs = optimalClasses.iterator(); ocs.hasNext();) {
            ConstraintSet set = im.get(ocs.next());
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
	public EList<ConstraintSet> getValid() {
        logger.info("Calculating valid constraint sets");
        EList<Constraint> optimalClasses = getIntersectionSet().getConstraint();
        EList<ConstraintSet> optimalLists = new BasicEList<ConstraintSet>();
        Map<Constraint, ConstraintSet> im = createIntersectionMap(true);
        for (Iterator<Constraint> ocs = optimalClasses.iterator(); ocs.hasNext();) {
            ConstraintSet set = im.get(ocs.next());
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
	public EList<ConstraintSet> getInvalid() {
        logger.info("Calculating invalid constraint sets");
        EList<Constraint> optimalClasses = getIntersectionSet().getConstraint();
        EList<ConstraintSet> optimalLists = new BasicEList<ConstraintSet>();
        Map<Constraint, ConstraintSet> im = createIntersectionMap(false);
        for (Iterator<Constraint> ocs = optimalClasses.iterator(); ocs.hasNext();) {
            ConstraintSet set = im.get(ocs.next());
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
	@SuppressWarnings("unchecked")
		@Override
	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case PlatformkitPackage.CONSTRAINT_SPACE__CONSTRAINT_SET:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getConstraintSet()).basicAdd(otherEnd, msgs);
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
			case PlatformkitPackage.CONSTRAINT_SPACE__CONSTRAINT_SET:
				return ((InternalEList<?>)getConstraintSet()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
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
	@SuppressWarnings("unchecked")
		@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case PlatformkitPackage.CONSTRAINT_SPACE__ONTOLOGY:
				getOntology().clear();
				getOntology().addAll((Collection<? extends String>)newValue);
				return;
			case PlatformkitPackage.CONSTRAINT_SPACE__CONSTRAINT_SET:
				getConstraintSet().clear();
				getConstraintSet().addAll((Collection<? extends ConstraintSet>)newValue);
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
	@Override
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
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (ontology: ");
		result.append(ontology);
		result.append(')');
		return result.toString();
	}

	public IOntologies getKnowledgeBase() {
		return knowledgeBase;
	}

	public void setKnowledgeBase(IOntologies knowledgeBase) {
		if (this.knowledgeBase != null) {
			removeAllOntologyChangeListeners(this.knowledgeBase);
		}
		this.knowledgeBase = knowledgeBase;
		if (knowledgeBase != null) {
			addAllOntologyChangeListeners(knowledgeBase);
		}
	}
	
	public IOntModel getOntModel() {
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
	private void addAllOntologyChangeListeners(IOntologies kb) {
		Assert.assertNotNull(kb);
		for (Iterator<ConstraintSet> it = getConstraintSet().iterator(); it.hasNext();) {
			it.next().addAllOntologyChangeListeners(kb);
		}
	}

	/**
	 * Removes all constraints as listeners from kb and resets cache.
	 * @param kb the knowledge base (may not be null).
	 */
	private void removeAllOntologyChangeListeners(IOntologies kb) {
		Assert.assertNotNull(kb);
		for (Iterator<ConstraintSet> it = getConstraintSet().iterator(); it.hasNext();) {
			it.next().removeAllOntologyChangeListeners(kb);
		}
		resetCache();
	}

	public void setPathResolver(PathResolver pathResolver) {
		this.pathResolver = pathResolver;
	}
	
    public PathResolver getPathResolver() {
    	if (pathResolver == null) {
    		pathResolver = createPathResolver();
    	}
		return pathResolver;
	}

    /**
     * @return A new PathResolver.
     * Requires {@link Resource#getURI()} of {@link #eResource()} to be set.
     */
    protected PathResolver createPathResolver() {
    	Assert.assertNotNull(eResource());
    	Assert.assertNotNull(eResource().getURI());
    	return new EMFURIPathResolver(eResource().getURI());
    }

	public boolean init(boolean searchPreClassified)
    throws IOException {
    	Assert.assertNotNull(getKnowledgeBase());
    	boolean preClassified = false;
        if (searchPreClassified) {
            try {
            	getKnowledgeBase().loadOntology(getPreClassifiedOntology());
            	preClassified = true;
            } catch (IOException e) {
            	//abort
            }
        } else {
            EList<String> onts = getOntology();
            for (int i = 0; i < onts.size(); i++) {
                InputStream in = getPathResolver().getContents(onts.get(i));
                getKnowledgeBase().loadOntology(in);
            }
        }
        return preClassified;
    }

    /**
     * @return A Map of intersection constraints mapped to their constraint sets.
     * @param validate If true, validate each constraint set before including.
     */
    private Map<Constraint, ConstraintSet> createIntersectionMap(boolean validate) {
        Map<Constraint, ConstraintSet> im = new HashMap<Constraint, ConstraintSet>();
        for (Iterator<ConstraintSet> css = getConstraintSet().iterator(); css.hasNext();) {
            ConstraintSet cs = css.next();
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
        for (Iterator<ConstraintSet> it = getConstraintSet().iterator(); it.hasNext();) {
            ConstraintSet cs = it.next();
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
    
	/**
	 * @return The contents of the inferred ontology (.inferred.owl).
	 * @throws IOException if the inferred ontology cannot be found.
	 */
	private InputStream getPreClassifiedOntology() throws IOException {
		URI uri = eResource().getURI().trimFileExtension().appendFileExtension("inferred.owl");
        logger.info("Searching pre-classified ontology at: " + uri.toString());
		return converter.createInputStream(uri);
	}

	public NotificationChain eSetResource(Internal resource, NotificationChain notifications) {
		setPathResolver(null);
		return super.eSetResource(resource, notifications);
	}
	
} //ConstraintSpaceImpl