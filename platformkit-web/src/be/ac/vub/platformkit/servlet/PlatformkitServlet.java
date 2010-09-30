/*******************************************************************************
 * Copyright (c) 2005-2010 Dennis Wagelaar, Vrije Universiteit Brussel.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dennis Wagelaar, Vrije Universiteit Brussel
 *******************************************************************************/
package be.ac.vub.platformkit.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.apache.commons.fileupload.FileUploadException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import be.ac.vub.platformkit.ConstraintSet;
import be.ac.vub.platformkit.ConstraintSpace;
import be.ac.vub.platformkit.PlatformkitPackage;
import be.ac.vub.platformkit.hibernate.HibernateUtil;
import be.ac.vub.platformkit.java.JavaOntologyProvider;
import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.kb.IOntologiesFactory;
import be.ac.vub.platformkit.kb.owlapi.OWLAPIOntologiesFactory;
import be.ac.vub.platformkit.kb.util.OntException;
import be.ac.vub.platformkit.logging.PlatformkitLogger;

/**
 * Web service interface for PlatformKit.
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformkitServlet extends HttpServlet {

	private static final long serialVersionUID = -8587189401115112481L;

	private static final IOntologiesFactory FACTORY = new OWLAPIOntologiesFactory();

	private static Logger logger = Logger.getLogger(PlatformkitLogger.LOGGER);
	private Level loglevel = null;
	private Map<String, ConstraintSpacePool> knownSpaces = new HashMap<String, ConstraintSpacePool>();
	private DateFormat dateFormat = new SimpleDateFormat();

	static {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
				"platformkit", new XMIResourceFactoryImpl());
		PlatformkitPackage packageInstance = PlatformkitPackage.eINSTANCE;
		Assert.assertNotNull(packageInstance);
	}

	/**
	 * Processes any request to the web service.
	 */
	protected void doAny(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			synchronized (this) {
				if (loglevel == null) {
					parseLogLevel(getInitParameter("loglevel"));
				}
				if (HibernateUtil.connectionPassword == null) {
					HibernateUtil.connectionPassword = getInitParameter("connection.password");
				}
			}
			PlatformkitSession session = new PlatformkitSession(req);
			//pre-calc
			logger.info(DateFormat.getDateInstance().format(new Date()));
			ConstraintSpace space = init(req, session.getBaseURL());
			//calculate most/least specific constraint sets
			logger.info(DateFormat.getDateInstance().format(new Date()));
			List<ConstraintSet> result = Collections.emptyList();
			PlatformDescription pd = session.getDescription();
			if (pd.getData() != null) {
				InputStream input = pd.getInputStream();
				space.getKnowledgeBase().loadInstances(input);
				input.close();
				if (session.getLeastSpecific()) {
					result = space.getLeastSpecific(!session.getNoValidate());
				} else {
					result = space.getMostSpecific(!session.getNoValidate());
				}
			}
			logger.info(DateFormat.getDateInstance().format(new Date()));
			String redirect = getContainer(session.getBaseURL());
			if (result.size() > 0) {
				redirect += ((ConstraintSet) result.get(0)).getName();
			} else {
				redirect += "none/";
			}
			logger.info("Redirecting to " + redirect);
			resp.sendRedirect(redirect);
			space.getKnowledgeBase().unloadInstances();
			freeConstraintSpace(space, session.getBaseURL());
		} catch (Throwable e) {
			logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw new ServletException(e);
		}
	}

	/**
	 * Processes a GET request to the web service.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doAny(req, resp);
	}

	/**
	 * Processes a POST request to the web service.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doAny(req, resp);
	}

	/**
	 * Parses and set the log level.
	 * @param logLevel
	 * @throws IllegalArgumentException
	 */
	private void parseLogLevel(String logLevel) throws IllegalArgumentException {
		if (logLevel.equals("DEBUG")) {
			loglevel = Level.FINE;
		} else if (logLevel.equals("INFO")) {
			loglevel = Level.INFO;
		} else if (logLevel.equals("WARN")) {
			loglevel = Level.WARNING;
		} else if (logLevel.equals("ERROR")) {
			loglevel = Level.SEVERE;
		} else if (logLevel.equals("OFF")) {
			loglevel = Level.OFF;
		} else {
			throw new IllegalArgumentException("Invalid log level: " + logLevel);
		}
		logger.setLevel(loglevel);
	}

	/**
	 * Initialises the reasoning environment with all parameters.
	 * @param req the HTTP request object
	 * @param baseurl The base URL of the application to configure
	 * @throws FileUploadException
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws OntException 
	 */
	private ConstraintSpace init(HttpServletRequest req, String baseurl)
			throws FileUploadException, IOException, IllegalArgumentException,
			OntException {
		logger.info("Request: " + req);
		Date date = new Date(getURLDate(baseurl));
		ConstraintSpace space = getConstraintSpace(baseurl, date);
		if (space.getKnowledgeBase() == null) {
			IOntologies ont = FACTORY.createIOntologies();
			ont.addLocalOntologies(JavaOntologyProvider.INSTANCE);
			space.setKnowledgeBase(ont);
			space.init(true);
			ont.attachDLReasoner();
		}
		return space;
	}

	/**
	 * Returns space to its pool, so that it can be reused by new clients. 
	 * @param space The constraint space to return to the pool
	 * @param baseurl The base URL of the Platformkit Model
	 */
	private void freeConstraintSpace(ConstraintSpace space, String baseurl) {
		synchronized (this) {
			logger.info("Returning constraint space to the pool");
			ConstraintSpacePool pool = knownSpaces.get(baseurl);
			pool.addSpace(space);
		}
	}

	/**
	 * @param baseurl The base URL of the Platformkit Model
	 * @param date The last modification timestamp 
	 * @return The constraint space object
	 * @throws FileUploadException
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	private ConstraintSpace getConstraintSpace(String baseurl, Date date)
			throws FileUploadException, IOException, IllegalArgumentException {
		synchronized (this) {
			ConstraintSpace space = findCachedConstraintSpace(baseurl, date);
			if (space == null) {
				logger.info("Loading new constraint space");
				Resource resource = loadModel(baseurl);
				if (resource.getContents().size() == 0) {
					throw new IOException("Resource at " + baseurl
							+ " contains no elements");
				}
				space = (ConstraintSpace) resource.getContents().get(0);
				if (!knownSpaces.containsKey(baseurl)) {
					knownSpaces.put(baseurl, new ConstraintSpacePool());
					// don't add space yet, since it isn't free
				}
				ConstraintSpacePool pool = knownSpaces.get(baseurl);
				Assert.assertNotNull(pool);
				pool.setLastModified(date);
			}
			return space;
		}
	}

	/**
	 * @param baseurl The base URL of the Platformkit Model
	 * @param date The last modification timestamp 
	 * @return The cached constraint space from a pool, if any
	 */
	private ConstraintSpace findCachedConstraintSpace(String baseurl, Date date) {
		ConstraintSpace space = null;
		ConstraintSpacePool pool = knownSpaces.get(baseurl);
		if (pool != null) {
			if (date.equals(pool.getLastModified())) {
				logger.info("Checking cached constraint space pool");
				if (pool.getSpaces().hasMoreElements()) {
					logger.info("Using cached constraint space from the pool");
					space = (ConstraintSpace) pool.getSpaces().nextElement();
					pool.removeSpace(space);
				}
			} else {
				logger
						.info("Different constraint space detected - purging pool");
				while (pool.getSpaces().hasMoreElements()) {
					space = (ConstraintSpace) pool.getSpaces().nextElement();
					pool.removeSpace(space);
					ResourceSet resourceSet = space.eResource()
							.getResourceSet();
					resourceSet.getResources().remove(space.eResource());
				}
				space = null;
			}
		}
		return space;
	}

	/**
	 * @param baseurl
	 * @return the last modified date from the base URL.
	 * @throws IOException
	 */
	private long getURLDate(String baseurl) throws IOException {
		checkBaseURL(baseurl);
		URL baseURL = new URL(baseurl);
		URLConnection conn = baseURL.openConnection();
		logger.info("Config date: "
				+ dateFormat.format(new Date(conn.getLastModified())));
		return conn.getLastModified();
	}

	/**
	 * Loads the Ecore model from the base URL.
	 * @param baseurl
	 * @param config The config properties to load.
	 * @return the Ecore model
	 * @throws IOException
	 */
	private Resource loadModel(String baseurl) throws IOException {
		checkBaseURL(baseurl);
		URI source = URI.createURI(baseurl);
		ResourceSet resourceSet = new ResourceSetImpl();
		return resourceSet.getResource(source, true);
	}

	/**
	 * @param baseurl
	 * @throws IOException if the baseurl is illegal
	 */
	private void checkBaseURL(String baseurl) throws IOException {
		if (!baseurl.startsWith(getInitParameter("allowed-url-base"))) {
			throw new IOException("Forbidden base url: " + baseurl);
		}
	}

	/**
	 * @param url
	 * @return The container/path portion of the URL.
	 * @throws MalformedURLException
	 */
	private static String getContainer(String url) throws MalformedURLException {
		URL thisURL = new URL(url);
		String baseurl = thisURL.toString();
		return baseurl.substring(0, baseurl.lastIndexOf('/') + 1);
	}

}
