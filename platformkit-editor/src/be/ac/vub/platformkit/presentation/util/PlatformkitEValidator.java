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

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import be.ac.vub.platformkit.Constraint;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.editor.preferences.PreferenceInitializer;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;
import be.ac.vub.platformkit.util.PlatformkitSwitch;

/**
 * EValidator for Platformkit models.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformkitEValidator extends EValidatorWrapper {

	/**
	 * Validates the elements of a Platformkit model.
	 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
	 */
	protected class EValidatorPlatformkitSwitch extends PlatformkitSwitch<Diagnostic> {

		/*
		 * (non-Javadoc)
		 * @see be.ac.vub.platformkit.util.PlatformkitSwitch#caseConstraint(be.ac.vub.platformkit.Constraint)
		 */
		@Override
		public Diagnostic caseConstraint(Constraint object) {
			//TODO guarantee previous validation of ConstraintSpace?
			Assert.isNotNull(object.getSet());
			Assert.isNotNull(object.getSet().getSpace());
			if (object.getOntClass() == null) {
				return new BasicDiagnostic
				(Diagnostic.ERROR,
						DIAGNOSTIC_SOURCE,
						0,
						String.format(PlatformkitEditorPlugin.getPlugin().getString("PlatformkitEValidator.owlClassNotFound"), object.getOntClassURI()),
						new Object [] { object }); //$NON-NLS-1$
			}
			return super.caseConstraint(object);
		}

		/*
		 * (non-Javadoc)
		 * @see be.ac.vub.platformkit.util.PlatformkitSwitch#caseConstraintSpace(be.ac.vub.platformkit.ConstraintSpace)
		 */
		@Override
		public Diagnostic caseConstraintSpace(ConstraintSpace object) {
			//don't validate constraint spaces that already have a knowledge base;
			//these have already been validated (changes reset the knowledge base)
			if (object.getKnowledgeBase() == null) {
				try {
					object.setKnowledgeBase(PreferenceInitializer.getPreferredOntologyFactory().createIOntologies());
					object.init(false);
				} catch (Exception e) {
					return new BasicDiagnostic
					(Diagnostic.ERROR,
							DIAGNOSTIC_SOURCE,
							0,
							e.getLocalizedMessage(),
							new Object [] { object });
				}
			}
			return super.caseConstraintSpace(object);
		}

	}

	protected PlatformkitSwitch<Diagnostic> validatorSwitch = new EValidatorPlatformkitSwitch();

	/**
	 * Creates a new {@link PlatformkitEValidator}.
	 */
	public PlatformkitEValidator() {
		super(null);
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.presentation.util.EValidatorWrapper#validate(org.eclipse.emf.ecore.EClass, org.eclipse.emf.ecore.EObject, org.eclipse.emf.common.util.DiagnosticChain, java.util.Map)
	 */
	@Override
	public boolean validate(EClass eClass, EObject eObject, 
			DiagnosticChain diagnostics, Map<Object, Object> context) {
		boolean superValid = super.validate(eClass, eObject, diagnostics, context);
		Diagnostic diag = validatorSwitch.doSwitch(eObject);
		if ((diagnostics != null) && (diag != null)) {
			diagnostics.add(diag);
		}
		return superValid && (diag == null);
	}

}
