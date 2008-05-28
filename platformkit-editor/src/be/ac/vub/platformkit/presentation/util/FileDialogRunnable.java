package be.ac.vub.platformkit.presentation.util;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;


public class FileDialogRunnable implements Runnable {
    protected String title = "Select Resources";
    protected String message;
    protected Object[] result = null;
    private ViewerFilter filter = null;

    /**
     * Creates a new FileDialogRunnable.
     * @param message The dialog message.
     */
    public FileDialogRunnable(String message) {
        setMessage(message);
    }
    
    /**
     * @return Returns the file objects.
     */
    public Object[] getFiles() {
        return result;
    }
    
    /**
     * @param message The message to set.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    public void run() {
        CheckedTreeSelectionDialog dlg = new CheckedTreeSelectionDialog(
        		PlatformkitEditorPlugin.INSTANCE.getShell(),
                new WorkbenchLabelProvider(),
                new WorkbenchContentProvider());
        dlg.setInput(ResourcesPlugin.getWorkspace().getRoot());
        dlg.setContainerMode(true);
        if (filter != null) {
            dlg.addFilter(filter);
        }
        dlg.setTitle(title);
        dlg.setMessage(message);
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
}
