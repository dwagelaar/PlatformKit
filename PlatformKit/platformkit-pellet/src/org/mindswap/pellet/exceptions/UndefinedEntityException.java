package org.mindswap.pellet.exceptions;

/**
 * <p>
 * Title: UndefinedEntityException
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 */
public class UndefinedEntityException extends RuntimeException {

	public UndefinedEntityException() {
		super();
	}

	public UndefinedEntityException(String e) {
		super( e );
	}
}
