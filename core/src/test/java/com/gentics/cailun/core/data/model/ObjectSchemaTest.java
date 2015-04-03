package com.gentics.cailun.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.service.ObjectSchemaService;
import com.gentics.cailun.demo.DemoDataProvider;
import com.gentics.cailun.test.AbstractDBTest;

public class ObjectSchemaTest extends AbstractDBTest {

	@Autowired
	ObjectSchemaService objectSchemaService;

	@Before
	public void setup() {
		setupData();
	}

	@Test
	public void testFindByName() {
		assertNotNull(objectSchemaService.findByName(DemoDataProvider.PROJECT_NAME, "content"));
		assertNull(objectSchemaService.findByName(DemoDataProvider.PROJECT_NAME, "content1235"));
	}

	@Test
	public void testFindAllSchemasForProject() {
		Iterable<ObjectSchema> result = objectSchemaService.findAll(DemoDataProvider.PROJECT_NAME);

		int nSchemas = 0;
		for (ObjectSchema schema : result) {
			assertNotNull(schema);
			nSchemas++;
		}
		assertEquals("There should be exactly one object schema for the given project with the name {" + DemoDataProvider.PROJECT_NAME + "}", 2,
				nSchemas);
	}

	@Test
	public void deleteByObject() {
		ObjectSchema schema = data().getContentSchema();
		objectSchemaService.delete(schema);
		assertNull(objectSchemaService.findOne(schema.getId()));
	}

	@Test
	public void deleteByName() {
		ObjectSchema schema = data().getContentSchema();
		Project project = data().getProject();
		objectSchemaService.deleteByName(project.getName(), schema.getName());
		assertNull(objectSchemaService.findOne(schema.getId()));
	}

	@Test
	public void deleteByNameWithInvalidProjectName() {
		ObjectSchema schema = data().getContentSchema();
		objectSchemaService.deleteByName("bogus", schema.getName());
		assertNotNull(objectSchemaService.findOne(schema.getId()));
	}

	@Test
	public void deleteByUUID() {
		ObjectSchema schema = data().getContentSchema();
		objectSchemaService.deleteByUUID(schema.getUuid());
	}

	@Test
	public void deleteWithNoPermission() {

	}

}
