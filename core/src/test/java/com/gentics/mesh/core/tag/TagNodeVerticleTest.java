package com.gentics.mesh.core.tag;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.query.impl.NodeRequestParameter;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

public class TagNodeVerticleTest extends AbstractIsolatedRestVerticleTest {
	@Autowired
	private TagFamilyVerticle tagFamilyVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		return new ArrayList<>(Arrays.asList(tagFamilyVerticle));
	}

	@Test
	public void testReadNodesForTag() {
		try (NoTrx noTx = db.noTrx()) {
			NodeListResponse nodeList = call(() -> getClient().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new NodeRequestParameter().draft()));
			NodeResponse concorde = new NodeResponse();
			concorde.setUuid(content("concorde").getUuid());
			assertThat(nodeList.getData()).as("Tagged nodes").isNotNull().usingElementComparatorOnFields("uuid").containsOnly(concorde);
		}
	}

	@Test
	public void testReadPublishedNodesForTag() {
		try (NoTrx noTx = db.noTrx()) {
			NodeListResponse nodeList = call(() -> getClient().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid()));
			assertThat(nodeList.getData()).as("Published tagged nodes").isNotNull().isEmpty();

			// publish the node
			content("concorde").publish(getMockedInternalActionContext(""));

			nodeList = call(() -> getClient().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid()));
			NodeResponse concorde = new NodeResponse();
			concorde.setUuid(content("concorde").getUuid());
			assertThat(nodeList.getData()).as("Tagged nodes").isNotNull().usingElementComparatorOnFields("uuid").containsOnly(concorde);
		}
	}

	@Test
	public void testReadNodesForTagInRelease() {
		try (NoTrx noTx = db.noTrx()) {
			NodeResponse concorde = new NodeResponse();
			concorde.setUuid(content("concorde").getUuid());

			// create new release
			Release initialRelease = project().getInitialRelease();
			Release newRelease = project().getReleaseRoot().create("newrelease", user());

			// get for latest release (must be empty)
			assertThat(call(() -> getClient().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new NodeRequestParameter().draft())).getData()).as("Nodes tagged in latest release").isNotNull().isEmpty();

			// get for new release (must be empty)
			assertThat(call(() -> getClient().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new NodeRequestParameter().draft().setRelease(newRelease.getUuid()))).getData()).as("Nodes tagged in new release").isNotNull()
							.isEmpty();

			// get for initial release
			assertThat(call(() -> getClient().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid(),
					new NodeRequestParameter().draft().setRelease(initialRelease.getUuid()))).getData()).as("Nodes tagged in initial release")
							.isNotNull().usingElementComparatorOnFields("uuid").containsOnly(concorde);
		}
	}
}
