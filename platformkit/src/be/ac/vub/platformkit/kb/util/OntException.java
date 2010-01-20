package be.ac.vub.platformkit.kb.util;

/**
 * PlatformKit ontology-related exception 
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class OntException extends Exception {

	private static final long serialVersionUID = -1850996948968329662L;

	public OntException() {
		super();
	}

	public OntException(String message) {
		super(message);
	}

	public OntException(Throwable cause) {
		super(cause);
	}

	public OntException(String message, Throwable cause) {
		super(message, cause);
	}

}
