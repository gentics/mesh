package com.gentics.mesh.core.field.s3binary;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.S3BinaryFieldSchemaImpl;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.test.context.MeshTestSetting;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.AWSTestMode.MINIO;
import static org.junit.Assert.*;

@MeshTestSetting(awsContainer = MINIO, startServer = true)
public class S3BinaryFieldEndpointTest extends AbstractFieldEndpointTest {

    private static final String FIELD_NAME = "s3";
    private static final String uploadBody = "{\n" +
            "\t\"language\":\"en\",\n" +
            "\t\"version\":\"1.0\",\n" +
            "\t\"filename\":\"test.jpg\"\n" +
            "}";

    private static final String metadataBody = "{\n" +
            "\t\"language\":\"en\",\n" +
            "\t\"version\":\"1.1\"\n" +
            "}";

    /**
     * Update the schema and add a binary field.
     *
     * @throws IOException
     */
    @Before
    public void updateSchema() throws IOException {
        try (Tx tx = tx()) {
            SchemaVersionModel schema = schemaContainer("content").getLatestVersion().getSchema();
            S3BinaryFieldSchema s3BinaryFieldSchema = new S3BinaryFieldSchemaImpl();
            s3BinaryFieldSchema.setName(FIELD_NAME);
            s3BinaryFieldSchema.setLabel("Some label");
            schema.addField(s3BinaryFieldSchema);
            schemaContainer("content").getLatestVersion().setSchema(schema);
            tx.success();
        }
    }

    @Override
    public void testReadNodeWithExistingField() throws IOException {

    }

    @Override
    public void testUpdateNodeFieldWithField() throws IOException {

    }

    @Override
    public void testUpdateSameValue() {

    }

    @Override
    public void testUpdateSetNull() {

    }

    @Override
    public void testUpdateSetEmpty() {

    }

    @Override
    public void testCreateNodeWithField() {

    }

    @Override
    public void testCreateNodeWithNoField() {

    }

