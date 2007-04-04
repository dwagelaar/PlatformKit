package be.ac.vub.platformkit.hibernate;

import org.hibernate.Session;

import be.ac.vub.platformkit.descriptions.PlatformDescription;

public class StartupManager {

    public static void main(String[] args) {
        StartupManager mgr = new StartupManager();
        
        if (args[0].equals("Populate")) {
        	mgr.store(mgr);
        }
        else {
            System.out.println("Unkown args[0] Argument!");
    	}

        if (args[1].equals("cl")) {
	        //List all persistent objects and their links using queries. There are no links for Dennis.
        	String tableName = PlatformDescription.class.getCanonicalName();
	        ClassicQueryLinkLister cl = new ClassicQueryLinkLister();
	        cl.listContentsOfAllTables();
	        cl.getObjectUsingBrowserID(tableName, "myBroswerID 3"); 
	        cl.getObjectUsingPlatformURI(tableName, "myPlatformURI 4"); 
        }
        else {
            System.out.println("Unkown args[1] Argument!");
    	}

        HibernateUtil.getSessionFactory().close();
    }

    //Convenience methods
    private void store(StartupManager mgr) {
    	mgr.createAndStorePlatformSpecification("myBrowserID 1" , "myPlatformURI 1");
    	mgr.createAndStorePlatformSpecification("myBrowserID 2" , "myPlatformURI 2");
    	mgr.createAndStorePlatformSpecification("myBrowserID 3" , "myPlatformURI 3");
    	mgr.createAndStorePlatformSpecification("myBrowserID 4" , "myPlatformURI 4");
    	mgr.createAndStorePlatformSpecification("myBrowserID 5" , "myPlatformURI 5");
    	mgr.createAndStorePlatformSpecification("myBrowserID 6" , "myPlatformURI 6");
    }

    private String createAndStorePlatformSpecification(String bID, String pURI) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        PlatformDescription thePS = new PlatformDescription();
        thePS.setBrowserID(bID);
        thePS.setPlatformURI(pURI);        
        session.save(thePS);
        session.getTransaction().commit();
        System.out.println("Storing PD object: " + thePS.getBrowserID() + ", " + thePS.getPlatformURI());
        return thePS.getBrowserID();
    }

}