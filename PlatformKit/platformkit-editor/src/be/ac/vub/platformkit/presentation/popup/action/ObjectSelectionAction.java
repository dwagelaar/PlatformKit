package be.ac.vub.platformkit.presentation.popup.action;

import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.domain.IEditingDomainProvider;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * General superclass for popup actions with an object selection.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 *
 */
public abstract class ObjectSelectionAction extends SelectionAction implements
		IObjectActionDelegate {

	protected EditingDomain editingDomain;
	protected IWorkbenchPart part;

	/**
	 * Creates a new {@link ObjectSelectionAction}.
	 */
	public ObjectSelectionAction() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		if (targetPart instanceof IEditingDomainProvider) {
			this.editingDomain = ((IEditingDomainProvider) targetPart).getEditingDomain();
		}
		this.part = targetPart;
	}

}
