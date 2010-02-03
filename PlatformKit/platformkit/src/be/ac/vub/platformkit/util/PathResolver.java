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

/**
 * Resolves relative paths to input streams pointing at file contents.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public interface PathResolver {

	/**
	 * @param path relative filename path.
	 * @return an input stream pointing to the contents of the file.
	 * @throws IOException if the path could not be resolved to an input stream.
	 */
	public InputStream getContents(String path) throws IOException;

}
