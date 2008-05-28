package be.ac.vub.platformkit.presentation.popup.action;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.domain.IEditingDomainProvider;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.WorkbenchException;

import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.util.PlatformKitActionUtil;
import be.ac.vub.platformkit.presentation.util.PlatformSpecDialogRunnable;

/**
 * Abstract right-click action with progress monitor class.
 * @author dennis
 *
 */
public abstract class PlatformKitAction implements IObjectActionDelegate {

	protected Logger logger = Logger.getLogger(IOntologies.LOGGER);
	protected EditingDomain editingDomain = null;
    protected ISelection selection;
    protected IAction action;
    protected ResourceSet resourceSet = new ResourceSetImpl();
    protected PlatformKitActionUtil util = new PlatformKitActionUtil();
	protected ViewerFilter filter = null;

	private boolean cancelled = false;
    
    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    	if (targetPart instanceof IEditingDomainProvider) {
    		this.editingDomain = ((IEditingDomainProvider) targetPart).getEditingDomain();
    	}
    }

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

    /**
     * @see IActionDelegate#run(IAction)
     */
    public void run(IAction action) {
        this.action = action;
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) {
                try {
                    runAction(monitor);
                } catch (Exception e) {
                	PlatformkitEditorPlugin.INSTANCE.report(e);
                	catchCleanup();
                } finally {
                    monitor.done();
                    finallyCleanup();
                }
            }
        };
        ProgressMonitorDialog dlg = new ProgressMonitorDialog(
        		PlatformkitEditorPlugin.INSTANCE.getShell());
        try {
            cancelled = false;
            dlg.run(true, true, op);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            PlatformkitEditorPlugin.INSTANCE.report(t);
        } catch (InterruptedException ie) {
        	PlatformkitEditorPlugin.INSTANCE.report(ie);
        } finally {
            resourceSet.getResources().clear();
        }
    }
    
    /**
     * Invoked when the action is executed.
     * @param monitor
     * @throws Exception
     */
    protected abstract void runAction(IProgressMonitor monitor)
    throws Exception;

    /**
     * Increases the progressmonitor by 1.
     * @param monitor
     * @throws WorkbenchException if user pressed cancel button.
     */
    protected void worked(IProgressMonitor monitor) 
    throws OperationCanceledException {
        worked(monitor, null);
    }

    /**
     * Increases the progressmonitor by 1.
     * @param monitor
     * @param subTask The subtask, or null if none.
     * @throws WorkbenchException if user pressed cancel button.
     */
    protected void worked(IProgressMonitor monitor, PlatformKitAction subTask) 
    throws OperationCanceledException {
        monitor.worked(1);
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
     * Loads a registered Ecore model from the given file.
     * @param file
     * @return The Ecore resource containing the model.
     * @throws IllegalArgumentException
     * @throws RuntimeException
     */
    protected Resource loadModel(IResource file) 
    throws IllegalArgumentException, RuntimeException {
        URI source = URI.createPlatformResourceURI(
                file.getProject().getName() + '/' +
                file.getProjectRelativePath().toString(),
                true);
        return resourceSet.getResource(source, true);
    }

    /**
     * @return True if last run was cancelled.
     */
    public boolean isCancelled() {
        return cancelled;
    }

	/**
	 * Loads the platform specification ontology chosen via
	 * a FileDialog.
	 * @param space
	 * @return True if any platform specification was loaded.
	 * @throws CoreException
	 */
	protected boolean getPlatform(ConstraintSpace space) throws CoreException {
	    boolean loaded = false;
	    PlatformSpecDialogRunnable dlg = new PlatformSpecDialogRunnable("Load platform specification");
        if (filter != null) {
            dlg.setFilter(filter);
        }
        PlatformkitEditorPlugin.getPlugin().getWorkbench().getDisplay().syncExec(dlg);
	    Object[] filenames = dlg.getFiles();
	    if (filenames != null) {
	        for (int i = 0; i < filenames.length; i++) {
	            if (filenames[i] instanceof IFile) {
	                IFile file = (IFile) filenames[i];
	                space.getKnowledgeBase().loadInstances(file.getContents());
	                loaded = true;
	            } else if (filenames[i] instanceof InputStream) {
                    space.getKnowledgeBase().loadInstances((InputStream) filenames[i]);
                    loaded = true;
                }
	        }
	    }
	    return loaded;
	}

	/**
	 * @param filter The filter to set.
	 */
	public void setFilter(ViewerFilter filter) {
	    this.filter = filter;
	}

	/**
	 * Invoked after an Exception is caught in {@link #run(IAction)}
	 */
	protected void catchCleanup() {
		//stub
	}
	
	/**
	 * Invoked in "finally" block after {@link #run(IAction)}
	 */
	protected void finallyCleanup() {
		//stub
	}
}
