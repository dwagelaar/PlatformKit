package be.ac.vub.platformkit.java.popup.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.progress.IProgressConstants;

import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;
import be.ac.vub.platformkit.java.actions.ShowCompatResultAction;
import be.ac.vub.platformkit.java.jobs.CompatJob;
import be.ac.vub.platformkit.java.ui.util.PlatformAPIDialogRunnable;

/**
 * Context menu action for creating compatibility reports for UML dependency models 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class CompatAction extends ProgressMonitorAction {
	
	protected CompatJob job;
	protected ShowCompatResultAction showResultAction;
    
    /**
     * Creates a CompatAction
     * @param apiResource The URL to the UML model of the API to compare against
     * @param apiName The name of the API to compare against
     */
	public CompatAction() {
		super();
		job = new CompatJob();
		showResultAction = new ShowCompatResultAction(job, "Result");
	    initJob();
	}

	/**
	 * Initialises the internal job
	 */
	protected void initJob() {
		job.setProperty(IProgressConstants.ACTION_PROPERTY, showResultAction);
	    job.addJobChangeListener(new JobChangeAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.core.runtime.jobs.JobChangeAdapter#done(org.eclipse.core.runtime.jobs.IJobChangeEvent)
			 */
			@Override
			public void done(IJobChangeEvent event) {
				showResultAction.run();
			}
	    });
	}

    /**
     * Invoked when the action is executed.
     * @param monitor
     * @throws Exception
     */
    @Override
    protected void runAction(IProgressMonitor monitor) throws Exception {
    	//TODO refactor out
    }
    
	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.java.popup.actions.ProgressMonitorAction#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		// Select platform API dialog
	    PlatformAPIDialogRunnable paDlg = new PlatformAPIDialogRunnable();
	    paDlg.setTitle("Select API models");
	    paDlg.setMessage("Select platform API models to check against");
	    paDlg.setInstruction("Select up to one API model from each category:");
	    PlatformkitJavaPlugin.getPlugin().getWorkbench().getDisplay().syncExec(paDlg);
	    URI[] emf_uris = paDlg.getResult();
	    if (emf_uris == null || emf_uris.length == 0) {
	    	return; //cancel
	    }
	    // run operation
	    job.setInput(((IStructuredSelection) selection).getFirstElement());
	    job.setEmfUris(emf_uris);
	    job.setEmfUriLabels(paDlg.getLabels());
	    job.setUser(true);
	    job.schedule();
	}
	
}