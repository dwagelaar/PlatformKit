package be.ac.vub.platformkit.java.popup.actions;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
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
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2m.atl.common.ATLLogger;
import org.eclipse.m2m.atl.core.ATLCoreException;
import org.eclipse.m2m.atl.core.IExtractor;
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
import be.ac.vub.platformkit.java.popup.util.PlatformAPIDialogRunnable;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * Context menu action for creating compatibility reports for UML dependency models 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
@SuppressWarnings("restriction")
public class CompatAction implements IObjectActionDelegate {
	
	/**
	 * Defines model loading strategy interface and behaviour
	 */
	public abstract class ModelLoadingStrategy {

		private String atlVMName;
		protected ATLUtil atlUtil;
		
		/**
		 * Creates a new ModelLoadingStrategy
		 * @param atlVMName
		 * @throws ATLCoreException
		 */
		public ModelLoadingStrategy(String atlVMName) throws ATLCoreException {
			Assert.isNotNull(atlVMName);
			this.atlVMName = atlVMName;
			atlUtil = new ATLUtil(atlVMName);
		}
		
		/**
		 * @return The UML2 metamodel
		 * @throws IOException 
		 * @throws ATLCoreException 
		 */
		public IReferenceModel getUML2() throws ATLCoreException, IOException {
			return atlUtil.loadRefModel(UML_MM.openStream(), "UML2", UML_MM.toString(), MODEL_HANDLER);
		}

		/**
		 * @param uml2 The UML2 metamodel
		 * @return The CompatibilityReport profile model
		 * @throws IOException 
		 * @throws ATLCoreException 
		 */
		public IModel getCRProfile(IReferenceModel uml2) throws ATLCoreException, IOException {
			return atlUtil.loadModel(uml2, CR_PROF, "CR", CR_PROF);
		}
		
		/**
		 * @param uml2 The UML2 metamodel
		 * @param emf_uri The EMF URI to load from
		 * @return the loaded 'IN' model
		 * @throws ATLCoreException 
		 */
		public abstract IModel loadINModel(IReferenceModel uml2, URI emf_uri) throws ATLCoreException;
		
		/**
		 * @param uml2 The UML2 metamodel
		 * @param source The source input stream to load from
		 * @param path The path to the source model
		 * @return the loaded 'DEPS' model
		 * @throws ATLCoreException 
		 */
		public IModel loadDEPSModel(IReferenceModel uml2, InputStream source,
				String path) throws ATLCoreException {
			return atlUtil.loadModel(uml2, /*source*/ path, "DEPS", path);
		}
		
		/**
		 * @param uml2 The UML2 metamodel
		 * @param path The future path of the model
		 * @return the new 'REPORT' model
		 * @throws ATLCoreException 
		 */
		public IModel createOUTModel(IReferenceModel uml2, String path) throws ATLCoreException {
			return atlUtil.newModel(uml2, "REPORT", path);
		}
		
		/**
		 * @param atlVMName
		 * @return True if this ModelLoadingStrategy is valid for the given ATL VM name.
		 */
		public boolean isValidFor(String atlVMName, boolean useCache) {
			return this.atlVMName.equals(atlVMName);
		}
		
		/**
		 * @return an ATL launcher
		 * @throws ATLCoreException 
		 */
		public ILauncher getAtlLauncher() throws ATLCoreException {
			return atlUtil.getLauncher();
		}
		
		/**
		 * @return an ATL extractor
		 */
		public IExtractor getAtlExtractor() {
			return atlUtil.getExtractor();
		}
		
		/**
		 * Flushes internal ATL objects and models
		 * @throws ATLCoreException
		 */
		public void flush() throws ATLCoreException {
			atlUtil = new ATLUtil(atlVMName);
		}
	}
	
	private class SimpleModelLoadingStrategy extends ModelLoadingStrategy {
		
		public SimpleModelLoadingStrategy(String atlVMName) throws ATLCoreException {
			super(atlVMName);
		}

		@Override
		public IModel loadINModel(IReferenceModel uml2, URI emfUri) throws ATLCoreException {
			return atlUtil.loadModel(uml2, emfUri.toString(), "IN", emfUri.toString());
		}

