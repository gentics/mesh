package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.data.service.ProjectService;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.BlueprintTransaction;
import com.gentics.mesh.util.InvalidArgumentException;

public class ProjectTest extends AbstractDBTest implements BasicObjectTestcases {

	@Autowired
	private ProjectService projectService;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	@Override
	public void testCreate() {
		ProjectRoot projectRoot = data().getMeshRoot().getProjectRoot();
		Project project = projectRoot.create("test");
		Project project2 = projectService.findByName(project.getName());
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
		assertNull(projectService.findByUUID(uuid));
	}

	@Test
	@Override
	public void testRootNode() {
		ProjectRoot projectRoot = data().getMeshRoot().getProjectRoot();
		int nProjectsBefore = projectRoot.getProjects().size();
		Project project = projectRoot.create("test1234556");
		int nProjectsAfter = projectRoot.getProjects().size();
		assertEquals(nProjectsBefore + 1, nProjectsAfter);
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		fail("not yet implemented");
	}

	@Test
	@Override
	public void testFindAll() {
		fail("not yet implemented");
	}

	@Test
	@Override
	public void testFindByName() {
		fail("not yet implemented");
	}

	@Test
	@Override
	public void testFindByUUID() {
		fail("not yet implemented");
	}


	@Test
	@Override
	public void testTransformation() {
		fail("not yet implemented");
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

	@Test
	@Override
	public void testPermissionsOnObject() {
		fail("not yet implemented");
	}

	@Override
	public void testRead() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testUpdate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testReadPermission() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testDeletePermission() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testUpdatePermission() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testCreatePermission() {
		// TODO Auto-generated method stub
		
	}

}
