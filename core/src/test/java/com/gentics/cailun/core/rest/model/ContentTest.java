package com.gentics.cailun.core.rest.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.repository.GenericContentRepository;
import com.gentics.cailun.test.Neo4jSpringTestConfiguration;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class ContentTest {

	@Autowired
	GenericContentRepository contentRepository;

	@Test
	public void testPageLinks() {
		GenericContent content = new GenericContent("test content");
		GenericContent content2 = new GenericContent("test content2");
		content.linkTo(content2);
		contentRepository.save(content);
		contentRepository.save(content2);
	}
}
