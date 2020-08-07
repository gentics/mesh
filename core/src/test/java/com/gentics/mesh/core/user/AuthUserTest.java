package com.gentics.mesh.core.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = TestSize.PROJECT, startServer = false)
public class AuthUserTest extends AbstractMeshTest {

	@Test
	public void testAuthorization() throws Exception {
		try (Tx tx = tx()) {
			RoleRoot roleDao = tx.data().roleDao();
			UserRoot userRoot = Tx.get().data().userDao();

			InternalActionContext ac = mockActionContext();
			MeshAuthUser requestUser = ac.getUser();
			Node targetNode = folder("2015");
			assertTrue(userRoot.hasPermission(requestUser, targetNode, GraphPermission.READ_PERM));
			roleDao.revokePermissions(role(), targetNode, GraphPermission.READ_PERM);
			assertFalse(userRoot.hasPermission(requestUser, targetNode, GraphPermission.READ_PERM));
		}
	}

}
