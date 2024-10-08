package com.gentics.mesh.core.field.number;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class NumberFieldEndpointTest extends AbstractNumberFieldEndpointTest {

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(FIELD_NAME, (Field) null);
		NumberFieldImpl field = response.getFields().getNumberField(FIELD_NAME);
		assertNull("The field should be null since we did not specify a field when executing the creation call", field);
	}

	@Test
	public void testCreateNodeWithWrongFieldType() {
		String fieldKey = FIELD_NAME;
		StringField field = new StringFieldImpl().setString("text");

		HibNode node = folder("2015");
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setParentNodeUuid(node.getUuid());
		nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.getFields().put(fieldKey, field);

		call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParametersImpl().setLanguages("en")), BAD_REQUEST,
			"field_number_error_invalid_type", fieldKey, "text");
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		disableAutoPurge();

		HibNode node = folder("2015");
		for (int i = 0; i < 20; i++) {
			HibNodeFieldContainer container = tx(tx -> { return tx.contentDao().getFieldContainer(node, "en"); });
			Number oldValue;
			Number newValue;
			try (Tx tx = tx()) {
				oldValue = getNumberValue(container, FIELD_NAME);
				newValue = Integer.valueOf(i + 42);
			}

			NodeResponse response = updateNode(FIELD_NAME, new NumberFieldImpl().setNumber(newValue));
			NumberFieldImpl field = response.getFields().getNumberField(FIELD_NAME);
			assertEquals(newValue.doubleValue(), field.getNumber().doubleValue(), 0.0);

			try (Tx tx = tx()) {
				assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion());
				assertEquals("Check old value", oldValue, getNumberValue(container, FIELD_NAME));
			}
		}
	}

	@Test
	public void testPreciseFloatValue() {
		double value = 0.123456f;
		NodeResponse firstResponse = updateNode(FIELD_NAME, new NumberFieldImpl().setNumber(value));
		Number storedNumber = firstResponse.getFields().getNumberField(FIELD_NAME).getNumber();
		assertEquals(Double.valueOf(value), storedNumber.doubleValue(), 0.0000001);
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		NodeResponse firstResponse = updateNode(FIELD_NAME, new NumberFieldImpl().setNumber(42));
		String oldNumber = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, new NumberFieldImpl().setNumber(42));
		assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldNumber);
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		disableAutoPurge();

		NodeResponse firstResponse = tx(() -> updateNode(FIELD_NAME, new NumberFieldImpl().setNumber(42)));
		String oldVersion = firstResponse.getVersion();

		// Field should be deleted
		NodeResponse secondResponse = tx(() -> updateNode(FIELD_NAME, null));
		assertThat(secondResponse.getFields().getNumberField(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			// Assert that the old version was not modified
			HibNode node = folder("2015");
			HibNodeFieldContainer latest = contentDao.getLatestDraftFieldContainer(node, english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getNumber(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getNumber(FIELD_NAME)).isNotNull();
			Number oldValue = latest.getPreviousVersion().getNumber(FIELD_NAME).getNumber();
			assertThat(oldValue.doubleValue()).isEqualTo(Double.valueOf(42));
		}
		NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
			secondResponse.getVersion());

		// Update again to restore a value
		updateNode(FIELD_NAME, new NumberFieldImpl().setNumber(42));
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		// Number fields can't be set to empty - The rest model will generate a null field for the update request json. Thus the field will be deleted.
		NodeResponse firstResponse = updateNode(FIELD_NAME, new NumberFieldImpl());
		assertThat(firstResponse.getFields().getNumberField(FIELD_NAME)).as("Updated Field").isNull();
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNode(FIELD_NAME, new NumberFieldImpl().setNumber(1.214353));
		NumberFieldImpl numberField = response.getFields().getNumberField(FIELD_NAME);
		assertEquals(Double.valueOf(1.214353), numberField.getNumber().doubleValue(), 0.00000001);
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() throws IOException {
		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			ContentDao contentDao = tx.contentDao();

			HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
					node.getProject().getLatestBranch(), user(),
					contentDao.getLatestDraftFieldContainer(node, english()), true);

			HibNumberField numberField = container.createNumber(FIELD_NAME);
			numberField.setNumber(100.9f);
			tx.success();
		}
		NodeResponse response = readNode(folder("2015"));
		NumberFieldImpl deserializedNumberField = response.getFields().getNumberField(FIELD_NAME);
		assertNotNull(deserializedNumberField);
		assertEquals(Double.valueOf(100.9), deserializedNumberField.getNumber().doubleValue(), 0.001);
	}

	/**
	 * Get the number value
	 * 
	 * @param container
	 *            container
	 * @param fieldName
	 *            field name
	 * @return number value (may be null)
	 */
	protected Number getNumberValue(HibNodeFieldContainer container, String fieldName) {
		HibNumberField field = container.getNumber(fieldName);
		return field != null ? field.getNumber() : null;
	}

	@Override
	public NodeResponse createNodeWithField() {
		return createNode(FIELD_NAME, new NumberFieldImpl().setNumber(1.214353));
	}
}
