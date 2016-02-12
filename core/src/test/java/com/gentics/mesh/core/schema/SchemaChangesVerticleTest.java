package com.gentics.mesh.core.schema;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;

public class SchemaChangesVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private SchemaVerticle verticle;

	@Autowired
	private NodeMigrationVerticle nodeMigrationVerticle;

	@Override
	@Before
	public void setupVerticleTest() throws Exception {
		super.setupVerticleTest();
		DeploymentOptions options = new DeploymentOptions();
		options.setWorker(true);
		vertx.deployVerticle(nodeMigrationVerticle, options);
	}

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testAddField() {
		SchemaContainer container = schemaContainer("content");
		assertNull("The schema should not yet have any changes", container.getNextChange());
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createAddChange("newField", "html");
		listOfChanges.getChanges().add(change);

		Future<GenericMessageResponse> future = getClient().applyChangesToSchema(container.getUuid(), listOfChanges);
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("migration_invoked", future, "content");
		container.reload();
		assertNotNull("The change should have been added to the schema.", container.getNextChange());
		assertNotNull("The container should now have a new version", container.getNextVersion());

//		// Assert that migration worked
//		Node node = content();
//		node.reload();
//		assertTrue("The version of the original schema and the schema that is now linked to the node should be different.",
//				container.getVersion() != node.getSchemaContainer().getVersion());

	}
}
