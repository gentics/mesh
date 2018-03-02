package com.gentics.mesh.core.release;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.COMPLETED;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.INITIAL_RELEASE_NAME;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.test.util.MeshAssert.failingLatch;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.release.ReleaseUpdateRequest;
import com.gentics.mesh.core.rest.release.info.ReleaseInfoMicroschemaList;
import com.gentics.mesh.core.rest.release.info.ReleaseInfoSchemaList;
import com.gentics.mesh.core.rest.release.info.ReleaseMicroschemaInfo;
import com.gentics.mesh.core.rest.release.info.ReleaseSchemaInfo;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.SchemaUpdateParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicRestTestcases;
import com.gentics.mesh.test.util.TestUtils;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class ReleaseEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@Before
	public void addAdminPerms() {
		// Grant admin perms. Otherwise we can't check the jobs
		tx(() -> group().addRole(roles().get("admin")));
	}

	@Override
	public void testUpdateMultithreaded() throws Exception {
		// TODO Auto-generated method stub
	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 200;
		try (Tx tx = tx()) {
			String projectName = PROJECT_NAME;
			String uuid = initialReleaseUuid();

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
		// Releases can't be deleted
	}

	@Test
	@Override
	@Ignore
	public void testCreateMultithreaded() throws Exception {
		String releaseName = "Release V";
		try (Tx tx = tx()) {
			Project project = project();
			int nJobs = 100;

			Set<MeshResponse<ReleaseResponse>> responseFutures = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				ReleaseCreateRequest request = new ReleaseCreateRequest();
				request.setName(releaseName + i);
				MeshResponse<ReleaseResponse> future = client().createRelease(PROJECT_NAME, request).invoke();
				responseFutures.add(future);
			}

			Set<String> uuids = new HashSet<>();
			uuids.add(initialReleaseUuid());
			for (MeshResponse<ReleaseResponse> future : responseFutures) {
				latchFor(future);
				assertSuccess(future);

				assertThat(future.result()).as("Response").isNotNull();
				assertThat(uuids).as("Existing uuids").doesNotContain(future.result().getUuid());
				uuids.add(future.result().getUuid());
			}

			// all releases must form a chain
			Set<String> foundReleases = new HashSet<>();
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
		try (Tx tx = tx()) {
			Set<MeshResponse<ReleaseResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findReleaseByUuid(PROJECT_NAME, initialReleaseUuid()).invoke());
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

		ReleaseCreateRequest request = new ReleaseCreateRequest();
		request.setName(releaseName);

		waitForJobs(() -> {
			ReleaseResponse response = call(() -> client().createRelease(PROJECT_NAME, request));
			assertThat(response).as("Release Response").isNotNull().hasName(releaseName).isActive().isNotMigrated();
		}, COMPLETED, 1);
	}

	@Override
	public void testCreateWithNoPerm() throws Exception {
		String releaseName = "Release V1";
		try (Tx tx = tx()) {
			Project project = project();
			role().grantPermissions(project, READ_PERM);
			role().revokePermissions(project, UPDATE_PERM);
			tx.success();
		}
		ReleaseCreateRequest request = new ReleaseCreateRequest();
		request.setName(releaseName);
		call(() -> client().createRelease(PROJECT_NAME, request), FORBIDDEN, "error_missing_perm", projectUuid() + "/" + PROJECT_NAME);
	}

	@Test
	@Override
	public void testCreateWithUuid() throws Exception {
		String releaseName = "Release V1";
		String uuid = UUIDUtil.randomUUID();
		try (Tx tx = db().tx()) {
			Project project = project();

			ReleaseUpdateRequest request = new ReleaseUpdateRequest();
			request.setName(releaseName);

			ReleaseResponse response = call(() -> client().updateRelease(project.getName(), uuid, request));
			assertThat(response).as("Release Response").isNotNull().hasName(releaseName).isActive().isNotMigrated().hasUuid(uuid);
		}
	}

	@Test
	@Override
	public void testCreateWithDuplicateUuid() throws Exception {
		String releaseName = "Release V1";
		try (Tx tx = db().tx()) {
			Project project = project();
			String uuid = user().getUuid();

			ReleaseUpdateRequest request = new ReleaseUpdateRequest();
			request.setName(releaseName);

			call(() -> client().updateRelease(project.getName(), uuid, request), INTERNAL_SERVER_ERROR, "error_internal");
		}
	}

	@Test
	public void testCreateWithoutName() throws Exception {
		call(() -> client().createRelease(PROJECT_NAME, new ReleaseCreateRequest()), BAD_REQUEST, "release_missing_name");
	}

	@Test
	public void testCreateWithConflictingName1() throws Exception {
		ReleaseCreateRequest request = new ReleaseCreateRequest();
		request.setName(PROJECT_NAME);
		call(() -> client().createRelease(PROJECT_NAME, request), CONFLICT, "release_conflicting_name", PROJECT_NAME);
	}

	@Test
	public void testCreateWithConflictingName2() throws Exception {
		ReleaseCreateRequest request = new ReleaseCreateRequest();
		String releaseName = "New Release";
		request.setName(releaseName);
		CountDownLatch latch = TestUtils.latchForMigrationCompleted(client());
		waitForJobs(() -> {
			call(() -> client().createRelease(PROJECT_NAME, request));
		}, COMPLETED, 1);
		failingLatch(latch);
		call(() -> client().createRelease(PROJECT_NAME, request), CONFLICT, "release_conflicting_name", releaseName);
	}

	@Test
	public void testCreateWithConflictingName3() throws Exception {
		String releaseName = "New Release";
		String newProjectName = "otherproject";

		// 1. Create a new release
		CountDownLatch latch = TestUtils.latchForMigrationCompleted(client());
		ReleaseCreateRequest request = new ReleaseCreateRequest();
		request.setName(releaseName);
		waitForJobs(() -> {
			call(() -> client().createRelease(PROJECT_NAME, request));
		}, COMPLETED, 1);
		failingLatch(latch);

		// 2. Create a new project and release and use the same name. This is allowed and should not raise any error.
		latch = TestUtils.latchForMigrationCompleted(client());
		ProjectCreateRequest createProject = new ProjectCreateRequest();
		createProject.setName(newProjectName);
		createProject.setSchema(new SchemaReferenceImpl().setName("folder"));
		call(() -> client().createProject(createProject));
		waitForJobs(() -> {
			call(() -> client().createRelease(newProjectName, request));
		}, COMPLETED, 1);
		failingLatch(latch);
	}

	@Override
	public void testCreateReadDelete() throws Exception {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {

		List<Pair<String, String>> releaseInfo = new ArrayList<>();

		try (Tx tx = tx()) {
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			releaseInfo.add(Pair.of(initialRelease.getUuid(), initialRelease.getName()));

			Release firstRelease = project.getReleaseRoot().create("One", user());
			releaseInfo.add(Pair.of(firstRelease.getUuid(), firstRelease.getName()));

			Release secondRelease = project.getReleaseRoot().create("Two", user());
			releaseInfo.add(Pair.of(secondRelease.getUuid(), secondRelease.getName()));

			Release thirdRelease = project.getReleaseRoot().create("Three", user());
			releaseInfo.add(Pair.of(thirdRelease.getUuid(), thirdRelease.getName()));

			tx.success();
		}

		for (Pair<String, String> info : releaseInfo) {
			ReleaseResponse response = call(() -> client().findReleaseByUuid(PROJECT_NAME, info.getKey()));
			assertThat(response).isNotNull().hasName(info.getValue()).hasUuid(info.getKey()).isActive();
		}

	}

	@Test
	public void testReadByBogusUUID() throws Exception {
		call(() -> client().findReleaseByUuid(PROJECT_NAME, "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		String roleUuid = db().tx(() -> role().getUuid());
		ReleaseResponse response = call(() -> client().findReleaseByUuid(PROJECT_NAME, initialReleaseUuid(), new RolePermissionParametersImpl()
			.setRoleUuid(roleUuid)));
		assertThat(response.getRolePerms()).hasPerm(READ, CREATE, UPDATE, DELETE);
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (Tx tx = tx()) {
			role().revokePermissions(project().getInitialRelease(), READ_PERM);
			tx.success();
		}
		call(() -> client().findReleaseByUuid(PROJECT_NAME, initialReleaseUuid()), FORBIDDEN, "error_missing_perm", initialReleaseUuid());
	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		Release initialRelease;
		Release firstRelease;
		Release thirdRelease;
		Release secondRelease;

		try (Tx tx = tx()) {
			Project project = project();
			initialRelease = project.getInitialRelease();
			firstRelease = project.getReleaseRoot().create("One", user());
			secondRelease = project.getReleaseRoot().create("Two", user());
			thirdRelease = project.getReleaseRoot().create("Three", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			ListResponse<ReleaseResponse> responseList = call(() -> client().findReleases(PROJECT_NAME));
			InternalActionContext ac = mockActionContext();

			assertThat(responseList).isNotNull();
			assertThat(responseList.getData()).usingElementComparatorOnFields("uuid", "name").containsOnly(initialRelease.transformToRestSync(ac, 0),
				firstRelease.transformToRestSync(ac, 0), secondRelease.transformToRestSync(ac, 0), thirdRelease.transformToRestSync(ac, 0));
		}
	}

	@Test
	public void testReadMultipleWithRestrictedPermissions() throws Exception {
		Project project = project();
		Release initialRelease = tx(() -> initialRelease());

		Release firstRelease;
		Release secondRelease;
		Release thirdRelease;

		try (Tx tx = tx()) {
			firstRelease = project.getReleaseRoot().create("One", user());
			secondRelease = project.getReleaseRoot().create("Two", user());
			thirdRelease = project.getReleaseRoot().create("Three", user());
			tx.success();
		}
		try (Tx tx = tx()) {
			role().revokePermissions(firstRelease, READ_PERM);
			role().revokePermissions(thirdRelease, READ_PERM);
			tx.success();
		}

		ListResponse<ReleaseResponse> responseList = call(() -> client().findReleases(PROJECT_NAME));

		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
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
		try (Tx tx = tx()) {

			// change name
			ReleaseUpdateRequest request1 = new ReleaseUpdateRequest();
			request1.setName(newName);
			ReleaseResponse response = call(() -> client().updateRelease(PROJECT_NAME, initialReleaseUuid(), request1));
			assertThat(response).as("Updated Release").isNotNull().hasName(newName).isActive();

			// change active
			ReleaseUpdateRequest request2 = new ReleaseUpdateRequest();
			// request2.setActive(false);
			response = call(() -> client().updateRelease(PROJECT_NAME, initialReleaseUuid(), request2));
			assertThat(response).as("Updated Release").isNotNull().hasName(newName).isInactive();

			// change active and name
			ReleaseUpdateRequest request3 = new ReleaseUpdateRequest();
			// request3.setActive(true);
			request3.setName(anotherNewName);
			response = call(() -> client().updateRelease(PROJECT_NAME, initialReleaseUuid(), request3));
			assertThat(response).as("Updated Release").isNotNull().hasName(anotherNewName).isActive();
		}
	}

	@Test
	public void testUpdateWithNameConflict() throws Exception {
		String newName = "New Release Name";
		try (Tx tx = tx()) {
			project().getReleaseRoot().create(newName, user());
			tx.success();
		}
		ReleaseUpdateRequest request = new ReleaseUpdateRequest();
		request.setName(newName);
		call(() -> client().updateRelease(PROJECT_NAME, initialReleaseUuid(), request), CONFLICT, "release_conflicting_name", newName);

	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		try (Tx tx = tx()) {
			role().revokePermissions(project().getInitialRelease(), UPDATE_PERM);
			tx.success();
		}
		ReleaseUpdateRequest request = new ReleaseUpdateRequest();
		// request.setActive(false);
		call(() -> client().updateRelease(PROJECT_NAME, initialReleaseUuid(), request), FORBIDDEN, "error_missing_perm", initialReleaseUuid());
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		ReleaseUpdateRequest request = new ReleaseUpdateRequest();
		// request.setActive(false);
		call(() -> client().createRelease(PROJECT_NAME, "bogus", request), BAD_REQUEST, "error_illegal_uuid", "bogus");
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
		try (Tx tx = tx()) {
			ReleaseInfoSchemaList list = call(() -> client().getReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid()));
			System.out.println(list.toJson());
			ReleaseSchemaInfo content = new ReleaseSchemaInfo(schemaContainer("content").getLatestVersion().transformToReference());
			ReleaseSchemaInfo folder = new ReleaseSchemaInfo(schemaContainer("folder").getLatestVersion().transformToReference());
			ReleaseSchemaInfo binaryContent = new ReleaseSchemaInfo(schemaContainer("binary_content").getLatestVersion().transformToReference());

			assertThat(list.getSchemas()).as("release schema versions").usingElementComparatorOnFields("name", "uuid", "version").containsOnly(
				content, folder, binaryContent);
		}
	}

	@Test
	public void testAssignSchemaVersionViaSchemaUpdate() throws Exception {
		try (Tx tx = tx()) {
			// create version 1 of a schema
			SchemaResponse schema = createSchema("schemaname");

			// Assign schema to project
			call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

			// Generate version 2
			waitForJobs(() -> {
				updateSchema(schema.getUuid(), "newschemaname");
			}, COMPLETED, 1);

			// Assert that version 2 is assigned to release
			ReleaseInfoSchemaList infoList = call(() -> client().getReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid()));
			assertThat(infoList.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new ReleaseSchemaInfo().setName("newschemaname").setUuid(schema.getUuid()).setVersion("2.0"));

			// Generate version 3
			updateSchema(schema.getUuid(), "anothernewschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(false));

			// Assert that version 2 is still assigned to release
			infoList = call(() -> client().getReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid()));
			assertThat(infoList.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new ReleaseSchemaInfo().setName("newschemaname").setUuid(schema.getUuid()).setVersion("2.0"));

			// Generate version 3 which should not be auto assigned to the project release
			updateSchema(schema.getUuid(), "anothernewschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(false).setReleaseNames(
				INITIAL_RELEASE_NAME));

			// Assert that version 2 is still assigned to the release
			infoList = call(() -> client().getReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid()));
			assertThat(infoList.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new ReleaseSchemaInfo().setName("newschemaname").setUuid(schema.getUuid()).setVersion("2.0"));

			// Generate version 4
			waitForJobs(() -> {
				updateSchema(schema.getUuid(), "anothernewschemaname2", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(true));
			}, COMPLETED, 1);

			// Assert that version 4 is assigned to the release
			infoList = call(() -> client().getReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid()));

			assertThat(infoList.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new ReleaseSchemaInfo().setName("anothernewschemaname2").setUuid(schema.getUuid()).setVersion("4.0"));

			// Generate version 5
			updateSchema(schema.getUuid(), "anothernewschemaname3", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(true).setReleaseNames(
				"bla", "bogus", "moped"));

			// Assert that version 4 is still assigned to the release since non of the names matches the project release
			infoList = call(() -> client().getReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid()));
			assertThat(infoList.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new ReleaseSchemaInfo().setName("anothernewschemaname2").setUuid(schema.getUuid()).setVersion("4.0"));

		}
	}

	@Test
	public void testAssignSchemaVersion() throws Exception {
		// Grant admin perm
		tx(() -> group().addRole(roles().get("admin")));

		// create version 1 of a schema
		SchemaResponse schema = createSchema("schemaname");

		// assign schema to project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

		// generate version 2
		updateSchema(schema.getUuid(), "newschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(false));

		// generate version 3
		updateSchema(schema.getUuid(), "anothernewschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(false));

		// check that version 1 is assigned to release
		ReleaseInfoSchemaList list = call(() -> client().getReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid()));
		assertThat(list.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
			new ReleaseSchemaInfo().setName("schemaname").setUuid(schema.getUuid()).setVersion("1.0"));

		// assign version 2 to the release
		ReleaseInfoSchemaList info = new ReleaseInfoSchemaList();
		info.getSchemas().add(new ReleaseSchemaInfo().setUuid(schema.getUuid()).setVersion("2.0"));
		waitForJobs(() -> {
			call(() -> client().assignReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid(), info));
		}, COMPLETED, 1);

		JobListResponse jobList = call(() -> client().findJobs());
		System.out.println(jobList.toJson());
		JobResponse job = jobList.getData().stream().filter(j -> j.getProperties().get("schemaUuid").equals(schema.getUuid())).findAny().get();

		ReleaseInfoSchemaList schemaList = call(() -> client().getReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid()));
		System.out.println(schemaList.toJson());
		ReleaseSchemaInfo schemaInfo = schemaList.getSchemas().stream().filter(s -> s.getUuid().equals(schema.getUuid())).findFirst().get();
		assertEquals(COMPLETED, schemaInfo.getMigrationStatus());
		assertEquals(job.getUuid(), schemaInfo.getJobUuid());

		list = call(() -> client().getReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid()));
		assertThat(list.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
			new ReleaseSchemaInfo().setName("newschemaname").setUuid(schema.getUuid()).setVersion("2.0"));
	}

	@Test
	public void testAssignBogusSchemaVersion() throws Exception {
		call(() -> client().assignReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid(), new SchemaReferenceImpl().setName("content").setVersion(
			"4711.0")), BAD_REQUEST, "error_schema_reference_not_found", "content", "-", "4711.0");
	}

	@Test
	public void testAssignBogusSchemaUuid() throws Exception {
		call(() -> client().assignReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid(), new SchemaReferenceImpl().setUuid("bogusuuid").setVersion(
			"1.0")), BAD_REQUEST, "error_schema_reference_not_found", "-", "bogusuuid", "1.0");
	}

	@Test
	public void testAssignBogusSchemaName() throws Exception {
		call(() -> client().assignReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid(), new SchemaReferenceImpl().setName("bogusname").setVersion(
			"1.0")), BAD_REQUEST, "error_schema_reference_not_found", "bogusname", "-", "1.0");
	}

	@Test
	public void testAssignUnassignedSchemaVersion() throws Exception {
		final String schemaName = "schemaname";
		try (Tx tx = tx()) {
			createSchema(schemaName);
			tx.success();
		}

		call(() -> client().assignReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid(), new SchemaReferenceImpl().setName(schemaName).setVersion(
			"1.0")), BAD_REQUEST, "error_schema_reference_not_found", schemaName, "-", "1.0");
	}

	@Test
	public void testAssignOlderSchemaVersion() throws Exception {
		String schemaUuid;
		try (Tx tx = tx()) {
			// create version 1 of a schema
			SchemaResponse schema = createSchema("schemaname");
			schemaUuid = schema.getUuid();
			// generate version 2
			updateSchema(schema.getUuid(), "newschemaname");

			// assign schema to project
			call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));
			tx.success();
		}
		// try to downgrade schema version
		call(() -> client().assignReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid(), new SchemaReferenceImpl().setUuid(schemaUuid).setVersion(
			"1.0")), BAD_REQUEST, "release_error_downgrade_schema_version", "schemaname", "2.0", "1.0");

	}

	@Test
	public void testAssignSchemaVersionNoPermission() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			role().revokePermissions(project.getInitialRelease(), UPDATE_PERM);
			tx.success();
		}

		call(() -> client().assignReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid(), new SchemaReferenceImpl().setName("content").setVersion(
			"1.0")), FORBIDDEN, "error_missing_perm", initialReleaseUuid());
	}

	@Test
	public void testAssignLatestSchemaVersion() throws Exception {
		String schemaUuid;
		try (Tx tx = tx()) {
			// create version 1 of a schema
			SchemaResponse schema = createSchema("schemaname");
			schemaUuid = schema.getUuid();

			// assign schema to project
			call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

			// generate version 2
			updateSchema(schema.getUuid(), "newschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(false));

			// generate version 3
			updateSchema(schema.getUuid(), "anothernewschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(false));
			tx.success();
		}

		// check that version 1 is assigned to release
		ReleaseInfoSchemaList list = call(() -> client().getReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid()));
		assertThat(list.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
			new ReleaseSchemaInfo().setName("schemaname").setUuid(schemaUuid).setVersion("1.0"));

		// assign latest version to the release
		ReleaseInfoSchemaList info = new ReleaseInfoSchemaList();
		info.getSchemas().add(new ReleaseSchemaInfo().setUuid(schemaUuid));
		call(() -> client().assignReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid(), info));

		// assert
		list = call(() -> client().getReleaseSchemaVersions(PROJECT_NAME, initialReleaseUuid()));
		assertThat(list.getSchemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
			new ReleaseSchemaInfo().setName("anothernewschemaname").setUuid(schemaUuid).setVersion("3.0"));

	}

	// Tests for assignment of microschema versions to releases

	@Test
	public void testReadMicroschemaVersions() throws Exception {
		ReleaseMicroschemaInfo vcard = new ReleaseMicroschemaInfo(db().tx(() -> microschemaContainer("vcard").getLatestVersion()
			.transformToReference()));
		ReleaseMicroschemaInfo captionedImage = new ReleaseMicroschemaInfo(db().tx(() -> microschemaContainer("captionedImage").getLatestVersion()
			.transformToReference()));
		ReleaseInfoMicroschemaList list = call(() -> client().getReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid()));
		assertThat(list.getMicroschemas()).as("release microschema versions").usingElementComparatorOnFields("name", "uuid", "version").containsOnly(
			vcard, captionedImage);
	}

	@Test
	public void testAssignMicroschemaVersion() throws Exception {
		try (Tx tx = tx()) {
			// create version 1 of a microschema
			MicroschemaResponse microschema = createMicroschema("microschemaname");

			// assign microschema to project
			call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

			// generate version 2
			updateMicroschema(microschema.getUuid(), "newmicroschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(false));

			// generate version 3
			updateMicroschema(microschema.getUuid(), "anothernewmicroschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(false));

			// check that version 1 is assigned to release
			ReleaseInfoMicroschemaList list = call(() -> client().getReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid()));
			assertThat(list.getMicroschemas()).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new ReleaseMicroschemaInfo(new MicroschemaReferenceImpl().setName("microschemaname").setUuid(microschema.getUuid()).setVersion(
					"1.0")));

			ReleaseInfoMicroschemaList info = new ReleaseInfoMicroschemaList();
			info.add(new MicroschemaReferenceImpl().setUuid(microschema.getUuid()).setVersion("2.0"));

			// assign version 2 to the release
			waitForJobs(() -> {
				call(() -> client().assignReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid(), info));
			}, COMPLETED, 1);

			// assert
			list = call(() -> client().getReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid()));
			assertThat(list.getMicroschemas()).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new ReleaseMicroschemaInfo(new MicroschemaReferenceImpl().setName("newmicroschemaname").setUuid(microschema.getUuid()).setVersion(
					"2.0")));
		}
	}

	@Test
	public void testAssignMicroschemaVersionViaMicroschemaUpdate() throws Exception {
		try (Tx tx = tx()) {
			// create version 1 of a microschema
			MicroschemaResponse microschema = createMicroschema("microschemaname");

			// assign microschema to project
			call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

			// generate version 2
			waitForJobs(() -> {
				updateMicroschema(microschema.getUuid(), "newmicroschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(true));
			}, COMPLETED, 1);

			// generate version 3
			waitForJobs(() -> {
				updateMicroschema(microschema.getUuid(), "anothernewmicroschemaname");
			}, COMPLETED, 1);

			// check that version 3 is assigned to release
			ReleaseInfoMicroschemaList list = call(() -> client().getReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid()));
			assertThat(list.getMicroschemas()).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new ReleaseMicroschemaInfo(new MicroschemaReferenceImpl().setName("anothernewmicroschemaname").setUuid(microschema.getUuid())
					.setVersion("3.0")));

			// assign version 2 to the release
			// call(() -> getClient().assignReleaseMicroschemaVersions(PROJECT_NAME, releaseUuid(),
			// new MicroschemaReferenceList(Arrays.asList(new MicroschemaReference().setUuid(microschema.getUuid()).setVersion(2)))));

			// assert
			// list = call(() -> getClient().getReleaseMicroschemaVersions(PROJECT_NAME, releaseUuid()));
			// assertThat(list).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version")
			// .contains(new MicroschemaReference().setName("newmicroschemaname").setUuid(microschema.getUuid()).setVersion(2));
		}
	}

	@Test
	public void testAssignBogusMicroschemaVersion() throws Exception {
		call(() -> client().assignReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid(), new MicroschemaReferenceImpl().setName("vcard")
			.setVersion("4711")), BAD_REQUEST, "error_microschema_reference_not_found", "vcard", "-", "4711");
	}

	@Test
	public void testAssignBogusMicroschemaUuid() throws Exception {
		call(() -> client().assignReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid(), new MicroschemaReferenceImpl().setUuid("bogusuuid")
			.setVersion("1.0")), BAD_REQUEST, "error_microschema_reference_not_found", "-", "bogusuuid", "1.0");
	}

	@Test
	public void testAssignBogusMicroschemaName() throws Exception {
		call(() -> client().assignReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid(), new MicroschemaReferenceImpl().setName("bogusname")
			.setVersion("1.0")), BAD_REQUEST, "error_microschema_reference_not_found", "bogusname", "-", "1.0");
	}

	@Test
	public void testAssignUnassignedMicroschemaVersion() throws Exception {
		try (Tx tx = tx()) {
			SchemaModel schema = createSchema("microschemaname");

			call(() -> client().assignReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid(), new MicroschemaReferenceImpl().setName(schema
				.getName()).setVersion(schema.getVersion())), BAD_REQUEST, "error_microschema_reference_not_found", schema.getName(), "-", schema
					.getVersion());
		}
	}

	@Test
	public void testAssignOlderMicroschemaVersion() throws Exception {

		// create version 1 of a microschema
		MicroschemaResponse microschema = createMicroschema("microschemaname");

		// generate version 2
		updateMicroschema(microschema.getUuid(), "newmicroschemaname");

		// assign microschema to project
		call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

		// try to downgrade microschema version
		call(() -> client().assignReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid(), new MicroschemaReferenceImpl().setUuid(microschema
			.getUuid()).setVersion("1.0")), BAD_REQUEST, "release_error_downgrade_microschema_version", "microschemaname", "2.0", "1.0");

	}

	@Test
	public void testAssignMicroschemaVersionNoPermission() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			role().revokePermissions(project.getInitialRelease(), UPDATE_PERM);
			tx.success();
		}
		call(() -> client().assignReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid(), new MicroschemaReferenceImpl().setName("vcard")
			.setVersion("1.0")), FORBIDDEN, "error_missing_perm", initialReleaseUuid());
	}

	@Test
	public void testAssignLatestMicroschemaVersion() throws Exception {
		try (Tx tx = tx()) {
			// create version 1 of a microschema
			MicroschemaResponse microschema = createMicroschema("microschemaname");

			// Assign microschema to project
			call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

			// Generate version 2
			waitForJobs(() -> {
				updateMicroschema(microschema.getUuid(), "newmicroschemaname");
			}, COMPLETED, 1);

			// Assert that version 2 is assigned to release
			ReleaseInfoMicroschemaList list = call(() -> client().getReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid()));
			assertThat(list.getMicroschemas()).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new ReleaseMicroschemaInfo(new MicroschemaReferenceImpl().setName("newmicroschemaname").setUuid(microschema.getUuid()).setVersion(
					"2.0")));

			// Generate version 3 which should not be auto assigned to the project release
			updateMicroschema(microschema.getUuid(), "anothernewschemaname", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(false));

			// Assert that version 2 is still assigned to release
			list = call(() -> client().getReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid()));
			assertThat(list.getMicroschemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new ReleaseMicroschemaInfo(new MicroschemaReferenceImpl().setName("newmicroschemaname").setUuid(microschema.getUuid()).setVersion(
					"2.0")));

			// Generate version 4
			updateMicroschema(microschema.getUuid(), "anothernewschemaname1", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(true)
				.setReleaseNames(INITIAL_RELEASE_NAME));

			// Assert that version 4 is assigned to the release
			list = call(() -> client().getReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid()));
			assertThat(list.getMicroschemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new ReleaseMicroschemaInfo(new MicroschemaReferenceImpl().setName("anothernewschemaname1").setUuid(microschema.getUuid())
					.setVersion("4.0")));

			// Generate version 5
			waitForJobs(() -> {
				updateMicroschema(microschema.getUuid(), "anothernewschemaname2", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(true));
			}, COMPLETED, 1);

			// Assert that version 5
			list = call(() -> client().getReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid()));
			assertThat(list.getMicroschemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new ReleaseMicroschemaInfo(new MicroschemaReferenceImpl().setName("anothernewschemaname2").setUuid(microschema.getUuid())
					.setVersion("5.0")));

			// Generate version 6
			updateMicroschema(microschema.getUuid(), "anothernewschemaname3", new SchemaUpdateParametersImpl().setUpdateAssignedReleases(true)
				.setReleaseNames("bla", "bogus", "moped"));

			// Assert that version 4 is still assigned to the release since non of the names matches the project release
			list = call(() -> client().getReleaseMicroschemaVersions(PROJECT_NAME, initialReleaseUuid()));
			assertThat(list.getMicroschemas()).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version").contains(
				new ReleaseMicroschemaInfo(new MicroschemaReferenceImpl().setName("anothernewschemaname2").setUuid(microschema.getUuid())
					.setVersion("5.0")));
		}
	}
}
