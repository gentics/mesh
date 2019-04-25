package com.gentics.mesh.core.project;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.text.DateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.joda.time.DateTime;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.parameter.impl.ProjectPurgeParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.TestUtils;
import com.gentics.mesh.util.DateUtils;

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
		for (int i = 0; i < 6; i++) {
			if (i == 3) {
				middle = Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
			}
			NodeUpdateRequest request = new NodeUpdateRequest();
			request.setVersion("draft");
			request.setLanguage("en");
			request.getFields().put("slug", FieldUtil.createStringField("blub" + i));
			call(() -> client().updateNode(projectName(), contentUuid(), request));
			TestUtils.sleep(1000);
		}

		final String middleDate = middle;
		waitForLatestJob(() -> {
			call(() -> client().purgeProject(projectUuid(), new ProjectPurgeParametersImpl().setSince(middleDate)));
		}, COMPLETED);
	}
}
