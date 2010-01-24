package be.ac.vub.platformkit.ui.util;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.ui.dialogs.PlatformKitTreeSelectionDialog;


public class FileDialogRunnable implements Runnable {

    private String title = "Select Resources";
    private String message = "Select Resources";
    private String instruction;

    protected Object[] result = null;
    private ViewerFilter filter = null;

    /**
     * Creates a new FileDialogRunnable.
     * @param message The dialog message.
     */
    public FileDialogRunnable() {
    	super();
    }
    
    /**
     * @return Returns the file objects.
     */
    public Object[] getFiles() {
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        PlatformKitTreeSelectionDialog dlg = new PlatformKitTreeSelectionDialog(
        		PlatformkitEditorPlugin.INSTANCE.getShell(),
                new WorkbenchLabelProvider(),
                new WorkbenchContentProvider());
        dlg.setInput(ResourcesPlugin.getWorkspace().getRoot());
        dlg.setContainerMode(true);
        if (filter != null) {
            dlg.addFilter(filter);
        }
        dlg.setTitle("PlatformKit");
        dlg.setTitleAreaText(getTitle());
        dlg.setTitleAreaMessage(getMessage());
        dlg.setMessage(getInstruction());
        dlg.open();
        if (dlg.getResult() != null) {
            result = getFiles(dlg.getResult());
        }
    }

    /**
     * @param filter The filter to set.
     */
    public void setFilter(ViewerFilter filter) {
        this.filter = filter;
    }
    
    /**
     * @param list
     * @return The file objects in the list.
     */
    private Object[] getFiles(Object[] list) {
        ArrayList<Object> files = new ArrayList<Object>();
        for (int i = 0; i < list.length; i++) {
            if (list[i] instanceof IFile) {
                files.add(list[i]);
            }
        }
        return files.toArray();
    }

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return the instruction
	 */
	public String getInstruction() {
		return instruction;
	}

	/**
	 * @param instruction the instruction to set
	 */
	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}
}
