/**
 * 
 */
package be.ac.vub.platformkit;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Provides shared resources for PlatformKit.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformkitResources {

	private static final ResourceBundle resourceBundle =
		ResourceBundle.getBundle("be.ac.vub.platformkit.messages"); //$NON-NLS-1$

	/**
	 * Not meant to be instantiated.
	 */
	private PlatformkitResources() {
		super();
	}

	/**
	 * @param key
	 * @return The (translated) string for the given key, or the key if not available.
	 */
	public static String getString(String key) {
		try {
			return resourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * @return the resourcebundle
	 */
	public static ResourceBundle getResourcebundle() {
		return resourceBundle;
	}

}