package be.ac.vub.platformkit.presentation.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
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
import be.ac.vub.platformkit.kb.IOntologies;

/**
 * Static utility functions.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 *
 */
public class PlatformKitActionUtil {

	protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);
	protected static PlatformkitFactory factory = PlatformkitFactory.eINSTANCE;
    private static Map<Object, ConstraintSpace> knownSpaces = new HashMap<Object, ConstraintSpace>();
    private static Map<Object, Long> fileDates = new HashMap<Object, Long>();
    
	/**
	 * @param object The object to search for Ontology EAnnotations.
	 * @param platformkitURI The URI of the Platformkit model.
	 * @param ontologies The list of ontologies to augment.
	 */
	public static void addOntologies(EObject object, URI platformkitURI, EList<String> ontologies) {
		Assert.isNotNull(object);
        EList<EAnnotation> annotations = PlatformKitActionUtil.getEAnnotations(object);
	    if (annotations == null) {
	        return;
	    }
	    Assert.isNotNull(platformkitURI);
	    for (int i = 0; i < annotations.size(); i++) {
	        EAnnotation ann = annotations.get(i);
	        if ("PlatformKit".equals(ann.getSource())) {
	            String ont = ann.getDetails().get("Ontology");
	            if (ont != null) {
	                StringTokenizer onts = new StringTokenizer(ont, "\n");
	                while (onts.hasMoreTokens()) {
	                    ont = onts.nextToken();
	                    ont = PlatformKitActionUtil.translatePath(ont, ann.eResource().getURI(), platformkitURI);
	                    if (!ontologies.contains(ont)) {
	                        ontologies.add(ont);
	                        logger.info("Added ontology " + ont);
	                    }
	                }
	            }
	        }
	    }
	}

	/**
	 * @param product The model of which to search the meta-model for Ontology EAnnotations.
	 * @param platformkitURI The URI of the Platformkit model.
	 * @param ontologies The list of ontologies to augment.
	 */
	public static void addOntologies(Resource product, URI platformkitURI, EList<String> ontologies) {
        TreeIterator<EObject> contents = product.getAllContents();
        while (contents.hasNext()) {
            EObject current = contents.next();
            addOntologies(current.eClass().getEPackage(), platformkitURI, ontologies);
        }
	}

	/**
	 * Translates a path from the source URI location
	 * to the target URI location
	 * @param path The ontology path to translate
	 * @param source The URI on which the path is based
	 * @param target The URI to translate the path to
	 * @return The translated ontology path
	 */
	private static String translatePath(String path, URI source, URI target) {
	    URI pathURI = URI.createURI(path);
	    URI absURI = pathURI.resolve(source);
	    return absURI.deresolve(target, true, true ,true).toString();
	}

	/**
	 * @param object
	 * @return Returns the ID attribute for the given EObject
	 * or the qualified name for an ENamedElement.
	 */
	private static Object getIDFrom(EObject object) {
	    EAttribute attr = PlatformKitActionUtil.getIDAttribute(object.eClass());
	    if (attr != null) {
	        return object.eGet(attr);
	    }
	    //Ecore does not specify 'name' as an ID for its meta-objects.
	    if (object instanceof ENamedElement) {
	        return PlatformKitActionUtil.getLabel((ENamedElement) object);
	    }
	    return null;
	}

	/**
	 * @param type
	 * @return The qualified name of type.
	 */
	private static String getLabel(ENamedElement type) {
	    EObject container = type.eContainer();
	    if (container instanceof ENamedElement) {
	        return getLabel((ENamedElement)container) + "::" + type.getName();
	    } else {
	        return type.getName();                
	    }
	}

	/**
	 * @param metaClass
	 * @return The EAttribute that is flagged as ID or null.
	 */
	private static EAttribute getIDAttribute(EClass metaClass) {
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
	 * @param object
	 * @return the annotations for the given object.
	 */
	private static EList<EAnnotation> getEAnnotations(EObject object) {
	    if (object instanceof EModelElement) {
	        return ((EModelElement) object).getEAnnotations();
	    }
	    return null;
	}

	/**
	 * @param resource The filesystem resource for the cddconfig file.
	 * @return The translated inferred owl file for the given cddconfig file.
	 */
	public static IFile getPreClassifiedOntology(IResource resource) {
	    IPath path = resource.getFullPath().removeFileExtension().addFileExtension("inferred.owl");
	    return resource.getWorkspace().getRoot().getFile(path);
	}

	/**
	 * @param uri The URI for the cddconfig file.
	 * @return The translated inferred owl URI for the given cddconfig URI.
	 */
	public static URI getPreClassifiedOntology(URI uri) {
		return uri.trimFileExtension().appendFileExtension("inferred.owl");
	}

    /**
     * Adds constraints to the given constraint set.
     * @param annotations the annotations to search for context constraints.
     * @param constraints the set of existing constraint strings.
     * @param set the constraint set to augment.
     */
    private static void addConstraints(EList<EAnnotation> annotations, Set<String> constraints, ConstraintSet set) {
        if (annotations == null) {
            return;
        }
        for (int i = 0; i < annotations.size(); i++) {
            EAnnotation ann = (EAnnotation) annotations.get(i);
            if ("PlatformKit".equals(ann.getSource())) {
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
    private static void addConstraints(EAnnotation ann, Set<String> constraints, ConstraintSet set) {
        for (int i = 0; i < ann.getDetails().size(); i++) {
            Entry<String, String> detail = ann.getDetails().get(i);
            if ("PlatformConstraint".equals(detail.getKey())) {
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
     * @return A new ConstraintSet that reflects the given metaobject, provided that the metaobject actually has constraint annotations, null otherwise.
     * @param object
     */
    public static ConstraintSet createMetaObjectConstraintSet(EObject object) {
        EList<EAnnotation> annotations = PlatformKitActionUtil.getEAnnotations(object);
        if (annotations == null) {
            return null;
        }
        Set<String> constraints = new HashSet<String>();
        ConstraintSet set = factory.createConstraintSet();
        Object target = PlatformKitActionUtil.getIDFrom(object);
        if (target != null) {
            set.setName(target.toString());
        }
        addConstraints(annotations, constraints, set);
        if (set.getConstraint().isEmpty()) {
            return null;
        } else {
        	return set;
        }
    }

    /**
     * @return A new ConstraintSet that reflects the given product configuration, provided that its meta-model actually has constraint annotations, null otherwise.
     * @param product The product configuration
     */
    public static ConstraintSet createModelConstraintSet(Resource product) {
        Set<String> constraints = new HashSet<String>();
        ConstraintSet set = factory.createConstraintSet();
        TreeIterator<EObject> contents = product.getAllContents();
        EObject previous = null;
        while (contents.hasNext()) {
            EObject current = contents.next();
            if (previous == null) {
                Object target = PlatformKitActionUtil.getIDFrom(current);
                if (target != null) {
                    set.setName(target.toString());
                }
            }
            EList<EAnnotation> annotations = current.eClass().getEAnnotations();
            addConstraints(annotations, constraints, set);
            previous = current;
        }
        if (set.getConstraint().isEmpty()) {
            return null;
        } else {
        	return set;
        }
    }

    /**
     * @param resource The filesystem resource for the cddconfig file.
     * @return The cached constraint space, if available, null otherwise.
     */
    public static ConstraintSpace getCachedConstraintSpace(IResource resource) {
        IFile file = PlatformKitActionUtil.getPreClassifiedOntology(resource);
        String path = file.getFullPath().toString();
        Long timeStamp = Long.valueOf(file.getModificationStamp());
        if (timeStamp.equals(fileDates.get(path))) {
            logger.info("Using cached constraint space from " + path);
            return (ConstraintSpace) knownSpaces.get(path);
        }
        return null;
    }

    /**
     * @param resource The filesystem resource for the cddconfig file.
     * @param space The constraint space to add to the cache.
     */
    public static void setCachedConstraintSpace(IResource resource, ConstraintSpace space) {
        IFile file = PlatformKitActionUtil.getPreClassifiedOntology(resource);
        String path = file.getFullPath().toString();
        Long timeStamp = Long.valueOf(file.getModificationStamp());
        logger.info("Adding " + path + " to constraint space cache");
        fileDates.put(path, timeStamp);
        knownSpaces.put(path, space);
    }
    
    /**
     * @param resource The filesystem resource for the cddconfig file.
     * @return The cached constraint space, if available, null otherwise.
     */
    public static ConstraintSpace getCachedConstraintSpace(URI resource) {
    	if (isPlatformResourceURI(resource)) {
            IPath cddconfigPath = new Path(toPlatformResourcePath(resource));
            Assert.isNotNull(cddconfigPath);
            IFile cddconfigFile = ResourcesPlugin.getWorkspace().getRoot().getFile(cddconfigPath);
            Assert.isNotNull(cddconfigFile);
            return getCachedConstraintSpace(cddconfigFile);
    	} else {
    		URI uri = PlatformKitActionUtil.getPreClassifiedOntology(resource);
            if (knownSpaces.get(uri) != null) {
                logger.info("Using cached constraint space from " + uri.toString());
                return (ConstraintSpace) knownSpaces.get(uri);
            }
            return null;
    	}
    }

    /**
     * @param resource The filesystem resource for the cddconfig file.
     * @param space The constraint space to add to the cache.
     */
    public static void setCachedConstraintSpace(URI resource, ConstraintSpace space) {
    	if (isPlatformResourceURI(resource)) {
            IPath cddconfigPath = new Path(toPlatformResourcePath(resource));
            Assert.isNotNull(cddconfigPath);
            IFile cddconfigFile = ResourcesPlugin.getWorkspace().getRoot().getFile(cddconfigPath);
            Assert.isNotNull(cddconfigFile);
            setCachedConstraintSpace(cddconfigFile, space);
    	} else {
        	URI uri = PlatformKitActionUtil.getPreClassifiedOntology(resource);
            logger.info("Adding " + uri.toString() + " to constraint space cache");
            knownSpaces.put(uri, space);
    	}
    }
    
    /**
     * @param uri
     * @return True iff uri starts with "platform:/resource/"
     */
    private static boolean isPlatformResourceURI(URI uri) {
    	return ("platform".equals(uri.scheme()) && uri.segmentCount() > 1 && "resource".equals(uri.segment(0)));
    }
    
    /**
     * @param uri
     * @return The url-decoded version of uri with the first 2 segments left off ("platform:/resource").
     */
    public static String toPlatformResourcePath(URI uri) {
        StringBuffer platformResourcePath = new StringBuffer();
        for (int i = 1, size = uri.segmentCount(); i < size; ++i)
        {
          platformResourcePath.append('/');
          platformResourcePath.append(URI.decode(uri.segment(i)));
        }
        return platformResourcePath.toString();
    }
    
    /**
     * @param o
     * @param delim qualifier delimiter, e.g. "::"
     * @return The qualified name for o separated by delim
     */
    public static String qName(ENamedElement o, String delim) {
    	if (o.eContainer() instanceof ENamedElement) {
    		return qName((ENamedElement) o.eContainer(), delim) + delim + o.getName();
    	} else {
    		return o.getName();
    	}
    }
}
