package com.gentics.mesh.core.tag;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.call;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class TagNodeEndpointTest extends AbstractMeshTest {

	@Test
	public void testReadNodesForTag() {
		try (Tx tx = tx()) {
			NodeListResponse nodeList = call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new VersioningParametersImpl().draft()));
			NodeResponse concorde = new NodeResponse();
			concorde.setUuid(content("concorde").getUuid());
			assertThat(nodeList.getData()).as("Tagged nodes").isNotNull().usingElementComparatorOnFields("uuid").containsOnly(concorde);
		}
	}

	@Test
	public void testReadPublishedNodesForTag() {
		try (Tx tx = tx()) {

			call(() -> client().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParametersImpl().setRecursive(true)));
			NodeListResponse nodeList = call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new VersioningParametersImpl().published()));
			assertThat(nodeList.getData()).as("Published tagged nodes").isNotNull().isEmpty();

			// publish the node and its parent
			InternalActionContext ac = mockActionContext();
			SearchQueueBatch batch = createBatch();
			content("concorde").getParentNode(project().getLatestRelease().getUuid()).publish(ac, batch);
			content("concorde").publish(ac, batch);

			nodeList = call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid()));
			NodeResponse concorde = new NodeResponse();
			concorde.setUuid(content("concorde").getUuid());
			assertThat(nodeList.getData()).as("Tagged nodes").isNotNull().usingElementComparatorOnFields("uuid").containsOnly(concorde);
		}
	}

	@Test
	public void testReadNodesForTagInRelease() {
		Release newRelease;
		NodeResponse concorde = new NodeResponse();
		concorde.setUuid(db().tx(() -> content("concorde").getUuid()));

		// Create new release
		try (Tx tx = tx()) {
			newRelease = project().getReleaseRoot().create("newrelease", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			Release initialRelease = initialRelease();
			// Get for latest release (must be empty)
			assertThat(call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new VersioningParametersImpl().draft())).getData()).as("Nodes tagged in latest release").isNotNull().isEmpty();

			// Get for new release (must be empty)
			assertThat(call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new VersioningParametersImpl().draft().setRelease(newRelease.getUuid()))).getData()).as("Nodes tagged in new release").isNotNull()
							.isEmpty();

			// Get for initial release
			assertThat(call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new VersioningParametersImpl().draft().setRelease(initialRelease.getUuid()))).getData()).as("Nodes tagged in initial release")
							.isNotNull().usingElementComparatorOnFields("uuid").containsOnly(concorde);
		}
	}

	@Test
	public void testTagOrder() {
		try (Tx tx = tx()) {
			TagFamily root = tagFamily("basic");
			Tag tag1 = root.create("test1", project(), user());
			Tag tag2 = root.create("test2", project(), user());
			Tag tag3 = root.create("test3", project(), user());

			Node node = content();

			node.addTag(tag1, latestRelease());
			node.addTag(tag3, latestRelease());
			node.addTag(tag2, latestRelease());

			role().grantPermissions(tag1, READ_PERM);
			role().grantPermissions(tag2, READ_PERM);
			role().grantPermissions(tag3, READ_PERM);
			tx.success();
		}

		String tagFamilyUuid = db().tx(() -> tagFamily("basic").getUuid());
		TagResponse tagResponse = call(() -> client().createTag(PROJECT_NAME, tagFamilyUuid, new TagCreateRequest().setName("test4")));

		String nodeUuid = contentUuid();
		TagListResponse list = call(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid));

		List<String> names = list.getData().stream().map(TagResponse::getName).collect(Collectors.toList());
		assertThat(names).containsExactly("test1", "test3", "test2");

		call(() -> client().addTagToNode(PROJECT_NAME, nodeUuid, tagResponse.getUuid()));

		list = call(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid));
		names = list.getData().stream().map(TagResponse::getName).collect(Collectors.toList());
		assertThat(names).containsExactly("test1", "test3", "test2", "test4");

	}
}
