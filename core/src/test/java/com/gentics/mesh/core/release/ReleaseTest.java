package com.gentics.mesh.core.release;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_VERSION;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.BranchSchemaEdge;
import com.gentics.mesh.core.data.Branch;
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
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.TestUtils;
import com.syncleus.ferma.tx.Tx;

import io.vertx.ext.web.RoutingContext;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class ReleaseTest extends AbstractMeshTest implements BasicObjectTestcases {

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
			Project project = project();
			BranchRoot releaseRoot = project.getBranchRoot();
			Branch initialRelease = releaseRoot.getInitialRelease();
			Branch releaseOne = releaseRoot.create("One", user());
			Branch releaseTwo = releaseRoot.create("Two", user());
			Branch releaseThree = releaseRoot.create("Three", user());

			Page<? extends Branch> page = releaseRoot.findAll(mockActionContext(), new PagingParametersImpl(1, 25));
			assertThat(page).isNotNull();
			ArrayList<Branch> arrayList = new ArrayList<Branch>();
			page.iterator().forEachRemaining(r -> arrayList.add(r));
			assertThat(arrayList).contains(initialRelease, releaseOne, releaseTwo, releaseThree);
		}
	}

	@Test
	@Override
	public void testFindAll() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			BranchRoot releaseRoot = project.getBranchRoot();
			Branch initialRelease = releaseRoot.getInitialRelease();
			Branch releaseOne = releaseRoot.create("One", user());
			Branch releaseTwo = releaseRoot.create("Two", user());
			Branch releaseThree = releaseRoot.create("Three", user());

			assertThat(new ArrayList<Branch>(releaseRoot.findAll())).usingElementComparatorOnFields("uuid").containsExactly(initialRelease,
					releaseOne, releaseTwo, releaseThree);
		}
	}

	@Test
	@Override
	public void testRootNode() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			BranchRoot releaseRoot = project.getBranchRoot();
			assertThat(releaseRoot).as("Release Root of Project").isNotNull();
			Branch initialRelease = project.getInitialBranch();
			assertThat(initialRelease).as("Initial Release of Project").isNotNull().isActive().isNamed(project.getName()).hasUuid().hasNext(null)
					.hasPrevious(null);
			Branch latestRelease = project.getLatestBranch();
			assertThat(latestRelease).as("Latest Release of Project").matches(initialRelease);
		}
	}

	@Test
	@Override
	public void testFindByName() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			BranchRoot releaseRoot = project.getBranchRoot();
			Branch foundRelease = releaseRoot.findByName(project.getName());
			assertThat(foundRelease).as("Release with name " + project.getName()).isNotNull().matches(project.getInitialBranch());
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			BranchRoot releaseRoot = project.getBranchRoot();
			Branch initialRelease = project.getInitialBranch();

			Branch foundRelease = releaseRoot.findByUuid(initialRelease.getUuid());
			assertThat(foundRelease).as("Release with uuid " + initialRelease.getUuid()).isNotNull().matches(initialRelease);
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
			Project project = project();
			BranchRoot releaseRoot = project.getBranchRoot();
			Branch initialRelease = releaseRoot.getInitialRelease();
			Branch firstNewRelease = releaseRoot.create("First new Release", user());
			Branch secondNewRelease = releaseRoot.create("Second new Release", user());
			Branch thirdNewRelease = releaseRoot.create("Third new Release", user());

			assertThat(project.getInitialBranch()).as("Initial Release").matches(initialRelease).hasNext(firstNewRelease).hasPrevious(null);
			assertThat(firstNewRelease).as("First new Release").isNamed("First new Release").hasNext(secondNewRelease).hasPrevious(initialRelease);
			assertThat(secondNewRelease).as("Second new Release").isNamed("Second new Release").hasNext(thirdNewRelease).hasPrevious(firstNewRelease);
			assertThat(project.getLatestBranch()).as("Latest Release").isNamed("Third new Release").matches(thirdNewRelease).hasNext(null)
					.hasPrevious(secondNewRelease);

			assertThat(new ArrayList<Branch>(releaseRoot.findAll())).usingElementComparatorOnFields("uuid").containsExactly(initialRelease,
					firstNewRelease, secondNewRelease, thirdNewRelease);

			for (SchemaContainer schema : project.getSchemaContainerRoot().findAllIt()) {
				for (Branch release : Arrays.asList(initialRelease, firstNewRelease, secondNewRelease, thirdNewRelease)) {
					assertThat(release).as(release.getName()).hasSchema(schema).hasSchemaVersion(schema.getLatestVersion());
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
			Branch initialRelease = project.getInitialBranch();
			initialRelease.setName("New Release Name");
			initialRelease.setActive(false);
			assertThat(initialRelease).as("Release").isNamed("New Release Name").isInactive();
		}
	}

	@Test
	@Override
	public void testReadPermission() throws Exception {
		try (Tx tx = tx()) {
			Branch newRelease = project().getBranchRoot().create("New Release", user());
			testPermission(GraphPermission.READ_PERM, newRelease);
		}
	}

	@Test
	@Override
	public void testDeletePermission() throws Exception {
		try (Tx tx = tx()) {
			Branch newRelease = project().getBranchRoot().create("New Release", user());
			testPermission(GraphPermission.DELETE_PERM, newRelease);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() throws Exception {
		try (Tx tx = tx()) {
			Branch newRelease = project().getBranchRoot().create("New Release", user());
			testPermission(GraphPermission.UPDATE_PERM, newRelease);
		}
	}

	@Test
	@Override
	public void testCreatePermission() throws Exception {
		try (Tx tx = tx()) {
			Branch newRelease = project().getBranchRoot().create("New Release", user());
			testPermission(GraphPermission.CREATE_PERM, newRelease);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			Branch release = project().getInitialBranch();

			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);

			BranchResponse releaseResponse = release.transformToRestSync(ac, 0);
			assertThat(releaseResponse).isNotNull().hasName(release.getName()).hasUuid(release.getUuid()).isActive().isMigrated();
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
			Branch release = latestRelease();
			List<SchemaContainerVersion> versions = project.getSchemaContainerRoot().findAll().stream().filter(v -> !v.getName().equals("content"))
					.map(SchemaContainer::getLatestVersion).collect(Collectors.toList());

			SchemaContainerVersionImpl newVersion = tx.getGraph().addFramedVertexExplicit(SchemaContainerVersionImpl.class);
			newVersion.setVersion("4.0");
			newVersion.setName("content");
			versions.add(newVersion);
			newVersion.setSchemaContainer(schemaContainer("content"));
			release.linkOut(newVersion, HAS_SCHEMA_VERSION);

			List<SchemaContainerVersion> found = new ArrayList<>();
			for (BranchSchemaEdge versionedge : release.findAllLatestSchemaVersionEdges()) {
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
			Branch initialRelease = project.getInitialBranch();
			Branch newRelease = project.getBranchRoot().create("New Release", user());

			for (Branch release : Arrays.asList(initialRelease, newRelease)) {
				assertThat(release).as(release.getName()).hasNotSchema(schemaContainer).hasNotSchemaVersion(latestVersion)
						.hasNotSchemaVersion(previousVersion);
			}

			// assign the schema to the project
			project.getSchemaContainerRoot().addSchemaContainer(user(), schemaContainer);

			for (Branch release : Arrays.asList(initialRelease, newRelease)) {
				assertThat(release).as(release.getName()).hasSchema(schemaContainer).hasSchemaVersion(latestVersion)
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
			List<? extends SchemaContainer> schemas = project.getSchemaContainerRoot().findAll();
			SchemaContainer schemaContainer = schemas.get(0);

			Branch initialRelease = project.getInitialBranch();
			Branch newRelease = project.getBranchRoot().create("New Release", user());

			project.getSchemaContainerRoot().removeSchemaContainer(schemaContainer);
			for (Branch release : Arrays.asList(initialRelease, newRelease)) {
				assertThat(release).as(release.getName()).hasNotSchema(schemaContainer).hasNotSchemaVersion(schemaContainer.getLatestVersion());
			}
		}
	}

	@Test
	public void testFindActiveSchemaVersions() {
		try (Tx tx = tx()) {

			Project project = project();
			Branch release = latestRelease();
			List<SchemaContainerVersion> versions = project.getSchemaContainerRoot().findAll().stream().map(SchemaContainer::getLatestVersion)
					.collect(Collectors.toList());

			List<SchemaContainerVersion> activeVersions = TestUtils.toList(release.findActiveSchemaVersions());
			assertThat(activeVersions).as("List of schema versions").usingElementComparatorOnFields("uuid", "name", "version").containsAll(versions);
		}
	}

	@Test
	public void testReleaseSchemaVersion() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();

			SchemaContainer schemaContainer = createSchemaDirect("bla");
			SchemaContainerVersion firstVersion = schemaContainer.getLatestVersion();

			// assign the schema to the project
			project.getSchemaContainerRoot().addSchemaContainer(user(), schemaContainer);

			// update schema
			updateSchema(schemaContainer, "newfield");
			SchemaContainerVersion secondVersion = schemaContainer.getLatestVersion();

			Branch initialRelease = project.getInitialBranch();
			Branch newRelease = project.getBranchRoot().create("New Release", user());

			assertThat(initialRelease).as(initialRelease.getName()).hasSchema(schemaContainer).hasSchemaVersion(firstVersion)
					.hasNotSchemaVersion(secondVersion);
			assertThat(newRelease).as(newRelease.getName()).hasSchema(schemaContainer).hasNotSchemaVersion(firstVersion)
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
			Branch initialRelease = project.getInitialBranch();
			Branch newRelease = project.getBranchRoot().create("New Release", user());

			for (Branch release : Arrays.asList(initialRelease, newRelease)) {
				assertThat(release).as(release.getName()).hasNotMicroschema(microschemaContainer).hasNotMicroschemaVersion(latestVersion)
						.hasNotMicroschemaVersion(previousVersion);
			}

			// assign the schema to the project
			project.getMicroschemaContainerRoot().addMicroschema(user(), microschemaContainer);

			for (Branch release : Arrays.asList(initialRelease, newRelease)) {
				assertThat(release).as(release.getName()).hasMicroschema(microschemaContainer).hasMicroschemaVersion(latestVersion)
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
			List<? extends MicroschemaContainer> microschemas = project.getMicroschemaContainerRoot().findAll();
			MicroschemaContainer microschemaContainer = microschemas.get(0);

			Branch initialRelease = project.getInitialBranch();
			Branch newRelease = project.getBranchRoot().create("New Release", user());

			project.getMicroschemaContainerRoot().removeMicroschema(microschemaContainer);

			for (Branch release : Arrays.asList(initialRelease, newRelease)) {
				assertThat(release).as(release.getName()).hasNotMicroschema(microschemaContainer)
						.hasNotMicroschemaVersion(microschemaContainer.getLatestVersion());
			}
		}
	}

	@Test
	public void testReleaseMicroschemaVersion() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();

			MicroschemaContainer microschemaContainer = createMicroschemaDirect("bla");
			MicroschemaContainerVersion firstVersion = microschemaContainer.getLatestVersion();

			// assign the microschema to the project
			project.getMicroschemaContainerRoot().addMicroschema(user(), microschemaContainer);

			// update microschema
			updateMicroschema(microschemaContainer, "newfield");
			MicroschemaContainerVersion secondVersion = microschemaContainer.getLatestVersion();

			Branch initialRelease = project.getInitialBranch();
			Branch newRelease = project.getBranchRoot().create("New Release", user());

			assertThat(initialRelease).as(initialRelease.getName()).hasMicroschema(microschemaContainer).hasMicroschemaVersion(firstVersion)
					.hasNotMicroschemaVersion(secondVersion);
			assertThat(newRelease).as(newRelease.getName()).hasMicroschema(microschemaContainer).hasNotMicroschemaVersion(firstVersion)
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
		return meshRoot().getSchemaContainerRoot().create(schema, user());
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
		SearchQueueBatch batch = createBatch();
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
		return meshRoot().getMicroschemaContainerRoot().create(microschema, user());
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
		SearchQueueBatch batch = createBatch();
		microschemaContainer.getLatestVersion().applyChanges(ac, model, batch);
	}
}
