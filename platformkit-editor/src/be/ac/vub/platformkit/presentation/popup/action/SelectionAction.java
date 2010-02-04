package be.ac.vub.platformkit.presentation.popup.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionDelegate;

/**
 * General superclass for popup actions with a selection.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public abstract class SelectionAction implements IActionDelegate {

	protected ISelection selection;
	protected IAction action;

	/**
	 * Creates a new {@link SelectionAction}.
	 */
	public SelectionAction() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
		this.action = action;
	}

}