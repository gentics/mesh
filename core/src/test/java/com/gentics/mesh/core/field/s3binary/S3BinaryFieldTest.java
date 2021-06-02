package com.gentics.mesh.core.field.s3binary;

import com.gentics.mesh.context.InternalActionContext;
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

import static com.gentics.mesh.core.field.binary.BinaryFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.s3binary.S3BinaryFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.s3binary.S3BinaryFieldTestHelper.FILL_BASIC;
import static com.gentics.mesh.test.context.AWSTestMode.MINIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

@MeshTestSetting(awsContainer = MINIO, testSize = TestSize.PROJECT_AND_NODE)
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
        try (Tx tx = tx()) {
            NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
            S3HibBinary s3binary = tx.s3binaries().create("1234", "1234/s3", "img.jpg").runInExistingTx(tx);
            S3BinaryGraphField field = container.createS3Binary(S3_BINARY_FIELD, s3binary);
            field.getS3Binary().setSize(Long.valueOf(220));
            assertNotNull(field);
            assertEquals(S3_BINARY_FIELD, field.getFieldKey());

            field.setFileName("blume.jpg");
            field.setMimeType("image/jpg");
            field.setImageDominantColor("#22A7F0");
            field.getS3Binary().setImageHeight(133);
            field.getS3Binary().setImageWidth(7);

            S3BinaryGraphField loadedField = container.getS3Binary(S3_BINARY_FIELD);
            S3HibBinary loadedS3Binary = loadedField.getS3Binary();
            assertNotNull("The previously created field could not be found.", loadedField);
            assertEquals(220, loadedS3Binary.getSize().intValue());

            assertEquals("blume.jpg", loadedField.getFileName());
            assertEquals("image/jpg", loadedField.getMimeType());
            assertEquals("#22A7F0", loadedField.getImageDominantColor());
            assertEquals(133, loadedField.getS3Binary().getImageHeight().intValue());
            assertEquals(7, loadedField.getS3Binary().getImageWidth().intValue());
        }
    }

    @Test
    @Override
    public void testClone() {
        try (Tx tx = tx()) {
            NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

            S3HibBinary s3binary = tx.s3binaries().create("1234","1234/s3","img.jg").runInExistingTx(tx);
            S3BinaryGraphField field = container.createS3Binary("s3", s3binary);
            field.getS3Binary().setSize(Long.valueOf(220));
            assertNotNull(field);
            assertEquals("s3", field.getFieldKey());

            field.setFileName("blume.jpg");
            field.setMimeType("image/jpg");
            field.setImageDominantColor("#22A7F0");
            field.getS3Binary().setImageHeight(133);
            field.getS3Binary().setImageWidth(7);

            NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
            field.cloneTo(otherContainer);

            S3BinaryGraphField clonedField = otherContainer.getS3Binary("s3");
            assertThat(clonedField).as("cloned field").isNotNull().isEqualToIgnoringGivenFields(field, "outV", "id", "uuid", "element");
            assertThat(clonedField.getS3Binary()).as("referenced binary of cloned field").isNotNull().isEqualToComparingFieldByField(field.getS3Binary());
        }
    }

    @Test
    @Override
    public void testEquals() {
        try (Tx tx = tx()) {
            NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
            S3HibBinary s3binary = tx.s3binaries().create("1234","1234/s3","img.jg").runInExistingTx(tx);
            S3BinaryGraphField fieldA = container.createS3Binary("fieldA", s3binary);
            S3BinaryGraphField fieldB = container.createS3Binary("fieldB", s3binary);
            assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
            fieldA.setFileName("someText");
            assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));

            assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
            assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
            fieldB.setFileName("someText");
            assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));
        }
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
        try (Tx tx = tx()) {
            invokeUpdateFromRestTestcase(S3_BINARY_FIELD, FETCH, CREATE_EMPTY);
        }
    }

    @Test
    @Override
    public void testUpdateFromRestNullOnCreateRequired() {
        try (Tx tx = tx()) {
            invokeUpdateFromRestNullOnCreateRequiredTestcase(S3_BINARY_FIELD, FETCH, false);
        }
    }

    @Test
    @Override
    public void testRemoveFieldViaNull() {
        try (Tx tx = tx()) {
            InternalActionContext ac = mockActionContext();
            invokeRemoveFieldViaNullTestcase(S3_BINARY_FIELD, FETCH, FILL_BASIC, (node) -> {
                updateContainer(ac, node, S3_BINARY_FIELD, null);
            });
        }
    }

    @Test
    @Override
    public void testRemoveRequiredFieldViaNull() {
        try (Tx tx = tx()) {
            InternalActionContext ac = mockActionContext();
            invokeRemoveRequiredFieldViaNullTestcase(S3_BINARY_FIELD, FETCH, FILL_BASIC, (container) -> {
                updateContainer(ac, container, S3_BINARY_FIELD, null);
            });
        }
    }

    @Test
    @Override
    public void testUpdateFromRestValidSimpleValue() {
        try (Tx tx = tx()) {
            InternalActionContext ac = mockActionContext();
            invokeUpdateFromRestValidSimpleValueTestcase(S3_BINARY_FIELD, FILL_BASIC, (container) -> {
                S3BinaryField field = new S3BinaryFieldImpl();
                field.setFileName("someFile.txt");
                updateContainer(ac, container, S3_BINARY_FIELD, field);
            }, (container) -> {
                S3BinaryGraphField field = container.getS3Binary(S3_BINARY_FIELD);
                assertNotNull("The graph field {" + S3_BINARY_FIELD + "} could not be found.", field);
                assertEquals("The html of the field was not updated.", "someFile.txt", field.getFileName());
            });
        }
    }
}
