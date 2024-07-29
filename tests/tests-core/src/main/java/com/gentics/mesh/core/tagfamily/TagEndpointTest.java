package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.dao.PersistingTagFamilyDao;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.ext.web.RoutingContext;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class TagEndpointTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			HibTagFamily tagFamily = tagFamily("colors");
			TagFamilyReference reference = tagFamily.transformToReference();
			assertNotNull(reference);
			assertEquals(tagFamily.getUuid(), reference.getUuid());
			assertEquals(tagFamily.getName(), reference.getName());
		}
	}

	@Test
	public void testTagFamilyProject() {
		try (Tx tx = tx()) {
			HibTagFamily tagFamily = tagFamily("colors");
			assertNotNull(tagFamily.getProject());
			assertEquals(project(), tagFamily.getProject());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			tx.tagFamilyDao().findAll(mockActionContext(), new PagingParametersImpl(1, 10L));
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();
			PersistingTagFamilyDao tagFamilyDao = ctx.tagFamilyDao();

			List<? extends HibTagFamily> families = tagFamilyDao.findAll().list();
			assertNotNull(families);
			assertEquals(2, families.size());

			HibTagFamily projectTagFamily = tagFamilyDao.findByName(project(), "colors");
			assertNotNull(projectTagFamily);

			assertNotNull(tagFamilyDao.create(project(), "bogus", user()));
			assertEquals(3, ctx.count(tagFamilyDao.getPersistenceClass(project())));
			assertEquals(3, tx.tagFamilyDao().count());
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();
			PersistingTagFamilyDao tagFamilyDao = ctx.tagFamilyDao();

			long nProjectsBefore = tagFamilyDao.count(project());
			assertNotNull(tagFamilyDao.create(project(), "test1234556", user()));
			long nProjectsAfter = tagFamilyDao.count(project());
			assertEquals(nProjectsBefore + 1, nProjectsAfter);
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (Tx tx = tx()) {
			TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
			assertNotNull(tagFamilyDao.findByName("colors"));
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (Tx tx = tx()) {
			TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
			HibTagFamily tagFamily = tagFamily("colors");

			HibTagFamily foundTagFamily = tagFamilyDao.findByUuid(tagFamily.getUuid());
			assertNotNull(foundTagFamily);
		}
	}

	@Test
	@Override
	public void testRead() throws IOException {
		try (Tx tx = tx()) {
			HibTagFamily tagFamily = tagFamily("colors");
			assertNotNull(tagFamily.getName());
			assertEquals("colors", tagFamily.getName());
			assertNotNull(tagFamily.getEditor());
			assertNotNull(tagFamily.getCreator());
		}
	}

	@Test
	@Override
	public void testCreate() throws IOException {
		try (Tx tx = tx()) {
			TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
			HibTagFamily family = tagFamilyDao.create(project(), "test", user());
			HibTagFamily family2 = tagFamilyDao.findByName(project(), family.getName());
			assertNotNull(family2);
			assertEquals("test", family2.getName());
			assertEquals(family.getUuid(), family2.getUuid());
		}
	}

	@Test
	@Override
	public void testDelete() {
		BulkActionContext context = createBulkContext();
		try (Tx tx = tx()) {
			TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
			HibTagFamily tagFamily = tagFamily("colors");
			tagFamilyDao.delete(tagFamily, context);
			tx.success();
		}
		// 6 = 1 Tag family + 3 color tags + 2 tagged nodes
		assertEquals("The batch should contain 6 entries.", 6, context.batch().size());
	}

	@Test
	@Override
	public void testUpdate() {
		try (Tx tx = tx()) {
			HibTagFamily tagFamily = tagFamily("colors");
			tagFamily.setName("new Name");
			assertEquals("new Name", tagFamily.getName());
		}
	}

	@Test
	@Override
	public void testReadPermission() {
		try (Tx tx = tx()) {
			HibTagFamily tagFamily = tx.tagFamilyDao().create(project(), "newProject", user());
			testPermission(InternalPermission.READ_PERM, tagFamily);
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (Tx tx = tx()) {
			HibTagFamily tagFamily = tx.tagFamilyDao().create(project(), "newProject", user());
			testPermission(InternalPermission.DELETE_PERM, tagFamily);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (Tx tx = tx()) {
			HibTagFamily tagFamily = tx.tagFamilyDao().create(project(), "newProject", user());
			testPermission(InternalPermission.UPDATE_PERM, tagFamily);
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (Tx tx = tx()) {
			HibTagFamily tagFamily = tx.tagFamilyDao().create(project(), "newProject", user());
			testPermission(InternalPermission.CREATE_PERM, tagFamily);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
			HibTagFamily tagFamily = tagFamily("colors");
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			TagFamilyResponse response = tagFamilyDao.transformToRestSync(tagFamily, ac, 0);
			assertNotNull(response);
			assertEquals(tagFamily.getName(), response.getName());
			assertEquals(tagFamily.getUuid(), response.getUuid());
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		try (Tx tx = tx()) {
			TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
			HibTagFamily tagFamily = tagFamilyDao.create(project(), "test123", user());
			assertNotNull(tagFamily);
			String uuid = tagFamily.getUuid();
			HibTagFamily foundTagFamily = tagFamilyDao.findByUuid(uuid);
			assertNotNull(foundTagFamily);
			BulkActionContext bac = createBulkContext();
			tagFamilyDao.delete(tagFamily, bac);
			// TODO check for attached nodes
			HibProject project = tx.projectDao().findByUuid(uuid);
			assertNull(project);
		}

	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Tx tx = tx()) {
			UserDao userDao = tx.userDao();
			TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
			HibTagFamily tagFamily = tagFamilyDao.create(project(), "test123", user());
			assertFalse(userDao.hasPermission(user(), tagFamily, InternalPermission.CREATE_PERM));
			userDao.inheritRolePermissions(user(), project().getTagFamilyPermissionRoot(), tagFamily);
			assertTrue(userDao.hasPermission(user(), tagFamily, InternalPermission.CREATE_PERM));
		}
	}

}
