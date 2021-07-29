package com.gentics.mesh.core.field.micronode;

import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparatorImpl;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.ext.web.RoutingContext;

@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = FULL, startServer = true)
public class MicroschemaModelTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			HibMicroschema vcard = microschemaContainer("vcard");
			MicroschemaReference reference = vcard.transformToReference();
			assertNotNull(reference);
			assertEquals("vcard", reference.getName());
			assertEquals(vcard.getUuid(), reference.getUuid());
		}
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			MicroschemaDaoWrapper microschemaDao = tx.microschemaDao();
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			int expectedMicroschemaContainers = microschemaContainers().size();

			for (long i = 1; i <= expectedMicroschemaContainers + 1; i++) {
				Page<? extends HibMicroschema> page = microschemaDao.findAll(ac, new PagingParametersImpl(1, i));

				assertEquals(microschemaContainers().size(), page.getTotalElements());
				assertEquals(Math.min(expectedMicroschemaContainers, i), page.getSize());
			}
		}
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testRootNode() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testFindByName() {
		try (Tx tx = tx()) {
			String invalidName = "thereIsNoMicroschemaWithThisName";

			for (String name : microschemaContainers().keySet()) {
				HibMicroschema container = boot().microschemaContainerRoot().findByName(name);
				assertNotNull("Could not find microschema container for name " + name, container);
				MicroschemaModel microschemaModel = container.getLatestVersion().getSchema();
				assertNotNull("Container for microschema " + name + " did not contain a microschema", microschemaModel);
				assertEquals("Check microschema name", name, microschemaModel.getName());
			}

			assertNull("Must not find microschema with name " + invalidName, boot().microschemaContainerRoot().findByName(invalidName));
		}
	}

	@Test
	@Override
	public void testFindByUUID() {
		try (Tx tx = tx()) {
			String invalidUUID = UUIDUtil.randomUUID();

			MicroschemaRoot root = boot().microschemaContainerRoot();
			for (HibMicroschema container : microschemaContainers().values()) {
				String uuid = container.getUuid();
				assertNotNull("Could not find microschema with uuid " + uuid, root.findByUuid(uuid));
			}

			assertNull("Must not find microschema with uuid " + invalidUUID, root.findByUuid(invalidUUID));
		}
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testRead() throws IOException {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCreate() throws IOException {
		try (Tx tx = tx()) {
			MicroschemaVersionModel schema = new MicroschemaModelImpl();
			schema.setName("test");
			HibMicroschema container = createMicroschema(schema);
			assertNotNull("The container was not created.", container);
			assertNotNull("The container schema was not set", container.getLatestVersion().getSchema());
			assertEquals("The creator was not set.", user().getUuid(), container.getCreator().getUuid());
		}
	}

	/**
	 * Assert that the schema version is in sync with its rest model.
	 */
	@Test
	public void testVersionSync() {
		try (Tx tx = tx()) {
			assertNotNull(microschemaContainer("vcard"));
			assertEquals("The microschema container and schema rest model version must always be in sync",
				microschemaContainer("vcard").getLatestVersion().getVersion(),
				microschemaContainer("vcard").getLatestVersion().getSchema().getVersion());
		}

	}

	@Test
	@Override
	public void testDelete() throws MeshJsonException {
		try (Tx tx = tx()) {
			MicroschemaDaoWrapper microschemaDao = tx.microschemaDao();
			MicroschemaVersionModel schema = new MicroschemaModelImpl();
			schema.setName("test");
			HibMicroschema container = createMicroschema(schema);
			assertNotNull(microschemaDao.findByName("test"));
			BulkActionContext bac = createBulkContext();
			microschemaDao.delete(container, bac);
			assertNull(microschemaDao.findByName("test"));
		}
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testUpdate() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testReadPermission() throws MeshJsonException {
		try (Tx tx = tx()) {
			MicroschemaVersionModel schema = new MicroschemaModelImpl();
			schema.setName("someNewMicroschema");
			HibMicroschema container = createMicroschema(schema);
			testPermission(InternalPermission.READ_PERM, container);
		}
	}

	@Test
	@Override
	public void testDeletePermission() throws MeshJsonException {
		try (Tx tx = tx()) {
			MicroschemaVersionModel schema = new MicroschemaModelImpl();
			schema.setName("someNewMicroschema");
			HibMicroschema container = createMicroschema(schema);
			testPermission(InternalPermission.DELETE_PERM, container);
		}

	}

	@Test
	@Override
	public void testUpdatePermission() throws MeshJsonException {
		try (Tx tx = tx()) {
			MicroschemaVersionModel schema = new MicroschemaModelImpl();
			schema.setName("someNewMicroschema");
			HibMicroschema container = createMicroschema(schema);
			testPermission(InternalPermission.UPDATE_PERM, container);
		}
	}

	@Test
	@Override
	public void testCreatePermission() throws MeshJsonException {
		try (Tx tx = tx()) {
			MicroschemaVersionModel schema = new MicroschemaModelImpl();
			schema.setName("someNewMicroschema");
			HibMicroschema container = createMicroschema(schema);
			testPermission(InternalPermission.CREATE_PERM, container);
		}
	}

	@Test
	@Override
	public void testTransformation() throws IOException {
		try (Tx tx = tx()) {
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			HibMicroschema vcard = microschemaContainer("vcard");
			MicroschemaResponse schema = vcard.transformToRestSync(ac, 0, "en");
			assertEquals(vcard.getUuid(), schema.getUuid());
		}
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testCreateDelete() {
		// TODO Auto-generated method stub
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCRUDPermissions() throws MeshJsonException {
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			UserDaoWrapper userDao = tx.userDao();
			MicroschemaVersionModel schema = new MicroschemaModelImpl();
			schema.setName("someNewMicroschema");
			HibMicroschema container = createMicroschema(schema);

			assertFalse(roleDao.hasPermission(role(), InternalPermission.CREATE_PERM, container));
			userDao.inheritRolePermissions(getRequestUser(), meshRoot().getMicroschemaContainerRoot(), container);
			assertTrue("The addCRUDPermissionOnRole method should add the needed permissions on the new microschema container.",
				roleDao.hasPermission(role(), InternalPermission.CREATE_PERM, container));
		}
	}

	/**
	 * Test getting NodeGraphFieldContainers that container Micronodes using a specific microschema container version
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetContainerUsingMicroschemaVersion() throws IOException {
		try (Tx tx = tx()) {
			MicroschemaDaoWrapper microschemaDao = tx.microschemaDao();
			HibMicroschemaVersion vcard = microschemaContainer("vcard").getLatestVersion();

			MicroschemaModel microschemaModel = vcard.getSchema();
			MicroschemaModel updatedMicroschemaModel = new MicroschemaModelImpl();
			updatedMicroschemaModel.setName(microschemaModel.getName());
			updatedMicroschemaModel.getFields().addAll(microschemaModel.getFields());
			updatedMicroschemaModel.addField(FieldUtil.createStringFieldSchema("newfield"));

			SchemaChangesListModel model = new SchemaChangesListModel();
			model.getChanges().addAll(new MicroschemaComparatorImpl().diff(microschemaModel, updatedMicroschemaModel));

			InternalActionContext ac = mockActionContext();
			EventQueueBatch batch = createBatch();
			microschemaDao.applyChanges(vcard, ac, model, batch);
			HibMicroschemaVersion newVCard = microschemaContainer("vcard").getLatestVersion();

			NodeGraphFieldContainer containerWithBoth = boot().contentDao().getGraphFieldContainer(folder("2015"), "en");
			containerWithBoth.createMicronode("single", vcard);
			containerWithBoth.createMicronodeFieldList("list").createMicronode().setSchemaContainerVersion(vcard);

			NodeGraphFieldContainer containerWithField = boot().contentDao().getGraphFieldContainer(folder("news"), "en");
			containerWithField.createMicronode("single", vcard);

			NodeGraphFieldContainer containerWithList = boot().contentDao().getGraphFieldContainer(folder("products"), "en");
			containerWithList.createMicronodeFieldList("list").createMicronode().setSchemaContainerVersion(vcard);

			NodeGraphFieldContainer containerWithOtherVersion = boot().contentDao().getGraphFieldContainer(folder("deals"), "en");
			containerWithOtherVersion.createMicronode("single", newVCard);

			List<? extends NodeGraphFieldContainer> containers = microschemaDao.findDraftFieldContainers(vcard, project().getLatestBranch().getUuid()).list();
			assertThat(new ArrayList<NodeGraphFieldContainer>(containers)).containsOnly(containerWithBoth, containerWithField, containerWithList)
				.hasSize(3);
		}
	}
}
