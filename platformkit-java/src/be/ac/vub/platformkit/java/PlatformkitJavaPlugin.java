/*******************************************************************************
 * Copyright (c) 2005-2010 Dennis Wagelaar, Vrije Universiteit Brussel.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dennis Wagelaar, Vrije Universiteit Brussel
 *******************************************************************************/
package be.ac.vub.platformkit.java;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import be.ac.vub.platformkit.ui.util.ErrorDialogRunnable;

/**
 * PlatformKit for Java platforms plug-in class 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformkitJavaPlugin extends AbstractUIPlugin {

	public static final String PLATFORMAPI_EXT_POINT = "be.ac.vub.platformkit.java.apimodel"; //$NON-NLS-1$

	private static PlatformkitJavaPlugin plugin = null;

	/**
	 * @return The singleton {@link PlatformkitJavaPlugin} instance.
	 */
	public static PlatformkitJavaPlugin getPlugin() {
		return plugin;
	}

	/**
	 * Sets the singleton instance of the Eclipse plugin.
	 * @param pi
	 */
	private static void setPlugin(PlatformkitJavaPlugin pi) {
		plugin = pi;
	}

	/**
	 * Creates a new {@link PlatformkitJavaPlugin}.
	 */
	public PlatformkitJavaPlugin() {
		super();
		setPlugin(this);
	}

	/**
	 * @return The active shell.
	 */
	public Shell getShell() {
		return getWorkbench().getDisplay().getActiveShell();
	}

	/**
	 * Reports an exception/error in the log and on the screen.
	 * @param e the exception to report.
	 */
	public void report(Throwable e) {
		getWorkbench().getDisplay().syncExec(new ErrorDialogRunnable(e, getLog()));
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

	/**
	 * @param imageFilePath
	 * @return The ImageDescriptor object for imageFilePath
	 * @see #imageDescriptorFromPlugin(String, String)
	 */
	public ImageDescriptor getImageDescriptor(String imageFilePath) {
		return imageDescriptorFromPlugin(getBundle().getSymbolicName(), imageFilePath);
	}

}
