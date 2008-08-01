/*
 * Created on Oct 21, 2005
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
public interface ProgressMonitor {

	public int getProgress();

	public int getProgressPercent();

	public void incrementProgress();

	public boolean isCanceled();

	public void setProgress(int value);

	public void setProgressLength(int length);

	public void setProgressMessage(String message);

	public void setProgressTitle(String title);

	public void taskFinished();

	public void taskStarted();
}