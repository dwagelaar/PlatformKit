package be.ac.vub.platformkit.presentation.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.ecore.EObject;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;

/**
 * Validates EObjects against an internal ConstraintSpace copy.
 * @author dennis
 *
 */
public class PlatformKitEObjectValidator implements IEObjectValidator {
	private Set<String> invalid = new HashSet<String>();
	private List<String> valid = new ArrayList<String>();
	
	/**
	 * Creates a new CDDEObjectValidator.
	 * @param space The ConstraintSpace to validate against.
	 */
	public PlatformKitEObjectValidator(ConstraintSpace space) {
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
		return ! invalid.contains(PlatformKitActionUtil.qName(value.eClass(), "::"));
	}
	
	/**
	 * @param value The object to validate.
	 * @return The optimisation index of the object in the list of valid objects, or -1 if no index exists.
	 */
	public int indexOf(EObject value) {
		return valid.indexOf(PlatformKitActionUtil.qName(value.eClass(), "::"));
	}

}
