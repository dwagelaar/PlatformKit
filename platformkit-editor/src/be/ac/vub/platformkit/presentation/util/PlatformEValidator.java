package be.ac.vub.platformkit.presentation.util;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EObjectValidator;

import be.ac.vub.platformkit.presentation.popup.action.Profile;

/**
 * Applies an {@link EObjectValidator} in the EMF validation chain.
 * @author dennis
 *
 */
public class PlatformEValidator extends EValidatorWrapper {

	public PlatformEValidator(EValidator inner) {
		super(inner);
	}

	@Override
	public boolean validate(EClass eClass, EObject eObject, DiagnosticChain diagnostics, Map context) {
		boolean superValid = super.validate(eClass, eObject, diagnostics, context);
		boolean valid = true;
		Resource res = eObject.eResource();
		Assert.isNotNull(res);
		IEObjectValidator validator = Profile.Registry.INSTANCE.getValidator(res);
		if (validator != null) {
			valid = validator.isValid(eObject);
		}
		if ((diagnostics != null) && (!valid)) {
            diagnostics.add
            (new BasicDiagnostic
              (Diagnostic.ERROR,
               DIAGNOSTIC_SOURCE,
               0,
               eClass.getName() + " instances are not valid in the chosen context",
               new Object [] { eObject }));
		}
		return superValid && valid;
	}

}
