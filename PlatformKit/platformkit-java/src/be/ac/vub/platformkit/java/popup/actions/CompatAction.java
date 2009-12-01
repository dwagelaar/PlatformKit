package be.ac.vub.platformkit.java.popup.actions;

import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2m.atl.common.ATLLogger;
import org.eclipse.m2m.atl.core.IModel;
import org.eclipse.m2m.atl.core.IReferenceModel;
import org.eclipse.m2m.atl.core.launch.ILauncher;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.dialogs.DialogUtil;
import org.eclipse.ui.part.FileEditorInput;

import be.ac.vub.platformkit.editor.preferences.PreferenceConstants;
import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;
import be.ac.vub.platformkit.java.popup.util.ATLUtil;
import be.ac.vub.platformkit.java.popup.util.ErrorDialogRunnable;
import be.ac.vub.platformkit.java.popup.util.MessageDialogRunnable;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

@SuppressWarnings("restriction")
public abstract class CompatAction implements IObjectActionDelegate {

	private static final URL UML_MM  = PlatformkitJavaPlugin.getPlugin().getBundle().getResource("metamodels/UMLProfiles.ecore");
	private static final URL CR_PROF = PlatformkitJavaPlugin.getPlugin().getBundle().getResource("profiles/CompatibilityReport.uml");
	private static final String MODEL_HANDLER = "UML2";
	
	protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);
	
    protected ISelection selection;
    protected IAction action;
    protected IFile outputFile;
    protected Map<URL,SoftReference<IModel>> apiModelCache = new HashMap<URL,SoftReference<IModel>>();

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
            if (outputFile != null) {
                openFileInEditor(outputFile);
            }
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            PlatformkitJavaPlugin.getPlugin().report(t);
        } catch (CoreException ce) {
        	PlatformkitJavaPlugin.getPlugin().report(ce);
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
    	final long startTime = System.currentTimeMillis();
        logger.info("Determining compatibility with " + apiName);
        monitor.beginTask("Determining compatibility with " + apiName, 5);
    	ATLLogger.getLogger().addHandler(PlatformkitEditorPlugin.getHandler());
        logger.info("Loading meta-models...");
        monitor.subTask("Loading meta-models...");
		final IPreferenceStore store = PlatformkitEditorPlugin.getPlugin().getPreferenceStore();
		final String atlVMName = store.getString(PreferenceConstants.P_ATLVM);
		final ATLUtil atlUtil = new ATLUtil(atlVMName);
        final IReferenceModel uml2 = atlUtil.loadRefModel(UML_MM.openStream(), "UML2", UML_MM.toString(), MODEL_HANDLER);
        final IModel crProf = atlUtil.loadModel(uml2, CR_PROF.openStream(), "CR", CR_PROF.toString());
        final IFile file =
        	(IFile) ((IStructuredSelection) selection).getFirstElement();
        Assert.isNotNull(file);
        final String fileLocation = "/" + file.getProject().getName() + "/" + file.getProjectRelativePath().toString();
        final IPath crPath = file.getParent().getProjectRelativePath().append("pkCompatReport.uml");
        final String crLocation = "/" + file.getProject().getName() + "/" + crPath.toString();
        IModel report = atlUtil.newModel(uml2, "REPORT", crLocation);
        //
        // 1
        //
        worked(monitor);
    	long currentTime = System.currentTimeMillis();
        logger.info("Loaded meta-models at " + formatTime(currentTime-startTime));
        logger.info("Loading dependency model...");
        monitor.subTask("Loading dependency model...");
        IModel deps = atlUtil.loadModel(uml2, file.getContents(), "DEPS", fileLocation);
        //
        // 2
        //
        worked(monitor);
    	currentTime = System.currentTimeMillis();
        logger.info("Loaded dependency model at " + formatTime(currentTime-startTime));
        logger.info("Loading API model...");
        monitor.subTask("Loading API model...");
        IModel in = null;
        if (store.getBoolean(PreferenceConstants.P_CACHE_API)) {
        	if (apiModelCache.containsKey(apiResource)) {
        		in = apiModelCache.get(apiResource).get();
        	}
        	if (in == null) {
                in = atlUtil.loadModel(uml2, apiResource.openStream(), "IN", apiResource.toString());
                apiModelCache.put(apiResource, new SoftReference<IModel>(in));
        	}
        } else {
            in = atlUtil.loadModel(uml2, apiResource.openStream(), "IN", apiResource.toString());
        }
        //
        // 3
        //
        worked(monitor);
    	currentTime = System.currentTimeMillis();
        logger.info("Loaded API model at " + formatTime(currentTime-startTime));
        logger.info("Running ATL transformation...");
        monitor.subTask("Running ATL transformation...");
        final URL uml2CompatibilityReport = 
        	PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2CompatibilityReport.asm");
        final URL uml2Comparison = 
        	PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2Comparison.asm");
        final URL uml2Lib = 
        	PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2.asm");
        final Map<String, Object> vmoptions = new HashMap<String, Object>();
        vmoptions.put("printExecutionTime", "true");
        final ILauncher launcher = atlUtil.getLauncher();
		launcher.addInModel(crProf, "CR", "UML2");
		launcher.addInModel(deps, "DEPS", "UML2");
		launcher.addInModel(in, "IN", "UML2");
		launcher.addOutModel(report, "REPORT", "UML2");
		launcher.addLibrary("UML2", uml2Lib.openStream());
		launcher.addLibrary("UML2Comparison", uml2Comparison.openStream());
		Object result = launcher.launch(ILauncher.RUN_MODE, monitor, vmoptions, uml2CompatibilityReport.openStream());
		Assert.isNotNull(result);
		if ("ASMBoolean".equals(result.getClass().getName())) {
			final Method getSymbol = result.getClass().getDeclaredMethod("getSymbol");
			result = (Boolean)getSymbol.invoke(result);
		}
		Assert.isTrue(result instanceof Boolean);
		boolean compatible = ((Boolean)result).booleanValue();
        //
        // 4
        //
        worked(monitor);
    	currentTime = System.currentTimeMillis();
        logger.info("Ran ATL transformation at " + formatTime(currentTime-startTime));
        if (!compatible) {
            logger.info("Saving compatibility report...");
            monitor.subTask("Saving compatibility report...");
            atlUtil.getExtractor().extract(report, crLocation);
    		file.getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
        	currentTime = System.currentTimeMillis();
            logger.info("Saved compatibility report at " + formatTime(currentTime-startTime));
        }
		//
		// 5
		//
        worked(monitor);
        logger.info("Showing result...");
        monitor.subTask("Showing result...");
        int mode;
        final StringBuffer summary = new StringBuffer();
        summary.append(file.getName());
        if (compatible) {
        	summary.append(" is compatible with ");
            summary.append(apiName);
            mode = MessageDialogRunnable.MODE_INFORMATION;
            outputFile = null;
        } else {
        	summary.append(" is not compatible with ");
            summary.append(apiName);
            summary.append(".\nCheck \"" + crLocation + "\" for details.");
            mode = MessageDialogRunnable.MODE_ERROR;
            outputFile = file.getProject().getFile(crPath);
        }
        final MessageDialogRunnable dlg = new MessageDialogRunnable(
                "Compatible with " + apiName, summary.toString());
    	dlg.setMode(mode);
    	ATLLogger.getLogger().removeHandler(PlatformkitEditorPlugin.getHandler());
    	currentTime = System.currentTimeMillis();
        logger.info("Finished at " + formatTime(currentTime-startTime));
        PlatformkitJavaPlugin.getPlugin().getWorkbench().getDisplay().syncExec(dlg);
    }
    
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

    private void openFileInEditor(IFile file) throws CoreException {
		//
		// get default editor descriptor
		//
		IEditorRegistry editorRegistry = WorkbenchPlugin.getDefault()
				.getEditorRegistry();
		IEditorDescriptor defaultEditorDescriptor = editorRegistry
				.getDefaultEditor(file.getName(), file.getContentDescription().getContentType());
		if (defaultEditorDescriptor == null) {
			defaultEditorDescriptor = editorRegistry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
		}
		//
		// Open new file in editor
		//
		IWorkbenchWindow dw = PlatformkitJavaPlugin.getPlugin().getWorkbench()
				.getActiveWorkbenchWindow();
		Assert.isNotNull(dw);
		FileEditorInput fileEditorInput = new FileEditorInput(file);
		try {
			IWorkbenchPage page = dw.getActivePage();
			if (page != null)
				page.openEditor(fileEditorInput, defaultEditorDescriptor
						.getId());
		} catch (PartInitException e) {
			DialogUtil.openError(dw.getShell(), "Could not open new file", e
					.getMessage(), e);
		}
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