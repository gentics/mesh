package com.gentics.mesh.core.field.s3binary;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.S3BinaryGraphField;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.S3BinaryField;
import com.gentics.mesh.core.rest.node.field.impl.S3BinaryFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.S3BinaryFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.mesh.util.VersionNumber;
import org.junit.Test;

import static org.junit.Assert.*;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE)
public class S3BinaryFieldTest extends AbstractFieldTest<S3BinaryFieldSchema> {

    private static final String S3_BINARY_FIELD = "s3binaryField";

    @Override
    protected S3BinaryFieldSchema createFieldSchema(boolean isRequired) {
        S3BinaryFieldSchema s3binaryFieldSchema = new S3BinaryFieldSchemaImpl();
        s3binaryFieldSchema.setName(S3_BINARY_FIELD);
        s3binaryFieldSchema.setRequired(isRequired);
        return s3binaryFieldSchema;
    }

    @Test
    @Override
    public void testFieldTransformation() throws Exception {
        HibNode node = folder("2015");
        try (Tx tx = tx()) {
            ContentDaoWrapper contentDao = tx.contentDao();
            // Update the schema and add a binary field
            SchemaVersionModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
            schema.addField(createFieldSchema(true));
            node.getSchemaContainer().getLatestVersion().setSchema(schema);
            NodeGraphFieldContainer container = contentDao.getLatestDraftFieldContainer(node, english());
            S3HibBinary s3binary = tx.s3binaries().create(UUIDUtil.randomUUID(), node.getUuid() + "/s3", "test.jpg").runInExistingTx(tx);
            S3BinaryGraphField field = container.createS3Binary(S3_BINARY_FIELD, s3binary);
            field.setMimeType("image/jpg");
            s3binary.setImageHeight(200);
            s3binary.setImageWidth(300);
            tx.success();
        }

        try (Tx tx = tx()) {
            String json = getJson(node);
            assertNotNull(json);
            NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
            assertNotNull(response);

            S3BinaryField deserializedNodeField = response.getFields().getS3BinaryField(S3_BINARY_FIELD);
            assertNotNull(deserializedNodeField);
            assertEquals("test.jpg", deserializedNodeField.getFileName());
            assertEquals(200, deserializedNodeField.getHeight().intValue());
            assertEquals(300, deserializedNodeField.getWidth().intValue());
        }
    }

    @Test
    @Override
    public void testFieldUpdate() {

    }

    @Test
    @Override
    public void testClone() {

    }

    @Test
    @Override
    public void testEquals() {

    }

    @Test
    @Override
    public void testEqualsNull() {
        try (Tx tx = tx()) {
            NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
            S3HibBinary s3binary = tx.s3binaries().create(UUIDUtil.randomUUID(), "/s3", "img.jpg").runInExistingTx(tx);
            S3BinaryGraphField fieldA = container.createS3Binary(S3_BINARY_FIELD, s3binary);
            assertFalse(fieldA.equals((Field) null));
            assertFalse(fieldA.equals((GraphField) null));
        }
    }

    @Test
    @Override
    public void testEqualsRestField() {
        try (Tx tx = tx()) {
            NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
            container.setVersion(new VersionNumber(2, 1));
            S3HibBinary s3binary = tx.s3binaries().create("1234", container.getUuid() + "/s3", "img.jpg").runInExistingTx(tx);
            S3BinaryGraphField fieldA = container.createS3Binary("fieldA", s3binary);

            // graph empty - rest empty
            assertTrue("The field should be equal to the html rest field since both fields have no value.", fieldA.equals(new S3BinaryFieldImpl()));

            // graph set - rest set - same value - different type
            fieldA.setFileName("someText");
            assertFalse("The field should not be equal to a string rest field. Even if it has the same value", fieldA.equals(new StringFieldImpl()
                    .setString("someText")));
            // graph set - rest set - different value
            assertFalse("The field should not be equal to the rest field since the rest field has a different value.", fieldA.equals(
                    new S3BinaryFieldImpl().setFileName("blub")));

            // graph set - rest set - same value
            assertTrue("The binary field filename value should be equal to a rest field with the same value", fieldA.equals(new S3BinaryFieldImpl()
                    .setFileName("someText")));
        }
    }

    @Test
    @Override
    public void testUpdateFromRestNullOnCreate() {

    }

    @Test
    @Override
    public void testUpdateFromRestNullOnCreateRequired() {

    }

    @Test
    @Override
    public void testRemoveFieldViaNull() {

    }

    @Test
    @Override
    public void testRemoveRequiredFieldViaNull() {

    }

    @Test
    @Override
    public void testUpdateFromRestValidSimpleValue() {

    }
}
