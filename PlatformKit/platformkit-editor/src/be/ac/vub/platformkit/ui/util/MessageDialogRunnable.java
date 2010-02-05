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

import org.eclipse.jface.dialogs.MessageDialog;

import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * Wraps the displaying of a message dialog in a Runnable 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class MessageDialogRunnable implements Runnable {
	
	public static final int MODE_ERROR       = 1;
	public static final int MODE_INFORMATION = 2;
	public static final int MODE_WARNING     = 4;
	
    private String title = null;
    private String message = null;
    private int mode = MODE_INFORMATION;

    /**
     * Creates a new {@link MessageDialogRunnable}.
     * @param title The dialog title.
     * @param message The dialog message.
     */
    public MessageDialogRunnable(String title, String message) {
        setTitle(title);
        setMessage(message);
    }

    /**
     * Creates a new ConstraintListsDialogRunnable.
     * @param title The dialog title.
     * @param message The dialog message.
     * @param mode The dialog mode.
     */
    public MessageDialogRunnable(String title, String message, int mode) {
        setTitle(title);
        setMessage(message);
        setMode(mode);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
    	switch (mode) {
    	case MODE_ERROR:
            MessageDialog.openError(
            		PlatformkitEditorPlugin.INSTANCE.getShell(), 
                    getTitle(), getMessage());
    		break;
    	case MODE_INFORMATION:
            MessageDialog.openInformation(
            		PlatformkitEditorPlugin.INSTANCE.getShell(), 
                    getTitle(), getMessage());
    		break;
    	case MODE_WARNING:
            MessageDialog.openWarning(
            		PlatformkitEditorPlugin.INSTANCE.getShell(), 
                    getTitle(), getMessage());
    		break;
    	}
    }

    /**
     * @param title The title to set.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return Returns the title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param message The message to set.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return Returns the message.
     */
    public String getMessage() {
        return message;
    }

	/**
	 * @return the mode
	 */
	public int getMode() {
		return mode;
	}

	/**
	 * @param mode the mode to set
	 */
	public void setMode(int mode) {
		this.mode = mode;
	}
}
