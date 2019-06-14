package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.UpdateFieldChange;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateFieldChangeImpl;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.test.context.MeshTestSetting;

/**
 * Test {@link UpdateFieldChangeImpl} methods.
 */
@MeshTestSetting(testSize = PROJECT, startServer = false)
public class UpdateFieldChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		try (Tx tx = tx()) {
			UpdateFieldChange change = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			change.setLabel("testLabel");
			assertEquals("testLabel", change.getLabel());
		}
	}

	@Test
	@Override
	public void testApply() {
		try (Tx tx = tx()) {
			SchemaContainerVersion version = tx.getGraph()
					.addFramedVertex(SchemaContainerVersionImpl.class);

			SchemaModelImpl schema = new SchemaModelImpl("test");
			schema.addField(FieldUtil.createStringFieldSchema("name"));

			UpdateFieldChange change = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			change.setFieldName("name");
			change.setLabel("updated");
			version.setSchema(schema);
			version.setNextChange(change);

			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertEquals("The field label was not updated by the mutator.", "updated",
					updatedSchema.getField("name").getLabel());
		}
	}

	@Test
	@Override
	public void testUpdateFromRest() {
		try (Tx tx = tx()) {
			SchemaChangeModel model = new SchemaChangeModel(UPDATEFIELD, "someField");
			UpdateFieldChange change = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			change.updateFromRest(model);
			assertEquals("someField", change.getFieldName());
		}
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		try (Tx tx = tx()) {
			UpdateFieldChange change = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			change.setFieldName("fieldName");

			SchemaChangeModel model = change.transformToRest();
			assertEquals("fieldName", model.getProperty(SchemaChangeModel.FIELD_NAME_KEY));
			assertEquals(UpdateFieldChange.OPERATION, model.getOperation());
		}
	}

}
