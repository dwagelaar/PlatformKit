package be.ac.vub.platformkit.presentation.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

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
        st = PlatformkitEditorPlugin.INSTANCE.log(message, IStatus.ERROR, e);
    }
    
    public void run() {
        ErrorDialog dlg = new ErrorDialog(
        		PlatformkitEditorPlugin.INSTANCE.getShell(),
                "Error",
                st.getMessage(),
                st,
                IStatus.ERROR);
        dlg.open();
    }
}
