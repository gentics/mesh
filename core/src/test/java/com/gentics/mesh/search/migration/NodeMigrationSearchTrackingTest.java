package com.gentics.mesh.search.migration;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;

import org.junit.Test;

import com.gentics.mesh.search.AbstractNodeSearchEndpointTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class NodeMigrationSearchTrackingTest extends AbstractNodeSearchEndpointTest {
	@Test
	public void testMigrationRequests() {
		grantAdminRole();
		waitForJob(() -> {
			waitForSearchIdleEvent(migrateSchema("folder"));
		});

		// It should delete and create documents during the migration.
		assertThat(trackingSearchProvider()).hasSymmetricNodeRequests();
	}

}
