package be.ac.vub.platformkit.presentation.util;

import java.io.IOException;

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

public class PlatformSpecDialogRunnable extends FileDialogRunnable {

    /**
     * Creates a new PlatformSpecDialogRunnable.
     * @param message The dialog message.
     */
    public PlatformSpecDialogRunnable(String message) {
        super(message);
    }

    public void run() {
        try {
            PlatformSpecDialog dlg = new PlatformSpecDialog(
                    PlatformkitEditorPlugin.INSTANCE.getShell());
            dlg.setTitle(title);
            dlg.setMessage(message);
            dlg.open();
            if (dlg.getReturnCode() == PlatformSpecDialog.OK) {
                if (dlg.isFromFileSelected()) {
                    super.run();
                } else {
                    result = dlg.getResult();
                }
            }
        } catch (IOException e) {
            PlatformkitEditorPlugin.INSTANCE.report(e);
        }
    }
}
