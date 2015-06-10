package com.gentics.mesh.core.data.model;

import static com.gentics.mesh.util.TinkerpopUtils.count;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.tinkerpop.Schema;
import com.gentics.mesh.core.data.service.SchemaService;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.test.AbstractDBTest;

public class ObjectSchemaTest extends AbstractDBTest {

	@Autowired
	private SchemaService objectSchemaService;

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
	public void testDeleteByObject() {
		Schema schema = data().getSchema("content");
		objectSchemaService.delete(schema);
		assertNull(objectSchemaService.findOne(schema.getId()));
	}

	@Test
	public void testDeleteByUUID() {
		Schema schema = data().getSchema("content");
//		try (Transaction tx = graphDb.beginTx()) {

			objectSchemaService.deleteByUUID(schema.getUuid());
//			tx.success();
//		}
		assertNull(objectSchemaService.findOne(schema.getId()));
	}

	// @Test
	// public void testDeleteWithNoPermission() {
	// UserInfo info = data().getUserInfo();
	// ObjectSchema schema = data().getContentSchema();
	// try (Transaction tx = graphDb.beginTx()) {
	// roleService.revokePermission(info.getRole(), schema, PermissionType.DELETE);
	// objectSchemaService.deleteByUUID(schema.getUuid());
	// tx.success();
	// }
	// assertNotNull(objectSchemaService.findOne(schema.getId()));
	// }

	@Test
	public void testObjectSchemaRootNode() {
		int nSchemasBefore = count(objectSchemaService.findRoot().getSchemas());

		Schema schema = objectSchemaService.create("test1235");
//		try (Transaction tx = graphDb.beginTx()) {
//			tx.success();
//		}

		int nSchemasAfter = count(objectSchemaService.findRoot().getSchemas());
		assertEquals(nSchemasBefore + 1, nSchemasAfter);
	}
}
