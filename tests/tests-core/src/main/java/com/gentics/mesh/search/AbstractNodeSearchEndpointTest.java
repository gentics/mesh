package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.util.IndexOptionHelper;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public abstract class AbstractNodeSearchEndpointTest extends AbstractMultiESTest {

	public AbstractNodeSearchEndpointTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	/**
	 * Do the search with the given set of expected languages and assert correctness of the result.
	 *
	 * @param expectedLanguages
	 * @throws InterruptedException
	 * @throws JSONException
	 */
	protected void searchWithLanguages(String... expectedLanguages) throws Exception {
		recreateIndices();

		String uuid = db().tx(() -> content("concorde").getUuid());

		NodeListResponse response = call(
			() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "concorde"),
				new PagingParametersImpl().setPage(1).setPerPage(100L),
				new NodeParametersImpl().setLanguages(expectedLanguages), new VersioningParametersImpl().draft()));
		assertEquals("Check # of returned nodes", expectedLanguages.length, response.getData().size());
		assertEquals("Check total count", expectedLanguages.length, response.getMetainfo().getTotalCount());

		Set<String> foundLanguages = new HashSet<>();
		for (NodeResponse nodeResponse : response.getData()) {
			assertEquals("Check uuid of found node", uuid, nodeResponse.getUuid());
			foundLanguages.add(nodeResponse.getLanguage());
		}

		Set<String> notFound = new HashSet<>(Arrays.asList(expectedLanguages));
		notFound.removeAll(foundLanguages);
		assertTrue("Did not find nodes in expected languages: " + notFound, notFound.isEmpty());

		Set<String> unexpected = new HashSet<>(foundLanguages);
		unexpected.removeAll(Arrays.asList(expectedLanguages));
		assertTrue("Found nodes in unexpected languages: " + unexpected, unexpected.isEmpty());
	}

	protected void addNumberSpeedFieldToOneNode(Number number) {
		HibNode node = tx(() -> content("concorde"));
		tx(() -> {
			SchemaVersionModel schema = content("concorde").getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new NumberFieldSchemaImpl().setName("speed"));
			content("concorde").getSchemaContainer().getLatestVersion().setSchema(schema);
			actions().updateSchemaVersion(content("concorde").getSchemaContainer().getLatestVersion());
		});

		NodeResponse response = call(() -> client().findNodeByUuid(projectName(), node.getUuid()));
		NodeUpdateRequest request = response.toRequest();
		request.getFields().put("speed", new NumberFieldImpl().setNumber(number));
		call(() -> client().updateNode(projectName(), node.getUuid(), request));
	}

	/**
	 * Add a micronode field to the tested content
	 */
	protected void addMicronodeField() {
		HibNode node = tx(() -> {
			HibNode nodeTmp = content("concorde");

			SchemaModel schema = nodeTmp.getSchemaContainer().getLatestVersion().getSchema();
			MicronodeFieldSchemaImpl vcardFieldSchema = new MicronodeFieldSchemaImpl();
			vcardFieldSchema.setName("vcard");
			vcardFieldSchema.setAllowedMicroSchemas(new String[] { "vcard" });
			schema.addField(vcardFieldSchema);
			actions().updateSchemaVersion(nodeTmp.getSchemaContainer().getLatestVersion());
			return nodeTmp;
		});

		NodeUpdateRequest request = tx(() -> {
			NodeResponse response = call(() -> client().findNodeByUuid(projectName(), node.getUuid()));
			NodeUpdateRequest requestTmp = response.toRequest();

			MicronodeResponse field = new MicronodeResponse();
			field.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
			field.getFields().put("firstName", new StringFieldImpl().setString("Mickey"));
			field.getFields().put("lastName", new StringFieldImpl().setString("Mouse"));
			requestTmp.getFields().put("vcard", field);

			return requestTmp;
		});

		call(() -> client().updateNode(projectName(), node.getUuid(), request));
	}

	/**
	 * Add a micronode list field to the tested content
	 */
	protected void addMicronodeListField() {
		HibNode node = tx(() -> content("concorde"));

		// Update the schema
		tx(() -> {
			SchemaModel schema = content("concorde").getSchemaContainer().getLatestVersion().getSchema();
			ListFieldSchema vcardListFieldSchema = new ListFieldSchemaImpl();
			vcardListFieldSchema.setName("vcardlist");
			vcardListFieldSchema.setListType("micronode");
			vcardListFieldSchema.setAllowedSchemas(new String[] { "vcard" });
			schema.addField(vcardListFieldSchema);
			actions().updateSchemaVersion(content("concorde").getSchemaContainer().getLatestVersion());
		});

		FieldList<MicronodeField> listField = new MicronodeFieldListImpl();
		for (Tuple<String, String> testdata : Arrays.asList(Tuple.tuple("Mickey", "Mouse"), Tuple.tuple("Donald", "Duck"))) {
			MicronodeResponse field = new MicronodeResponse();
			field.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
			field.getFields().put("firstName", new StringFieldImpl().setString(testdata.v1()));
			field.getFields().put("lastName", new StringFieldImpl().setString(testdata.v2()));
			listField.add(field);
		}

		NodeResponse response = call(() -> client().findNodeByUuid(projectName(), node.getUuid()));
		NodeUpdateRequest request = response.toRequest();
		request.getFields().put("vcardlist", listField);
		request.setLanguage(english());
		call(() -> client().updateNode(projectName(), node.getUuid(), request));

		// create an empty vcard list field
		NodeUpdateRequest germanRequest = call(() -> client().findNodeByUuid(projectName(), node.getUuid(), new NodeParametersImpl().setLanguages("de"))).toRequest();
		germanRequest.getFields().put("vcardlist", new MicronodeFieldListImpl());
		germanRequest.setLanguage(german());
		call(() -> client().updateNode(projectName(), node.getUuid(), germanRequest));
	}

	/**
	 * Add a node list field to the tested content
	 */
	protected void addNodeListField() {
		HibNode node = tx(() -> content("concorde"));

		// Update the schema
		tx(() -> {
			SchemaModel schema = content("concorde").getSchemaContainer().getLatestVersion().getSchema();
			ListFieldSchema nodeListFieldSchema = new ListFieldSchemaImpl();
			nodeListFieldSchema.setName("nodelist");
			nodeListFieldSchema.setListType("node");
			nodeListFieldSchema.setAllowedSchemas(schema.getName());
			schema.addField(nodeListFieldSchema);
			actions().updateSchemaVersion(content("concorde").getSchemaContainer().getLatestVersion());
		});

		// create a non-empty list for the english version
		NodeResponse response = call(() -> client().findNodeByUuid(projectName(), node.getUuid()));
		NodeUpdateRequest request = response.toRequest();
		NodeFieldListImpl fieldList = new NodeFieldListImpl();
		fieldList.add(new NodeFieldListItemImpl().setUuid(node.getUuid()));
		request.getFields().put("nodelist", fieldList);
		call(() -> client().updateNode(projectName(), node.getUuid(), request));

		// create an empty list for the german version
		NodeUpdateRequest germanRequest = call(() -> client().findNodeByUuid(projectName(), node.getUuid(), new NodeParametersImpl().setLanguages("de"))).toRequest();
		germanRequest.getFields().put("nodelist", new NodeFieldListImpl());
		call(() -> client().updateNode(projectName(), node.getUuid(), germanRequest));
	}

	/**
	 * Generate the JSON for a searched in the nested field vcardlist
	 * 
	 * @param firstName
	 *            firstname to search for
	 * @param lastName
	 *            lastname to search for
	 * @return search JSON
	 * @throws IOException
	 */
	protected String getNestedVCardListSearch(String firstName, String lastName) throws IOException {
		JsonObject request = new JsonObject(
			"{\"query\":{\"nested\":{\"path\":\"fields.vcardlist\",\"query\":{\"bool\":{\"must\":[{\"match\":{\"fields.vcardlist.fields-vcard.firstName\":\"Mickey\"}},{\"match\":{\"fields.vcardlist.fields-vcard.lastName\":\"Duck\"}}]}}}}}\n");
		JsonArray must = request.getJsonObject("query").getJsonObject("nested").getJsonObject("query").getJsonObject("bool").getJsonArray("must");
		must.getJsonObject(0).getJsonObject("match").put("fields.vcardlist.fields-vcard.firstName", firstName);
		must.getJsonObject(1).getJsonObject("match").put("fields.vcardlist.fields-vcard.lastName", lastName);
		return request.toString();
	}

	protected void addRawToSchemaField() {
		// Update the schema and enable the addRaw field
		String schemaUuid = tx(() -> content().getSchemaContainer().getUuid());
		SchemaUpdateRequest request = tx(() -> JsonUtil.readValue(content().getSchemaContainer().getLatestVersion().getJson(),
			SchemaUpdateRequest.class));
		request.getField("teaser").setElasticsearch(IndexOptionHelper.getRawFieldOption());

		grantAdmin();
		waitForJob(() -> {
			call(() -> client().updateSchema(schemaUuid, request));
		});
		revokeAdmin();
	}

}
