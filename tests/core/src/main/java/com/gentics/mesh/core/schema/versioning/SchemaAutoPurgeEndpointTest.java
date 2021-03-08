package com.gentics.mesh.core.schema.versioning;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.Tuple;

@MeshTestSetting(testSize = FULL, startServer = true)
public class SchemaAutoPurgeEndpointTest extends AbstractMeshTest {

	@Test
	public void testBasicAutoPurge() {
		String nodeUuid = contentUuid();
		assertVersions(nodeUuid, "en", "PD(1.0)=>I(0.1)");
		enableAutoPurgeOnSchema();
		assertVersions(nodeUuid, "en", "PD(2.0)=>I(0.1)");

		// 1. Update
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setVersion("draft");
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new name"));
		call(() -> client().updateNode(projectName(), nodeUuid, nodeUpdateRequest));
		assertVersions(nodeUuid, "en", "D(2.1)=>P(2.0)=>I(0.1)");

		// 2. Update
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new name2"));
		call(() -> client().updateNode(projectName(), nodeUuid, nodeUpdateRequest));
		assertVersions(nodeUuid, "en", "D(2.2)=>P(2.0)=>I(0.1)");

		// 3. Update
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new name3"));
		call(() -> client().updateNode(projectName(), nodeUuid, nodeUpdateRequest));
		assertVersions(nodeUuid, "en", "D(2.3)=>P(2.0)=>I(0.1)");

		// 4. Publish node
		call(() -> client().publishNode(projectName(), nodeUuid));
		assertVersions(nodeUuid, "en", "PD(3.0)=>I(0.1)");

		// 5. Take node offline
		call(() -> client().takeNodeOffline(projectName(), nodeUuid));
		assertVersions(nodeUuid, "en", "D(3.0)=>I(0.1)");

		// 6. Publish again
		call(() -> client().publishNode(projectName(), nodeUuid));
		assertVersions(nodeUuid, "en", "PD(4.0)=>I(0.1)");

		// Idempotency
		call(() -> client().publishNode(projectName(), nodeUuid));
		assertVersions(nodeUuid, "en", "PD(4.0)=>I(0.1)");

		// 7. Create new draft
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new name4"));
		call(() -> client().updateNode(projectName(), nodeUuid, nodeUpdateRequest));
		assertVersions(nodeUuid, "en", "D(4.1)=>P(4.0)=>I(0.1)");

		// Take language offline
		call(() -> client().takeNodeOffline(projectName(), nodeUuid));
		assertVersions(nodeUuid, "en", "D(4.1)=>I(0.1)");

		call(() -> client().publishNode(projectName(), nodeUuid));
		assertVersions(nodeUuid, "en", "PD(5.0)=>I(0.1)");

		// Now create a branch. A new initial edge should be created
		waitForJob(() -> {
			BranchCreateRequest branchCreateRequest = new BranchCreateRequest();
			branchCreateRequest.setName("branch1");
			branchCreateRequest.setLatest(false);
			call(() -> client().createBranch(projectName(), branchCreateRequest));
		});
		assertVersions(nodeUuid, "en", "PDI(5.0)=>I(0.1)");

		// Update the node again
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new name5"));
		call(() -> client().updateNode(projectName(), nodeUuid, nodeUpdateRequest));
		assertVersions(nodeUuid, "en", "D(5.1)=>PI(5.0)=>I(0.1)");

		// Publish it again and ensure that version 4.0 is not removed
		call(() -> client().publishNode(projectName(), nodeUuid));
		assertVersions(nodeUuid, "en", "PD(6.0)=>I(5.0)=>I(0.1)");

	}

	@Test
	public void testAutoPurgeWithUpload() {
		enableAutoPurgeOnSchema();
		HibNode node = content();
		String nodeUuid = tx(() -> node.getUuid());
		tx(() -> prepareSchema(node, "", "binary"));
		assertVersions(nodeUuid, "en", "PD(2.0)=>I(0.1)");

		call(() -> uploadRandomData(node, "en", "binary", 1000, "application/pdf", "somefile.PDF"));
		assertVersions(nodeUuid, "en", "D(2.1)=>P(2.0)=>I(0.1)");
		call(() -> uploadRandomData(node, "en", "binary", 1000, "application/pdf", "somefile.PDF"));
		assertVersions(nodeUuid, "en", "D(2.2)=>P(2.0)=>I(0.1)");
	}

