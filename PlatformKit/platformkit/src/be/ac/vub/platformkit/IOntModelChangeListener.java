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
package be.ac.vub.platformkit;

import org.eclipse.emf.ecore.EObject;

import be.ac.vub.platformkit.kb.IOntModel;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>IOnt Model Change Listener</b></em>'.
 * <!-- end-user-doc -->
 *
 *
 * @see be.ac.vub.platformkit.PlatformkitPackage#getIOntModelChangeListener()
 * @model interface="true" abstract="true"
 *        annotation="GenModel documentation='Change listener interface for {@link Ontologies#getOntModel()}'"
 * @generated
 */
public interface IOntModelChangeListener extends EObject {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String copyright = "(C) 2005-2010, Dennis Wagelaar, Vrije Universiteit Brussel";

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model ontModelType="be.ac.vub.platformkit.IOntModel"
	 *        ontModelAnnotation="GenModel documentation='the new ontology model.'"
	 *        annotation="GenModel documentation='Invoked when ontology model has changed.'"
	 * @generated
	 */
	void ontModelChanged(IOntModel ontModel);

} // IOntModelChangeListener