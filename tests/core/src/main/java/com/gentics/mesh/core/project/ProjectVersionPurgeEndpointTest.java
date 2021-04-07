package com.gentics.mesh.core.project;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.NONE;
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
import com.gentics.mesh.parameter.ProjectPurgeParameters;
import com.gentics.mesh.parameter.impl.ProjectPurgeParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.util.TestUtils;

@MeshTestSetting(elasticsearch = NONE, testSize = TestSize.FULL, startServer = true)
public class ProjectVersionPurgeEndpointTest extends AbstractMeshTest {

	@Test
	public void testPurgeWithNoPerm() {
		call(() -> client().purgeProject(projectUuid()), FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testBogusProject() {
		adminCall(() -> client().purgeProject(userUuid()), NOT_FOUND, "object_not_found_for_uuid", userUuid());
	}

	@Test
	public void testPurge() {
		disableAutoPurge();
		String nodeUuid = contentUuid();
		waitForJob(() -> {
			adminCall(() -> client().purgeProject(projectUuid()));
		});
		assertVersions(nodeUuid, "en", "PD(1.0)=>I(0.1)");

		for (int i = 0; i < 5; i++) {
			NodeUpdateRequest request = new NodeUpdateRequest();
			request.setVersion("draft");
			request.setLanguage("en");
			request.getFields().put("slug", FieldUtil.createStringField("blub" + i));
			call(() -> client().updateNode(projectName(), nodeUuid, request));
		}
		assertVersions(nodeUuid, "en", "D(1.5)=>(1.4)=>(1.3)=>(1.2)=>(1.1)=>P(1.0)=>I(0.1)");

		// Now only D I and P must remain.
		waitForJob(() -> {
			runAsAdmin(() -> {
				call(() -> client().purgeProject(projectUuid()));
			});
		});
		assertVersions(nodeUuid, "en", "D(1.5)=>P(1.0)=>I(0.1)");
	}

	@Test
	public void testPurgeWithBefore() throws InterruptedException, ExecutionException {
		disableAutoPurge();
		String nodeUuid = contentUuid();
		String middle = null;
		for (int i = 0; i < 12; i++) {
			if (i == 6) {
				middle = Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
				setupBranch(initialBranchUuid(), "demo2", false);
			}
			NodeUpdateRequest request = new NodeUpdateRequest();
			request.setVersion("draft");
			request.setLanguage("en");
			request.getFields().put("slug", FieldUtil.createStringField("blub" + i));
			call(() -> client().updateNode(projectName(), nodeUuid, request));

			NodeUpdateRequest request2 = new NodeUpdateRequest();
			request2.setVersion("draft");
			request2.setLanguage("de");
			request2.getFields().put("slug", FieldUtil.createStringField("blub_de" + i));
			call(() -> client().updateNode(projectName(), nodeUuid, request2));
			TestUtils.sleep(500);
		}

		final String middleDate = middle;
		waitForJob(() -> {
			ProjectPurgeParameters purgeParams = new ProjectPurgeParametersImpl();
			purgeParams.setBefore(middleDate);
			adminCall(() -> client().purgeProject(projectUuid(), purgeParams));
		});

		assertVersions(nodeUuid, "en", "D(1.12)=>(1.11)=>(1.10)=>(1.9)=>(1.8)=>(1.7)=>(1.6)=>PI(1.0)=>I(0.1)");
		assertVersions(nodeUuid, "de", "D(1.12)=>(1.11)=>(1.10)=>(1.9)=>(1.8)=>(1.7)=>(1.6)=>PI(1.0)=>I(0.1)");
		assertVersions(nodeUuid, "en", "D(1.6)=>PI(1.0)=>I(0.1)", "demo2");
		assertVersions(nodeUuid, "de", "D(1.6)=>PI(1.0)=>I(0.1)", "demo2");
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
