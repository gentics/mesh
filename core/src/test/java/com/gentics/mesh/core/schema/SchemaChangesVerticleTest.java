package com.gentics.mesh.core.schema;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaMigrationResponse;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class SchemaChangesVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private SchemaVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testAddField() {
		SchemaContainer schema = schemaContainer("content");
		assertNull("The schema should not yet have any changes", schema.getNextChange());
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = new SchemaChangeModel(SchemaChangeOperation.ADDFIELD, "newField");
		listOfChanges.getChanges().add(change);

		Future<SchemaMigrationResponse> future = getClient().applyChangesToSchema(schema.getUuid(), listOfChanges);
		latchFor(future);
		assertSuccess(future);
		schema.reload();
		assertNotNull("The change should have been added to the schema.", schema.getNextChange());

	}
}
