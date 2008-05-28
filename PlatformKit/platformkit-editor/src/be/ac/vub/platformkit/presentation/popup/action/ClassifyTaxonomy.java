package be.ac.vub.platformkit.presentation.popup.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.preference.IPreferenceStore;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.editor.preferences.PreferenceConstants;
import be.ac.vub.platformkit.editor.preferences.PreferenceInitializer;
import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.util.OntException;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.presentation.util.PlatformKitActionUtil;

/**
 * Pre-classifies the taxonomy of ontology classes for a
 * given CDD configuration. Note that this needs to be redone
 * whenever the ontologies and/or the CDD configurations changes.
 * The output is written to &lt;CDD config basename&gt;.inferred.owl.
 * Requires a DIG reasoner at port 8080.
 * @author dennis
 *
 */
public class ClassifyTaxonomy extends ConstraintSpaceAction {

	private static final int OUTPUTSIZE = 512*1024; // 512 KB
	
    private class BuildHierarchyMap extends PlatformKitAction implements Runnable {
        private IOntologies ont;
        
        /**
         * Constructor for sub-action.
         */
        public BuildHierarchyMap(IOntologies ont) {
            super();
            Assert.isNotNull(ont);
            this.ont = ont;
        }
        
        /**
         * @see Runnable#run()
         */
        public void run() {
            run(null);
        }
        
        /**
         * Invoked when the action is executed.
         * @param monitor
         * @throws Exception
         */
        protected void runAction(IProgressMonitor monitor)
        throws Exception {
            List<IOntClass> namedClasses = ont.getLocalNamedClasses();
            monitor.beginTask("Building Class Hierarchy Map", namedClasses.size());
            for (int i = 0; i < namedClasses.size(); i++) {
                try {
                    IOntClass c = namedClasses.get(i);
                    monitor.subTask("Building hierarchy map for " + c);
                    ont.buildHierarchyMap(c);
                } catch (OntException nfe) {
                	PlatformkitEditorPlugin.INSTANCE.log(
                            nfe.getMessage(), IStatus.WARNING, nfe);
                } finally {
                    worked(monitor);
                }
            }
        }
    }
    
    private class UpdateHierarchy extends PlatformKitAction implements Runnable {
        private IOntologies ont;
        
        /**
         * Constructor for sub-action.
         */
        public UpdateHierarchy(IOntologies ont) {
            super();
            Assert.isNotNull(ont);
            this.ont = ont;
        }
        
        /**
         * @see Runnable#run()
         */
        public void run() {
            run(null);
        }
        
        /**
         * Invoked when the action is executed.
         * @param monitor
         * @throws Exception
         */
        protected void runAction(IProgressMonitor monitor)
        throws Exception {
            List<IOntClass> namedClasses = ont.getLocalNamedClasses();
            monitor.beginTask("Updating Asserted Class Hierarchy", namedClasses.size());
            for (int i = 0; i < namedClasses.size(); i++) {
                try {
                    IOntClass c = namedClasses.get(i);
                    monitor.subTask("Updating hierarchy for " + c);
                    ont.updateHierarchy(c);
                } catch (OntException nfe) {
                	PlatformkitEditorPlugin.INSTANCE.log(
                            nfe.getMessage(), IStatus.WARNING, nfe);
                } finally {
                    worked(monitor);
                }
            }
        }
    }
    
    private class PruneHierarchyMap extends PlatformKitAction implements Runnable {
        private IOntologies ont;
        
        /**
         * Constructor for sub-action.
         */
        public PruneHierarchyMap(IOntologies ont) {
            super();
            Assert.isNotNull(ont);
            this.ont = ont;
        }
        
        /**
         * @see Runnable#run()
         */
        public void run() {
            run(null);
        }
        
        /**
         * Invoked when the action is executed.
         * @param monitor
         * @throws Exception
         */
        protected void runAction(IProgressMonitor monitor)
        throws Exception {
            List<IOntClass> namedClasses = ont.getLocalNamedClasses();
            monitor.beginTask("Pruning Class Hierarchy Map", namedClasses.size());
            for (int i = 0; i < namedClasses.size(); i++) {
                try {
                    IOntClass c = namedClasses.get(i);
                    monitor.subTask("Pruning hierarchy map for " + c);
                    ont.pruneHierarchyMap(c);
                } catch (OntException nfe) {
                	PlatformkitEditorPlugin.INSTANCE.log(
                            nfe.getMessage(), IStatus.WARNING, nfe);
                } finally {
                    worked(monitor);
                }
            }
        }
    }
    
