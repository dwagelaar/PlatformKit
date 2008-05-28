package be.ac.vub.platformkit.kb;

import java.util.Iterator;

public interface IOntModel {

    /**
     * <p>
     * Answer a resource that represents a class description node in this model. If a resource
     * with the given uri exists in the model, and can be viewed as an IOntClass, return the
     * IOntClass facet, otherwise return null.
     * </p>
     * @param uri The uri for the class node, or null for an anonymous class.
     * @return An IOntClass resource or null.
     */
    public IOntClass getOntClass(String uri);

    /**
     * <p>Answer a resource representing the class that is the intersection of the given list of class descriptions.</p>
     * @param uri The URI of the new intersection class, or null for an anonymous class description.
     * @param members A list of resources denoting the classes that comprise the intersection
     * @return An intersection class description
     */
    public IOntClass createIntersectionClass(String uri, Iterator<IOntClass> members);

}
