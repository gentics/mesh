package com.gentics.mesh.core.tag;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.PublishParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

public class TagNodeVerticleTest extends AbstractIsolatedRestVerticleTest {
	@Autowired
	private TagFamilyVerticle tagFamilyVerticle;

	@Autowired
	private NodeVerticle nodeVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		return new ArrayList<>(Arrays.asList(tagFamilyVerticle, nodeVerticle));
	}

	@Test
	public void testReadNodesForTag() {
		try (NoTx noTx = db.noTx()) {
			NodeListResponse nodeList = call(() -> getClient().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new VersioningParameters().draft()));
			NodeResponse concorde = new NodeResponse();
			concorde.setUuid(content("concorde").getUuid());
			assertThat(nodeList.getData()).as("Tagged nodes").isNotNull().usingElementComparatorOnFields("uuid").containsOnly(concorde);
		}
	}

	@Test
	public void testReadPublishedNodesForTag() {
		try (NoTx noTx = db.noTx()) {

			call(() -> getClient().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParameters().setRecursive(true)));
			NodeListResponse nodeList = call(() -> getClient().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid()));
			assertThat(nodeList.getData()).as("Published tagged nodes").isNotNull().isEmpty();

			// publish the node and its parent
			InternalActionContext ac = getMockedInternalActionContext(user());
			content("concorde").getParentNode(project().getLatestRelease().getUuid()).publish(ac);
			content("concorde").publish(ac);

			nodeList = call(() -> getClient().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid()));
			NodeResponse concorde = new NodeResponse();
			concorde.setUuid(content("concorde").getUuid());
			assertThat(nodeList.getData()).as("Tagged nodes").isNotNull().usingElementComparatorOnFields("uuid").containsOnly(concorde);
		}
	}

	@Test
	public void testReadNodesForTagInRelease() {
		try (NoTx noTx = db.noTx()) {
			NodeResponse concorde = new NodeResponse();
			concorde.setUuid(content("concorde").getUuid());

			// create new release
			Release initialRelease = project().getInitialRelease();
			Release newRelease = project().getReleaseRoot().create("newrelease", user());

			// get for latest release (must be empty)
			assertThat(call(() -> getClient().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new VersioningParameters().draft())).getData()).as("Nodes tagged in latest release").isNotNull().isEmpty();

			// get for new release (must be empty)
			assertThat(call(() -> getClient().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new VersioningParameters().draft().setRelease(newRelease.getUuid()))).getData()).as("Nodes tagged in new release").isNotNull()
							.isEmpty();

			// get for initial release
			assertThat(call(() -> getClient().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new VersioningParameters().draft().setRelease(initialRelease.getUuid()))).getData()).as("Nodes tagged in initial release")
							.isNotNull().usingElementComparatorOnFields("uuid").containsOnly(concorde);
		}
	}
}
