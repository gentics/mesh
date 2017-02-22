package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.UpdateMicroschemaChange;
import com.gentics.mesh.core.data.schema.impl.UpdateMicroschemaChangeImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class UpdateMicroschemaChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		try (NoTx noTx = db().noTx()) {
			UpdateMicroschemaChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateMicroschemaChangeImpl.class);
			change.setDescription("test");
			assertEquals("test", change.getDescription());
		}
	}

	@Test
	@Override
	public void testApply() {
		try (NoTx noTx = db().noTx()) {
			MicroschemaContainerVersion version = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			MicroschemaModel schema = new MicroschemaModel();

			UpdateMicroschemaChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateMicroschemaChangeImpl.class);
			change.setName("updated");
			version.setSchema(schema);
			version.setNextChange(change);

			Microschema updatedSchema = mutator.apply(version);
			assertEquals("updated", updatedSchema.getName());

			change = Database.getThreadLocalGraph().addFramedVertex(UpdateMicroschemaChangeImpl.class);
			change.setDescription("text");
			version.setNextChange(change);
			updatedSchema = mutator.apply(version);
			assertEquals("text", updatedSchema.getDescription());
		}
	}

	@Test
	@Override
	public void testUpdateFromRest() {
		try (NoTx noTx = db().noTx()) {
			SchemaChangeModel model = SchemaChangeModel.createUpdateMicroschemaChange();
			model.setProperty(SchemaChangeModel.NAME_KEY, "someName");

			UpdateMicroschemaChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateMicroschemaChangeImpl.class);
			change.updateFromRest(model);
			assertEquals("someName", change.getName());
		}
	}

	@Test
	@Override
	public void testGetMigrationScript() throws IOException {
		try (NoTx noTx = db().noTx()) {
			UpdateMicroschemaChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateMicroschemaChangeImpl.class);
			assertNull("Update microschema changes have a auto migation script.", change.getAutoMigrationScript());

			assertNull("Intitially no migration script should be set.", change.getMigrationScript());
			change.setCustomMigrationScript("test");
			assertEquals("The custom migration script was not changed.", "test", change.getMigrationScript());
		}
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		try (NoTx noTx = db().noTx()) {
			UpdateMicroschemaChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateMicroschemaChangeImpl.class);
			change.setCustomMigrationScript("testScript");
			change.setName("vcard");

			SchemaChangeModel model = change.transformToRest();
			assertEquals("vcard", model.getProperty(SchemaChangeModel.NAME_KEY));
			assertEquals("testScript", model.getMigrationScript());
		}
	}

}
