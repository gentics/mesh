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
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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
	public void testHtmlExists() throws IOException {
		testString(true, true);
	}

	@Test
	public void testHtmlNotExists() throws IOException {
		testHtml(true, false);
	}

	@Test
	public void testHtmlFieldNotExists() throws IOException {
		testHtml(false, false);
	}
	
	private void testHtml(boolean fieldShouldExist, boolean contentShouldExist) throws IOException {
		String value = "<div>This is a hypertext <a href=\"/\">Field Value</a></div>";
		
		Optional<FieldSchema> maybeField = fieldShouldExist 
				? Optional.of(new HtmlFieldSchemaImpl().setName("html_content").setLabel("HTML content"))
				: Optional.empty();
		
		Optional<Consumer<Node>> maybeContentSupplier = contentShouldExist
				? Optional.of(node -> {
					String uuid = tx(() -> node.getUuid());
					NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, 
							new VersioningParametersImpl().draft(),
							new NodeParametersImpl().setResolveLinks(LinkType.SHORT)
						));
					NodeUpdateRequest request = response.toRequest();
					HtmlField html = new HtmlFieldImpl();
					html.setHTML(value);
					request.getFields().put("html_content", html);
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
	
	// String list field

	@Test
	public void testStringListExists() throws IOException {
		testStringList(true, true);
	}

	@Test
	public void testStringListNotExists() throws IOException {
		testStringList(true, false);
	}

	@Test
	public void testStringListFieldNotExists() throws IOException {
		testStringList(false, false);
	}	
	
	private void testStringList(boolean fieldShouldExist, boolean contentShouldExist) throws IOException {
		testList(fieldShouldExist, contentShouldExist, FieldTypes.STRING, "val=1", "2val", "val3", "val 4");
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
	
	// Boolean list field

	@Test
	public void testBooleanListExists() throws IOException {
		testBooleanList(true, true);
	}

	@Test
	public void testBooleanListNotExists() throws IOException {
		testBooleanList(true, false);
	}

	@Test
	public void testBooleanListFieldNotExists() throws IOException {
		testBooleanList(false, false);
	}	
	
	private void testBooleanList(boolean fieldShouldExist, boolean contentShouldExist) throws IOException {
		testList(fieldShouldExist, contentShouldExist, FieldTypes.BOOLEAN, false, true, false, true, false);
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
	
	// Boolean list field

	@Test
	public void testDateListExists() throws IOException {
		testDateList(true, true);
	}

	@Test
	public void testDateListNotExists() throws IOException {
		testDateList(true, false);
	}

	@Test
	public void testDateListFieldNotExists() throws IOException {
		testDateList(false, false);
	}	
	
	private void testDateList(boolean fieldShouldExist, boolean contentShouldExist) throws IOException {
		Calendar now = Calendar.getInstance();
		String nowAsISO = DateUtils.toISO8601(now.getTimeInMillis());
		
		testList(fieldShouldExist, contentShouldExist, FieldTypes.DATE, nowAsISO, nowAsISO, nowAsISO, nowAsISO);
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
	
	// Number list field

	@Test
	public void testNumberListExists() throws IOException {
		testNumberList(true, true);
	}

	@Test
	public void testNumberListNotExists() throws IOException {
		testNumberList(true, false);
	}

	@Test
	public void testNumberListFieldNotExists() throws IOException {
		testNumberList(false, false);
	}	
	
	private void testNumberList(boolean fieldShouldExist, boolean contentShouldExist) throws IOException {
		testList(fieldShouldExist, contentShouldExist, FieldTypes.NUMBER, (Number) 1234, (Number) 5678, (Number) 9.1011, (Number) (-1213.1415));
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
		HibMicroschema container = microschemaContainers().get("vcard");
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
	
	// Micronode list field

	@Test
	public void testMicronodeListExists() throws IOException {
		testMicronodeList(true, true);
	}

	@Test
	public void testMicronodeListNotExists() throws IOException {
		testMicronodeList(true, false);
	}

	@Test
	public void testMicronodeListFieldNotExists() throws IOException {
		testMicronodeList(false, false);
	}	
	
	private void testMicronodeList(boolean fieldShouldExist, boolean contentShouldExist) throws IOException {
		HibMicroschema container = microschemaContainers().get("vcard");
		MicroschemaReferenceImpl reference = new MicroschemaReferenceImpl();
		reference.setUuid(container.getUuid());
		
		List<MicronodeResponse> list = IntStream.range(0, 5).mapToObj(i -> {
			MicronodeResponse micronode = new MicronodeResponse();
			micronode.getFields().put("firstName", StringField.of("Mickey" + i));
			micronode.getFields().put("lastName", StringField.of("Mouse" + i));
			micronode.setMicroschema(reference);
			return micronode;
		}).collect(Collectors.toList());
		
		Consumer<MeshWebrootFieldResponse> asserter = response -> {
			assertFalse(response.isPlainText());
			assertFalse(response.isBinary());
			JsonArray json = new JsonArray(response.getResponseAsJsonString());
			
			Set<String> valuesSet = new HashSet<>();
			list.forEach(micronode -> {
				valuesSet.add(micronode.getFields().getStringField("firstName").getString());
				valuesSet.add(micronode.getFields().getStringField("lastName").getString());
			});
			
			json.forEach(result -> {
				JsonObject o = (JsonObject) result;
				assertTrue(valuesSet.remove(o.getJsonObject("fields").getString("firstName")));
				assertTrue(valuesSet.remove(o.getJsonObject("fields").getString("lastName")));
				
				json.remove(result);				
			});
			
			assertTrue(valuesSet.isEmpty());
		};
		
		testList(fieldShouldExist, contentShouldExist, FieldTypes.MICRONODE, list, Optional.of(asserter));
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
		HibSchema container = schemaContainers().get("folder");
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
	
	// Node list field

	@Test
	public void testNodeListExists() throws IOException {
		testNodeList(true, true);
	}

	@Test
	public void testNodeListNotExists() throws IOException {
		testNodeList(true, false);
	}

	@Test
	public void testNodeListFieldNotExists() throws IOException {
		testNodeList(false, false);
	}	
	
	private void testNodeList(boolean fieldShouldExist, boolean contentShouldExist) throws IOException {
		HibSchema container = schemaContainers().get("folder");
		NodeFieldImpl nodeField = new NodeFieldImpl();
		
		List<NodeResponse> list = IntStream.range(0, 5).mapToObj(i -> {
			NodeResponse referenced = createNode();
			SchemaReferenceImpl reference = new SchemaReferenceImpl();
			reference.setUuid(container.getUuid());
			nodeField.setUuid(referenced.getUuid());
			return referenced;
		}).collect(Collectors.toList());
		
		Consumer<MeshWebrootFieldResponse> asserter = response -> {
			assertFalse(response.isPlainText());
			assertFalse(response.isBinary());
			JsonArray json = new JsonArray(response.getResponseAsJsonString());
			
			Set<String> valuesSet = list.stream().map(node -> node.getUuid()).collect(Collectors.toSet());
			
			json.forEach(result -> {
				JsonObject o = (JsonObject) result;
				assertTrue(valuesSet.remove(o.getString("uuid")));
				
				json.remove(result);				
			});
			
			assertTrue(valuesSet.isEmpty());
		};
		
		testList(fieldShouldExist, contentShouldExist, FieldTypes.NODE, list, Optional.of(asserter));
	}


	private <T extends Object> void testList(boolean fieldShouldExist, boolean contentShouldExist, FieldTypes type, T... listValues) throws IOException {
		List<T> values = Arrays.asList(listValues);
		testList(fieldShouldExist, contentShouldExist, type, values, Optional.empty());
	}
	
	private <T extends Object> void testList(boolean fieldShouldExist, boolean contentShouldExist, FieldTypes type, List<T> values, Optional<Consumer<MeshWebrootFieldResponse>> resultsAsserter) throws IOException {
		String itemTypeName = type.name().toLowerCase();
		
		Optional<FieldSchema> maybeField = fieldShouldExist 
				? Optional.of(new ListFieldSchemaImpl().setListType(itemTypeName).setName(itemTypeName + "_list_content").setLabel(itemTypeName.toUpperCase() + " list content"))
				: Optional.empty();
		
		@SuppressWarnings("unchecked")
		Optional<Consumer<Node>> maybeContentSupplier = contentShouldExist
				? Optional.of(node -> {
					String uuid = tx(() -> node.getUuid());
					NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, 
							new VersioningParametersImpl().draft(),
							new NodeParametersImpl().setResolveLinks(LinkType.SHORT)
						));
					NodeUpdateRequest request = response.toRequest();
					FieldList<T> fieldList;
					
					switch (type) {
					case STRING:
						fieldList = (FieldList<T>) new StringFieldListImpl();
						break;
					case HTML:
						fieldList = (FieldList<T>) new HtmlFieldListImpl();
						break;
					case NUMBER:
						fieldList = (FieldList<T>) new NumberFieldListImpl();
						break;
					case DATE:
						fieldList = (FieldList<T>) new DateFieldListImpl();
						break;
					case BOOLEAN:
						fieldList = (FieldList<T>) new BooleanFieldListImpl();
						break;
					case NODE:
						fieldList = (FieldList<T>) new NodeFieldListImpl();
						break;
					case MICRONODE:
						fieldList = (FieldList<T>) new MicronodeFieldListImpl();
						break;
					default:
						throw new IllegalArgumentException("Unsupported list item type: " + type.name());
					}					
					
					values.forEach(value -> fieldList.add(value));
					request.getFields().put(itemTypeName + "_list_content", fieldList);
					call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), request));
				})
				: Optional.empty();
				
		@SuppressWarnings("unchecked")
		Consumer<MeshWebrootFieldResponse> resultsConsumer = response -> {
			if (resultsAsserter.isPresent()) {
				resultsAsserter.get().accept(response);
			} else {
				assertFalse(response.isPlainText());
				assertFalse(response.isBinary());
				JsonArray json = new JsonArray(response.getResponseAsJsonString());
				assertTrue(Arrays.equals(json.getList().toArray(new Object[values.size()]), values.toArray(new Object[values.size()])));
			}
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
		HibNode node = content("news_2015");
		String nodeUuid = tx(() -> node.getUuid());
		String fieldName;

		if (field.isPresent()) {
			try (Tx tx = tx()) {
				ContentDaoWrapper contentDao = tx.contentDao();
				HibSchema container = schemaContainer(isBinaryContent ? "binary_content" :"content");
				node.setSchemaContainer(container);
				contentDao.getLatestDraftFieldContainer(node, english()).setSchemaContainerVersion(container.getLatestVersion());
				FieldSchema schema = field.get();
				prepareTypedSchema(node, schema);
				tx.success();
				fieldName = schema.getName();
			}
			if (contentSupplier.isPresent()) {
				contentSupplier.get().accept((Node) node);
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
