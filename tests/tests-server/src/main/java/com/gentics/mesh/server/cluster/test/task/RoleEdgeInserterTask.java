package com.gentics.mesh.server.cluster.test.task;

import java.util.concurrent.locks.Lock;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.root.impl.RoleRootImpl;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.graphdb.cluster.OrientDBClusterManager;
import com.gentics.mesh.server.cluster.test.AbstractClusterTest;
import com.gentics.mesh.util.UUIDUtil;
import com.hazelcast.core.HazelcastInstance;
import com.orientechnologies.common.concur.ONeedRetryException;

/**
 * Test task which inserts role edges.
 */
public class RoleEdgeInserterTask extends AbstractLoadTask {

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
	public RoleImpl createRole(Tx tx, String uuid) {
		RoleImpl v = ((GraphDBTx) tx).getGraph().addFramedVertex(RoleImpl.class);
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
					HazelcastInstance hz = ((OrientDBClusterManager) test.getDb().clusterManager()).getHazelcast();
					lock = hz.getLock(WriteLock.GLOBAL_LOCK_KEY);
					lock.lock();
				}
			}
			try {
				String roleUuid = UUIDUtil.randomUUID();
				test.tx(tx -> {
					RoleRootImpl roleRoot = ((GraphDBTx) tx).getGraph().getFramedVertices(RoleRootImpl.class).next();
					RoleImpl role = createRole(tx, roleUuid);
					roleRoot.addFramedEdge("HAS_ROLE", role);
					role.setProperty("name", "Test@" + System.nanoTime());
					System.out.println("Insert " + role.id() + " " + roleUuid);
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
