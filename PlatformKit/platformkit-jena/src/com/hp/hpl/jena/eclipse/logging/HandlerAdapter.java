package com.hp.hpl.jena.eclipse.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import junit.framework.Assert;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class HandlerAdapter extends AppenderSkeleton {
    private Handler handler;
    private static Map levelMap;
    
    static {
        levelMap = new HashMap();
        levelMap.put(org.apache.log4j.Level.ALL, java.util.logging.Level.ALL);
        levelMap.put(org.apache.log4j.Level.DEBUG, java.util.logging.Level.FINE);
        levelMap.put(org.apache.log4j.Level.ERROR, java.util.logging.Level.SEVERE);
        levelMap.put(org.apache.log4j.Level.FATAL, java.util.logging.Level.SEVERE);
        levelMap.put(org.apache.log4j.Level.INFO, java.util.logging.Level.INFO);
        levelMap.put(org.apache.log4j.Level.OFF, java.util.logging.Level.OFF);
        levelMap.put(org.apache.log4j.Level.TRACE, java.util.logging.Level.CONFIG);
        levelMap.put(org.apache.log4j.Level.WARN, java.util.logging.Level.WARNING);
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
        Level level = (Level) levelMap.get(event.getLevel());
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