	@Test
	public void testAutoPurgeWithBinaryTransform() {
		HibNode node = content();
		String nodeUuid = tx(() -> node.getUuid());
		assertVersions(nodeUuid, "en", "PD(1.0)=>I(0.1)");
		enableAutoPurgeOnSchema();
		// Version purge will get rid of 1.0
		assertVersions(nodeUuid, "en", "PD(2.0)=>I(0.1)");

		// 1. Upload image
		String version = db().tx(() -> {
			return uploadImage(node, "en", "image").getVersion();
		});
		assertVersions(nodeUuid, "en", "D(2.1)=>P(2.0)=>I(0.1)");

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100);
		call(() -> client().transformNodeBinaryField(PROJECT_NAME, nodeUuid, "en", version, "image", params));
		assertVersions(nodeUuid, "en", "D(2.2)=>P(2.0)=>I(0.1)");

	}

	@Test
	public void testAutoPurgeForSchemaMigration() {
		grantAdmin();
		enableAutoPurgeOnSchema();
		String contentSchemaUuid = tx(() -> schemaContainer("content").getUuid());
		String nodeUuid = contentUuid();
		assertVersions(nodeUuid, "en", "PD(2.0)=>I(0.1)");

		// 1. Migration
		SchemaUpdateRequest request = call(() -> client().findSchemaByUuid(contentSchemaUuid)).toUpdateRequest();
		request.addField(FieldUtil.createStringFieldSchema("1"));
		waitForJob(() -> {
			call(() -> client().updateSchema(contentSchemaUuid, request));
		});
		assertVersions(nodeUuid, "en", "PD(3.0)=>I(0.1)");

		// 2. Migration
		request.addField(FieldUtil.createStringFieldSchema("2"));
		waitForJob(() -> {
			call(() -> client().updateSchema(contentSchemaUuid, request));
		});
		assertVersions(nodeUuid, "en", "PD(4.0)=>I(0.1)");

		// 3. Create new draft
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setVersion("draft");
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new name"));
		call(() -> client().updateNode(projectName(), nodeUuid, nodeUpdateRequest));
		assertVersions(nodeUuid, "en", "D(4.1)=>P(4.0)=>I(0.1)");

		// 4. Migration
		request.addField(FieldUtil.createStringFieldSchema("3"));
		waitForJob(() -> {
			call(() -> client().updateSchema(contentSchemaUuid, request));
		});
		assertVersions(nodeUuid, "en", "D(5.1)=>P(5.0)=>I(0.1)");
	}

	@Test
	public void testAutoPurgeForMicroschemaMigration() {
		grantAdmin();
		String microschemaUuid = tx(() -> microschemaContainer("vcard").getUuid());
		String contentSchemaUuid = tx(() -> schemaContainer("content").getUuid());
		String nodeUuid = contentUuid();
		assertVersions(nodeUuid, "en", "PD(1.0)=>I(0.1)");

		// 1. Add vcard to schema
		SchemaUpdateRequest schemaRequest = call(() -> client().findSchemaByUuid(contentSchemaUuid)).toUpdateRequest();
		schemaRequest.addField(FieldUtil.createMicronodeFieldSchema("card").setAllowedMicroSchemas("vcard"));
		waitForJob(() -> {
			call(() -> client().updateSchema(contentSchemaUuid, schemaRequest));
		});
		assertVersions(nodeUuid, "en", "PD(2.0)=>I(0.1)");

		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setVersion("draft");
		Tuple<String, Field> firstNameField = Tuple.tuple("firstName", FieldUtil.createStringField("joe"));
		Tuple<String, Field> lastNameField = Tuple.tuple("lastName", FieldUtil.createStringField("doe"));
		MicronodeField micronode = FieldUtil.createMicronodeField("card", firstNameField, lastNameField);
		micronode.getMicroschema().setName("vcard");
		nodeUpdateRequest.getFields().put("card", micronode);
		call(() -> client().updateNode(projectName(), nodeUuid, nodeUpdateRequest));
		assertVersions(nodeUuid, "en", "D(2.1)=>P(2.0)=>I(0.1)");

		// 2. Migrate vcard
		MicroschemaUpdateRequest microschemaRequest = call(() -> client().findMicroschemaByUuid(microschemaUuid)).toRequest();
		microschemaRequest.addField(FieldUtil.createStringFieldSchema("extra"));
		waitForJob(() -> {
			call(() -> client().updateMicroschema(microschemaUuid, microschemaRequest));
		});
		assertVersions(nodeUuid, "en", "D(3.1)=>P(3.0)=>I(0.1)");
	}

	private void enableAutoPurgeOnSchema() {
		grantAdmin();
		String contentSchemaUuid = tx(() -> schemaContainer("content").getUuid());
		assertThat(call(() -> client().findSchemaByUuid(contentSchemaUuid))).autoPurgeIsNotSet();
		waitForJob(() -> {
			SchemaResponse schema = call(() -> client().findSchemaByUuid(contentSchemaUuid));
			SchemaUpdateRequest updateRequest = schema.toUpdateRequest();
			updateRequest.setAutoPurge(true);
			call(() -> client().updateSchema(contentSchemaUuid, updateRequest));
		});
		assertThat(call(() -> client().findSchemaByUuid(contentSchemaUuid))).autoPurgeIsEnabled();
	}

}
