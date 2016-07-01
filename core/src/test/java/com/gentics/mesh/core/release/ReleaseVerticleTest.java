package com.gentics.mesh.core.release;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.mock.Mocks.getMockedRoutingContext;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractSpringVerticle;
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
import com.gentics.mesh.core.verticle.microschema.MicroschemaVerticle;
import com.gentics.mesh.core.verticle.microschema.ProjectMicroschemaVerticle;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.core.verticle.release.ReleaseVerticle;
import com.gentics.mesh.core.verticle.schema.ProjectSchemaVerticle;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.mock.Mocks;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.test.AbstractBasicIsolatedCrudVerticleTest;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

public class ReleaseVerticleTest extends AbstractBasicIsolatedCrudVerticleTest {
	@Autowired
	private ReleaseVerticle releaseVerticle;

	@Autowired
	private ProjectVerticle projectVerticle;

	@Autowired
	private SchemaVerticle schemaVerticle;

	@Autowired
	private MicroschemaVerticle microschemaVerticle;

	@Autowired
	private ProjectSchemaVerticle projectSchemaVerticle;

	@Autowired
	private ProjectMicroschemaVerticle projectMicroschemaVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		return new ArrayList<AbstractSpringVerticle>(Arrays.asList(releaseVerticle, projectVerticle, schemaVerticle, microschemaVerticle,
				projectSchemaVerticle, projectMicroschemaVerticle));
	}

	@Override
	public void testUpdateMultithreaded() throws Exception {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 200;
		try (NoTrx noTx = db.noTrx()) {
			String projectName = project().getName();
			String uuid = project().getInitialRelease().getUuid();

			Set<Future<?>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(getClient().findReleaseByUuid(projectName, uuid));
			}

			for (Future<?> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

	@Override
	public void testDeleteByUUIDMultithreaded() throws Exception {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreateMultithreaded() throws Exception {
		String releaseName = "Release V";
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			int nJobs = 100;

			Set<Future<ReleaseResponse>> responseFutures = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				ReleaseCreateRequest request = new ReleaseCreateRequest();
				request.setName(releaseName + i);
				Future<ReleaseResponse> future = getClient().createRelease(project.getName(), request);
				responseFutures.add(future);
			}

			Set<String> uuids = new HashSet<>();
			uuids.add(project.getInitialRelease().getUuid());
			for (Future<ReleaseResponse> future : responseFutures) {
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
		try (NoTrx noTx = db.noTrx()) {
			Set<Future<ReleaseResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(getClient().findReleaseByUuid(project().getName(), project().getInitialRelease().getUuid()));
			}
			for (Future<ReleaseResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		String releaseName = "Release V1";
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();

			ReleaseCreateRequest request = new ReleaseCreateRequest();
			request.setName(releaseName);

			ReleaseResponse response = call(() -> getClient().createRelease(project.getName(), request));
			assertThat(response).as("Release Response").isNotNull().hasName(releaseName).isActive().isNotMigrated();
		}
	}

	@Test
	public void testCreateWithoutPerm() throws Exception {
		String releaseName = "Release V1";
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			String uuid = project.getUuid();
			String name = project.getName();
			role().grantPermissions(project, READ_PERM);
			role().revokePermissions(project, UPDATE_PERM);

			ReleaseCreateRequest request = new ReleaseCreateRequest();
			request.setName(releaseName);

			call(() -> getClient().createRelease(project.getName(), request), FORBIDDEN, "error_missing_perm", uuid + "/" + name);
		}
	}

	@Test
	public void testCreateWithoutName() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			call(() -> getClient().createRelease(project.getName(), new ReleaseCreateRequest()), BAD_REQUEST, "release_missing_name");
		}
	}

	@Test
	public void testCreateWithConflictingName1() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			ReleaseCreateRequest request = new ReleaseCreateRequest();
			request.setName(project.getName());

			call(() -> getClient().createRelease(project.getName(), request), CONFLICT, "release_conflicting_name", project.getName());
		}
	}

	@Test
	public void testCreateWithConflictingName2() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			String releaseName = "New Release";
			Project project = project();
			ReleaseCreateRequest request = new ReleaseCreateRequest();
			request.setName(releaseName);

			call(() -> getClient().createRelease(project.getName(), request));

			call(() -> getClient().createRelease(project.getName(), request), CONFLICT, "release_conflicting_name", releaseName);
		}
	}

	@Test
	public void testCreateWithConflictingName3() throws Exception {
		String releaseName = "New Release";
		String newProjectName = "otherproject";
		String projectName = db.noTrx(() -> project().getName());
		ReleaseCreateRequest request = new ReleaseCreateRequest();
		request.setName(releaseName);

		call(() -> getClient().createRelease(projectName, request));

		ProjectCreateRequest createProject = new ProjectCreateRequest();
		createProject.setName(newProjectName);
		createProject.setSchemaReference(new SchemaReference().setName("folder"));
		call(() -> getClient().createProject(createProject));

		call(() -> getClient().createRelease(newProjectName, request));

	}

	@Override
	public void testCreateReadDelete() throws Exception {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release firstRelease = project.getReleaseRoot().create("One", user());
			Release secondRelease = project.getReleaseRoot().create("Two", user());
			Release thirdRelease = project.getReleaseRoot().create("Three", user());

			for (Release release : Arrays.asList(initialRelease, firstRelease, secondRelease, thirdRelease)) {
				ReleaseResponse response = call(() -> getClient().findReleaseByUuid(project.getName(), release.getUuid()));
				assertThat(response).isNotNull().hasName(release.getName()).hasUuid(release.getUuid()).isActive();
			}
		}
	}

	@Test
	public void testReadByBogusUUID() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			call(() -> getClient().findReleaseByUuid(project.getName(), "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
		}
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			String projectName = project.getName();
			String uuid = project.getInitialRelease().getUuid();

			ReleaseResponse response = call(
					() -> getClient().findReleaseByUuid(projectName, uuid, new RolePermissionParameters().setRoleUuid(role().getUuid())));
			assertThat(response.getRolePerms()).isNotNull().contains("read", "create", "update", "delete");
		}
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			String releaseUuid = project.getInitialRelease().getUuid();
			String name = project.getName();
			role().revokePermissions(project.getInitialRelease(), READ_PERM);

			call(() -> getClient().findReleaseByUuid(name, releaseUuid), FORBIDDEN, "error_missing_perm", releaseUuid);
		}
	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release firstRelease = project.getReleaseRoot().create("One", user());
			Release secondRelease = project.getReleaseRoot().create("Two", user());
			Release thirdRelease = project.getReleaseRoot().create("Three", user());

			ListResponse<ReleaseResponse> responseList = call(() -> getClient().findReleases(project.getName()));

			InternalActionContext ac = Mocks.getMockedInternalActionContext(user());

			assertThat(responseList).isNotNull();
			assertThat(responseList.getData()).usingElementComparatorOnFields("uuid", "name").containsOnly(
					initialRelease.transformToRestSync(ac, 0).toBlocking().single(), firstRelease.transformToRestSync(ac, 0).toBlocking().single(),
					secondRelease.transformToRestSync(ac, 0).toBlocking().single(), thirdRelease.transformToRestSync(ac, 0).toBlocking().single());
		}
	}

	@Test
	public void testReadMultipleWithRestrictedPermissions() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release firstRelease = project.getReleaseRoot().create("One", user());
			Release secondRelease = project.getReleaseRoot().create("Two", user());
			Release thirdRelease = project.getReleaseRoot().create("Three", user());

			role().revokePermissions(firstRelease, READ_PERM);
			role().revokePermissions(thirdRelease, READ_PERM);

			ListResponse<ReleaseResponse> responseList = call(() -> getClient().findReleases(project.getName()));

			InternalActionContext ac = Mocks.getMockedInternalActionContext(user());
			assertThat(responseList).isNotNull();
			assertThat(responseList.getData()).usingElementComparatorOnFields("uuid", "name").containsOnly(
					initialRelease.transformToRestSync(ac, 0).toBlocking().single(), secondRelease.transformToRestSync(ac, 0).toBlocking().single());
		}
	}

	@Test
	@Override
	public void testUpdate() throws Exception {
		String newName = "New Release Name";
		String anotherNewName = "Another New Release Name";
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			String projectName = project.getName();
			String uuid = project.getInitialRelease().getUuid();

			// change name
			ReleaseUpdateRequest request1 = new ReleaseUpdateRequest();
			request1.setName(newName);
			ReleaseResponse response = call(() -> getClient().updateRelease(projectName, uuid, request1));
			assertThat(response).as("Updated Release").isNotNull().hasName(newName).isActive();

			// change active
			ReleaseUpdateRequest request2 = new ReleaseUpdateRequest();
			request2.setActive(false);
			response = call(() -> getClient().updateRelease(projectName, uuid, request2));
			assertThat(response).as("Updated Release").isNotNull().hasName(newName).isInactive();

			// change active and name
			ReleaseUpdateRequest request3 = new ReleaseUpdateRequest();
			request3.setActive(true);
			request3.setName(anotherNewName);
			response = call(() -> getClient().updateRelease(projectName, uuid, request3));
			assertThat(response).as("Updated Release").isNotNull().hasName(anotherNewName).isActive();
		}
	}

	@Test
	public void testUpdateWithNameConflict() throws Exception {
		String newName = "New Release Name";
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			project.getReleaseRoot().create(newName, user());

			ReleaseUpdateRequest request = new ReleaseUpdateRequest();
			request.setName(newName);
			call(() -> getClient().updateRelease(project.getName(), project.getInitialRelease().getUuid(), request), CONFLICT,
					"release_conflicting_name", newName);
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			String projectName = project.getName();
			role().revokePermissions(project.getInitialRelease(), UPDATE_PERM);

			ReleaseUpdateRequest request = new ReleaseUpdateRequest();
			request.setActive(false);
			call(() -> getClient().updateRelease(projectName, project.getInitialRelease().getUuid(), request), FORBIDDEN, "error_missing_perm",
					project.getInitialRelease().getUuid());
		}
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();

			ReleaseUpdateRequest request = new ReleaseUpdateRequest();
			request.setActive(false);
			call(() -> getClient().updateRelease(project.getName(), "bogus", request), NOT_FOUND, "object_not_found_for_uuid", "bogus");
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
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			SchemaReferenceList list = call(() -> getClient().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));

			SchemaReference content = schemaContainer("content").getLatestVersion().transformToReference();
			SchemaReference folder = schemaContainer("folder").getLatestVersion().transformToReference();
			SchemaReference binaryContent = schemaContainer("binary-content").getLatestVersion().transformToReference();

			assertThat(list).as("release schema versions").usingElementComparatorOnFields("name", "uuid", "version").containsOnly(content, folder,
					binaryContent);
		}
	}

	@Test
	public void testAssignSchemaVersion() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			// create version 1 of a schema
			Schema schema = createSchema("schemaname");
			Project project = project();

			// assign schema to project
			call(() -> getClient().assignSchemaToProject(project.getName(), schema.getUuid()));

			// generate version 2
			updateSchema(schema.getUuid(), "newschemaname");

			// generate version 3
			updateSchema(schema.getUuid(), "anothernewschemaname");

			// check that version 1 is assigned to release
			SchemaReferenceList list = call(() -> getClient().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new SchemaReference().setName("schemaname").setUuid(schema.getUuid()).setVersion(1));

			// assign version 2 to the release
			call(() -> getClient().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReferenceList(Arrays.asList(new SchemaReference().setUuid(schema.getUuid()).setVersion(2)))));

			// assert
			list = call(() -> getClient().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new SchemaReference().setName("newschemaname").setUuid(schema.getUuid()).setVersion(2));
		}
	}

	@Test
	public void testAssignBogusSchemaVersion() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();

			call(() -> getClient().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReference().setName("content").setVersion(4711)), BAD_REQUEST, "error_schema_reference_not_found", "content", "-",
					"4711");
		}
	}

	@Test
	public void testAssignBogusSchemaUuid() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();

			call(() -> getClient().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReference().setUuid("bogusuuid").setVersion(1)), BAD_REQUEST, "error_schema_reference_not_found", "-", "bogusuuid",
					"1");
		}
	}

	@Test
	public void testAssignBogusSchemaName() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();

			call(() -> getClient().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReference().setName("bogusname").setVersion(1)), BAD_REQUEST, "error_schema_reference_not_found", "bogusname", "-",
					"1");
		}
	}

	@Test
	public void testAssignUnassignedSchemaVersion() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Schema schema = createSchema("schemaname");
			Project project = project();

			call(() -> getClient().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReference().setName(schema.getName()).setVersion(schema.getVersion())), BAD_REQUEST, "error_schema_reference_not_found",
					schema.getName(), "-", Integer.toString(schema.getVersion()));
		}
	}

	@Test
	public void testAssignOlderSchemaVersion() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			// create version 1 of a schema
			Schema schema = createSchema("schemaname");
			Project project = project();

			// generate version 2
			updateSchema(schema.getUuid(), "newschemaname");

			// assign schema to project
			call(() -> getClient().assignSchemaToProject(project.getName(), schema.getUuid()));

			// try to downgrade schema version
			call(() -> getClient().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReference().setUuid(schema.getUuid()).setVersion(1)), BAD_REQUEST, "error_release_downgrade_schema_version",
					"schemaname", "2", "1");
		}
	}

	@Test
	public void testAssignSchemaVersionNoPermission() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			role().revokePermissions(project.getInitialRelease(), UPDATE_PERM);

			call(() -> getClient().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReference().setName("content").setVersion(1)), FORBIDDEN, "error_missing_perm", project.getInitialRelease().getUuid());
		}
	}

	@Test
	public void testAssignLatestSchemaVersion() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			// create version 1 of a schema
			Schema schema = createSchema("schemaname");
			Project project = project();

			// assign schema to project
			call(() -> getClient().assignSchemaToProject(project.getName(), schema.getUuid()));

			// generate version 2
			updateSchema(schema.getUuid(), "newschemaname");

			// generate version 3
			updateSchema(schema.getUuid(), "anothernewschemaname");

			// check that version 1 is assigned to release
			SchemaReferenceList list = call(() -> getClient().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new SchemaReference().setName("schemaname").setUuid(schema.getUuid()).setVersion(1));

			// assign latest version to the release
			call(() -> getClient().assignReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new SchemaReferenceList(Arrays.asList(new SchemaReference().setUuid(schema.getUuid())))));

			// assert
			list = call(() -> getClient().getReleaseSchemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial schema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new SchemaReference().setName("anothernewschemaname").setUuid(schema.getUuid()).setVersion(3));
		}
	}

	// Tests for assignment of microschema versions to releases

	@Test
	public void testReadMicroschemaVersions() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			MicroschemaReferenceList list = call(
					() -> getClient().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));

			MicroschemaReference vcard = microschemaContainer("vcard").getLatestVersion().transformToReference();
			MicroschemaReference captionedImage = microschemaContainer("captionedImage").getLatestVersion().transformToReference();

			assertThat(list).as("release microschema versions").usingElementComparatorOnFields("name", "uuid", "version").containsOnly(vcard,
					captionedImage);
		}
	}

	@Test
	public void testAssignMicroschemaVersion() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			// create version 1 of a microschema
			Microschema microschema = createMicroschema("microschemaname");
			Project project = project();

			// assign microschema to project
			call(() -> getClient().assignMicroschemaToProject(project.getName(), microschema.getUuid()));

			// generate version 2
			updateMicroschema(microschema.getUuid(), "newmicroschemaname");

			// generate version 3
			updateMicroschema(microschema.getUuid(), "anothernewmicroschemaname");

			// check that version 1 is assigned to release
			MicroschemaReferenceList list = call(
					() -> getClient().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new MicroschemaReference().setName("microschemaname").setUuid(microschema.getUuid()).setVersion(1));

			// assign version 2 to the release
			call(() -> getClient().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReferenceList(Arrays.asList(new MicroschemaReference().setUuid(microschema.getUuid()).setVersion(2)))));

			// assert
			list = call(() -> getClient().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new MicroschemaReference().setName("newmicroschemaname").setUuid(microschema.getUuid()).setVersion(2));
		}
	}

	@Test
	public void testAssignBogusMicroschemaVersion() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();

			call(() -> getClient().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReference().setName("vcard").setVersion(4711)), BAD_REQUEST, "error_microschema_reference_not_found", "vcard", "-",
					"4711");
		}
	}

	@Test
	public void testAssignBogusMicroschemaUuid() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();

			call(() -> getClient().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReference().setUuid("bogusuuid").setVersion(1)), BAD_REQUEST, "error_microschema_reference_not_found", "-",
					"bogusuuid", "1");
		}
	}

	@Test
	public void testAssignBogusMicroschemaName() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();

			call(() -> getClient().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReference().setName("bogusname").setVersion(1)), BAD_REQUEST, "error_microschema_reference_not_found", "bogusname",
					"-", "1");
		}
	}

	@Test
	public void testAssignUnassignedMicroschemaVersion() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Schema schema = createSchema("microschemaname");
			Project project = project();

			call(() -> getClient().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReference().setName(schema.getName()).setVersion(schema.getVersion())), BAD_REQUEST,
					"error_microschema_reference_not_found", schema.getName(), "-", Integer.toString(schema.getVersion()));
		}
	}

	@Test
	public void testAssignOlderMicroschemaVersion() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			// create version 1 of a microschema
			Microschema microschema = createMicroschema("microschemaname");
			Project project = project();

			// generate version 2
			updateMicroschema(microschema.getUuid(), "newmicroschemaname");

			// assign microschema to project
			call(() -> getClient().assignMicroschemaToProject(project.getName(), microschema.getUuid()));

			// try to downgrade microschema version
			call(() -> getClient().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReference().setUuid(microschema.getUuid()).setVersion(1)), BAD_REQUEST,
					"error_release_downgrade_microschema_version", "microschemaname", "2", "1");
		}
	}

	@Test
	public void testAssignMicroschemaVersionNoPermission() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Project project = project();
			role().revokePermissions(project.getInitialRelease(), UPDATE_PERM);

			call(() -> getClient().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReference().setName("vcard").setVersion(1)), FORBIDDEN, "error_missing_perm",
					project.getInitialRelease().getUuid());
		}
	}

	@Test
	public void testAssignLatestMicroschemaVersion() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			// create version 1 of a microschema
			Microschema microschema = createMicroschema("microschemaname");
			Project project = project();

			// assign microschema to project
			call(() -> getClient().assignMicroschemaToProject(project.getName(), microschema.getUuid()));

			// generate version 2
			updateMicroschema(microschema.getUuid(), "newmicroschemaname");

			// generate version 3
			updateMicroschema(microschema.getUuid(), "anothernewmicroschemaname");

			// check that version 1 is assigned to release
			MicroschemaReferenceList list = call(
					() -> getClient().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Initial microschema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new MicroschemaReference().setName("microschemaname").setUuid(microschema.getUuid()).setVersion(1));

			// assign latest version to the release
			call(() -> getClient().assignReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid(),
					new MicroschemaReferenceList(Arrays.asList(new MicroschemaReference().setUuid(microschema.getUuid())))));

			// assert
			list = call(() -> getClient().getReleaseMicroschemaVersions(project.getName(), project.getInitialRelease().getUuid()));
			assertThat(list).as("Updated microschema versions").usingElementComparatorOnFields("name", "uuid", "version")
					.contains(new MicroschemaReference().setName("anothernewmicroschemaname").setUuid(microschema.getUuid()).setVersion(3));
		}
	}
}
