/*
 * Created on Oct 20, 2005
 */
package org.mindswap.pellet.utils.progress;


/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2006
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 * 
 * @author Evren Sirin
 */
public abstract class AbstractProgressMonitor implements ProgressMonitor {
	protected String	progressTitle	= "";

	protected String	progressMessage	= "";

	protected int		progress		= 0;

	protected int		progressLength	= 0;

	protected int		progressPercent	= -1;

	protected long		startTime		= -1;

	protected boolean	cancelled		= false;

	public AbstractProgressMonitor() {
	}

	public AbstractProgressMonitor(int length) {
		setProgressLength( length );
	}

	public int getProgress() {
		return progress;
	}

	public int getProgressLength() {
		return progressLength;
	}

	public String getProgressMessage() {
		return progressMessage;
	}

	public int getProgressPercent() {
		return progressPercent;
	}

	public String getProgressTitle() {
		return progressTitle;
	}

	public void incrementProgress() {
		setProgress( progress + 1 );
	}

	public boolean isCanceled() {
		return cancelled;
	}

	protected void resetProgress() {
		progress = 0;
		progressPercent = -1;
	}

	public void setProgress(int progress) {
		this.progress = progress;

		updateProgress();
	}

	public void setProgressLength(int progressLength) {
		this.progressLength = progressLength;

		resetProgress();
	}

	public void setProgressMessage(String progressMessage) {
		this.progressMessage = progressMessage;
	}

	public void setProgressTitle(String progressTitle) {
		this.progressTitle = progressTitle;
	}

	public void taskFinished() {
	}

	public void taskStarted() {
		resetProgress();

		startTime = System.currentTimeMillis();
	}

	protected abstract void updateProgress();
}
