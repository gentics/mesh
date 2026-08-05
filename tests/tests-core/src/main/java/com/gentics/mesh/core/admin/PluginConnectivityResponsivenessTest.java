package com.gentics.mesh.core.admin;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.test.TestSize.PROJECT;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.AbstractPluginTest;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.category.PluginTests;

@Category(PluginTests.class)
@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true, optionChanger = MeshCoreOptionChanger.SMALL_EVENT_LOOP_POOL)
public class PluginConnectivityResponsivenessTest extends AbstractPluginTest {

	@Test
	public void testClientPlugin() throws IOException, InterruptedException {
		grantAdmin();

		copyAndDeploy(CLIENT_PATH, "client.jar");
		assertEquals(1, pluginManager().getPluginIds().size());

		int numRequests = 50;
		CountDownLatch latch = new CountDownLatch(numRequests);
		for (int i = 0; i < numRequests; i++) {
			new Thread(() -> {
				try {
					assertNotNull("Should find a default John Doe user", JsonUtil.readValue(httpGetNow(CURRENT_API_BASE_PATH + "/plugins/client/me"), UserResponse.class));
					latch.countDown();
				} catch (GenericRestException | IOException e) {
					throw new IllegalStateException(e);
				}
			}).start();
		}
		latch.await(10, TimeUnit.SECONDS);
	}
}
