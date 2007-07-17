package be.ac.vub.platformkit.presentation.popup.action;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.resource.Resource;

import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.util.FileDialogRunnable;

/**
 * Abstract action for adding new ConstraintSets to a ConstraintSpace.
 * Each context-constrained element should have an EAnnotation with
 * source set to 'CDDToolkit' and containing a DetailsEntry with the key
 * set to 'ContextConstraint' and the value set to the ontology constraint.
 * @author dennis
 * @see EAnnotation
 */
public abstract class AddConstraintSets extends ConstraintSpaceAction {
    private String sourceName;
    /**
	 * Constructor for Action1.
     * @param sourceName The description of the source model type,
     * e.g. "Product Line" or "Product Configuration".
	 */
	public AddConstraintSets(String sourceName) {
		super();
        setSourceName(sourceName);
	}

    /**
     * Invoked when the action is executed.
     * @param monitor
     * @throws Exception
     */
    protected final void runAction(IProgressMonitor monitor)
    throws Exception {
        monitor.beginTask("Adding " + getSourceName() + "s", 2);
        monitor.subTask("Getting " + getSourceName() + "(s)...");
        Resource[] sources = getSourceModels();
        worked(monitor);
        if (sources == null) {
            return;
        }
        monitor.subTask("Adding Constraint Sets for selected " + getSourceName() + "s...");
        List commands = createCommands(sources);
        Command cmd = new CompoundCommand(commands);
        editingDomain.getCommandStack().execute(cmd);
        worked(monitor);
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
        FileDialogRunnable dlg = new FileDialogRunnable("Load " + getSourceName() + "(s)");
        if (filter != null) {
            dlg.setFilter(filter);
        }
        PlatformkitEditorPlugin.getPlugin().getWorkbench().getDisplay().syncExec(dlg);
        Object[] files = dlg.getFiles();
        if (files != null) {
            Resource[] models = new Resource[files.length];
            for (int i = 0; i < models.length; i++) {
                models[i] = loadModel((IResource) files[i]);
            }
            return models;
        } else {
            return null;
        }
    }
    
    /**
     * @return A List of Commands to be executed to add new ConstraintSets that reflect the given source models.
     * @param sources
     */
    private List createCommands(Resource[] sources) {
    	List commands = new ArrayList();
    	EList ontologies = new BasicEList();
    	EList constraintSets = new BasicEList();
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
    protected abstract void addOntologies(Resource source, EList ontologies);

    /**
     * Searches source for constraints and adds a constraint set to the list.
     * @see ConstraintSpace#getConstraintSet()
     * @param source
     * @param constraintSets
     */
    protected abstract void addConstraintSet(Resource source, EList constraintSets);

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
