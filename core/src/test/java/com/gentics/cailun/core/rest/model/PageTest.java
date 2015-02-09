package com.gentics.cailun.core.rest.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.repository.PageRepository;
import com.gentics.cailun.test.Neo4jSpringTestConfiguration;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class PageTest {

	@Autowired
	PageRepository pageRepository;

	@Test
	public void testPageLinks() {
		Page page = new Page("test page");
		Page page2 = new Page("test page2");
		page.linkTo(page2);
		pageRepository.save(page);
		pageRepository.save(page2);
	}
}
