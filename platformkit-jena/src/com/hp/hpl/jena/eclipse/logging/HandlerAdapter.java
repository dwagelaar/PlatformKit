package com.hp.hpl.jena.eclipse.logging;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import junit.framework.Assert;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class HandlerAdapter extends AppenderSkeleton {

	private Handler handler;
    
    /**
     * @param description A Log4J log level description (e.g. "WARN" or "INFO")
     * @return The {@link Level} equivalent
     */
    public static Level getLevelFor(String description) {
    	Level level = Level.INFO;
    	if ("ALL".equals(description)) {
    		level = Level.ALL;
    	} else if ("DEBUG".equals(description)) {
    		level = Level.FINE;
    	} else if ("ERROR".equals(description)) {
    		level = Level.SEVERE;
    	} else if ("FATAL".equals(description)) {
    		level = Level.SEVERE;
    	} else if ("OFF".equals(description)) {
    		level = Level.OFF;
    	} else if ("TRACE".equals(description)) {
    		level = Level.CONFIG;
    	} else if ("WARN".equals(description)) {
    		level = Level.WARNING;
    	}
    	return level;
    }

    /**
     * Creates a HandlerAdapter.
     * @param handler The handler to write to.
     */
    public HandlerAdapter(Handler handler) {
        Assert.assertNotNull(handler);
        this.handler = handler;
    }
    
    /**
     * Appends given event to the internal handler.
     * @param event
     */
    protected void append(LoggingEvent event) {
        Level level = getLevelFor(event.getLevel().toString());
        LogRecord record = new LogRecord(level, event.getRenderedMessage());
        if (event.getThrowableInformation() != null) {
            record.setThrown(event.getThrowableInformation().getThrowable());
        }
        handler.publish(record);
    }

    /**
     * Calls {@link Handler#close()}
     */
    public void close() {
        handler.close();
    }

    /**
     * @return false
     */
    public boolean requiresLayout() {
        return false;
    }

}
