package com.gentics.mesh.linkrenderer;

import static com.gentics.mesh.FieldUtil.createStringField;
import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true)
public class CrossProjectLinkRendererTest extends AbstractMeshTest {

	private static final String OTHER_PROJECT_NAME = "projectB";

	private WebRootLinkReplacer replacer;

	private NodeResponse nodeResponse;

	@Before
	public void setupDeps() {
		replacer = meshDagger().webRootLinkReplacer();

		// Create second project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(OTHER_PROJECT_NAME);
		request.setHostname("dummy.io");
		request.setSsl(true);
		request.setSchema(new SchemaReferenceImpl().setName("folder"));
		ProjectResponse projectResponse = call(() -> client().createProject(request));

		// Assign the content schema
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		call(() -> client().assignSchemaToProject(OTHER_PROJECT_NAME, schemaUuid));

		// Create the node which we link to
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.getFields().put("teaser", createStringField("some teaser"));
		nodeCreateRequest.getFields().put("slug", createStringField("new-page.html"));
		nodeCreateRequest.getFields().put("content", createStringField("Blessed mealtime again!"));
		nodeCreateRequest.setSchemaName("content");
		nodeCreateRequest.setParentNodeUuid(projectResponse.getRootNode().getUuid());
		nodeResponse = call(() -> client().createNode(OTHER_PROJECT_NAME, nodeCreateRequest));
	}

	@Test
	public void testCrossProjectLinkRendering() {
		try (Tx tx = tx()) {
			final String content = "{{mesh.link('" + nodeResponse.getUuid() + "')}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, initialBranchUuid(), ContainerType.DRAFT, content, LinkType.SHORT, null, null);
			assertEquals("Check rendered content", "https://dummy.io/new-page.html", replacedContent);

			String linkToNode = replacer.resolve(ac, initialBranchUuid(), ContainerType.DRAFT, nodeResponse.getUuid(), LinkType.SHORT, "en");
			assertEquals("Check rendered content", "https://dummy.io/new-page.html", linkToNode);

			HibNode node = tx.nodeDao().findByUuid(tx.projectDao().findByName(OTHER_PROJECT_NAME), nodeResponse.getUuid());
			linkToNode = replacer.resolve(ac, initialBranchUuid(), ContainerType.DRAFT, node, LinkType.SHORT, "en");
			assertEquals("Check rendered content", "https://dummy.io/new-page.html", linkToNode);
		}
	}

	@Test
	public void testNodeContentRendering() {
		NodeResponse createResponse = createNode("name", StringField.of("{{mesh.link('" + nodeResponse.getUuid() + "')}}"));
		NodeResponse nodeResponse = client().findNodeByUuid(PROJECT_NAME, createResponse.getUuid(), new NodeParametersImpl().setResolveLinks(LinkType.SHORT)).blockingGet();
		assertThat(nodeResponse).hasStringField("name", "https://dummy.io/new-page.html");
	}
}
