package com.gentics.mesh.core.release;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.release.ReleaseUpdateRequest;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.MicroschemaReferenceList;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReferenceList;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.mock.Mocks;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.parameter.impl.SchemaUpdateParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractBasicCrudEndpointTest;

public class ReleaseEndpointTest extends AbstractBasicCrudEndpointTest {

	@Override
	public void testUpdateMultithreaded() throws Exception {
		// TODO Auto-generated method stub
	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 200;
		try (NoTx noTx = db.noTx()) {
			String projectName = project().getName();
			String uuid = project().getInitialRelease().getUuid();

			Set<MeshResponse<?>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findReleaseByUuid(projectName, uuid).invoke());
			}

			for (MeshResponse<?> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

	@Override
	public void testDeleteByUUIDMultithreaded() throws Exception {

	}

	@Test
	@Override
	public void testCreateMultithreaded() throws Exception {
		String releaseName = "Release V";
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			int nJobs = 100;

			Set<MeshResponse<ReleaseResponse>> responseFutures = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				ReleaseCreateRequest request = new ReleaseCreateRequest();
				request.setName(releaseName + i);
				MeshResponse<ReleaseResponse> future = client().createRelease(project.getName(), request).invoke();
				responseFutures.add(future);
			}

			Set<String> uuids = new HashSet<>();
			uuids.add(project.getInitialRelease().getUuid());
			for (MeshResponse<ReleaseResponse> future : responseFutures) {
				latchFor(future);
				assertSuccess(future);

				assertThat(future.result()).as("Response").isNotNull();
				assertThat(uuids).as("Existing uuids").doesNotContain(future.result().getUuid());
				uuids.add(future.result().getUuid());
			}

			// all releases must form a chain
			Set<String> foundReleases = new HashSet<>();
			project.reload();
			project.getReleaseRoot().reload();
			Release previousRelease = null;
			Release release = project.getInitialRelease();

			do {
				assertThat(release).as("Release").isNotNull().hasPrevious(previousRelease);
				assertThat(foundReleases).as("Existing uuids").doesNotContain(release.getUuid());
				foundReleases.add(release.getUuid());
				previousRelease = release;
				release = release.getNextRelease();
			} while (release != null);

			assertThat(previousRelease).as("Latest Release").matches(project.getLatestRelease());
			assertThat(foundReleases).as("Found Releases").containsOnlyElementsOf(uuids);
		}
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;
		try (NoTx noTx = db.noTx()) {
			Set<MeshResponse<ReleaseResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findReleaseByUuid(project().getName(), project().getInitialRelease().getUuid()).invoke());
			}
			for (MeshResponse<ReleaseResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		String releaseName = "Release V1";
		try (NoTx noTx = db.noTx()) {
			Project project = project();

			ReleaseCreateRequest request = new ReleaseCreateRequest();
			request.setName(releaseName);

			ReleaseResponse response = call(() -> client().createRelease(project.getName(), request));
			assertThat(response).as("Release Response").isNotNull().hasName(releaseName).isActive().isNotMigrated();
		}
	}

	@Override
	public void testCreateWithNoPerm() throws Exception {
		String releaseName = "Release V1";
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			String uuid = project.getUuid();
			String name = project.getName();
			role().grantPermissions(project, READ_PERM);
			role().revokePermissions(project, UPDATE_PERM);

			ReleaseCreateRequest request = new ReleaseCreateRequest();
			request.setName(releaseName);

			call(() -> client().createRelease(project.getName(), request), FORBIDDEN, "error_missing_perm", uuid + "/" + name);
		}
	}

	@Test
	public void testCreateWithoutName() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			call(() -> client().createRelease(project.getName(), new ReleaseCreateRequest()), BAD_REQUEST, "release_missing_name");
		}
	}

	@Test
	public void testCreateWithConflictingName1() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			ReleaseCreateRequest request = new ReleaseCreateRequest();
			request.setName(project.getName());

			call(() -> client().createRelease(project.getName(), request), CONFLICT, "release_conflicting_name", project.getName());
		}
	}

	@Test
	public void testCreateWithConflictingName2() throws Exception {
		try (NoTx noTx = db.noTx()) {
			String releaseName = "New Release";
			Project project = project();
			ReleaseCreateRequest request = new ReleaseCreateRequest();
			request.setName(releaseName);

			call(() -> client().createRelease(project.getName(), request));

			call(() -> client().createRelease(project.getName(), request), CONFLICT, "release_conflicting_name", releaseName);
		}
	}

	@Test
	public void testCreateWithConflictingName3() throws Exception {
		String releaseName = "New Release";
		String newProjectName = "otherproject";
		String projectName = db.noTx(() -> project().getName());
		ReleaseCreateRequest request = new ReleaseCreateRequest();
		request.setName(releaseName);

		call(() -> client().createRelease(projectName, request));

		ProjectCreateRequest createProject = new ProjectCreateRequest();
		createProject.setName(newProjectName);
		createProject.setSchema(new SchemaReference().setName("folder"));
		call(() -> client().createProject(createProject));

		call(() -> client().createRelease(newProjectName, request));

	}

	@Override
	public void testCreateReadDelete() throws Exception {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release firstRelease = project.getReleaseRoot().create("One", user());
			Release secondRelease = project.getReleaseRoot().create("Two", user());
			Release thirdRelease = project.getReleaseRoot().create("Three", user());

			for (Release release : Arrays.asList(initialRelease, firstRelease, secondRelease, thirdRelease)) {
				ReleaseResponse response = call(() -> client().findReleaseByUuid(project.getName(), release.getUuid()));
				assertThat(response).isNotNull().hasName(release.getName()).hasUuid(release.getUuid()).isActive();
			}
		}
	}

	@Test
	public void testReadByBogusUUID() throws Exception {
		String projectName = db.noTx(() -> project().getName());
		call(() -> client().findReleaseByUuid(projectName, "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		String projectName = db.noTx(() -> project().getName());
		String uuid = db.noTx(() -> project().getInitialRelease().getUuid());
		String roleUuid = db.noTx(() -> role().getUuid());

		ReleaseResponse response = call(() -> client().findReleaseByUuid(projectName, uuid, new RolePermissionParameters().setRoleUuid(roleUuid)));
		assertThat(response.getRolePerms()).hasPerm(READ, CREATE, UPDATE, DELETE);
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			String releaseUuid = project.getInitialRelease().getUuid();
			String name = project.getName();
			role().revokePermissions(project.getInitialRelease(), READ_PERM);

			call(() -> client().findReleaseByUuid(name, releaseUuid), FORBIDDEN, "error_missing_perm", releaseUuid);
		}
	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release firstRelease = project.getReleaseRoot().create("One", user());
			Release secondRelease = project.getReleaseRoot().create("Two", user());
			Release thirdRelease = project.getReleaseRoot().create("Three", user());

			ListResponse<ReleaseResponse> responseList = call(() -> client().findReleases(project.getName()));

			InternalActionContext ac = Mocks.getMockedInternalActionContext(user());

			assertThat(responseList).isNotNull();
			assertThat(responseList.getData()).usingElementComparatorOnFields("uuid", "name").containsOnly(initialRelease.transformToRestSync(ac, 0),
					firstRelease.transformToRestSync(ac, 0), secondRelease.transformToRestSync(ac, 0), thirdRelease.transformToRestSync(ac, 0));
		}
	}

	@Test
	public void testReadMultipleWithRestrictedPermissions() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release firstRelease = project.getReleaseRoot().create("One", user());
			Release secondRelease = project.getReleaseRoot().create("Two", user());
			Release thirdRelease = project.getReleaseRoot().create("Three", user());

			role().revokePermissions(firstRelease, READ_PERM);
			role().revokePermissions(thirdRelease, READ_PERM);

			ListResponse<ReleaseResponse> responseList = call(() -> client().findReleases(project.getName()));

			InternalActionContext ac = Mocks.getMockedInternalActionContext(user());
			assertThat(responseList).isNotNull();
			assertThat(responseList.getData()).usingElementComparatorOnFields("uuid", "name").containsOnly(initialRelease.transformToRestSync(ac, 0),
					secondRelease.transformToRestSync(ac, 0));
		}
	}

	@Test
	@Override
	public void testUpdate() throws Exception {
		String newName = "New Release Name";
		String anotherNewName = "Another New Release Name";
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			String projectName = project.getName();
			String uuid = project.getInitialRelease().getUuid();

			// change name
			ReleaseUpdateRequest request1 = new ReleaseUpdateRequest();
			request1.setName(newName);
			ReleaseResponse response = call(() -> client().updateRelease(projectName, uuid, request1));
			assertThat(response).as("Updated Release").isNotNull().hasName(newName).isActive();

			// change active
			ReleaseUpdateRequest request2 = new ReleaseUpdateRequest();
			// request2.setActive(false);
			response = call(() -> client().updateRelease(projectName, uuid, request2));
			assertThat(response).as("Updated Release").isNotNull().hasName(newName).isInactive();

			// change active and name
			ReleaseUpdateRequest request3 = new ReleaseUpdateRequest();
			// request3.setActive(true);
			request3.setName(anotherNewName);
			response = call(() -> client().updateRelease(projectName, uuid, request3));
			assertThat(response).as("Updated Release").isNotNull().hasName(anotherNewName).isActive();
		}
	}

	@Test
	public void testUpdateWithNameConflict() throws Exception {
		String newName = "New Release Name";
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			project.getReleaseRoot().create(newName, user());

			ReleaseUpdateRequest request = new ReleaseUpdateRequest();
			request.setName(newName);
			call(() -> client().updateRelease(project.getName(), project.getInitialRelease().getUuid(), request), CONFLICT,
					"release_conflicting_name", newName);
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			String projectName = project.getName();
			role().revokePermissions(project.getInitialRelease(), UPDATE_PERM);

			ReleaseUpdateRequest request = new ReleaseUpdateRequest();
			// request.setActive(false);
			call(() -> client().updateRelease(projectName, project.getInitialRelease().getUuid(), request), FORBIDDEN, "error_missing_perm",
					project.getInitialRelease().getUuid());
		}
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();

			ReleaseUpdateRequest request = new ReleaseUpdateRequest();
			// request.setActive(false);
			call(() -> client().updateRelease(project.getName(), "bogus", request), NOT_FOUND, "object_not_found_for_uuid", "bogus");
		}
	}

	@Override
	public void testDeleteByUUID() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		// TODO Auto-generated method stub

	}

	// Tests for assignment of schema versions to releases

	@Test
	public void testReadSchemaVersions() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			SchemaReferenceList list = call(() -> client().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));

			SchemaReference content = schemaContainer("content").getLatestVersion().transformToReference();
			SchemaReference folder = schemaContainer("folder").getLatestVersion().transformToReference();
			SchemaReference binaryContent = schemaContainer("binary-content").getLatestVersion().transformToReference();

			assertThat(list).as("release schema versions").usingElementComparatorOnFields("name", "uuid", "version").containsOnly(content, folder,
					binaryContent);
		}
	}

	@Test
	public void testAssignSchemaVersionViaSchemaUpdate() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// create version 1 of a schema
			Schema schema = createSchema("schemaname");
			Project project = project();

			// Assign schema to project
			call(() -> client().assignSchemaToProject(project.getName(), schema.getUuid()));

			// Generate version 2
			updateSchema(schema.getUuid(), "newschemaname");

			// Assert that version 2 is assigned to release
			SchemaReferenceList list = call(() -> client().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new SchemaReference().setName("newschemaname").setUuid(schema.getUuid()).setVersion(2));

			// Generate version 3
			updateSchema(schema.getUuid(), "anothernewschemaname", new SchemaUpdateParameters().setUpdateAssignedReleases(false));

			// Assert that version 2 is still assigned to release
			list = call(() -> client().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new SchemaReference().setName("newschemaname").setUuid(schema.getUuid()).setVersion(2));

			// Generate version 3 which should not be auto assigned to the project release
			updateSchema(schema.getUuid(), "anothernewschemaname",
					new SchemaUpdateParameters().setUpdateAssignedReleases(true).setReleaseNames(project.getInitialRelease().getName()));

			// Assert that version 2 is still assigned to the release
			list = call(() -> client().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new SchemaReference().setName("newschemaname").setUuid(schema.getUuid()).setVersion(2));

			// Generate version 4
			updateSchema(schema.getUuid(), "anothernewschemaname2", new SchemaUpdateParameters().setUpdateAssignedReleases(true));

			// Assert that version 4 is assigned to the release
			list = call(() -> client().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new SchemaReference().setName("anothernewschemaname2").setUuid(schema.getUuid()).setVersion(4));

			// Generate version 5
			updateSchema(schema.getUuid(), "anothernewschemaname3",
					new SchemaUpdateParameters().setUpdateAssignedReleases(true).setReleaseNames("bla", "bogus", "moped"));

			// Assert that version 4 is still assigned to the release since non of the names matches the project release
			list = call(() -> client().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new SchemaReference().setName("anothernewschemaname2").setUuid(schema.getUuid()).setVersion(4));

		}
	}

	@Test
	public void testAssignSchemaVersion() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// create version 1 of a schema
			Schema schema = createSchema("schemaname");
			Project project = project();

			// assign schema to project
			call(() -> client().assignSchemaToProject(project.getName(), schema.getUuid()));

			// generate version 2
			updateSchema(schema.getUuid(), "newschemaname", new SchemaUpdateParameters().setUpdateAssignedReleases(false));

			// generate version 3
			updateSchema(schema.getUuid(), "anothernewschemaname", new SchemaUpdateParameters().setUpdateAssignedReleases(false));

			// check that version 1 is assigned to release
			SchemaReferenceList list = call(() -> client().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new SchemaReference().setName("schemaname").setUuid(schema.getUuid()).setVersion(1));

			// assign version 2 to the release
			call(() -> client().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReferenceList(Arrays.asList(new SchemaReference().setUuid(schema.getUuid()).setVersion(2)))));

			// assert
			list = call(() -> client().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new SchemaReference().setName("newschemaname").setUuid(schema.getUuid()).setVersion(2));
		}
	}

	@Test
	public void testAssignBogusSchemaVersion() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();

			call(() -> client().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReference().setName("content").setVersion(4711)), BAD_REQUEST, "error_schema_reference_not_found", "content", "-",
					"4711");
		}
	}

	@Test
	public void testAssignBogusSchemaUuid() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();

			call(() -> client().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReference().setUuid("bogusuuid").setVersion(1)), BAD_REQUEST, "error_schema_reference_not_found", "-", "bogusuuid",
					"1");
		}
	}

	@Test
	public void testAssignBogusSchemaName() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();

			call(() -> client().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReference().setName("bogusname").setVersion(1)), BAD_REQUEST, "error_schema_reference_not_found", "bogusname", "-",
					"1");
		}
	}

	@Test
	public void testAssignUnassignedSchemaVersion() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Schema schema = createSchema("schemaname");
			Project project = project();

			call(() -> client().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReference().setName(schema.getName()).setVersion(schema.getVersion())), BAD_REQUEST, "error_schema_reference_not_found",
					schema.getName(), "-", Integer.toString(schema.getVersion()));
		}
	}

	@Test
	public void testAssignOlderSchemaVersion() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// create version 1 of a schema
			Schema schema = createSchema("schemaname");
			Project project = project();

			// generate version 2
			updateSchema(schema.getUuid(), "newschemaname");

			// assign schema to project
			call(() -> client().assignSchemaToProject(project.getName(), schema.getUuid()));

			// try to downgrade schema version
			call(() -> client().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReference().setUuid(schema.getUuid()).setVersion(1)), BAD_REQUEST, "release_error_downgrade_schema_version",
					"schemaname", "2", "1");
		}
	}

	@Test
	public void testAssignSchemaVersionNoPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			role().revokePermissions(project.getInitialRelease(), UPDATE_PERM);

			call(() -> client().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReference().setName("content").setVersion(1)), FORBIDDEN, "error_missing_perm", project.getInitialRelease().getUuid());
		}
	}

	@Test
	public void testAssignLatestSchemaVersion() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// create version 1 of a schema
			Schema schema = createSchema("schemaname");
			Project project = project();

			// assign schema to project
			call(() -> client().assignSchemaToProject(project.getName(), schema.getUuid()));

			// generate version 2
			updateSchema(schema.getUuid(), "newschemaname", new SchemaUpdateParameters().setUpdateAssignedReleases(false));

			// generate version 3
			updateSchema(schema.getUuid(), "anothernewschemaname", new SchemaUpdateParameters().setUpdateAssignedReleases(false));

			// check that version 1 is assigned to release
			SchemaReferenceList list = call(() -> client().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new SchemaReference().setName("schemaname").setUuid(schema.getUuid()).setVersion(1));

			// assign latest version to the release
			call(() -> client().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReferenceList(Arrays.asList(new SchemaReference().setUuid(schema.getUuid())))));

			// assert
			list = call(() -> client().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new SchemaReference().setName("anothernewschemaname").setUuid(schema.getUuid()).setVersion(3));
		}
	}

	// Tests for assignment of microschema versions to releases

	@Test
	public void testReadMicroschemaVersions() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			MicroschemaReferenceList list = call(
					() -> client().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));

			MicroschemaReference vcard = microschemaContainer("vcard").getLatestVersion().transformToReference();
			MicroschemaReference captionedImage = microschemaContainer("captionedImage").getLatestVersion().transformToReference();

			assertThat(list).as("release microschema versions").usingElementComparatorOnFields("name", "uuid", "version").containsOnly(vcard,
					captionedImage);
		}
	}

	@Test
	public void testAssignMicroschemaVersion() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// create version 1 of a microschema
			Microschema microschema = createMicroschema("microschemaname");
			Project project = project();

			// assign microschema to project
			call(() -> client().assignMicroschemaToProject(project.getName(), microschema.getUuid()));

			// generate version 2
			updateMicroschema(microschema.getUuid(), "newmicroschemaname", new SchemaUpdateParameters().setUpdateAssignedReleases(false));

			// generate version 3
			updateMicroschema(microschema.getUuid(), "anothernewmicroschemaname", new SchemaUpdateParameters().setUpdateAssignedReleases(false));

			// check that version 1 is assigned to release
			MicroschemaReferenceList list = call(
					() -> client().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new MicroschemaReference().setName("microschemaname").setUuid(microschema.getUuid()).setVersion(1));

			// assign version 2 to the release
			call(() -> client().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReferenceList(Arrays.asList(new MicroschemaReference().setUuid(microschema.getUuid()).setVersion(2)))));

			// assert
			list = call(() -> client().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new MicroschemaReference().setName("newmicroschemaname").setUuid(microschema.getUuid()).setVersion(2));
		}
	}

	@Test
	public void testAssignMicroschemaVersionViaMicroschemaUpdate() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// create version 1 of a microschema
			Microschema microschema = createMicroschema("microschemaname");
			Project project = project();

			// assign microschema to project
			call(() -> client().assignMicroschemaToProject(project.getName(), microschema.getUuid()));

			// generate version 2
			updateMicroschema(microschema.getUuid(), "newmicroschemaname", new SchemaUpdateParameters().setUpdateAssignedReleases(true));

			// generate version 3
			updateMicroschema(microschema.getUuid(), "anothernewmicroschemaname");

			// check that version 3 is assigned to release
			MicroschemaReferenceList list = call(
					() -> client().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new MicroschemaReference().setName("anothernewmicroschemaname").setUuid(microschema.getUuid()).setVersion(3));

			// assign version 2 to the release
			//			call(() -> getClient().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
			//					new MicroschemaReferenceList(Arrays.asList(new MicroschemaReference().setUuid(microschema.getUuid()).setVersion(2)))));

			// assert
			//			list = call(() -> getClient().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			//			assertThat(list).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version")
			//					.contains(new MicroschemaReference().setName("newmicroschemaname").setUuid(microschema.getUuid()).setVersion(2));
		}
	}

	@Test
	public void testAssignBogusMicroschemaVersion() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();

			call(() -> client().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReference().setName("vcard").setVersion(4711)), BAD_REQUEST, "error_microschema_reference_not_found", "vcard", "-",
					"4711");
		}
	}

	@Test
	public void testAssignBogusMicroschemaUuid() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();

			call(() -> client().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReference().setUuid("bogusuuid").setVersion(1)), BAD_REQUEST, "error_microschema_reference_not_found", "-",
					"bogusuuid", "1");
		}
	}

	@Test
	public void testAssignBogusMicroschemaName() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();

			call(() -> client().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReference().setName("bogusname").setVersion(1)), BAD_REQUEST, "error_microschema_reference_not_found", "bogusname",
					"-", "1");
		}
	}

	@Test
	public void testAssignUnassignedMicroschemaVersion() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Schema schema = createSchema("microschemaname");
			Project project = project();

			call(() -> client().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReference().setName(schema.getName()).setVersion(schema.getVersion())), BAD_REQUEST,
					"error_microschema_reference_not_found", schema.getName(), "-", Integer.toString(schema.getVersion()));
		}
	}

	@Test
	public void testAssignOlderMicroschemaVersion() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// create version 1 of a microschema
			Microschema microschema = createMicroschema("microschemaname");
			Project project = project();

			// generate version 2
			updateMicroschema(microschema.getUuid(), "newmicroschemaname");

			// assign microschema to project
			call(() -> client().assignMicroschemaToProject(project.getName(), microschema.getUuid()));

			// try to downgrade microschema version
			call(() -> client().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReference().setUuid(microschema.getUuid()).setVersion(1)), BAD_REQUEST,
					"release_error_downgrade_microschema_version", "microschemaname", "2", "1");
		}
	}

	@Test
	public void testAssignMicroschemaVersionNoPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			role().revokePermissions(project.getInitialRelease(), UPDATE_PERM);

			call(() -> client().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReference().setName("vcard").setVersion(1)), FORBIDDEN, "error_missing_perm",
					project.getInitialRelease().getUuid());
		}
	}

	@Test
	public void testAssignLatestMicroschemaVersion() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// create version 1 of a microschema
			Microschema microschema = createMicroschema("microschemaname");
			Project project = project();

			// Assign microschema to project
			call(() -> client().assignMicroschemaToProject(project.getName(), microschema.getUuid()));

			// Generate version 2
			updateMicroschema(microschema.getUuid(), "newmicroschemaname");

			// Assert that version 2 is assigned to release
			MicroschemaReferenceList list = call(
					() -> client().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new MicroschemaReference().setName("newmicroschemaname").setUuid(microschema.getUuid()).setVersion(2));

			// Generate version 3 which should not be auto assigned to the project release
			updateMicroschema(microschema.getUuid(), "anothernewschemaname", new SchemaUpdateParameters().setUpdateAssignedReleases(false));

			// Assert that version 2 is still assigned to release
			list = call(() -> client().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new MicroschemaReference().setName("newmicroschemaname").setUuid(microschema.getUuid()).setVersion(2));

			// Generate version 4
			updateMicroschema(microschema.getUuid(), "anothernewschemaname1",
					new SchemaUpdateParameters().setUpdateAssignedReleases(true).setReleaseNames(project.getInitialRelease().getName()));

			// Assert that version 4 is assigned to the release
			list = call(() -> client().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new MicroschemaReference().setName("anothernewschemaname1").setUuid(microschema.getUuid()).setVersion(4));

			// Generate version 5
			updateMicroschema(microschema.getUuid(), "anothernewschemaname2", new SchemaUpdateParameters().setUpdateAssignedReleases(true));

			// Assert that version 5
			list = call(() -> client().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new MicroschemaReference().setName("anothernewschemaname2").setUuid(microschema.getUuid()).setVersion(5));

			// Generate version 6
			updateMicroschema(microschema.getUuid(), "anothernewschemaname3",
					new SchemaUpdateParameters().setUpdateAssignedReleases(true).setReleaseNames("bla", "bogus", "moped"));

			// Assert that version 4 is still assigned to the release since non of the names matches the project release
			list = call(() -> client().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new MicroschemaReference().setName("anothernewschemaname2").setUuid(microschema.getUuid()).setVersion(5));
		}
	}
}
