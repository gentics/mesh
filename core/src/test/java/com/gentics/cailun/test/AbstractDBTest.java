package com.gentics.cailun.test;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.service.LanguageService;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public abstract class AbstractDBTest {

	@Autowired
	protected LanguageService langService;

	protected Language english;
	protected Language german;

	@Before
	public void setup() {
		german = new Language("german");
		german.setName("german");
		german.setLanguageTag("de_DE");
		langService.save(german);

		english = new Language("english");
		english.setLanguageTag("en_US");
		langService.save(english);
	}

}
