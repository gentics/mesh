package com.gentics.mesh.mock;

import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * Test rule, that will register itself as logger delegate factory for vert.x logging.
 * Every LogDelegate will be a mock, and they will all be stored in a static map, so that tests can get the mocks and can verify specific log messages
 */
public class MockingLoggerRule extends TestWatcher implements SLF4JServiceProvider, ILoggerFactory {

    // to avoid constant folding by the compiler, this field must *not* be final
    public static String REQUESTED_API_VERSION = "2.0.99"; // !final
	/**
	 * Static map of all mocks, which were created
	 */
	protected static Map<String, Logger> mocks = new HashMap<>();

	private final MDCAdapter mdcAdapter = new BasicMDCAdapter();
	private final IMarkerFactory markerFactory = new BasicMarkerFactory();

	/**
	 * When a test is starting, register as logger provider.
	 */
	@Override
	protected void starting(Description description) {
		System.setProperty(LoggerFactory.PROVIDER_PROPERTY_KEY, MockingLoggerRule.class.getName());
	}

	/**
	 * When a test finished, reset all mocks
	 */
	@Override
	protected void finished(Description description) {
		mocks.values().forEach(mock -> Mockito.reset(mock));
	}

	@Override
	public ILoggerFactory getLoggerFactory() {
		return this;
	}

	@Override
	public IMarkerFactory getMarkerFactory() {
		return markerFactory;
	}

	@Override
	public MDCAdapter getMDCAdapter() {
		return mdcAdapter;
	}

	@Override
	public String getRequestedApiVersion() {
		return REQUESTED_API_VERSION;
	}

	@Override
	public void initialize() {
		
	}

	@Override
	public Logger getLogger(String name) {
		return mocks.computeIfAbsent(name, key -> mock(Logger.class));
	}
}
