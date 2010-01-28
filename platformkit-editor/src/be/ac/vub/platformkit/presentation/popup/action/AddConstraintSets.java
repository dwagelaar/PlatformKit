package be.ac.vub.platformkit.presentation.popup.action;

import org.eclipse.core.resources.IResource;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.jobs.AddConstraintSetsJob;
import be.ac.vub.platformkit.ui.util.FileDialogRunnable;

/**
 * Abstract action for adding new {@link ConstraintSet}s to a {@link ConstraintSpace}.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public abstract class AddConstraintSets extends ViewerFilterAction {

    private String sourceName;
	protected AddConstraintSetsJob job;

    /**
	 * Creates a new {@link AddConstraintSets}.
     * @param sourceName The description of the source model type,
     * e.g. "Product Line" or "Product Configuration".
	 */
	public AddConstraintSets(String sourceName) {
		super();
        setSourceName(sourceName);
	}

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
	public void run(IAction action) {
        Resource[] sources = getSourceModels();
        if (sources == null) {
            return;
        }
	    // run operation
        job.setSourceName(getSourceName());
        job.setEditingDomain(editingDomain);
        job.setSources(sources);
    	job.setSpace((ConstraintSpace) ((IStructuredSelection) selection).getFirstElement());
    	job.setUser(true);
	    // lock editor
	    IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) 
	    	part.getSite().getAdapter(IWorkbenchSiteProgressService.class);
	    siteService.schedule(job);
    }
    
    /**
     * Loads the source model chosen via
     * a FileDialog.
     * @return The Ecore resource containing the model.
     * @throws IllegalArgumentException
     * @throws RuntimeException
     */
    protected Resource[] getSourceModels() 
    throws IllegalArgumentException, RuntimeException {
        FileDialogRunnable dlg = new FileDialogRunnable();
        dlg.setTitle("Load " + getSourceName() + "(s)");
        dlg.setMessage("Select " + getSourceName() + "(s)");
        dlg.setInstruction("Select resources:");
        if (getFilter() != null) {
            dlg.setFilter(getFilter());
        }
        PlatformkitEditorPlugin.getPlugin().getWorkbench().getDisplay().syncExec(dlg);
        Object[] files = dlg.getSelection();
        if (files != null) {
    		ResourceSet resourceSet = new ResourceSetImpl();
            Resource[] models = new Resource[files.length];
            for (int i = 0; i < models.length; i++) {
                models[i] = loadModel((IResource) files[i], resourceSet);
            }
            return models;
        } else {
            return null;
        }
    }
    
	/**
	 * Loads a registered Ecore model from the given file.
	 * @param file
	 * @param resourceSet
	 * @return The Ecore resource containing the model.
	 * @throws IllegalArgumentException
	 * @throws RuntimeException
	 */
	protected Resource loadModel(IResource file, ResourceSet resourceSet) 
	throws IllegalArgumentException, RuntimeException {
		URI source = URI.createPlatformResourceURI(
				file.getProject().getName() + '/' +
				file.getProjectRelativePath().toString(),
				true);
		return resourceSet.getResource(source, true);
	}

    /**
     * @param sourceName The sourceName to set.
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * @return Returns the sourceName.
     */
    public String getSourceName() {
        return sourceName;
    }

}
