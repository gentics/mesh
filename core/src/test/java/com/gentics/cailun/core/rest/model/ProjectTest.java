package com.gentics.cailun.core.rest.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.GlobalProjectRepository;
import com.gentics.cailun.core.rest.model.Project;
import com.gentics.cailun.test.AbstractDBTest;

public class ProjectTest extends AbstractDBTest {

	@Autowired
	GlobalProjectRepository projectRepository;

	@Test
	public void testCreation() {

		Project project = new Project("test");
		projectRepository.save(project);
		project = projectRepository.findOne(project.getId());
		assertNotNull(project);
		assertEquals("test", project.getName());
	}

}
