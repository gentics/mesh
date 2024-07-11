package com.gentics.mesh.core.branch;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.FULL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.branch.BranchSchemaVersion;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparatorImpl;
import com.gentics.mesh.core.data.schema.handler.SchemaComparatorImpl;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
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
			BranchDao branchDao = tx.branchDao();
			EventQueueBatch batch = createBatch();
			Branch initialBranch = tx.<CommonTx>unwrap().load(project().getInitialBranch().getId(), tx.<CommonTx>unwrap().branchDao().getPersistenceClass(project()));
			Branch branchOne = branchDao.create(project(), "One", user(), batch);
			Branch branchTwo = branchDao.create(project(), "Two", user(), batch);
			Branch branchThree = branchDao.create(project(), "Three", user(), batch);

			List<String> branchNames  = branchDao.findAll(project(), mockActionContext(), new PagingParametersImpl(1, 25L))
					.getWrappedList().stream().map(Branch::getName).collect(Collectors.toList());
			assertThat(branchNames).isNotNull();
			assertThat(branchNames).contains(initialBranch.getName(), branchOne.getName(), branchTwo.getName(), branchThree.getName());
		}
	}

	@Test
	@Override
	public void testFindAll() throws Exception {
		try (Tx tx = tx()) {
			BranchDao branchDao = tx.branchDao();
			Project project = project();
			Branch initialBranch = initialBranch();
			Branch branchOne = createBranch("One");
			Branch branchTwo = createBranch("Two");
			Branch branchThree = createBranch("Three");

			List<? extends Branch> branchList = branchDao.findAll(project).list();
			assertThat(new ArrayList<Branch>(branchList)).usingElementComparatorOnFields("uuid").containsExactlyInAnyOrder(initialBranch,
				branchOne, branchTwo, branchThree);
		}
	}

	@Test
	@Override
	public void testRootNode() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			BaseElement branchRoot = project.getBranchPermissionRoot();
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
			BranchDao branchDao = tx.branchDao();
			Project project = project();
			Branch foundBranch = branchDao.findByName(project, project.getName());
			assertThat(foundBranch).as("Branch with name " + project.getName()).isNotNull().matches(project.getInitialBranch());
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (Tx tx = tx()) {
			BranchDao branchDao = tx.branchDao();
			Project project = project();
			Branch initialBranch = project.getInitialBranch();
			Branch foundBranch = branchDao.findByUuid(project, initialBranch.getUuid());
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
			BranchDao branchDao = tx.branchDao();
			SchemaDao schemaDao = tx.schemaDao();

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

			assertThat(new ArrayList<Branch>(branchDao.findAll(project).list())).usingElementComparatorOnFields("uuid").containsExactlyInAnyOrder(
				initialBranch,
				firstNewBranch, secondNewBranch, thirdNewBranch);

			for (Schema schema : schemaDao.findAll(project)) {
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
			testPermission(InternalPermission.READ_PERM, newBranch);
		}
	}

	@Test
	@Override
	public void testDeletePermission() throws Exception {
		try (Tx tx = tx()) {
			Branch newBranch = createBranch("New Branch");
			testPermission(InternalPermission.DELETE_PERM, newBranch);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() throws Exception {
		try (Tx tx = tx()) {
			Branch newBranch = createBranch("New Branch");
			testPermission(InternalPermission.UPDATE_PERM, newBranch);
		}
	}

	@Test
	@Override
	public void testCreatePermission() throws Exception {
		try (Tx tx = tx()) {
			Branch newBranch = createBranch("New Branch");
			testPermission(InternalPermission.CREATE_PERM, newBranch);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			BranchDao branchDao = tx.branchDao();
			Branch branch = project().getInitialBranch();

			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);

			BranchResponse branchResponse = branchDao.transformToRestSync(branch, ac, 0);
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
			PersistingSchemaDao schemaDao = tx.<CommonTx>unwrap().schemaDao();
			PersistingBranchDao branchDao = tx.<CommonTx>unwrap().branchDao();
			Project project = project();
			Branch branch = latestBranch();
			List<SchemaVersion> versions = schemaDao.findAll(project).stream().filter(v -> !v.getName().equals("content"))
				.map(Schema::getLatestVersion).collect(Collectors.toList());

			Schema schema = schemaContainer("content");

			SchemaVersion newVersion = schemaDao.createPersistedVersion(schema, v -> {
				v.setVersion("4.0");
				v.setName("content");
				v.setSchemaContainer(schema);
			});

			versions.add(newVersion);
			branchDao.connectToSchemaVersion(branch, newVersion);

			List<SchemaVersion> found = new ArrayList<>();
			for (BranchSchemaVersion versionedge : branch.findAllLatestSchemaVersionEdges()) {
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
			SchemaDao schemaDao = tx.schemaDao();

			Schema schemaContainer = createSchemaDirect("bla");
			updateSchema(schemaContainer, "newfield");
			SchemaVersion latestVersion = schemaContainer.getLatestVersion();

			assertThat(latestVersion).as("latest version").isNotNull();
			SchemaVersion previousVersion = latestVersion.getPreviousVersion();
			assertThat(previousVersion).as("Previous version").isNotNull();

			Project project = project();
			Branch initialBranch = project.getInitialBranch();
			Branch newBranch = createBranch("New Branch");

			for (Branch branch : Arrays.asList(initialBranch, newBranch)) {
				branch = tx.<CommonTx>unwrap().load(branch.getId(), tx.<CommonTx>unwrap().branchDao().getPersistenceClass(project));
				assertThat(branch).as(branch.getName()).hasNotSchema(schemaContainer).hasNotSchemaVersion(latestVersion)
					.hasNotSchemaVersion(previousVersion);
			}

			// assign the schema to the project
			EventQueueBatch batch = createBatch();
			schemaDao.assign(schemaContainer, project(), user(), batch);

			for (Branch branch : Arrays.asList(initialBranch, newBranch)) {
				branch = tx.<CommonTx>unwrap().load(branch.getId(), tx.<CommonTx>unwrap().branchDao().getPersistenceClass(project));
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
			SchemaDao schemaDao = tx.schemaDao();
			Project project = project();
			List<? extends Schema> schemas = schemaDao.findAll(project).list();
			Schema schemaContainer = schemas.get(0);

			Branch initialBranch = initialBranch();
			Branch newBranch = createBranch("New Branch");

			EventQueueBatch batch = createBatch();
			schemaDao.unassign(schemaContainer, project, batch);
			for (Branch branch : Arrays.asList(initialBranch, newBranch)) {
				branch = tx.<CommonTx>unwrap().load(branch.getId(), tx.<CommonTx>unwrap().branchDao().getPersistenceClass(project));
				assertThat(branch).as(branch.getName()).hasNotSchema(schemaContainer).hasNotSchemaVersion(schemaContainer.getLatestVersion());
			}
		}
	}

	@Test
	public void testFindActiveSchemaVersions() {
		try (Tx tx = tx()) {
			SchemaDao schemaDao = tx.schemaDao();

			Project project = project();
			Branch branch = latestBranch();
			List<SchemaVersion> versions = schemaDao.findAll(project).stream().map(Schema::getLatestVersion)
				.collect(Collectors.toList());

			List<SchemaVersion> activeVersions = TestUtils.toList(branch.findActiveSchemaVersions());
			assertThat(activeVersions).as("List of schema versions").usingElementComparatorOnFields("uuid", "name", "version").containsAll(versions);
		}
	}

	@Test
	public void testBranchSchemaVersion() throws Exception {
		try (Tx tx = tx()) {
			SchemaDao schemaDao = tx.schemaDao();
			Project project = project();

			Schema schemaContainer = createSchemaDirect("bla");
			SchemaVersion firstVersion = schemaContainer.getLatestVersion();

			// assign the schema to the project
			EventQueueBatch batch = createBatch();
			schemaDao.assign(schemaContainer, project, user(), batch);

			// update schema
			updateSchema(schemaContainer, "newfield");
			SchemaVersion secondVersion = schemaContainer.getLatestVersion();

			Branch initialBranch = reloadBranch(initialBranch());
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
			List<MicroschemaVersion> versions = tx.microschemaDao().findAll(project).stream()
				.map(Microschema::getLatestVersion).collect(Collectors.toList());

			List<MicroschemaVersion> found = new ArrayList<>();
			for (MicroschemaVersion version : project.getInitialBranch().findAllMicroschemaVersions()) {
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
			Microschema microschema = createMicroschemaDirect("bla");
			updateMicroschema(microschema, "newfield");
			MicroschemaVersion latestVersion = microschema.getLatestVersion();

			assertThat(latestVersion).as("latest version").isNotNull();
			MicroschemaVersion previousVersion = latestVersion.getPreviousVersion();
			assertThat(previousVersion).as("Previous version").isNotNull();

			Project project = project();
			Branch initialBranch = initialBranch();
			Branch newBranch = createBranch("New Branch");

			for (Branch branch : Arrays.asList(initialBranch, newBranch)) {
				branch = tx.<CommonTx>unwrap().load(branch.getId(), tx.<CommonTx>unwrap().branchDao().getPersistenceClass(project));
				assertThat(branch).as(branch.getName()).hasNotMicroschema(microschema).hasNotMicroschemaVersion(latestVersion)
					.hasNotMicroschemaVersion(previousVersion);
			}

			// assign the schema to the project
			tx.microschemaDao().assign(microschema, project, user(), createBatch());

			for (Branch branch : Arrays.asList(initialBranch, newBranch)) {
				branch = tx.<CommonTx>unwrap().load(branch.getId(), tx.<CommonTx>unwrap().branchDao().getPersistenceClass(project));
				assertThat(branch).as(branch.getName()).hasMicroschema(microschema).hasMicroschemaVersion(latestVersion)
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
			MicroschemaDao microschemaDao = tx.microschemaDao();
			Project project = project();
			List<? extends Microschema> microschemas = microschemaDao.findAll(project).list();
			Microschema microschema = microschemas.get(0);

			Branch initialBranch = initialBranch();
			Branch newBranch = createBranch("New Branch");

			microschemaDao.unassign(microschema, project, createBatch());

			for (Branch branch : Arrays.asList(initialBranch, newBranch)) {
				branch = tx.<CommonTx>unwrap().load(branch.getId(), tx.<CommonTx>unwrap().branchDao().getPersistenceClass(project));
				assertThat(branch).as(branch.getName()).hasNotMicroschema(microschema)
					.hasNotMicroschemaVersion(microschema.getLatestVersion());
			}
		}
	}

	@Test
	public void testBranchMicroschemaVersion() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();

			Microschema microschema = createMicroschemaDirect("bla");
			MicroschemaVersion firstVersion = microschema.getLatestVersion();

			// assign the microschema to the project
			tx.microschemaDao().assign(microschema, project, user(), createBatch());

			// update microschema
			updateMicroschema(microschema, "newfield");
			MicroschemaVersion secondVersion = microschema.getLatestVersion();

			Branch initialBranch = reloadBranch(initialBranch());
			Branch newBranch = createBranch("New Branch");

			assertThat(initialBranch).as(initialBranch.getName()).hasMicroschema(microschema).hasMicroschemaVersion(firstVersion)
				.hasNotMicroschemaVersion(secondVersion);
			assertThat(newBranch).as(newBranch.getName()).hasMicroschema(microschema).hasNotMicroschemaVersion(firstVersion)
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
	protected Schema createSchemaDirect(String name) throws Exception {
		SchemaVersionModel schema = new SchemaModelImpl();
		schema.setName(name);
		schema.addField(FieldUtil.createStringFieldSchema("name"));
		schema.setDisplayField("name");
		SchemaDao schemaDao = Tx.get().schemaDao();
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
	protected void updateSchema(Schema schemaContainer, String newName) throws Exception {
		SchemaModel schema = schemaContainer.getLatestVersion().getSchema();

		SchemaModel updatedSchema = new SchemaModelImpl();
		updatedSchema.setName(schema.getName());
		updatedSchema.setDisplayField(schema.getDisplayField());
		updatedSchema.getFields().addAll(schema.getFields());
		updatedSchema.addField(FieldUtil.createStringFieldSchema(newName));

		SchemaChangesListModel model = new SchemaChangesListModel();
		model.getChanges().addAll(new SchemaComparatorImpl().diff(schema, updatedSchema));

		InternalActionContext ac = mockActionContext();
		EventQueueBatch batch = createBatch();
		Tx.get().schemaDao().applyChanges(schemaContainer.getLatestVersion(), ac, model, batch);
	}

	/**
	 * Create a new microschema with a single string field "name"
	 * 
	 * @param name
	 *            microschema name
	 * @return microschema container
	 * @throws Exception
	 */
	protected Microschema createMicroschemaDirect(String name) throws Exception {
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
	protected void updateMicroschema(Microschema microschemaContainer, String newName) throws Exception {
		MicroschemaModel microschemaModel = microschemaContainer.getLatestVersion().getSchema();

		MicroschemaModel updatedMicroschemaModel = new MicroschemaModelImpl();
		updatedMicroschemaModel.setName(microschemaModel.getName());
		updatedMicroschemaModel.getFields().addAll(microschemaModel.getFields());
		updatedMicroschemaModel.addField(FieldUtil.createStringFieldSchema(newName));

		SchemaChangesListModel model = new SchemaChangesListModel();
		model.getChanges().addAll(new MicroschemaComparatorImpl().diff(microschemaModel, updatedMicroschemaModel));

		InternalActionContext ac = mockActionContext();
		EventQueueBatch batch = createBatch();
		Tx.get().microschemaDao().applyChanges(microschemaContainer.getLatestVersion(), ac, model, batch);
	}
}
