package com.gentics.mesh.core.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = TestSize.PROJECT, startServer = false)
public class AuthUserTest extends AbstractMeshTest {

	@Test
	public void testAuthorization() throws Exception {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = Tx.get().userDao();

			InternalActionContext ac = mockActionContext();
			User requestUser = ac.getUser();
			Node targetNode = folder("2015");
			assertTrue(userDao.hasPermission(requestUser, targetNode, InternalPermission.READ_PERM));
			roleDao.revokePermissions(role(), targetNode, InternalPermission.READ_PERM);
			assertFalse(userDao.hasPermission(requestUser, targetNode, InternalPermission.READ_PERM));
		}
	}

}
