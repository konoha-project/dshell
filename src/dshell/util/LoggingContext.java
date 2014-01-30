package dshell.util;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.net.SyslogAppender;
import org.apache.log4j.varia.NullAppender;

public class LoggingContext {
	public static enum AppenderType {
		empty,
		stdout,
		stderr,
		syslog;
	}

	private Layout defaultLayout;
	private final Logger logger;

	private LoggingContext(){
		this.defaultLayout = new SimpleLayout();
		this.logger = Logger.getLogger(LoggingContext.class);
		BasicConfigurator.configure(new NullAppender());
	}

	public Logger getLogger() {
		return this.logger;
	}

	public void changeAppender(AppenderType type, String... options) {
		BasicConfigurator.resetConfiguration();
		Appender appender = new NullAppender();
		switch(type) {
		case empty:
			appender = new NullAppender();
			break;
		case stdout:
			appender = new ConsoleAppender(this.defaultLayout, "System.out");
			break;
		case stderr:
			appender = new ConsoleAppender(this.defaultLayout, "System.err");
			break;
		case syslog:
			String host = "localhost";
			if(options.length > 0) {
				host = options[1];
			}
			appender = new SyslogAppender(this.defaultLayout, host, SyslogAppender.LOG_LOCAL0);
			break;
		default:
			break;
		}
		BasicConfigurator.configure(appender);
	}

	private static class ContextHolder {
		private static final LoggingContext context = new LoggingContext();
	}
	public static LoggingContext getContext() {
		return ContextHolder.context;
	}
}
