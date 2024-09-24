package com.gentics.mesh.liquibase;

import java.util.logging.Level;

import org.slf4j.Logger;
import liquibase.logging.LogMessageFilter;
import liquibase.logging.core.AbstractLogger;

/**
 * Logger implementation that delegates logging to an instance of {@link Logger}
 */
public class LiquibaseDelegatingLogger extends AbstractLogger {
	/**
	 * Logger to delegate to
	 */
	protected Logger logger;

	/**
	 * Create an instance
	 * @param logger logger to delegate to
	 * @param filter message filter
	 */
	public LiquibaseDelegatingLogger(Logger logger, LogMessageFilter filter) {
		super(filter);
		this.logger = logger;
	}

	@Override
	public void log(Level level, String message, Throwable e) {
		if (level.intValue() >= Level.SEVERE.intValue()) {
			logger.error("FATAL:" + message, e);
		} else if (level.intValue() >= Level.WARNING.intValue()) {
			logger.warn(message, e);
		} else if (level.intValue() >= Level.INFO.intValue()) {
			logger.info(message, e);
		} else if (level.intValue() >= Level.CONFIG.intValue()) {
			logger.debug(message, e);
		} else {
			logger.trace(message, e);
		}
	}
}
