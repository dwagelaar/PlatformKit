package be.ac.vub.platformkit.servlet;

import java.io.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.util.Streams;
import java.util.*;
import be.ac.vub.platformkit.logging.*;

public class PlatformkitSession {
	private java.util.Properties parameters = new Properties();

	private boolean leastSpecific = false;

	private boolean noValidate = false;

	private java.lang.String baseURL = null;

	private be.ac.vub.platformkit.servlet.PlatformDescription description = null;

	private be.ac.vub.platformkit.servlet.PlatformDescriptionStore store = new PlatformDescriptionStore();

	public PlatformkitSession(javax.servlet.http.HttpServletRequest req)
			throws org.apache.commons.fileupload.FileUploadException,
			java.io.IOException, java.sql.SQLException {
		final Properties parameters = getParameters();
		setDescription(new PlatformDescription());
		final PlatformDescription description = getDescription();
		description.setBrowserID(req.getHeader("User-Agent"));
		if (ServletFileUpload.isMultipartContent(req)) {
			PlatformkitLogger.logger.info("Retrieving uploaded ontology file");
			ServletFileUpload upload = new ServletFileUpload();
			FileItemIterator fileItems = upload.getItemIterator(req);
			description.setData(new byte[0]);
			while (fileItems.hasNext()) {
				FileItemStream item = fileItems.next();
				InputStream stream = item.openStream();
				if (item.isFormField()) {
					if ("context".equals(item.getFieldName())
							&& (description.getData().length == 0)) {
						description.setFromInputStream(stream);
						PlatformkitLogger.logger
								.info("platform description data set from context (length = "
										+ description.getData().length + ")");
					} else {
						parameters.setProperty(item.getFieldName(), Streams
								.asString(stream));
					}
				} else if (description.getData().length == 0) {
					description.setFromInputStream(stream);
					PlatformkitLogger.logger
							.info("platform description data set from context file (length = "
									+ description.getData().length + ")");
				}
			}
		} else {
			PlatformkitLogger.logger
					.warning("No platform ontology file uploaded");
			final Enumeration<?> ns = req.getParameterNames();
			if (ns.hasMoreElements()) {
				while (ns.hasMoreElements()) {
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
			final PlatformDescription pd = getStore().getPlatformDescription(
					description.getBrowserID());
			if (pd != null) {
				PlatformkitLogger.logger
						.info("Standard platform ontology found for: \""
								+ pd.getBrowserID() + "\"");
				setDescription(pd);
			} else {
				PlatformkitLogger.logger
						.info("No standard platform ontology found for: \""
								+ description.getBrowserID() + "\"");
			}
		}
		PlatformkitLogger.logger.info(parameters.toString());
		setLeastSpecific(parameters.getProperty("result").equals(
				"leastspecific"));
		setNoValidate(parameters.getProperty("noValidate").equals("true"));
		setBaseURL(parameters.getProperty("baseurl"));
	}

	public java.util.Properties getParameters() {
		return parameters;
	}

	public void setParameters(java.util.Properties parameters) {
		this.parameters = parameters;
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

	public be.ac.vub.platformkit.servlet.PlatformDescriptionStore getStore() {
		return store;
	}

	public void setStore(
			be.ac.vub.platformkit.servlet.PlatformDescriptionStore store) {
		this.store = store;
	}

}
