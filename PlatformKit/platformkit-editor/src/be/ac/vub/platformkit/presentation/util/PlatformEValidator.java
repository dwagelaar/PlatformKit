package be.ac.vub.platformkit.presentation.util;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EObjectValidator;

/**
 * Applies an {@link EObjectValidator} in the EMF validation chain.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformEValidator extends EValidatorWrapper {

	/**
	 * Creates a new {@link PlatformEValidator}.
	 * @param inner the wrapped {@link EValidator}.
	 */
	public PlatformEValidator(EValidator inner) {
		super(inner);
	}

	/*
	 * (non-Javadoc)
	 * @see be.ac.vub.platformkit.presentation.util.EValidatorWrapper#validate(org.eclipse.emf.ecore.EClass, org.eclipse.emf.ecore.EObject, org.eclipse.emf.common.util.DiagnosticChain, java.util.Map)
	 */
	@Override
	public boolean validate(EClass eClass, EObject eObject, 
			DiagnosticChain diagnostics, Map<Object, Object> context) {
		boolean superValid = super.validate(eClass, eObject, diagnostics, context);
		boolean valid = true;
		Resource res = eObject.eResource();
		Assert.isNotNull(res);
		IEObjectValidator validator = Registry.INSTANCE.get(res);
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

	/**
	 * Registry of resource validators.
	 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
	 */
	public static class Registry extends WeakHashMap<Resource, IEObjectValidator> {

		public static Registry INSTANCE = new Registry();

		private Registry() {
			super();
		}

	}

}
