/*
 * $Id: CDDServlet.java 2517 2005-11-03 16:19:29Z dwagelaa $
 * Created on 1-sep-2005
 * (C) 2005, Dennis Wagelaar, SSEL, VUB
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2 as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  (See the file "COPYING" that is included with this source distribution.)
 */
package be.ac.vub.cddtoolkit.webservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import be.ac.vub.cddtoolkit.core.ConstraintList;
import be.ac.vub.cddtoolkit.core.ConstraintSpace;
import be.ac.vub.cddtoolkit.core.Ontologies;
import be.ac.vub.cddtoolkit.core.util.ResourceException;
import be.ac.vub.cddtoolkit.emf.cddconfig.CddconfigPackage;

/**
 * Web service interface for the CDDToolkit.
 * @author dennis
 */
public class CDDServlet extends HttpServlet {
    static final long serialVersionUID = 20060104;
    private static final int EXT_LENGTH = "cddconfig".length();
    
    private Logger logger = Logger.getLogger(Ontologies.LOGGER);
    private Level loglevel = Level.INFO;
    private Map knownSpaces = new HashMap();
    private Map urlDates = new HashMap();
    private Map urlResources = new HashMap();
    private DateFormat dateFormat = new SimpleDateFormat();
    protected ResourceSet resourceSet = new ResourceSetImpl();
    
    static {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
                "cddconfig", new XMIResourceFactoryImpl());
        CddconfigPackage packageInstance = CddconfigPackage.eINSTANCE;
        Assert.assertNotNull(packageInstance);
    }

    /**
     * Processes a POST request to the web service.
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            Properties pars = new Properties();
            InputStream context = parseParameters(req, pars);
            boolean leastSpecific = pars.getProperty("result").equals("leastspecific");
            boolean noValidate = pars.getProperty("noValidate").equals("true");
            String baseurl = pars.getProperty("baseurl");
            //pre-calc
            logger.info(DateFormat.getDateInstance().format(new Date()));
            ConstraintSpace space = init(req, baseurl);
            //calculate most/least specific constraint sets
            logger.info(DateFormat.getDateInstance().format(new Date()));
            List result;
            synchronized (space) {
                if (context != null) {
                    space.getKnowledgeBase().loadInstances(context);
                }
                if (leastSpecific) {
                    result = space.getLeastSpecific(!noValidate);
                } else {
                    result = space.getMostSpecific(!noValidate);
                }
            }
            logger.info(DateFormat.getDateInstance().format(new Date()));
            String redirect = getContainer(baseurl);
            if (result.size() > 0) {
                redirect += ((ConstraintList) result.get(0)).getId();
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
    throws FileUploadException, IOException, IllegalArgumentException, ResourceException {
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
        ConstraintSpace space = new ConstraintSpace(new Ontologies());
        synchronized (this) {
            urlResources.put(baseurl, resource);
            urlDates.put(baseurl, date);
            knownSpaces.put(baseurl, space);
        }
        synchronized (space) {
            space.readFromCDDConfig(resource, loadPreclassifiedOntology(baseurl));
            space.initNoDIG();
        }
        return space;
    }
    
    /**
     * Extracts the parameters into the given variable and returns
     * the context ontology input stream, if any.
     * @param req
     * @param pars
     * @return The context ontology input stream, if any, null otherwise.
     */
    private InputStream parseParameters(HttpServletRequest req, Properties pars)
            throws FileUploadException, IOException {
        InputStream context = null;
        if (FileUpload.isMultipartContent(req)) {
            DiskFileUpload fu = new DiskFileUpload();
            List fileItems = fu.parseRequest(req);
            for (Iterator fis = fileItems.iterator(); fis.hasNext();) {
                FileItem fi = (FileItem) fis.next();
                if (fi.isFormField()) {
                    pars.setProperty(fi.getFieldName(), fi.getString());
                } else {
                    context = fi.getInputStream();
                }
            }
        } else {
            logger.warning("No context file uploaded");
            for (Enumeration ns = req.getParameterNames(); ns.hasMoreElements();) {
                String name = (String) ns.nextElement();
                pars.setProperty(name, req.getParameter(name));
            }
        }
        return context;
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
     * @param baseurl URL of the cddconfig file
     * @return input stream to the preclassified ".inferred.owl" ontology
     * @throws IOException
     */
    private InputStream loadPreclassifiedOntology(String baseurl) throws IOException {
        checkBaseURL(baseurl);
        String ontUrl = baseurl.substring(0, baseurl.length() - EXT_LENGTH) + "inferred.owl";
        URL ontURL = new URL(ontUrl);
        return ontURL.openStream();
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
