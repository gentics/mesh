package com.gentics.mesh.search.index;

import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_FINISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.SchemaUpdateParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

/**
 * Test which will check how branch migrations and index sync work together.
 */
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true)
public class BranchIndexSyncTest extends AbstractMeshTest {

	@Test
	public void testSyncWithUnmigratedBranch() {
		grantAdminRole();

		// 1. Create second branch
		waitForJob(() -> {
			BranchCreateRequest branchCreateRequest = new BranchCreateRequest();
			branchCreateRequest.setLatest(true);
			branchCreateRequest.setName("newBranch");
			call(() -> client().createBranch(projectName(), branchCreateRequest));
		});

		// 2. Update the schema version without linking it to the branches
		String contentSchemaUuid = tx(() -> schemaContainer("content").getUuid());
		SchemaUpdateRequest request = JsonUtil.readValue(tx(() -> schemaContainer("content").getLatestVersion().getJson()),
			SchemaUpdateRequest.class);
		request.removeField("teaser");
		request.addField(FieldUtil.createNumberFieldSchema("teaser"));
		call(() -> client().updateSchema(contentSchemaUuid, request, new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false)));

		// 3. Invoke index sync
		waitForEvent(INDEX_SYNC_FINISHED, () -> {
			call(() -> client().invokeIndexClear());
			call(() -> client().invokeIndexSync());
		});
	}
}
