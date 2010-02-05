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

import org.eclipse.jface.viewers.ViewerFilter;

/**
 * General superclass for popup actions with a viewer filter.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public abstract class ViewerFilterAction extends ObjectSelectionAction {

	private ViewerFilter filter = null;

	/**
	 * Creates a new {@link ViewerFilterAction}.
	 */
	public ViewerFilterAction() {
		super();
	}

	/**
	 * @param filter The filter to set.
	 */
	public void setFilter(ViewerFilter filter) {
		this.filter = filter;
	}

	/**
	 * @return the filter
	 */
	public ViewerFilter getFilter() {
		return filter;
	}

}
