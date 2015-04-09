package com.gentics.cailun.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.repository.ProjectRepository;
import com.gentics.cailun.test.AbstractDBTest;

public class ProjectTest extends AbstractDBTest {

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectService projectService;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	@Transactional
	public void testCreation() {
		Project project = new Project("test");
		projectService.save(project);
		project = projectService.findOne(project.getId());
		assertNotNull(project);
		assertEquals("test", project.getName());
	}

	@Test
	@Transactional
	public void testDeletion() {
		Project project = data().getProject();
		projectService.delete(project);
		assertNull(projectService.findOne(project.getId()));
		assertNull(projectService.findByUUID(project.getUuid()));
	}

	@Test
	@Transactional
	public void testProjectRootNode() {
		int nProjectsBefore = projectRepository.findRoot().getProjects().size();
		Project project = new Project("test1234556");
		projectRepository.save(project);
		int nProjectsAfter = projectRepository.findRoot().getProjects().size();
		assertEquals(nProjectsBefore + 1, nProjectsAfter);
	}
}
