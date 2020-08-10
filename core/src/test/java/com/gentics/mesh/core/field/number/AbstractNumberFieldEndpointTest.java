package com.gentics.mesh.core.field.number;

import java.io.IOException;

import org.junit.Before;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaUpdateModel;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;

/**
 * Created by sebastian on 04.12.17.
 */
public abstract class AbstractNumberFieldEndpointTest extends AbstractFieldEndpointTest {

	protected static final String FIELD_NAME = "numberField";

	@Before
	public void updateSchema() throws IOException {
		try (Tx tx = tx()) {
			SchemaUpdateModel schema = schemaContainer("folder").getLatestVersion().getSchema();
			NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
			numberFieldSchema.setName(FIELD_NAME);
			// numberFieldSchema.setMin(10);
			// numberFieldSchema.setMax(1000);
			schema.addField(numberFieldSchema);
			schemaContainer("folder").getLatestVersion().setSchema(schema);
			tx.success();
		}
	}
}
