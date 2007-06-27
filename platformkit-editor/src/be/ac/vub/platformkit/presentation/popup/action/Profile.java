package be.ac.vub.platformkit.presentation.popup.action;

import java.util.Iterator;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.provider.ItemProviderAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.editor.preferences.PreferenceConstants;
import be.ac.vub.platformkit.kb.Ontologies;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.util.IEObjectValidator;
import be.ac.vub.platformkit.presentation.util.PlatformEValidator;
import be.ac.vub.platformkit.presentation.util.PlatformKitActionUtil;
import be.ac.vub.platformkit.presentation.util.PlatformKitEObjectValidator;
import be.ac.vub.platformkit.presentation.util.PlatformKitException;
import be.ac.vub.platformkit.presentation.util.provider.PlatformKitItemProviderAdapter;

/**
 * Profiles the options in an EMF editor
 * against a concrete context specification.
 * @author dennis
 *
 */
public class Profile extends PlatformKitAction {
	
	/**
	 * Registry of resource validators.
	 * @author dennis
	 */
	public static class Registry extends WeakHashMap {
		public static Registry INSTANCE = new Registry();
		
		private Registry() {
			super();
		}
		
		/**
		 * @param resource
		 * @return The registered validator for resource, if any, null otherwise.
		 */
		public IEObjectValidator getValidator(Resource resource) {
			Object object = get(resource);
			if (object instanceof IEObjectValidator) {
				return (IEObjectValidator) object;
			} else {
				return null;
			}
		}
	}
	
