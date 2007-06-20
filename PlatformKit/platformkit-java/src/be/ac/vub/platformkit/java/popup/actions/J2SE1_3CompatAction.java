package be.ac.vub.platformkit.java.popup.actions;

import java.net.URL;

import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;


public class J2SE1_3CompatAction extends CompatAction {

	private static final URL apiResource = PlatformkitJavaPlugin.getPlugin().getBundle().getResource("j2se-1_3-api.uml");
	private static final String apiName = "J2SE 1.3";
	
	public J2SE1_3CompatAction() {
		super(apiResource, apiName);
	}

}
