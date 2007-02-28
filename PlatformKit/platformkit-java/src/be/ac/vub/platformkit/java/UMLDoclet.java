package be.ac.vub.platformkit.java;

import junit.framework.Assert;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.RootDoc;

public class UMLDoclet extends Doclet implements Runnable {
	
	private RootDoc root = null;

	public UMLDoclet(RootDoc root) {
		Assert.assertNotNull(root);
		this.root = root;
	}
	
	public static boolean start(RootDoc root) {
		UMLDoclet doclet = new UMLDoclet(root);
		doclet.run();
		return true;
	}

	public void run() {
		ClassDoc[] classes = getRoot().classes();
		for (int i = 0; i < classes.length; i++) {
			System.out.println(classes[i]);
		}
	}

	public RootDoc getRoot() {
		return root;
	}
	
}
