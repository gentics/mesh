package com.gentics.mesh.core.schema;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.function.Predicate;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;


// TODO Also test other possible field types
@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
@RunWith(VertxUnitRunner.class)
public class SchemaAllowEndpointTest extends AbstractMeshTest {

    private static final String ALLOW_SCHEMA_NAME = "allow_schema";
    private static final String ALLOW_FIELD_NAME = "allow_schema";

    @Test
    public void testNodeLists(TestContext context) {
        createReferenceSchema(new ListFieldSchemaImpl().setListType("node").setAllowedSchemas(ALLOW_SCHEMA_NAME).setName(ALLOW_FIELD_NAME))
            .toCompletable().andThen(createFolder("test"))
            .map(this::createNodeList)
            .flatMap(this::createAllowSchemaNode)
            .subscribe(expectMeshRestError(context, "node_error_reference_not_allowed_schema"));
    }

    @Test
    public void testNodeField(TestContext context) {
        createReferenceSchema(new NodeFieldSchemaImpl().setAllowedSchemas(ALLOW_SCHEMA_NAME).setName(ALLOW_FIELD_NAME))
            .toCompletable().andThen(createFolder("test"))
            .map(this::createNodeField)
            .flatMap(this::createAllowSchemaNode)
            .subscribe(expectMeshRestError(context, "node_error_reference_not_allowed_schema"));
    }

    @Test
    public void testStringField(TestContext context) {
        createReferenceSchema(new StringFieldSchemaImpl().setAllowedValues("test123").setName(ALLOW_FIELD_NAME))
            .toCompletable().andThen(createAllowSchemaNode(new StringFieldImpl().setString("not_valid")))
            .subscribe(expectMeshRestError(context, "node_error_invalid_string_field_value"));
    }

    @Test
    public void testStringFieldList(TestContext context) {
        createReferenceSchema(new ListFieldSchemaImpl().setListType("string").setAllowedSchemas("test123").setName(ALLOW_FIELD_NAME))
            .toCompletable().andThen(createAllowSchemaNode(new StringFieldListImpl().setItems(Arrays.asList("not_valid"))))
            .subscribe(expectMeshRestError(context, "node_error_invalid_string_field_value"));
    }

    private <T> SingleObserver<T> expectMeshRestError(TestContext context, Predicate<MeshRestClientMessageException> exceptionPredicate) {
        Async async = context.async();

        return new SingleObserver<T>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(T nodeResponse) {
                context.fail("Expected MeshRestClientMessageException");
            }

            @Override
            public void onError(Throwable e) {
                if (!(e instanceof MeshRestClientMessageException)) {
                    context.fail(e);
                    return;
                }
                MeshRestClientMessageException error = (MeshRestClientMessageException) e;
                boolean ok;
                try {
                    ok = exceptionPredicate.test(error);
                } catch (Throwable e2) {
                    context.fail(e2);
                    return;
                }
                if (ok) {
                    async.complete();
                } else {
                    context.fail(error);
                }
            }
        };
    }

    private <T> SingleObserver<T> expectMeshRestError(TestContext context, String internalMessage) {
        return expectMeshRestError(context, error -> error.getResponseMessage().getInternalMessage().equals(internalMessage));
    }

    private NodeFieldList createNodeList(NodeResponse response) {
        NodeFieldListImpl field = new NodeFieldListImpl();
        field.setItems(Arrays.asList(new NodeFieldListItemImpl().setUuid(response.getUuid())));
        return field;
    }

    private NodeField createNodeField(NodeResponse response) {
        return new NodeFieldImpl().setUuid(response.getUuid());
    }

    private Single<NodeResponse> createAllowSchemaNode(Field field) {
        return getRootNodeUuid().flatMap(parentUuid -> {
            NodeCreateRequest request = new NodeCreateRequest();
            request.setSchemaName("allow_schema");
            request.setLanguage("en");
            request.setParentNodeUuid(parentUuid);
            FieldMap fieldMap = new FieldMapImpl();
            fieldMap.put(ALLOW_FIELD_NAME, field);
            request.setFields(fieldMap);
            return client().createNode(PROJECT_NAME, request).toSingle();
        });
    }

    private Single<NodeResponse> createFolder(String name) {
        return getRootNodeUuid().flatMap(parentUuid -> {
            NodeCreateRequest request = new NodeCreateRequest();
            request.setSchemaName("folder");
            request.setLanguage("en");
            request.setParentNodeUuid(parentUuid);
            FieldMap fieldMap = new FieldMapImpl();
            fieldMap.put("name", new StringFieldImpl().setString(name));
            request.setFields(fieldMap);
            return client().createNode(PROJECT_NAME, request).toSingle();
        });
    }

    private Single<SchemaResponse> createReferenceSchema(FieldSchema...fields) {
        SchemaCreateRequest schemaCreateRequest = new SchemaCreateRequest();
        schemaCreateRequest.setName(ALLOW_SCHEMA_NAME).setFields(Arrays.asList(fields));
        return client().createSchema(schemaCreateRequest).toSingle()
            .flatMap(response ->
                client().assignSchemaToProject(PROJECT_NAME, response.getUuid()).toSingle());
    }

    private Single<String> getRootNodeUuid() {
        return client().findProjectByName(PROJECT_NAME).toSingle().map(project -> project.getRootNode().getUuid());
    }
}
