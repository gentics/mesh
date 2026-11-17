package com.gentics.mesh.mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerFactoryFriend;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * Test rule, that will register itself as logger delegate factory for vert.x logging.
 * Every LogDelegate will be a mock, and they will all be stored in a static map, so that tests can get the mocks and can verify specific log messages.
 * If the rule is created with names of loggers to record, only those loggers record their invocations (and can be verified),
 * all other loggers are stub-only mocks, which do not record invocations and therefore do not accumulate memory.
 */
public class MockingLoggerRule extends TestWatcher implements SLF4JServiceProvider, ILoggerFactory {

	private static final AtomicBoolean inited = new AtomicBoolean();

	/**
	 * Names of the loggers, which record their invocations. If empty, all loggers record their invocations.
	 * Must be static, because SLF4J creates its own instance of this provider.
	 */
	private static final Set<String> recordedLoggers = ConcurrentHashMap.newKeySet();

    // to avoid constant folding by the compiler, this field must *not* be final
    public static String REQUESTED_API_VERSION = "2.0.99"; // !final
	/**
	 * Static map of all mocks, which were created
	 */
	protected static Map<String, Logger> mocks = new ConcurrentHashMap<>();

	private final MDCAdapter mdcAdapter = new BasicMDCAdapter();
	private final IMarkerFactory markerFactory = new BasicMarkerFactory();

	/**
	 * Create an instance, which does not restrict the recording loggers. Also used by SLF4J to instantiate the provider.
	 */
	public MockingLoggerRule() {
	}

	/**
	 * Create an instance, which lets only the given loggers record their invocations.
	 * @param recordedLoggers names of the loggers to record
	 */
	public MockingLoggerRule(String... recordedLoggers) {
		MockingLoggerRule.recordedLoggers.addAll(Arrays.asList(recordedLoggers));
	}

	/**
	 * Reset this rule.
	 */
	public void reset() {
		mocks.values().forEach(mock -> Mockito.reset(mock));
	}

	/**
	 * When a test is starting, register as logger provider.
	 */
	@Override
	protected void starting(Description description) {
		initialize();
	}

	/**
	 * When a test finished, reset all mocks
	 */
	@Override
	protected void finished(Description description) {
		reset();
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
		System.setProperty(LoggerFactory.PROVIDER_PROPERTY_KEY, MockingLoggerRule.class.getName());
		if (!inited.get()) {
			LoggerFactoryFriend.reset();
			inited.set(true);
		}
	}

	@Override
	public Logger getLogger(String name) {
		return mocks.computeIfAbsent(name, key -> recordedLoggers.isEmpty() || recordedLoggers.contains(key)
				? mock(Logger.class)
				: mock(Logger.class, withSettings().stubOnly()));
	}
}
