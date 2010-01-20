package be.ac.vub.platformkit.java.popup.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;

import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;

/**
 * Wraps the displaying of an error dialog in a Runnable 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class ErrorDialogRunnable implements Runnable {
    private IStatus st;

    /**
     * Creates a new ErrorDialogRunnable.
     * @param e The error to report.
     */
    public ErrorDialogRunnable(Throwable e) {
        String message;
        if (e.getMessage() == null) {
            message = e.getClass().getName(); 
        } else {
            message = e.getMessage();
        }
        st = PlatformkitJavaPlugin.getPlugin().log(message, IStatus.ERROR, e);
    }
    
    public void run() {
        ErrorDialog dlg = new ErrorDialog(
        		PlatformkitJavaPlugin.getPlugin().getShell(),
                "Error",
                st.getMessage(),
                st,
                IStatus.ERROR);
        dlg.open();
    }
}
