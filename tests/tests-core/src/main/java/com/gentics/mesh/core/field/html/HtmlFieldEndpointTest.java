package com.gentics.mesh.core.field.html;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.category.FailingTests;
import com.gentics.mesh.util.VersionNumber;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class HtmlFieldEndpointTest extends AbstractFieldEndpointTest {

	private static final String FIELD_NAME = "htmlField";

	@Before
	public void updateSchema() throws IOException {
		try (Tx tx = tx()) {
			HtmlFieldSchema htmlFieldSchema = new HtmlFieldSchemaImpl();
			htmlFieldSchema.setName(FIELD_NAME);
			htmlFieldSchema.setLabel("Some label");
			prepareTypedSchema(schemaContainer("folder"), List.of(htmlFieldSchema), Optional.empty());
			tx.success();
		}
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(null, (Field) null);
		HtmlFieldImpl htmlField = response.getFields().getHtmlField(FIELD_NAME);
		assertNull("The response should not contain the field because it should still be null", htmlField);
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		HibNode node = folder("2015");
		for (int i = 0; i < 20; i++) {
			VersionNumber oldVersion = tx(tx -> { return tx.contentDao().getFieldContainer(node, "en").getVersion(); });
			String newValue = "some<b>html <i>" + i + "</i>";

			NodeResponse response = updateNode(FIELD_NAME, new HtmlFieldImpl().setHTML(newValue));
			HtmlFieldImpl field = response.getFields().getHtmlField(FIELD_NAME);
			assertEquals(newValue, field.getHTML());
			assertEquals("Check version number", oldVersion.nextDraft().toString(), response.getVersion());
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		NodeResponse firstResponse = updateNode(FIELD_NAME, new HtmlFieldImpl().setHTML("bla"));
		String oldVersion = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, new HtmlFieldImpl().setHTML("bla"));
		assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldVersion);
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
			ContentDao contentDao = tx.contentDao();
			HibNode node = folder("2015");
			HibNodeFieldContainer latest = contentDao.getLatestDraftFieldContainer(node, english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getHtml(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getHtml(FIELD_NAME)).isNotNull();
			String oldValue = latest.getPreviousVersion().getHtml(FIELD_NAME).getHTML();
			assertThat(oldValue).isEqualTo("bla");
		}
		NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
			secondResponse.getVersion());
	}

	@Test
	@Override
	@Category(FailingTests.class)
	@Deprecated(forRemoval = true)
	// We should not tell apart the empty and null strings
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
		NodeResponse response = createNodeWithField();
		HtmlFieldImpl htmlField = response.getFields().getHtmlField(FIELD_NAME);
		assertEquals("Some<b>html", htmlField.getHTML());
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
					node.getProject().getLatestBranch(), user(),
					contentDao.getLatestDraftFieldContainer(node, english()), true);
			container.createHTML(FIELD_NAME).setHtml("some<b>html");
			tx.success();
		}
		NodeResponse response = readNode(folder("2015"));
		HtmlFieldImpl deserializedHtmlField = response.getFields().getHtmlField(FIELD_NAME);
		assertNotNull(deserializedHtmlField);
		assertEquals("some<b>html", deserializedHtmlField.getHTML());
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
	protected String getHtmlValue(HibNodeFieldContainer container, String fieldName) {
		HibHtmlField field = container.getHtml(fieldName);
		return field != null ? field.getHTML() : null;
	}

	@Override
	public NodeResponse createNodeWithField() {
		return createNode(FIELD_NAME, new HtmlFieldImpl().setHTML("Some<b>html"));
	}
}
