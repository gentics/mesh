package com.gentics.mesh.core.project.maintenance;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;

import java.util.Optional;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = NONE, testSize = TestSize.FULL, startServer = true)
public class ProjectVersionPurgeHandlerTest extends AbstractMeshTest {

	@Test
	public void testHandler() {
		Project project = project();

		for (int i = 0; i < 10; i++) {
			NodeUpdateRequest request = new NodeUpdateRequest();
			request.setVersion("draft");
			request.setLanguage("en");
			request.getFields().put("slug", FieldUtil.createStringField("blub" + i));
			call(() -> client().updateNode(projectName(), contentUuid(), request));
		}

		// Period period = Period.of(0, 1, 10);
		// LocalDate maxAge = LocalDate.now().minus(period);
		// System.out.println(maxAge);
		// System.out.println(DateUtils.fromISO8601("2019-03-15T18:21Z"));
		// ZonedDateTime maxAge = Instant.now().atZone(ZoneOffset.UTC);
		ProjectVersionPurgeHandler handler = MeshInternal.get().projectVersionPurgeHandler();
		handler.purgeVersions(project, Optional.empty()).blockingAwait();
	}
}
