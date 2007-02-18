/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package be.ac.vub.platformkit;

import com.hp.hpl.jena.ontology.OntModel;

import org.eclipse.emf.ecore.EObject;

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
	String copyright = "(C) 2007, Dennis Wagelaar, Vrije Universiteit Brussel";

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model ontModelAnnotation="GenModel documentation='the new ontology model.'"
	 *        annotation="GenModel documentation='Invoked when ontology model has changed.'"
	 * @generated
	 */
	void ontModelChanged(OntModel ontModel);

} // IOntModelChangeListener