package com.gentics.mesh.core.branch;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_VERSION;
import static com.gentics.mesh.test.TestSize.FULL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.BranchSchemaEdge;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.TestUtils;

import io.vertx.ext.web.RoutingContext;

@MeshTestSetting(testSize = FULL, startServer = false)
public class BranchTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			Branch branch = project().getInitialBranch();
			BranchReference reference = branch.transformToReference();
			assertThat(reference).isNotNull();
			assertThat(reference.getName()).as("Reference name").isEqualTo(branch.getName());
			assertThat(reference.getUuid()).as("Reference uuid").isEqualTo(branch.getUuid());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws Exception {
		try (Tx tx = tx()) {
			EventQueueBatch batch = createBatch();
			Project project = project();
			BranchRoot branchRoot = project.getBranchRoot();
			Branch initialBranch = branchRoot.getInitialBranch();
			Branch branchOne = branchRoot.create("One", user(), batch);
			Branch branchTwo = branchRoot.create("Two", user(), batch);
			Branch branchThree = branchRoot.create("Three", user(), batch);

			Page<? extends Branch> page = branchRoot.findAll(mockActionContext(), new PagingParametersImpl(1, 25L));
			assertThat(page).isNotNull();
			ArrayList<Branch> arrayList = new ArrayList<Branch>();
			page.iterator().forEachRemaining(r -> arrayList.add(r));
			assertThat(arrayList).contains(initialBranch, branchOne, branchTwo, branchThree);
		}
	}

	@Test
	@Override
	public void testFindAll() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			Branch initialBranch = initialBranch();
			Branch branchOne = createBranch("One");
			Branch branchTwo = createBranch("Two");
			Branch branchThree = createBranch("Three");

			BranchRoot branchRoot = project.getBranchRoot();
			assertThat(new ArrayList<Branch>(branchRoot.findAll().list())).usingElementComparatorOnFields("uuid").containsExactly(initialBranch,
				branchOne, branchTwo, branchThree);
		}
	}

	@Test
	@Override
	public void testRootNode() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			BranchRoot branchRoot = project.getBranchRoot();
			assertThat(branchRoot).as("Branch Root of Project").isNotNull();
			Branch initialBranch = project.getInitialBranch();
			assertThat(initialBranch).as("Initial Branch of Project").isNotNull().isActive().isNamed(project.getName()).hasUuid().hasNext(null)
				.hasPrevious(null);
			Branch latestBranch = project.getLatestBranch();
			assertThat(latestBranch).as("Latest Branch of Project").matches(initialBranch);
		}
	}

	@Test
	@Override
	public void testFindByName() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			BranchRoot branchRoot = project.getBranchRoot();
			Branch foundBranch = branchRoot.findByName(project.getName());
			assertThat(foundBranch).as("Branch with name " + project.getName()).isNotNull().matches(project.getInitialBranch());
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			BranchRoot branchRoot = project.getBranchRoot();
			Branch initialBranch = project.getInitialBranch();

			Branch foundBranch = branchRoot.findByUuid(initialBranch.getUuid());
			assertThat(foundBranch).as("Branch with uuid " + initialBranch.getUuid()).isNotNull().matches(initialBranch);
		}
	}

	@Test
	@Override
	public void testRead() throws Exception {
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		try (Tx tx = tx()) {
			Branch initialBranch = initialBranch();
			Branch firstNewBranch = createBranch("First new Branch");
			Branch secondNewBranch = createBranch("Second new Branch");
			Branch thirdNewBranch = createBranch("Third new Branch");

			Project project = project();
			assertThat(project.getInitialBranch()).as("Initial Branch").matches(initialBranch).hasNext(firstNewBranch).hasPrevious(null);
			assertThat(firstNewBranch).as("First new Branch").isNamed("First new Branch").hasNext(secondNewBranch).hasPrevious(initialBranch);
			assertThat(secondNewBranch).as("Second new Branch").isNamed("Second new Branch").hasNext(thirdNewBranch).hasPrevious(firstNewBranch);
			assertThat(project.getLatestBranch()).as("Latest Branch").isNamed("Third new Branch").matches(thirdNewBranch).hasNext(null)
				.hasPrevious(secondNewBranch);

			BranchRoot branchRoot = project.getBranchRoot();
			assertThat(new ArrayList<Branch>(branchRoot.findAll().list())).usingElementComparatorOnFields("uuid").containsExactly(initialBranch,
				firstNewBranch, secondNewBranch, thirdNewBranch);

			for (SchemaContainer schema : project.getSchemaContainerRoot().findAll()) {
				for (Branch branch : Arrays.asList(initialBranch, firstNewBranch, secondNewBranch, thirdNewBranch)) {
					assertThat(branch).as(branch.getName()).hasSchema(schema).hasSchemaVersion(schema.getLatestVersion());
				}
			}
		}
	}

	@Override
	public void testDelete() throws Exception {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdate() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			Branch initialBranch = project.getInitialBranch();
			initialBranch.setName("New Branch Name");
			initialBranch.setActive(false);
			assertThat(initialBranch).as("Branch").isNamed("New Branch Name").isInactive();
		}
	}

	@Test
	@Override
	public void testReadPermission() throws Exception {
		try (Tx tx = tx()) {
			Branch newBranch = createBranch("New Branch");
			testPermission(GraphPermission.READ_PERM, newBranch);
		}
	}

	@Test
	@Override
	public void testDeletePermission() throws Exception {
		try (Tx tx = tx()) {
			Branch newBranch = createBranch("New Branch");
			testPermission(GraphPermission.DELETE_PERM, newBranch);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() throws Exception {
		try (Tx tx = tx()) {
			Branch newBranch = createBranch("New Branch");
			testPermission(GraphPermission.UPDATE_PERM, newBranch);
		}
	}

	@Test
	@Override
	public void testCreatePermission() throws Exception {
		try (Tx tx = tx()) {
			Branch newBranch = createBranch("New Branch");
			testPermission(GraphPermission.CREATE_PERM, newBranch);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			Branch branch = project().getInitialBranch();

			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);

			BranchResponse branchResponse = branch.transformToRestSync(ac, 0);
			assertThat(branchResponse).isNotNull().hasName(branch.getName()).hasUuid(branch.getUuid()).isActive().isMigrated();
		}
	}

	@Override
	public void testCreateDelete() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void testCRUDPermissions() throws Exception {
		// TODO Auto-generated method stub
	}

	@Test
	public void testReadSchemaVersions() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			Branch branch = latestBranch();
			List<SchemaContainerVersion> versions = project.getSchemaContainerRoot().findAll().stream().filter(v -> !v.getName().equals("content"))
				.map(SchemaContainer::getLatestVersion).collect(Collectors.toList());

			SchemaContainerVersionImpl newVersion = tx.getGraph().addFramedVertexExplicit(SchemaContainerVersionImpl.class);
			newVersion.setVersion("4.0");
			newVersion.setName("content");
			versions.add(newVersion);
			newVersion.setSchemaContainer(schemaContainer("content"));
			branch.linkOut(newVersion, HAS_SCHEMA_VERSION);

			List<SchemaContainerVersion> found = new ArrayList<>();
			for (BranchSchemaEdge versionedge : branch.findAllLatestSchemaVersionEdges()) {
				found.add(versionedge.getSchemaContainerVersion());
			}
			assertThat(found).as("List of schema versions").usingElementComparatorOnFields("uuid", "name", "version")
				.containsOnlyElementsOf(versions);
		}
	}

	/**
	 * Test assigning a schema to a project
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAssignSchema() throws Exception {
		try (Tx tx = tx()) {
			SchemaContainer schemaContainer = createSchemaDirect("bla");
			updateSchema(schemaContainer, "newfield");
			SchemaContainerVersion latestVersion = schemaContainer.getLatestVersion();

			assertThat(latestVersion).as("latest version").isNotNull();
			SchemaContainerVersion previousVersion = latestVersion.getPreviousVersion();
			assertThat(previousVersion).as("Previous version").isNotNull();

			Project project = project();
			Branch initialBranch = project.getInitialBranch();
			Branch newBranch = createBranch("New Branch");

			for (Branch branch : Arrays.asList(initialBranch, newBranch)) {
				assertThat(branch).as(branch.getName()).hasNotSchema(schemaContainer).hasNotSchemaVersion(latestVersion)
					.hasNotSchemaVersion(previousVersion);
			}

			// assign the schema to the project
			EventQueueBatch batch = createBatch();
			project.getSchemaContainerRoot().addSchemaContainer(user(), schemaContainer, batch);

			for (Branch branch : Arrays.asList(initialBranch, newBranch)) {
				assertThat(branch).as(branch.getName()).hasSchema(schemaContainer).hasSchemaVersion(latestVersion)
					.hasNotSchemaVersion(previousVersion);
			}
		}
	}

	/**
	 * Test unassigning a schema from a project
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUnassignSchema() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			List<? extends SchemaContainer> schemas = project.getSchemaContainerRoot().findAll().list();
			SchemaContainer schemaContainer = schemas.get(0);

			Branch initialBranch = initialBranch();
			Branch newBranch = createBranch("New Branch");

			EventQueueBatch batch = createBatch();
			project.getSchemaContainerRoot().removeSchemaContainer(schemaContainer, batch);
			for (Branch branch : Arrays.asList(initialBranch, newBranch)) {
				assertThat(branch).as(branch.getName()).hasNotSchema(schemaContainer).hasNotSchemaVersion(schemaContainer.getLatestVersion());
			}
		}
	}

	@Test
	public void testFindActiveSchemaVersions() {
		try (Tx tx = tx()) {

			Project project = project();
			Branch branch = latestBranch();
			List<SchemaContainerVersion> versions = project.getSchemaContainerRoot().findAll().stream().map(SchemaContainer::getLatestVersion)
				.collect(Collectors.toList());

			List<SchemaContainerVersion> activeVersions = TestUtils.toList(branch.findActiveSchemaVersions());
			assertThat(activeVersions).as("List of schema versions").usingElementComparatorOnFields("uuid", "name", "version").containsAll(versions);
		}
	}

	@Test
	public void testBranchSchemaVersion() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();

			SchemaContainer schemaContainer = createSchemaDirect("bla");
			SchemaContainerVersion firstVersion = schemaContainer.getLatestVersion();

			// assign the schema to the project
			EventQueueBatch batch = createBatch();
			project.getSchemaContainerRoot().addSchemaContainer(user(), schemaContainer, batch);

			// update schema
			updateSchema(schemaContainer, "newfield");
			SchemaContainerVersion secondVersion = schemaContainer.getLatestVersion();

			Branch initialBranch = initialBranch();
			Branch newBranch = createBranch("New Branch");

			assertThat(initialBranch).as(initialBranch.getName()).hasSchema(schemaContainer).hasSchemaVersion(firstVersion)
				.hasNotSchemaVersion(secondVersion);
			assertThat(newBranch).as(newBranch.getName()).hasSchema(schemaContainer).hasNotSchemaVersion(firstVersion)
				.hasSchemaVersion(secondVersion);
		}
	}

	@Test
	public void testReadMicroschemaVersions() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			List<MicroschemaContainerVersion> versions = project.getMicroschemaContainerRoot().findAll().stream()
				.map(MicroschemaContainer::getLatestVersion).collect(Collectors.toList());

			List<MicroschemaContainerVersion> found = new ArrayList<>();
			for (MicroschemaContainerVersion version : project.getInitialBranch().findAllMicroschemaVersions()) {
				found.add(version);
			}
			assertThat(found).as("List of microschema versions").usingElementComparatorOnFields("uuid", "name", "version").containsAll(versions);
		}
	}

	/**
	 * Test assigning a microschema to a project
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAssignMicroschema() throws Exception {
		try (Tx tx = tx()) {
			MicroschemaContainer microschemaContainer = createMicroschemaDirect("bla");
			updateMicroschema(microschemaContainer, "newfield");
			MicroschemaContainerVersion latestVersion = microschemaContainer.getLatestVersion();

			assertThat(latestVersion).as("latest version").isNotNull();
			MicroschemaContainerVersion previousVersion = latestVersion.getPreviousVersion();
			assertThat(previousVersion).as("Previous version").isNotNull();

			Project project = project();
			Branch initialBranch = initialBranch();
			Branch newBranch = createBranch("New Branch");

			for (Branch branch : Arrays.asList(initialBranch, newBranch)) {
				assertThat(branch).as(branch.getName()).hasNotMicroschema(microschemaContainer).hasNotMicroschemaVersion(latestVersion)
					.hasNotMicroschemaVersion(previousVersion);
			}

			// assign the schema to the project
			project.getMicroschemaContainerRoot().addMicroschema(user(), microschemaContainer, createBatch());

			for (Branch branch : Arrays.asList(initialBranch, newBranch)) {
				assertThat(branch).as(branch.getName()).hasMicroschema(microschemaContainer).hasMicroschemaVersion(latestVersion)
					.hasNotMicroschemaVersion(previousVersion);
			}
		}
	}

	/**
	 * Test unassigning a microschema from a project
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUnassignMicroschema() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			List<? extends MicroschemaContainer> microschemas = project.getMicroschemaContainerRoot().findAll().list();
			MicroschemaContainer microschemaContainer = microschemas.get(0);

			Branch initialBranch = initialBranch();
			Branch newBranch = createBranch("New Branch");

			project.getMicroschemaContainerRoot().removeMicroschema(microschemaContainer, createBatch());

			for (Branch branch : Arrays.asList(initialBranch, newBranch)) {
				assertThat(branch).as(branch.getName()).hasNotMicroschema(microschemaContainer)
					.hasNotMicroschemaVersion(microschemaContainer.getLatestVersion());
			}
		}
	}

	@Test
	public void testBranchMicroschemaVersion() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();

			MicroschemaContainer microschemaContainer = createMicroschemaDirect("bla");
			MicroschemaContainerVersion firstVersion = microschemaContainer.getLatestVersion();

			// assign the microschema to the project
			project.getMicroschemaContainerRoot().addMicroschema(user(), microschemaContainer, createBatch());

			// update microschema
			updateMicroschema(microschemaContainer, "newfield");
			MicroschemaContainerVersion secondVersion = microschemaContainer.getLatestVersion();

			Branch initialBranch = initialBranch();
			Branch newBranch = createBranch("New Branch");

			assertThat(initialBranch).as(initialBranch.getName()).hasMicroschema(microschemaContainer).hasMicroschemaVersion(firstVersion)
				.hasNotMicroschemaVersion(secondVersion);
			assertThat(newBranch).as(newBranch.getName()).hasMicroschema(microschemaContainer).hasNotMicroschemaVersion(firstVersion)
				.hasMicroschemaVersion(secondVersion);
		}
	}

	/**
	 * Create a new schema with a single string field "name"
	 * 
	 * @param name
	 *            schema name
	 * @return schema container
	 * @throws Exception
	 */
	protected SchemaContainer createSchemaDirect(String name) throws Exception {
		SchemaModel schema = new SchemaModelImpl();
		schema.setName(name);
		schema.addField(FieldUtil.createStringFieldSchema("name"));
		schema.setDisplayField("name");
		SchemaDaoWrapper schemaDao = Tx.get().data().schemaDao();
		return schemaDao.create(schema, user());
	}

	/**
	 * Update the schema container by adding a new string field with given name and reload the schema container
	 * 
	 * @param schemaContainer
	 *            schema container
	 * @param newName
	 *            new name
	 * @throws Exception
	 */
	protected void updateSchema(SchemaContainer schemaContainer, String newName) throws Exception {
		Schema schema = schemaContainer.getLatestVersion().getSchema();

		Schema updatedSchema = new SchemaModelImpl();
		updatedSchema.setName(schema.getName());
		updatedSchema.setDisplayField(schema.getDisplayField());
		updatedSchema.getFields().addAll(schema.getFields());
		updatedSchema.addField(FieldUtil.createStringFieldSchema(newName));

		SchemaChangesListModel model = new SchemaChangesListModel();
		model.getChanges().addAll(new SchemaComparator().diff(schema, updatedSchema));

		InternalActionContext ac = mockActionContext();
		EventQueueBatch batch = createBatch();
		schemaContainer.getLatestVersion().applyChanges(ac, model, batch);
	}

	/**
	 * Create a new microschema with a single string field "name"
	 * 
	 * @param name
	 *            microschema name
	 * @return microschema container
	 * @throws Exception
	 */
	protected MicroschemaContainer createMicroschemaDirect(String name) throws Exception {
		MicroschemaModelImpl microschema = new MicroschemaModelImpl();
		microschema.setName(name);
		microschema.addField(FieldUtil.createStringFieldSchema("name"));
		return createMicroschema(microschema);
	}

	/**
	 * Update the microschema container by adding a new string field with given name and reload the microschema container
	 * 
	 * @param microschemaContainer
	 *            microschema container
	 * @param newName
	 *            new name
	 * @throws Exception
	 */
	protected void updateMicroschema(MicroschemaContainer microschemaContainer, String newName) throws Exception {
		Microschema microschema = microschemaContainer.getLatestVersion().getSchema();

		Microschema updatedMicroschema = new MicroschemaModelImpl();
		updatedMicroschema.setName(microschema.getName());
		updatedMicroschema.getFields().addAll(microschema.getFields());
		updatedMicroschema.addField(FieldUtil.createStringFieldSchema(newName));

		SchemaChangesListModel model = new SchemaChangesListModel();
		model.getChanges().addAll(new MicroschemaComparator().diff(microschema, updatedMicroschema));

		InternalActionContext ac = mockActionContext();
		EventQueueBatch batch = createBatch();
		microschemaContainer.getLatestVersion().applyChanges(ac, model, batch);
	}
}
