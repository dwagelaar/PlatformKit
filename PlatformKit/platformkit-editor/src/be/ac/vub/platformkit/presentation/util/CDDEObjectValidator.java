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

public class CDDEObjectValidator implements IEObjectValidator {
	private Set invalid = new HashSet();
	private List valid = new ArrayList();
	
	/**
	 * Creates a new CDDEObjectValidator.
	 * @param space The ConstraintSpace to validate against.
	 */
	public CDDEObjectValidator(ConstraintSpace space) {
		Assert.isNotNull(space);
		for (Iterator it = space.getInvalid().iterator(); it.hasNext();) {
			ConstraintSet set = (ConstraintSet) it.next();
			invalid.add(set.getName());
		}
		for (Iterator it = space.getValid().iterator(); it.hasNext();) {
			ConstraintSet set = (ConstraintSet) it.next();
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
