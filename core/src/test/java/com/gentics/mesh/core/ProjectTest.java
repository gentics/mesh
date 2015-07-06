package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.BlueprintTransaction;
import com.gentics.mesh.util.InvalidArgumentException;

public class ProjectTest extends AbstractBasicObjectTest {

	private ProjectRoot projectRoot;

	@Before
	public void setup() throws Exception {
		super.setup();
		projectRoot = boot.projectRoot();
	}

	@Test
	@Override
	public void testCreate() {
		ProjectRoot projectRoot = data().getMeshRoot().getProjectRoot();
		Project project = projectRoot.create("test");
		Project project2 = projectRoot.findByName(project.getName());
		assertNotNull(project2);
		assertEquals("test", project2.getName());
		assertEquals(project.getUuid(), project2.getUuid());
	}

	@Test
	@Override
	public void testDelete() {
		String uuid = data().getProject().getUuid();
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			Project project = data().getProject();
			project.delete();
			tx.success();
		}
		assertNull(projectRoot.findByUUID(uuid));
		// TODO assert on tag families of the project
	}

	@Test
	@Override
	public void testRootNode() {
		ProjectRoot projectRoot = data().getMeshRoot().getProjectRoot();
		int nProjectsBefore = projectRoot.findAll().size();
		assertNotNull(projectRoot.create("test1234556"));
		int nProjectsAfter = projectRoot.findAll().size();
		assertEquals(nProjectsBefore + 1, nProjectsAfter);
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		Page<? extends Project> page = projectRoot.findAll(getRequestUser(), new PagingInfo(1, 25));
		assertNotNull(page);
	}

	@Test
	@Override
	public void testFindAll() {
		List<? extends Project> projects = projectRoot.findAll();
		assertNotNull(projects);
		assertEquals(1, projects.size());
	}

	@Test
	@Override
	public void testFindByName() {
		assertNull(projectRoot.findByName("bogus"));
		assertNotNull(projectRoot.findByName("dummy"));
	}

	@Test
	@Override
	public void testFindByUUID() {
		assertNotNull(projectRoot.findByUUID(data().getProject().getUuid()));
		assertNull(projectRoot.findByUUID("bogus"));
	}

	@Test
	@Override
	public void testTransformation() {
		Project project = data().getProject();
		ProjectResponse response = project.transformToRest(getRequestUser());
		assertNotNull(response);
		assertEquals(project.getName(), response.getName());
		assertEquals(project.getUuid(), response.getUuid());
	}

	@Test
	@Override
	public void testCreateDelete() {
		fail("not yet implemented");
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		fail("not yet implemented");
	}

	@Override
	public void testRead() {
		fail("not yet implemented");
	}

	@Override
	public void testUpdate() {
		fail("not yet implemented");
	}

	@Override
	public void testReadPermission() {
		fail("not yet implemented");
	}

	@Override
	public void testDeletePermission() {
		fail("not yet implemented");
	}

	@Override
	public void testUpdatePermission() {
		fail("not yet implemented");
	}

	@Override
	public void testCreatePermission() {
		fail("not yet implemented");
	}

}
