package be.ac.vub.platformkit.kb.jena;

import java.io.IOException;

import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.IOntologiesFactory;

public class JenaOntologiesFactory implements IOntologiesFactory {

	public IOntologies createIOntologies() throws IOException {
		return new JenaOntologies();
	}

}
