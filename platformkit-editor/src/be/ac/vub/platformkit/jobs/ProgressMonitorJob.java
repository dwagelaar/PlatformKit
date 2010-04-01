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
package be.ac.vub.platformkit.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import be.ac.vub.platformkit.logging.PlatformkitLogger;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * General superclass for jobs with a progress monitor.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public abstract class ProgressMonitorJob extends Job {

	/**
	 * Creates a new {@link ProgressMonitorJob}
	 * @param name the name of the job.
	 */
	public ProgressMonitorJob(String name) {
		super(name);
	}

	private boolean cancelled = false;
	private long jobStartTime;

	/**
	 * @return True if last run was cancelled.
	 */
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * Increases the progressmonitor by 1.
	 * @param monitor
	 * @throws OperationCanceledException if user pressed cancel button.
	 */
	protected void worked(IProgressMonitor monitor, String message) 
	throws OperationCanceledException {
		worked(monitor, null, message);
	}

	/**
	 * Increases the progressmonitor by 1.
	 * @param monitor
	 * @param subTask The subtask, or null if none.
	 * @throws OperationCanceledException if user pressed cancel button.
	 */
	protected void worked(IProgressMonitor monitor, ProgressMonitorJob subTask, String message) 
	throws OperationCanceledException {
		monitor.worked(1);
		if (message != null) {
			long time = System.currentTimeMillis() - getJobStartTime();
			PlatformkitLogger.logger.info(String.format(
					PlatformkitEditorPlugin.getPlugin().getString("ProgressMonitorJob.logAt"), 
					message,
					time,
					time,
					time)); //$NON-NLS-1$
		}
		if (subTask != null) {
			if (subTask.isCancelled()) {
				monitor.setCanceled(true);
			}
		}
		checkCanceled(monitor);
	}

	/**
	 * Checks if the monitor was cancelled.
	 * @param monitor
	 * @throws OperationCanceledException if the monitor was cancelled.
	 */
	protected void checkCanceled(IProgressMonitor monitor)
	throws OperationCanceledException {
		if (monitor.isCanceled()) {
			cancelled = true;
			throw new OperationCanceledException(
					PlatformkitEditorPlugin.getPlugin().getString("operationCancelled")); //$NON-NLS-1$
		}
	}

	/**
	 * Logs and starts a new task on the progress monitor
	 * @param monitor
	 * @param message
	 */
	protected void subTask(IProgressMonitor monitor, String message) {
		PlatformkitLogger.logger.info(message);
		monitor.subTask(message);
	}

	/**
	 * Logs and starts a series of tasks, and sets the start time.
	 * @param monitor
	 * @param message
	 * @param totalWork The amount of subtasks
	 */
	protected void beginTask(IProgressMonitor monitor, String message, int totalWork) {
		setJobStartTime(System.currentTimeMillis());
		monitor.beginTask(message, totalWork);
		PlatformkitLogger.logger.info(message);
	}

	/**
	 * @return the job start time
	 */
	public long getJobStartTime() {
		return jobStartTime;
	}

	/**
	 * @param startTime the job start time to set
	 */
	protected void setJobStartTime(long startTime) {
		this.jobStartTime = startTime;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		IStatus st;
		try {
			runAction(monitor);
			st = new Status(
					IStatus.OK, 
					PlatformkitEditorPlugin.getPlugin().getBundle().getSymbolicName(), 
					String.format(
							PlatformkitEditorPlugin.getPlugin().getString("ProgressMonitorJob.completedSuccesfully"), 
							getName())); //$NON-NLS-1$
		} catch (OperationCanceledException e) {
			st = Status.CANCEL_STATUS;
			catchCleanup();
		} catch (Exception e) {
			PlatformkitEditorPlugin.report(e);
			st = new Status(
					IStatus.ERROR, 
					PlatformkitEditorPlugin.getPlugin().getBundle().getSymbolicName(), 
					e.getLocalizedMessage(),
					e);
			catchCleanup();
		} finally {
			monitor.done();
			finallyCleanup();
		}
		return st;
	}

	/**
	 * Invoked when the action is executed.
	 * @param monitor
	 * @throws Exception
	 */
	protected abstract void runAction(IProgressMonitor monitor) throws Exception;

	/**
	 * Invoked after an Exception is caught in {@link #run(IProgressMonitor)}.
	 */
	protected void catchCleanup() {
		//stub
	}

	/**
	 * Invoked in "finally" block after {@link #run(IProgressMonitor)}.
	 */
	protected void finallyCleanup() {
		//stub
	}

}
