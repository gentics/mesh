package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.USER_UPDATED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.gentics.mesh.cache.impl.EventAwareCacheImpl;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.UUIDUtil;

@MeshTestSetting(testSize = FULL, startServer = true)
public class EventAwareCacheTest extends AbstractMeshTest {

	@Test
	public void testCustomHandler() {
		AbstractMeshOptions options = new AbstractMeshOptions();
		options.getMonitoringOptions().setEnabled(false);
		EventAwareCache<String, Boolean> USER_STATE_CACHE = new EventAwareCacheImpl.Builder<String, Boolean>()
			.maxSize(15_000)
			.events(USER_UPDATED)
			.action((event, cache) -> {
				String uuid = event.body().getString("uuid");
				if (uuid != null) {
					cache.invalidate(uuid);
				} else {
					cache.invalidate();
				}

			})
			.setMetricsService(mock(MetricsService.class))
			.meshOptions(options)
			.name("testcache")
			.vertx(vertx())
			.build();

		// Set some values to the cache
		String uuid2 = UUIDUtil.randomUUID();
		USER_STATE_CACHE.put(userUuid(), true);
		USER_STATE_CACHE.put(uuid2, true);
		assertTrue("The entry was not cached.", USER_STATE_CACHE.get(userUuid()));
		assertEquals("There should be two entries in the cache.", 2, USER_STATE_CACHE.size());

		// Update user to trigger event
		UserUpdateRequest request = new UserUpdateRequest();
		request.setUsername("someothername");
		call(() -> client().updateUser(userUuid(), request));

		assertNull("The cache entry should have been invalidated.", USER_STATE_CACHE.get(userUuid()));
		assertTrue("The other cache entry should be still in the cache.", USER_STATE_CACHE.get(uuid2));
	}
}
