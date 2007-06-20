package be.ac.vub.platformkit.java.popup.actions;

import java.net.URL;

import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;


public class PersonalJava1_1CompatAction extends CompatAction {

	private static final URL apiResource = PlatformkitJavaPlugin.getPlugin().getBundle().getResource("personaljava-1_1-api.uml");
	private static final String apiName = "Personal Java 1.1";
	
	public PersonalJava1_1CompatAction() {
		super(apiResource, apiName);
	}

}
