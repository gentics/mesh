package com.gentics.mesh.core.field.html;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class HtmlFieldEndpointTest extends AbstractFieldEndpointTest {

	private static final String FIELD_NAME = "htmlField";

	@Before
	public void updateSchema() throws IOException {
		try (Tx tx = tx()) {
			SchemaVersionModel schema = schemaContainer("folder").getLatestVersion().getSchema();
			HtmlFieldSchema htmlFieldSchema = new HtmlFieldSchemaImpl();
			htmlFieldSchema.setName(FIELD_NAME);
			htmlFieldSchema.setLabel("Some label");
			schema.addField(htmlFieldSchema);
			schemaContainer("folder").getLatestVersion().setSchema(schema);
			tx.success();
		}
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNode(null, (Field) null);
			HtmlFieldImpl htmlField = response.getFields().getHtmlField(FIELD_NAME);
			assertNull("The response should not contain the field because it should still be null", htmlField);
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		Node node = folder("2015");
		for (int i = 0; i < 20; i++) {
			try (Tx tx = tx()) {
				NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
				String oldValue = getHtmlValue(container, FIELD_NAME);

				String newValue = "some<b>html <i>" + i + "</i>";

				NodeResponse response = updateNode(FIELD_NAME, new HtmlFieldImpl().setHTML(newValue));
				HtmlFieldImpl field = response.getFields().getHtmlField(FIELD_NAME);
				assertEquals(newValue, field.getHTML());

				assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion());
				assertEquals("Check old value", oldValue, getHtmlValue(container, FIELD_NAME));
			}
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		try (Tx tx = tx()) {
			NodeResponse firstResponse = updateNode(FIELD_NAME, new HtmlFieldImpl().setHTML("bla"));
			String oldVersion = firstResponse.getVersion();

			NodeResponse secondResponse = updateNode(FIELD_NAME, new HtmlFieldImpl().setHTML("bla"));
			assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldVersion);
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		disableAutoPurge();

		NodeResponse firstResponse = updateNode(FIELD_NAME, new HtmlFieldImpl().setHTML("bla"));
		String oldVersion = firstResponse.getVersion();

		// Simple field with no value results in a request JSON null value.
		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getHtmlField(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);

		// Assert that the old version was not modified
		try (Tx tx = tx()) {
			ContentDaoWrapper contentDao = tx.data().contentDao();
			Node node = folder("2015");
			NodeGraphFieldContainer latest = contentDao.getLatestDraftFieldContainer(node, english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getHtml(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getHtml(FIELD_NAME)).isNotNull();
			String oldValue = latest.getPreviousVersion().getHtml(FIELD_NAME).getHTML();
			assertThat(oldValue).isEqualTo("bla");

			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
				secondResponse.getVersion());
		}
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		NodeResponse firstResponse = updateNode(FIELD_NAME, new HtmlFieldImpl().setHTML("bla"));
		String oldVersion = firstResponse.getVersion();

		HtmlFieldImpl emptyField = new HtmlFieldImpl();
		emptyField.setHTML("");
		NodeResponse secondResponse = updateNode(FIELD_NAME, emptyField);
		assertThat(secondResponse.getFields().getHtmlField(FIELD_NAME)).as("Updated Field").isNotNull();
		assertThat(secondResponse.getFields().getHtmlField(FIELD_NAME).getHTML()).as("Updated Field Value").isEqualTo("");
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);

		NodeResponse thirdResponse = updateNode(FIELD_NAME, emptyField);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(), secondResponse.getVersion());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNodeWithField();
			HtmlFieldImpl htmlField = response.getFields().getHtmlField(FIELD_NAME);
			assertEquals("Some<b>html", htmlField.getHTML());
		}
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		Node node = folder("2015");
		try (Tx tx = tx()) {
			ContentDaoWrapper contentDao = tx.data().contentDao();
			NodeGraphFieldContainer container = contentDao.getLatestDraftFieldContainer(node, english());
			container.createHTML(FIELD_NAME).setHtml("some<b>html");
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeResponse response = readNode(node);
			HtmlFieldImpl deserializedHtmlField = response.getFields().getHtmlField(FIELD_NAME);
			assertNotNull(deserializedHtmlField);
			assertEquals("some<b>html", deserializedHtmlField.getHTML());
		}
	}

	/**
	 * Get the html value
	 * 
	 * @param container
	 *            container
	 * @param fieldName
	 *            field name
	 * @return html value (may be null)
	 */
	protected String getHtmlValue(NodeGraphFieldContainer container, String fieldName) {
		HtmlGraphField field = container.getHtml(fieldName);
		return field != null ? field.getHTML() : null;
	}

	@Override
	public NodeResponse createNodeWithField() {
		return createNode(FIELD_NAME, new HtmlFieldImpl().setHTML("Some<b>html"));
	}
}
