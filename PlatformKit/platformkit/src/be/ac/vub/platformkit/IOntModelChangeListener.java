/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
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
	String copyright = "(C) 2007-2008, Dennis Wagelaar, Vrije Universiteit Brussel";

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