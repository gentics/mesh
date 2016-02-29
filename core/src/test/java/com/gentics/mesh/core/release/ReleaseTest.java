package com.gentics.mesh.core.release;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.root.ReleaseRoot;
import com.gentics.mesh.test.AbstractBasicObjectTest;

public class ReleaseTest extends AbstractBasicObjectTest {

	@Override
	public void testTransformToReference() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testFindAllVisible() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testFindAll() throws Exception {
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub

	}

	@Override
	public void testCreate() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testDelete() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testUpdate() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testReadPermission() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testDeletePermission() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testUpdatePermission() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testCreatePermission() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void testTransformation() throws Exception {
		// TODO Auto-generated method stub

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
