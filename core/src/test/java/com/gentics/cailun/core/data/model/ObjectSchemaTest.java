package com.gentics.cailun.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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

	@Test
	public void testFindAllSchemasForProject() {
		Iterable<ObjectSchema> result = objectSchemaService.findAll(DummyDataProvider.PROJECT_NAME);

		int nSchemas = 0;
		for (ObjectSchema schema : result) {
			assertNotNull(schema);
			nSchemas++;
		}
		assertEquals("There should be exactly one object schema for the given project with the name {" + DummyDataProvider.PROJECT_NAME + "}", 2,
				nSchemas);
	}

	@Test
	public void deleteByObject() {
		ObjectSchema schema = getData().getContentSchema();
		objectSchemaService.delete(schema);
		assertNull(objectSchemaService.findOne(schema.getId()));
	}

	@Test
	public void deleteByName() {
		ObjectSchema schema = getData().getContentSchema();
		Project project = getData().getProject();
		objectSchemaService.deleteByName(project.getName(), schema.getName());
		assertNull(objectSchemaService.findOne(schema.getId()));
	}

	@Test
	public void deleteByNameWithInvalidProjectName() {
		ObjectSchema schema = getData().getContentSchema();
		objectSchemaService.deleteByName("bogus", schema.getName());
		assertNotNull(objectSchemaService.findOne(schema.getId()));
	}

	@Test
	public void deleteByUUID() {
		ObjectSchema schema = getData().getContentSchema();
		objectSchemaService.deleteByUUID(schema.getUuid());
	}

	@Test
	public void deleteWithNoPermission() {

	}

}