		/* (non-Javadoc)
		 * @see be.ac.vub.platformkit.java.popup.actions.CompatAction.ModelLoadingStrategy#isValidFor(java.lang.String, boolean)
		 */
		@Override
		public boolean isValidFor(String atlVMName, boolean useCache) {
			return super.isValidFor(atlVMName, useCache) && !useCache;
		}
	}

	private class CachingModelLoadingStrategy extends ModelLoadingStrategy {

	    protected Map<URI,SoftReference<IModel>> inModelCache = new HashMap<URI,SoftReference<IModel>>();

		public CachingModelLoadingStrategy(String atlVMName) throws ATLCoreException {
			super(atlVMName);
		}

		@Override
		public IModel loadINModel(IReferenceModel uml2, URI emfUri) throws ATLCoreException {
			IModel in = null;
        	if (inModelCache.containsKey(emfUri)) {
        		in = inModelCache.get(emfUri).get();
        	}
        	if (in == null) {
                in = atlUtil.loadModel(uml2, emfUri.toString(), "IN", emfUri.toString());
                inModelCache.put(emfUri, new SoftReference<IModel>(in));
        	}
			return in;
		}

		/* (non-Javadoc)
		 * @see be.ac.vub.platformkit.java.popup.actions.CompatAction.ModelLoadingStrategy#isValidFor(java.lang.String, boolean)
		 */
		@Override
		public boolean isValidFor(String atlVMName, boolean useCache) {
			return super.isValidFor(atlVMName, useCache) && useCache;
		}
	}
	
	public class CompatActionRunner {
		private IReferenceModel uml2;
		private IModel crProfile;
		private IModel deps;
		private IModel in;
		private IModel report;
		private IFile file;
		private String crLocation;
		private IPath crPath;
		
		/**
		 * @return the uml2
		 */
		public IReferenceModel getUml2() {
			return uml2;
		}
		/**
		 * @param uml2 the uml2 to set
		 */
		protected void setUml2(IReferenceModel uml2) {
			this.uml2 = uml2;
		}
		/**
		 * @return the crProfile
		 */
		public IModel getCrProfile() {
			return crProfile;
		}
		/**
		 * @param crProfile the crProfile to set
		 */
		protected void setCrProfile(IModel crProfile) {
			this.crProfile = crProfile;
		}
		/**
		 * @return the deps
		 */
		public IModel getDeps() {
			return deps;
		}
		/**
		 * @param deps the deps to set
		 */
		protected void setDeps(IModel deps) {
			this.deps = deps;
		}
		/**
		 * @return the in
		 */
		public IModel getIn() {
			return in;
		}
		/**
		 * @param in the in to set
		 */
		protected void setIn(IModel in) {
			this.in = in;
		}
		/**
		 * @return the report
		 */
		public IModel getReport() {
			return report;
		}
		/**
		 * @param report the report to set
		 */
		protected void setReport(IModel report) {
			this.report = report;
		}
		
		/**
		 * @return the file
		 */
		public IFile getFile() {
			return file;
		}
		/**
		 * @param file the file to set
		 */
		protected void setFile(IFile file) {
			this.file = file;
		}
		/**
		 * @return the crPath
		 */
		protected IPath getCrPath() {
			return crPath;
		}
		/**
		 * @param crPath the crPath to set
		 */
		protected void setCrPath(IPath crPath) {
			this.crPath = crPath;
			setCrLocation("platform:/resource/" + file.getProject().getName() + "/" + crPath.toString());
		}
		/**
		 * @return the crLocation
		 */
		public String getCrLocation() {
			return crLocation;
		}
		/**
		 * @param crLocation the crLocation to set
		 */
		protected void setCrLocation(String crLocation) {
			this.crLocation = crLocation;
		}
		/**
		 * Loads UML2 metamodel
		 * @param monitor
		 * @throws IOException 
		 * @throws ATLCoreException 
		 */
		
		public void loadUml2(IProgressMonitor monitor) throws ATLCoreException, IOException {
	        subTask(monitor, "Loading UML2 meta-model...");
	        setUml2(modelLoader.getUML2());
	        worked(monitor, "Loaded UML2 meta-model");
		}

		/**
		 * Loads CompatiblityReport profile
		 * @param monitor
		 * @throws IOException 
		 * @throws ATLCoreException 
		 */
		public void loadCRProfile(IProgressMonitor monitor) throws ATLCoreException, IOException {
	        subTask(monitor, "Loading CompatiblityReport profile...");
	        setCrProfile(modelLoader.getCRProfile(getUml2()));
	        worked(monitor, "Loaded CompatiblityReport profile");
		}
		
