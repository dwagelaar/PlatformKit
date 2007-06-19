package be.ac.vub.platformkit.servlet;

import java.util.*;
import be.ac.vub.platformkit.hibernate.*;
import org.hibernate.Session;

public class PlatformDescriptionStore {
public be.ac.vub.platformkit.servlet.PlatformDescription getPlatformDescription(java.lang.String browserID) {
Session session = HibernateUtil.getSessionFactory().getCurrentSession();
session.beginTransaction();
List ps = session.createQuery("from " + PlatformDescription.class.getCanonicalName() + " where browserID=" + "'" + browserID + "'").list();
Iterator e_itr = ps.iterator();
while ( e_itr.hasNext() ) {
	return (PlatformDescription) e_itr.next();
}
return null;
}

public void storePlatformDescription(be.ac.vub.platformkit.servlet.PlatformDescription pd) {
Session session = HibernateUtil.getSessionFactory().getCurrentSession();
session.beginTransaction();
session.save(pd);
session.getTransaction().commit();
}

}

