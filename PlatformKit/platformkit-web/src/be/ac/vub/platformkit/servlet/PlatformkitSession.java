package be.ac.vub.platformkit.servlet;

import be.ac.vub.platformkit.kb.Ontologies;
import java.io.*;
import java.util.logging.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.util.Streams;
import java.util.*;

public class PlatformkitSession {
	protected java.util.Properties parameters = new Properties();

	private boolean leastSpecific = false;

	private boolean noValidate = false;

	private java.lang.String baseURL = null;

	protected javax.servlet.http.HttpServletRequest req = null;

	protected java.util.logging.Logger logger = Logger
			.getLogger(Ontologies.LOGGER);

	private be.ac.vub.platformkit.servlet.PlatformDescription description = null;

	protected be.ac.vub.platformkit.servlet.PlatformDescriptionStore store = new PlatformDescriptionStore();

	public PlatformkitSession(javax.servlet.http.HttpServletRequest req)
			throws org.apache.commons.fileupload.FileUploadException,
			java.io.IOException, java.sql.SQLException {
		description = new PlatformDescription();
		description.setBrowserID(req.getHeader("User-Agent"));
		if (ServletFileUpload.isMultipartContent(req)) {
			logger.info("Retrieving uploaded ontology file");
			ServletFileUpload upload = new ServletFileUpload();
			FileItemIterator fileItems = upload.getItemIterator(req);
			while (fileItems.hasNext()) {
				FileItemStream item = fileItems.next();
				InputStream stream = item.openStream();
				if (item.isFormField()) {
					parameters.setProperty(item.getFieldName(), Streams
							.asString(stream));
				} else {
					description.setFromInputStream(stream);
				}
			}
		} else {
			logger.warning("No platform ontology file uploaded");
			if (req.getParameterNames().hasMoreElements()) {
				for (Enumeration ns = req.getParameterNames(); ns
						.hasMoreElements();) {
					String name = (String) ns.nextElement();
					parameters.setProperty(name, req.getParameter(name));
				}
			} else {
				StringTokenizer query = new StringTokenizer(req
						.getQueryString(), "&");
				while (query.hasMoreTokens()) {
					StringTokenizer par = new StringTokenizer(
							query.nextToken(), "=");
					parameters.setProperty(par.nextToken(), par.nextToken());
				}
			}
			PlatformDescription pd = store.getPlatformDescription(description
					.getBrowserID());
			if (pd != null) {
				logger.info("Standard platform ontology found for: \""
						+ pd.getBrowserID() + "\"");
				description = pd;
			} else {
				logger.info("No standard platform ontology found for: \""
						+ description.getBrowserID() + "\"");
			}
		}
		logger.info(parameters.toString());
		setLeastSpecific(parameters.getProperty("result").equals(
				"leastspecific"));
		setNoValidate(parameters.getProperty("noValidate").equals("true"));
		setBaseURL(parameters.getProperty("baseurl"));
	}

	public boolean getLeastSpecific() {
		return leastSpecific;
	}

	public void setLeastSpecific(boolean leastSpecific) {
		this.leastSpecific = leastSpecific;
	}

	public boolean getNoValidate() {
		return noValidate;
	}

	public void setNoValidate(boolean noValidate) {
		this.noValidate = noValidate;
	}

	public java.lang.String getBaseURL() {
		return baseURL;
	}

	public void setBaseURL(java.lang.String baseURL) {
		this.baseURL = baseURL;
	}

	public be.ac.vub.platformkit.servlet.PlatformDescription getDescription() {
		return description;
	}

	public void setDescription(
			be.ac.vub.platformkit.servlet.PlatformDescription description) {
		this.description = description;
	}

}
