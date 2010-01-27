package be.ac.vub.platformkit.presentation.jobs;

import java.io.InputStream;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.editor.preferences.PreferenceConstants;
import be.ac.vub.platformkit.editor.preferences.PreferenceInitializer;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.util.PlatformKitException;

/**
 * Operation to validate the options in a PlatformKit constraint space model
 * against platform instances.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class ValidateJob extends ConstraintSpaceJob {

	private String report;

	/**
	 * Creates a new {@link ValidateJob}.
	 */
	public ValidateJob() {
		super("Validating PlatformKit constraint sets");
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.jobs.ProgressMonitorJob#runAction(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void runAction(IProgressMonitor monitor) throws Exception {
        beginTask(monitor, getName(), 7);
        ConstraintSpace space = getSpace();
        IOntologies ont = space.getKnowledgeBase();
        //
        // 1
        //
        if (ont == null) {
            subTask(monitor, "Loading source ontologies...");
            ont = PreferenceInitializer.getPreferredOntologyFactory().createIOntologies();
            space.setKnowledgeBase(ont);
            if (!space.init(true)) {
                throw new PlatformKitException(
                        "Ontologies not pre-classified - Choose 'Classify Taxonomy' first");
            }
            worked(monitor, "Loaded source ontologies");
        } else {
            subTask(monitor, "Using pre-loaded source ontologies");
            worked(monitor, null);
        }
        //
        // 2
        //
        subTask(monitor, "Attaching DL reasoner...");
        attachDLReasoner(monitor, ont);
        worked(monitor, "Attached DL reasoner");
        //
        // 3
        //
        subTask(monitor, "Retrieving platform instances...");
        if (!loadPlatformInstances()) {
            throw new IllegalArgumentException("Must specify at least one platform instance for validation");
        }
        worked(monitor, "Retrieved platform instances");
        //
        // 4
        //
        subTask(monitor, "Retrieving intersection set...");
        ConstraintSet is = space.getIntersectionSet();
        is.getIntersection();
        worked(monitor, "Retrieved intersection set");
        //
        // 5
        //
        subTask(monitor, "Determining valid constraint sets...");
        List<ConstraintSet> valid = space.getValid();
        worked(monitor, "Determined valid constraint sets");
        //
        // 6
        //
        subTask(monitor, "Detaching reasoner...");
        ont.detachReasoner();
        worked(monitor, "Detached reasoner");
        //
        // 7
        //
        subTask(monitor, "Creating report...");
        setReport(createReport(valid));
        ont.unloadInstances();
        worked(monitor, "Created report");
	}

	/**
	 * Attaches either a DIG reasoner or the built-in Pellet reasoner.
	 * @param monitor
	 * @param ont
	 */
	protected void attachDLReasoner(IProgressMonitor monitor, IOntologies ont) {
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
	}

	/**
	 * Loads the platform specification ontologies specified by
	 * {@link #getPlatformInstanceSources()}.
	 * @return True if any platform specification was loaded.
	 * @throws CoreException
	 */
	protected boolean loadPlatformInstances() throws CoreException {
		boolean loaded = false;
		IOntologies ont = getSpace().getKnowledgeBase();
		Object[] sources = getPlatformInstanceSources();
		if (sources != null) {
			for (int i = 0; i < sources.length; i++) {
				if (sources[i] instanceof IFile) {
					IFile file = (IFile) sources[i];
					ont.loadInstances(file.getContents());
					loaded = true;
				} else if (sources[i] instanceof InputStream) {
					ont.loadInstances((InputStream) sources[i]);
					loaded = true;
				}
			}
		}
		return loaded;
	}

    /**
     * @param valid
     * @return a report on the valid options.
     * @throws IllegalArgumentException
     */
    protected String createReport(List<ConstraintSet> valid)
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
	 * @return the report
	 */
	public String getReport() {
		return report;
	}

	/**
	 * @param report the report to set
	 */
	protected void setReport(String report) {
		this.report = report;
	}

}
