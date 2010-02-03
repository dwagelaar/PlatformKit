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
package be.ac.vub.platformkit.util;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.Assert;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.ExtensibleURIConverterImpl;

/**
 * Resolves relative paths using an EMF URI base.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class EMFURIPathResolver implements PathResolver {
	private ExtensibleURIConverterImpl converter = new ExtensibleURIConverterImpl();
	private URI base;

	/**
	 * Creates an EMFURIPathResolver
	 * @param base the base URI to start navigating relative paths from.
	 */
	public EMFURIPathResolver(URI base) {
		Assert.assertNotNull(base);
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
