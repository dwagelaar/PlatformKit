package be.ac.vub.platformkit.hibernate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;
import be.ac.vub.platformkit.examples.ExamplesOntologyProvider;
import be.ac.vub.platformkit.logging.PlatformkitLogger;
import be.ac.vub.platformkit.servlet.PlatformDescription;
import be.ac.vub.platformkit.servlet.PlatformDescriptionStore;

/**
 * Populates the database with known platform descriptions.
 * @author dennis
 *
 */
public class PopulateDatabase {

	protected static Logger logger = Logger.getLogger(PlatformkitLogger.LOGGER);

	private Properties knownPlatforms = null;

	/**
	 * Populates the database
	 * @param args [<connection.url>, <connection.password>]
	 */
	public static void main(String[] args) {
		try {
			HibernateUtil.connectionURL = args[0];
			HibernateUtil.connectionPassword = args[1];
			HibernateUtil.hbm2ddlAuto = "create";
			PopulateDatabase p = new PopulateDatabase();
			p.registerKnownPlatforms();
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
	}

	/**
	 * @throws IOException
	 */
	public void registerKnownPlatforms() throws IOException {
		logger.info("Registering known platforms");
		final ExamplesOntologyProvider provider = ExamplesOntologyProvider.INSTANCE;
		final String[] ontologyNames = provider.getOntologyNames();
		final InputStream[] ontologies = provider.getOntologies();

		for (int i = 0; i < ontologyNames.length; i++) {
			registerKnownPlatform(ontologyNames[i], ontologies[i]);
		}
	}

	/**
	 * Stores all known platform descriptions that share the current ontology name.
	 * @param currentOntName The current ontology name (e.g. "JDK 1.5 PC")
	 * @param ontology The current ontology
	 * @throws IOException
	 */
	public void registerKnownPlatform(String currentOntName,
			InputStream ontology) throws IOException {
		final Properties knownPlatforms = getKnownPlatforms();
		final PlatformDescriptionStore store = new PlatformDescriptionStore();
		final PlatformDescription pd = new PlatformDescription();
		Assert.assertNotNull(ontology);
		pd.setFromInputStream(ontology);

		for (Enumeration<?> e = knownPlatforms.propertyNames(); e
				.hasMoreElements();) {
			String key = (String) e.nextElement();
			String value = knownPlatforms.getProperty(key);
			if (currentOntName.equals(value)) {
				logger.info("Registering known platform " + key);
				pd.setBrowserID(key);
				store.storePlatformDescription(pd);
			}
		}
	}

	/**
	 * @return The known platforms properties file.
	 * @throws IOException
	 */
	public Properties getKnownPlatforms() throws IOException {
		if (knownPlatforms == null) {
			knownPlatforms = new Properties();
			final URL url = PopulateDatabase.class
					.getResource("KnownPlatforms.properties.xml");
			knownPlatforms.loadFromXML(url.openStream());
		}
		return knownPlatforms;
	}

}
