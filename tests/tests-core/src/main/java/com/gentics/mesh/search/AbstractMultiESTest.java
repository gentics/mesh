package com.gentics.mesh.search;

import java.lang.annotation.Annotation;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.test.AWSTestMode;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.SSLTestMode;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestContext;
import com.gentics.mesh.test.context.PluginHelper;
import com.gentics.mesh.test.context.TestGraphHelper;
import com.gentics.mesh.test.context.TestHttpMethods;
import com.gentics.mesh.test.context.event.EventAsserter;
import com.gentics.mesh.test.util.MeshAssert;

import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import okhttp3.OkHttpClient;

public abstract class AbstractMultiESTest implements TestHttpMethods, TestGraphHelper, PluginHelper {

	static {
		// Use slf4j instead of JUL
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
	}

	private OkHttpClient httpClient;

	private static ElasticsearchTestMode currentMode = null;

	@Parameters(name = "{index}: ({0})")
	public static Collection<Object[]> esVersions() {
		return Arrays.asList(new Object[][] {
			{ ElasticsearchTestMode.CONTAINER_ES6 },
			{ ElasticsearchTestMode.CONTAINER_ES7 },
		});
	}

	private EventAsserter eventAsserter;

	private static final MeshTestContext testContext = new MeshTestContext();
	public static Runnable cleanupAction;
	public MeshTestSetting settings;

	// Shutdown hook to catch the last test and invoke the teardown once.
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (cleanupAction != null) {
				cleanupAction.run();
			}
		}));
	}

	public AbstractMultiESTest(ElasticsearchTestMode elasticsearch) throws Exception {
		// Invoke tear down once when the setting changes
		if (currentMode != null && currentMode != elasticsearch) {
			getTestContext().tearDownOnce(settings);
		}
		MeshTestSetting clazzAnnotation = getClass().getAnnotation(MeshTestSetting.class);
		this.settings = new MeshTestSettingProxy(clazzAnnotation, elasticsearch);
		this.eventAsserter = new EventAsserter(testContext);

		// Invoke setup once the first time and when the setting changes
		if (currentMode == null || currentMode != elasticsearch) {
			getTestContext().setupOnce(settings);
		}
		AbstractMultiESTest.currentMode = elasticsearch;
		// Register the cleanup action with the current settings.
		cleanupAction = () -> {
			try {
				getTestContext().tearDownOnce(settings);
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
	}

	@Before
	public void setup() throws Throwable {
		getTestContext().setup(settings);
	}

	@After
	public void tearDown() throws Throwable {
		getTestContext().tearDown(settings);
	}

	/**
	 * Proxy for the class annotations. The proxy allows to override the ES setting.
	 */
	public class MeshTestSettingProxy implements MeshTestSetting {

		private MeshTestSetting delegate;
		private ElasticsearchTestMode elasticsearch;

		public MeshTestSettingProxy(MeshTestSetting clazzAnnotation, ElasticsearchTestMode elasticsearch) {
			this.delegate = clazzAnnotation;
			this.elasticsearch = elasticsearch;
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return delegate.annotationType();
		}

		@Override
		public ElasticsearchTestMode elasticsearch() {
			return elasticsearch;
		}

		@Override
		public AWSTestMode awsContainer() {
			return delegate.awsContainer();
		}

		@Override
		public TestSize testSize() {
			return delegate.testSize();
		}

		@Override
		public boolean startServer() {
			return delegate.startServer();
		}

		@Override
		public boolean inMemoryDB() {
			return delegate.inMemoryDB();
		}

		@Override
		public boolean startStorageServer() {
			return delegate.startStorageServer();
		}

		@Override
		public boolean useKeycloak() {
			return delegate.useKeycloak();
		}

		@Override
		public boolean clusterMode() {
			return delegate.clusterMode();
		}

		@Override
		public SSLTestMode ssl() {
			return delegate.ssl();
		}

		@Override
		public boolean monitoring() {
			return delegate.monitoring();
		}

		@Override
		public MeshCoreOptionChanger optionChanger() {
			return delegate.optionChanger();
		}

		@Override
		public Class<? extends MeshOptionChanger> customOptionChanger() {
			return delegate.customOptionChanger();
		}
	}

	@Override
	public MeshTestContext getTestContext() {
		return testContext;
	}

	@Override
	public OkHttpClient httpClient() {
		if (this.httpClient == null) {
			int timeout;
			try {
				timeout = MeshAssert.getTimeout();
				this.httpClient = new OkHttpClient.Builder()
					.writeTimeout(timeout, TimeUnit.SECONDS)
					.readTimeout(timeout, TimeUnit.SECONDS)
					.connectTimeout(timeout, TimeUnit.SECONDS)
					.build();
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
		}
		return this.httpClient;
	}

	@Override
	public EventAsserter eventAsserter() {
		return eventAsserter;
	}
}
