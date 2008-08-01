/*
 * Created on Nov 29, 2006
 */
package org.mindswap.pellet.utils.progress;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Clark & Parsia, LLC. <http://www.clarkparsia.com></p>
 *
 * @author Evren Sirin
 */
public class SilentProgressMonitor extends AbstractProgressMonitor implements ProgressMonitor {
    public SilentProgressMonitor() {
    }

	@Override
	protected void updateProgress() {
		// do nothing		
	}
}
