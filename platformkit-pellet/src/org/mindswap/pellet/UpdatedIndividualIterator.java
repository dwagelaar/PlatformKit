package org.mindswap.pellet;

import java.util.Iterator;

import aterm.ATermAppl;

public class UpdatedIndividualIterator implements Iterator {
	Iterator i;
	ABox abox;
	
	public UpdatedIndividualIterator( ABox abox ) {
		this.abox = abox;
		i = abox.updatedIndividuals.iterator();
	}

	public boolean hasNext() {
		return i.hasNext();
	}

	public Object next() {
		ATermAppl a = (ATermAppl) i.next();
		return abox.getIndividual( a );
	}

	public void remove() {
		throw new RuntimeException("Remove is not supported");
	}

}
