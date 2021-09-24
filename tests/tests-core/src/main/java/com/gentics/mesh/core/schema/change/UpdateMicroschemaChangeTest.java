package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.UpdateMicroschemaChange;
import com.gentics.mesh.core.data.schema.impl.UpdateMicroschemaChangeImpl;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.test.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = FULL, startServer = false)
public class UpdateMicroschemaChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		try (Tx tx = tx()) {
			UpdateMicroschemaChange change = ((GraphDBTx) tx).getGraph().addFramedVertex(UpdateMicroschemaChangeImpl.class);
			change.setDescription("test");
			assertEquals("test", change.getDescription());
		}
	}

	@Test
	@Override
	public void testApply() {
		try (Tx tx = tx()) {
			MicroschemaVersion version = ((GraphDBTx) tx).getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			MicroschemaModelImpl schema = new MicroschemaModelImpl();

			UpdateMicroschemaChange change = ((GraphDBTx) tx).getGraph().addFramedVertex(UpdateMicroschemaChangeImpl.class);
			change.setName("updated");
			change.setIndexOptions(new JsonObject().put("key", "value"));
			version.setSchema(schema);
			version.setNextChange(change);

			MicroschemaModel updatedSchema = mutator.apply(version);
			assertEquals("updated", updatedSchema.getName());
			assertEquals("value", updatedSchema.getElasticsearch().getString("key"));

			change = ((GraphDBTx) tx).getGraph().addFramedVertex(UpdateMicroschemaChangeImpl.class);
			change.setDescription("text");
			version.setNextChange(change);
			updatedSchema = mutator.apply(version);
			assertEquals("text", updatedSchema.getDescription());
		}
	}

	@Test
	@Override
	public void testUpdateFromRest() {
		try (Tx tx = tx()) {
			SchemaChangeModel model = SchemaChangeModel.createUpdateMicroschemaChange();
			model.setProperty(SchemaChangeModel.NAME_KEY, "someName");

			UpdateMicroschemaChange change = ((GraphDBTx) tx).getGraph().addFramedVertex(UpdateMicroschemaChangeImpl.class);
			change.updateFromRest(model);
			assertEquals("someName", change.getName());
		}
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		try (Tx tx = tx()) {
			UpdateMicroschemaChange change = ((GraphDBTx) tx).getGraph().addFramedVertex(UpdateMicroschemaChangeImpl.class);
			change.setName("vcard");

			SchemaChangeModel model = change.transformToRest();
			assertEquals("vcard", model.getProperty(SchemaChangeModel.NAME_KEY));
		}
	}

}
