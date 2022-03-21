package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class EmptyAllowTest extends AbstractMeshTest {
	private String nodeUuid;

	@Before
	public void setUp() throws Exception {
		nodeUuid = tx(() -> folder("2015").getUuid());
	}

	private void createSchema(FieldSchema field) {
		field.setName("testField");

		SchemaCreateRequest req = new SchemaCreateRequest();
		req.setName("test");
		req.setFields(Collections.singletonList(field));

		SchemaResponse response = client().createSchema(req).blockingGet();
		client().assignSchemaToProject(PROJECT_NAME, response.getUuid()).blockingAwait();
	}

	private void createNode(Field field) {
		NodeCreateRequest req = new NodeCreateRequest();
		req.setLanguage("en");
		req.setSchemaName("test");
		req.setParentNodeUuid(nodeUuid);
		FieldMapImpl fieldMap = new FieldMapImpl();
		fieldMap.put("testField", field);
		req.setFields(fieldMap);

		client().createNode(PROJECT_NAME, req).blockingAwait();
	}

	private void runTest(FieldSchema schemaField, Field nodeField) {
		createSchema(schemaField);
		createNode(nodeField);
	}

	private MicronodeResponse createMicronode() {
		MicronodeResponse vcard = new MicronodeResponse().setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
		FieldMap fields = vcard.getFields();
		fields.put("firstName", new StringFieldImpl().setString("John"));
		fields.put("lastName", new StringFieldImpl().setString("Doe"));
		return vcard;
	}

	@Test
	public void string() {
		runTest(
			new StringFieldSchemaImpl().setAllowedValues(),
			new StringFieldImpl().setString("test"));
	}

	@Test
	public void stringList() {
		runTest(
			new ListFieldSchemaImpl().setListType("string").setAllowedSchemas(),
			new StringFieldListImpl().setItems(Collections.singletonList("test")));
	}

	@Test
	public void node() {
		runTest(
			new NodeFieldSchemaImpl().setAllowedSchemas(),
			new NodeFieldImpl().setUuid(nodeUuid));
	}

	@Test
	public void nodeList() {
		runTest(
			new ListFieldSchemaImpl().setListType("node").setAllowedSchemas(),
			new NodeFieldListImpl().setItems(Collections.singletonList(new NodeFieldListItemImpl().setUuid(nodeUuid))));
	}

	@Test
	public void micronode() {
		runTest(
			new MicronodeFieldSchemaImpl().setAllowedMicroSchemas(),
			createMicronode());
	}

	@Test
	public void micronodeList() {
		runTest(
			new ListFieldSchemaImpl().setListType("micronode").setAllowedSchemas(),
			new MicronodeFieldListImpl().setItems(Collections.singletonList(createMicronode())));
	}
}
