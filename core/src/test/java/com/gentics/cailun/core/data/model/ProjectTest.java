package com.gentics.cailun.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.test.AbstractDBTest;

public class ProjectTest extends AbstractDBTest {

	@Autowired
	private ProjectService projectService;

	@Before
	public void setup() {
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
		Project project = getDataProvider().getProject();
		projectService.delete(project);
		assertNull(projectService.findOne(project.getId()));
		assertNull(projectService.findByUUID(project.getUuid()));
	}
}
