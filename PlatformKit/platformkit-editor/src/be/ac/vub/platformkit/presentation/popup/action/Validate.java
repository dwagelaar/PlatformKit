package be.ac.vub.platformkit.presentation.popup.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.editor.preferences.PreferenceConstants;
import be.ac.vub.platformkit.editor.preferences.PreferenceInitializer;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.util.PlatformKitActionUtil;
import be.ac.vub.platformkit.presentation.util.PlatformKitException;
import be.ac.vub.platformkit.ui.util.MessageDialogRunnable;

/**
 * Validates the options in a CDD configuration
 * against a concrete context specification.
 * @author dennis
 *
 */
public class Validate extends ConstraintSpaceAction {
	/**
	 * Constructor for Action1.
	 */
	public Validate() {
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
	}

    /**
     * Invoked when the action is executed.
     * @param monitor
     * @throws Exception
     */
    protected final void runAction(IProgressMonitor monitor)
    throws Exception {
        monitor.beginTask("Validating PlatformKit constraint sets", 7);
        IOntologies ont = space.getKnowledgeBase();
        if (ont == null) {
            monitor.subTask("Loading source ontologies...");
            ont = PreferenceInitializer.getPreferredOntologyFactory().createIOntologies();
            space.setKnowledgeBase(ont);
            if (!space.init(true)) {
                throw new PlatformKitException(
                        "Ontologies not pre-classified - Choose 'Classify Taxonomy' first");
            }
            worked(monitor);
        } else {
            monitor.subTask("Using pre-loaded source ontologies");
            worked(monitor);
        }
        monitor.subTask("Attaching DL reasoner...");
		IPreferenceStore store = PlatformkitEditorPlugin.getPlugin()
				.getPreferenceStore();
		String reasoner = store.getString(PreferenceConstants.P_REASONER);
		if (PreferenceConstants.P_DIG.equals(reasoner)) {
			String url = store.getString(PreferenceConstants.P_DIG_URL);
			ont.setReasonerUrl(url);
			ont.attachDIGReasoner();
		} else {
	        ont.attachPelletReasoner();
		}
        worked(monitor);
        monitor.subTask("Retrieving platform specification...");
        if (!getPlatform(space)) {
            throw new IllegalArgumentException("Must specify a concrete platform for validation");
        }
        worked(monitor);
        monitor.subTask("Retrieving intersection set...");
        ConstraintSet is = space.getIntersectionSet();
        is.getIntersection();
        worked(monitor);
        monitor.subTask("Determining valid constraint sets...");
        List<ConstraintSet> valid = space.getValid();
        worked(monitor);
        monitor.subTask("Detaching reasoner...");
        ont.detachReasoner();
        worked(monitor);
        monitor.subTask("Creating report...");
        String report = createReport(valid);
        writeReport(report, space.eResource().getURI());
        showMessage(report);
        space.getKnowledgeBase().unloadInstances();
        worked(monitor);
    }

    /**
     * @param valid
     * @return a report on the valid options.
     * @throws IllegalArgumentException
     */
    private String createReport(List<ConstraintSet> valid)
    throws IllegalArgumentException {
        StringBuffer msg = new StringBuffer();
        msg.append("Valid constraint sets: ");
        msg.append('\n'); msg.append('\n');
        Assert.isNotNull(valid);
        for (int i = 0; i < valid.size(); i++) {
            ConstraintSet list = valid.get(i);
            msg.append(" - ");
            msg.append(list.getName());
            msg.append('\n');
        }
        return msg.toString();
    }
    
    /**
     * Displays a message dialog.
     * @param message
     */
    private void showMessage(String message) {
        MessageDialogRunnable dlg = new MessageDialogRunnable(
                "Valid Constraint Sets", message);
        PlatformkitEditorPlugin.getPlugin().getWorkbench().getDisplay().syncExec(dlg);
    }
    
    /**
     * Writes the report for the given CDD configuration file.
     * @param report
     * @param uri
     * @throws CoreException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    private void writeReport(String report, URI uri)
    throws CoreException, IOException, IllegalArgumentException {
        IPath platformkitPath = new Path(PlatformKitActionUtil.toPlatformResourcePath(uri));
        Assert.isNotNull(platformkitPath);
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(platformkitPath);
        Assert.isNotNull(file);
        Path path = new Path(
                file.getFullPath().removeFileExtension().
                addFileExtension("valid").
                addFileExtension("txt").lastSegment());
        IContainer cont = file.getParent();
        IFile dest = cont.getFile(path);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream printer = new PrintStream(output);
        printer.print(report);
        printer.flush();
        if (dest.exists()) {
            dest.setContents(new ByteArrayInputStream(output.toByteArray()), 
                    true, true, null);
        } else {
            dest.create(new ByteArrayInputStream(output.toByteArray()), 
                    true, null);
        }
        printer.close();
    }
}
