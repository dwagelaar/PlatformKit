package be.ac.vub.platformkit.examples;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import be.ac.vub.platformkit.kb.IOntologies;
import be.ac.vub.platformkit.presentation.util.INamedOntologyProvider;

public class ExamplesOntologyProvider implements INamedOntologyProvider {
    
    private static final String[] ontologies = new String[] {
        "codamos_2007_01/Generic/JDK1.1PC.owl",
        "codamos_2007_01/Generic/JDK1.2PC.owl",
        "codamos_2007_01/Generic/JDK1.3PC.owl",
        "codamos_2007_01/Generic/JDK1.4PC.owl",
        "codamos_2007_01/Generic/JDK1.5PC.owl",
        "codamos_2007_01/Generic/JDK1.6PC.owl",
        "codamos_2007_01/Generic/PersonalJava1.1PocketPC.owl",
        "codamos_2007_01/Generic/J2MEPP1.0PocketPC.owl",
        "codamos_2007_01/Generic/J2MEPP1.1PocketPC.owl",
        "codamos_2007_01/Generic/J2MEMIDP1.0Phone.owl",
        "codamos_2007_01/Generic/J2MEMIDP2.0Phone.owl",
        "codamos_2007_01/Sharp/ZaurusSL-C1000PP.owl",
        "codamos_2007_01/Sharp/ZaurusSL-C1000Jeode.owl",
        "codamos_2007_01/Siemens/CX70v.owl",
        "codamos_2007_01/Nokia/NokiaN800Jalimo.owl"
    };
    
    private static final String[] names = new String[] {
        "JDK 1.1 PC",
        "JDK 1.2 PC",
        "JDK 1.3 PC",
        "JDK 1.4 PC",
        "JDK 1.5 PC",
        "JDK 1.6 PC",
        "Microsoft PocketPC with Personal Java 1.1",
        "Microsoft PocketPC with J2ME PP 1.0",
        "Microsoft PocketPC with J2ME PP 1.1",
        "Mobile phone with J2ME MIDP 1.0",
        "Mobile phone with J2ME MIDP 2.0",
        "Sharp Zaurus SL-C1000 PDA with J2ME PP 1.0",
        "Sharp Zaurus SL-C1000 PDA with Jeode Personal Java 1.1",
        "Siemens CX70v mobile phone with J2ME MIDP 2.0",
        "Nokia N800 internet tablet with Jalimo Java 1.4 + SWT 3.0"
    };
    
    protected static Bundle bundle = Platform.getBundle("be.ac.vub.platformkit.examples");
    protected static Logger logger = Logger.getLogger(IOntologies.LOGGER);

    public static ExamplesOntologyProvider INSTANCE = new ExamplesOntologyProvider();

    public InputStream[] getOntologies() throws IOException {
        InputStream[] streams = new InputStream[ontologies.length];
        for (int i = 0; i < ontologies.length; i++) {
            URL resource = null;
            if (bundle == null) {
                resource = ExamplesOntologyProvider.class.getResource("../../../../../" + ontologies[i]);
            } else {
                resource = bundle.getResource(ontologies[i]);
            }
            if (resource == null) {
            	streams[i] = new FileInputStream("../platformkit-examples/" + ontologies[i]);
            } else {
                streams[i] = resource.openStream();
            }
            Assert.assertNotNull(streams[i]);
            logger.fine("Providing ontology " + ontologies[i] + " as " + streams[i]);
        }
        return streams;
    }

    public String[] getOntologyNames() {
        return names;
    }
    
}
