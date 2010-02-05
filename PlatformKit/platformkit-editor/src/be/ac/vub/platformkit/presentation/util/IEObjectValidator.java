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
package be.ac.vub.platformkit.presentation.util;

import org.eclipse.emf.ecore.EObject;

/**
 * Interface for determining validity of EObjects.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public interface IEObjectValidator {

	/**
	 * @param value The object to validate
	 * @return True if the object is valid, false otherwise
	 */
	public boolean isValid(EObject value);

	/**
	 * @param value The object to validate.
	 * @return The optimisation index of the object in the list of valid objects, or -1 if no index exists.
	 */
	public int indexOf(EObject value);
}
