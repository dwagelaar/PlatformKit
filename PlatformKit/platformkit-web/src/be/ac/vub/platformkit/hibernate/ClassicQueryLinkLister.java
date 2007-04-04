package be.ac.vub.platformkit.hibernate;

import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;

import be.ac.vub.platformkit.descriptions.PlatformDescription;

public class ClassicQueryLinkLister {

    public ClassicQueryLinkLister() {
    }
    
    public void listContentsOfAllTables() {        
        System.out.println("\nClassicQueryLinkLister:Event:");
        List ps = this.listTable(PlatformDescription.class.getCanonicalName());
		Iterator e_itr = ps.iterator();
		while ( e_itr.hasNext() ) {
			PlatformDescription thePD = (PlatformDescription) e_itr.next();
            System.out.println("Reading PD object: " + thePD.getBrowserID() + ", " + thePD.getBrowserID() + ", " + thePD.getPlatformURI());
        }
        System.out.println("");
    }
    
    private List listTable(String tableName) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List result = session.createQuery("from " + tableName).list();
        return result;
    }

    public void getObjectUsingBrowserID(String tableName, String val) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List ps = session.createQuery("from " + tableName + " where browserID=" + "'" + val + "'").list();
		Iterator e_itr = ps.iterator();
		while ( e_itr.hasNext() ) {
			PlatformDescription thePD = (PlatformDescription) e_itr.next();
            System.out.println("Reading PS object using browserID: " + thePD.getBrowserID() + ", " + thePD.getBrowserID() + ", " + thePD.getPlatformURI());
        }        
    }

    public void getObjectUsingPlatformURI(String tableName, String val) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List ps = session.createQuery("from " + tableName + " where platformURI=" + "'" + val + "'").list();
		Iterator e_itr = ps.iterator();
		while ( e_itr.hasNext() ) {
			PlatformDescription thePD = (PlatformDescription) e_itr.next();
            System.out.println("Reading PS object using platformURI: " + thePD.getBrowserID() + ", " + thePD.getBrowserID() + ", " + thePD.getPlatformURI());
        }        
    }

}