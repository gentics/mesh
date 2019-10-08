package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.parameter.impl.SearchParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = FULL, startServer = true)
public class WaitSearchEndpointTest extends AbstractMeshTest {

	@Before
	public void setUp() throws Exception {
		createNode("slug", new StringFieldImpl().setString("waittest"));
	}

	private NodeListResponse search(boolean wait) {
		return client().searchNodes(
			getSimpleQuery("fields.slug", "waittest"),
			new SearchParametersImpl().setWait(wait)).blockingGet();
	}

	private ObjectNode rawSearch(boolean wait) {
		return client().searchNodesRaw(
			getSimpleQuery("fields.slug", "waittest"),
			new SearchParametersImpl().setWait(wait)).blockingGet();
	}

	@Test
	public void searchWithWaitDisabled() {
		NodeListResponse result = search(false);

		assertThat(result.getMetainfo().getTotalCount()).isEqualTo(0);
	}

	@Test
	public void searchWithWaitEnabled() {
		NodeListResponse result = search(true);

		assertThat(result.getMetainfo().getTotalCount()).isEqualTo(1);
	}

	@Test
	public void rawSearchWithWaitDisabled() {
		ObjectNode result = rawSearch(false);

		if (complianceMode() == ComplianceMode.ES_7) {
			assertThat(result.get("responses").get(0).get("hits").get("total").get("value").asLong()).isEqualTo(0);
		} else {
			assertThat(result.get("responses").get(0).get("hits").get("total").asLong()).isEqualTo(0);
		}
	}

	@Test
	public void rawSearchWithWaitEnabled() {
		ObjectNode result = rawSearch(true);
		if (complianceMode() == ComplianceMode.ES_7) {
			assertThat(result.get("responses").get(0).get("hits").get("total").get("value").asLong()).isEqualTo(1);
		} else {
			assertThat(result.get("responses").get(0).get("hits").get("total").asLong()).isEqualTo(1);
		}
	}
}
