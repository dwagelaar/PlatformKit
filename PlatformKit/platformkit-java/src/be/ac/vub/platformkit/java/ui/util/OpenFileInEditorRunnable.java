package be.ac.vub.platformkit.java.ui.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;

/**
 * Wraps the opening of a file in its registered editor in a Runnable.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class OpenFileInEditorRunnable implements Runnable {
	
	private IFile file;

	/**
	 * Creates a new {@link OpenFileInEditorRunnable} for file.
	 * @param file
	 */
	public OpenFileInEditorRunnable(IFile file) {
		super();
		setFile(file);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			IFile file = getFile();
			//
			// get default editor descriptor
			//
			IEditorRegistry editorRegistry = PlatformUI.getWorkbench()
					.getEditorRegistry();
			IEditorDescriptor defaultEditorDescriptor = editorRegistry
					.getDefaultEditor(file.getName(), file.getContentDescription().getContentType());
			if (defaultEditorDescriptor == null) {
				defaultEditorDescriptor = editorRegistry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
			}
			//
			// Open new file in editor
			//
			IWorkbenchWindow dw = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			Assert.isNotNull(dw);
			FileEditorInput fileEditorInput = new FileEditorInput(file);
			IWorkbenchPage page = dw.getActivePage();
			if (page != null)
				page.openEditor(fileEditorInput, defaultEditorDescriptor
						.getId());
		} catch (Exception e) {
			PlatformkitJavaPlugin.getPlugin().report(e);
		}
	}

	/**
	 * @return the file
	 */
	public IFile getFile() {
		return file;
	}

	/**
	 * @param file the file to set
	 */
	public void setFile(IFile file) {
		this.file = file;
	}

}
