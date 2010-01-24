package be.ac.vub.platformkit.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckable;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * Provides basic dialog functionality for PlatformKit dialogs.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformKitDialogPart {

	public static final String WIZ_IMAGE = "icons/full/wizban/PlatformKitWizard.png";

	private Label titleAreaLabel;
	private Label titleAreaMessageLabel;
	private String titleAreaText;
	private String titleAreaMessage;
	
	protected Control titleArea;

	/**
	 * Creates a new PlatformKitDialogPart.
	 */
	public PlatformKitDialogPart() {
		super();
	}

	/**
	 * Creates the main dialog area
	 * @param parent
	 * @return The main dialog area
	 */
	public Composite createDialogArea(Composite parent) {
		// create the top level composite for the dialog area
		Composite composite = new Composite(parent, SWT.NONE);
		FormLayout layout = new FormLayout();
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		Dialog.applyDialogFont(composite);
		// add the title area
		titleArea = createTitleArea(composite);
		return composite;
	}
	
	/**
	 * Sets the FormData layout data for the containedArea
	 * @param containedArea
	 */
	public void setContainedAreaLayoutData(Control containedArea) {
		if (titleArea != null) {
			FormData dlgAreaData = new FormData();
			dlgAreaData.top = new FormAttachment(titleArea);
			dlgAreaData.right = new FormAttachment(100, 0);
			dlgAreaData.left = new FormAttachment(0, 0);
			dlgAreaData.bottom = new FormAttachment(100, 0);
			containedArea.setLayoutData(dlgAreaData);
		}
	}
	
	/**
	 * Builds the title area control under parent.
	 * @param parent The parent composite, using a FormLayout.
	 * @return The title area control.
	 */
	protected Control createTitleArea(Composite parent) {
		// Compute and store a font metric
		GC gc = new GC(parent);
		gc.setFont(JFaceResources.getDialogFont());
		FontMetrics fm = gc.getFontMetrics();
		gc.dispose();
		Composite titleArea = new Composite(parent, SWT.NO_FOCUS | SWT.EMBEDDED);
		titleArea.setLayout(new FormLayout());
		FormData titleAreaData = new FormData();
		titleAreaData.top = new FormAttachment(0, 0);
		titleAreaData.left = new FormAttachment(0, 0);
		titleAreaData.right = new FormAttachment(100, 0);
		titleArea.setLayoutData(titleAreaData);
		titleArea.setBackground(JFaceColors.getBannerBackground(parent.getDisplay()));
		int verticalSpacing = Dialog.convertVerticalDLUsToPixels(fm, IDialogConstants.VERTICAL_SPACING);
		int horizontalSpacing = Dialog.convertHorizontalDLUsToPixels(fm, IDialogConstants.HORIZONTAL_SPACING);
		// image
		Label titleImageLabel = new Label(titleArea, SWT.NONE);
        ImageDescriptor img = PlatformkitEditorPlugin.getImageDescriptor(WIZ_IMAGE);
        titleImageLabel.setImage(img.createImage());
		FormData imageData = new FormData();
		imageData.top = new FormAttachment(0, 0);
		imageData.right = new FormAttachment(100, 0);
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
		// build the separator line
		Label titleBarSeparator = new Label(parent, SWT.HORIZONTAL
				| SWT.SEPARATOR);
		FormData titleBarData = new FormData();
		titleBarData.top = new FormAttachment(titleArea);
		titleBarData.left = new FormAttachment(0, 0);
		titleBarData.right = new FormAttachment(100, 0);
		titleBarSeparator.setLayoutData(titleBarData);
        return titleBarSeparator;
	}
	
	/**
	 * Fire this when a tree viewer selection changes. Applies the dialog
	 * selection rules of selecting only leaf nodes in a tree, and only
	 * up to one leaf under every parent node.
	 * @param event
	 */
	public void checkSingleLeafOnly(CheckStateChangedEvent event) {
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

	/**
	 * @param parent
	 * @return An empty composite of size 0.
	 */
	public Composite createEmptyComposite(Composite parent) {
        Composite btnControl = new Composite(parent, SWT.RIGHT);
        GridData btnControlData = new GridData();
        btnControlData.heightHint = 0;
        btnControlData.widthHint = 0;
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
