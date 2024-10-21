package com.gentics.mesh.hibernate;

import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.test.context.PluginHelper;
import com.gentics.mesh.test.context.TestGraphHelper;
import com.gentics.mesh.test.context.TestHttpMethods;
import com.gentics.mesh.test.context.WrapperHelper;
import com.gentics.mesh.test.context.event.EventAsserter;
import com.gentics.mesh.test.util.MeshAssert;
import okhttp3.OkHttpClient;
import org.junit.Before;

/**
 * An alternative to {@link com.gentics.mesh.test.context.AbstractMeshTest} for test classes that want to embed
 * {@link com.gentics.mesh.test.context.MeshTestContext}
 */
public abstract class AbstractTest implements TestHttpMethods, TestGraphHelper, PluginHelper, WrapperHelper {

	private EventAsserter eventAsserter;
	private OkHttpClient httpClient;

	@Before
	public void setupEventAsserter() {
		eventAsserter = new EventAsserter(getTestContext());
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
