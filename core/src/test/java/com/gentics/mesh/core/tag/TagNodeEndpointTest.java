package com.gentics.mesh.core.tag;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
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
			BulkActionContext bac = createBulkContext();
			content("concorde").getParentNode(project().getLatestBranch().getUuid()).publish(ac, bac);
			content("concorde").publish(ac, bac);

			nodeList = call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid()));
			NodeResponse concorde = new NodeResponse();
			concorde.setUuid(content("concorde").getUuid());
			assertThat(nodeList.getData()).as("Tagged nodes").isNotNull().usingElementComparatorOnFields("uuid").containsOnly(concorde);
		}
	}

	@Test
	public void testReadNodesForTagInBranch() {
		NodeResponse concorde = new NodeResponse();
		concorde.setUuid(db().tx(() -> content("concorde").getUuid()));

		// Create new branch
		HibBranch newBranch = tx(() -> createBranch("newbranch"));

		try (Tx tx = tx()) {
			HibBranch initialBranch = initialBranch();
			// Get for latest branch (must be empty)
			assertThat(call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
				new VersioningParametersImpl().draft())).getData()).as("Nodes tagged in latest branch").isNotNull().isEmpty();

			// Get for new branch (must be empty)
			assertThat(call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
				new VersioningParametersImpl().draft().setBranch(newBranch.getUuid()))).getData()).as("Nodes tagged in new branch").isNotNull()
					.isEmpty();

			// Get for initial branch
			assertThat(call(() -> client().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
				new VersioningParametersImpl().draft().setBranch(initialBranch.getUuid()))).getData()).as("Nodes tagged in initial branch")
					.isNotNull().usingElementComparatorOnFields("uuid").containsOnly(concorde);
		}
	}

	@Test
	public void testTagOrder() {
		try (Tx tx = tx()) {
			TagDaoWrapper tagDao = tx.data().tagDao();
			RoleDaoWrapper roleDao = tx.data().roleDao();
			HibTagFamily root = tagFamily("basic");
			HibTag tag1 = tagDao.create(root, "test1", project(), user());
			HibTag tag2 = tagDao.create(root, "test2", project(), user());
			HibTag tag3 = tagDao.create(root, "test3", project(), user());

			Node node = content();

			node.addTag(tag1, latestBranch());
			node.addTag(tag3, latestBranch());
			node.addTag(tag2, latestBranch());

			roleDao.grantPermissions(role(), tag1, READ_PERM);
			roleDao.grantPermissions(role(), tag2, READ_PERM);
			roleDao.grantPermissions(role(), tag3, READ_PERM);
			tx.success();
		}

		String tagFamilyUuid = db().tx(() -> tagFamily("basic").getUuid());
		TagResponse tagResponse = call(() -> client().createTag(PROJECT_NAME, tagFamilyUuid, new TagCreateRequest().setName("test4")));

		String nodeUuid = contentUuid();
		TagListResponse list = call(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid));

		List<String> names = list.getData().stream().map(entry -> entry.getName()).collect(Collectors.toList());
		assertThat(names).containsExactly("test1", "test3", "test2");

		call(() -> client().addTagToNode(PROJECT_NAME, nodeUuid, tagResponse.getUuid()));

		list = call(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid));
		names = list.getData().stream().map(entry -> entry.getName()).collect(Collectors.toList());
		assertThat(names).containsExactly("test1", "test3", "test2", "test4");

	}
}
