package org.mindswap.pellet.utils.progress;

import javax.swing.SwingUtilities;

/**
 * <p>
 * Title: SwingProgressMonitor
 * </p>
 * <p>
 * Description: Very simple implementation of the Pellet progress monitor using
 * Swing widgets.
 * </p>
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 * 
 * @author Mike Smith
 */
public class SwingProgressMonitor extends AbstractProgressMonitor {

	private javax.swing.ProgressMonitor	monitor	= null;

	public SwingProgressMonitor() {
		super();
	}

	public SwingProgressMonitor(int length) {
		super( length );
	}

	public boolean isCanceled() {
		return monitor.isCanceled();
	}

	protected void resetProgress() {
		super.resetProgress();
		monitor = new javax.swing.ProgressMonitor( null, progressTitle, progressMessage, 0,
				progressLength );
		monitor.setProgress( progress );
	}

	public void setProgressMessage(String progressMessage) {
		super.setProgressMessage( progressMessage );
		monitor.setNote( progressMessage );
	}

	public void taskFinished() {
		super.taskFinished();
		monitor.close();
	}

	protected void updateProgress() {
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				monitor.setProgress( progress );
			}
		} );
	}
}