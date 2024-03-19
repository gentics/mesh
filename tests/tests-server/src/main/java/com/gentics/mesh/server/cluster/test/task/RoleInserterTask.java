package com.gentics.mesh.server.cluster.test.task;

import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.server.cluster.test.AbstractClusterTest;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Test task which inserts roles.
 */
public class RoleInserterTask extends AbstractLoadTask {

	public RoleInserterTask(AbstractClusterTest test) {
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
			String roleUuid = UUIDUtil.randomUUID();
			test.tx(tx -> {
				RoleImpl role = createRole(tx, roleUuid);
				role.setProperty("name", "Test@" + System.nanoTime());
				System.out.println("Insert " + role.id() + " " + roleUuid);
				tx.success();
				return role;
			});
			System.out.println("Inserted " + roleUuid);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
