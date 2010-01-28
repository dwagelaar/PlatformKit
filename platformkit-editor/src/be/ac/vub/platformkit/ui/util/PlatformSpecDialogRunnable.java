package be.ac.vub.platformkit.ui.util;

import java.io.IOException;

import org.eclipse.jface.window.Window;

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
    	    setReturnCode(dlg.getReturnCode());
            if (dlg.getReturnCode() == Window.OK) {
                if (dlg.isFromFileSelected()) {
                    super.run();
                } else {
                    setSelection(dlg.getResult());
                }
            }
        } catch (IOException e) {
            PlatformkitEditorPlugin.report(e);
        }
    }

}
