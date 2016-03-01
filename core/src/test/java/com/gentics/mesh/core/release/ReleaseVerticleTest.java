package com.gentics.mesh.core.release;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;

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
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.core.verticle.release.ReleaseVerticle;
import com.gentics.mesh.test.AbstractBasicCrudVerticleTest;

import io.vertx.core.Future;

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

	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		// TODO Auto-generated method stub

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
		assertThat(response).as("Release Response").isNotNull().hasName(releaseName);
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

	@Override
	public void testReadByUUID() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testReadByUuidWithRolePerms() {
		// TODO Auto-generated method stub

	}

	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testReadMultiple() throws Exception {
		// TODO Auto-generated method stub

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
