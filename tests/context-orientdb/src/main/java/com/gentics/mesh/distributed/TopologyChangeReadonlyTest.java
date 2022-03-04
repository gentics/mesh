package com.gentics.mesh.distributed;

import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.test.MeshTestSetting;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * Test cases for the topology change readonly behavior
 */
@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = false, clusterMode = true)
public class TopologyChangeReadonlyTest extends AbstractMeshTest {
	/**
	 * Name of the project
	 */
	protected String projectName;

	/**
	 * UUID of the project's base node
	 */
	protected String projectBaseNodeUuid;

	/**
	 * Name of the base node's schema
	 */
	protected String schemaName;

	/**
	 * Read some data from the db
	 */
	@Before
	public void setup() {
		db().tx(() -> {
			projectName = project().getName();
			projectBaseNodeUuid = project().getBaseNode().getUuid();
			schemaName = project().getBaseNode().getSchemaContainer().getName();
		});
	}

	/**
	 * Test that a change in the storage status (in this case to BACKUP) will cause writing requests to fail, while reading requests will still succeed.
	 * @throws InterruptedException
	 */
	@Test
	public void test() throws InterruptedException {
		// enable the feature
		options().getClusterOptions().setTopologyChangeReadOnly(true);
		options().getClusterOptions().setTopologyLockDelay(1);

		// current status of the topology lock
		AtomicBoolean topoLocked = new AtomicBoolean(false);

		// count down latches for topology lock changes
		CountDownLatch lockLatch = new CountDownLatch(1);
		CountDownLatch unlockLatch = new CountDownLatch(1);

		// add event handler, which tracks the changes in topology lock
		mesh().vertx().eventBus().consumer(CLUSTER_DATABASE_CHANGE_STATUS.address, (Message<JsonObject> handler) -> {
			boolean locked = db().clusterManager().isClusterTopologyLocked();
			boolean wasLocked = topoLocked.getAndSet(locked);
			if (locked && !wasLocked) {
				lockLatch.countDown();
			} else if (!locked && wasLocked) {
				unlockLatch.countDown();
			}
		});

		// invoke the graphdb backup (asynchronously)
		mesh().vertx().executeBlocking(bc -> {
			try {
				OrientDBMeshOptions options = (OrientDBMeshOptions) options();
				db().backupDatabase(options.getStorageOptions().getBackupDirectory());
				bc.complete();
			} catch (IOException e) {
				bc.fail(e);
			}
		}, rs -> {
		});

		// wait for the topology lock to be set
		lockLatch.await(1, TimeUnit.MINUTES);

		// reading nodes must succeed
		read();
		try {
			// creating nodes must fail
			create();
			fail("Creating was expected to fail");
		} catch (RuntimeException e) {
		}

		// wait for the topology lock to be cleared
		unlockLatch.await(1, TimeUnit.MINUTES);

		// reading nodes must succeed
		read();
		// creating nodes must succeed
		create();
	}

	/**
	 * Read a node (and wait for the response)
	 */
	protected void read() {
		client().findNodeByUuid(projectName, projectBaseNodeUuid).blockingAwait();
	}

	/**
	 * Create a node (and wait for the response)
	 *
	 */
	protected void create() {
		NodeCreateRequest request = new NodeCreateRequest();
		request.setParentNodeUuid(projectBaseNodeUuid);
		request.setSchemaName(schemaName);
		request.setLanguage("en");
		client().createNode(projectName, request).blockingAwait();
	}
}
