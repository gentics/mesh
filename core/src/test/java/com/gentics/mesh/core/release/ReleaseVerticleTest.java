package com.gentics.mesh.core.release;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseListResponse;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.core.verticle.release.ReleaseVerticle;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.RolePermissionParameter;
import com.gentics.mesh.test.AbstractBasicCrudVerticleTest;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

public class ReleaseVerticleTest extends AbstractBasicCrudVerticleTest {
	@Autowired
	private ReleaseVerticle releaseVerticle;

	@Autowired
	private ProjectVerticle projectVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		return new ArrayList<AbstractSpringVerticle>(Arrays.asList(releaseVerticle, projectVerticle));
	}

	@Override
	public void testUpdateMultithreaded() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testReadByUuidMultithreaded() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testDeleteByUUIDMultithreaded() throws Exception {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreateMultithreaded() throws Exception {
		String releaseName = "Release V";
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
		} while(release != null);

		assertThat(previousRelease).as("Latest Release").matches(project.getLatestRelease());
		assertThat(foundReleases).as("Found Releases").containsOnlyElementsOf(uuids);
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;
		Set<Future<ReleaseResponse>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().findReleaseByUuid(project().getName(), project().getInitialRelease().getUuid()));
		}
		for (Future<ReleaseResponse> future : set) {
			latchFor(future);
			assertSuccess(future);
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		String releaseName = "Release V1";
		Project project = project();

		ReleaseCreateRequest request = new ReleaseCreateRequest();
		request.setName(releaseName);

		Future<ReleaseResponse> future = getClient().createRelease(project.getName(), request);
		latchFor(future);
		assertSuccess(future);

		ReleaseResponse response = future.result();
		assertThat(response).as("Release Response").isNotNull().hasName(releaseName).isActive();
	}

	@Test
	public void testCreateWithoutPerm() throws Exception {
		String releaseName = "Release V1";
		Project project = project();
		String uuid = project.getUuid();
		String name = project.getName();
		role().grantPermissions(project, READ_PERM);
		role().revokePermissions(project, UPDATE_PERM);

		ReleaseCreateRequest request = new ReleaseCreateRequest();
		request.setName(releaseName);

		Future<ReleaseResponse> future = getClient().createRelease(project.getName(), request);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid + "/" + name);
	}

	@Test
	public void testCreateWithoutName() throws Exception {
		Project project = project();
		Future<ReleaseResponse> future = getClient().createRelease(project.getName(), new ReleaseCreateRequest());
		latchFor(future);
		expectException(future, BAD_REQUEST, "release_missing_name");
	}

	@Test
	public void testCreateWithConflictingName1() throws Exception {
		Project project = project();
		ReleaseCreateRequest request = new ReleaseCreateRequest();
		request.setName(project.getName());

		Future<ReleaseResponse> future = getClient().createRelease(project.getName(), request);
		latchFor(future);
		expectException(future, CONFLICT, "release_conflicting_name", project.getName());
	}

	@Test
	public void testCreateWithConflictingName2() throws Exception {
		String releaseName = "New Release";
		Project project = project();
		ReleaseCreateRequest request = new ReleaseCreateRequest();
		request.setName(releaseName);

		Future<ReleaseResponse> future = getClient().createRelease(project.getName(), request);
		latchFor(future);
		assertSuccess(future);

		future = getClient().createRelease(project.getName(), request);
		latchFor(future);
		expectException(future, CONFLICT, "release_conflicting_name", releaseName);
	}

	@Test
	public void testCreateWithConflictingName3() throws Exception {
		String releaseName = "New Release";
		String newProjectName = "otherproject";
		Project project = project();
		ReleaseCreateRequest request = new ReleaseCreateRequest();
		request.setName(releaseName);

		Future<ReleaseResponse> future = getClient().createRelease(project.getName(), request);
		latchFor(future);
		assertSuccess(future);

		ProjectCreateRequest createProject = new ProjectCreateRequest();
		createProject.setName(newProjectName);
		Future<ProjectResponse> projectFuture = getClient().createProject(createProject);
		latchFor(projectFuture);
		assertSuccess(projectFuture);

		future = getClient().createRelease(newProjectName, request);
		latchFor(future);
		assertSuccess(future);
	}

	@Override
	public void testCreateReadDelete() throws Exception {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		Project project = project();
		Release initialRelease = project.getInitialRelease();
		Release firstRelease = project.getReleaseRoot().create("One", user());
		Release secondRelease = project.getReleaseRoot().create("Two", user());
		Release thirdRelease = project.getReleaseRoot().create("Three", user());

		for (Release release : Arrays.asList(initialRelease, firstRelease, secondRelease, thirdRelease)) {
			Future<ReleaseResponse> future = getClient().findReleaseByUuid(project.getName(), release.getUuid());
			latchFor(future);
			assertSuccess(future);
			assertThat(future.result()).isNotNull().hasName(release.getName()).hasUuid(release.getUuid()).isActive();
		}
	}

	@Test
	public void testReadByBogusUUID() throws Exception {
		Project project = project();
		Future<ReleaseResponse> future = getClient().findReleaseByUuid(project.getName(), "bogus");
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		Project project = project();
		String projectName = project.getName();
		String uuid = project.getInitialRelease().getUuid();

		Future<ReleaseResponse> future = getClient().findReleaseByUuid(projectName, uuid, new RolePermissionParameter().setRoleUuid(role().getUuid()));
		latchFor(future);
		assertSuccess(future);
		assertNotNull(future.result().getRolePerms());
		assertEquals(4, future.result().getRolePerms().length);
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		Project project = project();
		String releaseUuid = project.getInitialRelease().getUuid();
		String name = project.getName();
		role().revokePermissions(project.getInitialRelease(), READ_PERM);

		Future<ReleaseResponse> future = getClient().findReleaseByUuid(name, releaseUuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", releaseUuid);
	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		Project project = project();
		Release initialRelease = project.getInitialRelease();
		Release firstRelease = project.getReleaseRoot().create("One", user());
		Release secondRelease = project.getReleaseRoot().create("Two", user());
		Release thirdRelease = project.getReleaseRoot().create("Three", user());

		Future<ReleaseListResponse> future = getClient().findReleases(project.getName());
		latchFor(future);
		assertSuccess(future);

		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);

		ListResponse<ReleaseResponse> responseList = future.result();
		assertThat(responseList).isNotNull();
		assertThat(responseList.getData()).usingElementComparatorOnFields("uuid", "name").containsOnly(
				initialRelease.transformToRestSync(ac).toBlocking().single(),
				firstRelease.transformToRestSync(ac).toBlocking().single(),
				secondRelease.transformToRestSync(ac).toBlocking().single(),
				thirdRelease.transformToRestSync(ac).toBlocking().single());
	}

	@Test
	public void testReadMultipleWithRestrictedPermissions() throws Exception {
		Project project = project();
		Release initialRelease = project.getInitialRelease();
		Release firstRelease = project.getReleaseRoot().create("One", user());
		Release secondRelease = project.getReleaseRoot().create("Two", user());
		Release thirdRelease = project.getReleaseRoot().create("Three", user());

		role().revokePermissions(firstRelease, READ_PERM);
		role().revokePermissions(thirdRelease, READ_PERM);

		Future<ReleaseListResponse> future = getClient().findReleases(project.getName());
		latchFor(future);
		assertSuccess(future);

		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);

		ListResponse<ReleaseResponse> responseList = future.result();
		assertThat(responseList).isNotNull();
		assertThat(responseList.getData()).usingElementComparatorOnFields("uuid", "name").containsOnly(
				initialRelease.transformToRestSync(ac).toBlocking().single(),
				secondRelease.transformToRestSync(ac).toBlocking().single());
	}

	@Override
	public void testUpdate() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testUpdateWithBogusUuid() throws HttpStatusCodeErrorException, Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testDeleteByUUID() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		// TODO Auto-generated method stub

	}

}
