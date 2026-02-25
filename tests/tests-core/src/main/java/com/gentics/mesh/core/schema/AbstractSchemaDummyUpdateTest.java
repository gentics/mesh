package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.runners.Parameterized.Parameter;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.context.AbstractMeshTest;

/**
 * Abstract base class for tests of "dummy" changes in schemas (involving empty and null values)
 * @param <T> type of the changed attribute
 */
public abstract class AbstractSchemaDummyUpdateTest<T> extends AbstractMeshTest {
	/**
	 * Test description (just for output)
	 */
	@Parameter(0)
	public String description;

	/**
	 * Test setter
	 */
	@Parameter(1)
	public BiConsumer<SchemaModel, T> setter;

	/**
	 * Do the update test. This will first create a schema and will set the original value, using the {@link #setter}.
	 * Then it will update the schema with the new value and assert that either the schema was changed and has version 2.0 or
	 * was not changed and still has version 1.0 (depending on parameter expectChange).
	 * @param originalValue original value
	 * @param newValue new value
	 * @param expectChange true when the schema is expected to be different with the new value
	 */
	protected final void doUpdateTest(T originalValue, T newValue, boolean expectChange) {
		SchemaResponse originalSchema = createOriginalSchema(originalValue);
		String schemaUuid = originalSchema.getUuid();

		call(() -> client().updateSchema(schemaUuid, prepareUpdate(schemaUuid, newValue)));

		SchemaResponse updatedSchema = load(schemaUuid);
		if (expectChange) {
			assertThat(updatedSchema).as("Updated schema").usingRecursiveComparison().ignoringFields("version").isNotEqualTo(originalSchema);
			assertThat(updatedSchema).as("Updated schema").hasFieldOrPropertyWithValue("version", "2.0");
		} else {
			assertThat(updatedSchema).as("Updated schema").usingRecursiveComparison().ignoringFields("version").isEqualTo(originalSchema);
			assertThat(updatedSchema).as("Updated schema").hasFieldOrPropertyWithValue("version", "1.0");
		}
	}

	/**
	 * Do the diff test. This will first create a schema and will set the original value, using the {@link #setter}.
	 * Then it will diff the original schema with a schema having the new value set.
	 * When parameter expectChange is true, it will assert that the list of changes is not empty.
	 * @param originalValue original value
	 * @param newValue new value
	 * @param expectChange true when the diff is expected to be not empty
	 */
	protected final void doDiffTest(T originalValue, T newValue, boolean expectChange) {
		SchemaResponse originalSchema = createOriginalSchema(originalValue);
		String schemaUuid = originalSchema.getUuid();

		SchemaChangesListModel diff = call(() -> client().diffSchema(schemaUuid, prepareUpdate(schemaUuid, newValue)));

		if (expectChange) {
			assertThat(diff.getChanges()).as("Changes").isNotEmpty();
		} else {
			assertThat(diff.getChanges()).as("Changes").isEmpty();
		}
	}

	/**
	 * Method that returns the list of test specific fields
	 * @return list of fields
	 */
	protected List<FieldSchema> fields() {
		// per default: no fields
		return Collections.emptyList();
	}

	/**
	 * Create the original schema
	 * @param initialValue initial value
	 * @return schema
	 */
	private SchemaResponse createOriginalSchema(T initialValue) {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("Test");
		request.setFields(fields());
		setter.accept(request, initialValue);

		SchemaResponse originalSchema = call(() -> client().createSchema(request));
		assertThat(originalSchema).as("Created schema").hasFieldOrPropertyWithValue("version", "1.0");
		return originalSchema;
	}

	/**
	 * Load the schema with given uuid
	 * @param uuid uuid
	 * @return schema
	 */
	private SchemaResponse load(String uuid) {
		return call(() -> client().findSchemaByUuid(uuid));
	}

	/**
	 * Prepare the update request for the schema with given uuid and the new value
	 * @param uuid uuid
	 * @param newValue new value
	 * @return schema update request
	 */
	private SchemaUpdateRequest prepareUpdate(String uuid, T newValue) {
		SchemaResponse schema = load(uuid);

		SchemaUpdateRequest update = JsonUtil.readValue(JsonUtil.toJson(schema), SchemaUpdateRequest.class);
		setter.accept(update, newValue);
		return update;
	}
}
