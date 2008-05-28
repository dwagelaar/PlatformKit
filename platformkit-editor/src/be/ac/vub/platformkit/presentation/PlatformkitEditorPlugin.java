/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package be.ac.vub.platformkit.presentation;

import java.util.logging.Handler;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.ui.EclipseUIPlugin;
import org.eclipse.emf.common.util.ResourceLocator;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import be.ac.vub.platformkit.PlatformkitPackage;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.presentation.logging.ConsoleStreamHandler;
import be.ac.vub.platformkit.presentation.util.ErrorDialogRunnable;
import be.ac.vub.platformkit.presentation.util.PlatformkitEValidator;

import com.hp.hpl.jena.eclipse.JenaPlugin;

/**
 * This is the central singleton for the Platformkit editor plugin.
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public final class PlatformkitEditorPlugin extends EMFPlugin {
    
    public static final String PLATFORMSPEC_EXT_POINT = "be.ac.vub.platformkit.editor.platformSpec";
    
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final String copyright = "(C) 2007-2008, Dennis Wagelaar, Vrije Universiteit Brussel";

	/**
	 * Keep track of the singleton.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final PlatformkitEditorPlugin INSTANCE = new PlatformkitEditorPlugin();

	/**
	 * Keep track of the singleton.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static Implementation plugin;

    private static MessageConsole console = null;
    private static MessageConsoleStream consoleStream = null;
    private static IConsoleManager consoleMgr = null; 
    private static final String PLATFORMKIT_CONSOLE = "be.ac.vub.platformkit.presentation.console"; 

	/**
	 * Create the instance.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PlatformkitEditorPlugin() {
		super
			(new ResourceLocator [] {
			});
	}

	/**
	 * Returns the singleton instance of the Eclipse plugin.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the singleton instance.
	 * @generated
	 */
	@Override
	public ResourceLocator getPluginResourceLocator() {
		return plugin;
	}

	/**
	 * Returns the singleton instance of the Eclipse plugin.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the singleton instance.
	 * @generated
	 */
	public static Implementation getPlugin() {
		return plugin;
	}

	/**
	 * The actual implementation of the Eclipse <b>Plugin</b>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static class Implementation extends EclipseUIPlugin {
		/**
		 * Creates an instance.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 */
		public Implementation() {
			super();

			// Remember the static instance.
			//
			plugin = this;
	        if (console == null) {
	            initConsole();
	        }
	        EValidator.Registry.INSTANCE.put(PlatformkitPackage.eINSTANCE, new PlatformkitEValidator());
		}

		private void initConsole () {
	        console = findConsole(PLATFORMKIT_CONSOLE);
	        consoleStream = console.newMessageStream();
	        activateConsole();
	        consoleStream.println("Platformkit Console initiated");
	        Handler handler = new ConsoleStreamHandler(consoleStream);
	        Logger.getLogger(IOntologies.LOGGER).addHandler(handler);
	        JenaPlugin.getDefault().addLogHandler(handler);
	    }

	    private MessageConsole findConsole(String name) {
	        ConsolePlugin plugin = ConsolePlugin.getDefault();
	        consoleMgr = plugin.getConsoleManager();
	        IConsole[] existing = consoleMgr.getConsoles();
	        for (int i = 0; i < existing.length; i++)
	            if (name.equals(existing[i].getName()))
	                return (MessageConsole) existing[i];
	        //no console found, so create a new one
	        MessageConsole myConsole = new MessageConsole(name, null);
	        consoleMgr.addConsoles(new IConsole[]{myConsole});
	        return myConsole;
	    }
	    
	    private void activateConsole () {
	        IWorkbenchPage page = null;
	        IWorkbenchWindow window = getPlugin().getWorkbench().getActiveWorkbenchWindow();
	        if (window != null) {
	            page = window.getActivePage();
	        }
	        String id = IConsoleConstants.ID_CONSOLE_VIEW;
	        try {
	            if (page != null) {
	                IConsoleView view = (IConsoleView) page.showView(id);
	                view.display(console);      
	            }
	        } catch (org.eclipse.ui.PartInitException pex) {
	            pex.printStackTrace();
	        }
	    }   
	}
	
    /**
     * @return The active shell.
     */
    public Shell getShell() {
        return getPlugin().getWorkbench().getDisplay().getActiveShell();
    }

    /**
     * Reports an exception/error in the log and on the screen.
     * @param e the exception to report.
     */
    public void report(Throwable e) {
        getPlugin().getWorkbench().getDisplay().syncExec(new ErrorDialogRunnable(e));
    }

    /**
     * Logs a message.
     * @param message the log message.
     * @param level the log level (OK, INFO, WARNING, ERROR)
     * @param exception the related exception, if any.
     */
    public IStatus log(String message, int level, Throwable exception) {
        IStatus st = new Status(
                level, 
                getPlugin().getBundle().getSymbolicName(), 
                IStatus.OK, 
                message, 
                exception);
        getPlugin().getLog().log(st);
        return st;
    }

}
