
package com.gentics.mesh.server.cluster.test.task;

import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.root.impl.RoleRootImpl;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.server.cluster.test.AbstractClusterTest;
import com.gentics.mesh.util.UUIDUtil;
import com.orientechnologies.common.concur.ONeedRetryException;

/**
 * Test task
 */
public class RoleEdgeGlobalLockInserterTask extends AbstractLoadTask {

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
	public RoleImpl createRole(Tx tx, String uuid) {
		RoleImpl v = ((GraphDBTx) tx).getGraph().addFramedVertex(RoleImpl.class);
		v.property("uuid", uuid);
		v.property("name", "SOME VALUE" + System.nanoTime());
		return v;
	}

	@Override
	public void runTask(long txDelay, boolean lockTx, boolean lockForDBSync) {
		try {
			MeshComponent comp = test.getMesh().internal();
			try (WriteLock lock = comp.globalLock().lock(null)) {
				String roleUuid = UUIDUtil.randomUUID();
				test.tx(tx -> {
					RoleRootImpl roleRoot = ((GraphDBTx) tx).getGraph().getFramedVertices(RoleRootImpl.class).next();
					RoleImpl role = createRole(tx, roleUuid);
					roleRoot.addFramedEdge("HAS_ROLE", role);
					role.property("name", "Test@" + System.nanoTime());
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
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
