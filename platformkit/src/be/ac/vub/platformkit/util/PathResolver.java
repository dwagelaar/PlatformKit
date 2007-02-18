package be.ac.vub.platformkit.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Resolves relative paths to input streams pointing at file contents.
 * @author dennis
 *
 */
public interface PathResolver {
    
    /**
     * @param path relative filename path.
     * @return an input stream pointing to the contents of the file.
     * @throws IOException if the path could not be resolved to an input stream.
     */
    public InputStream getContents(String path) throws IOException;

}
