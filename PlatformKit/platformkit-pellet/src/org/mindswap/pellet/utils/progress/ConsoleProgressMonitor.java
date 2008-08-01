/*
 * Created on Oct 20, 2005
 */
package org.mindswap.pellet.utils.progress;


/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: Clark & Parsia, LLC. <http://www.clarkparsia.com></p>
 *
 * @author Evren Sirin
 */
public class ConsoleProgressMonitor extends AbstractProgressMonitor implements ProgressMonitor {    
    private String lastMessage = "";
    
    public boolean CONSOLE = System.getProperty( "console", "true" ).equals( "true" );
        
    public ConsoleProgressMonitor() {        
    }
    
    public ConsoleProgressMonitor( int length ) {
        setProgressLength( length );
    }
    
    protected void resetProgress() {
        super.resetProgress();       
        
        lastMessage = "";
    }

	public void taskStarted() {
        super.taskStarted();
        
        System.out.println( progressTitle + " " + progressLength + " elements" );
	}

    protected void updateProgress() {
        int pc = (int) ((100.0*progress) / progressLength);
        
        if( pc == progressPercent )
            return;
        
        progressPercent = pc;
        
        if( CONSOLE ) {
	        for( int i = 0; i < lastMessage.length(); i++ )
	            System.out.print( '\b' );        
        }	                
	        
        lastMessage = progressTitle + ": " + progressMessage + " " + pc + "% complete in " + calcElapsedTime();
        
        if( CONSOLE ) {
        	System.out.print( lastMessage );
        }
        else if( pc > 0 && pc % 10 == 0 ) {
	        System.out.println( lastMessage );
        }        
    }
    

    private String calcElapsedTime() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        long timeInSeconds = elapsedTime / 1000;
        
        long hours, minutes, seconds;
        hours = timeInSeconds / 3600;
        timeInSeconds = timeInSeconds - (hours * 3600);
        minutes = timeInSeconds / 60;
        timeInSeconds = timeInSeconds - (minutes * 60);
        seconds = timeInSeconds;
        
        StringBuffer buffer = new StringBuffer();
        buffer.append( pad( hours ) );
        buffer.append( ':' );
        buffer.append( pad( minutes ) );
        buffer.append( ':' );
        buffer.append( pad( seconds ) );

        return buffer.toString();
    }
    
    private String pad( long value ) {
        String str = (value < 10) ? "0" : "";
        return str + value;
    }
    
    public void taskFinished() {
        setProgress( progressLength );
        
        if( CONSOLE )
        	System.out.println();        
        System.out.println( progressTitle + " finished" );
    }
}
