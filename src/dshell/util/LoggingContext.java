package dshell.util;

import java.io.IOException;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.net.SyslogAppender;
import org.apache.log4j.varia.NullAppender;

public class LoggingContext {
	public static enum AppenderType {
		empty,
		file,
		stdout,
		stderr,
		syslog;
	}

	private Layout defaultLayout;
	private final Logger logger;

	private LoggingContext(){
		this.defaultLayout = new PatternLayout("%d{ISO8601}: %m%n");
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
		case file:
			String fileName = options[0];
			if(fileName.startsWith("~")) {
				fileName = System.getenv("HOME") + fileName.substring(1); 
			}
			try {
				appender = new FileAppender(this.defaultLayout, fileName, true);
			}
			catch (IOException e) {
				Utils.fatal(1, "not found log file: " + options[0]);
			}
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
				host = options[0];
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
