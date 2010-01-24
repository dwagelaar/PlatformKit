package be.ac.vub.platformkit.java.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckable;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;

import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;

/**
 * Platform API selection dialog 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformAPISelectionDialog extends CheckedTreeSelectionDialog {
	
	public static final String WIZ_IMAGE = "icons/full/wizban/PlatformKitWizard.png";

	private Label titleAreaLabel;
	private Label titleAreaMessageLabel;
	private String titleAreaText;
	private String titleAreaMessage;

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
	 * @see org.eclipse.ui.dialogs.CheckedTreeSelectionDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		// create the top level composite for the dialog area
		Composite composite = new Composite(parent, SWT.NONE);
		FormLayout layout = new FormLayout();
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);
		// add the title area
		Control titleArea = createTitleArea(composite);
		// build the separator line
		Label titleBarSeparator = new Label(composite, SWT.HORIZONTAL
				| SWT.SEPARATOR);
		FormData titleBarData = new FormData();
		titleBarData.top = new FormAttachment(titleArea);
		titleBarData.left = new FormAttachment(0, 0);
		titleBarData.right = new FormAttachment(100, 0);
		titleBarSeparator.setLayoutData(titleBarData);
		// create the dialog working area
		Control dlgArea = super.createDialogArea(composite);
		FormData dlgAreaData = new FormData();
		dlgAreaData.top = new FormAttachment(titleBarSeparator);
		dlgAreaData.right = new FormAttachment(100, 0);
		dlgAreaData.left = new FormAttachment(0, 0);
		dlgAreaData.bottom = new FormAttachment(100, 0);
		dlgArea.setLayoutData(dlgAreaData);
		return dlgArea;
	}
	
	/**
	 * Builds the title area control under parent.
	 * @param parent The parent composite, using a FormLayout.
	 * @return The title area control.
	 */
	protected Control createTitleArea(Composite parent) {
		Composite titleArea = new Composite(parent, SWT.NO_FOCUS | SWT.EMBEDDED);
		titleArea.setLayout(new FormLayout());
		FormData titleAreaData = new FormData();
		titleAreaData.top = new FormAttachment(0, 0);
		titleAreaData.left = new FormAttachment(0, 0);
		titleAreaData.right = new FormAttachment(100, 0);
		titleArea.setLayoutData(titleAreaData);
		titleArea.setBackground(JFaceColors.getBannerBackground(parent.getDisplay()));
		int verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		int horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		// image
		Label titleImageLabel = new Label(titleArea, SWT.NONE);
        ImageDescriptor img = PlatformkitJavaPlugin.getPlugin().getImageDescriptor(WIZ_IMAGE);
        titleImageLabel.setImage(img.createImage());
		FormData imageData = new FormData();
		imageData.top = new FormAttachment(0, 0);
		imageData.right = new FormAttachment(100, 0); // horizontalSpacing
		titleImageLabel.setLayoutData(imageData);
		// title
		titleAreaLabel = new Label(titleArea, SWT.LEFT);
		titleAreaLabel.setFont(JFaceResources.getBannerFont());
		titleAreaLabel.setText(getTitleAreaText());
		FormData titleData = new FormData();
		titleData.top = new FormAttachment(0, verticalSpacing);
		titleData.right = new FormAttachment(titleImageLabel);
		titleData.left = new FormAttachment(0, horizontalSpacing);
		titleAreaLabel.setLayoutData(titleData);
		// message
		titleAreaMessageLabel = new Label(titleArea, SWT.LEFT);
		titleAreaMessageLabel.setText(getTitleAreaMessage());
		titleAreaMessageLabel.setFont(JFaceResources.getDialogFont());
		FormData messageData = new FormData();
		messageData.top = new FormAttachment(titleAreaLabel);
		messageData.right = new FormAttachment(titleImageLabel);
		messageData.left = new FormAttachment(0, horizontalSpacing * 2);
		titleAreaMessageLabel.setLayoutData(messageData);
        return titleArea;
	}
	
	/**
	 * Fires when the selection of API models changes. Applies the dialog
	 * selection rules.
	 * @param event
	 */
	protected void checkSelectionStateChanged(CheckStateChangedEvent event) {
		final Object element = event.getElement();
		if (element instanceof TreeNode) {
			final TreeNode node = (TreeNode) element;
			final ICheckable checkable = event.getCheckable();
			if (checkable.getChecked(node)) {
				if (node.hasChildren()) {
					// no direct selection allowed
					checkable.setChecked(node, false);
				} else {
					// only one sibling can be selected
					final TreeNode[] siblings = node.getParent().getChildren();
					for (TreeNode sibling : siblings) {
						if ((sibling != node) && (checkable.getChecked(sibling))) {
							checkable.setChecked(sibling, false);
						}
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.CheckedTreeSelectionDialog#createTreeViewer(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected CheckboxTreeViewer createTreeViewer(Composite parent) {
		CheckboxTreeViewer treeViewer = super.createTreeViewer(parent);
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				checkSelectionStateChanged(event);
			}
		});
		return treeViewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.CheckedTreeSelectionDialog#createSelectionButtons(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createSelectionButtons(Composite composite) {
        Composite btnControl = new Composite(composite, SWT.RIGHT);
        btnControl.setBackground(JFaceColors.getBannerForeground(composite.getDisplay()));
//        buttonComposite.setLayout(new FormLayout());
        GridData btnControlData = new GridData();
        btnControlData.heightHint = 0;
        btnControl.setLayoutData(btnControlData);
        return btnControl;
	}
	
	/**
	 * Sets the title area text
	 * @param title
	 */
	public void setTitleAreaText(String title) {
		this.titleAreaText = title;
	}

	/**
	 * Sets the title area message
	 * @param message
	 */
	public void setTitleAreaMessage(String message) {
		this.titleAreaMessage = message;
	}

	/**
	 * @return the title area text
	 */
	public String getTitleAreaText() {
		return titleAreaText;
	}

	/**
	 * @return the title area message
	 */
	public String getTitleAreaMessage() {
		return titleAreaMessage;
	}
	
}
