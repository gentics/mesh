package com.gentics.mesh.plugin;

import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.RestAPIVersion;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;

import io.reactivex.Completable;
import io.vertx.core.eventbus.MessageConsumer;
import okhttp3.OkHttpClient;

/**
 * Test plugin which will
 * <ol>
 * <li>Invoke the OrientDB backup and fail immediately, when initialized the first time</li>
 * <li>Succeed initialization for every further attempt</li>
 * </ol>
 */
public class BackupPlugin extends AbstractPlugin {
	protected static AtomicBoolean firstStart = new AtomicBoolean(true);

	/**
	 * Create instance
	 * @param wrapper plugin wrapper
	 * @param env environment
	 */
	public BackupPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable initialize() {
		if (firstStart.getAndSet(false)) {
			return invokeBackupAndFail();
		} else {
			return Completable.complete();
		}
	}

	/**
	 * Invoke the OrientDB back (using a client with timeout set to 1 ms), wait for the CLUSTER_DATABASE_CHANGE_STATUS event and then fail
	 * @return failing completable
	 */
	protected Completable invokeBackupAndFail() {
		OkHttpClient okHttpClient = new OkHttpClient.Builder()
				.callTimeout(Duration.ofMillis(1))
				.connectTimeout(Duration.ofMillis(1))
				.writeTimeout(Duration.ofMillis(1))
				.readTimeout(Duration.ofMillis(1))
				.build();

		int port = environment().options().getHttpServerOptions().getPort();
		String host = "127.0.0.1";
		MeshRestClient client = MeshRestClient.create(MeshRestClientConfig.newConfig()
			.setPort(port)
			.setHost(host)
			.setBasePath(RestAPIVersion.V1.getBasePath())
			.build(), okHttpClient);

		client.setAPIKey(environment().adminToken());

		return client.invokeBackup().toCompletable().onErrorResumeNext(t -> waitForEvent(10_000))
				.andThen(Completable.error(new RuntimeException()));
	}

	/**
	 * Wait for the event CLUSTER_DATABASE_CHANGE_STATUS
	 * @param timeoutMs timeout
	 * @return completable
	 */
	protected Completable waitForEvent(int timeoutMs) {
		return Completable.fromAction(() -> {
			CountDownLatch latch = new CountDownLatch(1);
			MessageConsumer<Object> consumer = vertx().eventBus().consumer(CLUSTER_DATABASE_CHANGE_STATUS.address);
			consumer.handler(msg -> latch.countDown());
			// The completion handler will be invoked once the consumer has been registered
			consumer.completionHandler(res -> {
				if (res.failed()) {
					throw new RuntimeException("Could not listen to event", res.cause());
				}
			});
			try {
				if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
					throw new RuntimeException("Timeout while waiting for event");
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			consumer.unregister();
		});
	}
}
