package com.gentics.mesh.core.project;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.parameter.impl.JobParametersImpl;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true)
public class ProjectEndpointDeleteTest extends AbstractMeshTest {

	@Test
	public void testDeleteByUUID() throws Exception {
		String uuid = projectUuid();
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		String parentNodeUuid = tx(() -> folder("news").getUuid());

		// Create a lot of test nodes
		for (int i = 0; i < 100; i++) {
			NodeCreateRequest request = new NodeCreateRequest();
			SchemaReferenceImpl schemaReference = new SchemaReferenceImpl();
			schemaReference.setName("content");
			schemaReference.setUuid(schemaUuid);
			request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
			request.getFields().put("slug", FieldUtil.createStringField("new-page" + i + ".html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setSchema(schemaReference);
			request.setLanguage("en");
			request.setParentNodeUuid(parentNodeUuid);
			call(() -> client().createNode(PROJECT_NAME, request));
		}

		call(() -> client().deleteProject(uuid));
	}

	@Test
	public void testDeleteAfterPurge() {
		adminCall(() -> client().purgeProject(projectUuid()));
		call(() -> client().deleteProject(projectUuid()));
		call(() -> client().findProjectByUuid(projectUuid()), HttpResponseStatus.NOT_FOUND);
		JobListResponse response = adminCall(() -> client().findJobs(new JobParametersImpl().setType(JobType.versionpurge)));
		Assertions.assertThat(response.getData()).isEmpty();
	}

}
