package com.gentics.mesh.core.schema;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import io.vertx.core.json.JsonObject;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(testSize = FULL, startServer = true)
public class SchemaUpdateEndpointNewFieldsTest extends AbstractMeshTest {

    @Test
    public void testCreateNewFieldWithElasticSearchProperties() {
        // 1. create schema with single field
        FieldSchema field1 = createFieldSchema("field1");
        field1.setElasticsearch(new JsonObject().put("test", "123"));
        SchemaResponse schema = createSchema(field1);

        // 2. create a new field with elastic search properties
        FieldSchema field2 = createFieldSchema("field2");
        JsonObject elasticSearch = new JsonObject().put("test", "123");
        field2.setElasticsearch(elasticSearch);

        // 3. add the field to the update request
        SchemaUpdateRequest schemaUpdateRequest = schema.toUpdateRequest();
        schemaUpdateRequest.getFields().add(field2);

        // 4. update the schema
        call(() -> client().updateSchema(schema.getUuid(), schemaUpdateRequest));

        // 5. make sure elastic search properties were saved
        SchemaResponse response = call(() -> client().findSchemaByUuid(schema.getUuid()));
        Assertions.assertThat(response.getField("field2", FieldSchema.class).getElasticsearch()).isEqualTo(elasticSearch);
    }

    private SchemaResponse createSchema(FieldSchema... schemas) {
        SchemaCreateRequest request = new SchemaCreateRequest();
        request.setName("test");
        List<FieldSchema> fields = Arrays.stream(schemas).collect(Collectors.toList());
        request.setFields(fields);

        return client().createSchema(request).blockingGet();
    }

    private FieldSchema createFieldSchema(String name) {
        FieldSchema schema = new StringFieldSchemaImpl();
        schema.setName(name);
        schema.setLabel(name);
        return schema;
    }
}
