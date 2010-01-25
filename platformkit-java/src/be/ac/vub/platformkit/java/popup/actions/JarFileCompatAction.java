package be.ac.vub.platformkit.java.popup.actions;

import be.ac.vub.platformkit.java.actions.ShowCompatResultAction;
import be.ac.vub.platformkit.java.jobs.JarFileCompatJob;

/**
 * Context menu action for creating compatibility reports from Jar files
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class JarFileCompatAction extends CompatAction {

	/**
	 * Creates a new {@link JarFileCompatAction}.
	 */
	public JarFileCompatAction() {
		super();
		job = new JarFileCompatJob();
		showResultAction = new ShowCompatResultAction(job, "Result");
	    initJob();
	}
	
}
