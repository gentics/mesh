package com.gentics.mesh.core.field.s3binary;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.AWSTestMode.MINIO;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadataRequest;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryUploadRequest;
import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(awsContainer = MINIO, startServer = true, elasticsearch = ElasticsearchTestMode.CONTAINER_ES7)
public class S3BinaryFieldEndpointTest extends AbstractFieldEndpointTest {

    private static final String FIELD_NAME = "s3";

    private static final S3BinaryUploadRequest UPLOAD_REQUEST = new S3BinaryUploadRequest()
    		.setFilename("test.jpg").setLanguage("en").setVersion("1.0");

    private static final S3BinaryMetadataRequest METADATA_REQUEST = new S3BinaryMetadataRequest()
    		.setLanguage("en").setVersion("1.1");

    /**
     * Update the schema and add a binary field.
     *
     * @throws IOException
     */
    @Before
    public void updateSchema() throws IOException {
    	HibSchema schemaContainer = schemaContainer("content");
		String schemaUuid = tx(() -> schemaContainer.getUuid());
		HibSchemaVersion currentVersion = tx(() -> schemaContainer.getLatestVersion());
		assertNull("The schema should not yet have any changes", tx(() -> currentVersion.getNextChange()));

		// 1. Setup changes
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		JsonObject elasticSearch = new JsonObject().put("test", "test");
		SchemaChangeModel change = SchemaChangeModel.createAddFieldChange(FIELD_NAME, "s3binary", "Some label", elasticSearch);
		listOfChanges.getChanges().add(change);

		// 3. Invoke migration
		GenericMessageResponse status = call(() -> client().applyChangesToSchema(schemaUuid, listOfChanges));
		assertThat(status).matches("schema_changes_applied", "content");
		SchemaResponse updatedSchema = call(() -> client().findSchemaByUuid(schemaUuid));

		waitForJobs(() -> {
			call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(),
				new SchemaReferenceImpl().setName("content").setVersion(updatedSchema.getVersion())));
		}, COMPLETED, 1);
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
        call(() -> client().updateNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), FIELD_NAME, UPLOAD_REQUEST));
        //uploading
        File tempFile = createTempFile();
        s3BinaryStorage().createBucket("test-bucket").blockingGet();
        s3BinaryStorage().uploadFile("test-bucket", s3binaryNode.getUuid() + "/s3/en", tempFile, false).blockingGet();
        s3BinaryStorage().exists("test-bucket", s3binaryNode.getUuid() + "/s3/en").blockingGet();

        // 2. Download the data using the REST API
        MeshBinaryResponse response = call(() -> client().downloadBinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "en", FIELD_NAME));
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
        call(() -> client().updateNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), FIELD_NAME, UPLOAD_REQUEST));
        //uploading
        File tempFile = createTempFile();
        s3BinaryStorage().createBucket("test-bucket").blockingGet();
        s3BinaryStorage().createBucket("test-cache-bucket").blockingGet();
        s3BinaryStorage().uploadFile("test-bucket", s3binaryNode.getUuid() + "/s3/en", tempFile, false).blockingGet();
        NodeResponse s3 = call(() -> client().extractMetadataNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), FIELD_NAME, METADATA_REQUEST));
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
        call(() -> client().updateNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), FIELD_NAME, UPLOAD_REQUEST));
        //uploading
        createTempFile();
        s3BinaryStorage().createBucket("test-bucket").blockingGet();
        s3BinaryStorage().createBucket("test-cache-bucket").blockingGet();
        //here the call should fail since there is no file uploaded.
        try {
            client().extractMetadataNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), FIELD_NAME, METADATA_REQUEST).blockingGet();
            fail("This test muste fail because of no file provided for an upload.");
        } catch (Exception ex) {
            assertTrue(ex instanceof RuntimeException);
        }
    }

    @Test
    public void testDownloadBinaryWithParams() {
        S3RestResponse s3RestResponse = new S3RestResponse();
        s3RestResponse.setVersion("1");
        NodeResponse s3binaryNode = createNodeWithField();
        BufferedImage buf = null;
        //creating
        call(() -> client().updateNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), FIELD_NAME, UPLOAD_REQUEST));
        //uploading
        File tempFile = createTempFile();
        s3BinaryStorage().createBucket("test-bucket").blockingGet();
        s3BinaryStorage().createBucket("test-cache-bucket").blockingGet();
        s3BinaryStorage().uploadFile("test-bucket", s3binaryNode.getUuid() + "/s3/en", tempFile, false).blockingGet();
        s3BinaryStorage().exists("test-bucket", s3binaryNode.getUuid() + "/s3/en").blockingGet();

        //extracting metadata in order to resize img
        call(() -> client().extractMetadataNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), FIELD_NAME, METADATA_REQUEST));

        // 2. Download the data using the REST API
        MeshBinaryResponse response = call(() -> client().downloadBinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "en", FIELD_NAME,
                new ImageManipulationParametersImpl().setWidth(100)));

        assertNotNull(response);

        try {
            byte[] downloadBytes = IOUtils.toByteArray(response.getStream());
            InputStream in = new ByteArrayInputStream(downloadBytes);
            buf = ImageIO.read(in);
        } catch (IOException ioException) {
            fail();
        }
        assertEquals("image/jpeg", response.getContentType());
        assertEquals(100, buf.getWidth());
        response.close();
    }

    @Test
    public void testDownloadBinaryDifferentLanguages() {
    	S3RestResponse s3RestResponse = new S3RestResponse();
        s3RestResponse.setVersion("1");
        NodeResponse s3binaryNode = createNodeWithField();
        NodeUpdateRequest updRequest = s3binaryNode.toRequest().setLanguage(german());
        updRequest.getFields().put("slug", FieldUtil.createStringField("ordner" + 1));
        updRequest.getFields().put("title", FieldUtil.createStringField("ordner" + 2));
        updRequest.getFields().put("teaser", FieldUtil.createStringField("ordner" + 2));
        call(() -> client().updateNode(PROJECT_NAME, s3binaryNode.getUuid(), updRequest, new NodeParametersImpl().setLanguages("de")));
        BufferedImage bufEn = null;
        BufferedImage bufDe = null;
        {
        	//creating English
            call(() -> client().updateNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), FIELD_NAME, new S3BinaryUploadRequest()
            		.setFilename("test.jpg").setLanguage(english()).setVersion("1.0")));
            //uploading
            File tempFile = createTempFile("blume.jpg");
            s3BinaryStorage().createBucket("test-bucket").blockingGet();
            s3BinaryStorage().createBucket("test-cache-bucket").blockingGet();
            s3BinaryStorage().uploadFile("test-bucket", s3binaryNode.getUuid() + "/s3/en", tempFile, false).blockingGet();
            s3BinaryStorage().exists("test-bucket", s3binaryNode.getUuid() + "/s3/en").blockingGet();

            //extracting metadata in order to resize img
            call(() -> client().extractMetadataNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), FIELD_NAME, METADATA_REQUEST));

            // 2. Download the data using the REST API
            MeshBinaryResponse response = call(() -> client().downloadBinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "en", FIELD_NAME, new NodeParametersImpl().setLanguages("en")));

            assertNotNull(response);
            try {
                byte[] downloadBytes = IOUtils.toByteArray(response.getStream());
                InputStream in = new ByteArrayInputStream(downloadBytes);
                bufEn = ImageIO.read(in);
            } catch (IOException ioException) {
                fail();
            }
            assertEquals("image/jpeg", response.getContentType());
            assertEquals(1160, bufEn.getWidth());
            response.close();
        }
        {
        	//creating German
            call(() -> client().updateNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), FIELD_NAME, new S3BinaryUploadRequest()
            		.setFilename("test.jpg").setLanguage(german()).setVersion("0.1")));
            //uploading
            File tempFile = createTempFile("blume_large.jpg");
            s3BinaryStorage().createBucket("test-bucket").blockingGet();
            s3BinaryStorage().createBucket("test-cache-bucket").blockingGet();
            s3BinaryStorage().uploadFile("test-bucket", s3binaryNode.getUuid() + "/s3/de", tempFile, false).blockingGet();
            s3BinaryStorage().exists("test-bucket", s3binaryNode.getUuid() + "/s3/de").blockingGet();

            //extracting metadata in order to resize imgnull
            call(() -> client().extractMetadataNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), FIELD_NAME, new S3BinaryMetadataRequest().setLanguage(german()).setVersion("0.2")));

            // 2. Download the data using the REST API
            MeshBinaryResponse response = call(() -> client().downloadBinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "de", FIELD_NAME, new NodeParametersImpl().setLanguages("de")));

            assertNotNull(response);
            try {
                byte[] downloadBytes = IOUtils.toByteArray(response.getStream());
                InputStream in = new ByteArrayInputStream(downloadBytes);
                bufDe = ImageIO.read(in);
            } catch (IOException ioException) {
                fail();
            }
            assertEquals("image/jpeg", response.getContentType());
            assertEquals(2000, bufDe.getWidth());
            response.close();
        }
    }
   
    @Test
    public void testTransformS3BinarySuccessful() {
        S3RestResponse s3RestResponse = new S3RestResponse();
        s3RestResponse.setVersion("1");
        NodeResponse s3binaryNode = createNodeWithField();
        //creating
        call(() -> client().updateNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), FIELD_NAME, UPLOAD_REQUEST));
        //uploading
        File tempFile = createTempFile();
        s3BinaryStorage().createBucket("test-bucket").blockingGet();
        s3BinaryStorage().createBucket("test-cache-bucket").blockingGet();
        s3BinaryStorage().uploadFile("test-bucket", s3binaryNode.getUuid() + "/s3/en", tempFile, false).blockingGet();
        client().extractMetadataNodeS3BinaryField(PROJECT_NAME, s3binaryNode.getUuid(), FIELD_NAME, METADATA_REQUEST).blockingGet();
        NodeResponse call = call(() -> client().transformNodeBinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "en", "draft", FIELD_NAME, new ImageManipulationParametersImpl().setWidth(250)));
        assertNotNull(call);
        assertEquals(250, call.getFields().getS3BinaryField(FIELD_NAME).getWidth().intValue());
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
                    FIELD_NAME,
                    UPLOAD_REQUEST
            ));
            //uploading
            File tempFile = createTempFile();
            s3BinaryStorage().createBucket("test-bucket").blockingGet();
            s3RestResponse1 = s3BinaryStorage().uploadFile("test-bucket", s3binaryNode.getUuid() + "/s3/en", tempFile, false).blockingGet();
        }
        Boolean doesObjectExists = s3BinaryStorage().exists("test-bucket", s3binaryNode.getUuid() + "/s3/en").blockingGet();
        assertTrue(doesObjectExists);
        assertNotNull(s3RestResponse1);
        NodeResponse s3binaryNode1 = call(() -> client().findNodeByUuid(PROJECT_NAME, s3binaryNode.getUuid()));
        assertNotNull(s3binaryNode1);
        assertNotNull(s3binaryNode1.getFields().getS3BinaryField(FIELD_NAME));
    }

    @Test
    public void testUploadOldFormatWorking() {
        S3RestResponse s3RestResponse = new S3RestResponse();
        s3RestResponse.setVersion("1");
        NodeResponse s3binaryNode = createNodeWithField();
        S3RestResponse s3RestResponse1 = null;
        //creating
        call(() -> client().updateNodeS3BinaryField(
                PROJECT_NAME,
                s3binaryNode.getUuid(),
                FIELD_NAME,
                UPLOAD_REQUEST
        ));
        // some very illegal stuff
        try (Tx tx = tx()) {
        	S3HibBinaryField s3Field = tx.contentDao().findVersion(tx.nodeDao().findByUuidGlobal(s3binaryNode.getUuid()), english(), initialBranchUuid(), "1.1").getS3Binary(FIELD_NAME);
        	s3Field.getBinary().setS3ObjectKey(s3binaryNode.getUuid() + "/s3");
        	tx.success();
        }
        //uploading
        long length;
        try (Tx tx = tx()) {
            File tempFile = createTempFile();
            s3BinaryStorage().createBucket("test-bucket").blockingGet();
            s3RestResponse1 = s3BinaryStorage().uploadFile("test-bucket", s3binaryNode.getUuid() + "/s3", tempFile, false).blockingGet();
            length = tempFile.length();
        }
        assertNotNull(s3RestResponse1);
        //new format has not been uploaded
        Boolean doesObjectExists = s3BinaryStorage().exists("test-bucket", s3binaryNode.getUuid() + "/s3/en").blockingGet();
        assertFalse(doesObjectExists);
        // but the old format exists
        doesObjectExists = s3BinaryStorage().exists("test-bucket", s3binaryNode.getUuid() + "/s3").blockingGet();
        assertTrue(doesObjectExists);
        // but the node does not care
        // 2. Download the data using the REST API
        MeshBinaryResponse response = call(() -> client().downloadBinaryField(PROJECT_NAME, s3binaryNode.getUuid(), "en", FIELD_NAME));

        assertNotNull(response);
        try {
            byte[] downloadBytes = IOUtils.toByteArray(response.getStream());
            assertEquals(length, downloadBytes.length);
        } catch (IOException ioException) {
            fail();
        } finally {
        	response.close();
        }
    }
   
    @Test
    public void testUploadEmptyFilename() {
        NodeResponse s3binaryNode = createNodeWithField();

        final S3BinaryUploadRequest request = new S3BinaryUploadRequest().setFilename("").setLanguage("en").setVersion("1.0");

        try {
            client().updateNodeS3BinaryField(
                    PROJECT_NAME,
                    s3binaryNode.getUuid(),
                    FIELD_NAME,
                    request
            ).blockingAwait();
            fail("Empty file name should not pass");
        } catch (Exception e) {
            assertTrue(e.getMessage().indexOf("Error:400 in POST") > -1);
        }
    }
}
