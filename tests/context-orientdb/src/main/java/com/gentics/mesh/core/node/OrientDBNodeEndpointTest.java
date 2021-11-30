package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_PUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNPUBLISHED;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Test;

import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true)
public class OrientDBNodeEndpointTest extends AbstractMeshTest {

	@Test
	public void testTakeNodeOffline() {
		HibNode node = folder("products");
		String nodeUuid = tx(() -> node.getUuid());
		String schemaUuid = tx(() -> schemaContainer("folder").getUuid());
		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());

		expect(NODE_UNPUBLISHED).match(1, NodeMeshEventModel.class, event -> {
			assertThat(event)
				.hasUuid(baseNodeUuid)
				.hasSchema("folder", schemaUuid)
				.hasBranchUuid(initialBranchUuid())
				.hasLanguage("en")
				.hasProject(PROJECT_NAME, projectUuid());
		});
		expect(NODE_UNPUBLISHED).total(29);
		call(() -> client().takeNodeOffline(PROJECT_NAME, baseNodeUuid, new PublishParametersImpl().setRecursive(true)));
		awaitEvents();

		expect(NODE_PUBLISHED).match(1, NodeMeshEventModel.class, event -> {
			assertThat(event)
				.hasUuid(nodeUuid)
				.hasSchema("folder", schemaUuid)
				.hasBranchUuid(initialBranchUuid())
				.hasLanguage("de")
				.hasProject(PROJECT_NAME, projectUuid());
		});
		expect(NODE_PUBLISHED).match(1, NodeMeshEventModel.class, event -> {
			assertThat(event)
				.hasUuid(nodeUuid)
				.hasSchema("folder", schemaUuid)
				.hasBranchUuid(initialBranchUuid())
				.hasLanguage("en")
				.hasProject(PROJECT_NAME, projectUuid());
		});
		expect(NODE_PUBLISHED).total(2);
		assertThat(call(() -> client().publishNode(PROJECT_NAME, nodeUuid))).as("Publish Status").isPublished("en").isPublished("de");
		awaitEvents();

		// assert that the containers have both webrootpath properties set
		try (Tx tx1 = tx()) {
			for (String language : Arrays.asList("en", "de")) {
				NodeGraphFieldContainer container = (NodeGraphFieldContainer) boot().contentDao().getFieldContainer(folder("products"), language);
				GraphFieldContainerEdge draftEdge = container.getContainerEdge(DRAFT, initialBranchUuid()).next();
				assertThat(draftEdge.getSegmentInfo()).isNotNull();
				GraphFieldContainerEdge publishEdge = container.getContainerEdge(PUBLISHED, initialBranchUuid()).next();
				assertThat(publishEdge.getSegmentInfo()).isNotNull();
			}
		}

		call(() -> client().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParametersImpl().setRecursive(true)));
		assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid))).as("Publish status").isNotPublished("en").isNotPublished("de");

		// assert that the containers have only the draft webrootpath properties set
		try (Tx tx2 = tx()) {
			for (String language : Arrays.asList("en", "de")) {
				NodeGraphFieldContainer container = (NodeGraphFieldContainer) boot().contentDao().getFieldContainer(folder("products"), language);
				GraphFieldContainerEdge draftEdge = container.getContainerEdge(DRAFT, initialBranchUuid()).next();
				assertThat(draftEdge.getSegmentInfo()).isNotNull();
				assertFalse(container.getContainerEdge(PUBLISHED, initialBranchUuid()).hasNext());
			}
		}

	}
}
