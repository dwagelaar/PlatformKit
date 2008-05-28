package be.ac.vub.platformkit.presentation.popup.action;

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.PlatformkitPackage;

/**
 * Action base class for all right-click actions targeting a {@link ConstraintSpace}
 * @author dennis
 *
 */
public abstract class ConstraintSpaceAction extends PlatformKitAction {
	
	protected ConstraintSpace space = null;

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
        super.selectionChanged(action, selection);
        space = (ConstraintSpace) ((IStructuredSelection) selection).getFirstElement();
    }

	/**
	 * @param constraintSets
	 * @return a Command that adds the constraint sets to the constraint space.
	 */
	protected Command createAddConstraintSetCommand(EList<ConstraintSet> constraintSets) {
		Assert.isNotNull(editingDomain);
		Assert.isNotNull(space);
		Assert.isNotNull(constraintSets);
		AddCommand cmd = new AddCommand(
				editingDomain, 
				space, 
				PlatformkitPackage.eINSTANCE.getConstraintSpace_ConstraintSet(), 
				constraintSets);
		return cmd;
	}

	/**
	 * @param constraintSets
	 * @return a Command that removes the constraint sets from the constraint space.
	 */
	protected Command createRemoveConstraintSetCommand(EList<ConstraintSet> constraintSets) {
		Assert.isNotNull(editingDomain);
		Assert.isNotNull(space);
		Assert.isNotNull(constraintSets);
		RemoveCommand cmd = new RemoveCommand(
				editingDomain, 
				space, 
				PlatformkitPackage.eINSTANCE.getConstraintSpace_ConstraintSet(), 
				constraintSets);
		return cmd;
	}

	/**
	 * @param ontologies
	 * @return a Command that adds the ontologies to the constraint space.
	 */
	protected Command createAddOntologyCommand(EList<String> ontologies) {
		Assert.isNotNull(editingDomain);
		Assert.isNotNull(space);
		Assert.isNotNull(ontologies);
		AddCommand cmd = new AddCommand(
				editingDomain, 
				space, 
				PlatformkitPackage.eINSTANCE.getConstraintSpace_Ontology(), 
				ontologies);
		return cmd;
	}
	
	protected void catchCleanup() {
		if (space != null) {
			space.setKnowledgeBase(null);
		}
	}

}
