package com.gentics.mesh.core.project.maintenance;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;

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
		ProjectVersionPurgeHandler handler = MeshInternal.get().projectVersionPurgeHandler();
		handler.purgeVersions(project).blockingAwait();
	}
}
