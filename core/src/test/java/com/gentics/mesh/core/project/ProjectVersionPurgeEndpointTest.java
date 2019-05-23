package com.gentics.mesh.core.project;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;
import com.gentics.mesh.parameter.ProjectPurgeParameters;
import com.gentics.mesh.parameter.impl.ProjectPurgeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.TestUtils;

@MeshTestSetting(elasticsearch = NONE, testSize = TestSize.FULL, startServer = true)
public class ProjectVersionPurgeEndpointTest extends AbstractMeshTest {

	@Test
	public void testPurgeWithNoPerm() {
		call(() -> client().purgeProject(projectUuid()), FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testBogusProject() {
		call(() -> client().purgeProject(userUuid()), NOT_FOUND, "object_not_found_for_uuid", userUuid());
	}

	@Test
	public void testBasicPurge() {
		grantAdminRole();
		waitForLatestJob(() -> {
			call(() -> client().purgeProject(projectUuid()));
		}, COMPLETED);
	}

	@Test
	public void testPurgeWithSince() throws InterruptedException, ExecutionException {
		grantAdminRole();
		String middle = null;
		for (int i = 0; i < 12; i++) {
			if (i == 6) {
				middle = Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
				String branchUuid = setupBranch(initialBranchUuid(), "demo2", false);
				// String branchUuid2 = setupBranch(initialBranchUuid(), "demo3", false);
				// String branchUuid3 = setupBranch(branchUuid, "demo4", false);
			}
			NodeUpdateRequest request = new NodeUpdateRequest();
			request.setVersion("draft");
			request.setLanguage("en");
			request.getFields().put("slug", FieldUtil.createStringField("blub" + i));
			call(() -> client().updateNode(projectName(), contentUuid(), request));

			NodeUpdateRequest request2 = new NodeUpdateRequest();
			request2.setVersion("draft");
			request2.setLanguage("de");
			request2.getFields().put("slug", FieldUtil.createStringField("blub_de" + i));
			call(() -> client().updateNode(projectName(), contentUuid(), request2));
			TestUtils.sleep(500);
		}

		final String middleDate = middle;
		waitForLatestJob(() -> {
			ProjectPurgeParameters purgeParams = new ProjectPurgeParametersImpl();
			// purgeParams.setSince(middleDate);
			call(() -> client().purgeProject(projectUuid(), purgeParams));
		}, COMPLETED);

		NodeVersionsResponse versions = call(() -> client().listNodeVersions(projectName(), contentUuid()));

		NodeVersionsResponse versions2 = call(
			() -> client().listNodeVersions(projectName(), contentUuid(), new VersioningParametersImpl().setBranch("demo2")));
	}

	private String setupBranch(String initialBranchUuid, String branchName, boolean latest) throws InterruptedException, ExecutionException {
		CompletableFuture<String> fut = new CompletableFuture<>();
		waitForJobs(() -> {
			BranchCreateRequest branchCreateRequest = new BranchCreateRequest();
			branchCreateRequest.setBaseBranch(new BranchReference().setUuid(initialBranchUuid()));
			branchCreateRequest.setName(branchName);
			branchCreateRequest.setLatest(latest);
			BranchResponse resp = call(() -> client().createBranch(projectName(), branchCreateRequest));
			fut.complete(resp.getUuid());
		}, COMPLETED, 1);
		return fut.get();
	}
}
