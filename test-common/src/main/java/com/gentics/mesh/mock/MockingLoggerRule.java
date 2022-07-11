package com.gentics.mesh.mock;

import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;

import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.logging.LogDelegate;
import io.vertx.core.spi.logging.LogDelegateFactory;

/**
 * Test rule, that will register itself as logger delegate factory for vert.x logging.
 * Every LogDelegate will be a mock, and they will all be stored in a static map, so that tests can get the mocks and can verify specific log messages
 */
public class MockingLoggerRule extends TestWatcher implements LogDelegateFactory {
	/**
	 * Static map of all mocks, which were created
	 */
	protected static Map<String, LogDelegate> mocks = new HashMap<>();

	/**
	 * When a test is starting, register as logger delegate factory
	 */
	@Override
	protected void starting(Description description) {
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, MockingLoggerRule.class.getName());
	}

	/**
	 * When a test finished, reset all mocks
	 */
	@Override
	protected void finished(Description description) {
		mocks.values().forEach(mock -> Mockito.reset(mock));
	}

	/**
	 * Get the LogDelegate (mock) for the given logger
	 * @param name logger name
	 * @return LogDelegate mock
	 */
	public LogDelegate get(String name) {
		return mocks.computeIfAbsent(name, key -> mock(LogDelegate.class));
	}

	@Override
	public LogDelegate createDelegate(String name) {
		return get(name);
	}
}
