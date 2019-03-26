package com.gentics.mesh.core.field.date;

import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.util.DateUtils.fromISO8601;
import static com.gentics.mesh.util.DateUtils.toISO8601;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(testSize = FULL, startServer = true)
public class DateFieldEndpointTest extends AbstractFieldEndpointTest {
	private static final String FIELD_NAME = "dateField";

	@Before
	public void updateSchema() throws IOException {
		try (Tx tx = tx()) {
			SchemaModel schema = schemaContainer("folder").getLatestVersion().getSchema();
			DateFieldSchema dateFieldSchema = new DateFieldSchemaImpl();
			dateFieldSchema.setName(FIELD_NAME);
			dateFieldSchema.setLabel("Some label");
			schema.addField(dateFieldSchema);
			schemaContainer("folder").getLatestVersion().setSchema(schema);
			tx.success();
		}
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNode(FIELD_NAME, (Field) null);
			DateFieldImpl field = response.getFields().getDateField(FIELD_NAME);
			assertNull(field);
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		Node node = folder("2015");
		for (int i = 0; i < 20; i++) {
			Long nowEpoch = fromISO8601(toISO8601(System.currentTimeMillis() + (i * 10000)));
			NodeGraphFieldContainer container;
			Long oldValue;

			try (Tx tx = tx()) {
				container = node.getGraphFieldContainer("en");
				oldValue = getDateValue(container, FIELD_NAME);
			}

			NodeResponse response = updateNode(FIELD_NAME, new DateFieldImpl().setDate(toISO8601(nowEpoch)));
			DateFieldImpl field = response.getFields().getDateField(FIELD_NAME);
			assertEquals("The timestamp did not match up.", toISO8601(nowEpoch), field.getDate());

			try (Tx tx = tx()) {
				assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion());
				assertEquals("Check old value", oldValue, getDateValue(container, FIELD_NAME));
			}
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		try (Tx tx = tx()) {
			Long nowEpoch = fromISO8601(toISO8601(System.currentTimeMillis()));
			NodeResponse firstResponse = updateNode(FIELD_NAME, new DateFieldImpl().setDate(toISO8601(nowEpoch)));
			String oldVersion = firstResponse.getVersion();

			NodeResponse secondResponse = updateNode(FIELD_NAME, new DateFieldImpl().setDate(toISO8601(nowEpoch)));
			assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldVersion);
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		Node node = folder("2015");
		NodeResponse secondResponse;

		Long nowEpoch = fromISO8601(toISO8601(System.currentTimeMillis()));
		NodeResponse firstResponse = updateNode(FIELD_NAME, new DateFieldImpl().setDate(toISO8601(nowEpoch)));
		String oldVersion = firstResponse.getVersion();

		secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getDateField(FIELD_NAME)).as("Field Value").isNull();
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);

		// Assert that the old version was not modified
		try (Tx tx = tx()) {
			NodeGraphFieldContainer latest = node.getLatestDraftFieldContainer(english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getDate(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getDate(FIELD_NAME)).isNotNull();
			Long oldValue = latest.getPreviousVersion().getDate(FIELD_NAME).getDate();
			assertThat(oldValue).isEqualTo(nowEpoch);
		}

		try (Tx tx = tx()) {
			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
				secondResponse.getVersion());
		}
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		Long nowEpoch = fromISO8601(toISO8601(System.currentTimeMillis()));
		NodeResponse firstResponse = updateNode(FIELD_NAME, new DateFieldImpl().setDate(toISO8601(nowEpoch)));
		String oldVersion = firstResponse.getVersion();

		// Date fields can't be set to empty.
		NodeResponse secondResponse = updateNode(FIELD_NAME, new DateFieldImpl());
		assertThat(secondResponse.getFields().getDateField(FIELD_NAME)).as("Field Value").isNull();
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);
	}

	@Test
	public void testDateFormat() {
		String invalidDate = "2017-08-21T10:46:26+0200";
		updateNodeFailure(FIELD_NAME, new DateFieldImpl().setDate(invalidDate), BAD_REQUEST, "error_date_format_invalid", invalidDate);
	}

	/**
	 * Get the date value
	 *
	 * @param container
	 *            container
	 * @param fieldName
	 *            field name
	 * @return date value (may be null)
	 */
	protected Long getDateValue(NodeGraphFieldContainer container, String fieldName) {
		DateGraphField field = container.getDate(fieldName);
		return field != null ? field.getDate() : null;
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (Tx tx = tx()) {
			Long nowEpoch = fromISO8601(toISO8601(System.currentTimeMillis()));
			NodeResponse response = createNode(FIELD_NAME, new DateFieldImpl().setDate(toISO8601(nowEpoch)));
			DateField field = response.getFields().getDateField(FIELD_NAME);
			assertEquals(toISO8601(nowEpoch), field.getDate());
		}
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		Node node = folder("2015");
		Long nowEpoch;
		try (Tx tx = tx()) {
			nowEpoch = fromISO8601(toISO8601(System.currentTimeMillis()));

			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			container.createDate(FIELD_NAME).setDate(nowEpoch);
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeResponse response = readNode(node);
			DateField deserializedDateField = response.getFields().getDateField(FIELD_NAME);
			assertNotNull(deserializedDateField);
			assertEquals(toISO8601(nowEpoch), deserializedDateField.getDate());
		}
	}

	@Override
	public NodeResponse createNodeWithField() {
		Long nowEpoch = fromISO8601(toISO8601(System.currentTimeMillis()));
		return createNode(FIELD_NAME, new DateFieldImpl().setDate(toISO8601(nowEpoch)));
	}
}