	/**
	 * Constructor for Action1.
	 */
	public ClassifyTaxonomy() {
		super();
	}
    
    /**
     * Invoked when the action is executed.
     * @param monitor
     * @throws Exception
     */
    protected void runAction(IProgressMonitor monitor)
    throws Exception {
        monitor.beginTask("Classifying Taxonomy", 9);
        monitor.subTask("Loading source ontologies...");
        // Don't use existing knowledgebase, since it may be pre-classified
        IOntologies ont = PreferenceInitializer.getPreferredOntologyFactory().createIOntologies();
        space.setKnowledgeBase(ont);
        space.init(false);
        worked(monitor);
        monitor.subTask("Creating intersection and union classes...");
        //create intersection set before reasoning
        ConstraintSet is = space.getIntersectionSet();
        is.getIntersection();
        worked(monitor);
        monitor.subTask("Attaching DL reasoner...");
		IPreferenceStore store = PlatformkitEditorPlugin.getPlugin()
				.getPreferenceStore();
		String reasoner = store.getString(PreferenceConstants.P_REASONER);
		if (PreferenceConstants.P_DIG.equals(reasoner)) {
			String url = store.getString(PreferenceConstants.P_DIG_URL);
			ont.setReasonerUrl(url);
			ont.attachDIGReasoner();
		} else {
	        ont.attachPelletReasoner();
		}
        worked(monitor);
        monitor.subTask("Checking consistency...");
        ont.checkConsistency();
        worked(monitor);
        monitor.subTask("Building class hierarchy map...");
        BuildHierarchyMap subTaskBuild = new BuildHierarchyMap(ont);
        PlatformkitEditorPlugin.getPlugin().getWorkbench().getDisplay().syncExec(subTaskBuild);
        worked(monitor, subTaskBuild);
        monitor.subTask("Detaching reasoner...");
        ont.detachReasoner();
        worked(monitor);
        monitor.subTask("Pruning class hierarchy map...");
        PruneHierarchyMap subTaskPrune = new PruneHierarchyMap(ont);
        PlatformkitEditorPlugin.getPlugin().getWorkbench().getDisplay().syncExec(subTaskPrune);
        worked(monitor, subTaskPrune);
        monitor.subTask("Updating asserted class hierarchy...");
        UpdateHierarchy subTaskAugment = new UpdateHierarchy(ont);
        PlatformkitEditorPlugin.getPlugin().getWorkbench().getDisplay().syncExec(subTaskAugment);
        worked(monitor, subTaskAugment);
        monitor.subTask("Writing target ontology...");
        writeOntology(ont, space.eResource().getURI());
        worked(monitor);
    }
    
    /**
     * Writes the ontology for the given resource URI.
     * @param ont
     * @param uri
     * @throws CoreException
     * @throws IOException
     */
    private void writeOntology(IOntologies ont, URI uri)
    throws CoreException, IOException {
        IPath platformkitPath = new Path(PlatformKitActionUtil.toPlatformResourcePath(uri));
        Assert.isNotNull(platformkitPath);
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(platformkitPath);
        Assert.isNotNull(file);
        Path path = new Path(
                file.getFullPath().removeFileExtension().
                addFileExtension("inferred").
                addFileExtension("owl").lastSegment());
        IContainer cont = file.getParent();
        IFile dest = cont.getFile(path);
    	logger.info("Writing ontology to " + dest.getLocation());
        ByteArrayOutputStream output = new ByteArrayOutputStream(OUTPUTSIZE);
        ont.saveOntology(output);
        if (dest.exists()) {
            dest.setContents(new ByteArrayInputStream(output.toByteArray()), 
                    true, true, null);
        } else {
            dest.create(new ByteArrayInputStream(output.toByteArray()), 
                    true, null);
        }
        output.close();
    	logger.info("Ontology written to " + dest.getLocation());
    }
}
