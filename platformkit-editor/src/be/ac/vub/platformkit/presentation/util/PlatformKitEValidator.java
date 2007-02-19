package be.ac.vub.platformkit.presentation.util;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EObjectValidator;

import be.ac.vub.platformkit.presentation.popup.action.Profile;

/**
 * Applies an IEObjectValidator in the EMF validation chain.
 * @author dennis
 *
 */
public class PlatformKitEValidator implements EValidator {
	
	public static final String DIAGNOSTIC_SOURCE = "be.ac.vub.cddtoolkit.eclipseui";

	private EValidator inner;
	
	/**
	 * Creates a new CDDEValidator.
	 * @param inner The wrapped EValidator.
	 */
	public PlatformKitEValidator(EValidator inner) {
		super();
		this.inner = inner;
	}

	public boolean validate(EObject eObject, DiagnosticChain diagnostics,
			Map context) {
		return validate(eObject.eClass(), eObject, diagnostics, context);
	}

	public boolean validate(EClass eClass, EObject eObject,
			DiagnosticChain diagnostics, Map context) {
		boolean innerValid = true;
		if (inner != null) {
			innerValid = inner.validate(eClass, eObject, diagnostics, context);
		} else {
			innerValid = EObjectValidator.INSTANCE.validate(eClass, eObject, diagnostics, context);
		}
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
		return innerValid && valid;
	}

	public boolean validate(EDataType eDataType, Object value,
			DiagnosticChain diagnostics, Map context) {
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
