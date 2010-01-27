package be.ac.vub.platformkit.presentation.popup.action;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import be.ac.vub.platformkit.presentation.jobs.AddProductLineJob;

/**
 * Adds the metaclass constraints of a product line, described in Ecore,
 * to the PlatformKit constraint space model.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class AddProductLine extends AddConstraintSets {

	/**
	 * Creates a new {@link AddProductLine}.
	 */
	public AddProductLine() {
		super("Product Line Meta-model");
		job = new AddProductLineJob();
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

}
