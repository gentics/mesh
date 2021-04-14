package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import java.util.Arrays;

import org.junit.Test;

import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true, clusterMode = false)
public class MicronodeMigrationAllFieldsTest extends AbstractMeshTest {

	public static final String SCHEMA_NAME = "AllFields";

	@Test
	public void testMigration() {
		grantAdmin();
		createSchema();
		createAllFieldsNode();
		updateMicroschema();
	}

	private SchemaResponse createSchema() {
		SchemaCreateRequest request = new SchemaCreateRequest()
			.setName(SCHEMA_NAME)
			.setFields(Arrays.asList(
				new StringFieldSchemaImpl().setName("string"),
				new NumberFieldSchemaImpl().setName("number"),
				new BooleanFieldSchemaImpl().setName("boolean"),
				new DateFieldSchemaImpl().setName("date"),
				new HtmlFieldSchemaImpl().setName("html"),
				new NodeFieldSchemaImpl().setName("node"),
				new BinaryFieldSchemaImpl().setName("binary"),
				new MicronodeFieldSchemaImpl().setName("micronode"),
				new ListFieldSchemaImpl()
					.setListType("string").setName("liststring"),
				new ListFieldSchemaImpl()
					.setListType("number").setName("listnumber"),
				new ListFieldSchemaImpl()
					.setListType("date").setName("listdate"),
				new ListFieldSchemaImpl()
					.setListType("boolean").setName("listboolean"),
				new ListFieldSchemaImpl()
					.setListType("html").setName("listhtml"),
				new ListFieldSchemaImpl()
					.setListType("node").setName("listnode"),
				new ListFieldSchemaImpl()
					.setListType("micronode").setName("listmicronode")
			));

		SchemaResponse schemaResponse = client().createSchema(request).blockingGet();
		client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()).blockingAwait();
		return schemaResponse;
	}

	private NodeResponse createAllFieldsNode() {
		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.setSchemaName(SCHEMA_NAME);
		request.setParentNodeUuid(folderUuid());
		FieldMap fields = request.getFields();

		MicronodeResponse micronode = new MicronodeResponse();
		micronode.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
		FieldMap micronodeFields = micronode.getFields();
		micronodeFields.putString("firstName", "John");
		micronodeFields.putString("lastName", "Doe");

		fields.put("micronode", micronode);
		fields.put("listmicronode", new MicronodeFieldListImpl().setItems(Arrays.asList(
			micronode,
			micronode
		)));

		fields.put("string", new StringFieldImpl().setString("example"));
		fields.put("number", new NumberFieldImpl().setNumber(1234));
		fields.put("boolean", new BooleanFieldImpl().setValue(false));
		fields.put("date", new DateFieldImpl().setDate("2020-02-21T12:40:00Z"));
		fields.put("html", new HtmlFieldImpl().setHTML("<p>example</p>"));
		fields.put("node", new NodeFieldImpl().setUuid(folderUuid()));
		fields.put("liststring", new StringFieldListImpl().setItems(Arrays.asList(
			"example1",
			"example2"
		)));
		fields.put("listnumber", new NumberFieldListImpl().setItems(Arrays.asList(
			123,
			456
		)));
		fields.put("listdate", new DateFieldListImpl().setItems(Arrays.asList(
			"2020-02-21T12:40:00Z",
			"2020-02-22T12:40:00Z"
		)));
		fields.put("listboolean", new BooleanFieldListImpl().setItems(Arrays.asList(
			false, true
		)));
		fields.put("listhtml", new HtmlFieldListImpl().setItems(Arrays.asList(
			"<p>example1</p>",
			"<p>example2</p>"
		)));
		fields.put("listnode", new NodeFieldListImpl().setItems(Arrays.asList(
			new NodeFieldListItemImpl().setUuid(folderUuid()),
			new NodeFieldListItemImpl().setUuid(folderUuid())
		)));

		return client().createNode(PROJECT_NAME, request).blockingGet();
	}

	private void updateMicroschema() {
		MicroschemaResponse microschema = client().findMicroschemas().blockingGet().getData().get(0);
		microschema.getFields().add(new StringFieldSchemaImpl().setName("additionalStringField"));

		waitForJob(() -> {
			client().updateMicroschema(microschema.getUuid(), microschema.toRequest()).blockingAwait();
		});
	}
}
