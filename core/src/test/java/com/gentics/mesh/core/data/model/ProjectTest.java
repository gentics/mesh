package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.root.ProjectRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.service.ProjectService;
import com.gentics.mesh.test.AbstractDBTest;

public class ProjectTest extends AbstractDBTest {

	@Autowired
	private ProjectService projectService;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testCreation() {
		ProjectRoot projectRoot = data().getMeshRoot().getProjectRoot();
		Project project = projectRoot.create("test");
		Project project2 = projectService.findByName(project.getName());
		assertNotNull(project2);
		assertEquals("test", project2.getName());
		assertEquals(project.getUuid(), project2.getUuid());
	}

	@Test
	public void testDeletion() {
		Project project = data().getProject();
		project.delete();
		// assertNull(projectService.findOne(project.getId()));
		assertNull(projectService.findByUUID(project.getUuid()));
	}

	@Test
	public void testProjectRootNode() {
		ProjectRoot projectRoot = data().getMeshRoot().getProjectRoot();
		int nProjectsBefore = projectRoot.getProjects().size();
		Project project = projectRoot.create("test1234556");
		int nProjectsAfter = projectRoot.getProjects().size();
		assertEquals(nProjectsBefore + 1, nProjectsAfter);
	}
}
