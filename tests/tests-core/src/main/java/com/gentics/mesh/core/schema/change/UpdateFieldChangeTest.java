package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.HibUpdateFieldChange;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.test.MeshTestSetting;

/**
 * Test {@link UpdateFieldChangeImpl} methods.
 */
@MeshTestSetting(testSize = PROJECT, startServer = false)
public class UpdateFieldChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			HibUpdateFieldChange change = createChange(schemaDao(tx), version, UPDATEFIELD);
			change.setLabel("testLabel");
			assertEquals("testLabel", change.getLabel());
		}
	}

	@Test
	@Override
	public void testApply() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));

			SchemaModelImpl schema = new SchemaModelImpl("test");
			schema.addField(FieldUtil.createStringFieldSchema("name"));

			HibUpdateFieldChange change = createChange(schemaDao(tx), version, UPDATEFIELD);
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
			HibSchemaVersion version = createVersion(schemaDao(tx));

			SchemaChangeModel model = new SchemaChangeModel(UPDATEFIELD, "someField");
			HibUpdateFieldChange change = (HibUpdateFieldChange) schemaDao(tx).createChange(version, model);
			assertEquals("someField", change.getFieldName());
		}
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			HibUpdateFieldChange change = createChange(schemaDao(tx), version, UPDATEFIELD);
			change.setFieldName("fieldName");

			SchemaChangeModel model = change.transformToRest();
			assertEquals("fieldName", model.getProperty(SchemaChangeModel.FIELD_NAME_KEY));
			assertEquals(HibUpdateFieldChange.OPERATION, model.getOperation());
		}
	}
}
