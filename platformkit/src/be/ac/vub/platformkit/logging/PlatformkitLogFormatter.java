package be.ac.vub.platformkit.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Log formatter that is less verbose than {@link SimpleFormatter}
 * @author Dennis Wagelaar <dennis.wagelaar@vub.ac.be>
 */
public class PlatformkitLogFormatter extends Formatter {
	
	public static PlatformkitLogFormatter INSTANCE = new PlatformkitLogFormatter();
	
	protected PlatformkitLogFormatter() {
		super();
	}

	public String format(LogRecord record) {
		StringBuffer line = new StringBuffer();
		line.append(record.getLevel().getLocalizedName());
		line.append(": ");
		line.append(record.getMessage());
		line.append('\n');
		if (record.getThrown() != null) {
			StringWriter writer = new StringWriter();
			record.getThrown().printStackTrace(new PrintWriter(writer, true));
			line.append(writer.toString());
		}
		return line.toString();
	}

}
