package com.gentics.mesh.search.migration;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.search.AbstractNodeSearchEndpointTest;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;

@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class NodeMigrationSearchTrackingTest extends AbstractNodeSearchEndpointTest {

	public NodeMigrationSearchTrackingTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(ElasticsearchTestMode.TRACKING);
	}

	@Test
	public void testMigrationRequests() {
		waitForJob(() -> {
			waitForSearchIdleEvent(migrateSchema("folder"));
		});

		// It should delete and create documents during the migration.
		assertThat(trackingSearchProvider()).hasSymmetricNodeRequests();
	}

}
