package com.gentics.mesh.core.webrootfield;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshWebrootFieldResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.DateUtils;

@MeshTestSetting(testSize = FULL, startServer = true)
public class WebRootFieldTypeTest extends AbstractMeshTest {

	// Binary field

	@Test
	public void testBinaryExists() throws IOException {
		testBinary(true, true);
	}

	@Test
	public void testBinaryNotExists() throws IOException {
		testBinary(true, false);
	}

	@Test
	public void testBinaryFieldNotExists() throws IOException {
		testBinary(false, false);
	}
	
	private void testBinary(boolean fieldShouldExist, boolean contentShouldExist) throws IOException {
		String fileName = "somefile.dat";
		
		Optional<FieldSchema> maybeBinaryField = fieldShouldExist 
				? Optional.of(new BinaryFieldSchemaImpl().setAllowedMimeTypes("image/*").setName("binary").setLabel("Binary content"))
				: Optional.empty();
		
		Optional<Consumer<Node>> maybeBinaryContentSupplier = contentShouldExist
				? Optional.of(node -> {
						String contentType = "application/octet-stream";
						int binaryLen = 8000;
						call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));
					})
				: Optional.empty();
					
		Consumer<MeshWebrootFieldResponse> resultsConsumer = response -> {
			MeshBinaryResponse downloadResponse = response.getBinaryResponse();
			assertTrue(response.isBinary());
			assertFalse(response.isPlainText());
			assertNotNull(downloadResponse);
		};
		
		testField("/News/2015/" + fileName, maybeBinaryField, maybeBinaryContentSupplier, resultsConsumer, true);
	}

	// String field

	@Test
	public void testStringExists() throws IOException {
		testString(true, true);
	}

	@Test
	public void testStringNotExists() throws IOException {
		testString(true, false);
	}

	@Test
	public void testStringFieldNotExists() throws IOException {
		testString(false, false);
	}
	
	private void testString(boolean fieldShouldExist, boolean contentShouldExist) throws IOException {
		String value = "String Field Value";
		
		Optional<FieldSchema> maybeField = fieldShouldExist 
				? Optional.of(new StringFieldSchemaImpl().setName("string_content").setLabel("String content"))
				: Optional.empty();
		
		Optional<Consumer<Node>> maybeContentSupplier = contentShouldExist
				? Optional.of(node -> {
					String uuid = tx(() -> node.getUuid());
					NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, 
							new VersioningParametersImpl().draft(),
							new NodeParametersImpl().setResolveLinks(LinkType.SHORT)
						));
					NodeUpdateRequest request = response.toRequest();
					request.getFields().put("string_content", StringField.of(value));
					call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request));
				})
				: Optional.empty();
				
		Consumer<MeshWebrootFieldResponse> resultsConsumer = response -> {
			assertTrue(response.isPlainText());
			assertFalse(response.isBinary());
			assertEquals(response.getResponseAsPlainText(), value);
		};
		
		testField("/News/2015/News_2015.en.html", maybeField, maybeContentSupplier, resultsConsumer, false);
	}
	
	// Boolean field

	@Test
	public void testBooleanExists() throws IOException {
		testBoolean(true, true);
	}

	@Test
	public void testBooleanNotExists() throws IOException {
		testBoolean(true, false);
	}

	@Test
	public void testBooleanFieldNotExists() throws IOException {
		testBoolean(false, false);
	}
	
	private void testBoolean(boolean fieldShouldExist, boolean contentShouldExist) throws IOException {
		boolean value = true;
		
		Optional<FieldSchema> maybeField = fieldShouldExist 
				? Optional.of(new BooleanFieldSchemaImpl().setName("boolean_content").setLabel("Boolean content"))
				: Optional.empty();
		
		Optional<Consumer<Node>> maybeContentSupplier = contentShouldExist
				? Optional.of(node -> {
					String uuid = tx(() -> node.getUuid());
					NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, 
							new VersioningParametersImpl().draft(),
							new NodeParametersImpl().setResolveLinks(LinkType.SHORT)
						));
					NodeUpdateRequest request = response.toRequest();
					BooleanField field = new BooleanFieldImpl();
					field.setValue(value);
					request.getFields().put("boolean_content", field);
					call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request));
				})
				: Optional.empty();
				
		Consumer<MeshWebrootFieldResponse> resultsConsumer = response -> {
			assertTrue(response.isPlainText());
			assertFalse(response.isBinary());
			assertEquals(response.getResponseAsPlainText(), Boolean.toString(value));
		};
		
		testField("/News/2015/News_2015.en.html", maybeField, maybeContentSupplier, resultsConsumer, false);
	}
	
	// Date field

	@Test
	public void testDateExists() throws IOException {
		testDate(true, true);
	}

	@Test
	public void testDateNotExists() throws IOException {
		testDate(true, false);
	}

	@Test
	public void testDateFieldNotExists() throws IOException {
		testDate(false, false);
	}
	
	private void testDate(boolean fieldShouldExist, boolean contentShouldExist) throws IOException {
		Calendar now = Calendar.getInstance();
		String nowAsISO = DateUtils.toISO8601(now.getTimeInMillis());
		
		Optional<FieldSchema> maybeField = fieldShouldExist 
				? Optional.of(new DateFieldSchemaImpl().setName("date_content").setLabel("Date content"))
				: Optional.empty();
		
		Optional<Consumer<Node>> maybeContentSupplier = contentShouldExist
				? Optional.of(node -> {
					String uuid = tx(() -> node.getUuid());
					NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, 
							new VersioningParametersImpl().draft(),
							new NodeParametersImpl().setResolveLinks(LinkType.SHORT)
						));
					NodeUpdateRequest request = response.toRequest();
					DateField field = new DateFieldImpl();
					field.setDate(nowAsISO);
					request.getFields().put("date_content", field);
					call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request));
				})
				: Optional.empty();
				
		Consumer<MeshWebrootFieldResponse> resultsConsumer = response -> {
			assertTrue(response.isPlainText());
			assertFalse(response.isBinary());
			Calendar parsed = Calendar.getInstance();
			parsed.setTimeInMillis(DateUtils.fromISO8601(response.getResponseAsPlainText()));
			assertEquals(now, parsed);
		};
		
		testField("/News/2015/News_2015.en.html", maybeField, maybeContentSupplier, resultsConsumer, false);
	}
	
	// Number field

	@Test
	public void testNumberExists() throws IOException {
		testNumber(true, true);
	}

	@Test
	public void testNumberNotExists() throws IOException {
		testNumber(true, false);
	}

	@Test
	public void testNumberFieldNotExists() throws IOException {
		testNumber(false, false);
	}
	
	private void testNumber(boolean fieldShouldExist, boolean contentShouldExist) throws IOException {
		float value = 12345.67890f;
		
		Optional<FieldSchema> maybeField = fieldShouldExist 
				? Optional.of(new NumberFieldSchemaImpl().setName("number_content").setLabel("Float number content"))
				: Optional.empty();
		
		Optional<Consumer<Node>> maybeContentSupplier = contentShouldExist
				? Optional.of(node -> {
					String uuid = tx(() -> node.getUuid());
					NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, 
							new VersioningParametersImpl().draft(),
							new NodeParametersImpl().setResolveLinks(LinkType.SHORT)
						));
					NodeUpdateRequest request = response.toRequest();
					NumberField field = new NumberFieldImpl();
					field.setNumber(value);
					request.getFields().put("number_content", field);
					call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request));
				})
				: Optional.empty();
				
		Consumer<MeshWebrootFieldResponse> resultsConsumer = response -> {
			assertTrue(response.isPlainText());
			assertFalse(response.isBinary());
			assertEquals(value, Float.parseFloat(response.getResponseAsPlainText()), 0.00001);
		};
		
		testField("/News/2015/News_2015.en.html", maybeField, maybeContentSupplier, resultsConsumer, false);
	}
	
	// Micronode field
	
	@Test
	public void testMicronodeFieldExists() throws IOException {
		testMicronode(true, true);
	}

	@Test
	public void testMicronodeNotExists() throws IOException {
		testMicronode(true, false);
	}

	@Test
	public void testMicronodeFieldNotExists() throws IOException {
		testMicronode(false, false);
	}
	
	private void testMicronode(boolean fieldShouldExist, boolean contentShouldExist) throws IOException {
		MicroschemaContainer container = microschemaContainers().get("vcard");
		MicronodeResponse micronode = new MicronodeResponse();
		MicroschemaReferenceImpl reference = new MicroschemaReferenceImpl();
		reference.setUuid(container.getUuid());
		micronode.getFields().put("firstName", StringField.of("Mickey"));
		micronode.getFields().put("lastName", StringField.of("Mouse"));
		micronode.setMicroschema(reference);
		
		Optional<FieldSchema> maybeField = fieldShouldExist 
				? Optional.of(new MicronodeFieldSchemaImpl()
						.setAllowedMicroSchemas("vcard")
						.setName("micronode_content")
						.setLabel("Micronode VCARD content"))
				: Optional.empty();
		
		Optional<Consumer<Node>> maybeContentSupplier = contentShouldExist
				? Optional.of(node -> {
					String uuid = tx(() -> node.getUuid());
					NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, 
							new VersioningParametersImpl().draft(),
							new NodeParametersImpl().setResolveLinks(LinkType.SHORT)
						));
					
					NodeUpdateRequest request = response.toRequest();
					request.getFields().put("micronode_content", micronode);
					call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request));
				})
				: Optional.empty();
				
		Consumer<MeshWebrootFieldResponse> resultsConsumer = response -> {
			assertFalse(response.isPlainText());
			assertFalse(response.isBinary());
			
			MicronodeResponse micronodeResponse = JsonUtil.readValue(response.getResponseAsJsonString(), MicronodeResponse.class);
			assertEquals(reference.getUuid(), micronodeResponse.getMicroschema().getUuid());
			assertEquals("Mickey", micronodeResponse.getFields().getStringField("firstName").getString());
			assertEquals("Mouse", micronodeResponse.getFields().getStringField("lastName").getString());
		};
		
		testField("/News/2015/News_2015.en.html", maybeField, maybeContentSupplier, resultsConsumer, false);
	}
	
	// Node field
	
	@Test
	public void testNodeFieldExists() throws IOException {
		testNode(true, true);
	}

	@Test
	public void testNodeNotExists() throws IOException {
		testNode(true, false);
	}

	@Test
	public void testNodeFieldNotExists() throws IOException {
		testNode(false, false);
	}
	
	private void testNode(boolean fieldShouldExist, boolean contentShouldExist) throws IOException {
		SchemaContainer container = schemaContainers().get("folder");
		NodeFieldImpl nodeField = new NodeFieldImpl();
		NodeResponse referenced = createNode();
		SchemaReferenceImpl reference = new SchemaReferenceImpl();
		reference.setUuid(container.getUuid());
		nodeField.setUuid(referenced.getUuid());
		
		Optional<FieldSchema> maybeField = fieldShouldExist 
				? Optional.of(new NodeFieldSchemaImpl()
						.setAllowedSchemas("folder")
						.setName("node_content")
						.setLabel("Node content"))
				: Optional.empty();
		
		Optional<Consumer<Node>> maybeContentSupplier = contentShouldExist
				? Optional.of(node -> {
					String uuid = tx(() -> node.getUuid());
					NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, 
							new VersioningParametersImpl().draft(),
							new NodeParametersImpl().setResolveLinks(LinkType.SHORT)
						));
					
					NodeUpdateRequest request = response.toRequest();
					request.getFields().put("node_content", nodeField);
					call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request));
				})
				: Optional.empty();
				
		Consumer<MeshWebrootFieldResponse> resultsConsumer = response -> {
			assertFalse(response.isPlainText());
			assertFalse(response.isBinary());
			
			NodeResponse nodeResponse = JsonUtil.readValue(response.getResponseAsJsonString(), NodeResponse.class);
			assertEquals(referenced.getUuid(), nodeResponse.getUuid());
		};
		
		testField("/News/2015/News_2015.en.html", maybeField, maybeContentSupplier, resultsConsumer, false);
	}
	
	private <F extends FieldSchema> void testField(
			String path,
			Optional<F> field, 
			Optional<Consumer<Node>> contentSupplier,
			Consumer<MeshWebrootFieldResponse> resultsConsumer,
			boolean isBinaryContent
	) throws IOException {
		Node node = content("news_2015");
		String nodeUuid = tx(() -> node.getUuid());
		String fieldName;

		if (field.isPresent()) {
			try (Tx tx = tx()) {
				SchemaContainer container = schemaContainer(isBinaryContent ? "binary_content" :"content");
				node.setSchemaContainer(container);
				node.getLatestDraftFieldContainer(english()).setSchemaContainerVersion(container.getLatestVersion());
				FieldSchema schema = field.get();
				prepareTypedSchema(node, schema);
				tx.success();
				fieldName = schema.getName();
			}
			if (contentSupplier.isPresent()) {
				contentSupplier.get().accept(node);
			}
		} else {
			fieldName = "field";
		}
		
		if (field.isPresent()) {
			if (contentSupplier.isPresent()) {
				MeshWebrootFieldResponse response = call(() -> client().webrootField(PROJECT_NAME, fieldName, path, new VersioningParametersImpl().draft(),
						new NodeParametersImpl().setResolveLinks(LinkType.FULL)));
				
				assertEquals("Webroot response node uuid header value did not match", nodeUuid, response.getNodeUuid());
				resultsConsumer.accept(response);
			} else {
				if (isBinaryContent) {
					call(() -> client().webrootField(PROJECT_NAME, fieldName, path), NOT_FOUND, "node_not_found_for_path", path);
				} else {
					call(() -> client().webrootField(PROJECT_NAME, fieldName, path), NOT_FOUND, "error_field_not_found_with_name", fieldName);
				}
			}
		} else {
			if (isBinaryContent) {
				call(() -> client().webrootField(PROJECT_NAME, fieldName, path), NOT_FOUND, "node_not_found_for_path", path);
			} else {
				call(() -> client().webrootField(PROJECT_NAME, fieldName, path), NOT_FOUND, "error_field_not_found_with_name", fieldName);
			}
		}
	}
}
