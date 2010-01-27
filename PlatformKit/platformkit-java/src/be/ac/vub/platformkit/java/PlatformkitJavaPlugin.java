package be.ac.vub.platformkit.java;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import be.ac.vub.platformkit.ui.util.ErrorDialogRunnable;

/**
 * PlatformKit for Java platforms plug-in class 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformkitJavaPlugin extends AbstractUIPlugin {
	
	public static final String PLATFORMAPI_EXT_POINT = "be.ac.vub.platformkit.java.apimodel";
	
	private static PlatformkitJavaPlugin plugin = null;
	
	public static PlatformkitJavaPlugin getPlugin() {
		return plugin;
	}

	public PlatformkitJavaPlugin() {
		super();
		// Remember the static instance.
		plugin = this;
	}

    /**
     * @return The active shell.
     */
    public Shell getShell() {
        return getWorkbench().getDisplay().getActiveShell();
    }

    /**
     * Reports an exception/error in the log and on the screen.
     * @param e the exception to report.
     */
    public void report(Throwable e) {
        getWorkbench().getDisplay().syncExec(new ErrorDialogRunnable(e, getLog()));
    }

    /**
     * Logs a message.
     * @param message the log message.
     * @param level the log level (OK, INFO, WARNING, ERROR)
     * @param exception the related exception, if any.
     */
    public IStatus log(String message, int level, Throwable exception) {
        IStatus st = new Status(
                level, 
                getPlugin().getBundle().getSymbolicName(), 
                IStatus.OK, 
                message, 
                exception);
        getPlugin().getLog().log(st);
        return st;
    }
    
    /**
     * @param imageFilePath
     * @return The ImageDescriptor object for imageFilePath
     * @see #imageDescriptorFromPlugin(String, String)
     */
    public ImageDescriptor getImageDescriptor(String imageFilePath) {
    	return imageDescriptorFromPlugin(getBundle().getSymbolicName(), imageFilePath);
    }

}
