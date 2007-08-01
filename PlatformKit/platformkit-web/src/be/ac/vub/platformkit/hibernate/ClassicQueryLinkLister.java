package be.ac.vub.platformkit.hibernate;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.hibernate.Session;

import be.ac.vub.platformkit.kb.Ontologies;
import be.ac.vub.platformkit.servlet.PlatformDescription;
import be.ac.vub.platformkit.servlet.PlatformDescriptionStore;

public class ClassicQueryLinkLister {

    private static Logger logger = Logger.getLogger(Ontologies.LOGGER);

    public ClassicQueryLinkLister() {
    }
    
    public void listContentsOfAllTables() {        
        System.out.println("\nClassicQueryLinkLister:Event:");
        List ps = this.listTable(PlatformDescription.class.getCanonicalName());
		Iterator e_itr = ps.iterator();
		while ( e_itr.hasNext() ) {
			PlatformDescription thePD = (PlatformDescription) e_itr.next();
            System.out.println("Reading PD object: " + thePD.getBrowserID() + ", " + thePD.getBrowserID() + ", " + thePD.getData());
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
            System.out.println("Reading PS object using browserID: " + thePD.getBrowserID() + ", " + thePD.getBrowserID() + ", " + thePD.getData());
        }        
    }

    public void getObjectUsingPlatformURI(String tableName, String val) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List ps = session.createQuery("from " + tableName + " where platformOWL=" + "'" + val + "'").list();
		Iterator e_itr = ps.iterator();
		while ( e_itr.hasNext() ) {
			PlatformDescription thePD = (PlatformDescription) e_itr.next();
            System.out.println("Reading PS object using platformURI: " + thePD.getBrowserID() + ", " + thePD.getBrowserID() + ", " + thePD.getData());
        }        
    }
    
    public void registerKnownPlatforms() throws IOException {
    	logger.info("Registering known platforms");
    	PlatformDescriptionStore store = new PlatformDescriptionStore();
    	PlatformDescription pd = new PlatformDescription();
    	
    	// Apple MacOS X has Java 1.5 built in
    	URL resource = ClassicQueryLinkLister.class.getResource("ontology/codamos_2007_01/JDK1.5PC.owl");
    	Assert.assertNotNull(resource);
    	pd.setBrowserID("Mozilla/5.0 (Macintosh; U; Intel Mac OS X; nl; rv:1.8.1.4) Gecko/20070515 Firefox/2.0.0.4");
    	pd.setFromInputStream(resource.openStream());
    	store.storePlatformDescription(pd);
    	pd.setBrowserID("Mozilla/5.0 (Macintosh; U; Intel Mac OS X; nl-nl) AppleWebKit/419 (KHTML, like Gecko) Safari/419.3");
    	store.storePlatformDescription(pd);
    	pd.setBrowserID("Mozilla/5.0 (Macintosh; U; Intel Mac OS X; en-US; rv:1.8.1.4) Gecko/20070515 Firefox/2.0.0.4");
    	store.storePlatformDescription(pd);
    	pd.setBrowserID("Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en) AppleWebKit/419 (KHTML, like Gecko) Safari/419.3");
    	store.storePlatformDescription(pd);
    	pd.setBrowserID("Mozilla/5.0 (Macintosh; U; Intel Mac OS X; en) AppleWebKit/419 (KHTML, like Gecko) Safari/419.3");
    	store.storePlatformDescription(pd);
    	pd.setBrowserID("Mozilla/5.0 (Macintosh; U; Intel Mac OS X; nl; rv:1.8.1.5) Gecko/20070713 Firefox/2.0.0.5");
    	store.storePlatformDescription(pd);
    	pd.setBrowserID("Mozilla/5.0 (Macintosh; U; Intel Mac OS X; en-US; rv:1.8.1.5) Gecko/20070713 Firefox/2.0.0.5");
    	store.storePlatformDescription(pd);
    	pd.setBrowserID("Mozilla/5.0 (Macintosh; U; Intel Mac OS X; nl; rv:1.8.1.6) Gecko/20070725 Firefox/2.0.0.6");
    	store.storePlatformDescription(pd);
    	pd.setBrowserID("Mozilla/5.0 (Macintosh; U; Intel Mac OS X; en-US; rv:1.8.1.6) Gecko/20070725 Firefox/2.0.0.6");
    	store.storePlatformDescription(pd);
    	
    	// Siemens CX70v mobile phone
    	resource = ClassicQueryLinkLister.class.getResource("ontology/codamos_2007_01/CX70v.owl");
    	Assert.assertNotNull(resource);
    	pd.setBrowserID("SIE-CX7V");
    	pd.setFromInputStream(resource.openStream());
    	store.storePlatformDescription(pd);

    	// Emulator
    	resource = ClassicQueryLinkLister.class.getResource("ontology/codamos_2007_01/J2MEMIDP2.0Phone.owl");
    	Assert.assertNotNull(resource);
    	pd.setBrowserID("Profile/MIDP-2.0 Configuration/CLDC-1.1");
    	pd.setFromInputStream(resource.openStream());
    	store.storePlatformDescription(pd);
    }
    
    public static void main(String[] args) {
    	try {
        	ClassicQueryLinkLister l = new ClassicQueryLinkLister();
        	l.registerKnownPlatforms();
    	} catch (Exception e) {
    		logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
    	}
    }

}