package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.REMOVEFIELD;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.HibRemoveFieldChange;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class RemoveFieldChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			HibRemoveFieldChange change = createChange(schemaDao(tx), version, REMOVEFIELD);
			
			change.setFieldName("someField");
			assertEquals("someField", change.getFieldName());
		}
	}

	@Test
	@Override
	public void testApply() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			
			// 1. Create schema with field
			SchemaVersionModel schema = new SchemaModelImpl();
			schema.addField(FieldUtil.createStringFieldSchema("test"));

			// 2. Create remove field change
			HibRemoveFieldChange change = createChange(schemaDao(tx), version, REMOVEFIELD);
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
			SchemaChangeModel model = new SchemaChangeModel(REMOVEFIELD, "someField");
			HibSchemaVersion version = createVersion(schemaDao(tx));
			HibRemoveFieldChange change = (HibRemoveFieldChange) schemaDao(tx).createChange(version, model);
			assertEquals("someField", change.getFieldName());
		}
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			HibRemoveFieldChange change = createChange(schemaDao(tx), version, REMOVEFIELD);
			assertEquals(HibRemoveFieldChange.OPERATION, change.transformToRest().getOperation());
			change.setFieldName("test2");

			SchemaChangeModel model = change.transformToRest();
			assertEquals("test2", model.getProperty(SchemaChangeModel.FIELD_NAME_KEY));
		}
	}

}
