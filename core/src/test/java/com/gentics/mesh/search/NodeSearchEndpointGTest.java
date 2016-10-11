package com.gentics.mesh.search;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.test.performance.TestUtils;

public class NodeSearchEndpointGTest extends AbstractNodeSearchEndpointTest {

	@Test
	public void testSearchDraftNodes() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		String oldContent = "supersonic";
		String newContent = "urschnell";
		String uuid = db.noTx(() -> content("concorde").getUuid());
		NodeResponse concorde = call(() -> getClient().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft()));

		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent), new VersioningParameters().draft()));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(newContent), new VersioningParameters().draft()));
		assertThat(response.getData()).as("Search result").isEmpty();

		// change draft version of content
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.getFields().put("content", FieldUtil.createHtmlField(newContent));
		update.setVersion(new VersionReference().setNumber("1.0"));
		call(() -> getClient().updateNode(PROJECT_NAME, concorde.getUuid(), update));

		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent), new VersioningParameters().draft()));
		assertThat(response.getData()).as("Search result").isEmpty();

		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(newContent), new VersioningParameters().draft()));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
	}

	@Test
	public void testSearchPublishedInRelease() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		String uuid = db.noTx(() -> content("concorde").getUuid());
		NodeResponse concorde = call(() -> getClient().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft()));
		call(() -> getClient().publishNode(PROJECT_NAME, uuid));

		CountDownLatch latch = TestUtils.latchForMigrationCompleted(getClient());
		ReleaseCreateRequest createRelease = new ReleaseCreateRequest();
		createRelease.setName("newrelease");
		call(() -> getClient().createRelease(PROJECT_NAME, createRelease));
		failingLatch(latch);

		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery("supersonic")));
		assertThat(response.getData()).as("Search result").isEmpty();

		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery("supersonic"),
				new VersioningParameters().setRelease(db.noTx(() -> project().getInitialRelease().getName()))));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
	}

}
