package com.gentics.mesh.core.field.micronode;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.TestUtils;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.tx.Tx;

import io.vertx.ext.web.RoutingContext;

@MeshTestSetting(useElasticsearch = true, testSize = FULL, startServer = true)
public class MicroschemaContainerTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			MicroschemaContainer vcard = microschemaContainer("vcard");
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
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			int expectedMicroschemaContainers = microschemaContainers().size();

			for (int i = 1; i <= expectedMicroschemaContainers + 1; i++) {
				Page<? extends MicroschemaContainer> page = boot().microschemaContainerRoot().findAll(ac, new PagingParametersImpl(1, i));

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
				MicroschemaContainer container = boot().microschemaContainerRoot().findByName(name);
				assertNotNull("Could not find microschema container for name " + name, container);
				Microschema microschema = container.getLatestVersion().getSchema();
				assertNotNull("Container for microschema " + name + " did not contain a microschema", microschema);
				assertEquals("Check microschema name", name, microschema.getName());
			}

			assertNull("Must not find microschema with name " + invalidName, boot().microschemaContainerRoot().findByName(invalidName));
		}
	}

	@Test
	@Override
	public void testFindByUUID() {
		try (Tx tx = tx()) {
			String invalidUUID = UUIDUtil.randomUUID();

			MicroschemaContainerRoot root = boot().microschemaContainerRoot();
			for (MicroschemaContainer container : microschemaContainers().values()) {
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
	public void testRoot() {
		try (Tx tx = tx()) {
			MicroschemaContainer vcard = microschemaContainer("vcard");
			assertNotNull(vcard.getRoot());
		}
	}

	@Test
	@Override
	public void testCreate() throws IOException {
		try (Tx tx = tx()) {
			MicroschemaModel schema = new MicroschemaModelImpl();
			schema.setName("test");
			MicroschemaContainer container = MeshInternal.get().boot().meshRoot().getMicroschemaContainerRoot().create(schema, user());
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
			MicroschemaModel schema = new MicroschemaModelImpl();
			schema.setName("test");
			MicroschemaContainer container = MeshInternal.get().boot().meshRoot().getMicroschemaContainerRoot().create(schema, user());
			assertNotNull(MeshInternal.get().boot().meshRoot().getMicroschemaContainerRoot().findByName("test"));
			SearchQueueBatch batch = createBatch();
			container.delete(batch);
			assertNull(MeshInternal.get().boot().meshRoot().getMicroschemaContainerRoot().findByName("test"));
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
			MicroschemaModel microschema = new MicroschemaModelImpl();
			microschema.setName("someNewMicroschema");
			MicroschemaContainer microschemaContainer = meshRoot().getMicroschemaContainerRoot().create(microschema, user());
			testPermission(GraphPermission.READ_PERM, microschemaContainer);
		}
	}

	@Test
	@Override
	public void testDeletePermission() throws MeshJsonException {
		try (Tx tx = tx()) {
			MicroschemaModel microschema = new MicroschemaModelImpl();
			microschema.setName("someNewMicroschema");
			MicroschemaContainer microschemaContainer = meshRoot().getMicroschemaContainerRoot().create(microschema, user());
			testPermission(GraphPermission.DELETE_PERM, microschemaContainer);
		}

	}

	@Test
	@Override
	public void testUpdatePermission() throws MeshJsonException {
		try (Tx tx = tx()) {
			MicroschemaModel microschema = new MicroschemaModelImpl();
			microschema.setName("someNewMicroschema");
			MicroschemaContainer microschemaContainer = meshRoot().getMicroschemaContainerRoot().create(microschema, user());
			testPermission(GraphPermission.UPDATE_PERM, microschemaContainer);
		}
	}

	@Test
	@Override
	public void testCreatePermission() throws MeshJsonException {
		try (Tx tx = tx()) {
			MicroschemaModel microschema = new MicroschemaModelImpl();
			microschema.setName("someNewMicroschema");
			MicroschemaContainer microschemaContainer = meshRoot().getMicroschemaContainerRoot().create(microschema, user());
			testPermission(GraphPermission.CREATE_PERM, microschemaContainer);
		}
	}

	@Test
	@Override
	public void testTransformation() throws IOException {
		try (Tx tx = tx()) {
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			MicroschemaContainer vcard = microschemaContainer("vcard");
			MicroschemaResponse schema = vcard.transformToRest(ac, 0, "en").blockingGet();
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
			MicroschemaContainerRoot root = meshRoot().getMicroschemaContainerRoot();

			MicroschemaModel microschema = new MicroschemaModelImpl();
			microschema.setName("someNewMicroschema");
			MicroschemaContainer container = root.create(microschema, user());

			assertFalse(role().hasPermission(GraphPermission.CREATE_PERM, container));
			getRequestUser().addCRUDPermissionOnRole(meshRoot().getMicroschemaContainerRoot(), GraphPermission.CREATE_PERM, container);
			assertTrue("The addCRUDPermissionOnRole method should add the needed permissions on the new microschema container.",
					role().hasPermission(GraphPermission.CREATE_PERM, container));
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
			MicroschemaContainerVersion vcard = microschemaContainer("vcard").getLatestVersion();

			Microschema microschema = vcard.getSchema();
			Microschema updatedMicroschema = new MicroschemaModelImpl();
			updatedMicroschema.setName(microschema.getName());
			updatedMicroschema.getFields().addAll(microschema.getFields());
			updatedMicroschema.addField(FieldUtil.createStringFieldSchema("newfield"));

			SchemaChangesListModel model = new SchemaChangesListModel();
			model.getChanges().addAll(new MicroschemaComparator().diff(microschema, updatedMicroschema));

			InternalActionContext ac = mockActionContext();
			SearchQueueBatch batch = createBatch();
			vcard.applyChanges(ac, model, batch);
			MicroschemaContainerVersion newVCard = microschemaContainer("vcard").getLatestVersion();

			NodeGraphFieldContainer containerWithBoth = folder("2015").getGraphFieldContainer("en");
			containerWithBoth.createMicronode("single", vcard);
			containerWithBoth.createMicronodeFieldList("list").createMicronode().setSchemaContainerVersion(vcard);

			NodeGraphFieldContainer containerWithField = folder("news").getGraphFieldContainer("en");
			containerWithField.createMicronode("single", vcard);

			NodeGraphFieldContainer containerWithList = folder("products").getGraphFieldContainer("en");
			containerWithList.createMicronodeFieldList("list").createMicronode().setSchemaContainerVersion(vcard);

			NodeGraphFieldContainer containerWithOtherVersion = folder("deals").getGraphFieldContainer("en");
			containerWithOtherVersion.createMicronode("single", newVCard);

			Iterator<? extends NodeGraphFieldContainer> it = vcard.getDraftFieldContainers(project().getLatestRelease().getUuid());
			List<NodeGraphFieldContainer> containers = TestUtils.toList(it);
			assertThat(containers).containsOnly(containerWithBoth, containerWithField, containerWithList).hasSize(3);
		}
	}
}
