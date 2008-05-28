package be.ac.vub.platformkit.kb.owlapi;

import java.io.IOException;

import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.IOntologiesFactory;

public class OWLAPIOntologiesFactory implements IOntologiesFactory {

	public IOntologies createIOntologies() throws IOException {
		return new OWLAPIOntologies();
	}

}
