package com.gentics.mesh.plugin.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.plugin.common.plugin.TestPlugin;
import com.gentics.mesh.plugin.test.common.HibernateMeshLocalServer;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;
import com.gentics.mesh.router.PluginRouter;
import com.gentics.mesh.router.RootRouter;
import com.gentics.mesh.router.RootRouterImpl;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.RouterStorageImpl;
import com.gentics.mesh.test.local.MeshLocalServer;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.impl.RouterImpl;

/**
 * Tests for plugin routers
 */
public class PluginRouterTest {
	@ClassRule
	public static final MeshLocalServer server = new HibernateMeshLocalServer().waitForStartup();

	@BeforeClass
	public static void setupOnce() throws InterruptedException, ExecutionException, TimeoutException {
		server.getMesh().deployPlugin(TestPlugin.class, "testplugin").blockingAwait();
		PluginTestUtil.waitForPluginRegistration(server, 5_000, "testplugin");
	}

	/**
	 * Get the vertx instance from a (possible private) field "vertx" in the given instance. This method
	 * will fail, if the vertx instance cannot be extracted for some reason
	 * @param <T> type of the instance
	 * @param classOfT class of the instance
	 * @param instance instance
	 * @return vert.x instance
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	protected static <T> Vertx getVertx(Class<T> classOfT, T instance)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field vertxField = classOfT.getDeclaredField("vertx");
		vertxField.setAccessible(true);
		Object vertxObject = vertxField.get(instance);
		if (vertxObject instanceof Vertx vertx) {
			return vertx;
		} else {
			throw new NoSuchFieldException();
		}
	}

	/**
	 * Test that all the routers use a common vert.x instance
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	@Test
	public void testSingleVertx()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Mesh mesh = server.getMesh();
		MeshComponent component = mesh.internal();
		Vertx meshVertxInstance = mesh.getVertx();
		for (RouterStorage routerStorage : component.routerStorageRegistry().getInstances()) {
			if (routerStorage instanceof RouterStorageImpl routerStorageImpl) {
				assertThat(getVertx(RouterStorageImpl.class, routerStorageImpl)).as("Vert.x instance of routerStorage")
						.isEqualTo(meshVertxInstance);
			}

			RootRouter rootRouter = routerStorage.root();
			if (rootRouter instanceof RootRouterImpl rootRouterImpl) {
				assertThat(getVertx(RootRouterImpl.class, rootRouterImpl)).as("Vert.x instance of root router")
						.isEqualTo(meshVertxInstance);
			}

			PluginRouter pluginRouter = rootRouter.apiRouter().pluginRouter();
			for (Router router : pluginRouter.pluginRouters().values()) {
				if (router instanceof RouterImpl routerImpl) {
					assertThat(getVertx(RouterImpl.class, routerImpl)).as("Vert.x Instance of router")
							.isEqualTo(meshVertxInstance);
				}
			}
		}
	}
}
