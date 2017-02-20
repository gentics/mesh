package com.gentics.mesh.core.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.mock.Mocks;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, useTinyDataset = false, startServer = false)
public class AuthUserTest extends AbstractMeshTest {

	@Test
	public void testAuthorization() throws Exception {
		try (NoTx noTrx = db().noTx()) {
			InternalActionContext ac = Mocks.getMockedInternalActionContext(user());
			MeshAuthUser requestUser = ac.getUser();
			Node targetNode = content();
			assertTrue(requestUser.hasPermission(targetNode, GraphPermission.READ_PERM));
			role().revokePermissions(targetNode, GraphPermission.READ_PERM);
			assertFalse(requestUser.hasPermission(targetNode, GraphPermission.READ_PERM));
		}
	}

}
