package be.ac.vub.platformkit.presentation.popup.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.presentation.jobs.ClassifyTaxonomyJob;

/**
 * Pre-classifies the taxonomy of ontology classes for a
 * given CDD configuration. Note that this needs to be redone
 * whenever the ontologies and/or the CDD configurations changes.
 * The output is written to &lt;CDD config basename&gt;.inferred.owl.
 * Requires a DIG reasoner at port 8080.
 * @author dennis
 *
 */
public class ClassifyTaxonomy extends ObjectSelectionAction {

	protected ClassifyTaxonomyJob job;
	
    /**
	 * Creates a new {@link ClassifyTaxonomy}.
	 */
	public ClassifyTaxonomy() {
		super();
		job = new ClassifyTaxonomyJob();
	}
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
	public void run(IAction action) {
	    // run operation
    	job.setSpace((ConstraintSpace) ((IStructuredSelection) selection).getFirstElement());
	    job.setUser(true);
	    // lock editor
	    IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) 
	    	part.getSite().getAdapter(IWorkbenchSiteProgressService.class);
	    siteService.schedule(job);
    }
    
}
