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

import java.util.Map;

import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.util.EObjectValidator;

/**
 * Wraps an inner EValidator and applies it if not null.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public abstract class EValidatorWrapper implements EValidator {

	public static final String DIAGNOSTIC_SOURCE = "be.ac.vub.platformkit.editor"; //$NON-NLS-1$

	private EValidator inner;

	/**
	 * Creates a new EValidatorWrapper.
	 * @param inner The wrapped EValidator.
	 */
	public EValidatorWrapper(EValidator inner) {
		super();
		this.inner = inner;
	}

	public boolean validate(EObject eObject, DiagnosticChain diagnostics,
			Map<Object, Object> context) {
		return validate(eObject.eClass(), eObject, diagnostics, context);
	}

	public boolean validate(EClass eClass, EObject eObject,
			DiagnosticChain diagnostics, Map<Object, Object> context) {
		boolean innerValid = true;
		if (inner != null) {
			innerValid = inner.validate(eClass, eObject, diagnostics, context);
		} else {
			innerValid = EObjectValidator.INSTANCE.validate(eClass, eObject, diagnostics, context);
		}
		return innerValid;
	}

	public boolean validate(EDataType eDataType, Object value,
			DiagnosticChain diagnostics, Map<Object, Object> context) {
		boolean innerValid = true;
		if (inner != null) {
			innerValid = inner.validate(eDataType, value, diagnostics, context);
		} else {
			innerValid = EObjectValidator.INSTANCE.validate(eDataType, value, diagnostics, context);
		}
		return innerValid;
	}

	/**
	 * @return The wrapped EValidator.
	 */
	public EValidator getInner() {
		return inner;
	}

}
