package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.codehaus.jettison.json.JSONException;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;

public abstract class AbstractNodeSearchEndpointTest extends AbstractMeshTest {

	/**
	 * Do the search with the given set of expected languages and assert correctness of the result.
	 *
	 * @param expectedLanguages
	 * @throws InterruptedException
	 * @throws JSONException
	 */
	protected void searchWithLanguages(String... expectedLanguages) throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		String uuid = db().tx(() -> content("concorde").getUuid());

		NodeListResponse response = call(
				() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("concorde"), new PagingParametersImpl().setPage(1).setPerPage(100),
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
		Node node = content("concorde");
		SchemaModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
		schema.addField(new NumberFieldSchemaImpl().setName("speed"));
		node.getSchemaContainer().getLatestVersion().setSchema(schema);

		node.getLatestDraftFieldContainer(english()).createNumber("speed").setNumber(number);
	}

	/**
	 * Add a micronode field to the tested content
	 */
	protected void addMicronodeField() {
		Node node = content("concorde");

		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		MicronodeFieldSchemaImpl vcardFieldSchema = new MicronodeFieldSchemaImpl();
		vcardFieldSchema.setName("vcard");
		vcardFieldSchema.setAllowedMicroSchemas(new String[] { "vcard" });
		schema.addField(vcardFieldSchema);

		MicronodeGraphField vcardField = node.getLatestDraftFieldContainer(english()).createMicronode("vcard",
				microschemaContainers().get("vcard").getLatestVersion());
		vcardField.getMicronode().createString("firstName").setString("Mickey");
		vcardField.getMicronode().createString("lastName").setString("Mouse");
	}

	/**
	 * Add a micronode list field to the tested content
	 */
	protected void addMicronodeListField() {
		Node node = content("concorde");

		// Update the schema
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		ListFieldSchema vcardListFieldSchema = new ListFieldSchemaImpl();
		vcardListFieldSchema.setName("vcardlist");
		vcardListFieldSchema.setListType("micronode");
		vcardListFieldSchema.setAllowedSchemas(new String[] { "vcard" });
		schema.addField(vcardListFieldSchema);

		// Set the mapping for the schema
		meshDagger().nodeContainerIndexHandler().updateNodeIndexMapping(schema).await();

		MicronodeGraphFieldList vcardListField = node.getLatestDraftFieldContainer(english()).createMicronodeFieldList("vcardlist");
		for (Tuple<String, String> testdata : Arrays.asList(Tuple.tuple("Mickey", "Mouse"), Tuple.tuple("Donald", "Duck"))) {
			Micronode micronode = vcardListField.createMicronode();
			micronode.setSchemaContainerVersion(microschemaContainers().get("vcard").getLatestVersion());
			micronode.createString("firstName").setString(testdata.v1());
			micronode.createString("lastName").setString(testdata.v2());
		}

		// create an empty vcard list field
		node.getLatestDraftFieldContainer(german()).createMicronodeFieldList("vcardlist");
	}

	/**
	 * Add a node list field to the tested content
	 */
	protected void addNodeListField() {
		Node node = content("concorde");

		// Update the schema
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		ListFieldSchema nodeListFieldSchema = new ListFieldSchemaImpl();
		nodeListFieldSchema.setName("nodelist");
		nodeListFieldSchema.setListType("node");
		nodeListFieldSchema.setAllowedSchemas(schema.getName());
		schema.addField(nodeListFieldSchema);

		// Set the mapping for the schema
		meshDagger().nodeContainerIndexHandler().updateNodeIndexMapping(schema).await();

		// create a non-empty list for the english version
		NodeGraphFieldList nodeListField = node.getLatestDraftFieldContainer(english()).createNodeList("nodelist");
		nodeListField.addItem(nodeListField.createNode("testNode", node));

		// create an empty list for the german version
		node.getLatestDraftFieldContainer(german()).createNodeList("nodelist");
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
		return XContentFactory.jsonBuilder().startObject().startObject("query").startObject("nested").field("path", "fields.vcardlist")
				.startObject("query").startObject("bool").startArray("must").startObject().startObject("match")
				.field("fields.vcardlist.fields-vcard.firstName", firstName).endObject().endObject().startObject().startObject("match")
				.field("fields.vcardlist.fields-vcard.lastName", lastName).endObject().endObject().endArray().endObject().endObject().endObject()
				.endObject().endObject().string();
	}
}
