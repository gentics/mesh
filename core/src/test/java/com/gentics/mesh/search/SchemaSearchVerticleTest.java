package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.MeshAssert;

import io.vertx.core.Future;

public class SchemaSearchVerticleTest extends AbstractSearchVerticleTest implements BasicSearchCrudTestcases {

	@Autowired
	private SchemaVerticle schemaVerticle;

	@Override
	public List<AbstractSpringVerticle> getVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(schemaVerticle);
		return list;
	}

	@Test
	public void testSearchSchema() throws InterruptedException, JSONException {
		fullIndex();

		Future<SchemaListResponse> future = getClient().searchSchemas(getSimpleQuery("folder"), new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		SchemaListResponse response = future.result();
		assertEquals(1, response.getData().size());

		future = getClient().searchSchemas(getSimpleQuery("blub"), new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals(0, response.getData().size());

		future = getClient().searchSchemas(getSimpleTermQuery("name", "folder"), new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals(1, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentCreation() throws Exception {
		final String newName = "newschema";
		SchemaResponse schema = createSchema(newName);
		MeshAssert.assertElement(boot.schemaContainerRoot(), schema.getUuid(), true);
		Future<SchemaListResponse> future = getClient().searchSchemas(getSimpleTermQuery("name", newName), new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		SchemaListResponse response = future.result();
		assertEquals(1, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		final String schemaName = "newschemaname";
		SchemaResponse schema = createSchema(schemaName);

		Future<SchemaListResponse> future = getClient().searchSchemas(getSimpleTermQuery("name", schemaName),
				new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		assertEquals(1, future.result().getData().size());

		deleteSchema(schema.getUuid());
		future = getClient().searchSchemas(getSimpleTermQuery("name", schemaName), new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		final String schemaName = "newschemaname";
		SchemaResponse schema = createSchema(schemaName);

		String newSchemaName = "updatedschemaname";
		updateSchema(schema.getUuid(), newSchemaName);

		Future<SchemaListResponse> future = getClient().searchSchemas(getSimpleTermQuery("name", schemaName),
				new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getData().size());

		future = getClient().searchSchemas(getSimpleTermQuery("name", newSchemaName), new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		assertEquals(1, future.result().getData().size());
	}
}