		/**
		 * Loads dependency model
		 * @param monitor
		 * @throws IOException 
		 * @throws ATLCoreException 
		 */
		public void loadDepsModel(IProgressMonitor monitor) throws ATLCoreException, CoreException {
			subTask(monitor, "Loading dependency model...");
	        setFile((IFile) ((IStructuredSelection) selection).getFirstElement());
	        final IFile file = getFile();
	        Assert.isNotNull(file);
	        final String fileLocation = "platform:/resource/" + file.getProject().getName() + "/" + file.getProjectRelativePath().toString();
	        setDeps(modelLoader.loadDEPSModel(getUml2(), file.getContents(), fileLocation));
	        setCrPath(file.getParent().getProjectRelativePath().append("pkCompatReport.uml"));
	        worked(monitor, "Loaded dependency model");
		}

		/**
		 * Loads API model
		 * @param monitor
		 * @param apiName The API name to display
		 * @param emf_uri The API model URI to load from
		 * @throws IOException 
		 * @throws ATLCoreException 
		 */
		public void loadAPIModel(IProgressMonitor monitor, String apiName, URI emf_uri) throws ATLCoreException, CoreException {
	        subTask(monitor, "Loading API model: " + apiName);
	        setIn(modelLoader.loadINModel(getUml2(), emf_uri));
	        worked(monitor, "Loaded API model");
		}

		/**
		 * Performs a single run of the UML2CompatbilityReport transformation
		 * @param monitor
		 * @return The transformation result
		 * @throws ATLCoreException 
		 * @throws CoreException 
		 * @throws IOException 
		 * @throws InvocationTargetException 
		 * @throws IllegalAccessException 
		 * @throws NoSuchMethodException 
		 * @throws IllegalArgumentException 
		 * @throws SecurityException 
		 * @throws ClassCastException 
		 */
		public boolean run(IProgressMonitor monitor) throws ATLCoreException, CoreException, IOException, ClassCastException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            subTask(monitor, "Creating compatibility report...");
    		modelLoader.flush(); // report must be in new resource set
        	setReport(modelLoader.createOUTModel(getUml2(), getCrLocation()));
            final ILauncher launcher = modelLoader.getAtlLauncher();
    		launcher.addInModel(getCrProfile(), "CR", "UML2");
    		launcher.addInModel(getDeps(), "DEPS", "UML2");
    		launcher.addInModel(getIn(), "IN", "UML2");
    		launcher.addOutModel(getReport(), "REPORT", "UML2");
    		launcher.addLibrary("UML2", uml2Lib.openStream());
    		launcher.addLibrary("UML2Comparison", uml2Comparison.openStream());
    		Object result = launcher.launch(ILauncher.RUN_MODE, monitor, vmoptions, uml2CompatibilityReport.openStream());
    		worked(monitor, "Created compatibility report");
    		return ATLUtil.getBooleanValue(result);
		}

