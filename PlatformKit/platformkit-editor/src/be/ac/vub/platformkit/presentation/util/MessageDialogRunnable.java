package be.ac.vub.platformkit.presentation.util;

import org.eclipse.jface.dialogs.MessageDialog;

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

public class MessageDialogRunnable implements Runnable {
    private String title = null;
    private String message = null;

    /**
     * Creates a new ConstraintListsDialogRunnable.
     * @param title The dialog title.
     * @param message The dialog message.
     */
    public MessageDialogRunnable(String title, String message) {
        setTitle(title);
        setMessage(message);
    }

    public void run() {
        MessageDialog.openInformation(
        		PlatformkitEditorPlugin.INSTANCE.getShell(), 
                getTitle(), getMessage());
    }

    /**
     * @param title The title to set.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return Returns the title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param message The message to set.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return Returns the message.
     */
    public String getMessage() {
        return message;
    }
}
