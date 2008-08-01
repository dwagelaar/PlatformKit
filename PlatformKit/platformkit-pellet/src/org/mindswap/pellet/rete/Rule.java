package org.mindswap.pellet.rete;

import java.util.ArrayList;
import java.util.List;

public class Rule {
	public List body, head;
	public Rule() {
		body = new ArrayList();
		head = new ArrayList();
	}
	
	public Rule( List body, List head ) {
		this.body = body;
		this.head = head;
	}
	
	public String toString() {
		return body + " => " + head;
	}
}
