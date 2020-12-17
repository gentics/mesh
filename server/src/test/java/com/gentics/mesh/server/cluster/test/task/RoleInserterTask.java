package com.gentics.mesh.server.cluster.test.task;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.server.cluster.test.AbstractClusterTest;
import com.gentics.mesh.util.UUIDUtil;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.tinkerpop.blueprints.Vertex;

/**
 * Test task which inserts roles.
 */
public class RoleInserterTask extends AbstractLoadTask {

	public static final String ROLE = "RoleImpl";

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
	public Vertex createRole(Tx tx, String uuid) {
		Vertex v = tx.getGraph().addVertex("class:" + ROLE);
		v.setProperty("uuid", uuid);
		v.setProperty("name", "SOME VALUE" + System.nanoTime());
		return v;
	}

	@Override
	public void runTask(long txDelay, boolean lockTx, boolean lockForDBSync) {
		try {
			String roleUuid = UUIDUtil.randomUUID();
			test.tx(tx -> {
				Vertex role = createRole(tx, roleUuid);
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
	}
}