	/**
	 * Constructor for Action1.
	 */
	public Profile() {
		super();
        setFilter(new ViewerFilter() {
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof IResource) {
                    IResource resource = (IResource) element;
                    if (resource.getType() == IResource.FILE) {
                        return resource.getFileExtension().toLowerCase().equals("owl");
                    }
                    return true;
                }
                return false;
            }
        });
	}

    /**
     * Invoked when the action is executed.
     * @param monitor
     * @throws Exception
     */
    protected final void runAction(IProgressMonitor monitor)
    throws Exception {
        monitor.beginTask("Profiling EMF editor against context", 8);
        monitor.subTask("Searching for constraint space...");
        EObject object = (EObject) ((IStructuredSelection) selection).getFirstElement();
        Assert.isNotNull(object);
        logger.info(object.eAdapters().toString());
        Resource res = object.eResource();
        Assert.isNotNull(res);
        if (res.getContents().size() == 0) {
        	//empty model?! this is wrong in EMF models
        	//not our task to cause more exceptions
        	logger.severe("Resource " + res + " is empty!");
        	return;
        }
        EObject root = (EObject) res.getContents().get(0);
        Assert.isNotNull(root);
        Resource rootRes = root.eClass().eResource();
        Assert.isNotNull(rootRes);
        URI resURI = rootRes.getURI();
        URI platformkitURI = resURI.trimFileExtension().appendFileExtension("platformkit");
        logger.info("Platformkit URI = " + platformkitURI.toString());
        ConstraintSpace space = null;
       	space = PlatformKitActionUtil.getCachedConstraintSpace(platformkitURI);
        worked(monitor);
        if (space == null) {
            monitor.subTask("Loading Platformkit model...");
            Resource platformkit = resourceSet.getResource(platformkitURI, true);
            worked(monitor);
            monitor.subTask("Loading source ontologies...");
            if (platformkit.getContents().size() == 0) {
            	//empty model?! this is wrong in EMF models
            	//not our task to cause more exceptions
            	logger.severe("Resource " + platformkit + " is empty!");
            	return;
            }
            space = (ConstraintSpace) platformkit.getContents().get(0);
            Ontologies ont = new Ontologies();
            space.setKnowledgeBase(ont);
            if (!space.init(true)) {
                throw new PlatformKitException(
                        "Ontologies not pre-classified - Choose 'Classify Taxonomy' first");
            }
            PlatformKitActionUtil.setCachedConstraintSpace(platformkitURI, space);
            worked(monitor);
            monitor.subTask("Attaching DL reasoner...");
    		IPreferenceStore store = PlatformkitEditorPlugin.getPlugin()
    				.getPreferenceStore();
    		String reasoner = store.getString(PreferenceConstants.P_REASONER);
    		if (PreferenceConstants.P_DIG.equals(reasoner)) {
    			String url = store.getString(PreferenceConstants.P_DIG_URL);
    			ont.setReasonerUrl(url);
    			ont.attachDIGReasoner();
    		} else {
    	        ont.attachPelletReasoner();
    		}
            worked(monitor);
        } else {
            monitor.subTask("Using cached constraint space");
            worked(monitor);
            worked(monitor);
            worked(monitor);
        }
        monitor.subTask("Retrieving platform specification...");
        if (getPlatform(space)) {
            worked(monitor);
            monitor.subTask("Retrieving intersection set...");
            ConstraintSet is = space.getIntersectionSet();
            is.getIntersection();
            worked(monitor);
            monitor.subTask("Determining (in-)valid constraint sets...");
            IEObjectValidator validator = new PlatformKitEObjectValidator(space);
            Registry.INSTANCE.put(res, validator);
            worked(monitor);
            monitor.subTask("Profiling editor...");
            updateAllObjects(res);
            worked(monitor);
        } else {
        	worked(monitor);
        	worked(monitor);
            monitor.subTask("Determining (in-)valid constraint sets...");
            Registry.INSTANCE.remove(res);
            worked(monitor);
            monitor.subTask("Profiling editor...");
            updateAllObjects(res);
            worked(monitor);
        }
    }
    
    /**
     * Updates all EObjects in res.
     * @param res
     */
    private void updateAllObjects(Resource res) {
    	IEObjectValidator validator = (IEObjectValidator) Registry.INSTANCE.get(res);
    	for (Iterator it = res.getAllContents(); it.hasNext();) {
    		Object object = it.next();
    		if (object instanceof EObject) {
    			updateObject((EObject) object, validator);
    			registerEValidator((EObject) object);
    		}
    	}
    }

    /**
     * Updates the given EObject with a wrapper and the given validator.
     * @param object
     * @param validator
     */
    protected void updateObject(EObject object, IEObjectValidator validator) {
    	Assert.isNotNull(object);
		for (int i = 0; i < object.eAdapters().size(); i++) {
			Object adapter = object.eAdapters().get(i);
			if ((adapter instanceof ItemProviderAdapter) && (editingDomain instanceof AdapterFactoryEditingDomain)) {
				PlatformKitItemProviderAdapter wrapper =
					new PlatformKitItemProviderAdapter(
							(ItemProviderAdapter) adapter, 
							((AdapterFactoryEditingDomain) editingDomain).getAdapterFactory());
				logger.info("Created wrapper adapter for " + object.toString());
				object.eAdapters().set(i, wrapper);
				adapter = wrapper;
			}
   			if (adapter instanceof PlatformKitItemProviderAdapter) {
   				PlatformKitItemProviderAdapter pkAdapter = (PlatformKitItemProviderAdapter) adapter;
   				if ((pkAdapter.getValidator() != validator)) {
       				pkAdapter.setValidator(validator);
       				logger.info("Attached new validator to wrapper " + pkAdapter.toString());
   				}
   			}
		}
		if (validator == null) {
			return;
		}
    }
    
    /**
     * Registers the CDDEValidator for object's meta-model.
     * @param object
     */
    protected void registerEValidator(EObject object) {
    	Assert.isNotNull(object);
    	EPackage pack = object.eClass().getEPackage();
    	Assert.isNotNull(pack);
    	EValidator orig = EValidator.Registry.INSTANCE.getEValidator(pack);
		if (!(orig instanceof PlatformEValidator)) {
    		EValidator eValidator = new PlatformEValidator(orig);
    		EValidator.Registry.INSTANCE.put(pack, eValidator);
    		logger.info("Registered new PlatformEValidator for " + pack.getNsURI());
    	}
    }
}
