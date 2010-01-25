package be.ac.vub.platformkit.java.popup.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.WorkbenchException;

import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;
import be.ac.vub.platformkit.kb.IOntologies;

/**
 * General superclass for popup actions with a progress monitor.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public abstract class ProgressMonitorAction implements IActionDelegate, Runnable {

	protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);
	protected Shell shell;
	protected ISelection selection;
	protected IAction action;
	private boolean cancelled = false;
	protected long startTime;

	public ProgressMonitorAction() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
        run(null);
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
	public void run(IAction action) {
	    IRunnableWithProgress op = new IRunnableWithProgress() {
	        public void run(IProgressMonitor monitor) {
	            try {
	                runAction(monitor);
	            } catch (Exception e) {
	                PlatformkitJavaPlugin.getPlugin().report(e);
                	catchCleanup();
	            } finally {
	                monitor.done();
                    finallyCleanup();
	            }
	        }
	    };
	    ProgressMonitorDialog dlg = new ProgressMonitorDialog(
	    		PlatformkitJavaPlugin.getPlugin().getShell());
	    try {
	        cancelled = false;
	        dlg.run(true, true, op);
            runWithMainThread();
	    } catch (InvocationTargetException e) {
	        Throwable t = e.getCause();
	        PlatformkitJavaPlugin.getPlugin().report(t);
	    } catch (Exception e) {
	    	PlatformkitJavaPlugin.getPlugin().report(e);
	    }
	}
	
    /**
     * Invoked when the action is executed.
     * @param monitor
     * @throws Exception
     */
	@Deprecated
    protected abstract void runAction(IProgressMonitor monitor) throws Exception;

    /**
     * Runs after {@link #runAction(IProgressMonitor)} inside main thread.
     * @throws Exception
     */
    protected void runWithMainThread() throws Exception {
    	//stub
    }

	/**
	 * Invoked after an Exception is caught in {@link #runAction(IProgressMonitor))}
	 */
	protected void catchCleanup() {
		//stub
	}

	/**
	 * Invoked in "finally" block after {@link #runAction(IProgressMonitor)}
	 */
	protected void finallyCleanup() {
		//stub
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	    this.selection = selection;
	    this.action = action;
	}

	/**
	 * @return True if last run was cancelled.
	 */
	public boolean isCancelled() {
	    return cancelled;
	}

    /**
     * Increases the progressmonitor by 1.
     * @param monitor
     * @throws WorkbenchException if user pressed cancel button.
     */
    protected void worked(IProgressMonitor monitor, String message) 
    throws OperationCanceledException {
        worked(monitor, null, message);
    }

    /**
     * Increases the progressmonitor by 1.
     * @param monitor
     * @param subTask The subtask, or null if none.
     * @throws WorkbenchException if user pressed cancel button.
     */
    protected void worked(IProgressMonitor monitor, ProgressMonitorAction subTask, String message) 
    throws OperationCanceledException {
        monitor.worked(1);
        if (message != null) {
        	long currentTime = System.currentTimeMillis();
            logger.info(message + " at " + formatTime(currentTime-startTime));
        }
        if (subTask != null) {
            if (subTask.isCancelled()) {
                monitor.setCanceled(true);
            }
        }
        if (monitor.isCanceled()) {
            cancelled = true;
            throw new OperationCanceledException("Operation cancelled by user");
        }
    }
    
    /**
     * Logs and starts a new task on the progress monitor
     * @param monitor
     * @param message
     */
    protected void subTask(IProgressMonitor monitor, String message) {
        logger.info(message);
        monitor.subTask(message);
    }
    
    /**
     * Logs and starts a series of tasks, and sets the start time.
     * @param monitor
     * @param message
     * @param totalWork The amount of subtasks
     */
    protected void beginTask(IProgressMonitor monitor, String message, int totalWork) {
    	setStartTime(System.currentTimeMillis());
        monitor.beginTask(message, totalWork);
        logger.info(message);
    }

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @param startTime the startTime to set
	 */
	protected void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * @param millis
	 * @return String formatted as "m:ss"
	 */
	private String formatTime(long millis) {
		long seconds = millis / 1000;
		long minutes = seconds / 60;
		seconds = seconds % 60;
		StringBuffer s = new StringBuffer();
		s.append(minutes);
		s.append(':');
		if (seconds < 10) {
			s.append('0');
		}
		s.append(seconds);
		return s.toString();
	}

}