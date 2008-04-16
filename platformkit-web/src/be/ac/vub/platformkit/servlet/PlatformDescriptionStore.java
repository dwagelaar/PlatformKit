package be.ac.vub.platformkit.servlet;

import java.util.*;
import be.ac.vub.platformkit.hibernate.*;
import org.hibernate.Session;
import java.util.regex.*;

public class PlatformDescriptionStore {
	protected static final java.util.regex.Pattern quotes = Pattern
			.compile("'");

	protected static final java.lang.String escapedQuote = "\\'";

	public be.ac.vub.platformkit.servlet.PlatformDescription getPlatformDescription(
			java.lang.String browserID) {
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		session.beginTransaction();
		Matcher m = quotes.matcher(browserID);
		List ps = session.createQuery(
				"from " + PlatformDescription.class.getCanonicalName()
						+ " where browserID=" + "'"
						+ m.replaceAll(escapedQuote) + "'").list();
		Iterator e_itr = ps.iterator();
		while (e_itr.hasNext()) {
			return (PlatformDescription) e_itr.next();
		}
		return null;
	}

	public void storePlatformDescription(
			be.ac.vub.platformkit.servlet.PlatformDescription pd) {
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		session.beginTransaction();
		session.save(pd);
		session.getTransaction().commit();
	}

}
