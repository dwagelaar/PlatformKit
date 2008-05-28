package be.ac.vub.platformkit.presentation.popup.action;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.IOntologiesFactory;
import be.ac.vub.platformkit.presentation.util.PlatformKitException;

/**
 * Abstract action for sorting the constraint sets in a PlatformKit model.
 * @author dennis
 *
 */
public abstract class SortPlatformkitModel extends ConstraintSpaceAction {
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
	 * Constructor for Action1.
     * @throws IllegalArgumentException
	 */
	public SortPlatformkitModel(int mode) throws IllegalArgumentException {
		super();
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
        title = buf.toString();
	}

    /**
     * Invoked when the action is executed.
     * @param monitor
     * @throws Exception
     */
    protected final void runAction(IProgressMonitor monitor)
    throws Exception {
        monitor.beginTask("Sorting PlatformKit model " + title, 5);
        IOntologies ont = space.getKnowledgeBase();
        if (ont == null) {
            monitor.subTask("Loading source ontologies...");
            ont = IOntologiesFactory.INSTANCE.createIOntologies();
            space.setKnowledgeBase(ont);
            if (!space.init(true)) {
                throw new PlatformKitException(
                        "Ontologies not pre-classified - Choose 'Classify Taxonomy' first");
            }
            worked(monitor);
        } else {
        	monitor.subTask("Using pre-loaded source ontologies...");
        	worked(monitor);
        }
        monitor.subTask("Attaching transitive reasoner...");
        ont.attachTransitiveReasoner();
        worked(monitor);
        monitor.subTask("Retrieving intersection set...");
        ConstraintSet is = space.getIntersectionSet();
        is.getIntersection();
        worked(monitor);
        EList<ConstraintSet> specific;
        switch (mode) {
            case MOST_SPECIFIC:
                monitor.subTask("Determining most-specific constraint sets...");
                specific = space.getMostSpecific(false);
            break;
            case LEAST_SPECIFIC:
                monitor.subTask("Determining least-specific constraint sets...");
                specific = space.getLeastSpecific(false);
            break;
            default: throw new IllegalArgumentException("Invalid mode");
        }
        worked(monitor);
        monitor.subTask("Sorting CDD configuration options...");
        //cannot run in compound command
        editingDomain.getCommandStack().execute(createRemoveConstraintSetCommand(space.getConstraintSet()));
        editingDomain.getCommandStack().execute(createAddConstraintSetCommand(specific));
        worked(monitor);
    }
}
