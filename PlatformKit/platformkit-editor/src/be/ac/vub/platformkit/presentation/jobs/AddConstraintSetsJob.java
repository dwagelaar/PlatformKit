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
package be.ac.vub.platformkit.presentation.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import be.ac.vub.platformkit.Constraint;
import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.PlatformkitFactory;
import be.ac.vub.platformkit.logging.PlatformkitLogger;
import be.ac.vub.platformkit.presentation.PlatformkitEditorPlugin;

/**
 * Abstract job for adding new {@link ConstraintSet}s to a {@link ConstraintSpace}.
 * Each context-constrained element should have an {@link EAnnotation} with
 * source set to 'PlatformKit' and containing a details entry with the key
 * set to 'PlatformConstraint' and the value set to the ontology class that
 * expressed the platform dependency constraint.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public abstract class AddConstraintSetsJob extends ConstraintSpaceJob {

	/**
	 * Translates a path from the source URI location
	 * to the target URI location
	 * @param path The ontology path to translate
	 * @param source The URI on which the path is based
	 * @param target The URI to translate the path to
	 * @return The translated ontology path
	 */
	protected static String translatePath(String path, URI source, URI target) {
		URI pathURI = URI.createURI(path);
		URI absURI = pathURI.resolve(source);
		return absURI.deresolve(target, true, true ,true).toString();
	}

	/**
	 * @param object
	 * @return the annotations for the given object.
	 */
	protected static EList<EAnnotation> getEAnnotations(EObject object) {
		if (object instanceof EModelElement) {
			return ((EModelElement) object).getEAnnotations();
		}
		return null;
	}

	/**
	 * @param object
	 * @return Returns the ID attribute for the given EObject
	 * or the qualified name for an ENamedElement.
	 */
	protected static Object getIDFrom(EObject object) {
		EAttribute attr = getIDAttribute(object.eClass());
		if (attr != null) {
			return object.eGet(attr);
		}
		//Ecore does not specify 'name' as an ID for its meta-objects.
		if (object instanceof ENamedElement) {
			return getLabel((ENamedElement) object);
		}
		return null;
	}

	/**
	 * @param metaClass
	 * @return The EAttribute that is flagged as ID or null.
	 */
	protected static EAttribute getIDAttribute(EClass metaClass) {
		EList<EObject> contents = metaClass.eContents();
		for (int i = 0; i < contents.size(); i++) {
			if (contents.get(i) instanceof EAttribute) {
				EAttribute attr = (EAttribute) contents.get(i);
				if (attr.isID()) {
					return attr;
				}
			}
		}
		return null;
	}

	/**
	 * @param type
	 * @return The qualified name of type.
	 */
	protected static String getLabel(ENamedElement type) {
		EObject container = type.eContainer();
		if (container instanceof ENamedElement) {
			return getLabel((ENamedElement)container) + "::" + type.getName(); //$NON-NLS-1$
		} else {
			return type.getName();                
		}
	}

	private String sourceName;
	private Resource[] sources;
	protected PlatformkitFactory factory = PlatformkitFactory.eINSTANCE;

	/**
	 * Creates a new {@link AddConstraintSetsJob}
	 * @param name the name of the job.
	 */
	public AddConstraintSetsJob(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see be.ac.vub.platformkit.jobs.ProgressMonitorJob#runAction(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void runAction(IProgressMonitor monitor) throws Exception {
		beginTask(monitor, String.format(
				PlatformkitEditorPlugin.getPlugin().getString("AddConstraintSetsJob.beginTask"), 
				getSourceName()), 1); //$NON-NLS-1$
		subTask(monitor, String.format(
				PlatformkitEditorPlugin.getPlugin().getString("AddConstraintSetsJob.addingCS"), 
				getSourceName())); //$NON-NLS-1$
		Resource[] sources = getSources();
		List<Command> commands = createCommands(sources);
		Command cmd = new CompoundCommand(commands);
		getEditingDomain().getCommandStack().execute(cmd);
		worked(monitor, PlatformkitEditorPlugin.getPlugin().getString("AddConstraintSetsJob.addedCS")); //$NON-NLS-1$
	}

	/**
	 * @return A List of Commands to be executed to add new ConstraintSets that reflect the given source models.
	 * @param sources
	 */
	protected List<Command> createCommands(Resource[] sources) {
		List<Command> commands = new ArrayList<Command>();
		EList<String> ontologies = new BasicEList<String>();
		EList<ConstraintSet> constraintSets = new BasicEList<ConstraintSet>();
		ConstraintSpace space = getSpace();
		ontologies.addAll(space.getOntology());
		for (int i = 0; i < sources.length; i++) {
			addOntologies(sources[i], ontologies);
			addConstraintSet(sources[i], constraintSets);
		}
		ontologies.removeAll(space.getOntology());
		if (!ontologies.isEmpty()) {
			commands.add(createAddOntologyCommand(ontologies));
		}
		if (!constraintSets.isEmpty()) {
			commands.add(createAddConstraintSetCommand(constraintSets));
		}
		return commands;
	}

	/**
	 * Searches source for ontologies and adds them to the list.
	 * @see ConstraintSpace#getOntology()
	 * @param source
	 * @param ontologies
	 */
	protected abstract void addOntologies(Resource source, EList<String> ontologies);

	/**
	 * Searches source for constraints and adds a constraint set to the list.
	 * @see ConstraintSpace#getConstraintSet()
	 * @param source
	 * @param constraintSets
	 */
	protected abstract void addConstraintSet(Resource source, EList<ConstraintSet> constraintSets);

	/**
	 * Searches the metaobject for ontology annotations and adds them to the list.
	 * @param object The metaobject.
	 * @param ontologies The list of ontologies to add to.
	 */
	protected void addMetaObjectOntologies(EObject object, EList<String> ontologies) {
		URI platformkitURI = getSpace().eResource().getURI();
		Assert.isNotNull(object);
		EList<EAnnotation> annotations = getEAnnotations(object);
		if (annotations == null) {
			return;
		}
		Assert.isNotNull(platformkitURI);
		for (int i = 0; i < annotations.size(); i++) {
			EAnnotation ann = annotations.get(i);
			if ("PlatformKit".equals(ann.getSource())) { //$NON-NLS-1$
				String ont = ann.getDetails().get("Ontology"); //$NON-NLS-1$
				if (ont != null) {
					StringTokenizer onts = new StringTokenizer(ont, "\n"); //$NON-NLS-1$
					while (onts.hasMoreTokens()) {
						ont = onts.nextToken();
						ont = translatePath(ont, ann.eResource().getURI(), platformkitURI);
						if (!ontologies.contains(ont)) {
							ontologies.add(ont);
							PlatformkitLogger.logger.info(String.format(
									PlatformkitEditorPlugin.getPlugin().getString("AddConstraintSetsJob.addedOnt"), 
									ont)); //$NON-NLS-1$
						}
					}
				}
			}
		}
	}

	/**
	 * Adds constraints to the given constraint set.
	 * @param annotations the annotations to search for context constraints.
	 * @param constraints the set of existing constraint strings.
	 * @param set the constraint set to augment.
	 */
	protected void addConstraints(EList<EAnnotation> annotations, Set<String> constraints, ConstraintSet set) {
		if (annotations == null) {
			return;
		}
		for (int i = 0; i < annotations.size(); i++) {
			EAnnotation ann = (EAnnotation) annotations.get(i);
			if ("PlatformKit".equals(ann.getSource())) { //$NON-NLS-1$
				addConstraints(ann, constraints, set);
			}
		}
	}

	/**
	 * Adds constraints to the given constraint set.
	 * @param ann the annotation to search for context constraints.
	 * @param constraints the set of existing constraint strings.
	 * @param set the constraint set to augment.
	 */
	protected void addConstraints(EAnnotation ann, Set<String> constraints, ConstraintSet set) {
		for (int i = 0; i < ann.getDetails().size(); i++) {
			Entry<String, String> detail = ann.getDetails().get(i);
			if ("PlatformConstraint".equals(detail.getKey())) { //$NON-NLS-1$
				String constraint = detail.getValue();
				if (!constraints.contains(constraint) && (constraint != null)) {
					Constraint c = factory.createConstraint();
					c.setSet(set);
					c.setOntClassURI(constraint);
					constraints.add(constraint);
				}
			}
		}
	}

	/**
	 * @return the sourceName
	 */
	public String getSourceName() {
		return sourceName;
	}

	/**
	 * @param sourceName the sourceName to set
	 */
	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	/**
	 * @return the sources
	 */
	public Resource[] getSources() {
		return sources;
	}

	/**
	 * @param sources the sources to set
	 */
	public void setSources(Resource[] sources) {
		this.sources = sources;
	}

}
