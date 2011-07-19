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
package be.ac.vub.platformkit.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Adapter class for writing to {@link IFile}s via the {@link OutputStream} interface.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class IFileOutputStream extends ByteArrayOutputStream {

	/**
	 * Introduces nested exceptions on {@link IOException}.
	 * @author <a href="mailto:dennis.wagelaar@vub.ac.be">Dennis Wagelaar</a>
	 */
	private static class WrappedIOException extends IOException {

		private static final long serialVersionUID = 8054573470333348243L;

		/**
		 * Creates a new {@link WrappedIOException}.
		 * @param cause the nested exception
		 */
		public WrappedIOException(Throwable cause) {
			super();
			initCause(cause);
		}

	}
	
	private static final int OUTPUTSIZE = 512*1024; // 512 KB

	private final IFile target;
	private final IProgressMonitor monitor;

	/**
	 * Creates a new {@link IFileOutputStream}.
	 * @param target the file to write to
	 */
	public IFileOutputStream(IFile target) {
		this(target, null, OUTPUTSIZE);
	}

	/**
	 * Creates a new {@link IFileOutputStream}.
	 * @param target the file to write to
	 * @param monitor the progress monitor to use during file writing
	 */
	public IFileOutputStream(IFile target, IProgressMonitor monitor) {
		this(target, monitor, OUTPUTSIZE);
	}

	/**
	 * Creates a new {@link IFileOutputStream}.
	 * @param target the file to write to
	 * @param monitor the progress monitor to use during file writing
	 * @param size the internal buffer size
	 */
	public IFileOutputStream(IFile target, IProgressMonitor monitor, int size) {
		super(size);
		this.target = target;
		this.monitor = monitor;
	}

	/**
	 * @return the target
	 */
	public IFile getTarget() {
		return target;
	}

	/**
	 * @return the monitor
	 */
	public IProgressMonitor getMonitor() {
		return monitor;
	}

	/**
	 * The file contents are written on close.
	 * Calling {@link #close()} multiple times will overwrite the file with the
	 * entire contents of the internal buffer.
	 */
	@Override
	public void close() throws IOException {
		super.close();
		final IFile dest = getTarget();
		final InputStream is = new ByteArrayInputStream(toByteArray());
		try {
			if (dest.exists()) {
				dest.setContents(is, true, true, getMonitor());
			} else {
				dest.create(is, true, getMonitor());
			}
		} catch (CoreException e) {
			throw new WrappedIOException(e);
		}
	}

}
