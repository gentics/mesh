package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.IndexOptions;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = true, testSize = FULL, startServer = true, startESServer = true)
public class NodeSearchEndpointFTest extends AbstractNodeSearchEndpointTest {

	@Test
	public void testSearchAndSort() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		String json = "{";
		json += "				\"sort\" : {";
		json += "			      \"created\" : {\"order\" : \"asc\"}";
		json += "			    },";
		json += "			    \"query\":{";
		json += "			        \"bool\" : {";
		json += "			            \"must\" : {";
		json += "			                \"term\" : { \"schema.name.raw\" : \"content\" }";
		json += "			            }";
		json += "			        }";
		json += "			    }";
		json += "			}";

		String search = json;
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, search, new VersioningParametersImpl().draft()));
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());

		long lastCreated = 0;
		for (NodeResponse nodeResponse : response.getData()) {
			Date date = Date.from(Instant.parse(nodeResponse.getCreated()));
			if (lastCreated > date.getTime()) {
				fail("Found entry that was not sorted by create timestamp. Last entry: {" + lastCreated + "} current entry: {"
						+ nodeResponse.getCreated() + "}");
			} else {
				lastCreated = date.getTime();
			}
			assertEquals("content", nodeResponse.getSchema().getName());
		}
	}

	@Test
	public void testSearchContent() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("the"),
				new PagingParametersImpl().setPage(1).setPerPage(2), new VersioningParametersImpl().draft()));
		assertEquals(1, response.getData().size());
		assertEquals(1, response.getMetainfo().getTotalCount());
		for (NodeResponse nodeResponse : response.getData()) {
			assertNotNull(nodeResponse);
			assertNotNull(nodeResponse.getUuid());
		}

	}

	/**
	 * Test searching for a node which is only persisted in the search index but no longer in the graph.
	 * 
	 * @throws InterruptedException
	 * @throws JSONException
	 */
	@Test
	public void testSearchMissingVertex() throws Exception {
		Node node = content("honda nr");
		try (Tx tx = tx()) {
			recreateIndices();
			node.remove();
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeListResponse response = call(() -> client().searchNodes(getSimpleQuery("the"), new PagingParametersImpl().setPage(1).setPerPage(2)));
			assertEquals(0, response.getData().size());
			assertEquals(0, response.getMetainfo().getTotalCount());
		}
	}

	/**
	 * Test searching a node after adding a micronode field by using the raw field.
	 * @throws Exception
	 */
	@Test
	public void testSearchNewMicroschema() throws Exception {
		tx(() -> group().addRole(roles().get("admin")));
		// 1. Create microschema
		MicroschemaCreateRequest microschema = new MicroschemaCreateRequest()
			.setName("test")
			.setFields(
				Arrays.asList(
					new StringFieldSchemaImpl().setName("searchme").setIndexOptions(new IndexOptions().setAddRaw(true))
				)
			);
		MicroschemaResponse microschemaResponse = call(() -> client().createMicroschema(microschema));

		// 2. Add micronode to existing schema
		SchemaResponse schema = call(() -> client().findSchemas(PROJECT_NAME)).getData().get(0);
		List<FieldSchema> fields = schema.getFields();
		fields.add(new MicronodeFieldSchemaImpl().setAllowedMicroSchemas("test").setName("test"));

		SchemaUpdateRequest updateRequest = new SchemaUpdateRequest()
			.setName(schema.getName())
			.setFields(fields);
		call(() -> client().updateSchema(schema.getUuid(), updateRequest));

		// 3. await migration
		for (;;) {
			JobListResponse jobs = call(() -> client().findJobs());
			if (jobs.getData().size() > 0) {
				if (jobs.getData().get(0).getStatus() == MigrationStatus.COMPLETED) {
					break;
				}
			}
			Thread.sleep(1000);
		}


		// 4. Add new node
		NodeCreateRequest createRequest = new NodeCreateRequest()
			.setSchema(new SchemaReferenceImpl().setName(schema.getName()))
			.setLanguage("en")
			.setParentNodeUuid(tx(() -> folder("2015").getUuid()));

		MicronodeResponse micronode = new MicronodeResponse().setMicroschema(new MicroschemaReferenceImpl().setUuid(microschemaResponse.getUuid()));
		micronode.getFields().put("searchme", new StringFieldImpl().setString("toBeSearched"));
		createRequest.getFields().put("test", micronode);
		NodeResponse createdNode = call(() -> client().createNode(PROJECT_NAME, createRequest));

		System.out.println("Good night!");
		Thread.sleep(12000000);
		String searchQuery = "{\n" +
			"  \"query\": {\n" +
			"    \"bool\": {\n" +
			"      \"must\": [\n" +
			"        {\n" +
			"          \"term\": {\n" +
			"            \"schema.name.raw\": \"" + schema.getName() + "\"\n" +
			"          }\n" +
			"        },\n" +
			"        {\n" +
			"          \"term\": {\n" +
			"            \"fields.test.fields-test.searchme.raw\": \"toBeSearched\"\n" +
			"          }\n" +
			"        }\n" +
			"      ]\n" +
			"    }\n" +
			"  }\n" +
			"}";

		NodeListResponse result = call(() -> client().searchNodes(searchQuery));
		assertEquals(1, result.getMetainfo().getTotalCount());
		assertEquals(createdNode.getUuid(), result.getData().get(0).getUuid());
	}
}
