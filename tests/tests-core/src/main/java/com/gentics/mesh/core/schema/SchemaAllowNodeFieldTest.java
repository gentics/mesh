package com.gentics.mesh.core.schema;

import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class SchemaAllowNodeFieldTest extends AbstractMeshTest {
    private String nodeUuid;
    private String testNodeUuid;

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
    
    private void createTestNode() {
        NodeCreateRequest nodeReq = new NodeCreateRequest();
        nodeReq.setLanguage("en");
        nodeReq.setSchemaName("test");
        nodeReq.setParentNodeUuid(nodeUuid);
        NodeResponse createdNode = call(() -> client().createNode(PROJECT_NAME, nodeReq));

        testNodeUuid = createdNode.getUuid();
    }
    
    private void createNodeAndExpectFailure(Field field) {
        NodeCreateRequest req = new NodeCreateRequest();
        req.setLanguage("en");
        req.setSchemaName("test");
        req.setParentNodeUuid(nodeUuid);
        FieldMapImpl fieldMap = new FieldMapImpl();
        fieldMap.put("testField", field);
        req.setFields(fieldMap);

        call(() -> client().createNode(PROJECT_NAME, req), BAD_REQUEST,"node_error_invalid_schema_field_value","testField","test");
    }

    private void runTest(Field nodeField) {
        createNode(nodeField);
    }

    private void runTestAndExpectFailure(Field nodeField) {
        createNodeAndExpectFailure(nodeField);
    }

    @Test
    public void node() {
        createSchema(new NodeFieldSchemaImpl().setAllowedSchemas("test"));
        createTestNode();
        runTest(
                new NodeFieldImpl().setUuid(testNodeUuid));
    }

    @Test
    public void nodeNotAllowed() {
        createSchema(new NodeFieldSchemaImpl().setAllowedSchemas("test2"));
        createTestNode();
        runTestAndExpectFailure(
                new NodeFieldImpl().setUuid(testNodeUuid));
    }

    @Test
    public void nodeList() {
        createSchema(new ListFieldSchemaImpl().setListType("node").setAllowedSchemas("test"));
        createTestNode();
        runTest(
                new NodeFieldListImpl().setItems(Collections.singletonList(new NodeFieldListItemImpl().setUuid(testNodeUuid))));
    }

    @Test
    public void nodeListNotAllowed() {
        createSchema(new ListFieldSchemaImpl().setListType("node").setAllowedSchemas("test2"));
        createTestNode();
        runTestAndExpectFailure(
                new NodeFieldListImpl().setItems(Collections.singletonList(new NodeFieldListItemImpl().setUuid(testNodeUuid))));
    }
}
