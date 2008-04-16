package be.ac.vub.platformkit.hibernate;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import be.ac.vub.platformkit.kb.Ontologies;

public class HibernateUtil {

	protected static Logger logger = Logger.getLogger(Ontologies.LOGGER);

	private static SessionFactory sessionFactory = null;

	public static String connectionURL = null;
	public static String connectionUsername = null;
	public static String connectionPassword = null;
	public static String hbm2ddlAuto = null;

	private static SessionFactory createSessionFactory() {
		SessionFactory sessionFactory = null;
		try {
			// Create the SessionFactory from hibernate.cfg.xml
			Configuration cfg = new Configuration().configure();
			if (connectionURL != null) {
				logger.info("Overriding hibernate.connection.url value");
				cfg.setProperty("hibernate.connection.url", connectionURL);
			}
			if (connectionUsername != null) {
				logger.info("Overriding hibernate.connection.username value");
				cfg.setProperty("hibernate.connection.username",
						connectionUsername);
			}
			if (connectionPassword != null) {
				logger.info("Overriding hibernate.connection.password value");
				cfg.setProperty("hibernate.connection.password",
						connectionPassword);
			}
			if (hbm2ddlAuto != null) {
				logger.info("Overriding hibernate.hbm2ddl.auto value");
				cfg.setProperty("hibernate.hbm2ddl.auto", hbm2ddlAuto);
			}
			sessionFactory = cfg.buildSessionFactory();
		} catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			logger.log(Level.SEVERE, "Initial SessionFactory creation failed."
					+ ex.getLocalizedMessage(), ex);
			throw new ExceptionInInitializerError(ex);
		}
		return sessionFactory;
	}

	public static SessionFactory getSessionFactory() {
		if (sessionFactory == null) {
			sessionFactory = createSessionFactory();
		}
		return sessionFactory;
	}

}