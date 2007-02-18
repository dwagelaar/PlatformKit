package be.ac.vub.platformkit.presentation.util;

import org.eclipse.emf.ecore.EObject;

/**
 * Interface for determining validity of EObjects.
 * @author dennis
 *
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
