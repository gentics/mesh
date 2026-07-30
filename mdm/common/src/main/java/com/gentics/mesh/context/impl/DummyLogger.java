package com.gentics.mesh.context.impl;

import io.vertx.core.spi.logging.LogDelegate;

/**
 * Dummy logger which does not log anything
 */
public class DummyLogger implements LogDelegate {

	@Override
	public boolean isWarnEnabled() {
		return false;
	}

	@Override
	public boolean isInfoEnabled() {
		return false;
	}

	@Override
	public boolean isDebugEnabled() {
		return false;
	}

	@Override
	public boolean isTraceEnabled() {
		return false;
	}

	@Override
	public void error(Object message) {

	}

	@Override
	public void error(Object message, Throwable t) {

	}

	@Override
	public void warn(Object message) {

	}

	@Override
	public void warn(Object message, Throwable t) {

	}

	@Override
	public void info(Object message) {

	}

	@Override
	public void info(Object message, Throwable t) {

	}

	@Override
	public void debug(Object message) {

	}

	@Override
	public void debug(Object message, Throwable t) {

	}

	@Override
	public void trace(Object message) {

	}

	@Override
	public void trace(Object message, Throwable t) {

	}

	@Override
	public String implementation() {
		return "NOOP";
	}
}
