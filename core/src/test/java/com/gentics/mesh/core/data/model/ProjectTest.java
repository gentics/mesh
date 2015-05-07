package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.Project;
import com.gentics.mesh.core.data.service.ProjectService;
import com.gentics.mesh.core.repository.ProjectRepository;
import com.gentics.mesh.test.AbstractDBTest;

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
	public void testCreation() {
		Project project = new Project("test");
		projectService.save(project);
		project = projectService.findOne(project.getId());
		assertNotNull(project);
		assertEquals("test", project.getName());
	}

	@Test
	public void testDeletion() {
		Project project = data().getProject();
		projectService.delete(project);
		assertNull(projectService.findOne(project.getId()));
		assertNull(projectService.findByUUID(project.getUuid()));
	}

	@Test
	public void testProjectRootNode() {
		int nProjectsBefore = projectRepository.findRoot().getProjects().size();

		try (Transaction tx = graphDb.beginTx()) {
			Project project = new Project("test1234556");
			projectRepository.save(project);
			tx.success();
		}
		int nProjectsAfter = projectRepository.findRoot().getProjects().size();
		assertEquals(nProjectsBefore + 1, nProjectsAfter);
	}
}
