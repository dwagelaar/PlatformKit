package be.ac.vub.platformkit.java.popup.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * Wraps the displaying of the platform API dialog in a Runnable 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformAPIDialogRunnable implements Runnable {
    private String title = "Select Resources";
    private String message;
    private Object[] result = null;
    private ILabelProvider labelProv;

    /**
     * Creates a new PlatformAPIDialogRunnable.
     * @param message The dialog message.
     */
    public PlatformAPIDialogRunnable(String message) {
        super();
        setMessage(message);
    }

    public void run() {
        try {
            ListSelectionDialog dlg = createPlatformAPIDialog(
                    PlatformkitEditorPlugin.INSTANCE.getShell());
            dlg.setTitle(title);
            dlg.setMessage(message);
            dlg.open();
            if (dlg.getReturnCode() == Window.OK) {
                setResult(dlg.getResult());
            }
        } catch (IOException e) {
            PlatformkitJavaPlugin.getPlugin().report(e);
        }
    }

    protected ListSelectionDialog createPlatformAPIDialog(Shell parentShell) throws IOException {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        if (registry == null) {
            throw new IOException("Eclipse platform extension registry not found. Built-in platform specification ontologies do not work outside Eclipse.");
        }
        final List<URI> content = new ArrayList<URI>();
        final Map<URI, String> labels = new HashMap<URI, String>();
        IExtensionPoint point = registry.getExtensionPoint(PlatformkitJavaPlugin.PLATFORMAPI_EXT_POINT);
        IExtension[] extensions = point.getExtensions();
        for (int i = 0 ; i < extensions.length ; i++) {
            IConfigurationElement[] elements = extensions[i].getConfigurationElements();
            for (int j = 0 ; j < elements.length ; j++) {
                try {
                	URI emf_uri = URI.createURI(elements[j].getAttribute("emf_uri"));
                	content.add(emf_uri);
                	labels.put(emf_uri, elements[j].getAttribute("name"));
                } catch (IllegalArgumentException e) {
                    throw new IOException(e.getLocalizedMessage());
                }
            }
        }
        labelProv = new LabelProvider() {
            public String getText(Object element) {
                if (labels.containsKey(element)) {
                    return labels.get(element).toString();
                } else {
                    return super.getText(element);
                }
            }
        };
        IStructuredContentProvider contentProv = ArrayContentProvider.getInstance();
        ListSelectionDialog dlg = new ListSelectionDialog(parentShell, content, contentProv, labelProv, null);
        return dlg;
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
	 * @return the result
	 */
	public Object[] getResult() {
		return result;
	}

	/**
	 * @param result the result to set
	 */
	protected void setResult(Object[] result) {
		this.result = result;
	}
	
	public String getLabelFor(Object value) {
		return labelProv.getText(value);
	}
}
