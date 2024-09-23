package com.gentics.mesh.liquibase;

import org.slf4j.LoggerFactory;

import liquibase.logging.Logger;
import liquibase.logging.core.AbstractLogService;

/**
 * LogService implementation, that delegates logging to {@link org.slf4j.Logger}
 */
public class LiquibaseLogService extends AbstractLogService {

	@Override
	public int getPriority() {
		return PRIORITY_DEFAULT;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Logger getLog(Class clazz) {
		return new LiquibaseDelegatingLogger(LoggerFactory.getLogger(clazz), null);
	}
}
