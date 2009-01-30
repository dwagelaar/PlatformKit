package be.ac.vub.platformkit.java.popup.actions;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2m.atl.adt.launching.AtlVM;
import org.eclipse.m2m.atl.engine.AtlEMFModelHandler;
import org.eclipse.m2m.atl.engine.AtlModelHandler;
import org.eclipse.m2m.atl.engine.vm.ModelLoader;
import org.eclipse.m2m.atl.engine.vm.nativelib.ASMBoolean;
import org.eclipse.m2m.atl.engine.vm.nativelib.ASMModel;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.uml2.uml.UMLPackage;

import be.ac.vub.platformkit.editor.preferences.PreferenceConstants;
import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;
import be.ac.vub.platformkit.java.popup.util.ErrorDialogRunnable;
import be.ac.vub.platformkit.java.popup.util.MessageDialogRunnable;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

public abstract class CompatAction implements IObjectActionDelegate {
	
	protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);
	
    protected ISelection selection;
    protected IAction action;

    private boolean cancelled = false;
    private URL apiResource = null;
    private String apiName = null;

    /**
     * Creates a CompatAction
     * @param apiResource The URL to the UML model of the API to compare against
     * @param apiName The name of the API to compare against
     */
	public CompatAction(URL apiResource, String apiName) {
		super();
		Assert.isNotNull(apiResource);
		Assert.isNotNull(apiName);
		this.apiResource = apiResource;
		this.apiName = apiName;
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
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
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            PlatformkitJavaPlugin.getPlugin().report(t);
        } catch (InterruptedException ie) {
        	PlatformkitJavaPlugin.getPlugin().report(ie);
        }
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
	}
	
    /**
     * @return True if last run was cancelled.
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * Reports an exception/error in the log and on the screen.
     * @param e the exception to report.
     */
    public void report(Throwable e) {
        PlatformUI.getWorkbench().getDisplay().syncExec(new ErrorDialogRunnable(e));
    }

    /**
     * Invoked when the action is executed.
     * @param monitor
     * @throws Exception
     */
    protected void runAction(IProgressMonitor monitor) throws Exception {
        monitor.beginTask("Determining compatibility with " + apiName, 5);
        monitor.subTask("Loading models...");
        AtlEMFModelHandler amh;
        try {
        	amh = (AtlEMFModelHandler) AtlModelHandler.getDefault("UML2");
        } catch (RuntimeException e) {
        	logger.warning("UML2 model handler not available; falling back to EMF model handler");
            amh = (AtlEMFModelHandler) AtlModelHandler.getDefault(AtlModelHandler.AMH_EMF);
        }
        final ModelLoader ml = amh.createModelLoader();
        final ASMModel uml2 = ml.loadModel("UML2", ml.getMOF(), "uri:" + UMLPackage.eINSTANCE.getNsURI());
        final IFile file =
        	(IFile) ((IStructuredSelection) selection).getFirstElement();
        Assert.isNotNull(file);
        worked(monitor);
        final ASMModel previn = ml.loadModel("PREVIN", uml2, file.getContents());
        worked(monitor);
        final ASMModel in = ml.loadModel("IN", uml2, apiResource.openStream());
        worked(monitor);
        monitor.subTask("Running ATL query...");
        final Map<String, String> params = Collections.emptyMap();
        final Map<String, ASMModel> models = new HashMap<String, ASMModel>();
        models.put(uml2.getName(), uml2);
        models.put(previn.getName(), previn);
        models.put(in.getName(), in);
        final URL uml2CompatibilityComparison = 
        	PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2CompatibilityComparison.asm");
        final URL uml2Comparison = 
        	PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2Comparison.asm");
        final List<URL> superimpose = Collections.emptyList();
        final Map<String, URL> libs = new HashMap<String, URL>();
        libs.put("UML2Comparison", uml2Comparison);
        final Map<String, String> options = new HashMap<String, String>();
        options.put("printExecutionTime", "true");
		final IPreferenceStore store = PlatformkitEditorPlugin.getPlugin()
				.getPreferenceStore();
		final String atlVMName = store.getString(PreferenceConstants.P_ATLVM);
		final AtlVM atlVM = AtlVM.getVM(atlVMName);
		final Object result = atlVM.launch(uml2CompatibilityComparison, libs, models, params, superimpose, options);
		boolean compatible;
		if (result instanceof ASMBoolean) {
			compatible = ((ASMBoolean)result).getSymbol();
		} else {
			Assert.isTrue(result instanceof Boolean);
			compatible = ((Boolean)result).booleanValue();
		}
        worked(monitor);
        monitor.subTask("Showing result...");
        final StringBuffer report = new StringBuffer();
        report.append(file.getName());
        if (compatible) {
        	report.append(" is compatible with ");
        } else {
        	report.append(" is not compatible with ");
        }
        report.append(apiName);
        report.append(".\nCheck the ATL console log for details.");
        final MessageDialogRunnable dlg = new MessageDialogRunnable(
                "Compatible with " + apiName, report.toString());
        if (compatible) {
        	dlg.setMode(MessageDialogRunnable.MODE_INFORMATION);
        } else {
        	dlg.setMode(MessageDialogRunnable.MODE_ERROR);
        }
        PlatformkitJavaPlugin.getPlugin().getWorkbench().getDisplay().syncExec(dlg);
        worked(monitor);
    }

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
    protected void worked(IProgressMonitor monitor, CompatAction subTask) 
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