package be.ac.vub.platformkit.presentation.popup.action;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.resource.Resource;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.presentation.util.PlatformKitActionUtil;


/**
 * Adds a product configuration, described in an annotated DSL,
 * to the CDD configuration file. Each context-constrained meta-class
 * in the DSL should have an EAnnotation with source set to the
 * ontology constraint class and reference set to another EAnnotation
 * with source set to 'ContextConstraint'.
 * @author dennis
 * @see EAnnotation
 */
public class AddProductConfig extends AddConstraintSets {

	/**
	 * Constructor for Action1.
	 */
	public AddProductConfig() {
		super("Product Configuration Model");
		//TODO filter doesn't work, since normal EMF plugins aren't here
//        setFilter(new ViewerFilter() {
//            private Map extensions = Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap();
//            public boolean select(Viewer viewer, Object parentElement, Object element) {
//                if (element instanceof IResource) {
//                    IResource resource = (IResource) element;
//                    if (resource.getType() == IResource.FILE) {
//                    	logger.info("filter using map " + extensions);
//                    	logger.info("filter found " + resource);
//                        return extensions.get(resource.getFileExtension()) != null;
//                    }
//                    return true;
//                }
//                return false;
//            }
//        });
	}

    protected void addOntologies(Resource source, EList<String> ontologies) {
        PlatformKitActionUtil.addOntologies(source, space.eResource().getURI(), ontologies);
    }
    
    protected void addConstraintSet(Resource source, EList<ConstraintSet> constraintSets) {
    	ConstraintSet set = PlatformKitActionUtil.createModelConstraintSet(source);
    	if (set != null) {
    		constraintSets.add(set);
    	}
    }
}
