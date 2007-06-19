package be.ac.vub.platformkit.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import be.ac.vub.platformkit.kb.Ontologies;

/**
 * Web service interface for PlatformKit.
 * @author dennis
 */
public class PlatformkitServlet extends HttpServlet {

	private static final long serialVersionUID = -8587189401115112481L;

    private Logger logger = Logger.getLogger(Ontologies.LOGGER);
    private Level loglevel = Level.INFO;
    private Map knownSpaces = new HashMap();
    private Map urlDates = new HashMap();
    private Map urlResources = new HashMap();
    private DateFormat dateFormat = new SimpleDateFormat();
    protected ResourceSet resourceSet = new ResourceSetImpl();
    
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
			PlatformkitSession session = new PlatformkitSession(req);
            //pre-calc
            logger.info(DateFormat.getDateInstance().format(new Date()));
            ConstraintSpace space = init(req, session.getBaseURL());
            //calculate most/least specific constraint sets
            logger.info(DateFormat.getDateInstance().format(new Date()));
            List result;
			PlatformDescription pd = session.getDescription();
            synchronized (space) {
                if (pd.getPlatformOWL() != null) {
                	InputStream input = pd.getPlatformOWL().getInputStream();
                    space.getKnowledgeBase().loadInstances(input);
                    input.close();
                }
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
		} catch (Exception e) {
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
     */
    private ConstraintSpace init(HttpServletRequest req, String baseurl)
    throws FileUploadException, IOException, IllegalArgumentException {
        parseLogLevel(getInitParameter("loglevel"));
        logger.info("Request: " + req);
        Long date = new Long(getURLDate(baseurl));
        Resource resource;
        if (urlDates.containsKey(baseurl)) {
            Long oldDate = (Long) urlDates.get(baseurl);
            if (date.equals(oldDate)) {
                logger.info("Using cached constraint space");
                return (ConstraintSpace) knownSpaces.get(baseurl);
            } else {
                logger.info("Different constraint space detected - unloading cached resource");
                resource = (Resource) urlResources.get(baseurl);
                Assert.assertNotNull(resource);
                resource.unload();
            }
        }
        resource = loadModel(baseurl);
        if (resource.getContents().size() == 0) {
        	throw new IOException("Resource at " + baseurl + " contains no elements");
        }
        ConstraintSpace space = (ConstraintSpace) resource.getContents().get(0);
        synchronized (this) {
            urlResources.put(baseurl, resource);
            urlDates.put(baseurl, date);
            knownSpaces.put(baseurl, space);
        }
        synchronized (space) {
        	Ontologies ont = new Ontologies();
        	space.setKnowledgeBase(ont);
        	space.init(true);
        	ont.attachPelletReasoner();
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
        logger.info("Config date: " + dateFormat.format(new Date(conn.getLastModified())));
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
