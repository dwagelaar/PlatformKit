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
package be.ac.vub.platformkit.presentation.popup.action;

import be.ac.vub.platformkit.presentation.jobs.SortPlatformkitModelJob;

/**
 * Sorts the options in a PlatformKit model most-specific first.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class SortMostSpecificFirst extends SortPlatformkitModel {

	/**
	 * Creates a new {@link SortMostSpecificFirst}.
	 */
	public SortMostSpecificFirst() {
		super(SortPlatformkitModelJob.MOST_SPECIFIC);
	}

}
