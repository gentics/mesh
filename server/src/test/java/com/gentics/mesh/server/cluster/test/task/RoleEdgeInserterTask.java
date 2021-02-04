package com.gentics.mesh.server.cluster.test.task;

import java.util.concurrent.locks.Lock;

import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.server.cluster.test.AbstractClusterTest;
import com.gentics.mesh.util.UUIDUtil;
import com.hazelcast.core.HazelcastInstance;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.tinkerpop.blueprints.Vertex;

/**
 * Test task which inserts role edges.
 */
public class RoleEdgeInserterTask extends AbstractLoadTask {

	public static final String ROLE = "RoleImpl";

	public RoleEdgeInserterTask(AbstractClusterTest test) {
		super(test);
	}

	/**
	 * Create a new role vertex.
	 *
	 * @param tx
	 * @param uuid
	 * @return
	 */
	public Vertex createRole(Tx tx, String uuid) {
		Vertex v = tx.getGraph().addVertex("class:" + ROLE);
		v.setProperty("uuid", uuid);
		v.setProperty("name", "SOME VALUE" + System.nanoTime());
		return v;
	}

	@Override
	public void runTask(long txDelay, boolean lockTx, boolean lockForDBSync) {
		try {
			Lock lock = null;
			if (lockTx) {
				if (lockTx) {
					HazelcastInstance hz = test.getDb().clusterManager().getHazelcast();
					lock = hz.getLock(WriteLock.GLOBAL_LOCK_KEY);
					lock.lock();
				}
			}
			try {
				String roleUuid = UUIDUtil.randomUUID();
				test.tx(tx -> {
					Vertex roleRoot = tx.getGraph().getVertices("@class", "RoleRootImpl").iterator().next();
					Vertex role = createRole(tx, roleUuid);
					roleRoot.addEdge("HAS_ROLE", role);
					role.setProperty("name", "Test@" + System.nanoTime());
					System.out.println("Insert " + role.getId() + " " + roleUuid);
					tx.success();
					return role;
				});
				System.out.println("Inserted " + roleUuid);
			} catch (ONeedRetryException e) {
				e.printStackTrace();
				System.out.println("Ignoring ONeedRetryException - normally we would retry the action.");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (lockTx) {
					if (lock != null) {
						lock.unlock();
					}
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