		/**
		 * Merges current compatibility report with otherReport by intersection
		 * @param monitor
		 * @param otherReport
		 * @throws ATLCoreException
		 * @throws CoreException
		 * @throws IOException
		 * @throws ClassCastException
		 * @throws SecurityException
		 * @throws IllegalArgumentException
		 * @throws NoSuchMethodException
		 * @throws IllegalAccessException
		 * @throws InvocationTargetException
		 */
		public void mergeReport(IProgressMonitor monitor, IModel otherReport) throws ATLCoreException, CoreException, IOException, ClassCastException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            subTask(monitor, "Merging compatibility reports...");
			IModel report = getReport();
			if ((otherReport == null) || (report == null)) {
				throw new IllegalArgumentException("Cannot merge null reports");
			}
    		modelLoader.flush(); // report must be in new resource set
        	setReport(modelLoader.createOUTModel(getUml2(), getCrLocation()));
            final ILauncher launcher = modelLoader.getAtlLauncher();
    		launcher.addInModel(report, "IN", "UML2");
    		launcher.addInModel(otherReport, "MERGE", "UML2");
    		launcher.addOutModel(getReport(), "OUT", "UML2");
    		launcher.addLibrary("UML2", uml2Lib.openStream());
    		launcher.launch(ILauncher.RUN_MODE, monitor, vmoptions, uml2Copy.openStream(), uml2CRMerge.openStream());
    		worked(monitor, "Merged compatibility reports");
		}

		/**
		 * Prunes the current compatibility report
		 * @param monitor
		 * @return True if the pruned report is empty (i.e. the result is compatible)
		 * @throws ATLCoreException
		 * @throws CoreException
		 * @throws IOException
		 * @throws ClassCastException
		 * @throws SecurityException
		 * @throws IllegalArgumentException
		 * @throws NoSuchMethodException
		 * @throws IllegalAccessException
		 * @throws InvocationTargetException
		 */
		public boolean pruneReport(IProgressMonitor monitor) throws ATLCoreException, CoreException, IOException, ClassCastException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            subTask(monitor, "Pruning compatibility report...");
			IModel report = getReport();
			if (report == null) {
				throw new IllegalArgumentException("Cannot prune null report");
			}
    		modelLoader.flush(); // report must be in new resource set
        	setReport(modelLoader.createOUTModel(getUml2(), getCrLocation()));
            final ILauncher launcher = modelLoader.getAtlLauncher();
    		launcher.addInModel(report, "IN", "UML2");
    		launcher.addOutModel(getReport(), "OUT", "UML2");
    		Object result = launcher.launch(ILauncher.RUN_MODE, monitor, vmoptions, uml2Copy.openStream(), uml2CRPrune.openStream());
    		worked(monitor, "Pruned compatibility report");
    		return ATLUtil.getBooleanValue(result);
		}

		/**
		 * Saves the report model
		 * @param monitor
		 * @throws ATLCoreException 
		 * @throws CoreException 
		 */
		public void saveReport(IProgressMonitor monitor) throws ATLCoreException, CoreException {
			subTask(monitor, "Saving compatibility report...");
			modelLoader.getAtlExtractor().extract(getReport(), getCrLocation());
    		getFile().getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
    		worked(monitor, "Saved compatibility report");
		}

		/**
		 * @return The file that contains the report model
		 */
		public IFile getReportFile() {
			return getFile().getProject().getFile(getCrPath());
		}
	}
	
	private static final URL UML_MM  = PlatformkitJavaPlugin.getPlugin().getBundle().getResource("metamodels/UMLProfiles.ecore");
	private static final String CR_PROF = "http://soft.vub.ac.be/platformkit-java/CompatibilityReport.profile.uml";
	private static final String MODEL_HANDLER = "UML2";
	
	protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);

	protected final URL uml2CompatibilityReport = 
    	PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2CompatibilityReport.asm");
    protected final URL uml2Comparison = 
    	PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2Comparison.asm");
    protected final URL uml2Lib = 
    	PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2.asm");
    protected final URL uml2Copy = 
    	PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2Copy.asm");
    protected final URL uml2CRMerge = 
    	PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2CRMerge.asm");
    protected final URL uml2CRPrune = 
    	PlatformkitJavaPlugin.getPlugin().getBundle().getResource("transformations/UML2CRPrune.asm");
    protected final Map<String, Object> vmoptions = new HashMap<String, Object>();

    protected ISelection selection;
    protected IAction action;
    protected IFile outputFile;
    protected IPreferenceStore store = PlatformkitEditorPlugin.getPlugin().getPreferenceStore();
    protected ModelLoadingStrategy modelLoader = null;
    
    private long startTime;
    private boolean cancelled = false;

    /**
     * Creates a CompatAction
     * @param apiResource The URL to the UML model of the API to compare against
     * @param apiName The name of the API to compare against
     */
	public CompatAction() {
		super();
	    vmoptions.put("printExecutionTime", "true");
	    vmoptions.put("allowInterModelReferences", "true");
	    vmoptions.put("supportUML2Stereotypes", "true");
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
        checkAndSwitchStrategy();
        CompatActionRunner runner = new CompatActionRunner();
        runActionWithRunner(monitor, runner);
    }
    
    /**
     * Runs the main action using the given runner object
     * @param monitor
     * @param runner
     * @throws Exception
     */
    protected void runActionWithRunner(IProgressMonitor monitor, CompatActionRunner runner)
    		throws Exception {
        outputFile = null;
	    PlatformAPIDialogRunnable paDlg = new PlatformAPIDialogRunnable("Select platform API model(s)");
	    PlatformkitJavaPlugin.getPlugin().getWorkbench().getDisplay().syncExec(paDlg);
	    Object[] emf_uris = paDlg.getResult();
	    if (emf_uris == null || emf_uris.length == 0) {
	    	return; //cancel
	    }
	    beginTask(monitor, "Determining compatibility", 6 + emf_uris.length * 3);
    	ATLLogger.getLogger().addHandler(PlatformkitEditorPlugin.getHandler());
    	//
    	// Step 1
    	//
        runner.loadUml2(monitor);
    	//
    	// Step 2
    	//
        runner.loadCRProfile(monitor);
    	//
    	// Step 3
    	//
        runner.loadDepsModel(monitor);
        StringBuffer apiList = null;
        IModel lastReport = null;
        boolean compatible = true;
        for (Object emf_uri_obj : emf_uris) {
        	URI emf_uri = (URI) emf_uri_obj;
            //
            // Step 4+n
            //
        	String apiName = paDlg.getLabelFor(emf_uri);
        	runner.loadAPIModel(monitor, apiName, emf_uri);
            //
            // Step 5+n
            //
        	boolean thisCompatible = runner.run(monitor);
            //
            // Step 6+n
            //
        	compatible &= thisCompatible;
        	if (!thisCompatible) {
        		if (lastReport != null) {
        			runner.mergeReport(monitor, lastReport);
        		} else {
        			worked(monitor, null);
        		}
        		lastReport = runner.getReport();
        	} else {
    			worked(monitor, null);
        	}
        	if (apiList == null) {
        		apiList = new StringBuffer();
        		apiList.append(apiName);
        	} else {
        		apiList.append(", ");
        		apiList.append(apiName);
        	}
        }
        //
        // Step 7
        //
        if (!compatible) {
            compatible = runner.pruneReport(monitor);
        } else {
        	worked(monitor, null);
        }
        //
        // Step 8
        //
        if (!compatible) {
            runner.saveReport(monitor);
        } else {
        	worked(monitor, null);
        }
		//
		// Step 9
		//
        subTask(monitor, "Showing result...");
        int mode;
        final StringBuffer summary = new StringBuffer();
        summary.append(runner.getFile().getName());
        if (compatible) {
        	summary.append(" is compatible with ");
            summary.append(apiList);
            mode = MessageDialogRunnable.MODE_INFORMATION;
        } else {
        	summary.append(" is not compatible with ");
            summary.append(apiList);
            summary.append(".\nCheck \"" + runner.getCrLocation() + "\" for details.");
            mode = MessageDialogRunnable.MODE_ERROR;
            outputFile = runner.getReportFile();
        }
        final MessageDialogRunnable dlg = new MessageDialogRunnable(
                "Compatible with " + apiList, summary.toString());
    	dlg.setMode(mode);
        worked(monitor, "Finished");
        PlatformkitJavaPlugin.getPlugin().getWorkbench().getDisplay().syncExec(dlg);
    }
    
    /**
     * Checks the current model loading strategy and updates it if necessary
     * @throws ATLCoreException
     */
    protected void checkAndSwitchStrategy() throws ATLCoreException {
        final boolean useCache = store.getBoolean(PreferenceConstants.P_CACHE_API);
		final String atlVMName = store.getString(PreferenceConstants.P_ATLVM);
		if (atlVMName == null || atlVMName.equals("")) {
			throw new ATLCoreException("No ATL VM chosen; select one in the PlatformKit preferences page");
		}
		if (modelLoader != null && modelLoader.isValidFor(atlVMName, useCache)) {
			return;
		}
		if (useCache) {
			modelLoader = new CachingModelLoadingStrategy(atlVMName);
		} else {
			modelLoader = new SimpleModelLoadingStrategy(atlVMName);
		}
		Assert.isNotNull(modelLoader);
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
    protected void worked(IProgressMonitor monitor, CompatAction subTask, String message) 
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
	 * Invoked after an Exception is caught in {@link #run(IAction)}
	 */
	protected void catchCleanup() {
		//stub
	}
	
	/**
	 * Invoked in "finally" block after {@link #run(IAction)}
	 */
	protected void finallyCleanup() {
		try {
        	ATLLogger.getLogger().removeHandler(PlatformkitEditorPlugin.getHandler());
			modelLoader.flush();
		} catch (ATLCoreException e) {
			PlatformkitJavaPlugin.getPlugin().report(e);
		}
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
}