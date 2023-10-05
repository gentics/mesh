package com.gentics.mesh.search;

import static com.gentics.mesh.search.SearchProvider.DEFAULT_TYPE;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.search.index.node.NodeContainerMappingProvider;
import com.gentics.mesh.core.search.index.node.NodeIndexHandler;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class MicroschemaMappingTest extends AbstractMeshTest {

	private static final String SCHEMA_NAME = "test_schema";
	private static final String MICROSCHEMA_NAME = "test_microschema";
	private static final String MICRONODE_FIELD_NAME = "micronodeField";
	private static final String MICRONODE_LIST_FIELD_NAME = "micronodeListField";
	private static final String DYNAMIC_FIELD_NAME = "testField";

	/**
	 * An data-provider function to output test-data for each type. The content of the Array is passed into {@link #schemaField}, {@link #fieldMapping},
	 * {@link #nodeField} and {@link #searchQuery}.
	 */
	@Parameters
	public static Collection<Object[]> paramData() {
		// @formatter:off
		List<Object[]> params = new ArrayList<>(4 * 8);
		for (Boolean noSchemaIndex : new Boolean[] {false, true}) {
			for (Boolean noFieldIndex : new Boolean[] {false, true}) {
				params.add(new Object[] {
					FieldUtil.createStringFieldSchema(DYNAMIC_FIELD_NAME),
					new JsonObject()
						.put("test", new JsonObject()
							.put("type", "keyword")
							.put("index", true)),
					FieldUtil.createStringField("aaa-aaa"),
					(Function<String, JsonObject>) ((path) -> new JsonObject()
						.put("term", new JsonObject()
							.put(path + ".test", "aaa-aaa"))),
					noSchemaIndex,
					noFieldIndex
				});
				params.add(new Object[] {
					FieldUtil.createStringFieldSchema(DYNAMIC_FIELD_NAME),
					new JsonObject()
						.put("test", new JsonObject()
							.put("type", "text")
							.put("index", true)),
					FieldUtil.createStringField("a very cool test string"),
					(Function<String, JsonObject>) ((path) -> new JsonObject()
						.put("match", new JsonObject()
							.put(path + ".test", "cool"))),
					noSchemaIndex,
					noFieldIndex
				});
				params.add(new Object[] {
					FieldUtil.createHtmlFieldSchema(DYNAMIC_FIELD_NAME),
					new JsonObject()
						.put("test", new JsonObject()
							.put("type", "keyword")
							.put("index", true)),
					FieldUtil.createHtmlField("aaa-aaa"),
					(Function<String, JsonObject>) ((path) -> new JsonObject()
						.put("term", new JsonObject()
							.put(path + ".test", "aaa-aaa"))),
					noSchemaIndex,
					noFieldIndex
				});
				params.add(new Object[] {
					FieldUtil.createHtmlFieldSchema(DYNAMIC_FIELD_NAME),
					new JsonObject()
						.put("test", new JsonObject()
							.put("type", "text")
							.put("index", true)),
					FieldUtil.createHtmlField("a very cool test string"),
					(Function<String, JsonObject>) ((path) -> new JsonObject()
						.put("match", new JsonObject()
							.put(path + ".test", "cool"))),
					noSchemaIndex,
					noFieldIndex
				});
				params.add(new Object[] {
					FieldUtil.createListFieldSchema(DYNAMIC_FIELD_NAME, "string"),
					new JsonObject()
						.put("test", new JsonObject()
							.put("type", "keyword")
							.put("index", true)),
					FieldUtil.createStringListField("aaa-aaa"),
					(Function<String, JsonObject>) ((path) -> new JsonObject()
						.put("term", new JsonObject()
							.put(path + ".test", "aaa-aaa"))),
					noSchemaIndex,
					noFieldIndex
				});
				params.add(new Object[] {
					FieldUtil.createListFieldSchema(DYNAMIC_FIELD_NAME, "string"),
					new JsonObject()
						.put("test", new JsonObject()
							.put("type", "text")
							.put("index", true)),
					FieldUtil.createStringListField("a very cool test string"),
					(Function<String, JsonObject>) ((path) -> new JsonObject()
						.put("match", new JsonObject()
							.put(path + ".test", "cool"))),
					noSchemaIndex,
					noFieldIndex
				});
				params.add(new Object[] {
					FieldUtil.createListFieldSchema(DYNAMIC_FIELD_NAME, "html"),
					new JsonObject()
						.put("test", new JsonObject()
							.put("type", "keyword")
							.put("index", true)),
					FieldUtil.createHtmlListField("aaa-aaa"),
					(Function<String, JsonObject>) ((path) -> new JsonObject()
						.put("term", new JsonObject()
							.put(path + ".test", "aaa-aaa"))),
					noSchemaIndex,
					noFieldIndex
				});
				params.add(new Object[] {
					FieldUtil.createListFieldSchema(DYNAMIC_FIELD_NAME, "html"),
					new JsonObject()
						.put("test", new JsonObject()
							.put("type", "text")
							.put("index", true)),
					FieldUtil.createHtmlListField("a very cool test string"),
					(Function<String, JsonObject>) ((path) -> new JsonObject()
						.put("match", new JsonObject()
							.put(path + ".test", "cool"))),
					noSchemaIndex,
					noFieldIndex
				});
			}
		}
		return params;
		// @formatter:on
	}

	@Parameter(0)
	public FieldSchema schemaField;

	@Parameter(1)
	public JsonObject fieldMapping;

	@Parameter(2)
	public Field nodeField;

	@Parameter(3)
	public Function<String, JsonObject> searchQuery;

	@Parameter(4)
	public Boolean noSchemaIndex;

	@Parameter(5)
	public Boolean noFieldIndex;

	/**
	 * The created dummy schema in the preparations. Used to get the ES-Mapping of it.
	 */
	private SchemaResponse schema;

	@Before
	public void createDummySchemas() {
		// Create and assign a dummy microschema
		MicroschemaCreateRequest createMicroschema = new MicroschemaCreateRequest();
		createMicroschema.setName(MICROSCHEMA_NAME);
		createMicroschema.addField(this.schemaField.setElasticsearch(this.fieldMapping).setNoIndex(noFieldIndex));
		createMicroschema.setNoIndex(noSchemaIndex);
		MicroschemaResponse microschema = call(() -> client().createMicroschema(createMicroschema));
		call(() -> client().assignMicroschemaToProject(PROJECT_NAME, microschema.getUuid()));

		// Create and assign a dummy schema with the microschema as field
		SchemaCreateRequest createSchema = new SchemaCreateRequest();
		createSchema.setName(SCHEMA_NAME);
		createSchema.addField(
			FieldUtil.createMicronodeFieldSchema(MICRONODE_FIELD_NAME).setAllowedMicroSchemas(MICROSCHEMA_NAME));
		createSchema.addField(FieldUtil.createListFieldSchema(MICRONODE_LIST_FIELD_NAME, "micronode").setAllowedSchemas(
			MICROSCHEMA_NAME));
		this.schema = call(() -> client().createSchema(createSchema));
		call(() -> client().assignSchemaToProject(PROJECT_NAME, this.schema.getUuid()));

		waitForSearchIdleEvent();
	}

	@Test
	public void testMicroschemaCustomMappingJson() {
		NodeIndexHandler handler = mesh().indexHandlerRegistry().getNodeIndexHandler();
		NodeContainerMappingProvider provider = handler.getMappingProvider();
		ComplianceMode mode = options().getSearchOptions().getComplianceMode();

		tx(() -> {
			HibBranch branch = latestBranch();
			JsonObject schemaMapping = provider.getMapping(this.schema, branch, null).get();
			if (mode == ComplianceMode.ES_6) {
				schemaMapping = schemaMapping.getJsonObject(DEFAULT_TYPE);
			}
			assertNotNull(schemaMapping);
			JsonObject fieldsMapping = schemaMapping
				.getJsonObject("properties")
				.getJsonObject("fields");
			assertNotNull(fieldsMapping);
			JsonObject microschemaMapping = fieldsMapping
				.getJsonObject("properties")
				.getJsonObject(MICRONODE_FIELD_NAME)
				.getJsonObject("properties")
				.getJsonObject("fields-" + MICROSCHEMA_NAME);
			if (noSchemaIndex) {
				assertNull(microschemaMapping);
			} else {
				assertNotNull(microschemaMapping);
				JsonObject fieldMapping = microschemaMapping.getJsonObject("properties").getJsonObject(DYNAMIC_FIELD_NAME);

				if (noFieldIndex) {
					assertNull(fieldMapping);
				} else {
					assertEquals(this.fieldMapping, fieldMapping.getJsonObject("fields"));
				}
			}
		});
	}

	@Test
	public void testMicroschemaCustomMappingSearch() {
		String searchPath = "fields." + MICRONODE_FIELD_NAME + ".fields-" + MICROSCHEMA_NAME + "." + DYNAMIC_FIELD_NAME;

		tx(() -> {
			// Create a node with the newly created (Micro-)Schema
			NodeCreateRequest createNode = new NodeCreateRequest();
			createNode.setSchemaName(SCHEMA_NAME);
			createNode.setLanguage("en");
			createNode.setParentNodeUuid(project().getBaseNode().getUuid());
			createNode.getFields().put(MICRONODE_FIELD_NAME,
				FieldUtil.createMicronodeField(MICROSCHEMA_NAME, new Tuple<>(DYNAMIC_FIELD_NAME, this.nodeField)));
			NodeResponse node = call(() -> client().createNode(PROJECT_NAME, createNode));

			waitForSearchIdleEvent();

			// Search for the node
			NodeListResponse response = call(() -> client().searchNodes(project().getName(),
				new JsonObject().put("query", this.searchQuery.apply(searchPath)).encode()));

			if (noFieldIndex || noSchemaIndex) {
				// Expect the search to return a single element
				assertEquals(0, response.getMetainfo().getTotalCount());
			} else {
				// Expect the search to return a single element
				assertEquals(1, response.getMetainfo().getTotalCount());

				// Check if the node is the one we expected
				NodeResponse responseNode = response.getData().get(0);
				assertEquals(SCHEMA_NAME, responseNode.getSchema().getName());
				assertEquals(node.getUuid(), responseNode.getUuid());
			}
		});
	}

	@Test
	public void testNestedMicroschemaCustomMappingSearch() {
		String searchPath = "fields." + MICRONODE_LIST_FIELD_NAME + ".fields-" + MICROSCHEMA_NAME + "."
			+ DYNAMIC_FIELD_NAME;

		tx(() -> {
			// Create a node with the newly created (Micro-)Schema
			NodeCreateRequest createNode = new NodeCreateRequest();
			createNode.setSchemaName(SCHEMA_NAME);
			createNode.setLanguage("en");
			createNode.setParentNodeUuid(project().getBaseNode().getUuid());
			createNode.getFields().put(MICRONODE_LIST_FIELD_NAME, FieldUtil.createMicronodeListField(
				FieldUtil.createMicronodeField(MICROSCHEMA_NAME, new Tuple<>(DYNAMIC_FIELD_NAME, this.nodeField))));
			NodeResponse node = call(() -> client().createNode(PROJECT_NAME, createNode));

			JsonObject query = new JsonObject().put(
				"query",
				new JsonObject().put(
					"nested",
					new JsonObject()
						.put("path", "fields." + MICRONODE_LIST_FIELD_NAME)
						.put("query", this.searchQuery.apply(searchPath))));

			waitForSearchIdleEvent();
			// Search for the node
			NodeListResponse response = call(() -> client().searchNodes(project().getName(), query.encode()));
			if (noFieldIndex || noSchemaIndex) {
				// Expect no result
				assertEquals(0, response.getMetainfo().getTotalCount());
			} else {
				// Expect the search to return a single element
				assertEquals(1, response.getMetainfo().getTotalCount());

				// Check if the node is the one we expected
				NodeResponse responseNode = response.getData().get(0);
				assertEquals(SCHEMA_NAME, responseNode.getSchema().getName());
				assertEquals(node.getUuid(), responseNode.getUuid());
			}
		});
	}
}
