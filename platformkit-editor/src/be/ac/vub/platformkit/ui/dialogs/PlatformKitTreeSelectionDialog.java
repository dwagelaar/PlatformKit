package be.ac.vub.platformkit.ui.dialogs;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;


/**
 * PlatformKit tree selection dialog 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformKitTreeSelectionDialog extends CheckedTreeSelectionDialog {
	
	protected PlatformKitDialogPart dlgPart = new PlatformKitDialogPart();

	/**
	 * Creates a new PlatformKitTreeSelectionDialog
	 * @param parent
	 * @param labelProvider
	 * @param contentProvider
	 */
	public PlatformKitTreeSelectionDialog(Shell parent, ILabelProvider labelProvider,
			ITreeContentProvider contentProvider) {
		super(parent, labelProvider, contentProvider);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.CheckedTreeSelectionDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		// create the top level composite for the dialog area
		Composite composite = dlgPart.createDialogArea(parent);
		// create the dialog working area
		Control dlgArea = super.createDialogArea(composite);
		dlgPart.setContainedAreaLayoutData(dlgArea);
		return dlgArea;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.CheckedTreeSelectionDialog#createSelectionButtons(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createSelectionButtons(Composite composite) {
		return dlgPart.createEmptyComposite(composite);
	}
	
	/**
	 * Sets the title area text
	 * @param title
	 */
	public void setTitleAreaText(String title) {
		dlgPart.setTitleAreaText(title);
	}

	/**
	 * Sets the title area message
	 * @param message
	 */
	public void setTitleAreaMessage(String message) {
		dlgPart.setTitleAreaMessage(message);
	}

	/**
	 * @return the title area text
	 */
	public String getTitleAreaText() {
		return dlgPart.getTitleAreaText();
	}

	/**
	 * @return the title area message
	 */
	public String getTitleAreaMessage() {
		return dlgPart.getTitleAreaMessage();
	}
	
}
