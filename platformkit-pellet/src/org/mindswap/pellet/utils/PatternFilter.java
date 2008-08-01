/*
 * Created on Oct 12, 2006
 */
package org.mindswap.pellet.utils;

import java.io.File;
import java.io.FileFilter;

/**
 * File filter implementation that filters by pattern matching on the file name using
 * regular expressions. Two patterns are specified. A file is accepted if its name 
 * matches the first pattern and does NOT match the second pattern.
 * 
 * @author Evren Sirin
 *
 */
public class PatternFilter implements FileFilter {
    private String match;
    private String noMatch;
    
    public PatternFilter( String match ) {
        this.match = match;
        this.noMatch = "";
    }
    
    public PatternFilter( String match, String noMatch ) {
        this.match = match;
        this.noMatch = noMatch;
    }
    
	public boolean accept( File file ) {
		return file.getName().matches( match ) 
            && !file.getName().matches( noMatch );
	}			
}