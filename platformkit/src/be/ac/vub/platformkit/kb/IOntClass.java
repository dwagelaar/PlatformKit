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
package be.ac.vub.platformkit.kb;

/**
 * Interface for ontology classes 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public interface IOntClass {

	/**
	 * <p>Answer true if the given class is equivalent to this class.</p>
	 * @param c A class to test for
	 * @return True if the given property is equivalent to this class.
	 * @throws ClassCastException if c implementation is not compatible with self.
	 */
	public boolean hasEquivalentClass(IOntClass c) throws ClassCastException;

	/**
	 * <p>Answer true if the given class is a sub-class of this class.</p>
	 * @param c A class to test.
	 * @return True if the given class is a sub-class of this class.
	 * @throws ClassCastException if c implementation is not compatible with self.
	 */
	public boolean hasSubClass(IOntClass c) throws ClassCastException;

	/**
	 * <p>Answer true if the given class is a super-class of this class.</p>
	 * @param c A class to test.
	 * @return True if the given class is a super-class of this class.
	 * @throws ClassCastException if c implementation is not compatible with self.
	 */
	public boolean hasSuperClass(IOntClass c) throws ClassCastException;

	/**
	 * <p>Answer true if there are individuals in the model that have this
	 * class among their types.<p>
	 *
	 * @return True if there are instances that have this class as one of
	 *         the classes to which they belong
	 */
	public boolean hasInstances();

}
