package be.ac.vub.platformkit.java.actions;

import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;

import be.ac.vub.platformkit.java.jobs.CompatJob;
import be.ac.vub.platformkit.java.ui.util.OpenFileInEditorRunnable;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.ui.util.MessageDialogRunnable;

/**
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 *
 */
public class ShowCompatResultAction extends Action {

	protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);
	
	private CompatJob job;

	/**
	 * 
	 */
	public ShowCompatResultAction(CompatJob job) {
		super();
		setJob(job);
	}

	/**
	 * @param text
	 */
	public ShowCompatResultAction(CompatJob job, String text) {
		super(text);
		setJob(job);
	}

	/**
	 * @param text
	 * @param image
	 */
	public ShowCompatResultAction(CompatJob job, String text, ImageDescriptor image) {
		super(text, image);
		setJob(job);
	}

	/**
	 * @param text
	 * @param style
	 */
	public ShowCompatResultAction(CompatJob job, String text, int style) {
		super(text, style);
		setJob(job);
	}

	@Override
	public void run() {
		CompatJob job = getJob();
		if (job.getResult() == null) {
			return;
		}
		// show result
		IFile outputFile = job.getOutputFile();
		boolean compatible = outputFile == null;
		int mode;
		final StringBuffer summary = new StringBuffer();
		summary.append(job.getInputName());
		if (compatible) {
			summary.append(" is compatible with ");
		    summary.append(job.getApiList());
		    mode = MessageDialogRunnable.MODE_INFORMATION;
		} else {
			summary.append(" is not compatible with ");
		    summary.append(job.getApiList());
		    summary.append(".\nCheck \"" + outputFile.getFullPath().toString() + "\" for details.");
		    mode = MessageDialogRunnable.MODE_ERROR;
		}
		final MessageDialogRunnable dlg = new MessageDialogRunnable(
		        "Compatible with " + job.getApiList(), summary.toString());
		dlg.setMode(mode);
		PlatformUI.getWorkbench().getDisplay().syncExec(dlg);
		// open report in editor
		if (!compatible) {
			PlatformUI.getWorkbench().getDisplay().syncExec(new OpenFileInEditorRunnable(outputFile));
		}
	}

    /**
	 * @return the job
	 */
	public CompatJob getJob() {
		return job;
	}

	/**
	 * @param job the job to set
	 */
	public void setJob(CompatJob job) {
		this.job = job;
	}
}
