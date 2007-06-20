package be.ac.vub.platformkit.java.popup.actions;

import java.net.URL;

import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;


public class JDK1_1CompatAction extends CompatAction {

	private static final URL apiResource = PlatformkitJavaPlugin.getPlugin().getBundle().getResource("jdk-1_1-api.uml");
	private static final String apiName = "JDK 1.1";
	
	public JDK1_1CompatAction() {
		super(apiResource, apiName);
	}

}
