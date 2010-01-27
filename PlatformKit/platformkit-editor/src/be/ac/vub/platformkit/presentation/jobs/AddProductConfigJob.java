package be.ac.vub.platformkit.presentation.jobs;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.presentation.util.PlatformKitActionUtil;

/**
 * Operation to add a product configuration, described in an annotated DSL,
 * to the PlatformKit constraint space model.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class AddProductConfigJob extends AddConstraintSetsJob {

	/**
	 * Creates a new {@link AddProductConfigJob}.
	 */
	public AddProductConfigJob() {
		super("Adding Product Configuration Models");
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.presentation.jobs.AddConstraintSetsJob#addConstraintSet(org.eclipse.emf.ecore.resource.Resource, org.eclipse.emf.common.util.EList)
	 */
	@Override
	protected void addConstraintSet(Resource source, EList<ConstraintSet> constraintSets) {
    	ConstraintSet set = PlatformKitActionUtil.createModelConstraintSet(source);
    	if (set != null) {
    		constraintSets.add(set);
    	}
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.presentation.jobs.AddConstraintSetsJob#addOntologies(org.eclipse.emf.ecore.resource.Resource, org.eclipse.emf.common.util.EList)
	 */
	@Override
	protected void addOntologies(Resource source, EList<String> ontologies) {
        PlatformKitActionUtil.addOntologies(source, getSpace().eResource().getURI(), ontologies);
	}

}
