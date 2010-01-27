/**
 * 
 */
package be.ac.vub.platformkit.presentation.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.editor.preferences.PreferenceInitializer;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.presentation.util.PlatformKitException;

/**
 * Operation for sorting the constraint sets in a PlatformKit model.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class SortPlatformkitModelJob extends ConstraintSpaceJob {

	/**
	 * Most-specific first mode.
	 */
	public static final int MOST_SPECIFIC = 0;
	/**
	 * Least-specific first mode.
	 */
	public static final int LEAST_SPECIFIC = 1;

	private int mode;
	private String title;

	/**
	 * Creates a new {@link SortPlatformkitModelJob}.
	 * @param mode {@link #MOST_SPECIFIC} or {@link #LEAST_SPECIFIC}.
	 * @throws IllegalArgumentException 
	 */
	public SortPlatformkitModelJob(int mode) throws IllegalArgumentException {
		super("Sorting Platformkit Model");
		setMode(mode);
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.jobs.ProgressMonitorJob#runAction(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void runAction(IProgressMonitor monitor) throws Exception {
		beginTask(monitor, getName() + " " + getTitle(), 6);
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
			subTask(monitor, "Using pre-loaded source ontologies...");
			worked(monitor, null);
		}
		//
		// 2
		//
		subTask(monitor, "Attaching transitive reasoner...");
		ont.attachTransitiveReasoner();
		worked(monitor, "Attached transitive reasoner");
		//
		// 3
		//
		subTask(monitor, "Retrieving intersection set...");
		ConstraintSet is = space.getIntersectionSet();
		is.getIntersection();
		worked(monitor, "Retrieved intersection set");
		//
		// 4
		//
		EList<ConstraintSet> specific;
		switch (getMode()) {
		case MOST_SPECIFIC:
			subTask(monitor, "Determining most-specific constraint sets...");
			specific = space.getMostSpecific(false);
			break;
		case LEAST_SPECIFIC:
			subTask(monitor, "Determining least-specific constraint sets...");
			specific = space.getLeastSpecific(false);
			break;
		default: throw new IllegalArgumentException("Invalid mode");
		}
		worked(monitor, "Determined most/least specific");
		//
		// 5
		//
		subTask(monitor, "Detaching reasoner...");
		ont.detachReasoner();
		worked(monitor, "Detached reasoner");
		//
		// 6
		//
		subTask(monitor, "Sorting constraint sets...");
		//cannot run in compound command
		getEditingDomain().getCommandStack().execute(createRemoveConstraintSetCommand(space.getConstraintSet()));
		getEditingDomain().getCommandStack().execute(createAddConstraintSetCommand(specific));
		worked(monitor, "Sorted constraint sets");
	}

	/**
	 * @return {@link #MOST_SPECIFIC} or {@link #LEAST_SPECIFIC}.
	 */
	public int getMode() {
		return mode;
	}

	/**
	 * @param mode {@link #MOST_SPECIFIC} or {@link #LEAST_SPECIFIC}.
	 * @throws IllegalArgumentException
	 */
	public void setMode(int mode) throws IllegalArgumentException {
		this.mode = mode;
		this.mode = mode;
		StringBuffer buf = new StringBuffer();
		switch (mode) {
		case MOST_SPECIFIC:  buf.append("Most");
		break;
		case LEAST_SPECIFIC: buf.append("Least");
		break;
		default: throw new IllegalArgumentException("Invalid mode");
		}
		buf.append(" Specific First");
		setTitle(buf.toString());
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	protected void setTitle(String title) {
		this.title = title;
	}

}
