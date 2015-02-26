package com.gentics.cailun.core.rest.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.service.ObjectSchemaService;
import com.gentics.cailun.test.AbstractDBTest;
import com.gentics.cailun.test.DummyDataProvider;

public class ObjectSchemaTest extends AbstractDBTest {

	@Autowired
	ObjectSchemaService objectSchemaService;

	@Before
	public void setup() {
		setupData();
	}

	@Test
	public void testFindByName() {
		assertNotNull(objectSchemaService.findByName(DummyDataProvider.PROJECT_NAME, "content"));
		assertNull(objectSchemaService.findByName(DummyDataProvider.PROJECT_NAME, "content1235"));
	}

}
