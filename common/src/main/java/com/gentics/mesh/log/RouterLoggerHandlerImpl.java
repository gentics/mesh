package com.gentics.mesh.log;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.impl.LoggerHandlerImpl;

/**
 * Improved implementation of Vert.x logger handler
 */
public class RouterLoggerHandlerImpl extends LoggerHandlerImpl {

	private static final Logger LOG = LoggerFactory.getLogger(RouterLoggerHandlerImpl.class);

	public RouterLoggerHandlerImpl(boolean immediate, LoggerFormat format) {
		super(immediate, format);
	}

	public RouterLoggerHandlerImpl(LoggerFormat format) {
		super(format);
	}

	@Override
	protected void doLog(int status, String message) {
		if (status >= 500) {
			LOG.error(message);
		} else {
			LOG.info(message);
		}
	}
}
