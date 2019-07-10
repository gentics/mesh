package com.gentics.mesh.search.migration;

import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.search.AbstractNodeSearchEndpointTest;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = CONTAINER, testSize = FULL, startServer = true)
public class NodeMigrationDuringSyncTest extends AbstractNodeSearchEndpointTest {

	@Test
	public void testSyncDuringMigration() throws Exception {
		recreateIndices();
		NodeResponse parent = createNode();
		// Create some nodes for load during migration
		IntStream.range(0, 1000).forEach(i -> createNode(parent));
		waitForSearchIdleEvent();

		migrateSchema("folder", false).blockingAwait();

		// Wait so that sync does not happen at the beginning of the migration
		Thread.sleep(2000);
		SyncEventHandler.invokeSyncCompletable().blockingAwait(30, TimeUnit.SECONDS);

		waitForSearchIdleEvent();
	}
}
