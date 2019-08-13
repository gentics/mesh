package com.gentics.mesh.search.migration;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.Test;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.rest.client.MeshWebsocket;
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

		AtomicInteger fullSyncs = new AtomicInteger(0);
		MeshWebsocket eventbus = client().eventbus();
		eventbus.registerEvents(MeshEvent.INDEX_SYNC_REQUEST);
		eventbus.events().subscribe(ev -> fullSyncs.incrementAndGet());

		// Wait so that sync does not happen at the beginning of the migration
		Thread.sleep(1000);
		SyncEventHandler.invokeSyncCompletable(meshApi()).blockingAwait(30, TimeUnit.SECONDS);

		waitForSearchIdleEvent();

		assertThat(fullSyncs.get()).isEqualTo(1);
		eventbus.close();
	}
}
