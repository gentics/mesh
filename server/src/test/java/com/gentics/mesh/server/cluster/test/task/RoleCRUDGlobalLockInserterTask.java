
package com.gentics.mesh.server.cluster.test.task;

import com.gentics.mesh.context.impl.LocalActionContextImpl;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.role.RoleCrudHandler;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.server.cluster.test.AbstractClusterTest;
import com.gentics.mesh.util.UUIDUtil;
import com.tinkerpop.blueprints.Vertex;

/**
 * Test task
 */
public class RoleCRUDGlobalLockInserterTask extends AbstractLoadTask {

	public static final String ROLE = "RoleImpl";

	public RoleCRUDGlobalLockInserterTask(AbstractClusterTest test) {
		super(test);
	}

	/**
	 * Create a new role
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
			RoleCrudHandler crudHandler = comp.roleCrudHandler();
			MeshAuthUser user = comp.database().tx(tx -> {
				UserDao userDao = tx.userDao();
				return userDao.findMeshAuthUserByUsername("admin");
			});
			String roleUuid = UUIDUtil.randomUUID();
			LocalActionContextImpl<RoleResponse> ac = new LocalActionContextImpl<>(user, RoleResponse.class);
			ac.setPayloadObject(new RoleCreateRequest().setName("test" + roleUuid));
			crudHandler.handleCreate(ac);
			System.out.println("Inserted " + roleUuid);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
