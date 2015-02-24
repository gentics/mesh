package com.gentics.cailun.test;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.service.LanguageService;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public abstract class AbstractDBTest {

	@Autowired
	protected LanguageService languageService;

	@Autowired
	private DummyDataProvider dataProvider;

	protected Language german;

	protected Language english;

	@Before
	public void setup() {
		dataProvider.setup();
		english = languageService.findByName("english");
		german = languageService.findByName("german");
	}
}
