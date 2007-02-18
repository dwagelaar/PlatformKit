package be.ac.vub.platformkit.presentation.popup.action;

import org.eclipse.core.resources.IResource;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.presentation.util.PlatformKitActionUtil;

/**
 * Adds the metaclass constraints of a product line, described in Ecore,
 * to the CDD configuration file. Each context-constrained meta-class
 * of the DSL should have an EAnnotation with source set to the
 * ontology constraint class and reference set to another EAnnotation
 * with source set to 'ContextConstraint'.
 * @author dennis
 * @see EAnnotation
 */
public class AddProductLine extends AddConstraintSets {

	/**
	 * Constructor for Action1.
	 */
	public AddProductLine() {
		super("Product Line Meta-model");
        setFilter(new ViewerFilter() {
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof IResource) {
                    IResource resource = (IResource) element;
                    if (resource.getType() == IResource.FILE) {
                        return resource.getFileExtension().toLowerCase().equals("ecore");
                    }
                    return true;
                }
                return false;
            }
        });
	}

    protected void addOntologies(Resource source, EList ontologies) {
        TreeIterator contents = source.getAllContents();
        while (contents.hasNext()) {
            EObject current = (EObject) contents.next();
            addMetaObjectOntologies(current, ontologies);
        }
    }
    
    protected void addConstraintSet(Resource source, EList constraintSets) {
        TreeIterator contents = source.getAllContents();
        while (contents.hasNext()) {
            EObject current = (EObject) contents.next();
            addMetaObjectConstraintSet(current, constraintSets);
        }
    }
    
    /**
     * Searches the metaobject for ontology annotations and adds them to the list.
     * @param object The metaobject.
     * @param ontologies The list of ontologies to add to.
     */
    private void addMetaObjectOntologies(EObject object, EList ontologies) {
        PlatformKitActionUtil.addOntologies(object, space.eResource().getURI(), ontologies);
    }
    
    /**
     * Searches the metaobject for platform constraint annotations and adds a ConstraintSet object to the list.
     * @param object The metaobject.
     * @param constraintSets The list of ontologies to add to.
     */
    private void addMetaObjectConstraintSet(EObject object, EList constraintSets) {
    	ConstraintSet set = PlatformKitActionUtil.createMetaObjectConstraintSet(object);
    	if (set != null) {
    		constraintSets.add(set);
    	}
    }

}