    @Override
    public NodeResponse createNodeWithField() {
        String parentUuid = tx(() -> folder("2015").getUuid());

        grantAdmin();

        NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
        nodeCreateRequest.setLanguage("en").setParentNodeUuid(parentUuid).setSchemaName("content");

        nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("folder" + 1));
        nodeCreateRequest.getFields().put("title", FieldUtil.createStringField("folder" + 2));
        nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("folder" + 2));
        NodeResponse nodeResponse = client().createNode(PROJECT_NAME, nodeCreateRequest).blockingGet();
        call(() -> client().publishNode(PROJECT_NAME, nodeResponse.getUuid()));
        return nodeResponse;
    }

    @Test
    public void testDownloadBinary() {
        S3RestResponse s3RestResponse = new S3RestResponse();
        s3RestResponse.setVersion("1");
        NodeResponse s3binaryNode = createNodeWithField();
        call(() -> client().updateNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "s3", uploadBody));
        //uploading
        File tempFile = createTempFile();
        s3BinaryStorage().createAsyncBucket("test-bucket").blockingGet();
        s3BinaryStorage().uploadFile("test-bucket", s3binaryNode.getUuid() + "/s3", tempFile, false).blockingGet();
        s3BinaryStorage().exists("test-bucket", s3binaryNode.getUuid() + "/s3").blockingGet();

        // 2. Download the data using the REST API
        MeshBinaryResponse response = call(() -> client().downloadBinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "en", "s3"));
        assertNotNull(response);
        assertEquals("image/jpeg", response.getContentType());
        response.close();
    }

    @Test
    public void testExtractMetadataSuccessful() {
        S3RestResponse s3RestResponse = new S3RestResponse();
        s3RestResponse.setVersion("1");
        NodeResponse s3binaryNode = createNodeWithField();
        //creating
        call(() -> client().updateNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "s3", uploadBody));
        //uploading
        File tempFile = createTempFile();
        s3BinaryStorage().createAsyncBucket("test-bucket").blockingGet();
        s3BinaryStorage().createAsyncBucket("test-cache-bucket").blockingGet();
        s3BinaryStorage().uploadFile("test-bucket", s3binaryNode.getUuid() + "/s3", tempFile, false).blockingGet();
        NodeResponse s3 = call(() -> client().extractMetadataNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "s3", metadataBody));
        assertEquals("test.jpg", s3.getFields().getS3BinaryField(FIELD_NAME).getFileName());
        assertEquals("image/jpeg", s3.getFields().getS3BinaryField(FIELD_NAME).getMimeType());
        assertEquals(1376, s3.getFields().getS3BinaryField(FIELD_NAME).getHeight().intValue());
    }

    @Test
    public void testExtractMetadataNoFileUploadedShouldFail() {
        S3RestResponse s3RestResponse = new S3RestResponse();
        s3RestResponse.setVersion("1");
        NodeResponse s3binaryNode = createNodeWithField();
        //creating
        call(() -> client().updateNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "s3", uploadBody));
        //uploading
        File tempFile = createTempFile();
        s3BinaryStorage().createAsyncBucket("test-bucket").blockingGet();
        s3BinaryStorage().createAsyncBucket("test-cache-bucket").blockingGet();
        //here the call should fail since there is no file uploaded.
        try {
            client().extractMetadataNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "s3", metadataBody).blockingGet();
        } catch (Exception ex) {
            assertTrue(ex instanceof RuntimeException);
        }
    }

    @Test
    public void testDownloadBinaryWithParams() {
        S3RestResponse s3RestResponse = new S3RestResponse();
        s3RestResponse.setVersion("1");
        NodeResponse s3binaryNode = createNodeWithField();
        S3RestResponse s3RestResponse1 = null;
        BufferedImage buf = null;
        //creating
        call(() -> client().updateNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "s3", uploadBody));
        //uploading
        File tempFile = createTempFile();
        s3BinaryStorage().createAsyncBucket("test-bucket").blockingGet();
        s3BinaryStorage().createAsyncBucket("test-cache-bucket").blockingGet();
        s3BinaryStorage().uploadFile("test-bucket", s3binaryNode.getUuid() + "/s3", tempFile, false).blockingGet();
        s3BinaryStorage().exists("test-bucket", s3binaryNode.getUuid() + "/s3").blockingGet();

        //extracting metadata in order to resize img
        call(() -> client().extractMetadataNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "s3", metadataBody));

        // 2. Download the data using the REST API
        MeshBinaryResponse response = call(() -> client().downloadBinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "en", "s3",
                new ImageManipulationParametersImpl().setWidth(100)));
        try {
            byte[] downloadBytes = IOUtils.toByteArray(response.getStream());
            InputStream in = new ByteArrayInputStream(downloadBytes);
            buf = ImageIO.read(in);
        } catch (IOException ioException) {
            fail();
        }
        assertEquals(100, buf.getWidth());
        assertNotNull(response);
        assertEquals("image/jpeg", response.getContentType());
        response.close();
    }

    @Test
    public void testUploadWorking() {
        S3RestResponse s3RestResponse = new S3RestResponse();
        s3RestResponse.setVersion("1");
        NodeResponse s3binaryNode = createNodeWithField();
        S3RestResponse s3RestResponse1 = null;
        //creating
        try (Tx tx = tx()) {
            call(() -> client().updateNodeS3BinaryField(
                    PROJECT_NAME,
                    s3binaryNode.getUuid(),
                    "s3",
                    uploadBody
            ));
            //uploading
            File tempFile = createTempFile();
            s3BinaryStorage().createAsyncBucket("test-bucket").blockingGet();
            s3RestResponse1 = s3BinaryStorage().uploadFile("test-bucket", s3binaryNode.getUuid() + "/s3", tempFile, false).blockingGet();
        }
        Boolean doesObjectExists = s3BinaryStorage().exists("test-bucket", s3binaryNode.getUuid() + "/s3").blockingGet();
        assertTrue(doesObjectExists);
        assertNotNull(s3RestResponse1);
    }

    @Test
    public void testUploadEmptyFilename() {
        NodeResponse s3binaryNode = createNodeWithField();
        String body = "{\n" +
                "\t\"language\":\"en\",\n" +
                "\t\"version\":\"1.0\",\n" +
                "\t\"filename\":\"\"\n" +
                "}";

        try {
            client().updateNodeS3BinaryField(
                    PROJECT_NAME,
                    s3binaryNode.getUuid(),
                    "s3",
                    body
            ).blockingAwait();
            fail("Empty file name should not pass");
        } catch (Exception e) {
            assertTrue(e.getMessage().indexOf("Error:400 in POST") > -1);
        }
    }
}
