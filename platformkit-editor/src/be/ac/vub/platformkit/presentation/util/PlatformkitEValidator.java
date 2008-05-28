package be.ac.vub.platformkit.presentation.util;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import be.ac.vub.platformkit.Constraint;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.kb.IOntologiesFactory;
import be.ac.vub.platformkit.util.PlatformkitSwitch;

/**
 * EValidator for Platformkit models.
 * @author dennis
 *
 */
public class PlatformkitEValidator extends EValidatorWrapper {
	
	protected class EValidatorPlatformkitSwitch extends PlatformkitSwitch<Diagnostic> {

		public Diagnostic caseConstraint(Constraint object) {
			//TODO guarantee previous validation of ConstraintSpace?
			Assert.isNotNull(object.getSet());
			Assert.isNotNull(object.getSet().getSpace());
			if (object.getOntClass() == null) {
	            return new BasicDiagnostic
	                    (Diagnostic.ERROR,
	                     DIAGNOSTIC_SOURCE,
	                     0,
	                     object.getOntClassURI() + " not found in ontologies",
	                     new Object [] { object });
			}
			return super.caseConstraint(object);
		}

		public Diagnostic caseConstraintSpace(ConstraintSpace object) {
			//don't validate constraint spaces that already have a knowledge base;
			//these have already been validated (changes reset the knowledge base)
			if (object.getKnowledgeBase() == null) {
				try {
					object.setKnowledgeBase(IOntologiesFactory.INSTANCE.createIOntologies());
					object.init(false);
				} catch (IOException ioe) {
		            return new BasicDiagnostic
		                    (Diagnostic.ERROR,
		                     DIAGNOSTIC_SOURCE,
		                     0,
		                     ioe.getLocalizedMessage(),
		                     new Object [] { object });
				}
			}
			return super.caseConstraintSpace(object);
		}

	}
	
	protected PlatformkitSwitch<Diagnostic> validatorSwitch = new EValidatorPlatformkitSwitch();

	public PlatformkitEValidator() {
		super(null);
	}

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
