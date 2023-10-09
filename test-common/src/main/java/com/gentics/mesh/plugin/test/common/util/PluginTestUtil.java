package com.gentics.mesh.plugin.test.common.util;

import static com.gentics.mesh.core.rest.plugin.PluginStatus.REGISTERED;
import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.plugin.PluginListResponse;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshWebsocket;
import com.gentics.mesh.test.docker.MeshContainer;
import com.gentics.mesh.test.local.MeshLocalServer;

public final class PluginTestUtil {

	private PluginTestUtil() {
	}

	/**
	 * Check whether the plugin can be found and is registered. Otherwise fail after the given timeout is exceeded.
	 * 
	 * @param container
	 * @param timeoutInMilliseconds
	 * @param pluginId
	 * @throws InterruptedException
	 */
	public static void waitForPluginRegistration(MeshContainer container, int timeoutInMilliseconds, String pluginId) throws InterruptedException {
		waitForPluginRegistration(container.client(), timeoutInMilliseconds, pluginId);
	}

	public static void waitForPluginRegistration(MeshLocalServer server, int timeoutInMilliseconds, String pluginId) throws InterruptedException {
		waitForPluginRegistration(server.client(), timeoutInMilliseconds, pluginId);
	}

	public static void waitForPluginRegistration(MeshRestClient client, int timeoutInMilliseconds, String pluginId) throws InterruptedException {
		// 1. Initial check
		PluginListResponse plugins = call(() -> client.findPlugins());
		Optional<PluginResponse> foundPlugin = plugins.getData().stream().filter(plugin -> plugin.getId().equals(pluginId)).findFirst();
		if (foundPlugin.isPresent()) {
			if (REGISTERED == foundPlugin.get().getStatus()) {
				return;
			}
		}
		// Wait for event
		MeshWebsocket eb = client.eventbus();
		try {
			eb.registerEvents(MeshEvent.PLUGIN_REGISTERED);
			CountDownLatch latch = new CountDownLatch(1);
			eb.events().subscribe(ignore -> {
				latch.countDown();
			}, err -> err.printStackTrace());
			boolean eventReceived = latch.await(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
			// when we did not get the event in time, we do another check, maybe we just missed it
			if (!eventReceived) {
				plugins = call(() -> client.findPlugins());
				foundPlugin = plugins.getData().stream().filter(plugin -> plugin.getId().equals(pluginId)).findFirst();
				if (foundPlugin.isPresent()) {
					if (REGISTERED == foundPlugin.get().getStatus()) {
						return;
					}
				}
			}
			assertThat(eventReceived).as(String.format("Plugin %s was registered within %d ms", pluginId, timeoutInMilliseconds)).isTrue();
		} finally {
			eb.close();
		}
	}
}
