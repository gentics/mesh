package com.gentics.mesh.core.project;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;
import com.gentics.mesh.parameter.impl.ProjectPurgeParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.TestUtils;

@MeshTestSetting(elasticsearch = NONE, testSize = TestSize.FULL, startServer = true)
public class ProjectVersionPurgeEndpointTest extends AbstractMeshTest {

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
	public void testPurgeWithSince() {
		grantAdminRole();

		String middle = null;
		for (int i = 0; i < 12; i++) {
			if (i == 6) {
				middle = Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
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

		System.out.println(middle);
		final String middleDate = middle;
		waitForLatestJob(() -> {
			call(() -> client().purgeProject(projectUuid(), new ProjectPurgeParametersImpl().setSince(middleDate)));
		}, COMPLETED);

		NodeVersionsResponse versions = call(() -> client().listNodeVersions(projectName(), contentUuid()));
		System.out.println(versions.toJson());
	}
}
