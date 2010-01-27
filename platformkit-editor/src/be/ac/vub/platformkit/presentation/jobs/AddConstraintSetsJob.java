package be.ac.vub.platformkit.presentation.jobs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.resource.Resource;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;

/**
 * Abstract job for adding new {@link ConstraintSet}s to a {@link ConstraintSpace}.
 * Each context-constrained element should have an {@link EAnnotation} with
 * source set to 'PlatformKit' and containing a details entry with the key
 * set to 'PlatformConstraint' and the value set to the ontology class that
 * expressed the platform dependency constraint.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 *
 */
public abstract class AddConstraintSetsJob extends ConstraintSpaceJob {

	private String sourceName;
	private Resource[] sources;

	/**
	 * Creates a new {@link AddConstraintSetsJob}
	 * @param name the name of the job.
	 */
	public AddConstraintSetsJob(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.jobs.ProgressMonitorJob#runAction(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void runAction(IProgressMonitor monitor) throws Exception {
		beginTask(monitor, "Adding " + getSourceName() + "s", 1);
		subTask(monitor, "Adding Constraint Sets for selected " + getSourceName() + "s...");
		Resource[] sources = getSources();
		List<Command> commands = createCommands(sources);
		Command cmd = new CompoundCommand(commands);
		getEditingDomain().getCommandStack().execute(cmd);
		worked(monitor, "Added Constraint Sets");
	}

	/**
	 * @return A List of Commands to be executed to add new ConstraintSets that reflect the given source models.
	 * @param sources
	 */
	protected List<Command> createCommands(Resource[] sources) {
		List<Command> commands = new ArrayList<Command>();
		EList<String> ontologies = new BasicEList<String>();
		EList<ConstraintSet> constraintSets = new BasicEList<ConstraintSet>();
		ConstraintSpace space = getSpace();
		ontologies.addAll(space.getOntology());
		for (int i = 0; i < sources.length; i++) {
			addOntologies(sources[i], ontologies);
			addConstraintSet(sources[i], constraintSets);
		}
		ontologies.removeAll(space.getOntology());
		if (!ontologies.isEmpty()) {
			commands.add(createAddOntologyCommand(ontologies));
		}
		if (!constraintSets.isEmpty()) {
			commands.add(createAddConstraintSetCommand(constraintSets));
		}
		return commands;
	}

	/**
	 * Searches source for ontologies and adds them to the list.
	 * @see ConstraintSpace#getOntology()
	 * @param source
	 * @param ontologies
	 */
	protected abstract void addOntologies(Resource source, EList<String> ontologies);

	/**
	 * Searches source for constraints and adds a constraint set to the list.
	 * @see ConstraintSpace#getConstraintSet()
	 * @param source
	 * @param constraintSets
	 */
	protected abstract void addConstraintSet(Resource source, EList<ConstraintSet> constraintSets);

	/**
	 * @return the sourceName
	 */
	public String getSourceName() {
		return sourceName;
	}

	/**
	 * @param sourceName the sourceName to set
	 */
	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	/**
	 * @return the sources
	 */
	public Resource[] getSources() {
		return sources;
	}

	/**
	 * @param sources the sources to set
	 */
	public void setSources(Resource[] sources) {
		this.sources = sources;
	}

}
