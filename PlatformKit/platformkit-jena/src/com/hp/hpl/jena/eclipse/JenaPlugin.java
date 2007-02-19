package com.hp.hpl.jena.eclipse;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.eclipse.ui.plugin.*;
import org.osgi.framework.BundleContext;

import com.hp.hpl.jena.eclipse.logging.HandlerAdapter;

import java.util.*;
import java.util.logging.Handler;

/**
 * The main plugin class to be used in the desktop.
 */
public class JenaPlugin extends AbstractUIPlugin {
	//The shared instance.
	private static JenaPlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	
	/**
	 * The constructor.
	 */
	public JenaPlugin() {
		super();
		plugin = this;
        LogManager.getRootLogger().setLevel(Level.WARN);
		try {
			resourceBundle = ResourceBundle.getBundle("com.hp.hpl.jena.eclipse.JenaPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static JenaPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = JenaPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
    
    /**
     * Adds the given log handler to the known loggers.
     * @param handler
     */
    public void addLogHandler(Handler handler) {
        LogManager.getRootLogger().addAppender(new HandlerAdapter(handler));
    }
}
