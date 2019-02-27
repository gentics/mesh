package com.gentics.mesh.search.migration;

import com.gentics.mesh.search.AbstractNodeSearchEndpointTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import org.junit.Test;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeMigrationSearchTrackingTest extends AbstractNodeSearchEndpointTest {
	@Test
	public void testMigrationRequests() {
		waitForSearchIdleEvent(migrateSchema("folder"));

		// It should delete and create documents during the migration.
		assertThat(trackingSearchProvider()).hasSymmetricNodeRequests();
	}

}
