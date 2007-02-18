package be.ac.vub.platformkit.presentation.util;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.URIConverterImpl;
import org.eclipse.jface.util.Assert;

import be.ac.vub.platformkit.util.PathResolver;

/**
 * Resolves relative paths using an EMF URI base.
 * @author dennis
 *
 */
public class EMFURIPathResolver implements PathResolver {
	private URIConverterImpl converter = new URIConverterImpl();
    private URI base;

    /**
     * Creates an EMFURIPathResolver
     * @param base the base URI to start navigating relative paths from.
     */
    public EMFURIPathResolver(URI base) {
        Assert.isNotNull(base);
        this.base = base;
    }
    
    /**
     * @see PathResolver#getContents(String)
     */
    public InputStream getContents(String path) throws IOException {
      	URI uri = URI.createURI(path);
       	URI absuri = uri.resolve(base);
        return converter.createInputStream(absuri);
    }

}
