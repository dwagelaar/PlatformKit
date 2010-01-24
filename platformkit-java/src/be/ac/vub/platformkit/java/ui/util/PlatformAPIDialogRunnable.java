package be.ac.vub.platformkit.java.ui.util;

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
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.jface.viewers.TreeNodeContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;
import be.ac.vub.platformkit.java.ui.dialogs.PlatformAPISelectionDialog;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * Wraps the displaying of the platform API dialog in a Runnable 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformAPIDialogRunnable implements Runnable {
    private String title;
    private String message;
    private String instruction;
    private Object[] result = null;
    private ILabelProvider labelProv;

    /**
     * Creates a new PlatformAPIDialogRunnable.
     */
    public PlatformAPIDialogRunnable() {
        super();
    }

    public void run() {
        try {
            PlatformAPISelectionDialog dlg = createPlatformAPIDialog(
                    PlatformkitEditorPlugin.INSTANCE.getShell());
            dlg.setTitle("PlatformKit");
            dlg.setTitleAreaText(getTitle());
            dlg.setTitleAreaMessage(getMessage());
            dlg.setMessage(getInstruction());
            dlg.open();
            if (dlg.getReturnCode() == Window.OK) {
                setResult(filterResults(dlg.getResult()));
            }
        } catch (IOException e) {
            PlatformkitJavaPlugin.getPlugin().report(e);
        }
    }
    
    /**
     * @param results
     * @return The filtered dialog results
     */
    private Object[] filterResults(Object[] results) {
    	List<Object> filteredResults = new ArrayList<Object>();
    	for (Object res : results) {
    		if (res instanceof TreeNode) {
    			Object value = ((TreeNode)res).getValue();
    			if (value instanceof URI) {
    				filteredResults.add(value);
    			}
    		} else if (res instanceof URI) {
    			filteredResults.add(res);
    		}
    	}
    	return filteredResults.toArray();
    }

    protected PlatformAPISelectionDialog createPlatformAPIDialog(Shell parentShell) throws IOException {
        final IExtensionRegistry registry = Platform.getExtensionRegistry();
        if (registry == null) {
            throw new IOException("Eclipse platform extension registry not found. Built-in platform specification ontologies do not work outside Eclipse.");
        }
        final List<TreeNode> content = new ArrayList<TreeNode>();
        final Map<Object, String> labels = new HashMap<Object, String>();
        final Map<String, TreeNode> categories = new HashMap<String, TreeNode>();
        final Map<String, List<TreeNode>> categoryChildren = new HashMap<String, List<TreeNode>>();
        final IExtensionPoint point = registry.getExtensionPoint(PlatformkitJavaPlugin.PLATFORMAPI_EXT_POINT);
        final IExtension[] extensions = point.getExtensions();
        for (int i = 0 ; i < extensions.length ; i++) {
            IConfigurationElement[] elements = extensions[i].getConfigurationElements();
            for (int j = 0 ; j < elements.length ; j++) {
                try {
                	URI emf_uri = URI.createURI(elements[j].getAttribute("emf_uri"));
                	String category = elements[j].getAttribute("category");
                	TreeNode node = new TreeNode(emf_uri);
                	if (!categories.containsKey(category)) {
                		TreeNode catNode = new TreeNode(category);
                		content.add(catNode);
                		categories.put(category, catNode);
                		labels.put(catNode, category);
                		categoryChildren.put(category, new ArrayList<TreeNode>());
                	}
            		node.setParent(categories.get(category));
                	categoryChildren.get(category).add(node);
                	labels.put(emf_uri, elements[j].getAttribute("name"));
                	labels.put(node, elements[j].getAttribute("name"));
                } catch (IllegalArgumentException e) {
                    throw new IOException(e.getLocalizedMessage());
                }
            }
        }
        for (String category : categories.keySet()) {
        	TreeNode catNode = categories.get(category);
        	List<TreeNode> children = categoryChildren.get(category);
        	catNode.setChildren(children.toArray(new TreeNode[children.size()]));
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
        ITreeContentProvider contentProv = new TreeNodeContentProvider();
        PlatformAPISelectionDialog dlg = new PlatformAPISelectionDialog(parentShell, labelProv, contentProv);
        dlg.setInput(content.toArray(new TreeNode[content.size()]));
        dlg.setContainerMode(true);
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
