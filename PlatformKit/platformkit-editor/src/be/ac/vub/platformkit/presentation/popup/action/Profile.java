package be.ac.vub.platformkit.presentation.popup.action;

import org.eclipse.core.resources.IResource;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.jobs.ProfileJob;
import be.ac.vub.platformkit.ui.util.PlatformSpecDialogRunnable;

/**
 * Profiles the options in an EMF editor
 * against a platform instance specification.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class Profile extends ViewerFilterAction {

	protected ProfileJob job;

	/**
	 * Creates a new {@link Profile}.
	 */
	public Profile() {
		super();
		setFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IResource) {
					IResource resource = (IResource) element;
					if (resource.getType() == IResource.FILE) {
						return resource.getFileExtension().toLowerCase().equals("owl");
					}
					return true;
				}
				return false;
			}
		});
		job = new ProfileJob();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		PlatformSpecDialogRunnable dlg = new PlatformSpecDialogRunnable();
		if (getFilter() != null) {
			dlg.setFilter(getFilter());
		}
		PlatformkitEditorPlugin.getPlugin().getWorkbench().getDisplay().syncExec(dlg);
		// run operation
		job.setPlatformInstanceSources(dlg.getFiles());
		job.setSelectedObject((EObject) ((IStructuredSelection) selection).getFirstElement());
		job.setEditingDomain(editingDomain);
		job.setUser(true);
		// lock editor
		IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) 
		part.getSite().getAdapter(IWorkbenchSiteProgressService.class);
		siteService.schedule(job);
	}

}
