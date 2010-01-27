package be.ac.vub.platformkit.ui.util;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.PlatformUI;

/**
 * Wraps an error dialog in a {@link Runnable}.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class ErrorDialogRunnable implements Runnable {

    private IStatus st;

    /**
     * Creates a new {@link ErrorDialogRunnable}.
     * @param e The error to report.
     * @param logger The logger to use for reporting
     */
    public ErrorDialogRunnable(Throwable e, ILog logger) {
        String message;
        if (e.getMessage() == null) {
            message = e.getClass().getName(); 
        } else {
            message = e.getMessage();
        }
        setStatus(new Status(
                IStatus.ERROR, 
                logger.getBundle().getSymbolicName(), 
                IStatus.OK, 
                message, 
                e));
        logger.log(getStatus());
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
    	IStatus st = getStatus();
        ErrorDialog dlg = new ErrorDialog(
        		PlatformUI.getWorkbench().getDisplay().getActiveShell(),
                "Error",
                st.getMessage(),
                st,
                IStatus.ERROR);
        dlg.open();
    }
    
	/**
	 * @return the status
	 */
	public IStatus getStatus() {
		return st;
	}

	/**
	 * @param st the status to set
	 */
	protected void setStatus(IStatus st) {
		this.st = st;
	}
}
