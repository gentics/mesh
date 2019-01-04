package com.gentics.mesh.core.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = TestSize.PROJECT, startServer = false)
public class AuthUserTest extends AbstractMeshTest {

	@Test
	public void testAuthorization() throws Exception {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			MeshAuthUser requestUser = ac.getUser();
			Node targetNode = folder("2015");
			assertTrue(requestUser.hasPermission(targetNode, GraphPermission.READ_PERM));
			role().revokePermissions(targetNode, GraphPermission.READ_PERM);
			assertFalse(requestUser.hasPermission(targetNode, GraphPermission.READ_PERM));
		}
	}

}
