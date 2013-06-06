package ca.brood.brootils.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/*
 * Note this is not quite production ready yet, but the main
 * function shows that it does work as intended.
 * TODO: - Look into using log4j NDC for getting more info from
 * the caller
 * - Set a max number of messages to store
 * - What happens when logIdCounter gets too big?
 */

public class LoggingLimiter {
	private static Logger log;
	private static AtomicInteger logIdCounter;
	private static Map<Integer, LogMessage> messages;
	static {
		logIdCounter = new AtomicInteger(1);
		messages = new HashMap<Integer, LogMessage>();
		log = Logger.getLogger(LoggingLimiter.class);
	}
	
	public static void main(String[] args) {
		PropertyConfigurator.configure("logger.config");
		
		Logger l1 = Logger.getLogger("Logger 1");
		Logger l2 = Logger.getLogger("Logger 2");
		
		
		int l1id = 0;
		int l2id = 0;
		
		for (int i=1; i<=100; i++) {
			l1id = LoggingLimiter.log(l1id, l1, Level.WARN, "Test warning", null);
			if (Math.random() < 0.95) {
				l2id = LoggingLimiter.log(l2id, l2, Level.FATAL, "FAILED!", null);
			} else {
				LoggingLimiter.purgeLog(l2id, Level.INFO, "SUCCESS - error cleared");
			}
			
			if (i%10==0) {
				LoggingLimiter.logAllMessages();
			}
			
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				
			}
		}
	}
	
	public static synchronized int log(int id, Logger l, Level level, String message, Throwable throwable) {
		if (id == 0) 
			id = logIdCounter.getAndIncrement();
		
		if (messages.containsKey(id)) {
			LogMessage theMessage = messages.get(id);
			theMessage.count++;
		} else {
			LogMessage n = new LogMessage();
			n.log = l;
			n.message = message;
			n.level = level;
			n.throwable = throwable;
			n.timestamp = System.currentTimeMillis();
			n.count = 1l;
			messages.put(id, n);
			logMessageBasic(n);
		}
		
		return id;
	}
	
	public static synchronized void logAllMessages() {
		log.info("Currently repeating log messages: ");
		long currentTime = System.currentTimeMillis();
		for (Integer i : messages.keySet()) {
			logMessageExtended(messages.get(i), currentTime);
		}
	}
	
	public static synchronized void purgeLog(int id, Level level, String message) {
		LogMessage m = messages.remove(id);
		if (m != null)
			logMessage(m.log, level, message, null);
	}
	
	public static synchronized void purgeLog(int id) {
		messages.remove(id);
	}
	
	private static void logMessageExtended(LogMessage m, long currentTime) {
		logMessage(m.log, m.level, "(Seen message "+m.count+
				(m.count>1?" times":" time")+" in the last "+(currentTime-m.timestamp)+
				" milliseconds) "+m.message, m.throwable);
	}
	
	private static void logMessageBasic(LogMessage m) {
		logMessage(m.log, m.level, m.message, m.throwable);
	}
	
	private static void logMessage(Logger log, Level level, String message, Throwable t) {
		switch (level.toInt()) {
		case Level.TRACE_INT:
			if (t == null)
				log.trace(message);
			else
				log.trace(message, t);
			break;
		case Level.DEBUG_INT:
			if (t == null)
				log.debug(message);
			else
				log.debug(message, t);
			break;
		case Level.INFO_INT:
			if (t == null)
				log.info(message);
			else
				log.info(message, t);
			break;
		case Level.WARN_INT:
			if (t == null)
				log.warn(message);
			else
				log.warn(message, t);
			break;
		case Level.ERROR_INT:
			if (t == null)
				log.error(message);
			else
				log.error(message, t);
			break;
		case Level.FATAL_INT:
			if (t == null)
				log.fatal(message);
			else
				log.fatal(message, t);
			break;
		}
	}
	
	private static class LogMessage {
		public String message;
		public Logger log;
		public Level level;
		public Throwable throwable;
		public Long timestamp;
		public Long count;
	}
}
