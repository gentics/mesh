package com.gentics.mesh.search.permission;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class TagFamilyPermissionSearchTest extends AbstractMeshTest {

	@Test
	public void testReadPermHandling() throws Exception {

		String tagfamilyname = "testtagfamily42a";
		TagFamilyResponse response = createTagFamily(PROJECT_NAME, tagfamilyname);
		try (Tx tx = tx()) {
			UserDao userDao = tx.userDao();
			RoleDao roleDao = tx.roleDao();

			HibTagFamily tagFamily = tx.tagFamilyDao().findByUuid(project(), response.getUuid());
			System.out.println("TagFamily Uuid:" + response.getUuid());
			for (HibRole role : userDao.getRoles(user())) {
				roleDao.revokePermissions(role, tagFamily, InternalPermission.READ_PERM);
			}
			tx.success();
		}

		recreateIndices();

		String json = getESText("tagFamilyWildcard.es");

		TagFamilyListResponse list = call(() -> client().searchTagFamilies(json));
		assertEquals("The tagFamily should not be found since the requestor has no permission to see it", 0, list.getData().size());

		// Now add the perm
		try (Tx tx = tx()) {
			UserDao userDao = tx.userDao();
			RoleDao roleDao = tx.roleDao();
			TagFamilyDao tagFamilyDao = tx.tagFamilyDao();

			HibTagFamily tagFamily = tagFamilyDao.findByUuid(project(), response.getUuid());
			System.out.println("TagFamily Uuid:" + response.getUuid());
			for (HibRole role : userDao.getRoles(user())) {
				roleDao.grantPermissions(role, tagFamily, InternalPermission.READ_PERM);
			}
			tx.success();
		}

		recreateIndices();

		list = call(() -> client().searchTagFamilies(json));
		assertEquals("The tagFamily should be found since we added the permission to see it", 1, list.getData().size());

	}

	@Test
	public void testIndexPermUpdate() throws Exception {
		recreateIndices();
		String tagfamilyName = "testtagfamily42a";
		TagFamilyResponse response = createTagFamily(PROJECT_NAME, tagfamilyName);
		waitForSearchIdleEvent();

		String json = getESText("tagFamilyWildcard.es");

		TagFamilyListResponse list = call(() -> client().searchTagFamilies(json));
		assertEquals("The tagFamily should be found since the requestor has permission to see it", 1, list.getData().size());

		// Revoke read permission
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(false);
		call(() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/tagFamilies/" + response.getUuid(), request));
		waitForSearchIdleEvent();

		list = call(() -> client().searchTagFamilies(json));
		assertEquals("The tagFamily should not be found since the requestor has no permission to see it", 0, list.getData().size());

	}

}
