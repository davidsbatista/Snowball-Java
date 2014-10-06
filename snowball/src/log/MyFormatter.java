package log;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class MyFormatter extends Formatter {

	@Override
	public String format(LogRecord rec) {
		StringBuffer buf = new StringBuffer(5000);
		buf.append(formatMessage(rec));
		buf.append("\n\n");
		return buf.toString();
	}
}