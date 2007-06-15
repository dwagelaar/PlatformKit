package be.ac.vub.platformkit.presentation.logging;

import java.io.OutputStream;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

import be.ac.vub.platformkit.logging.PlatformkitLogFormatter;

/**
 * Flushes after every log and doesn't close output stream.
 * @author dennis
 */
public class ConsoleStreamHandler extends StreamHandler {

    /**
     * Creates a ConsoleStreamHandler for out.
     * @param out
     */
    public ConsoleStreamHandler(OutputStream out) {
        super(out, PlatformkitLogFormatter.INSTANCE);
    }
    
    /**
     * @see StreamHandler#publish(java.util.logging.LogRecord)
     */
    public void publish(LogRecord record) {
        super.publish(record);
        flush();
    }
    
    /**
     * Don't close output stream.
     */
    public void close() {
    	flush();
    }
}
