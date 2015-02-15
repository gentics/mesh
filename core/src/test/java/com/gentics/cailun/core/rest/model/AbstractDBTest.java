package com.gentics.cailun.core.rest.model;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.GlobalLanguageRepository;

public abstract class AbstractDBTest {

	@Autowired
	GlobalLanguageRepository languageRepository;

	protected Language english;
	protected Language german;

	@Before
	public void setup() {
		german = new Language("german");
		english = new Language("english");
		languageRepository.save(english);
		languageRepository.save(german);
	}

}
