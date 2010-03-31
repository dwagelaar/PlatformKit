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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.kb.util.OntException;

/**
 * Validates {@link EObject}s against an internal {@link ConstraintSpace} copy.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformKitEObjectValidator implements IEObjectValidator {

	private Set<String> invalid = new HashSet<String>();
	private List<String> valid = new ArrayList<String>();

	/**
	 * Creates a new {@link PlatformKitEObjectValidator}.
	 * @param space The ConstraintSpace to validate against.
	 * @throws OntException 
	 */
	public PlatformKitEObjectValidator(ConstraintSpace space) throws OntException {
		Assert.isNotNull(space);
		for (Iterator<ConstraintSet> it = space.getInvalid().iterator(); it.hasNext();) {
			ConstraintSet set = it.next();
			invalid.add(set.getName());
		}
		for (Iterator<ConstraintSet> it = space.getValid().iterator(); it.hasNext();) {
			ConstraintSet set = it.next();
			valid.add(set.getName());
		}
	}

	/**
	 * @param value The object to validate.
	 * @return True if the object's EClass is valid according to the ConstraintSpace.
	 */
	public boolean isValid(EObject value) {
		return ! invalid.contains(qName(value.eClass(), "::")); //$NON-NLS-1$
	}

	/**
	 * @param value The object to validate.
	 * @return The optimisation index of the object in the list of valid objects, or -1 if no index exists.
	 */
	public int indexOf(EObject value) {
		return valid.indexOf(qName(value.eClass(), "::")); //$NON-NLS-1$
	}

	/**
	 * @param o
	 * @param delim qualifier delimiter, e.g. "::"
	 * @return The qualified name for o separated by delim
	 */
	public static String qName(ENamedElement o, String delim) {
		if (o.eContainer() instanceof ENamedElement) {
			return qName((ENamedElement) o.eContainer(), delim) + delim + o.getName();
		} else {
			return o.getName();
		}
	}
}
