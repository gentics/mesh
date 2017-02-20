package com.gentics.mesh.core.tag;

import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.test.TestFullDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

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
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.PublishParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, useTinyDataset = false, startServer = true)
public class TagNodeEndpointTest extends AbstractMeshTest {

	@Test
	public void testReadNodesForTag() {
		try (NoTx noTx = db().noTx()) {
			NodeListResponse nodeList = call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new VersioningParameters().draft()));
			NodeResponse concorde = new NodeResponse();
			concorde.setUuid(content("concorde").getUuid());
			assertThat(nodeList.getData()).as("Tagged nodes").isNotNull().usingElementComparatorOnFields("uuid").containsOnly(concorde);
		}
	}

	@Test
	public void testReadPublishedNodesForTag() {
		try (NoTx noTx = db().noTx()) {

			call(() -> client().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParameters().setRecursive(true)));
			NodeListResponse nodeList = call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new VersioningParameters().published()));
			assertThat(nodeList.getData()).as("Published tagged nodes").isNotNull().isEmpty();

			// publish the node and its parent
			InternalActionContext ac = getMockedInternalActionContext(user());
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
		try (NoTx noTx = db().noTx()) {
			NodeResponse concorde = new NodeResponse();
			concorde.setUuid(content("concorde").getUuid());

			// create new release
			Release initialRelease = project().getInitialRelease();
			Release newRelease = project().getReleaseRoot().create("newrelease", user());

			// get for latest release (must be empty)
			assertThat(call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new VersioningParameters().draft())).getData()).as("Nodes tagged in latest release").isNotNull().isEmpty();

			// get for new release (must be empty)
			assertThat(call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new VersioningParameters().draft().setRelease(newRelease.getUuid()))).getData()).as("Nodes tagged in new release").isNotNull()
							.isEmpty();

			// get for initial release
			assertThat(call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new VersioningParameters().draft().setRelease(initialRelease.getUuid()))).getData()).as("Nodes tagged in initial release")
							.isNotNull().usingElementComparatorOnFields("uuid").containsOnly(concorde);
		}
	}

	@Test
	public void testTagOrder() {
		Tag tag1;
		Tag tag2;
		Tag tag3;
		try (NoTx noTx = db().noTx()) {
			TagFamily root = tagFamily("basic");
			tag1 = root.create("test1", project(), user());
			tag2 = root.create("test2", project(), user());
			tag3 = root.create("test3", project(), user());

			Node node = content();

			node.addTag(tag1, release());
			node.addTag(tag3, release());
			node.addTag(tag2, release());

		}

		String tagFamilyUuid = db().noTx(() -> tagFamily("basic").getUuid());
		TagResponse tagResponse = call(() -> client().createTag(PROJECT_NAME, tagFamilyUuid, new TagCreateRequest().setName("test4")));

		String nodeUuid = db().noTx(() -> content().getUuid());

		TagListResponse list = call(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid));

		List<String> names = list.getData().stream().map(entry -> entry.getName()).collect(Collectors.toList());
		assertThat(names).containsExactly("test1", "test3", "test2");

		call(() -> client().addTagToNode(PROJECT_NAME, nodeUuid, tagResponse.getUuid()));

		list = call(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid));
		names = list.getData().stream().map(entry -> entry.getName()).collect(Collectors.toList());
		assertThat(names).containsExactly("test1", "test3", "test2", "test4");
		

	}
}
