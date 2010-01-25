package be.ac.vub.platformkit.java.popup.actions;

import be.ac.vub.platformkit.java.actions.ShowCompatResultAction;
import be.ac.vub.platformkit.java.jobs.JavaProjectCompatJob;

/**
 * Context menu action for creating compatibility reports from Java projects
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class JavaProjectCompatAction extends CompatAction {

	/**
	 * Creates a new {@link JavaProjectCompatAction}.
	 */
	public JavaProjectCompatAction() {
		super();
		job = new JavaProjectCompatJob();
		showResultAction = new ShowCompatResultAction(job, "Result");
	    initJob();
	}
	
}
