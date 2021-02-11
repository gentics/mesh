package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLBasicPermissionTest extends AbstractMeshTest {

	@Test
	public void testReadProjectNoPerm() throws Throwable {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), project(), READ_PERM);
			tx.success();
		}
		call(() -> client().graphqlQuery(PROJECT_NAME, "{project{name}}"));
	}

}
