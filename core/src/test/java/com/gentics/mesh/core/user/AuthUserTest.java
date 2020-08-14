package com.gentics.mesh.core.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = TestSize.PROJECT, startServer = false)
public class AuthUserTest extends AbstractMeshTest {

	@Test
	public void testAuthorization() throws Exception {
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			UserDaoWrapper userDao = Tx.get().data().userDao();

			InternalActionContext ac = mockActionContext();
			MeshAuthUser requestUser = ac.getUser();
			Node targetNode = folder("2015");
			assertTrue(userDao.hasPermission(requestUser, targetNode, InternalPermission.READ_PERM));
			roleDao.revokePermissions(role(), targetNode, InternalPermission.READ_PERM);
			assertFalse(userDao.hasPermission(requestUser, targetNode, InternalPermission.READ_PERM));
		}
	}

}
