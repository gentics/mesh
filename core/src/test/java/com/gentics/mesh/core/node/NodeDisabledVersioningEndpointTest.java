package com.gentics.mesh.core.node;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class NodeDisabledVersioningEndpointTest extends AbstractMeshTest {

	private static final String SCHEMA_NAME = "test_schema_no_versioning";
	private static final String NODE_NAME = "test-node";
	private static final String NODE_LANGUAGE = "en";
	private static final List<String> STRING_LIST = Arrays.asList("foo", "bar", "doo");

	private static boolean schemaCreated = false;

	private String nodeUuid;
	private String parentPath;

	@Before
	public void setup() {
		if (!schemaCreated) {
			SchemaCreateRequest rq = new SchemaCreateRequest()
				.setName(SCHEMA_NAME)
				.setDisplayField("name")
				.setSegmentField("name")
				.setVersioning(false);
			List<FieldSchema> fields = rq.getFields();

			fields.add(new StringFieldSchemaImpl().setName("name").setRequired(true));
			fields.add(new ListFieldSchemaImpl().setListType("string").setName("list"));

			client().createSchema(rq).toSingle()
				.map(SchemaResponse::getUuid)
				.flatMapCompletable(schemaUuid -> client().assignSchemaToProject(PROJECT_NAME, schemaUuid).toCompletable())
				.blockingAwait();

			schemaCreated = true;
		}

		parentPath = call(() -> client().findNodeByUuid(PROJECT_NAME, folderUuid())).getPath();

		NodeCreateRequest rq = new NodeCreateRequest()
			.setSchemaName(SCHEMA_NAME)
			.setLanguage(NODE_LANGUAGE)
			.setParentNodeUuid(folderUuid());
		FieldMap fields = rq.getFields();

		fields.put("name", new StringFieldImpl().setString(NODE_NAME));
		fields.put("list", new StringFieldListImpl().setItems(STRING_LIST));

		nodeUuid = call(() -> client().createNode(PROJECT_NAME, rq)).getUuid();
		System.out.println("Created test node: " + nodeUuid);
	}

	@Test
	public void testCreatedNodePublished() {
		// No need for assertions, this call will fail if the node is not found.
		call(() -> client().findNodeByUuid(
			PROJECT_NAME,
			nodeUuid,
			new VersioningParametersImpl().setVersion("published")));
	}

	// WIP
	@Test
	public void testUpdateNode() {
		NodeUpdateRequest rq = new NodeUpdateRequest()
			.setLanguage(NODE_LANGUAGE);
		FieldMap fields = rq.getFields();
		List<String> list = new ArrayList<>(STRING_LIST);

		for (int ii = 1; ii <= 10; ii++) {
			list.add(String.format("item-%02d", ii));

			fields.put("name", new StringFieldImpl().setString(String.format("%s-%02d", NODE_NAME, ii)));
			fields.put("list", new StringFieldListImpl().setItems(list));

			NodeResponse rs = call(() -> client().updateNode(PROJECT_NAME, nodeUuid, rq));

			System.out.println(String.format("Update #%02d", ii));
			System.out.println(rs.toJson());
			System.out.println();

			NodeGraphFieldContainer container = project().findNode(nodeUuid).getGraphFieldContainer("en", initialBranchUuid(), ContainerType.PUBLISHED);

			rs = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().setVersion("draft")));

			assertThat(rs)
				.as(String.format("Node after update #%02d", ii))
				.hasVersion(String.format("%d.0", ii + 1));

			String path = String.format("%s/%s-%02d", parentPath, NODE_NAME, ii);

			rs = call(() -> client().webroot(PROJECT_NAME, path)).getNodeResponse();

			System.out.println(String.format("Loaded via webroot #%02d", ii));
			System.out.println(rs.toJson());
			System.out.println();
		}
	}
}
