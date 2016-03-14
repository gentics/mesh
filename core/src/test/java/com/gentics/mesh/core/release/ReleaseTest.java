package com.gentics.mesh.core.release;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.Test;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.ReleaseRoot;
import com.gentics.mesh.core.rest.release.ReleaseReference;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.test.AbstractBasicObjectTest;

import io.vertx.ext.web.RoutingContext;

public class ReleaseTest extends AbstractBasicObjectTest {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		InternalActionContext ac = getMockedInternalActionContext("");
		Release release = project().getInitialRelease();
		ReleaseReference reference = release.transformToReference();
		assertThat(reference).isNotNull();
		assertThat(reference.getName()).as("Reference name").isEqualTo(release.getName());
		assertThat(reference.getUuid()).as("Reference uuid").isEqualTo(release.getUuid());
	}

	@Test
	@Override
	public void testFindAllVisible() throws Exception {
		Project project = project();
		ReleaseRoot releaseRoot = project.getReleaseRoot();
		Release initialRelease = releaseRoot.getInitialRelease();
		Release releaseOne = releaseRoot.create("One", user());
		Release releaseTwo = releaseRoot.create("Two", user());
		Release releaseThree = releaseRoot.create("Three", user());

		PageImpl<? extends Release> page = releaseRoot.findAll(getRequestUser(), new PagingParameter(1, 25));
		assertThat(page).isNotNull();
		ArrayList<Release> arrayList = new ArrayList<Release>();
		page.iterator().forEachRemaining(r -> arrayList.add(r));
		assertThat(arrayList).usingElementComparatorOnFields("uuid").containsExactly(initialRelease, releaseOne,
				releaseTwo, releaseThree);
	}

	@Test
	@Override
	public void testFindAll() throws Exception {
		Project project = project();
		ReleaseRoot releaseRoot = project.getReleaseRoot();
		Release initialRelease = releaseRoot.getInitialRelease();
		Release releaseOne = releaseRoot.create("One", user());
		Release releaseTwo = releaseRoot.create("Two", user());
		Release releaseThree = releaseRoot.create("Three", user());

		assertThat(new ArrayList<Release>(releaseRoot.findAll())).usingElementComparatorOnFields("uuid")
				.containsExactly(initialRelease, releaseOne, releaseTwo, releaseThree);
	}

	@Test
	@Override
	public void testRootNode() throws Exception {
		Project project = project();
		ReleaseRoot releaseRoot = project.getReleaseRoot();
		assertThat(releaseRoot).as("Release Root of Project").isNotNull();
		Release initialRelease = project.getInitialRelease();
		assertThat(initialRelease).as("Initial Release of Project").isNotNull().isActive().isNamed(project.getName())
				.hasUuid().hasNext(null).hasPrevious(null);
		Release latestRelease = project.getLatestRelease();
		assertThat(latestRelease).as("Latest Release of Project").matches(initialRelease);
	}

	@Test
	@Override
	public void testFindByName() throws Exception {
		Project project = project();
		ReleaseRoot releaseRoot = project.getReleaseRoot();
		Release foundRelease = releaseRoot.findByName(project.getName()).toBlocking().single();
		assertThat(foundRelease).as("Release with name " + project.getName()).isNotNull().matches(project.getInitialRelease());
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		Project project = project();
		ReleaseRoot releaseRoot = project.getReleaseRoot();
		Release initialRelease = project.getInitialRelease();

		Release foundRelease = releaseRoot.findByUuid(initialRelease.getUuid()).toBlocking().single();
		assertThat(foundRelease).as("Release with uuid " + initialRelease.getUuid()).isNotNull().matches(initialRelease);
	}

	@Override
	public void testRead() throws Exception {
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		Project project = project(); 
		ReleaseRoot releaseRoot = project.getReleaseRoot();
		Release initialRelease = releaseRoot.getInitialRelease();
		Release firstNewRelease = releaseRoot.create("First new Release", user());
		Release secondNewRelease = releaseRoot.create("Second new Release", user());
		Release thirdNewRelease = releaseRoot.create("Third new Release", user());

		assertThat(project.getInitialRelease()).as("Initial Release").matches(initialRelease).hasNext(firstNewRelease)
				.hasPrevious(null);
		assertThat(firstNewRelease).as("First new Release").hasNext(secondNewRelease).hasPrevious(initialRelease);
		assertThat(secondNewRelease).as("Second new Release").hasNext(thirdNewRelease).hasPrevious(firstNewRelease);
		assertThat(project.getLatestRelease()).as("Latest Release").matches(thirdNewRelease).hasNext(null)
				.hasPrevious(secondNewRelease);

		assertThat(new ArrayList<Release>(releaseRoot.findAll())).usingElementComparatorOnFields("uuid")
				.containsExactly(initialRelease, firstNewRelease, secondNewRelease, thirdNewRelease);
	}

	@Override
	public void testDelete() throws Exception {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdate() throws Exception {
		Project project = project();
		Release initialRelease = project.getInitialRelease();
		initialRelease.setName("New Release Name");
		initialRelease.setActive(false);
		initialRelease.reload();

		assertThat(initialRelease).as("Release").isNamed("New Release Name").isInactive();
	}

	@Test
	@Override
	public void testReadPermission() throws Exception {
		Project project = project();
		Release newRelease = project.getReleaseRoot().create("New Release", user());
		testPermission(GraphPermission.READ_PERM, newRelease);
	}

	@Test
	@Override
	public void testDeletePermission() throws Exception {
		Project project = project();
		Release newRelease = project.getReleaseRoot().create("New Release", user());
		testPermission(GraphPermission.DELETE_PERM, newRelease);
	}

	@Test
	@Override
	public void testUpdatePermission() throws Exception {
		Project project = project();
		Release newRelease = project.getReleaseRoot().create("New Release", user());
		testPermission(GraphPermission.UPDATE_PERM, newRelease);
	}

	@Test
	@Override
	public void testCreatePermission() throws Exception {
		Project project = project();
		Release newRelease = project.getReleaseRoot().create("New Release", user());
		testPermission(GraphPermission.CREATE_PERM, newRelease);
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		Project project = project();
		Release release = project.getInitialRelease();

		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);

		ReleaseResponse releaseResponse = release.transformToRestSync(ac).toBlocking().first();
		assertThat(releaseResponse).isNotNull().hasName(release.getName()).hasUuid(release.getUuid()).isActive();
	}

	@Override
	public void testCreateDelete() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testCRUDPermissions() throws Exception {
		// TODO Auto-generated method stub

	}

}
