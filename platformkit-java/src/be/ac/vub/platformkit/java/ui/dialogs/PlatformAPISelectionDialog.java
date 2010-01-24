package be.ac.vub.platformkit.java.ui.dialogs;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import be.ac.vub.platformkit.ui.dialogs.PlatformKitTreeSelectionDialog;

/**
 * Platform API selection dialog 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformAPISelectionDialog extends PlatformKitTreeSelectionDialog {
	
	/**
	 * Creates a new PlatformAPISelectionDialog
	 * @param parent
	 * @param labelProvider
	 * @param contentProvider
	 */
	public PlatformAPISelectionDialog(Shell parent, ILabelProvider labelProvider,
			ITreeContentProvider contentProvider) {
		super(parent, labelProvider, contentProvider);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.CheckedTreeSelectionDialog#createTreeViewer(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected CheckboxTreeViewer createTreeViewer(Composite parent) {
		CheckboxTreeViewer treeViewer = super.createTreeViewer(parent);
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				dlgPart.checkSingleLeafOnly(event);
			}
		});
		return treeViewer;
	}

}
