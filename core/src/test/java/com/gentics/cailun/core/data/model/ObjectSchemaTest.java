package com.gentics.cailun.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.service.ObjectSchemaService;
import com.gentics.cailun.core.repository.ObjectSchemaRepository;
import com.gentics.cailun.demo.DemoDataProvider;
import com.gentics.cailun.test.AbstractDBTest;

@Transactional
public class ObjectSchemaTest extends AbstractDBTest {

	@Autowired
	private ObjectSchemaService objectSchemaService;

	@Autowired
	private ObjectSchemaRepository objectSchemaRepository;

	@Before
	public void setup() throws Exception {
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
	public void testDeleteByObject() {
		ObjectSchema schema = data().getContentSchema();
		objectSchemaService.delete(schema);
		assertNull(objectSchemaService.findOne(schema.getId()));
	}

	@Test
	public void testDeleteByName() {
		ObjectSchema schema = data().getContentSchema();
		Project project = data().getProject();
		objectSchemaService.deleteByName(project.getName(), schema.getName());
		assertNull(objectSchemaService.findOne(schema.getId()));
	}

	@Test
	public void testDeleteByNameWithInvalidProjectName() {
		ObjectSchema schema = data().getContentSchema();
		objectSchemaService.deleteByName("bogus", schema.getName());
		assertNotNull(objectSchemaService.findOne(schema.getId()));
	}

	@Test
	public void testDeleteByUUID() {
		ObjectSchema schema = data().getContentSchema();
		objectSchemaService.deleteByUUID(schema.getUuid());
	}

	@Test
	public void testDeleteWithNoPermission() {
		fail("Not yet implemented");
	}

	@Test
	public void testObjectSchemaRootNode() {
		int nSchemasBefore = objectSchemaRepository.findRoot().getSchemas().size();

		ObjectSchema schema = new ObjectSchema("test1235");
		objectSchemaRepository.save(schema);

		int nSchemasAfter = objectSchemaRepository.findRoot().getSchemas().size();
		assertEquals(nSchemasBefore + 1, nSchemasAfter);
	}
}
