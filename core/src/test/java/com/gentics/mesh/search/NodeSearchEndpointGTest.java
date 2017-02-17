package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestFullDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
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
		try (NoTx noTx = db().noTx()) {
			recreateIndices();
		}

		String oldContent = "supersonic";
		String newContent = "urschnell";
		String uuid = db().noTx(() -> content("concorde").getUuid());
		NodeResponse concorde = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft()));

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent), new VersioningParameters().draft()));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery(newContent), new VersioningParameters().draft()));
		assertThat(response.getData()).as("Search result").isEmpty();

		// change draft version of content
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.getFields().put("content", FieldUtil.createHtmlField(newContent));
		update.setVersion(new VersionReference().setNumber("1.0"));
		call(() -> client().updateNode(PROJECT_NAME, concorde.getUuid(), update));

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent), new VersioningParameters().draft()));
		assertThat(response.getData()).as("Search result").isEmpty();

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery(newContent), new VersioningParameters().draft()));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
	}

	@Test
	public void testSearchPublishedInRelease() throws Exception {
		try (NoTx noTx = db().noTx()) {
			recreateIndices();
		}

		String uuid = db().noTx(() -> content("concorde").getUuid());
		NodeResponse concorde = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft()));
		call(() -> client().publishNode(PROJECT_NAME, uuid));

		CountDownLatch latch = TestUtils.latchForMigrationCompleted(client());
		ReleaseCreateRequest createRelease = new ReleaseCreateRequest();
		createRelease.setName("newrelease");
		call(() -> client().createRelease(PROJECT_NAME, createRelease));
		failingLatch(latch);

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("supersonic")));
		assertThat(response.getData()).as("Search result").isEmpty();

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("supersonic"),
				new VersioningParameters().setRelease(db().noTx(() -> project().getInitialRelease().getName()))));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
	}

	@Test
	public void testSearchTagFamilies() throws Exception {
		try (NoTx noTx = db().noTx()) {
			recreateIndices();
		}

		String query = "{\n" +
				"  \"query\": {\n" +
				"    \"nested\": {\n" +
				"      \"path\": \"tagFamilies\",\n" +
				"      \"query\": {\n" +
				"        \"match\": {\n" +
				"          \"tagFamilies.colors.tags.name\": \"red\"\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, query));
		assertThat(response.getData()).isNotEmpty();
	}

}
