package com.gentics.mesh.linkrenderer;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

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
		nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
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

			linkToNode = replacer.resolve(ac, initialBranchUuid(), ContainerType.DRAFT, project().getNodeRoot().findByUuid(nodeResponse.getUuid()),
					LinkType.SHORT, "en");
			assertEquals("Check rendered content", "https://dummy.io/new-page.html", linkToNode);
		}
	}

}
