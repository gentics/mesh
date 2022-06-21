package com.gentics.mesh.core.field.s3binary;

import static com.gentics.mesh.core.field.binary.BinaryFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.s3binary.S3BinaryFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.s3binary.S3BinaryFieldTestHelper.FILL_BASIC;
import static com.gentics.mesh.test.AWSTestMode.MINIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.S3BinaryField;
import com.gentics.mesh.core.rest.node.field.impl.S3BinaryFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.S3BinaryFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.NoConsistencyCheck;
import com.gentics.mesh.util.CoreTestUtils;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.mesh.util.VersionNumber;

@MeshTestSetting(awsContainer = MINIO, testSize = TestSize.PROJECT_AND_NODE)
public class S3BinaryFieldTest extends AbstractFieldTest<S3BinaryFieldSchema> {

    private static final String S3_BINARY_FIELD = "s3binaryField";

    @Override
    protected S3BinaryFieldSchema createFieldSchema(boolean isRequired) {
        return createFieldSchema(S3_BINARY_FIELD, isRequired);
    }
    protected S3BinaryFieldSchema createFieldSchema(String fieldKey, boolean isRequired) {
        S3BinaryFieldSchema s3binaryFieldSchema = new S3BinaryFieldSchemaImpl();
        s3binaryFieldSchema.setName(fieldKey);
        s3binaryFieldSchema.setRequired(isRequired);
        return s3binaryFieldSchema;
    }

    @Test
    @Override
    public void testFieldTransformation() throws Exception {
        try (Tx tx = tx()) {
        	HibNode node = folder("2015");
            ContentDao contentDao = tx.contentDao();
            // Update the schema and add a binary field
            prepareTypedSchema(node, createFieldSchema(true), false);
            tx.commit();
            HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
                    node.getProject().getLatestBranch(), user(),
                    contentDao.getLatestDraftFieldContainer(node, english()), true);
            S3HibBinary s3binary = tx.s3binaries().create(UUIDUtil.randomUUID(), node.getUuid() + "/s3", "test.jpg").runInExistingTx(tx);
            S3HibBinaryField field = container.createS3Binary(S3_BINARY_FIELD, s3binary);
            field.setMimeType("image/jpg");
            s3binary.setImageHeight(200);
            s3binary.setImageWidth(300);
            tx.success();
        }

        String json = tx(() -> getJson(folder("2015")));
        assertNotNull(json);
        NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
        assertNotNull(response);

        S3BinaryField deserializedNodeField = response.getFields().getS3BinaryField(S3_BINARY_FIELD);
        assertNotNull(deserializedNodeField);
        assertEquals("test.jpg", deserializedNodeField.getFileName());
        assertEquals(200, deserializedNodeField.getHeight().intValue());
        assertEquals(300, deserializedNodeField.getWidth().intValue());
    }

    @Test
    @Override
    public void testFieldUpdate() {
        try (Tx tx = tx()) {
            HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
            S3HibBinary s3binary = tx.s3binaries().create(UUID.randomUUID().toString(), "1234/s3", "img.jpg").runInExistingTx(tx);
            S3HibBinaryField field = container.createS3Binary(S3_BINARY_FIELD, s3binary);
            field.getBinary().setSize(220);
            assertNotNull(field);
            assertEquals(S3_BINARY_FIELD, field.getFieldKey());

            field.setFileName("blume.jpg");
            field.setMimeType("image/jpg");
            field.setImageDominantColor("#22A7F0");
            field.getBinary().setImageHeight(133);
            field.getBinary().setImageWidth(7);

            S3HibBinaryField loadedField = container.getS3Binary(S3_BINARY_FIELD);
            S3HibBinary loadedS3Binary = loadedField.getBinary();
            assertNotNull("The previously created field could not be found.", loadedField);
            assertEquals(220, loadedS3Binary.getSize());

            assertEquals("blume.jpg", loadedField.getFileName());
            assertEquals("image/jpg", loadedField.getMimeType());
            assertEquals("#22A7F0", loadedField.getImageDominantColor());
            assertEquals(133, loadedField.getBinary().getImageHeight().intValue());
            assertEquals(7, loadedField.getBinary().getImageWidth().intValue());
        }
    }

    @Test
    @NoConsistencyCheck
    @Override
    public void testClone() {
        try (Tx tx = tx()) {
            HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));

            S3HibBinary s3binary = tx.s3binaries().create(UUID.randomUUID().toString(),"1234/s3","img.jg").runInExistingTx(tx);
            S3HibBinaryField field = container.createS3Binary(S3_BINARY_FIELD, s3binary);
            field.getBinary().setSize(220);
            assertNotNull(field);
            assertEquals(S3_BINARY_FIELD, field.getFieldKey());

            field.setFileName("blume.jpg");
            field.setMimeType("image/jpg");
            field.setImageDominantColor("#22A7F0");
            field.getBinary().setImageHeight(133);
            field.getBinary().setImageWidth(7);

            HibNodeFieldContainer otherContainer = CoreTestUtils.createContainer(createFieldSchema(true));
            field.cloneTo(otherContainer);

            S3HibBinaryField clonedField = otherContainer.getS3Binary(S3_BINARY_FIELD);
            assertThat(clonedField).as("cloned field").isNotNull().isEqualToIgnoringGivenFields(field, "outV", "id", "uuid", "element", "contentUuid", "dbUuid", "value", "parentContainer");
            assertThat(clonedField.getBinary()).as("referenced binary of cloned field").isNotNull().isEqualToComparingFieldByField(field.getBinary());
        }
    }

    @Test
    @Override
    public void testEquals() {
        try (Tx tx = tx()) {
            HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema("fieldA", true), createFieldSchema("fieldB", true));
            S3HibBinary s3binaryA = tx.s3binaries().create(UUID.randomUUID().toString(),"1234/s3","img.jg").runInExistingTx(tx);
            S3HibBinary s3binaryB = tx.s3binaries().create(UUID.randomUUID().toString(),"1234/s3","img.jg").runInExistingTx(tx);
            S3HibBinaryField fieldA = container.createS3Binary("fieldA", s3binaryA);
            S3HibBinaryField fieldB = container.createS3Binary("fieldB", s3binaryB);
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
            HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
            S3HibBinary s3binary = tx.s3binaries().create(UUIDUtil.randomUUID(), "/s3", "img.jpg").runInExistingTx(tx);
            S3HibBinaryField fieldA = container.createS3Binary(S3_BINARY_FIELD, s3binary);
            assertFalse(fieldA.equals((Field) null));
            assertFalse(fieldA.equals((S3HibBinaryField) null));
        }
    }

    @Test
    @Override
    public void testEqualsRestField() {
        try (Tx tx = tx()) {
            HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
            tx.contentDao().setVersion(container, new VersionNumber(2, 1));
            S3HibBinary s3binary = tx.s3binaries().create(UUID.randomUUID().toString(), container.getUuid() + "/s3", "img.jpg").runInExistingTx(tx);
            S3HibBinaryField fieldA = container.createS3Binary(S3_BINARY_FIELD, s3binary);

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
                S3HibBinaryField field = container.getS3Binary(S3_BINARY_FIELD);
                assertNotNull("The graph field {" + S3_BINARY_FIELD + "} could not be found.", field);
                assertEquals("The html of the field was not updated.", "someFile.txt", field.getFileName());
            });
        }
    }
}
