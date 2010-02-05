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
package be.ac.vub.platformkit.ui.util;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.PlatformUI;

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * Wraps an error dialog in a {@link Runnable}.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class ErrorDialogRunnable implements Runnable {

    private IStatus st;

    /**
     * Creates a new {@link ErrorDialogRunnable}.
     * @param e The error to report.
     * @param logger The logger to use for reporting
     */
    public ErrorDialogRunnable(Throwable e, ILog logger) {
        String message;
        if (e.getMessage() == null) {
            message = e.getClass().getName(); 
        } else {
            message = e.getMessage();
        }
        setStatus(new Status(
                IStatus.ERROR, 
                logger.getBundle().getSymbolicName(), 
                IStatus.OK, 
                message, 
                e));
        logger.log(getStatus());
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
    	IStatus st = getStatus();
        ErrorDialog dlg = new ErrorDialog(
        		PlatformUI.getWorkbench().getDisplay().getActiveShell(),
                PlatformkitEditorPlugin.getPlugin().getString("ErrorDialogRunnable.dlgTitle"),
                st.getMessage(),
                st,
                IStatus.ERROR); //$NON-NLS-1$
        dlg.open();
    }
    
	/**
	 * @return the status
	 */
	public IStatus getStatus() {
		return st;
	}

	/**
	 * @param st the status to set
	 */
	protected void setStatus(IStatus st) {
		this.st = st;
	}
}
