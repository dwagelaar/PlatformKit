package be.ac.vub.platformkit.ui.util;

import java.io.IOException;

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.ui.dialogs.PlatformSpecDialog;

public class PlatformSpecDialogRunnable extends FileDialogRunnable {

    /**
     * Creates a new PlatformSpecDialogRunnable.
     */
    public PlatformSpecDialogRunnable() {
        super();
	    setTitle("Load platform specification");
	    setMessage("Select a built-in platform specification, or \nload a platform specification from a file");
	    setInstruction("Select a platform specification:");
    }

    public void run() {
        try {
            PlatformSpecDialog dlg = new PlatformSpecDialog(
                    PlatformkitEditorPlugin.INSTANCE.getShell());
            dlg.setTitle("PlatformKit");
            dlg.setTitleAreaText(getTitle());
            dlg.setTitleAreaMessage(getMessage());
            dlg.setMessage(getInstruction());
            dlg.open();
            if (dlg.getReturnCode() == PlatformSpecDialog.OK) {
                if (dlg.isFromFileSelected()) {
                    super.run();
                } else {
                    result = dlg.getResult();
                }
            }
        } catch (IOException e) {
            PlatformkitEditorPlugin.report(e);
        }
    }

}
