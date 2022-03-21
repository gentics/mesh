package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

/**
 * Tests for the following issues:
 * <ul>
 *     <li><a href="https://github.com/gentics/mesh/issues/979">https://github.com/gentics/mesh/issues/979</a></li>
 *     <li><a href="https://github.com/gentics/mesh/issues/1014">https://github.com/gentics/mesh/issues/1014</a></li>
 *     <li><a href="https://github.com/gentics/mesh/issues/1025">https://github.com/gentics/mesh/issues/1025</a></li>
 * </ul>
 *
 */
@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class MicroschemaMigrationTest extends AbstractMeshTest {

	private MicroschemaResponse phoneMicroschema;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void migrateNewNode() {
		grantAdmin();
		createTestSchema();
		createMicroschemas();

		NodeResponse testNode = createTestNode();
		publishNode(testNode);
		assertVersions(testNode, "PD(1.0)=>I(0.1)");
		updateMicroschema();
		assertVersions(testNode, "PD(2.0)=>I(0.1)");
	}

	@Test
	public void migrateUpdatedNode() {
		grantAdmin();
		createTestSchema();
		createMicroschemas();

		NodeResponse testNode = createTestNode();
		publishNode(testNode);
		assertVersions(testNode, "PD(1.0)=>I(0.1)");
		testNode = addLocation(testNode, location("aut", "salzburg"));
		assertVersions(testNode, "PD(2.0)=>I(0.1)");
		testNode = addLocation(testNode, location("aut", "innsbruck"));
		assertVersions(testNode, "PD(3.0)=>I(0.1)");
		testNode = addLocation(testNode, location("aut", "klagenfurt"));
		assertVersions(testNode, "PD(4.0)=>I(0.1)");
		updateMicroschema();
		assertVersions(testNode, "PD(5.0)=>I(0.1)");
	}

	@Test
	public void migrateNewNodeNoPurge() {
		grantAdmin();
		createTestSchema(false);
		createMicroschemas();

		NodeResponse testNode = createTestNode();
		publishNode(testNode);
		assertVersions(testNode, "PD(1.0)=>I(0.1)");
		updateMicroschema();
		assertVersions(testNode, "PD(2.0)=>(1.0)=>I(0.1)");
	}

	@Test
	public void migrateUpdatedNodeNoPurge() {
		grantAdmin();
		createTestSchema(false);
		createMicroschemas();

		NodeResponse testNode = createTestNode();
		publishNode(testNode);
		assertVersions(testNode, "PD(1.0)=>I(0.1)");
		testNode = addLocation(testNode, location("aut", "salzburg"));
		assertVersions(testNode, "PD(2.0)=>(1.1)=>(1.0)=>I(0.1)");
		testNode = addLocation(testNode, location("aut", "innsbruck"));
		assertVersions(testNode, "PD(3.0)=>(2.1)=>(2.0)=>(1.1)=>(1.0)=>I(0.1)");
		testNode = addLocation(testNode, location("aut", "klagenfurt"));
		assertVersions(testNode, "PD(4.0)=>(3.1)=>(3.0)=>(2.1)=>(2.0)=>(1.1)=>(1.0)=>I(0.1)");
		updateMicroschema();
		assertVersions(testNode, "PD(5.0)=>(4.0)=>(3.1)=>(3.0)=>(2.1)=>(2.0)=>(1.1)=>(1.0)=>I(0.1)");
	}

	@Test
	public void migrateModifiedNode() {
		grantAdmin();
		createTestSchema(false);
		createMicroschemas();

		NodeResponse testNode = createTestNode();
		testNode = addLocation(testNode, location("aut", "eisenstadt"), true);
		assertVersions(testNode, "PD(1.0)=>(0.2)=>I(0.1)");
		testNode = addLocation(testNode, location("aut", "bregenz"), false);
		assertVersions(testNode, "D(1.1)=>P(1.0)=>(0.2)=>I(0.1)");
		updateMicroschema();
		assertVersions(testNode, "D(2.1)=>P(2.0)=>(1.1)=>(1.0)=>(0.2)=>I(0.1)");

		assertLocations(testNode, "0.1", location("aut", "vienna"), location("aut", "graz"), location("aut", "linz"));
		assertLocations(testNode, "0.2", location("aut", "vienna"), location("aut", "graz"), location("aut", "linz"),
				location("aut", "eisenstadt"));
		assertLocations(testNode, "1.0", location("aut", "vienna"), location("aut", "graz"), location("aut", "linz"),
				location("aut", "eisenstadt"));
		assertLocations(testNode, "1.1", location("aut", "vienna"), location("aut", "graz"), location("aut", "linz"),
				location("aut", "eisenstadt"), location("aut", "bregenz"));
		assertLocations(testNode, "2.0", location("aut", "vienna"), location("aut", "graz"), location("aut", "linz"),
				location("aut", "eisenstadt"));
		assertLocations(testNode, "2.1", location("aut", "vienna"), location("aut", "graz"), location("aut", "linz"),
				location("aut", "eisenstadt"), location("aut", "bregenz"));
	}

	public void updateMicroschema() {
		MicroschemaUpdateRequest updateSchemaRequest = addRandomField(phoneMicroschema);
		waitForLatestJob(() -> client().updateMicroschema(phoneMicroschema.getUuid(), updateSchemaRequest).blockingAwait());
	}

	private NodeResponse createTestNode() {
		return client().createNode(PROJECT_NAME, new NodeCreateRequest()
			.setSchemaName("testSchema")
			.setParentNodeUuid(getProject().getRootNode().getUuid())
			.setLanguage("en")
			.setFields(FieldMap.of(
				"name", StringField.of("testNode"),
				"phone", phoneNumber("+43", "1234567"),
				"locations", new MicronodeFieldListImpl().setItems(Arrays.asList(
					location("aut", "vienna"),
					location("aut", "graz"),
					location("aut", "linz")
			))
		))).blockingGet();
	}

	private NodeResponse addLocation(NodeResponse node, MicronodeResponse location) {
		return addLocation(node, location, true);
	}

	private NodeResponse addLocation(NodeResponse node, MicronodeResponse location, boolean publish) {
		NodeUpdateRequest updateRequest = node.toRequest();
		MicronodeFieldList locations = updateRequest.getFields().getMicronodeFieldList("locations");
		locations.getItems().add(location);
		updateRequest.getFields().put("locations", locations);
		NodeResponse nodeResponse = client().updateNode(PROJECT_NAME, node.getUuid(), updateRequest).blockingGet();
		if (publish) {
			publishNode(nodeResponse);
		}
		return client().findNodeByUuid(PROJECT_NAME, nodeResponse.getUuid()).blockingGet();
	}

	private void assertLocations(NodeResponse node, String version, MicronodeResponse...expected) {
		NodeResponse nodeResponse = client()
				.findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().setVersion(version))
				.blockingGet();
		assertThat(nodeResponse.getFields().getMicronodeFieldList("locations")).as("Locations of version " + version).isNotNull();
		assertThat(nodeResponse.getFields().getMicronodeFieldList("locations").getItems())
				.as("Locations of version " + version).usingElementComparator(this::compareLocations).containsOnly(expected);
	}

	private int compareLocations(MicronodeField a, MicronodeField b) {
		int cmp = getValue(a.getFields(), "country").compareTo(getValue(b.getFields(), "country"));
		if (cmp != 0) {
			return cmp;
		} else {
			return getValue(a.getFields(), "city").compareTo(getValue(b.getFields(), "city"));
		}
	}

	private String getValue(FieldMap map, String key) {
		StringFieldImpl field = map.getStringField(key);
		return field != null ? field.toString() : null;
	}

	private MicronodeResponse phoneNumber(String countryCode, String number) {
		MicronodeResponse micronodeResponse = new MicronodeResponse();
		micronodeResponse.setMicroschema(new MicroschemaReferenceImpl().setName("phonenumber"));
		FieldMap fields = micronodeResponse.getFields();
		fields.put("countrycode", StringField.of(countryCode));
		fields.put("number", StringField.of(number));
		return micronodeResponse;
	}

	private MicronodeResponse location(String country, String city) {
		MicronodeResponse micronodeResponse = new MicronodeResponse();
		micronodeResponse.setMicroschema(new MicroschemaReferenceImpl().setName("location"));
		FieldMap fields = micronodeResponse.getFields();
		fields.put("country", StringField.of(country));
		fields.put("city", StringField.of(city));
		return micronodeResponse;
	}

	private void createTestSchema() {
		createTestSchema(true);
	}

	private void createTestSchema(boolean autoPurge) {
		SchemaCreateRequest createRequest = new SchemaCreateRequest();
		createRequest.setName("testSchema");
		createRequest.setAutoPurge(autoPurge);
		createRequest.setFields(Arrays.asList(
			new StringFieldSchemaImpl().setName("name"),
			new MicronodeFieldSchemaImpl().setName("phone"),
			new ListFieldSchemaImpl().setListType("micronode").setName("locations")
		));
		SchemaResponse schemaResponse = client().createSchema(createRequest).blockingGet();
		client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()).blockingAwait();
	}

	private void createMicroschemas() {
		phoneMicroschema = createAndAssign(new MicroschemaCreateRequest()
			.setName("phonenumber")
			.setFields(Arrays.asList(
				new StringFieldSchemaImpl().setName("countrycode"),
				new StringFieldSchemaImpl().setName("number")
			))
		);

		createAndAssign(new MicroschemaCreateRequest()
			.setName("location")
			.setFields(Arrays.asList(
				new StringFieldSchemaImpl().setName("country"),
				new StringFieldSchemaImpl().setName("city")
			))
		);
	}

	private MicroschemaResponse createAndAssign(MicroschemaCreateRequest microschemaCreateRequest) {
		MicroschemaResponse microschemaResponse = client().createMicroschema(microschemaCreateRequest).blockingGet();
		client().assignMicroschemaToProject(PROJECT_NAME, microschemaResponse.getUuid()).blockingAwait();
		return microschemaResponse;
	}
}
