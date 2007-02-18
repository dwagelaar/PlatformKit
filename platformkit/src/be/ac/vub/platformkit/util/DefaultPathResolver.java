package be.ac.vub.platformkit.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Resolves paths using java.io.File and a basepath string.
 * @author dennis
 *
 */
public class DefaultPathResolver implements PathResolver {

    private String basepath;
    
    public DefaultPathResolver(String basepath) {
        this.basepath = basepath;
    }
    
    public DefaultPathResolver() {
        this("./");
    }
    
    /**
     * @see PathResolver#getContents(String)
     */
    public InputStream getContents(String path) throws IOException {
        File file = new File(basepath + path);
        return new FileInputStream(file);
    }

}
