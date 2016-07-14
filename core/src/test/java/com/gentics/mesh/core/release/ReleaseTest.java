package com.gentics.mesh.core.release;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.mock.Mocks.getMockedRoutingContext;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.ReleaseRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.release.ReleaseReference;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.test.AbstractBasicIsolatedObjectTest;
import com.gentics.mesh.util.FieldUtil;

import io.vertx.ext.web.RoutingContext;

public class ReleaseTest extends AbstractBasicIsolatedObjectTest {
	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Release release = project().getInitialRelease();
			ReleaseReference reference = release.transformToReference();
			assertThat(reference).isNotNull();
			assertThat(reference.getName()).as("Reference name").isEqualTo(release.getName());
			assertThat(reference.getUuid()).as("Reference uuid").isEqualTo(release.getUuid());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			ReleaseRoot releaseRoot = project.getReleaseRoot();
			Release initialRelease = releaseRoot.getInitialRelease();
			Release releaseOne = releaseRoot.create("One", user());
			Release releaseTwo = releaseRoot.create("Two", user());
			Release releaseThree = releaseRoot.create("Three", user());

			PageImpl<? extends Release> page = releaseRoot.findAll(getMockedInternalActionContext(user()), new PagingParameters(1, 25));
			assertThat(page).isNotNull();
			ArrayList<Release> arrayList = new ArrayList<Release>();
			page.iterator().forEachRemaining(r -> arrayList.add(r));
			assertThat(arrayList).usingElementComparatorOnFields("uuid").containsExactly(initialRelease, releaseOne, releaseTwo, releaseThree);
		}
	}

	@Test
	@Override
	public void testFindAll() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			ReleaseRoot releaseRoot = project.getReleaseRoot();
			Release initialRelease = releaseRoot.getInitialRelease();
			Release releaseOne = releaseRoot.create("One", user());
			Release releaseTwo = releaseRoot.create("Two", user());
			Release releaseThree = releaseRoot.create("Three", user());

			assertThat(new ArrayList<Release>(releaseRoot.findAll())).usingElementComparatorOnFields("uuid").containsExactly(initialRelease,
					releaseOne, releaseTwo, releaseThree);
		}
	}

	@Test
	@Override
	public void testRootNode() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			ReleaseRoot releaseRoot = project.getReleaseRoot();
			assertThat(releaseRoot).as("Release Root of Project").isNotNull();
			Release initialRelease = project.getInitialRelease();
			assertThat(initialRelease).as("Initial Release of Project").isNotNull().isActive().isNamed(project.getName()).hasUuid().hasNext(null)
					.hasPrevious(null);
			Release latestRelease = project.getLatestRelease();
			assertThat(latestRelease).as("Latest Release of Project").matches(initialRelease);
		}
	}

	@Test
	@Override
	public void testFindByName() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			ReleaseRoot releaseRoot = project.getReleaseRoot();
			Release foundRelease = releaseRoot.findByName(project.getName()).toBlocking().value();
			assertThat(foundRelease).as("Release with name " + project.getName()).isNotNull().matches(project.getInitialRelease());
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			ReleaseRoot releaseRoot = project.getReleaseRoot();
			Release initialRelease = project.getInitialRelease();

			Release foundRelease = releaseRoot.findByUuid(initialRelease.getUuid()).toBlocking().value();
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
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			ReleaseRoot releaseRoot = project.getReleaseRoot();
			Release initialRelease = releaseRoot.getInitialRelease();
			Release firstNewRelease = releaseRoot.create("First new Release", user());
			Release secondNewRelease = releaseRoot.create("Second new Release", user());
			Release thirdNewRelease = releaseRoot.create("Third new Release", user());

			assertThat(project.getInitialRelease()).as("Initial Release").matches(initialRelease).hasNext(firstNewRelease).hasPrevious(null);
			assertThat(firstNewRelease).as("First new Release").isNamed("First new Release").hasNext(secondNewRelease).hasPrevious(initialRelease);
			assertThat(secondNewRelease).as("Second new Release").isNamed("Second new Release").hasNext(thirdNewRelease).hasPrevious(firstNewRelease);
			assertThat(project.getLatestRelease()).as("Latest Release").isNamed("Third new Release").matches(thirdNewRelease).hasNext(null)
					.hasPrevious(secondNewRelease);

			assertThat(new ArrayList<Release>(releaseRoot.findAll())).usingElementComparatorOnFields("uuid").containsExactly(initialRelease,
					firstNewRelease, secondNewRelease, thirdNewRelease);

			for (SchemaContainer schema : project.getSchemaContainerRoot().findAll()) {
				for (Release release : Arrays.asList(initialRelease, firstNewRelease, secondNewRelease, thirdNewRelease)) {
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
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			initialRelease.setName("New Release Name");
			initialRelease.setActive(false);
			initialRelease.reload();

			assertThat(initialRelease).as("Release").isNamed("New Release Name").isInactive();
		}
	}

	@Test
	@Override
	public void testReadPermission() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Release newRelease = project().getReleaseRoot().create("New Release", user());
			testPermission(GraphPermission.READ_PERM, newRelease);
		}
	}

	@Test
	@Override
	public void testDeletePermission() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Release newRelease = project().getReleaseRoot().create("New Release", user());
			testPermission(GraphPermission.DELETE_PERM, newRelease);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Release newRelease = project().getReleaseRoot().create("New Release", user());
			testPermission(GraphPermission.UPDATE_PERM, newRelease);
		}
	}

	@Test
	@Override
	public void testCreatePermission() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Release newRelease = project().getReleaseRoot().create("New Release", user());
			testPermission(GraphPermission.CREATE_PERM, newRelease);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Release release = project().getInitialRelease();

			RoutingContext rc = getMockedRoutingContext(user());
			InternalActionContext ac = InternalActionContext.create(rc);

			ReleaseResponse releaseResponse = release.transformToRestSync(ac, 0).toBlocking().value();
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
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			List<SchemaContainerVersion> versions = project.getSchemaContainerRoot().findAll().stream().map(SchemaContainer::getLatestVersion)
					.collect(Collectors.toList());

			List<SchemaContainerVersion> found = new ArrayList<>();
			for (SchemaContainerVersion version : project.getInitialRelease().findAllSchemaVersions()) {
				found.add(version);
			}
			assertThat(found).as("List of schema versions").usingElementComparatorOnFields("uuid", "name", "version").containsAll(versions);
		}
	}

	/**
	 * Test assigning a schema to a project
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAssignSchema() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			SchemaContainer schemaContainer = createSchema("bla");
			updateSchema(schemaContainer, "newfield");
			SchemaContainerVersion latestVersion = schemaContainer.getLatestVersion();

			assertThat(latestVersion).as("latest version").isNotNull();
			SchemaContainerVersion previousVersion = latestVersion.getPreviousVersion();
			assertThat(previousVersion).as("Previous version").isNotNull();

			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("New Release", user());

			for (Release release : Arrays.asList(initialRelease, newRelease)) {
				assertThat(release).as(release.getName()).hasNotSchema(schemaContainer).hasNotSchemaVersion(latestVersion)
						.hasNotSchemaVersion(previousVersion);
			}

			// assign the schema to the project
			project.getSchemaContainerRoot().addSchemaContainer(schemaContainer);

			initialRelease.reload();
			newRelease.reload();

			for (Release release : Arrays.asList(initialRelease, newRelease)) {
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
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			List<? extends SchemaContainer> schemas = project.getSchemaContainerRoot().findAll();
			SchemaContainer schemaContainer = schemas.get(0);

			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("New Release", user());

			project.getSchemaContainerRoot().removeSchemaContainer(schemaContainer);
			initialRelease.reload();
			newRelease.reload();

			for (Release release : Arrays.asList(initialRelease, newRelease)) {
				assertThat(release).as(release.getName()).hasNotSchema(schemaContainer).hasNotSchemaVersion(schemaContainer.getLatestVersion());
			}
		}
	}

	@Test
	public void testReleaseSchemaVersion() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();

			SchemaContainer schemaContainer = createSchema("bla");
			SchemaContainerVersion firstVersion = schemaContainer.getLatestVersion();

			// assign the schema to the project
			project.getSchemaContainerRoot().addSchemaContainer(schemaContainer);

			// update schema
			updateSchema(schemaContainer, "newfield");
			SchemaContainerVersion secondVersion = schemaContainer.getLatestVersion();

			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("New Release", user());

			assertThat(initialRelease).as(initialRelease.getName()).hasSchema(schemaContainer).hasSchemaVersion(firstVersion)
					.hasNotSchemaVersion(secondVersion);
			assertThat(newRelease).as(newRelease.getName()).hasSchema(schemaContainer).hasNotSchemaVersion(firstVersion)
					.hasSchemaVersion(secondVersion);
		}
	}

	@Test
	public void testReadMicroschemaVersions() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			List<MicroschemaContainerVersion> versions = project.getMicroschemaContainerRoot().findAll().stream()
					.map(MicroschemaContainer::getLatestVersion).collect(Collectors.toList());

			List<MicroschemaContainerVersion> found = new ArrayList<>();
			for (MicroschemaContainerVersion version : project.getInitialRelease().findAllMicroschemaVersions()) {
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
		try (NoTrx noTx = db.noTrx()) {
			MicroschemaContainer microschemaContainer = createMicroschema("bla");
			updateMicroschema(microschemaContainer, "newfield");
			MicroschemaContainerVersion latestVersion = microschemaContainer.getLatestVersion();

			assertThat(latestVersion).as("latest version").isNotNull();
			MicroschemaContainerVersion previousVersion = latestVersion.getPreviousVersion();
			assertThat(previousVersion).as("Previous version").isNotNull();

			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("New Release", user());

			for (Release release : Arrays.asList(initialRelease, newRelease)) {
				assertThat(release).as(release.getName()).hasNotMicroschema(microschemaContainer).hasNotMicroschemaVersion(latestVersion)
						.hasNotMicroschemaVersion(previousVersion);
			}

			// assign the schema to the project
			project.getMicroschemaContainerRoot().addMicroschema(microschemaContainer);

			initialRelease.reload();
			newRelease.reload();

			for (Release release : Arrays.asList(initialRelease, newRelease)) {
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
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			List<? extends MicroschemaContainer> microschemas = project.getMicroschemaContainerRoot().findAll();
			MicroschemaContainer microschemaContainer = microschemas.get(0);

			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("New Release", user());

			project.getMicroschemaContainerRoot().removeMicroschema(microschemaContainer);
			initialRelease.reload();
			newRelease.reload();

			for (Release release : Arrays.asList(initialRelease, newRelease)) {
				assertThat(release).as(release.getName()).hasNotMicroschema(microschemaContainer)
						.hasNotMicroschemaVersion(microschemaContainer.getLatestVersion());
			}
		}
	}

	@Test
	public void testReleaseMicroschemaVersion() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();

			MicroschemaContainer microschemaContainer = createMicroschema("bla");
			MicroschemaContainerVersion firstVersion = microschemaContainer.getLatestVersion();

			// assign the microschema to the project
			project.getMicroschemaContainerRoot().addMicroschema(microschemaContainer);

			// update microschema
			updateMicroschema(microschemaContainer, "newfield");
			MicroschemaContainerVersion secondVersion = microschemaContainer.getLatestVersion();

			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("New Release", user());

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
	protected SchemaContainer createSchema(String name) throws Exception {
		Schema schema = new SchemaModel();
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

		Schema updatedSchema = new SchemaModel();
		updatedSchema.setName(schema.getName());
		updatedSchema.setDisplayField(schema.getDisplayField());
		updatedSchema.getFields().addAll(schema.getFields());
		updatedSchema.addField(FieldUtil.createStringFieldSchema(newName));

		SchemaChangesListModel model = new SchemaChangesListModel();
		model.getChanges().addAll(SchemaComparator.getIntance().diff(schema, updatedSchema));

		InternalActionContext ac = getMockedInternalActionContext();
		schemaContainer.getLatestVersion().applyChanges(ac, model).toBlocking().value();
		schemaContainer.reload();
	}

	/**
	 * Create a new microschema with a single string field "name"
	 * 
	 * @param name
	 *            microschema name
	 * @return microschema container
	 * @throws Exception
	 */
	protected MicroschemaContainer createMicroschema(String name) throws Exception {
		MicroschemaModel microschema = new MicroschemaModel();
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

		Microschema updatedMicroschema = new MicroschemaModel();
		updatedMicroschema.setName(microschema.getName());
		updatedMicroschema.getFields().addAll(microschema.getFields());
		updatedMicroschema.addField(FieldUtil.createStringFieldSchema(newName));

		SchemaChangesListModel model = new SchemaChangesListModel();
		model.getChanges().addAll(MicroschemaComparator.getIntance().diff(microschema, updatedMicroschema));

		InternalActionContext ac = getMockedInternalActionContext();
		microschemaContainer.getLatestVersion().applyChanges(ac, model).toBlocking().value();
		microschemaContainer.reload();
	}
}
