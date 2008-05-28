package be.ac.vub.platformkit.presentation.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;

import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

public class PlatformSpecDialog extends ListDialog {

    protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);
    
    private boolean builtinSelected = true;
    
    public PlatformSpecDialog(Shell parentShell) throws IOException {
        super(parentShell);
        setContentProvider(new ArrayContentProvider());
        initPlatformSpecList();
    }
    
    private void initPlatformSpecList() throws IOException {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        if (registry == null) {
            logger.warning("Eclipse platform extension registry not found. Built-in platform specification ontologies do not work outside Eclipse.");
            return;
        }
        final List<InputStream> content = new ArrayList<InputStream>();
        final Map<InputStream, String> labels = new HashMap<InputStream, String>();
        IExtensionPoint point = registry.getExtensionPoint(PlatformkitEditorPlugin.PLATFORMSPEC_EXT_POINT);
        IExtension[] extensions = point.getExtensions();
        for (int i = 0 ; i < extensions.length ; i++) {
            IConfigurationElement[] elements = extensions[i].getConfigurationElements();
            for (int j = 0 ; j < elements.length ; j++) {
                try {
                    INamedOntologyProvider provider = (INamedOntologyProvider)
                    elements[j].createExecutableExtension("provider");
                    String[] names = provider.getOntologyNames();
                    InputStream[] streams = provider.getOntologies();
                    for (int k = 0; k < streams.length; k++) {
                        content.add(streams[k]);
                        labels.put(streams[k], names[k]);
                    }
                } catch (CoreException e) {
                    throw new IOException(e.getLocalizedMessage());
                }
            }
        }
        setInput(content);
        setLabelProvider(new LabelProvider() {
            public String getText(Object element) {
                if (labels.containsKey(element)) {
                    return labels.get(element).toString();
                } else {
                    return super.getText(element);
                }
            }
        });
        if (content.size() > 0) {
            setInitialSelections(new Object[] {content.get(0)});
        }
    }

    protected Control createDialogArea(Composite parent) {
        Composite panel = (Composite) super.createDialogArea(parent);
        
        Button builtinBtn = new Button(panel, SWT.RADIO);
        builtinBtn.setText("Built-in platform specification");
        builtinBtn.setSelection(true);
        
        Button fromFileBtn = new Button(panel, SWT.RADIO);
        fromFileBtn.setText("Platform specification from file");
        
        builtinBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                getTableViewer().getControl().setEnabled(true);
                builtinSelected = true;
            }
        });
        
        fromFileBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                getTableViewer().getControl().setEnabled(false);
                builtinSelected = false;
            }
        });
        
        return panel;
    }
    
    /**
     * @return True if built-in platform specification is selected, false otherwise.
     */
    public boolean isBuiltinSelected() {
        return builtinSelected;
    }

    /**
     * @return True if platform specification from file is selected, false otherwise.
     */
    public boolean isFromFileSelected() {
        return !builtinSelected;
    }

}
