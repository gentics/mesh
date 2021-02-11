package com.gentics.mesh.core.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = TestSize.PROJECT, startServer = false)
public class AuthUserTest extends AbstractMeshTest {

	@Test
	public void testAuthorization() throws Exception {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = Tx.get().userDao();

			InternalActionContext ac = mockActionContext();
			HibUser requestUser = ac.getUser();
			HibNode targetNode = folder("2015");
			assertTrue(userDao.hasPermission(requestUser, targetNode, InternalPermission.READ_PERM));
			roleDao.revokePermissions(role(), targetNode, InternalPermission.READ_PERM);
			assertFalse(userDao.hasPermission(requestUser, targetNode, InternalPermission.READ_PERM));
		}
	}

}
