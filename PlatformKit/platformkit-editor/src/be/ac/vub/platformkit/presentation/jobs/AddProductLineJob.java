/**
 * 
 */
package be.ac.vub.platformkit.presentation.jobs;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.presentation.util.PlatformKitActionUtil;

/**
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 *
 */
public class AddProductLineJob extends AddConstraintSetsJob {

	/**
	 * Creates a new {@link AddProductLineJob}.
	 */
	public AddProductLineJob() {
		super("Adding Product Line Meta-models");
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.presentation.jobs.AddConstraintSetsJob#addConstraintSet(org.eclipse.emf.ecore.resource.Resource, org.eclipse.emf.common.util.EList)
	 */
	@Override
	protected void addConstraintSet(Resource source, EList<ConstraintSet> constraintSets) {
        TreeIterator<EObject> contents = source.getAllContents();
        while (contents.hasNext()) {
            EObject current = contents.next();
            addMetaObjectConstraintSet(current, constraintSets);
        }
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.presentation.jobs.AddConstraintSetsJob#addOntologies(org.eclipse.emf.ecore.resource.Resource, org.eclipse.emf.common.util.EList)
	 */
	@Override
	protected void addOntologies(Resource source, EList<String> ontologies) {
        TreeIterator<EObject> contents = source.getAllContents();
        while (contents.hasNext()) {
            EObject current = contents.next();
            addMetaObjectOntologies(current, ontologies);
        }
	}

    /**
     * Searches the metaobject for ontology annotations and adds them to the list.
     * @param object The metaobject.
     * @param ontologies The list of ontologies to add to.
     */
    protected void addMetaObjectOntologies(EObject object, EList<String> ontologies) {
        PlatformKitActionUtil.addOntologies(object, getSpace().eResource().getURI(), ontologies);
    }
    
    /**
     * Searches the metaobject for platform constraint annotations and adds a ConstraintSet object to the list.
     * @param object The metaobject.
     * @param constraintSets The list of ontologies to add to.
     */
    protected void addMetaObjectConstraintSet(EObject object, EList<ConstraintSet> constraintSets) {
    	ConstraintSet set = PlatformKitActionUtil.createMetaObjectConstraintSet(object);
    	if (set != null) {
    		constraintSets.add(set);
    	}
    }

}
