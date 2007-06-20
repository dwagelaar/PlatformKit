package be.ac.vub.platformkit.java.popup.actions;

import java.net.URL;

import be.ac.vub.platformkit.java.PlatformkitJavaPlugin;


public class J2MEMIDP1_0CompatAction extends CompatAction {

	private static final URL apiResource = PlatformkitJavaPlugin.getPlugin().getBundle().getResource("j2me-midp-1_0-api.uml");
	private static final String apiName = "J2ME MIDP 1.0";
	
	public J2MEMIDP1_0CompatAction() {
		super(apiResource, apiName);
	}

}
