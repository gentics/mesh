package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.impl.RemoveFieldChangeImpl;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class RemoveFieldChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		try (Tx tx = tx()) {
			RemoveFieldChange change = ((GraphDBTx) tx).getGraph().addFramedVertex(RemoveFieldChangeImpl.class);

			change.setFieldName("someField");
			assertEquals("someField", change.getFieldName());
		}
	}

	@Test
	@Override
	public void testApply() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createSchemaVersion(tx);

			// 1. Create schema with field
			SchemaVersionModel schema = new SchemaModelImpl();
			schema.addField(FieldUtil.createStringFieldSchema("test"));

			// 2. Create remove field change
			RemoveFieldChange change = ((GraphDBTx) tx).getGraph().addFramedVertex(RemoveFieldChangeImpl.class);
			change.setFieldName("test");

			version.setNextChange(change);
			version.setSchema(schema);

			// 3. Apply the change
			FieldSchemaContainer updatedSchema = mutator.apply(version);

			assertThat(updatedSchema).hasNoField("test");
		}
	}

	@Test
	@Override
	public void testUpdateFromRest() throws IOException {
		try (Tx tx = tx()) {
			SchemaChangeModel model = new SchemaChangeModel();
			model.setProperty(SchemaChangeModel.FIELD_NAME_KEY, "someField");
			RemoveFieldChange change = ((GraphDBTx) tx).getGraph().addFramedVertex(RemoveFieldChangeImpl.class);
			change.updateFromRest(model);
			assertEquals("someField", change.getFieldName());
		}
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		try (Tx tx = tx()) {
			RemoveFieldChange change = ((GraphDBTx) tx).getGraph().addFramedVertex(RemoveFieldChangeImpl.class);
			assertEquals(RemoveFieldChange.OPERATION, change.transformToRest().getOperation());
			change.setFieldName("test2");

			SchemaChangeModel model = change.transformToRest();
			assertEquals("test2", model.getProperty(SchemaChangeModel.FIELD_NAME_KEY));
		}
	}

}
