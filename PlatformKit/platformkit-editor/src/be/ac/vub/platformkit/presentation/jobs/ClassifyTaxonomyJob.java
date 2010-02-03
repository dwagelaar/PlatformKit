package be.ac.vub.platformkit.presentation.jobs;

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

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.editor.preferences.PreferenceInitializer;
import be.ac.vub.platformkit.kb.IOntClass;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.util.OntException;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * Operation for pre-classifying the taxonomy of ontology classes for a
 * given PlatformKit constraint space model.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class ClassifyTaxonomyJob extends ConstraintSpaceJob {

	private static final int OUTPUTSIZE = 512*1024; // 512 KB

	/**
	 * Creates a new {@link ClassifyTaxonomyJob}.
	 */
	public ClassifyTaxonomyJob() {
		super("Classifying Taxonomy");
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.jobs.ProgressMonitorJob#runAction(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void runAction(IProgressMonitor monitor) throws Exception {
		beginTask(monitor, getName(), 9);
		//
		// 1
		//
		subTask(monitor, "Loading source ontologies...");
		// Don't use existing knowledgebase, since it may be pre-classified
		IOntologies ont = PreferenceInitializer.getPreferredOntologyFactory().createIOntologies();
		ConstraintSpace space = getSpace();
		space.setKnowledgeBase(ont);
		space.init(false);
		worked(monitor, "Loaded source ontologies");
		//
		// 2
		//
		subTask(monitor, "Creating intersection and union classes...");
		//create intersection set before reasoning
		ConstraintSet is = space.getIntersectionSet();
		is.getIntersection();
		worked(monitor, "Created intersection and union classes");
		//
		// 3
		//
		subTask(monitor, "Attaching DL reasoner...");
		attachDLReasoner(monitor, ont);
		worked(monitor, "Attached DL reasoner");
		//
		// 4
		//
		subTask(monitor, "Checking consistency...");
		ont.checkConsistency();
		worked(monitor, "Checked consistency");
		//
		// 5
		//
		subTask(monitor, "Building class hierarchy map...");
		buildHierarchyMap(monitor, ont);
		worked(monitor, "Built class hierarchy map");
		//
		// 6
		//
		subTask(monitor, "Detaching reasoner...");
		ont.detachReasoner();
		worked(monitor, "Detached reasoner");
		//
		// 7
		//
		subTask(monitor, "Pruning class hierarchy map...");
		pruneHierarchyMap(monitor, ont);
		worked(monitor, "Pruned class hierarchy map");
		//
		// 8
		//
		subTask(monitor, "Updating asserted class hierarchy...");
		updateHierarchy(monitor, ont);
		worked(monitor, "Updated asserted class hierarchy");
		//
		// 9
		//
		subTask(monitor, "Writing target ontology...");
		writeOntology(ont, space.eResource().getURI());
		worked(monitor, "Written target ontology");
	}

	/**
	 * Operation for building a hierarchy map for an ontology.
	 * @param monitor
	 * @param ont
	 */
	protected void buildHierarchyMap(IProgressMonitor monitor, IOntologies ont) {
		List<IOntClass> namedClasses = ont.getLocalNamedClasses();
		for (int i = 0; i < namedClasses.size(); i++) {
			try {
				IOntClass c = namedClasses.get(i);
				ont.buildHierarchyMap(c);
			} catch (OntException nfe) {
				PlatformkitEditorPlugin.INSTANCE.log(
						nfe.getMessage(), IStatus.WARNING, nfe);
			} finally {
				checkCanceled(monitor);
			}
		}
	}

	/**
	 * Operation for pruning the generated hierarchy map for an ontology.
	 * @param monitor
	 * @param ont
	 */
	protected void pruneHierarchyMap(IProgressMonitor monitor, IOntologies ont) {
		List<IOntClass> namedClasses = ont.getLocalNamedClasses();
		for (int i = 0; i < namedClasses.size(); i++) {
			try {
				IOntClass c = namedClasses.get(i);
				ont.pruneHierarchyMap(c);
			} catch (OntException nfe) {
				PlatformkitEditorPlugin.INSTANCE.log(
						nfe.getMessage(), IStatus.WARNING, nfe);
			} finally {
				checkCanceled(monitor);
			}
		}
	}

	/**
	 * Operation for updating the asserted class hierarchy for an ontology.
	 * @param monitor
	 * @param ont
	 */
	protected void updateHierarchy(IProgressMonitor monitor, IOntologies ont) {
		List<IOntClass> namedClasses = ont.getLocalNamedClasses();
		for (int i = 0; i < namedClasses.size(); i++) {
			try {
				IOntClass c = namedClasses.get(i);
				ont.updateHierarchy(c);
			} catch (OntException nfe) {
				PlatformkitEditorPlugin.INSTANCE.log(
						nfe.getMessage(), IStatus.WARNING, nfe);
			} finally {
				checkCanceled(monitor);
			}
		}
	}

	/**
	  * Writes the ontology for the given resource URI.
	  * @param ont
	  * @param uri
	  * @throws CoreException
	  * @throws IOException
	  */
	 protected void writeOntology(IOntologies ont, URI uri)
	 throws CoreException, IOException {
		 IPath platformkitPath = new Path(uri.toPlatformString(true));
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
