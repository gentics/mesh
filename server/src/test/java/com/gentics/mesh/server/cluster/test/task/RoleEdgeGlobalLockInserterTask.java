
package com.gentics.mesh.server.cluster.test.task;

import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.server.cluster.test.AbstractClusterTest;
import com.gentics.mesh.util.UUIDUtil;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.tinkerpop.blueprints.Vertex;

/**
 * Test task
 */
public class RoleEdgeGlobalLockInserterTask extends AbstractLoadTask {

	public static final String ROLE = "RoleImpl";

	public RoleEdgeGlobalLockInserterTask(AbstractClusterTest test) {
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
		Vertex v = ((GraphDBTx) tx).getGraph().addVertex("class:" + ROLE);
		v.setProperty("uuid", uuid);
		v.setProperty("name", "SOME VALUE" + System.nanoTime());
		return v;
	}

	@Override
	public void runTask(long txDelay, boolean lockTx, boolean lockForDBSync) {
		try {
			MeshComponent comp = test.getMesh().internal();
			try (WriteLock lock = comp.globalLock().lock(null)) {
				String roleUuid = UUIDUtil.randomUUID();
				test.tx(tx -> {
					Vertex roleRoot = ((GraphDBTx) tx).getGraph().getVertices("@class", "RoleRootImpl").iterator().next();
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
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
